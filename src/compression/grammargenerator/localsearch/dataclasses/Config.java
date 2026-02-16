package compression.grammargenerator.localsearch.dataclasses;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Value;
import lombok.experimental.Accessors;

import static compression.grammargenerator.localsearch.dataclasses.SearchStrategy.FIRST_IMPROVEMENT;

/**
 * Configuration parameters for local search runs.
 */
@Value
@Builder(toBuilder = true)
@Accessors(fluent = true)
public class Config {
	@Default int nNonterminals = 3;
	@Default int initialRuleCount = 20;
	@Default long baseSeed = 42;
	@Default int maxSteps = 25;
	@Default int maxSwapCandidatesPerStep = 100;
	@Default int maxNeighborEvaluationsPerStep = 150;
	/**
	 * Maximum number of neighbor candidates to consider per step; use -1 to explore all.
	 */
	@Default int maxCandidatesPerStep = 100;
	@Default int maxSeedAttempts = 2000;
	@Default boolean withNonCanonicalRules = false;
	@Default int objectiveLimit = -1;
	@Default int numRuns = 200;
	@Default SearchStrategy searchStrategy = FIRST_IMPROVEMENT;
	@Default String objectiveDatasetName = "small-dataset";
	@Default String parsableDatasetName = "minimal-parsable";
	@Default int poolSize = recommendedPoolSize();

	public static Config defaults() {
		return Config.builder().build();
	}

	public Config withStrategy(SearchStrategy strategy) {
		return toBuilder().searchStrategy(strategy).build();
	}

	public Config withMaxCandidatesPerStep(int value) {
		return toBuilder().maxCandidatesPerStep(value).build();
	}

	public static int recommendedPoolSize() {
		return Math.max(1, (int) Math.round(Runtime.getRuntime().availableProcessors() * 0.7));
	}
}
