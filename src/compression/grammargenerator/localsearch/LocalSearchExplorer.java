package compression.grammargenerator.localsearch;

import compression.RuleProbType;
import compression.data.CachedDataset;
import compression.data.Dataset;
import compression.data.FolderBasedDataset;
import compression.grammar.NonTerminal;
import compression.grammar.RNAWithStructure;
import compression.grammar.Rule;
import compression.grammar.SecondaryStructureGrammar;
import compression.grammar.Terminal;
import compression.grammargenerator.AbstractGrammarExplorer;
import compression.grammargenerator.RandomGrammarExplorer;
import compression.grammargenerator.localsearch.dataclasses.Config;
import compression.grammargenerator.localsearch.dataclasses.NeighborSearchOutcome;
import compression.grammargenerator.localsearch.dataclasses.RunResult;
import compression.grammargenerator.localsearch.dataclasses.RunStats;
import compression.grammargenerator.localsearch.dataclasses.SearchState;
import compression.parser.SRFParser;
import compression.util.MyMultimap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static java.lang.System.out;

/**
 * Simple hill-climbing local search over grammars:
 * start from a (random) parsable grammar, evaluate bits/base on the objective dataset,
 * then repeatedly take the first improving neighbor (add / remove / swap one rule)
 * until no improving move is found.
 * <p>
 * Hyperparameters are configured inside {@link #main(String[])} for now so the
 * search can be launched directly from an IDE.
 */
public class LocalSearchExplorer extends AbstractGrammarExplorer {
	private final Random random;
	private final long seed;
	private final Dataset objectiveDataset;
	private final Dataset objectiveDatasetLimited;
	private final List<List<Terminal<Character>>> parsableDatasetWords;
	private final List<List<Terminal<Character>>> objectiveDatasetWords;
    private final boolean withNonCanonicalRules;
	private final Map<Rule, Integer> ruleToIndex;

	private static final double IMPROVEMENT_EPS = 1e-3; // at least 1/1000
	private LocalSearchExplorer(final int nNonterminals,
	                            final long seed,
	                            final Dataset objectiveDataset,
	                            final Dataset parsableDataset,
	                            final boolean withNonCanonicalRules,
	                            final int objectiveLimit) {
		super(nNonterminals);
		this.random = new Random(seed);
		this.seed = seed;
		this.objectiveDataset = objectiveDataset;
		this.withNonCanonicalRules = withNonCanonicalRules;
		this.parsableDatasetWords = new ArrayList<>(parsableDataset.getSize());
		for (RNAWithStructure rna : parsableDataset)
			parsableDatasetWords.add(rna.secondaryStructureAsTerminals());
		List<RNAWithStructure> rnas = new ArrayList<>(objectiveDataset.getSize());
		for (RNAWithStructure rna : objectiveDataset)
			rnas.add(rna);
		if (objectiveLimit > 0 && objectiveLimit < rnas.size())
			rnas = rnas.subList(0, objectiveLimit);
        List<RNAWithStructure> objectiveRnasLimited = Collections.unmodifiableList(rnas);
		this.objectiveDatasetLimited = new Dataset() {
			@Override
			public int getSize() {
				return objectiveRnasLimited.size();
			}

			@Override
			public String name() {
				return objectiveDataset.name() + "-limited";
			}

			@Override
			public Iterator<RNAWithStructure> iterator() {
				return objectiveRnasLimited.iterator();
			}
		};
		this.objectiveDatasetWords = new ArrayList<>(objectiveRnasLimited.size());
		for (RNAWithStructure rna : objectiveRnasLimited)
			objectiveDatasetWords.add(rna.secondaryStructureAsTerminals());
		this.ruleToIndex = new HashMap<>(allPossibleRules.length);
		for (int i = 0; i < allPossibleRules.length; i++)
			ruleToIndex.put(allPossibleRules[i], i);
	}

