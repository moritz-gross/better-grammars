package compression.grammargenerator.localsearch;

/**
 * Helper utilities for formatting log output for local search runs.
 */
final class Logging {
	private static final String[] RUN_COLORS = {
			"\u001B[34m", // blue
			"\u001B[32m", // green
			"\u001B[36m", // cyan
			"\u001B[35m", // magenta
			"\u001B[33m"  // yellow
	};
	private static final String ANSI_RESET = "\u001B[0m";

	private Logging() {
		// utility
	}

	static String runLabel(int runNumber) {
		String base = "Run " + runNumber;
		return colorForRun(runNumber) + base + ANSI_RESET;
	}

	private static String colorForRun(int runNumber) {
		return RUN_COLORS[(runNumber - 1) % RUN_COLORS.length];
	}
}
