# Local Search

This package implements a local-search grammar explorer. It starts from a random parsable seed grammar, scores that grammar on an objective dataset, and then repeatedly evaluates neighboring grammars until no acceptable next move is found.

## Search Loop

The current implementation uses a hill-climbing style loop:

- sample a random seed grammar that parses both the parsable dataset and the objective dataset
- score the seed with bits per base on the objective dataset
- enumerate single-move neighbors by adding, removing, or swapping one rule
- keep only valid and parsable neighbors
- select the next state according to the configured `SearchStrategy`
- stop when the step limit is reached or the strategy does not return a next improvement

## Search Strategies

The package supports multiple ways to search the neighborhood:

- `FIRST_IMPROVEMENT`: stop as soon as the first improving neighbor is found
- `BEST_IMPROVEMENT`: inspect improving neighbors in the neighborhood and pick the best one found
- `STOCHASTIC_IMPROVEMENT`: collect improving neighbors and sample from them with a bias toward better scores
- `FIRST_OR_STOCHASTIC_IMPROVEMENT`: take the first strict improvement if one appears; otherwise fall back to stochastic selection among explored neighbors

First improvement is cheaper per step because it can terminate early. Best improvement spends more evaluations per step, but uses a stronger local choice because it compares more of the neighborhood before moving.

## Main Classes

- `LocalSearchExplorer`: public entry point for running local search
- `LocalSearchRunner`: orchestrates multi-run execution with a thread pool
- `NeighborSearcher`: generates and evaluates neighboring grammars
- `RuleMaskCodec`: converts between grammars and boolean rule masks
- `Config`: bundles run parameters and defaults
- `dataclasses.SearchStrategy`: defines how a step searches the neighborhood and chooses the next candidate

## Running

`LocalSearchExplorer.main(...)` runs the search with `Config.defaults()`. `LocalSearchExplorer.runWithConfig(...)` can be used to run custom configurations programmatically.

By default, the configuration uses:

- `small-dataset` as the objective dataset
- `minimal-parsable` as the parsable dataset
- `FIRST_OR_STOCHASTIC_IMPROVEMENT` as the search strategy
- multiple runs in parallel via `poolSize`

## Outputs

Progress is logged through `Logging`. In addition, `CsvProgressWriter` writes a timestamped CSV file to `results/` with:

- run number
- step number
- bits per base
- grammar size
- number of evaluated neighbors

## Notes

- The search objective is lower bits per base.
- Neighbor candidates are filtered for grammar validity and dataset parsability before scoring.
- The package contains the search implementation only; datasets and grammar generation live in the surrounding `compression` packages.
