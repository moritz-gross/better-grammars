package compression.grammargenerator.localsearch.dataclasses;

/**
 * Outcome of evaluating neighbors for a single step.
 */
public record NeighborSearchOutcome(SearchState next,
                                    int evaluated,
                                    int improvementNeighborIndex,
                                    int previousGrammarSize,
                                    double previousBitsPerBase,
                                    boolean improved) {
}
