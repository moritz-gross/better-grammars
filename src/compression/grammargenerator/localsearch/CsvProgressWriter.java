package compression.grammargenerator.localsearch;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Objects;

import static java.time.format.DateTimeFormatter.*;

/**
 * Thread-safe CSV writer for logging local search progress.
 * Writes run number, step number, bits per base, grammar size, and neighbors evaluated.
 */
public final class CsvProgressWriter implements AutoCloseable {
	private final BufferedWriter writer;
	private final Object lock = new Object();
    private static ExploringTheWorld exploringTheWorld;
    private static CsvProgressWriter INSTANCE;

	private CsvProgressWriter(BufferedWriter writer) {
		this.writer = writer;
	}

	/**
	 * Creates a new CSV writer with timestamped filename in the results directory.
	 */
	public static CsvProgressWriter create() throws IOException {
        //changed this Method (and Class) to Singleton - Fabian
        if(INSTANCE == null) {
            Path resultsDir = Paths.get("results");
            Files.createDirectories(resultsDir);

            String timestamp = java.time.LocalDateTime.now().format(ofPattern("yyyyMMdd_HHmmss"));
            Path csvPath = resultsDir.resolve("localsearch_" + timestamp + ".csv");

            BufferedWriter writer = new BufferedWriter(new FileWriter(csvPath.toFile()));
            INSTANCE = new CsvProgressWriter(writer);
            INSTANCE.writeHeader();
            return INSTANCE;
        }
		return INSTANCE;

	}

    /**
     * *
     * does the exact same thing as the method as the Method create() above
     * it just adds an instance I need for ExploringTheWorld
     */
    public static void createForExploringTheWorld() throws IOException {
        INSTANCE = create();
        exploringTheWorld = ExploringTheWorld.getInstance();
    }

	private void writeHeader() throws IOException {
		synchronized (lock) {
			writer.write("Run,Step,BitsPerBase,GrammarSize,NeighborsEvaluated\n");
			writer.flush();
		}
	}

	/**
	 * Writes a progress entry to the CSV file.
	 * Thread-safe for concurrent writes from multiple runs.
	 */
	public void writeProgress(int runNumber, int step, double bitsPerBase, int grammarSize, int neighborsEvaluated) {
		synchronized (lock) {
			try {
				writer.write(String.format(Locale.US, "%d,%d,%.6f,%d,%d%n",
						runNumber, step, bitsPerBase, grammarSize, neighborsEvaluated));
				writer.flush();
                if(Objects.nonNull(exploringTheWorld)) exploringTheWorld.largerDataCollectionOfRuleCountXTerminalCountAddData(runNumber,  step, bitsPerBase, grammarSize, neighborsEvaluated);
			} catch (IOException e) {
				// Log error but don't fail the run
				System.err.println("Failed to write CSV progress: " + e.getMessage());
			}
		}
	}

	@Override
	public void close() throws IOException {
		synchronized (lock) {
			writer.close();
		}
	}
}