	public static void main(String[] args) throws Exception {
		Config config = Config.defaults();

		final Dataset objectiveDataset = new CachedDataset(new FolderBasedDataset(config.objectiveDatasetName()));
		final Dataset parsableDataset = new CachedDataset(new FolderBasedDataset(config.parsableDatasetName()));

		Logging.printConfig(config, objectiveDataset, parsableDataset);

		ExecutorService executor = Executors.newFixedThreadPool(config.poolSize());
		List<Future<RunResult>> futures = new ArrayList<>();

		for (int r = 0; r < config.numRuns(); r++) {
			final int runNumber = r + 1;
			final long runSeed = config.baseSeed() + r;
			Callable<RunResult> task = () -> {
				Logging.printRunStart(runNumber, config.numRuns(), runSeed);
				LocalSearchExplorer explorer = new LocalSearchExplorer(
						config.nNonterminals(),
						runSeed,
						objectiveDataset,
						parsableDataset,
						config.withNonCanonicalRules(),
						config.objectiveLimit());
				return explorer.runSingleRun(
						config.initialRuleCount(),
						config.maxSeedAttempts(),
						config.maxSteps(),
						config.maxSwapCandidatesPerStep(),
						config.maxNeighborEvaluationsPerStep(),
						config.logSteps(),
						runNumber);
			};
			futures.add(executor.submit(task));
		}

		List<RunResult> runResults = new ArrayList<>();
		for (int i = 0; i < futures.size(); i++) {
			try {
				RunResult result = futures.get(i).get();
				runResults.add(result);
				RunStats stats = result.stats();
				Logging.printRunCompleted(stats);
			} catch (ExecutionException e) {
				out.printf("Run %d failed: %s%n", i + 1, e.getCause().getMessage());
			}
		}
		executor.shutdown();

		RunResult best = runResults.stream()
				.min(Comparator.comparingDouble(r -> r.best().bitsPerBase()))
				.orElse(null);

		out.println("\n=== Run summary ===");
        for (RunResult r : runResults) {
            RunStats stats = r.stats();
            Logging.printSummaryLine(stats);
        }
		if (best != null) {
			Logging.printBestOverall(best);
		}
	}

	private RunResult runSingleRun(final int initialRuleCount,
	                               final int maxSeedAttempts,
	                               final int maxSteps,
	                               final int maxSwapCandidatesPerStep,
	                               final int maxNeighborEvaluationsPerStep,
	                               final boolean logSteps,
	                               final int runNumber) {
		SearchState current = sampleParsableSeed(initialRuleCount, maxSeedAttempts);
		Logging.printSeed(runNumber, current.grammar().size(), current.bitsPerBase());

		int stepsTaken = 0;
		int totalNeighborsEvaluated = 0;
		for (int step = 0; step < maxSteps; step++) {
			stepsTaken++;
			NeighborSearchOutcome outcome = firstImprovingNeighbor(
					current,
					maxSwapCandidatesPerStep,
					maxNeighborEvaluationsPerStep);
			totalNeighborsEvaluated += outcome.evaluated();
			if (!outcome.improved()) {
				if (logSteps) {
					Logging.printStepNoImprovement(runNumber, step, current.grammar().size(), current.bitsPerBase(), outcome.evaluated());
				}
				break;
			}
			current = outcome.next();
			if (logSteps) {
				Logging.printStepImprovement(
						runNumber,
						step,
						outcome.previousGrammarSize(),
						outcome.previousBitsPerBase(),
						outcome.evaluated(),
						outcome.improvementNeighborIndex(),
						current.grammar().size(),
						current.bitsPerBase());
			}
		}

		RunStats stats = new RunStats(
				runNumber,
				seed,
				stepsTaken,
				totalNeighborsEvaluated,
				current.grammar().size(),
				current.bitsPerBase());
		return new RunResult(current, stats);
	}

	private SearchState sampleParsableSeed(final int nRules, final int maxAttempts) {
		RandomGrammarExplorer generator = new RandomGrammarExplorer(nNonterminals);
		for (int attempt = 1; attempt <= maxAttempts; attempt++) {
			SecondaryStructureGrammar grammar = generator.randomGrammar(random, nRules);
			SRFParser<Character> parser = new SRFParser<>(grammar);
			if (!passesDataset(parser, parsableDatasetWords)) continue;
			if (!passesDataset(parser, objectiveDatasetWords)) continue;
			boolean[] mask = toMask(grammar);
			double score = score(grammar);
			if (!Double.isFinite(score)) continue;
			out.printf("Seed candidate %d: size=%d bits/base=%.4f%n", attempt, grammar.size(), score);
			return new SearchState(mask, grammar, score);
		}
		throw new IllegalStateException("Could not find a parsable seed grammar after " + maxAttempts + " attempts");
	}

