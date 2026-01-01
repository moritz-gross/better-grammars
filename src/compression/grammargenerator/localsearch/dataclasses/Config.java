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
                     int maxCandidatesPerStep,
                     int maxSeedAttempts,
                     boolean withNonCanonicalRules,
                     int objectiveLimit,
                     int numRuns,
                     SearchStrategy searchStrategy,
                     String objectiveDatasetName,
                     String parsableDatasetName,
                     int poolSize) {
	public static Config defaults() {
		return new Config(
				3,  // nNonterminals
				20,  // initialRuleCount
				42,  // baseSeed
				25,  // maxSteps
				100,  // maxSwapCandidatesPerStep
				150,  // maxNeighborEvaluationsPerStep
				100,  // maxCandidatesPerStep
				2000,  // maxSeedAttempts
				false,  // withNonCanonicalRules
				-1,  // objectiveLimit
				3,  // numRuns
				SearchStrategy.FIRST_IMPROVEMENT,
				"small-dataset",
				"minimal-parsable",
				3);
	}

	public Config withStrategy(SearchStrategy strategy) {
		return new Config(
				nNonterminals,
				initialRuleCount,
				baseSeed,
				maxSteps,
				maxSwapCandidatesPerStep,
				maxNeighborEvaluationsPerStep,
				maxCandidatesPerStep,
				maxSeedAttempts,
				withNonCanonicalRules,
				objectiveLimit,
				numRuns,
				strategy,
				objectiveDatasetName,
				parsableDatasetName,
				poolSize);
	}
}
