package compression.grammargenerator.localsearch;

import compression.data.Dataset;
import compression.grammargenerator.localsearch.dataclasses.Config;
import compression.grammargenerator.localsearch.dataclasses.RunResult;
import compression.grammargenerator.localsearch.dataclasses.RunStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper utilities for formatting log output for local search runs.
 */
public final class Logging {
	private static final Logger log = LoggerFactory.getLogger("localsearch.Logging");
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
		log.info("===== starting {} of {} (seed={}) =====", runLabel(runNumber), totalRuns, seed);
	}

	public static void printSeed(int runNumber, int size, double bitsPerBase) {
		log.info("{} seed: size={} bits/base={}", runLabel(runNumber), size, formatScore(bitsPerBase));
	}

	public static void printSeedCandidate(int attempt, int size, double bitsPerBase) {
		log.info("Seed candidate {}: size={} bits/base={}", attempt, size, formatScore(bitsPerBase));
	}

	public static void printStepNoImprovement(int runNumber, int step, int size, double score, int explored) {
		log.info("{} step {}: size={} score={} | explored {} neighbors, no improvement",
				runLabel(runNumber), step, size, formatScore(score), explored);
	}

	public static void printStepImprovement(int runNumber,
	                                        int step,
	                                        int previousSize,
	                                        double previousScore,
	                                        int explored,
	                                        int improvementIndex,
	                                        int newSize,
	                                        double newScore) {
		log.info("{} step {}: size={} score={} | explored {} neighbors (improvement at #{}) -> size={} score={}",
				runLabel(runNumber),
				step,
				previousSize,
				formatScore(previousScore),
				explored,
				improvementIndex,
				newSize,
				formatScore(newScore));
	}

	public static void printRunCompleted(RunStats stats) {
		log.info("{} completed: size={} bits/base={} steps={} neighbors={}",
				runLabel(stats.getRunNumber()),
				stats.getBestSize(),
				formatScore(stats.getBestBitsPerBase()),
				stats.getStepsTaken(),
				stats.getTotalNeighborsEvaluated());
	}

	public static void printRunFailure(int runNumber, Throwable cause) {
		log.warn("Run {} failed: {}", runNumber, cause == null ? "unknown error" : cause.getMessage());
	}

	public static void printRunSummaryHeader() {
		log.info("=== Run summary ===");
	}

	public static void printLine(String message) {
		log.info(message);
	}

	public static void printSummaryLine(RunStats stats) {
		log.info("{} | seed={} | steps={} | bits/base={} | size={} | neighbors={}",
				runLabel(stats.getRunNumber()),
				stats.getSeed(),
				stats.getStepsTaken(),
				formatScore(stats.getBestBitsPerBase()),
				stats.getBestSize(),
				stats.getTotalNeighborsEvaluated());
	}

	public static void printBestOverall(RunResult best) {
		log.info("Best overall: {} seed={} size={} bits/base={}",
				runLabel(best.getStats().getRunNumber()),
				best.getStats().getSeed(),
				best.getBest().getGrammar().size(),
				formatScore(best.getBest().getBitsPerBase()));
	}

	public static void printConfig(Config config, Dataset objectiveDataset, Dataset parsableDataset) {
		log.info("=== Local search configuration ===");
		log.info("nNonterminals = {}", config.nNonterminals());
		log.info("initialRuleCount = {}", config.initialRuleCount());
		log.info("baseSeed = {}", config.baseSeed());
		log.info("objectiveDataset = {}", objectiveDataset);
		log.info("objectiveLimit = {}", config.objectiveLimit());
		log.info("parsableDataset = {}", parsableDataset);
		log.info("withNonCanonicalRules = {}", config.withNonCanonicalRules());
		log.info("maxSteps = {}", config.maxSteps());
		log.info("maxSwapCandidatesPerStep = {}", config.maxSwapCandidatesPerStep());
		log.info("maxNeighborEvaluationsPerStep = {}", config.maxNeighborEvaluationsPerStep());
		log.info("maxCandidatesPerStep = {}", config.maxCandidatesPerStep());
		log.info("maxSeedAttempts = {}", config.maxSeedAttempts());
		log.info("numRuns = {}", config.numRuns());
		log.info("poolSize = {}", config.poolSize());
		log.info("searchStrategy = {}", config.searchStrategy());
	}

	public static String runLabel(int runNumber) {
		String base = "Run " + runNumber;
		return colorForRun(runNumber) + base + ANSI_RESET;
	}

	private static String colorForRun(int runNumber) {
		return RUN_COLORS[(runNumber - 1) % RUN_COLORS.length];
	}

	private static String formatScore(double score) {
		return String.format("%.4f", score);
	}
}
