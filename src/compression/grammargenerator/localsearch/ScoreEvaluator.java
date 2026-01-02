package compression.grammargenerator.localsearch;

import compression.grammar.SecondaryStructureGrammar;

/**
 * Computes a compression score for a grammar.
 */
@FunctionalInterface
interface ScoreEvaluator {
	double score(SecondaryStructureGrammar grammar);
}
