package compression.grammargenerator.localsearch.dataclasses;

import org.junit.Test;

import static org.junit.Assert.*;

public class ConfigTest {

	/**
	 * Ensures withStrategy returns a new config instance with the updated strategy and leaves the original intact.
	 */
	@Test
	public void testWithStrategyProducesNewConfig() {
		Config original = Config.defaults();
		Config updated = original.withStrategy(SearchStrategy.BEST_IMPROVEMENT);

		assertNotSame(original, updated);
		assertEquals(SearchStrategy.FIRST_OR_STOCHASTIC_IMPROVEMENT, original.searchStrategy());
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
