package compression.grammargenerator.localsearch;

import compression.grammargenerator.localsearch.dataclasses.Config;
import compression.grammargenerator.localsearch.dataclasses.RunResult;
import compression.grammargenerator.localsearch.dataclasses.SearchStrategy;

import java.util.List;

import static java.lang.System.out;

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

		out.println("=== Running first-improvement local search ===");
		List<RunResult> firstImprovementResults = LocalSearchExplorer.runWithConfig(firstImprovementConfig);

		out.println("\n=== Running best-improvement local search ===");
		List<RunResult> bestImprovementResults = LocalSearchExplorer.runWithConfig(bestImprovementConfig);

		RunResult bestFirst = LocalSearchExplorer.bestResult(firstImprovementResults);
		RunResult bestBest = LocalSearchExplorer.bestResult(bestImprovementResults);

		out.println("\n=== Strategy comparison ===");
		if (bestFirst != null) {
			out.printf("First improvement best: bits/base=%.4f size=%d seed=%d run=%d%n",
					bestFirst.best().bitsPerBase(),
					bestFirst.best().grammar().size(),
					bestFirst.stats().seed(),
					bestFirst.stats().runNumber());
		} else {
			out.println("First improvement produced no successful runs.");
		}

		if (bestBest != null) {
			out.printf("Best improvement best:  bits/base=%.4f size=%d seed=%d run=%d%n",
					bestBest.best().bitsPerBase(),
					bestBest.best().grammar().size(),
					bestBest.stats().seed(),
					bestBest.stats().runNumber());
		} else {
			out.println("Best improvement produced no successful runs.");
		}

		if (bestFirst != null && bestBest != null) {
			double delta = bestFirst.best().bitsPerBase() - bestBest.best().bitsPerBase();
			String winner = delta > 0 ? "Best-improvement wins" : delta < 0 ? "First-improvement wins" : "Tie";
			out.printf("Comparison: %s (delta=%.4f bits/base)%n", winner, Math.abs(delta));
		}
	}
}
