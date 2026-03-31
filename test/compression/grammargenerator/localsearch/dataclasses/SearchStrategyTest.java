package compression.grammargenerator.localsearch.dataclasses;

import compression.grammargenerator.localsearch.dataclasses.SearchStrategy.ImprovementTracker;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

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

		tracker.consider(first, 0, 3.0);
		assertTrue(tracker.hasImprovement());
		assertTrue(tracker.shouldStop());
		assertSame(first, tracker.best());
		assertEquals(0, tracker.bestIndex());

		tracker.consider(second, 1, 2.0);
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

		tracker.consider(initial, 0, 3.0);
		assertFalse(tracker.shouldStop());
		assertSame(initial, tracker.best());
		assertEquals(0, tracker.bestIndex());

		tracker.consider(better, 1, 2.0);
		assertSame(better, tracker.best());
		assertEquals(1, tracker.bestIndex());

		tracker.consider(worse, 2, 1.5);
		assertSame(better, tracker.best());
		assertEquals(1, tracker.bestIndex());
		assertFalse(tracker.shouldStop());
	}

    /**
     * Verifies that STOCHASTIC_IMPROVEMENT never stops early.
     */
    @Test
    public void testStochasticImprovementNotStoppingEarly() {
        ImprovementTracker tracker = SearchStrategy.STOCHASTIC_IMPROVEMENT.newTracker();
        SearchState initial = new SearchState(new boolean[] { true }, null, 2.0);
        SearchState better = new SearchState(new boolean[] { false }, null, 1.5);
        SearchState worse = new SearchState(new boolean[] { true, true }, null, 1.7);

        tracker.consider(initial, 0, 3.0);
        assertFalse(tracker.shouldStop());

        tracker.consider(better, 1, 2.0);
        assertFalse(tracker.shouldStop());

        tracker.consider(worse, 2, 1.5);
        assertFalse(tracker.shouldStop());
    }

    /**
     * Checks that FIRST_OR_STOCHASTIC_IMPROVEMENT captures the first candidate with lower BitsPerBase, stops early, and ignores later candidates.
     */
    @Test
    public void testFirstOrStochasticImprovementStopsAfterFirstCandidate() {
        ImprovementTracker tracker = SearchStrategy.FIRST_OR_STOCHASTIC_IMPROVEMENT.newTracker();
        SearchState first = new SearchState(new boolean[] { true, false }, null, 2.0);

        tracker.consider(first, 0, 3.0);
        assertTrue(tracker.hasImprovement());
        assertTrue(tracker.shouldStop());
        assertSame(first, tracker.best());
        assertEquals(0, tracker.bestIndex());
    }

    /**
     * Checks that FIRST_OR_STOCHASTIC_IMPROVEMENT creates list if no grammar that is better than the current one is reachable.
     */
    @Test
    public void testFirstOrStochasticImprovementUsesList(){
        ImprovementTracker tracker = SearchStrategy.FIRST_OR_STOCHASTIC_IMPROVEMENT.newTracker();
        SearchState first = new SearchState(new boolean[] { true }, null, 3.0);
        SearchState second = new SearchState(new boolean[] { false }, null, 2.5);
        SearchState third = new SearchState(new boolean[] { true, true }, null, 3.5);

        tracker.consider(first, 0, 2.0);
        assertFalse(tracker.shouldStop());

        tracker.consider(second, 1, 2.0);
        assertFalse(tracker.shouldStop());

        tracker.consider(third, 2, 2.0);
        assertFalse(tracker.shouldStop());

        tracker.getStochasticImprovement(new Random());

        List<SearchState> neighborList = new ArrayList<>();
        neighborList.add(first);
        neighborList.add(second);
        neighborList.add(third);
        neighborList.sort(Comparator.comparing(SearchState::getBitsPerBase));

        assertEquals(neighborList, tracker.sortedNeighbours);

    }
}
