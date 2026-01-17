package compression.grammargenerator.localsearch.dataclasses;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class NeighborSearchOutcomeTest {

	/**
	 * Verifies that NeighborSearchOutcome stores the search step metadata and exposes it through accessors.
	 */
	@Test
	public void testOutcomeStoresEvaluationDetails() {
		SearchState next = new SearchState(new boolean[] { false, true }, null, 1.1);
		NeighborSearchOutcome outcome = new NeighborSearchOutcome(
				next,
				12,
				3,
				9,
				1.8,
				true
		);

		assertSame(next, outcome.getNext());
		assertEquals(12, outcome.getEvaluated());
		assertEquals(3, outcome.getImprovementNeighborIndex());
		assertEquals(9, outcome.getPreviousGrammarSize());
		assertEquals(1.8, outcome.getPreviousBitsPerBase(), 0.0001);
		assertTrue(outcome.isImproved());
	}
}
