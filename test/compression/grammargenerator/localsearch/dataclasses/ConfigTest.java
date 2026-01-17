package compression.grammargenerator.localsearch.dataclasses;

import org.junit.Test;

import static org.junit.Assert.*;

public class ConfigTest {

	/**
	 * Verifies that the defaults() factory exposes the documented baseline configuration values.
	 * <p>
	 * (not very interesting)
	 */
	@Test
	public void testDefaultsExposeExpectedValues() {
		Config config = Config.defaults();

		assertEquals(3, config.nNonterminals());
		assertEquals(20, config.initialRuleCount());
		assertEquals(42L, config.baseSeed());
		assertEquals(25, config.maxSteps());
		assertEquals(100, config.maxSwapCandidatesPerStep());
		assertEquals(150, config.maxNeighborEvaluationsPerStep());
		assertEquals(100, config.maxCandidatesPerStep());
		assertEquals(2000, config.maxSeedAttempts());
		assertFalse(config.withNonCanonicalRules());
		assertEquals(-1, config.objectiveLimit());
		assertEquals(3, config.numRuns());
		assertEquals(SearchStrategy.FIRST_IMPROVEMENT, config.searchStrategy());
		assertEquals("small-dataset", config.objectiveDatasetName());
		assertEquals("minimal-parsable", config.parsableDatasetName());
		assertEquals(3, config.poolSize());
	}

	/**
	 * Ensures withStrategy returns a new config instance with the updated strategy and leaves the original intact.
	 */
	@Test
	public void testWithStrategyProducesNewConfig() {
		Config original = Config.defaults();
		Config updated = original.withStrategy(SearchStrategy.BEST_IMPROVEMENT);

		assertNotSame(original, updated);
		assertEquals(SearchStrategy.FIRST_IMPROVEMENT, original.searchStrategy());
		assertEquals(SearchStrategy.BEST_IMPROVEMENT, updated.searchStrategy());
	}

	/**
	 * Confirms withMaxCandidatesPerStep updates only the candidate limit and preserves unrelated fields.
	 */
	@Test
	public void testWithMaxCandidatesPerStepUpdatesOnlyValue() {
		Config original = Config.defaults();
		Config updated = original.withMaxCandidatesPerStep(500);

		assertNotSame(original, updated);
		assertEquals(100, original.maxCandidatesPerStep());
		assertEquals(500, updated.maxCandidatesPerStep());
		assertEquals(original.searchStrategy(), updated.searchStrategy());
	}
}
