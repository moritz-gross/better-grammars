package compression.grammargenerator;

import compression.RuleProbType;
import compression.data.CachedDataset;
import compression.data.Dataset;
import compression.data.FolderBasedDataset;
import compression.grammar.NonTerminal;
import compression.grammar.RNAWithStructure;
import compression.grammar.Rule;
import compression.grammar.SecondaryStructureGrammar;
import compression.grammar.Terminal;
import compression.parser.SRFParser;
import compression.util.MyMultimap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
		this.objectiveDataset = objectiveDataset;
		this.withNonCanonicalRules = withNonCanonicalRules;
		this.parsableDatasetWords = new ArrayList<>(parsableDataset.getSize());
		for (RNAWithStructure rna : parsableDataset) {
			parsableDatasetWords.add(rna.secondaryStructureAsTerminals());
		}
		List<RNAWithStructure> rnas = new ArrayList<>(objectiveDataset.getSize());
		for (RNAWithStructure rna : objectiveDataset) {
			rnas.add(rna);
		}
		if (objectiveLimit > 0 && objectiveLimit < rnas.size()) {
			rnas = rnas.subList(0, objectiveLimit);
		}
        List<RNAWithStructure> objectiveRnasLimited = Collections.unmodifiableList(rnas);
		this.objectiveDatasetLimited = new ListBackedDataset(objectiveDataset.name() + "-limited", objectiveRnasLimited);

		this.objectiveDatasetWords = new ArrayList<>(objectiveRnasLimited.size());
		for (RNAWithStructure rna : objectiveRnasLimited) {
			objectiveDatasetWords.add(rna.secondaryStructureAsTerminals());
		}
		this.ruleToIndex = new HashMap<>(allPossibleRules.length);
		for (int i = 0; i < allPossibleRules.length; i++) {
			ruleToIndex.put(allPossibleRules[i], i);
		}
	}

	public static void main(String[] args) throws Exception {
		final int nNonterminals = 3;
		final int initialRuleCount = 20;             // start reasonably large; hill-climb will remove/swap/add rules
		final long baseSeed = 42424242L;
		final int maxSteps = 100;                    // stop if no improving neighbor earlier
		final int maxSwapCandidatesPerStep = 100;    // sample this many random swaps per step
		final int maxNeighborEvaluationsPerStep = 150;
		final int maxSeedAttempts = 2000;            // retries to find a parsable seed
		final boolean withNonCanonicalRules = false; // user preference
		final int objectiveLimit = -1;               // limit objective dataset to first N RNAs (-1 means use all)
		final int numRuns = 3;                       // how many independent hill-climb runs
		final boolean logSteps = true;              // toggle per-step logging

		final Dataset objectiveDataset = new CachedDataset(new FolderBasedDataset("small-dataset"));
		final Dataset parsableDataset = new CachedDataset(new FolderBasedDataset("minimal-parsable"));

		System.out.println("=== Local search configuration ===");
		System.out.println("nNonterminals = " + nNonterminals);
		System.out.println("initialRuleCount = " + initialRuleCount);
		System.out.println("baseSeed = " + baseSeed);
		System.out.println("objectiveDataset = " + objectiveDataset);
		System.out.println("objectiveLimit = " + objectiveLimit);
		System.out.println("parsableDataset = " + parsableDataset);
		System.out.println("withNonCanonicalRules = " + withNonCanonicalRules);
		System.out.println("maxSteps = " + maxSteps);
		System.out.println("maxSwapCandidatesPerStep = " + maxSwapCandidatesPerStep);
		System.out.println("maxNeighborEvaluationsPerStep = " + maxNeighborEvaluationsPerStep);
		System.out.println("maxSeedAttempts = " + maxSeedAttempts);
		System.out.println("numRuns = " + numRuns);

		int poolSize = 2; // hardcoded for now
		ExecutorService executor = Executors.newFixedThreadPool(poolSize);
		List<Future<SearchState>> futures = new ArrayList<>();

		for (int r = 0; r < numRuns; r++) {
			final int runNumber = r + 1;
			final long runSeed = baseSeed + r;
			Callable<SearchState> task = () -> {
				System.out.printf("%n===== Run %d of %d (seed=%d) =====%n", runNumber, numRuns, runSeed);
				LocalSearchExplorer explorer = new LocalSearchExplorer(
						nNonterminals,
						runSeed,
						objectiveDataset,
						parsableDataset,
						withNonCanonicalRules,
						objectiveLimit);
				return explorer.runSingleRun(
						initialRuleCount,
						maxSeedAttempts,
						maxSteps,
						maxSwapCandidatesPerStep,
						maxNeighborEvaluationsPerStep,
						logSteps,
						runNumber,
						numRuns);
			};
			futures.add(executor.submit(task));
		}

		List<SearchState> runResults = new ArrayList<>();
		for (int i = 0; i < futures.size(); i++) {
			try {
				SearchState state = futures.get(i).get();
				runResults.add(state);
				System.out.printf("Run %d completed: size=%d bits/base=%.4f%n", i + 1, state.grammar.size(), state.bitsPerBase);
			} catch (ExecutionException e) {
				System.out.printf("Run %d failed: %s%n", i + 1, e.getCause().getMessage());
			}
		}
		executor.shutdown();

		SearchState best = runResults.stream()
				.min(Comparator.comparingDouble(SearchState::bitsPerBase))
				.orElse(null);

		System.out.println("\n=== Run summary ===");
		for (int i = 0; i < runResults.size(); i++) {
			SearchState s = runResults.get(i);
			System.out.printf("Run %d: size=%d bits/base=%.4f%n", i + 1, s.grammar.size(), s.bitsPerBase);
		}
		if (best != null) {
			System.out.printf("%nBest overall: size=%d bits/base=%.4f%n", best.grammar.size(), best.bitsPerBase);
		}
	}

	private SearchState runSingleRun(final int initialRuleCount,
	                                 final int maxSeedAttempts,
	                                 final int maxSteps,
	                                 final int maxSwapCandidatesPerStep,
	                                 final int maxNeighborEvaluationsPerStep,
	                                 final boolean logSteps,
	                                 final int runNumber,
	                                 final int totalRuns) {
		SearchState current = sampleParsableSeed(initialRuleCount, maxSeedAttempts);
		System.out.printf("Run %d/%d seed: size=%d bits/base=%.4f%n", runNumber, totalRuns, current.grammar.size(), current.bitsPerBase);

		for (int step = 0; step < maxSteps; step++) {
			NeighborSearchOutcome outcome = firstImprovingNeighbor(
					current,
					maxSwapCandidatesPerStep,
					maxNeighborEvaluationsPerStep);
			if (logSteps) {
				System.out.printf("run %d", runNumber);
			}
			if (!outcome.improved()) {
				if (logSteps) {
					System.out.printf("Step %d: size=%d score=%.4f | explored %d neighbors, no improvement%n",
							step, current.grammar.size(), current.bitsPerBase, outcome.evaluated());
				}
				break;
			}
			current = outcome.next();
			if (logSteps) {
				System.out.printf("Step %d: size=%d score=%.4f | explored %d neighbors (improvement at #%d) -> size=%d score=%.4f%n",
						step,
						outcome.previousGrammarSize(),
						outcome.previousBitsPerBase(),
						outcome.evaluated(),
						outcome.improvementNeighborIndex(),
						current.grammar.size(),
						current.bitsPerBase);
			}
		}

		return current;
	}

	private SearchState sampleParsableSeed(final int nRules, final int maxAttempts) {
		RandomGrammarExplorer generator = new RandomGrammarExplorer(nNonterminals);
		for (int attempt = 1; attempt <= maxAttempts; attempt++) {
			SecondaryStructureGrammar grammar = generator.randomGrammar(random, nRules);
			if (!passesDataset(grammar, parsableDatasetWords)) continue;
			if (!passesDataset(grammar, objectiveDatasetWords)) continue;
			boolean[] mask = toMask(grammar);
			double score = score(grammar);
			if (!Double.isFinite(score)) continue;
			System.out.printf("Seed candidate %d: size=%d bits/base=%.4f%n", attempt, grammar.size(), score);
			return new SearchState(mask, grammar, score);
		}
		throw new IllegalStateException("Could not find a parsable seed grammar after " + maxAttempts + " attempts");
	}

	private NeighborSearchOutcome firstImprovingNeighbor(final SearchState current,
	                                                     final int maxSwapCandidates,
	                                                     final int maxNeighborEvaluations) {
		List<Move> moves = enumerateMoves(current.ruleMask, maxSwapCandidates);
		Collections.shuffle(moves, random);
		int evaluated = 0;
		int neighborIndex = 0;
		for (Move move : moves) {
			if (evaluated >= maxNeighborEvaluations) break;
			boolean[] candidateMask = applyMove(current.ruleMask, move);
			SecondaryStructureGrammar candidateGrammar = buildGrammarIfValid(candidateMask);
			if (candidateGrammar == null) continue;
			if (!passesDataset(candidateGrammar, parsableDatasetWords)) continue;
			if (!passesDataset(candidateGrammar, objectiveDatasetWords)) continue;
			double score = score(candidateGrammar);
			evaluated++;
			neighborIndex++;
			if (score + IMPROVEMENT_EPS < current.bitsPerBase) {
				return new NeighborSearchOutcome(
						new SearchState(candidateMask, candidateGrammar, score),
						evaluated,
						neighborIndex,
						current.grammar.size(),
						current.bitsPerBase,
						true);
			}
		}
		return new NeighborSearchOutcome(null, evaluated, -1, current.grammar.size(), current.bitsPerBase, false);
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

	private boolean passesDataset(final SecondaryStructureGrammar grammar, final List<List<Terminal<Character>>> words) {
		SRFParser<Character> ssParser = new SRFParser<>(grammar);
		for (List<Terminal<Character>> word : words) {
			if (!ssParser.parsable(word)) {
				return false;
			}
		}
		return true;
	}

	private record ListBackedDataset(String name, List<RNAWithStructure> rnas) implements Dataset {

		@Override
			public int getSize() {
				return rnas.size();
			}


		@Override
			public Iterator<RNAWithStructure> iterator() {
				return rnas.iterator();
			}
		}

	private record SearchState(boolean[] ruleMask, SecondaryStructureGrammar grammar, double bitsPerBase) {
	}

	private record NeighborSearchOutcome(SearchState next,
	                                     int evaluated,
	                                     int improvementNeighborIndex,
	                                     int previousGrammarSize,
	                                     double previousBitsPerBase,
	                                     boolean improved) {
	}

	record QuickResult(double bitsPerBase, int grammarSize) {
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
