package compression.grammargenerator.localsearch.dataclasses;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class RunResultTest {

	/**
	 * Ensures RunStats exposes the constructor-provided summary fields verbatim.
	 */
	@Test
	public void testRunStatsHoldsValues() {
		RunStats stats = new RunStats(
				2,
				123L,
				10,
				250,
				42,
				1.23
		);

		assertEquals(2, stats.getRunNumber());
		assertEquals(123L, stats.getSeed());
		assertEquals(10, stats.getStepsTaken());
		assertEquals(250, stats.getTotalNeighborsEvaluated());
		assertEquals(42, stats.getBestSize());
		assertEquals(1.23, stats.getBestBitsPerBase(), 0.0001);
	}

	/**
	 * Confirms RunResult ties together the best SearchState and its associated RunStats.
	 */
	@Test
	public void testRunResultHoldsBestAndStats() {
		SearchState best = new SearchState(new boolean[] { true, false, true }, null, 0.75);
		RunStats stats = new RunStats(
				1,
				42L,
				5,
				100,
				12,
				0.75
		);
		RunResult result = new RunResult(best, stats);

		assertSame(best, result.getBest());
		assertSame(stats, result.getStats());
	}
}
