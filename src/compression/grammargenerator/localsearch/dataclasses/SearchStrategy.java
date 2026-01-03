package compression.grammargenerator.localsearch.dataclasses;

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
	};

	public abstract ImprovementTracker newTracker();

	public abstract static class ImprovementTracker {
		private SearchState best;
		private int bestIndex = -1;

		public final void consider(SearchState candidate, int neighborIndex) {
			if (accept(candidate)) {
				best = candidate;
				bestIndex = neighborIndex;
			}
		}

		protected abstract boolean accept(SearchState candidate);

		public abstract boolean shouldStop();

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
		public boolean shouldStop() {
			return false;
		}
	}
}
