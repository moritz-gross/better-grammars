package compression.grammargenerator.localsearch.dataclasses;

import compression.grammargenerator.localsearch.dataclasses.SearchStrategy.ImprovementTracker;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.junit.Assert.assertFalse;

public class SearchStrategyTest {

	/**
	 * Checks that FIRST_IMPROVEMENT captures the first candidate, stops early, and ignores later candidates.
	 */
	@Test
	public void testFirstImprovementStopsAfterFirstCandidate() {
		ImprovementTracker tracker = SearchStrategy.FIRST_IMPROVEMENT.newTracker();
		SearchState first = new SearchState(new boolean[] { true, false }, null, 2.0);
		SearchState second = new SearchState(new boolean[] { false, true }, null, 1.0);

		tracker.consider(first, 0);
		assertTrue(tracker.hasImprovement());
		assertTrue(tracker.shouldStop());
		assertSame(first, tracker.best());
		assertEquals(0, tracker.bestIndex());

		tracker.consider(second, 1);
		assertSame(first, tracker.best());
		assertEquals(0, tracker.bestIndex());
	}

	/**
	 * Verifies that BEST_IMPROVEMENT keeps the lowest bits-per-base candidate while never early-stopping.
	 */
	@Test
	public void testBestImprovementSelectsLowestBits() {
		ImprovementTracker tracker = SearchStrategy.BEST_IMPROVEMENT.newTracker();
		SearchState initial = new SearchState(new boolean[] { true }, null, 2.0);
		SearchState better = new SearchState(new boolean[] { false }, null, 1.5);
		SearchState worse = new SearchState(new boolean[] { true, true }, null, 1.7);

		tracker.consider(initial, 0);
		assertFalse(tracker.shouldStop());
		assertSame(initial, tracker.best());
		assertEquals(0, tracker.bestIndex());

		tracker.consider(better, 1);
		assertSame(better, tracker.best());
		assertEquals(1, tracker.bestIndex());

		tracker.consider(worse, 2);
		assertSame(better, tracker.best());
		assertEquals(1, tracker.bestIndex());
		assertFalse(tracker.shouldStop());
	}
}
