package compression.grammargenerator.localsearch;

import compression.grammargenerator.localsearch.dataclasses.Config;
import compression.grammargenerator.localsearch.dataclasses.RunResult;
import compression.grammargenerator.localsearch.dataclasses.SearchStrategy;

import java.util.List;

import static compression.grammargenerator.localsearch.dataclasses.SearchStrategy.FIRST_IMPROVEMENT;

public class LongRunForLocalSearch {
    /**
     * more things can be adjusted in dataclasses/Config
     * This classes only purpose is to give an additional option for starting a run without directly changing the config class
     * Depending on the capacity of the device this is executed a run can roughly take 2 and a half minutes for STOCHASTIC_IMPROVEMENT
     * and 8x longer (20m minutes?) for FIRST_OR_STOCHASTIC_IMPROVEMENT. (this is a rough guess since we did not write down times for single run)
     * 15hours, 18min for 100 best-improvement
     */
    public static void main(String[] args) throws Exception {
        //added as an example for a run.
        runforLocalSearch(100,150,50,FIRST_IMPROVEMENT);

    }

    /**
     *
     * @param numberOfRuns means how many random generated Grammars we improve. / For how many Grammars we execute a local search
     * @param maxNeighborEvaluationsPerStep is the amount of attempts we try to improve the grammar in a single step.
     *                                      (a single step is either add, remove or swap a Rule)
     *                                      One Neighbour is either an add, remove or swap of a Rule which then is usually
     *                                      tested for improvement, a step is an accepted neighbour/change for the next iteration.
     * @param maxSteps is the amount of steps we take before ending the local search.
     * @param  searchStrategy is the search strategy we want to use
     * @return the returned List of RunResult can usually be ignored. The CSV written by the CsvProgressWriter holds a lot
     *          more data and is more interesting. It's not a void method in case you need the returned List for something.
     * @throws Exception
     */
    public static List<RunResult> runforLocalSearch(int numberOfRuns, int maxNeighborEvaluationsPerStep, int maxSteps, SearchStrategy searchStrategy) throws Exception {
        List<RunResult> result = LocalSearchExplorer.runWithConfig(Config.builder()
                .numRuns(numberOfRuns)
                .maxNeighborEvaluationsPerStep(maxNeighborEvaluationsPerStep)
                .searchStrategy(searchStrategy)
                .maxSteps(maxSteps)
                .build());
        return result;
    }

}
