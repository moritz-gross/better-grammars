package compression.grammargenerator.localsearch.dataclasses;

import compression.grammar.SecondaryStructureGrammar;

/**
 * Represents a candidate grammar and its bit score during search.
 */
public record SearchState(boolean[] ruleMask, SecondaryStructureGrammar grammar, double bitsPerBase) {
}