	private NeighborSearchOutcome firstImprovingNeighbor(final SearchState current,
	                                                     final int maxSwapCandidates,
	                                                     final int maxNeighborEvaluations) {
		List<Move> moves = enumerateMoves(current.ruleMask(), maxSwapCandidates);
		Collections.shuffle(moves, random);
		int evaluated = 0;
		int neighborIndex = 0;
		for (Move move : moves) {
			if (evaluated >= maxNeighborEvaluations) break;
			boolean[] candidateMask = applyMove(current.ruleMask(), move);
			SecondaryStructureGrammar candidateGrammar = buildGrammarIfValid(candidateMask);
			if (candidateGrammar == null) continue;
			SRFParser<Character> parser = new SRFParser<>(candidateGrammar);
			if (!passesDataset(parser, parsableDatasetWords)) continue;
			if (!passesDataset(parser, objectiveDatasetWords)) continue;
			double score = score(candidateGrammar);
			evaluated++;
			neighborIndex++;
			if (score + IMPROVEMENT_EPS < current.bitsPerBase()) {
				return new NeighborSearchOutcome(
						new SearchState(candidateMask, candidateGrammar, score),
						evaluated,
						neighborIndex,
						current.grammar().size(),
						current.bitsPerBase(),
						true);
			}
		}
		return new NeighborSearchOutcome(null, evaluated, -1, current.grammar().size(), current.bitsPerBase(), false);
	}

	private List<Move> enumerateMoves(final boolean[] ruleMask, final int maxSwapCandidates) {
		List<Integer> present = new ArrayList<>();
		List<Integer> absent = new ArrayList<>();
		for (int i = 0; i < ruleMask.length; i++) {
			if (ruleMask[i]) present.add(i);
			else absent.add(i);
		}

		List<Move> moves = new ArrayList<>();
		for (Integer idx : present) {
			moves.add(Move.remove(idx));
		}
		for (Integer idx : absent) {
			moves.add(Move.add(idx));
		}

		if (!present.isEmpty() && !absent.isEmpty() && maxSwapCandidates > 0) {
			Set<Long> seenPairs = new HashSet<>();
			int swapsToGenerate = Math.min(maxSwapCandidates, present.size() * absent.size());
			for (int i = 0; i < swapsToGenerate; i++) {
				int from = present.get(random.nextInt(present.size()));
				int to = absent.get(random.nextInt(absent.size()));
				long key = (((long) from) << 32) | (to & 0xffffffffL);
				if (seenPairs.add(key)) {
					moves.add(Move.swap(from, to));
				}
			}
		}
		return moves;
	}

	private boolean[] applyMove(final boolean[] ruleMask, final Move move) {
		boolean[] next = Arrays.copyOf(ruleMask, ruleMask.length);
		switch (move.type) {
			case ADD:
				next[move.target] = true;
				break;
			case REMOVE:
				next[move.target] = false;
				break;
			case SWAP:
				next[move.source] = false;
				next[move.target] = true;
				break;
		}
		return next;
	}

	private SecondaryStructureGrammar buildGrammarIfValid(final boolean[] mask) {
		MyMultimap<NonTerminal, Rule> rules = new MyMultimap<>();
		for (int i = 0; i < mask.length; i++) {
			if (mask[i]) {
				Rule rule = allPossibleRules[i];
				rules.put(rule.left, rule);
			}
		}
		try {
			String name = "LocalSearch_" + Arrays.toString(mask);
			return new SecondaryStructureGrammar(name, nonTerminals[nNonterminals - 1], rules);
		} catch (IllegalArgumentException e) {
			return null; // e.g., start symbol lost all rules
		}
	}

	private double score(final SecondaryStructureGrammar grammar) {
		try {
			return getBitsPerBase(objectiveDatasetLimited, RuleProbType.ADAPTIVE, grammar, withNonCanonicalRules);
		} catch (RuntimeException e) {
			// if encoding fails (e.g., due to parsing), treat as bad
			return Double.POSITIVE_INFINITY;
		}
	}

	private boolean[] toMask(final SecondaryStructureGrammar grammar) {
		boolean[] mask = new boolean[allPossibleRules.length];
		for (Rule rule : grammar.getAllRules()) {
			Integer idx = ruleToIndex.get(rule);
			if (idx != null) {
				mask[idx] = true;
			}
		}
		return mask;
	}

	private boolean passesDataset(final SRFParser<Character> ssParser, final List<List<Terminal<Character>>> words) {
		for (List<Terminal<Character>> word : words) {
			if (!ssParser.parsable(word)) {
				return false;
			}
		}
		return true;
	}

	private record Move(Type type, int source, int target) {
		enum Type {ADD, REMOVE, SWAP}

		static Move add(final int target) {
			return new Move(Type.ADD, -1, target);
		}

		static Move remove(final int target) {
			return new Move(Type.REMOVE, target, target);
		}

		static Move swap(final int source, final int target) {
			return new Move(Type.SWAP, source, target);
		}
	}
}
