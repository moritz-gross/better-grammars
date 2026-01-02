package compression.grammargenerator.localsearch;

import compression.grammargenerator.localsearch.dataclasses.Config;
import compression.grammargenerator.localsearch.dataclasses.RunResult;
import compression.grammargenerator.localsearch.dataclasses.SearchStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Runs local search with both neighbor selection strategies and prints a comparison.
 */
public final class StrategyComparison {
	private static final Logger log = LoggerFactory.getLogger("localsearch.StrategyComparison");
	private StrategyComparison() {
		// utility
	}

	public static void main(String[] args) throws Exception {
		Config baseConfig = Config.defaults();
		Config firstImprovementConfig = baseConfig.withStrategy(SearchStrategy.FIRST_IMPROVEMENT);
		Config bestImprovementConfig = baseConfig.withStrategy(SearchStrategy.BEST_IMPROVEMENT);

		log.info("=== Running first-improvement local search ===");
		List<RunResult> firstImprovementResults = LocalSearchExplorer.runWithConfig(firstImprovementConfig);

		log.info("=== Running best-improvement local search ===");
		List<RunResult> bestImprovementResults = LocalSearchExplorer.runWithConfig(bestImprovementConfig);

		RunResult bestFirst = LocalSearchExplorer.bestResult(firstImprovementResults);
		RunResult bestBest = LocalSearchExplorer.bestResult(bestImprovementResults);

		log.info("=== Strategy comparison ===");
		if (bestFirst != null) {
			log.info("First improvement best: bits/base={} size={} seed={} run={}",
					format(bestFirst.best().bitsPerBase()),
					bestFirst.best().grammar().size(),
					bestFirst.stats().seed(),
					bestFirst.stats().runNumber());
		} else {
			log.info("First improvement produced no successful runs.");
		}

		if (bestBest != null) {
			log.info("Best improvement best: bits/base={} size={} seed={} run={}",
					format(bestBest.best().bitsPerBase()),
					bestBest.best().grammar().size(),
					bestBest.stats().seed(),
					bestBest.stats().runNumber());
		} else {
			log.info("Best improvement produced no successful runs.");
		}

		if (bestFirst != null && bestBest != null) {
			double delta = bestFirst.best().bitsPerBase() - bestBest.best().bitsPerBase();
			String winner = delta > 0 ? "Best-improvement wins" : delta < 0 ? "First-improvement wins" : "Tie";
			log.info("Comparison: {} (delta={} bits/base)", winner, format(Math.abs(delta)));
		}
	}

	private static String format(double value) {
		return String.format("%.4f", value);
	}
}
