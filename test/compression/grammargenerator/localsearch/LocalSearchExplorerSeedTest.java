package compression.grammargenerator.localsearch;

import compression.data.Dataset;
import compression.grammar.RNAWithStructure;
import compression.grammargenerator.localsearch.dataclasses.SearchStrategy;
import org.junit.Test;

import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertThrows;

public class LocalSearchExplorerSeedTest {

	/**
	 * Ensures sampleParsableSeed throws when no generated grammar can parse the dataset.
	 */
	@Test
	public void testSampleParsableSeedThrowsWhenNoParsableGrammar() {
		Dataset datasetWithUnsupportedChar = new ListDataset(
				"unsupported",
				List.of(new RNAWithStructure("A", "X", "bad")));

        assertThrows(IllegalStateException.class, () -> new LocalSearchExplorer(
                1,
                42L,
                datasetWithUnsupportedChar,
                datasetWithUnsupportedChar,
                false,
                -1,
                SearchStrategy.FIRST_IMPROVEMENT).sampleParsableSeed(1, 10));
	}

	private record ListDataset(String name, List<RNAWithStructure> rnas) implements Dataset {

		@Override
			public int getSize() {
				return rnas.size();
			}

		@Override
			public Iterator<RNAWithStructure> iterator() {
				return rnas.iterator();
			}
		}
}
