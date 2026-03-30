/**
 * Local-search exploration for grammar compression.
 *
 * <p>The package searches over grammars generated from a fixed rule universe. A search state
 * consists of:
 *
 * <ul>
 *   <li>a boolean rule mask</li>
 *   <li>the corresponding {@code SecondaryStructureGrammar}</li>
 *   <li>its {@code bits/base} score on the objective dataset</li>
 * </ul>
 *
 * <p>Each run follows this structure:
 *
 * <ol>
 *   <li>sample a random seed grammar until it is valid, parses both datasets, and has a finite score</li>
 *   <li>enumerate add, remove, and sampled swap moves from the current rule mask</li>
 *   <li>reconstruct the candidate grammar and reject invalid or non-parsable candidates</li>
 *   <li>score the remaining candidates on the objective dataset</li>
 *   <li>let the configured {@code SearchStrategy} decide whether to stop or move</li>
 * </ol>
 *
 * <p>The package is not limited to strict hill climbing. In particular,
 * {@code FIRST_OR_STOCHASTIC_IMPROVEMENT} may continue with a non-improving move if no strict
 * improvement is found among the explored neighbors.
 *
 * <p>Main entry points:
 *
 * <ul>
 *   <li>{@code LocalSearchExplorer}: public API and single-run search logic</li>
 *   <li>{@code LocalSearchRunner}: multi-run orchestration and aggregation</li>
 *   <li>{@code NeighborSearcher}: move generation, filtering, and strategy-specific selection</li>
 * </ul>
 *
 * <p>See {@code README.md} in this package for the strategy matrix, config reference, output
 * format, and experiment-oriented entry points.
 */
package compression.grammargenerator.localsearch;
