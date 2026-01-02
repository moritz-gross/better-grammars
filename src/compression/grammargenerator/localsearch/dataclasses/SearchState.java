package compression.grammargenerator.localsearch.dataclasses;

import compression.grammar.SecondaryStructureGrammar;
import lombok.AllArgsConstructor;
import lombok.Value;

/**
 * Represents a candidate grammar and its bit score during search.
 */
@Value
@AllArgsConstructor
public class SearchState {
	boolean[] ruleMask;
	SecondaryStructureGrammar grammar;
	double bitsPerBase;
}
