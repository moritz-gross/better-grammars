package compression.grammargenerator.localsearch.dataclasses;

import lombok.Value;

/**
 * Best state and stats for a completed run.
 */
@Value
public class RunResult {
	SearchState best;
	RunStats stats;
}
