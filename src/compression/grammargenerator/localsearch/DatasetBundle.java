package compression.grammargenerator.localsearch;

import compression.data.Dataset;
import compression.grammar.RNAWithStructure;
import compression.grammar.Terminal;
import com.google.common.collect.ImmutableList;
import lombok.Value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Holds prepared datasets and cached word lists for local search.
 */
@Value
class DatasetBundle {
	Dataset objectiveDatasetLimited;
	List<List<Terminal<Character>>> parsableDatasetWords;
	List<List<Terminal<Character>>> objectiveDatasetWords;

	static DatasetBundle from(final Dataset objectiveDataset, final Dataset parsableDataset, final int objectiveLimit) {
		ImmutableList.Builder<List<Terminal<Character>>> parsableWordsBuilder = ImmutableList.builder();
		for (RNAWithStructure rna : parsableDataset) {
			parsableWordsBuilder.add(rna.secondaryStructureAsTerminals());
		}

		List<RNAWithStructure> rnas = new ArrayList<>(objectiveDataset.getSize());
		for (RNAWithStructure rna : objectiveDataset) {
			rnas.add(rna);
		}
		if (objectiveLimit > 0 && objectiveLimit < rnas.size()) {
			rnas = rnas.subList(0, objectiveLimit);
		}
		List<RNAWithStructure> objectiveRnasLimited = Collections.unmodifiableList(rnas);
		Dataset objectiveDatasetLimited = new Dataset() {
			@Override
			public int getSize() {
				return objectiveRnasLimited.size();
			}

			@Override
			public String name() {
				return objectiveDataset.name() + "-limited";
			}

			@Override
			public Iterator<RNAWithStructure> iterator() {
				return objectiveRnasLimited.iterator();
			}
		};

		ImmutableList.Builder<List<Terminal<Character>>> objectiveWordsBuilder = ImmutableList.builder();
		for (RNAWithStructure rna : objectiveRnasLimited) {
			objectiveWordsBuilder.add(rna.secondaryStructureAsTerminals());
		}

		return new DatasetBundle(
				objectiveDatasetLimited,
				parsableWordsBuilder.build(),
				objectiveWordsBuilder.build());
	}
}
