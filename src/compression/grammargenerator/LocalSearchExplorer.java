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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Iterator;

/**
 * Simple hill-climbing local search over grammars:
 * start from a (random) parsable grammar, evaluate bits/base on the objective dataset,
 * then repeatedly take the first improving neighbor (add / remove / swap one rule)
 * until no improving move is found.
 *
 * Hyperparameters are configured inside {@link #main(String[])} for now so the
 * search can be launched directly from an IDE.
 */
public class LocalSearchExplorer extends AbstractGrammarExplorer {
	private final Random random;
	private final Dataset objectiveDataset;
	private final Dataset objectiveDatasetLimited;
	private final List<List<Terminal<Character>>> parsableDatasetWords;
	private final List<List<Terminal<Character>>> objectiveDatasetWords;
	private final List<RNAWithStructure> objectiveRnasLimited;
	private final boolean withNonCanonicalRules;
	private final Map<Rule, Integer> ruleToIndex;

	private static final double IMPROVEMENT_EPS = 1e-9;

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
		this.objectiveRnasLimited = Collections.unmodifiableList(rnas);
		this.objectiveDatasetLimited = new ListBackedDataset(objectiveDataset.getName() + "-limited", objectiveRnasLimited);

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
		final int nNonterminals = 6;
		final int initialRuleCount = 25;             // start large, hill-climb will remove/swap/add rules
		final long seed = 42424242L;
		final int maxIterations = 100;               // stop if no improving neighbor earlier
		final int maxSwapCandidatesPerIter = 500;    // sample this many random swaps per iteration
		final int maxNeighborEvaluationsPerIter = 800;
		final int maxSeedAttempts = 200;             // retries to find a parsable seed
		final boolean withNonCanonicalRules = false; // user preference
		final int objectiveLimit = 25;               // limit objective dataset to first N RNAs (0 = full)

		final Dataset objectiveDataset = new CachedDataset(new FolderBasedDataset("dowell-benchmark-10-percent"));
		final Dataset parsableDataset = new CachedDataset(new FolderBasedDataset("minimal-parsable"));

		LocalSearchExplorer explorer = new LocalSearchExplorer(
				nNonterminals,
				seed,
				objectiveDataset,
				parsableDataset,
				withNonCanonicalRules,
				objectiveLimit);

		System.out.println("=== Local search configuration ===");
		System.out.println("nNonterminals = " + nNonterminals);
		System.out.println("initialRuleCount = " + initialRuleCount);
		System.out.println("seed = " + seed);
		System.out.println("objectiveDataset = " + objectiveDataset);
		System.out.println("objectiveLimit = " + objectiveLimit);
		System.out.println("parsableDataset = " + parsableDataset);
		System.out.println("withNonCanonicalRules = " + withNonCanonicalRules);
		System.out.println("maxIterations = " + maxIterations);
		System.out.println("maxSwapCandidatesPerIter = " + maxSwapCandidatesPerIter);
		System.out.println("maxNeighborEvaluationsPerIter = " + maxNeighborEvaluationsPerIter);

		SearchState current = explorer.sampleParsableSeed(initialRuleCount, maxSeedAttempts);
		System.out.printf("Seed grammar size=%d bits/base=%.4f%n", current.grammar.size(), current.bitsPerBase);

		for (int iter = 0; iter < maxIterations; iter++) {
			System.out.printf("%nIteration %d: size=%d score=%.6f%n", iter, current.grammar.size(), current.bitsPerBase);
			SearchState next = explorer.firstImprovingNeighbor(
					current,
					maxSwapCandidatesPerIter,
					maxNeighborEvaluationsPerIter);
			if (next == null) {
				System.out.println("No improving neighbor found. Terminating.");
				break;
			}
			current = next;
			System.out.printf(" -> accepted move: size=%d score=%.6f%n", current.grammar.size(), current.bitsPerBase);
		}

		System.out.println("\n=== Best grammar found ===");
		System.out.println("Size = " + current.grammar.size());
		System.out.println("Bits/base = " + current.bitsPerBase);
		System.out.println(current.grammar);
	}

	private SearchState sampleParsableSeed(final int nRules, final int maxAttempts) {
		RandomGrammarExplorer generator = new RandomGrammarExplorer(nNonterminals);
		for (int attempt = 1; attempt <= maxAttempts; attempt++) {
			SecondaryStructureGrammar grammar = generator.randomGrammar(random, nRules);
			if (!passesParsable(grammar)) continue;
			if (!passesDataset(grammar, objectiveDatasetWords)) continue;
			boolean[] mask = toMask(grammar);
			double score = score(grammar);
			System.out.printf("Seed candidate %d: size=%d bits/base=%.6f%n", attempt, grammar.size(), score);
			return new SearchState(mask, grammar, score);
		}
		throw new IllegalStateException("Could not find a parsable seed grammar after " + maxAttempts + " attempts");
	}

	private SearchState firstImprovingNeighbor(final SearchState current,
	                                           final int maxSwapCandidates,
	                                           final int maxNeighborEvaluations) {
		List<Move> moves = enumerateMoves(current.ruleMask, maxSwapCandidates);
		Collections.shuffle(moves, random);
		int evaluated = 0;
		for (Move move : moves) {
			if (evaluated >= maxNeighborEvaluations) break;
			boolean[] candidateMask = applyMove(current.ruleMask, move);
			SecondaryStructureGrammar candidateGrammar = buildGrammarIfValid(candidateMask);
			if (candidateGrammar == null) continue;
			if (!passesParsable(candidateGrammar)) continue;
			if (!passesDataset(candidateGrammar, objectiveDatasetWords)) continue;
			double score = score(candidateGrammar);
			evaluated++;
			if (score + IMPROVEMENT_EPS < current.bitsPerBase) {
				return new SearchState(candidateMask, candidateGrammar, score);
			}
		}
		return null;
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

	private boolean passesParsable(final SecondaryStructureGrammar grammar) {
		return passesDataset(grammar, parsableDatasetWords);
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

	private static class ListBackedDataset implements Dataset {
		private final String name;
		private final List<RNAWithStructure> rnas;

		ListBackedDataset(final String name, final List<RNAWithStructure> rnas) {
			this.name = name;
			this.rnas = rnas;
		}

		@Override
		public int getSize() {
			return rnas.size();
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public Iterator<RNAWithStructure> iterator() {
			return rnas.iterator();
		}
	}

	private static class SearchState {
		final boolean[] ruleMask;
		final SecondaryStructureGrammar grammar;
		final double bitsPerBase;

		SearchState(final boolean[] ruleMask, final SecondaryStructureGrammar grammar, final double bitsPerBase) {
			this.ruleMask = ruleMask;
			this.grammar = grammar;
			this.bitsPerBase = bitsPerBase;
		}
	}

	private static class Move {
		enum Type {ADD, REMOVE, SWAP}

		final Type type;
		final int source;
		final int target;

		private Move(final Type type, final int source, final int target) {
			this.type = type;
			this.source = source;
			this.target = target;
		}

		static Move add(final int target) {
			return new Move(Type.ADD, -1, target);
		}

		static Move remove(final int target) {
			return new Move(Type.REMOVE, target, -1);
		}

		static Move swap(final int source, final int target) {
			return new Move(Type.SWAP, source, target);
		}
	}
}
