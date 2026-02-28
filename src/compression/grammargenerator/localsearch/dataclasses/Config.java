package compression.grammargenerator.localsearch.dataclasses;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Value;
import lombok.experimental.Accessors;

import static compression.grammargenerator.localsearch.dataclasses.SearchStrategy.*;

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
	@Default int maxCandidatesPerStep = 50; //100
	@Default int maxSeedAttempts = 10000; //2000
	@Default boolean withNonCanonicalRules = false;
	@Default int objectiveLimit = -1;
	@Default int numRuns = 3;
	@Default public SearchStrategy searchStrategy = FIRST_OR_STOCHASTIC_IMPROVEMENT;
	@Default String objectiveDatasetName = "small-dataset";
	@Default String parsableDatasetName = "minimal-parsable";
	@Default int poolSize = 3;

	public static Config defaults() {
		return Config.builder().build();
	}

	public Config withStrategy(SearchStrategy strategy) {
		return toBuilder().searchStrategy(strategy).build();
	}

	public Config withMaxCandidatesPerStep(int value) {
		return toBuilder().maxCandidatesPerStep(value).build();
	}
}
