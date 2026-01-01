package compression.grammargenerator.localsearch.dataclasses;

/**
 * Configuration parameters for local search runs.
 */
public record Config(int nNonterminals,
                     int initialRuleCount,
                     long baseSeed,
                     int maxSteps,
                     int maxSwapCandidatesPerStep,
                     int maxNeighborEvaluationsPerStep,
                     int maxSeedAttempts,
                     boolean withNonCanonicalRules,
                     int objectiveLimit,
                     int numRuns,
                     boolean logSteps,
                     String objectiveDatasetName,
                     String parsableDatasetName,
                     int poolSize) {
	public static Config defaults() {
		return new Config(
				3,
				20,
				42,
				10,
				100,
				150,
				2000,
				false,
				-1,
				3,
				true,
				"small-dataset",
				"minimal-parsable",
				2);
	}
}
