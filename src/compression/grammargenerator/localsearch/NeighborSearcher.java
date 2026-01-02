package compression.grammargenerator.localsearch;

import compression.RuleProbType;
import compression.data.Dataset;
import compression.grammar.NonTerminal;
import compression.grammar.Rule;
import compression.grammar.SecondaryStructureGrammar;
import compression.grammar.Terminal;
import compression.grammargenerator.localsearch.dataclasses.NeighborSearchOutcome;
import compression.grammargenerator.localsearch.dataclasses.SearchState;
import compression.grammargenerator.localsearch.dataclasses.SearchStrategy;
import compression.parser.SRFParser;
import compression.util.MyMultimap;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Encapsulates neighbor generation and selection for local search.
 */
@RequiredArgsConstructor
final class NeighborSearcher {
	private static final double IMPROVEMENT_EPS = 1e-3; // at least 1/1000

	private final Rule[] allPossibleRules;
	private final NonTerminal startSymbol;
	private final List<List<Terminal<Character>>> parsableDatasetWords;
	private final List<List<Terminal<Character>>> objectiveDatasetWords;
	private final Dataset objectiveDatasetLimited;
	private final boolean withNonCanonicalRules;
	private final Random random;

	NeighborSearchOutcome search(final SearchState current,
	                             final int maxSwapCandidates,
	                             final int maxNeighborEvaluations,
	                             final int maxCandidatesPerStep,
	                             final SearchStrategy strategy) {
		List<Move> moves = enumerateMoves(current.getRuleMask(), maxSwapCandidates);
		Collections.shuffle(moves, random);
		int evaluated = 0;
		int neighborIndex = 0;
		int considered = 0;
		SearchState bestImprovement = null;
		int bestImprovementIndex = -1;
		for (Move move : moves) {
			if (maxCandidatesPerStep >= 0 && considered >= maxCandidatesPerStep) break;
			if (evaluated >= maxNeighborEvaluations) break;
			considered++;
			boolean[] candidateMask = applyMove(current.getRuleMask(), move);
			SecondaryStructureGrammar candidateGrammar = buildGrammarIfValid(candidateMask);
			if (candidateGrammar == null) continue;
			SRFParser<Character> parser = new SRFParser<>(candidateGrammar);
			if (!passesDataset(parser, parsableDatasetWords)) continue;
			if (!passesDataset(parser, objectiveDatasetWords)) continue;
			double score = LocalSearchExplorer.getBitsPerBase(objectiveDatasetLimited, RuleProbType.ADAPTIVE, candidateGrammar, withNonCanonicalRules);
			evaluated++;
			neighborIndex++;
			if (score + IMPROVEMENT_EPS < current.getBitsPerBase()) {
				SearchState candidateState = new SearchState(candidateMask, candidateGrammar, score);
				if (strategy == SearchStrategy.FIRST_IMPROVEMENT) {
					return new NeighborSearchOutcome(
							candidateState,
							evaluated,
							neighborIndex,
							current.getGrammar().size(),
							current.getBitsPerBase(),
							true);
				}
				if (bestImprovement == null || score < bestImprovement.getBitsPerBase()) {
					bestImprovement = candidateState;
					bestImprovementIndex = neighborIndex;
				}
			}
		}
		if (bestImprovement != null) {
			return new NeighborSearchOutcome(
					bestImprovement,
					evaluated,
					bestImprovementIndex,
					current.getGrammar().size(),
					current.getBitsPerBase(),
					true);
		}
		return new NeighborSearchOutcome(null, evaluated, -1, current.getGrammar().size(), current.getBitsPerBase(), false);
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
			return new SecondaryStructureGrammar(name, startSymbol, rules);
		} catch (IllegalArgumentException e) {
			return null; // invalid grammar
		}
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
