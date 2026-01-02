package compression.grammargenerator.localsearch.dataclasses;

import lombok.Value;

/**
 * Summary statistics for a single run.
 */
@Value
public class RunStats {
	int runNumber;
	long seed;
	int stepsTaken;
	int totalNeighborsEvaluated;
	int bestSize;
	double bestBitsPerBase;
}
