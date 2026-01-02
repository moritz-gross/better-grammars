package compression.grammargenerator.localsearch;

import compression.RuleProbType;
import compression.data.Dataset;
import compression.grammar.SecondaryStructureGrammar;
import compression.grammar.Terminal;
import compression.grammargenerator.AbstractGrammarExplorer;
import compression.grammargenerator.RandomGrammarExplorer;
import compression.grammargenerator.localsearch.dataclasses.Config;
import compression.grammargenerator.localsearch.dataclasses.NeighborSearchOutcome;
import compression.grammargenerator.localsearch.dataclasses.RunResult;
import compression.grammargenerator.localsearch.dataclasses.RunStats;
import compression.grammargenerator.localsearch.dataclasses.SearchState;
import compression.grammargenerator.localsearch.dataclasses.SearchStrategy;
import compression.parser.SRFParser;

import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Simple hill-climbing local search over grammars:
 * start from a (random) parsable grammar, evaluate bits/base on the objective dataset,
 * then repeatedly apply neighbor moves (add / remove / swap one rule) using either
 * first-improvement or best-improvement until no improving move is found.
 */
public class LocalSearchExplorer extends AbstractGrammarExplorer {
	private final Random random;
	private final long seed;
	private final Dataset objectiveDatasetLimited;
	private final List<List<Terminal<Character>>> parsableDatasetWords;
	private final List<List<Terminal<Character>>> objectiveDatasetWords;
    private final boolean withNonCanonicalRules;
	private final SearchStrategy searchStrategy;
	private final RuleMaskCodec ruleMaskCodec;
	private final NeighborSearcher neighborSearcher;

	LocalSearchExplorer(final int nNonterminals,
	                            final long seed,
	                            final Dataset objectiveDataset,
	                            final Dataset parsableDataset,
	                            final boolean withNonCanonicalRules,
	                            final int objectiveLimit,
	                            final SearchStrategy searchStrategy) {
		super(nNonterminals);
		this.random = new Random(seed);
		this.seed = seed;
		this.withNonCanonicalRules = withNonCanonicalRules;
		this.searchStrategy = searchStrategy;
		DatasetBundle bundle = DatasetBundle.from(objectiveDataset, parsableDataset, objectiveLimit);
		this.parsableDatasetWords = bundle.getParsableDatasetWords();
		this.objectiveDatasetWords = bundle.getObjectiveDatasetWords();
		this.objectiveDatasetLimited = bundle.getObjectiveDatasetLimited();
		this.ruleMaskCodec = new RuleMaskCodec(allPossibleRules, nonTerminals[nNonterminals - 1]);
		ScoreEvaluator scoreEvaluator = grammar ->
				getBitsPerBase(objectiveDatasetLimited, RuleProbType.ADAPTIVE, grammar, withNonCanonicalRules);
		this.neighborSearcher = new NeighborSearcher(
				ruleMaskCodec,
				parsableDatasetWords,
				objectiveDatasetWords,
				scoreEvaluator,
				random);
	}

	public static void main(String[] args) throws Exception {
		runWithConfig(Config.defaults());
	}

	public static List<RunResult> runWithConfig(final Config config) throws Exception {
		return LocalSearchRunner.run(config);
	}

	public static RunResult bestResult(List<RunResult> runResults) {
		return runResults.stream()
				.min(Comparator.comparingDouble(r -> r.getBest().getBitsPerBase()))
				.orElse(null);
	}

	RunResult runSingleRun(final int initialRuleCount,
	                               final int maxSeedAttempts,
	                               final int maxSteps,
	                               final int maxSwapCandidatesPerStep,
	                               final int maxNeighborEvaluationsPerStep,
	                               final int maxCandidatesPerStep,
	                               final int runNumber) {
		SearchState current = sampleParsableSeed(initialRuleCount, maxSeedAttempts);
		Logging.printSeed(runNumber, current.getGrammar().size(), current.getBitsPerBase());

		int stepsTaken = 0;
		int totalNeighborsEvaluated = 0;
		for (int step = 0; step < maxSteps; step++) {
			stepsTaken++;
			NeighborSearchOutcome outcome = neighborSearcher.search(current, maxSwapCandidatesPerStep, maxNeighborEvaluationsPerStep, maxCandidatesPerStep, searchStrategy);
			totalNeighborsEvaluated += outcome.getEvaluated();
			if (!outcome.isImproved()) {
				Logging.printStepNoImprovement(runNumber, step, current.getGrammar().size(), current.getBitsPerBase(), outcome.getEvaluated());
				break;
			}
			current = outcome.getNext();
			Logging.printStepImprovement(
					runNumber,
					step,
					outcome.getPreviousGrammarSize(),
					outcome.getPreviousBitsPerBase(),
					outcome.getEvaluated(),
					outcome.getImprovementNeighborIndex(),
					current.getGrammar().size(),
					current.getBitsPerBase());
		}

		RunStats stats = new RunStats(
				runNumber,
				seed,
				stepsTaken,
				totalNeighborsEvaluated,
				current.getGrammar().size(),
				current.getBitsPerBase());
		return new RunResult(current, stats);
	}

	private SearchState sampleParsableSeed(final int nRules, final int maxAttempts) {
		RandomGrammarExplorer generator = new RandomGrammarExplorer(nNonterminals);
		for (int attempt = 1; attempt <= maxAttempts; attempt++) {
			SecondaryStructureGrammar grammar = generator.randomGrammar(random, nRules);
			SRFParser<Character> parser = new SRFParser<>(grammar);
		if (!Utils.passesDataset(parser, parsableDatasetWords)) continue;
		if (!Utils.passesDataset(parser, objectiveDatasetWords)) continue;
			boolean[] mask = ruleMaskCodec.toMask(grammar);
			double score = getBitsPerBase(objectiveDatasetLimited, RuleProbType.ADAPTIVE, grammar, withNonCanonicalRules);
			if (!Double.isFinite(score)) continue;
			Logging.printSeedCandidate(attempt, grammar.size(), score);
			return new SearchState(mask, grammar, score);
		}
		throw new IllegalStateException("Could not find a parsable seed grammar after " + maxAttempts + " attempts");
	}

}
