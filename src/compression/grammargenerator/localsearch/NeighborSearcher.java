package compression.grammargenerator.localsearch;

import compression.grammar.SecondaryStructureGrammar;
import compression.grammar.Terminal;
import compression.grammargenerator.localsearch.dataclasses.NeighborSearchOutcome;
import compression.grammargenerator.localsearch.dataclasses.SearchState;
import compression.grammargenerator.localsearch.dataclasses.SearchStrategy;
import compression.grammargenerator.localsearch.dataclasses.SearchStrategy.ImprovementTracker;
import compression.parser.SRFParser;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Generates and evaluates one-step neighbors for a {@link SearchState}.
 *
 * <p>A step starts from the current rule mask and builds candidate moves by removing one present
 * rule, adding one absent rule, and sampling swap moves between present and absent rules. The
 * resulting move list is then reordered with a biased shuffle before exploration.
 *
 * <p>Each candidate is processed in this order:
 *
 * <ol>
 *   <li>apply the move to the rule mask</li>
 *   <li>rebuild the grammar and reject invalid masks</li>
 *   <li>reject grammars that fail the parsable dataset</li>
 *   <li>reject grammars that fail the objective dataset</li>
 *   <li>score the remaining grammar and pass it to the configured {@link SearchStrategy}</li>
 * </ol>
 *
 * <p>{@code maxCandidatesPerStep} limits how many moves are considered from the reordered move
 * list. {@code maxNeighborEvaluationsPerStep} separately limits how many valid candidates are
 * actually scored, so the two counters can diverge.
 */
@RequiredArgsConstructor
final class NeighborSearcher {
	private final RuleMaskCodec ruleMaskCodec;
	private final List<List<Terminal<Character>>> parsableDatasetWords;
	private final List<List<Terminal<Character>>> objectiveDatasetWords;
	private final ScoreEvaluator scoreEvaluator;
	private final Random random;

