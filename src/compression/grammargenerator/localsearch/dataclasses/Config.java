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

	/** Number of nonterminals used to generate the global rule universe. */
	@Default int nNonterminals = 4;

	/** Number of rules in the randomly sampled seed grammar. */
	@Default int initialRuleCount = 20;

	/** Base seed; run {@code r} uses {@code baseSeed + (r - 1)}. */
	@Default long baseSeed = 42;

	/** Maximum number of search steps per run. */
	@Default int maxSteps = 50;

	/** Maximum number of swap moves sampled when building one neighborhood. */
	@Default int maxSwapCandidatesPerStep = 100;

	/** Maximum number of valid neighbor grammars scored in one step. */
	@Default int maxNeighborEvaluationsPerStep = 150;

	/** Experimental flag used by {@code ExploringTheWorld} for extra CSV collection. */
	@Default boolean RunWithLargerDataCollection = false;

	/** Maximum number of moves considered per step before filtering; use {@code -1} to consider all. */
	@Default int maxCandidatesPerStep = 100;

	/** Maximum number of attempts to find an initial seed that passes validation and scoring. */
	@Default int maxSeedAttempts = 2000;

	/** Whether score computation may use non-canonical rules. */
	@Default boolean withNonCanonicalRules = false;

	/** Optional prefix limit on the objective dataset; {@code -1} keeps the full dataset. */
	@Default int objectiveLimit = -1;

	/** Number of independent local-search runs to execute. */
	@Default int numRuns = 100;

	/** Neighborhood policy used to choose the next search state. */
	@Default public SearchStrategy searchStrategy = BEST_IMPROVEMENT;

	/** Dataset name used for scoring and objective-side parsability checks. */
	@Default String objectiveDatasetName = "small-dataset";

	/** Dataset name that every seed and neighbor must parse. */
	@Default String parsableDatasetName = "minimal-parsable";

	/** Thread-pool size used for parallel multi-run execution. */
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
