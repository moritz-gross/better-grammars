package compression.grammargenerator.localsearch.dataclasses;

import compression.grammargenerator.localsearch.Logging;

import java.util.*;

/**
 * Neighbor selection strategy for local search.
 */
public enum SearchStrategy {

    /**
     * The first neighboring grammar found that is better than the current grammar is selected
     */
    FIRST_IMPROVEMENT {
        @Override
        public ImprovementTracker newTracker() {
            return new FirstImprovementTracker();
        }
    },

    /**
     * All neighboring grammars or as much as allowed by the config that are an improvement, are considered and the best one will be selected
     */
    BEST_IMPROVEMENT {
        @Override
        public ImprovementTracker newTracker() {
            return new BestImprovementTracker();
        }
    },

    /**
     * All neighboring grammars or as much as allowed by the config that are an improvement, are saved in a list.
     * The list is sorted with the best grammar at the front. In the list the n-th element (starting with 1) will be
     * selected with the likeliness 1/2^n
     */
    STOCHASTIC_IMPROVEMENT {
        public ImprovementTracker newTracker() {
            return new StochasticImprovementTracker();
        }
    }
    ;

    public abstract ImprovementTracker newTracker();

    public abstract static class ImprovementTracker {
        private SearchState best;
        private int bestIndex = -1;
        protected final List<SearchState> sortedNeighbours;
        protected final HashMap<SearchState, Integer> indexes;

        public ImprovementTracker(){
            sortedNeighbours  = new ArrayList<>();
            indexes = new HashMap<>();
        }

        public final void consider(SearchState candidate, int neighborIndex) {
            if (accept(candidate)) {
                best = candidate;
                bestIndex = neighborIndex;
            }
            if (stochastic()) {
                sortedNeighbours.add(candidate);
                indexes.put(candidate, neighborIndex);
            }
        }

        protected abstract boolean accept(SearchState candidate);

        public abstract boolean stochastic();

        public abstract boolean shouldStop();

        public abstract NeighborSearchOutcome getStochasticImprovement(Random random);

        public boolean hasImprovement() {
            return best != null;
        }

        public SearchState best() {
            return best;
        }

        public int bestIndex() {
            return bestIndex;
        }
    }

    private static final class FirstImprovementTracker extends ImprovementTracker {
        @Override
        protected boolean accept(SearchState candidate) {
            return !hasImprovement();
        }

        @Override
        public boolean stochastic() {
            return false;
        }

        public NeighborSearchOutcome getStochasticImprovement(Random random){
            return null;
        }

            @Override
        public boolean shouldStop() {
            return hasImprovement();
        }
    }

    private static final class BestImprovementTracker extends ImprovementTracker {
        @Override
        protected boolean accept(SearchState candidate) {
            return !hasImprovement() || candidate.getBitsPerBase() < best().getBitsPerBase();
        }

        @Override
        public boolean stochastic() {
            return false;
        }
        public NeighborSearchOutcome getStochasticImprovement(Random random){
            return null;
        }

            @Override
        public boolean shouldStop() {
            return false;
        }
    }

    private static final class StochasticImprovementTracker extends ImprovementTracker {

        @Override
        protected boolean accept(SearchState candidate) {
            return false;
        }

        @Override
        public boolean stochastic() {
            return true;
        }

        @Override
        public boolean shouldStop() {
            return false;
        }

        public NeighborSearchOutcome getStochasticImprovement(Random random){

            if(sortedNeighbours.isEmpty()){
                return new NeighborSearchOutcome(null, -1, -1, -1, -1, false);
            }

            sortedNeighbours.sort(Comparator.comparing(SearchState::getBitsPerBase));

            for(int i = 0; i < sortedNeighbours.size(); i++){
                double randomNumber = random.nextDouble();
                if(randomNumber >= 0.5){
                    return new NeighborSearchOutcome(sortedNeighbours.get(i), -1, indexes.get(sortedNeighbours.get(i)), -1, -1, true);
                }
            }
            return new NeighborSearchOutcome(sortedNeighbours.getLast(), -1, indexes.get(sortedNeighbours.getLast()), -1, -1, true);
        }
    }
}