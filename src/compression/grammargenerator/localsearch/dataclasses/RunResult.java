package compression.grammargenerator.localsearch.dataclasses;

/**
 * Best state and stats for a completed run.
 */
public record RunResult(SearchState best, RunStats stats) {
}
