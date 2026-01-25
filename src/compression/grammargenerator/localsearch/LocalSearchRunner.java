package compression.grammargenerator.localsearch;

import compression.data.CachedDataset;
import compression.data.Dataset;
import compression.data.FolderBasedDataset;
import compression.grammargenerator.localsearch.dataclasses.Config;
import compression.grammargenerator.localsearch.dataclasses.RunResult;
import compression.grammargenerator.localsearch.dataclasses.RunStats;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Orchestrates multi-run local search execution.
 */
final class LocalSearchRunner {
	private LocalSearchRunner() {
		// utility
	}

	static List<RunResult> run(final Config config) throws Exception {
		final Dataset objectiveDataset = new CachedDataset(new FolderBasedDataset(config.objectiveDatasetName()));
		final Dataset parsableDataset = new CachedDataset(new FolderBasedDataset(config.parsableDatasetName()));

		Logging.printConfig(config, objectiveDataset, parsableDataset);

		// Create CSV writer for progress logging
		CsvProgressWriter csvWriter = CsvProgressWriter.create();
		Logging.setCsvWriter(csvWriter);

		try {
			ExecutorService executor = Executors.newFixedThreadPool(config.poolSize());
			List<Future<RunResult>> futures = new ArrayList<>();

			for (int r = 0; r < config.numRuns(); r++) {
				final int runNumber = r + 1;
				final long runSeed = config.baseSeed() + r;
				Callable<RunResult> task = () -> {
					Logging.printRunStart(runNumber, config.numRuns(), runSeed);
					LocalSearchExplorer explorer = new LocalSearchExplorer(
							config.nNonterminals(),
							runSeed,
							objectiveDataset,
							parsableDataset,
							config.withNonCanonicalRules(),
							config.objectiveLimit(),
							config.searchStrategy());
					return explorer.runSingleRun(
							config.initialRuleCount(),
							config.maxSeedAttempts(),
							config.maxSteps(),
							config.maxSwapCandidatesPerStep(),
							config.maxNeighborEvaluationsPerStep(),
							config.maxCandidatesPerStep(),
							runNumber);
				};
				futures.add(executor.submit(task));
			}

			List<RunResult> runResults = new ArrayList<>();
			for (int i = 0; i < futures.size(); i++) {
				try {
					RunResult result = futures.get(i).get();
					runResults.add(result);
					RunStats stats = result.getStats();
					Logging.printRunCompleted(stats);
				} catch (ExecutionException e) {
					Logging.printRunFailure(i + 1, e.getCause());
				}
			}
			executor.shutdown();

			RunResult best = LocalSearchExplorer.bestResult(runResults);

			Logging.printRunSummaryHeader();
			for (RunResult r : runResults) {
				Logging.printSummaryLine(r.getStats());
			}
			if (best != null) {
				Logging.printBestOverall(best);
			}
			return runResults;
		} finally {
			csvWriter.close();
			Logging.setCsvWriter(null);
		}
	}
}
