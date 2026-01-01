package compression.grammargenerator.localsearch;

import compression.data.Dataset;
import compression.grammargenerator.localsearch.LocalSearchExplorer.RunResult;
import compression.grammargenerator.localsearch.LocalSearchExplorer.RunStats;

import static java.lang.System.out;

/**
 * Helper utilities for formatting log output for local search runs.
 */
public final class Logging {
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

	public static void printRunStart(int runNumber, int totalRuns, long seed) {
		out.printf("%n===== starting %s of %d (seed=%d) =====%n", runLabel(runNumber), totalRuns, seed);
	}

	public static void printSeed(int runNumber, int size, double bitsPerBase) {
		out.printf("%s seed: size=%d bits/base=%.4f%n", runLabel(runNumber), size, bitsPerBase);
	}

	public static void printStepNoImprovement(int runNumber, int step, int size, double score, int explored) {
		out.printf("%s step %d: size=%d score=%.4f | explored %d neighbors, no improvement%n",
				runLabel(runNumber), step, size, score, explored);
	}

	public static void printStepImprovement(int runNumber,
	                                        int step,
	                                        int previousSize,
	                                        double previousScore,
	                                        int explored,
	                                        int improvementIndex,
	                                        int newSize,
	                                        double newScore) {
		out.printf("%s step %d: size=%d score=%.4f | explored %d neighbors (improvement at #%d) -> size=%d score=%.4f%n",
				runLabel(runNumber),
				step,
				previousSize,
				previousScore,
				explored,
				improvementIndex,
				newSize,
				newScore);
	}

	public static void printRunCompleted(RunStats stats) {
		out.printf("%s completed: size=%d bits/base=%.4f steps=%d neighbors=%d%n",
				runLabel(stats.runNumber()),
				stats.bestSize(),
				stats.bestBitsPerBase(),
				stats.stepsTaken(),
				stats.totalNeighborsEvaluated());
	}

	public static void printSummaryLine(RunStats stats) {
		out.printf("%s | seed=%d | steps=%d | bits/base=%.4f | size=%d | neighbors=%d%n",
				runLabel(stats.runNumber()),
				stats.seed(),
				stats.stepsTaken(),
				stats.bestBitsPerBase(),
				stats.bestSize(),
				stats.totalNeighborsEvaluated());
	}

	public static void printBestOverall(RunResult best) {
		out.printf("%nBest overall: %s seed=%d size=%d bits/base=%.4f%n",
				runLabel(best.stats().runNumber()),
				best.stats().seed(),
				best.best().grammar().size(),
				best.best().bitsPerBase());
	}

	public static void printConfig(Config config, Dataset objectiveDataset, Dataset parsableDataset) {
		out.println("=== Local search configuration ===");
		out.println("nNonterminals = " + config.nNonterminals());
		out.println("initialRuleCount = " + config.initialRuleCount());
		out.println("baseSeed = " + config.baseSeed());
		out.println("objectiveDataset = " + objectiveDataset);
		out.println("objectiveLimit = " + config.objectiveLimit());
		out.println("parsableDataset = " + parsableDataset);
		out.println("withNonCanonicalRules = " + config.withNonCanonicalRules());
		out.println("maxSteps = " + config.maxSteps());
		out.println("maxSwapCandidatesPerStep = " + config.maxSwapCandidatesPerStep());
		out.println("maxNeighborEvaluationsPerStep = " + config.maxNeighborEvaluationsPerStep());
		out.println("maxSeedAttempts = " + config.maxSeedAttempts());
		out.println("numRuns = " + config.numRuns());
		out.println("poolSize = " + config.poolSize());
		out.println("logSteps = " + config.logSteps());
	}

	public static String runLabel(int runNumber) {
		String base = "Run " + runNumber;
		return colorForRun(runNumber) + base + ANSI_RESET;
	}

	private static String colorForRun(int runNumber) {
		return RUN_COLORS[(runNumber - 1) % RUN_COLORS.length];
	}
}
