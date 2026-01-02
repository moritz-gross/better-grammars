package compression.grammargenerator.localsearch.dataclasses;

import lombok.AllArgsConstructor;
import lombok.Value;

/**
 * Outcome of evaluating neighbors for a single step.
 */
@Value
@AllArgsConstructor
public class NeighborSearchOutcome {
	SearchState next;
	int evaluated;
	int improvementNeighborIndex;
	int previousGrammarSize;
	double previousBitsPerBase;
	boolean improved;
}
