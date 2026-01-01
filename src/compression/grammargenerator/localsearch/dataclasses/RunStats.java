package compression.grammargenerator.localsearch.dataclasses;

/**
 * Summary statistics for a single run.
 */
public record RunStats(int runNumber,
                       long seed,
                       int stepsTaken,
                       int totalNeighborsEvaluated,
                       int bestSize,
                       double bestBitsPerBase) {
}
