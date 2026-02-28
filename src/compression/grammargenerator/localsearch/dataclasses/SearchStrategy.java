package compression.grammargenerator.localsearch.dataclasses;

import java.util.*;

/**
 * Neighbor selection strategy for local search.
 */
public enum SearchStrategy {
    FIRST_IMPROVEMENT {
        @Override
        public ImprovementTracker newTracker() {
            return new FirstImprovementTracker();
        }
    },
    BEST_IMPROVEMENT {
        @Override
        public ImprovementTracker newTracker() {
            return new BestImprovementTracker();
        }
    },
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

            sortedNeighbours.sort(Comparator.comparing(SearchState::getBitsPerBase));

            //better improvements are more likely than weaker improvements:
            for(int i = 0; i < sortedNeighbours.size(); i++){
                double randomNumber = random.nextDouble();
                if(randomNumber >= 0.5){
                    return new NeighborSearchOutcome(sortedNeighbours.get(i), -1, indexes.get(sortedNeighbours.get(i)), -1, -1, false);
                }
            }
            return new NeighborSearchOutcome(sortedNeighbours.getLast(), -1, indexes.get(sortedNeighbours.getLast()), -1, -1, false);
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
}