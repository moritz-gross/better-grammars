package compression.grammargenerator.localsearch;

import compression.grammargenerator.localsearch.dataclasses.Config;
import compression.grammargenerator.localsearch.dataclasses.RunResult;
import compression.grammargenerator.localsearch.dataclasses.SearchStrategy;

import java.util.List;

/**
 * Runs local search with both neighbor selection strategies and prints a comparison.
 */
public final class StrategyComparison {
	private StrategyComparison() {
		// utility
	}

	public static void main(String[] args) throws Exception {
		Config baseConfig = Config.defaults();
		Config firstImprovementConfig = baseConfig.withStrategy(SearchStrategy.FIRST_IMPROVEMENT);
		Config bestImprovementConfig = baseConfig.withStrategy(SearchStrategy.BEST_IMPROVEMENT);

		Logging.printLine("=== Running first-improvement local search ===");
		List<RunResult> firstImprovementResults = LocalSearchExplorer.runWithConfig(firstImprovementConfig);

		Logging.printLine("=== Running best-improvement local search ===");
		List<RunResult> bestImprovementResults = LocalSearchExplorer.runWithConfig(bestImprovementConfig);

		RunResult bestFirst = LocalSearchExplorer.bestResult(firstImprovementResults);
		RunResult bestBest = LocalSearchExplorer.bestResult(bestImprovementResults);

		Logging.printLine("=== Strategy comparison ===");
		if (bestFirst != null) {
			Logging.printLine(String.format(
					"First improvement best: bits/base=%.4f size=%d seed=%d run=%d",
					bestFirst.getBest().getBitsPerBase(),
					bestFirst.getBest().getGrammar().size(),
					bestFirst.getStats().getSeed(),
					bestFirst.getStats().getRunNumber()));
		} else {
			Logging.printLine("First improvement produced no successful runs.");
		}

		if (bestBest != null) {
			Logging.printLine(String.format(
					"Best improvement best: bits/base=%.4f size=%d seed=%d run=%d",
					bestBest.getBest().getBitsPerBase(),
					bestBest.getBest().getGrammar().size(),
					bestBest.getStats().getSeed(),
					bestBest.getStats().getRunNumber()));
		} else {
			Logging.printLine("Best improvement produced no successful runs.");
		}

		if (bestFirst != null && bestBest != null) {
			double delta = bestFirst.getBest().getBitsPerBase() - bestBest.getBest().getBitsPerBase();
			String winner = delta > 0 ? "Best-improvement wins" : delta < 0 ? "First-improvement wins" : "Tie";
			Logging.printLine(String.format(
					"Comparison: %s (delta=%.4f bits/base)",
					winner,
					Math.abs(delta)));
		}
	}
}