	/**
	 * Explores the neighborhood around {@code current} according to the supplied limits and
	 * strategy, returning the chosen next state for this step if one exists.
	 */
	NeighborSearchOutcome search(final SearchState current,
	                             final int maxSwapCandidates,
	                             final int maxNeighborEvaluations,
	                             final int maxCandidatesPerStep,
	                             final SearchStrategy strategy,
	                             final Random rng) {
		List<Move> moves = enumerateMoves(current.getRuleMask(), maxSwapCandidates);
		//Collections.shuffle(moves, random);
        moves = rebalancedShuffle(moves);
		int evaluated = 0;
		int neighborIndex = 0;
		int considered = 0;
		ImprovementTracker tracker = strategy.newTracker();
		for (Move move : moves) {
			if (maxCandidatesPerStep >= 0 && considered >= maxCandidatesPerStep) break;
			if (evaluated >= maxNeighborEvaluations) break;
			considered++;
			boolean[] candidateMask = applyMove(current.getRuleMask(), move);

			SecondaryStructureGrammar candidateGrammar = ruleMaskCodec.buildGrammarIfValid(candidateMask);
			if (candidateGrammar == null) continue;
			SRFParser<Character> parser = new SRFParser<>(candidateGrammar);
			if (!Utils.passesDataset(parser, parsableDatasetWords)) continue;
			if (!Utils.passesDataset(parser, objectiveDatasetWords)) continue;
			double score = scoreEvaluator.score(candidateGrammar);
			evaluated++;
			neighborIndex++;
            double currentScore = current.getBitsPerBase();
			if (score < currentScore) {
				SearchState candidateState = new SearchState(candidateMask, candidateGrammar, score);
				tracker.consider(candidateState, neighborIndex, currentScore);
				if (tracker.shouldStop()) {
					return new NeighborSearchOutcome(
							tracker.best(),
							evaluated,
							tracker.bestIndex(),
							current.getGrammar().size(),
							current.getBitsPerBase(),
							true);
				}
			}
            else{
                if(tracker.acceptsWorsening()){
                    SearchState candidateState = new SearchState(candidateMask, candidateGrammar, score);
                    tracker.consider(candidateState, neighborIndex, currentScore);
                    if (tracker.shouldStop()) { //shouldStop in this case not needed at the moment, but maybe in the future
                        return new NeighborSearchOutcome(
                                tracker.best(),
                                evaluated,
                                tracker.bestIndex(),
                                current.getGrammar().size(),
                                current.getBitsPerBase(),
                                true);
                    }
                }
            }

		}

        if(tracker.isStochastic()){
            rng.nextDouble();
            NeighborSearchOutcome nso = tracker.getStochasticImprovement(rng);
            return new NeighborSearchOutcome(
                    nso.getNext(),
                    evaluated,
                    nso.getImprovementNeighborIndex(),
                    current.getGrammar().size(),
                    current.getBitsPerBase(),
                    nso.isImproved());
        }
		if (tracker.hasImprovement()) {
			return new NeighborSearchOutcome(
					tracker.best(),
					evaluated,
					tracker.bestIndex(),
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
        //    System.out.println("remove at: " + idx);
		}
		for (Integer idx : absent) {
			moves.add(Move.add(idx));
         //   System.out.println("add at: " + idx);
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
               //     System.out.println("swap from: " + from + " -> " + to);
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
    /**
     * @return a semi random sorted List of moves with a stochastic bias.
     * important! not all elements are sorted with such a bias.
     * The first part of the list is sorted with the bias.
     * the second part of the list is filled with the moves that were not selected, since we do not want to loose moves
     * here. (We obviously hope that the second part is almost never reached)
     */
    private List<Move> rebalancedShuffle(List<Move> moves){
        List <Move> shuffledMoves = new ArrayList<>();
        List <Move> addMoves = new ArrayList<>();
        List <Move> removeMoves = new ArrayList<>();
        List <Move> swapMoves = new ArrayList<>();

        for(Move move : moves){
            if(move.type == Move.Type.ADD){
                addMoves.add(move);
            } else if(move.type == Move.Type.REMOVE){
                removeMoves.add(move);
            } else{
                swapMoves.add(move);
            }
        }
        addMoves = shuffleMoves(addMoves);
        removeMoves = shuffleMoves(removeMoves);
        swapMoves = shuffleMoves(swapMoves);

        int addIndex = 0;
        int removeIndex = 0;
        int swapIndex = 0;
        for(int i = 0; i < moves.size(); i++){
            double probability = random.nextDouble(1);
            if(probability < 0.2){
                if(removeMoves.size() > removeIndex){
                    shuffledMoves.add(removeMoves.get(removeIndex));
                    removeIndex++;
                }

            } else if(probability < 0.6){
                if(addMoves.size() > addIndex){
                    shuffledMoves.add(addMoves.get(addIndex));
                    addIndex++;
                }
            } else{
                if(swapMoves.size() > swapIndex){
                    shuffledMoves.add(swapMoves.get(swapIndex));
                    swapIndex++;
                }
            }
        }
        //adds the not used possible moves just in case the semi random sorted/prioritized moves were unsuccessful
        while(addMoves.size() > addIndex){
            shuffledMoves.add(addMoves.get(addIndex));
            addIndex++;
        }
        while(swapMoves.size() > swapIndex){
            shuffledMoves.add(swapMoves.get(swapIndex));
            swapIndex++;
        }
        while(removeMoves.size() > removeIndex){
            shuffledMoves.add(removeMoves.get(removeIndex));
            removeIndex++;
        }
        return shuffledMoves;
    }
    private List<Move> shuffleMoves(List<Move> moves){
        int initialMovesSize = moves.size();
        List <Move> shuffledMoves = new ArrayList<>();
        for(int i = 0; i < initialMovesSize; i++){
            int index =(int) (random.nextDouble(1) * moves.size());
            shuffledMoves.add(moves.get(index));
            moves.remove(index);
        }
        return shuffledMoves;
    }
}
