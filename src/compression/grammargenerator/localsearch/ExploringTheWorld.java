package compression.grammargenerator.localsearch;

import compression.grammargenerator.localsearch.dataclasses.Config;
import compression.grammargenerator.localsearch.dataclasses.RunResult;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static compression.grammargenerator.localsearch.dataclasses.SearchStrategy.FIRST_IMPROVEMENT;
import static java.time.format.DateTimeFormatter.ofPattern;

public class ExploringTheWorld {


    BufferedWriter bufferedWriter;
    FileWriter fileWriter;
    private static ExploringTheWorld INSTANCE;

    //for largerDataTOCSV

    int largeIncreasedNTerminals = 0;
    int largeIncreaseTerminals;
    int largeIncreasedRuleCount = 0;
    int largeIncreaseRuleCount;
    int largeInitialRuleCount;
    int largeInitialNTerminalsCount;
    /* A large Runs is one execution of LocalSearchExplorer for the variables below*/
    int numRunsPerLargeRun;
    int runsMadePerLargeRun;


    public static void main(String[] args) {

        ExploringTheWorld exploringTheWorld = ExploringTheWorld.getInstance();
        try {
            exploringTheWorld.exploreRuleCountXTerminalCount(10,75,15,3,5,4,true);
        } catch (Exception e) {
            System.out.println("Well maybe that did not work");
        }
    }
    private ExploringTheWorld() {

    }
    public static ExploringTheWorld getInstance(){
        if(INSTANCE==null){
            INSTANCE = new ExploringTheWorld();

        }
        return INSTANCE;
    }

    void exploreRuleCountXTerminalCount(int numRuns,int maxNeighborEvaluationsPerStep, int initialRuleCount, int initialnNonterminals, int increaseTerminals, int increaseRuleCount, boolean runLarge) throws Exception {

        /* A large Runs is one execution of LocalSearchExplorer*/
        numRunsPerLargeRun = numRuns;
        int increasedNonterminals = 0;
        int increasedRuleCount = 0;

        List<List<RunResult>> collectedResults = new ArrayList<>();
        if(runLarge){

            try {
                largerDataCollectionOfRuleCountXTerminalCountToCSV(initialRuleCount,initialnNonterminals,increaseTerminals,increaseRuleCount);
            } catch (IOException e) {
                System.out.println("larger data collection failed at initialization");
                throw e;
            }
            for(int i=increasedNonterminals;i<increaseTerminals;i++){
                for(int j=increasedRuleCount;j<increaseRuleCount;j++){
                    List<RunResult> oneResult = LocalSearchExplorer.runWithConfig(Config.builder()
                            .numRuns(numRuns)
                            .maxNeighborEvaluationsPerStep(maxNeighborEvaluationsPerStep)
                            .initialRuleCount(initialRuleCount+j)
                            .nNonterminals(initialnNonterminals +i)
                            .RunWithLargerDataCollection(true)
                            .searchStrategy(FIRST_IMPROVEMENT)
                            .build());
                    collectedResults.add(oneResult);
                }
            }
            simpleExploreRCXTCTToCSV(collectedResults,initialRuleCount,initialnNonterminals,increaseTerminals,increaseRuleCount);


        } else{
            for(int i=increasedNonterminals;i<increaseTerminals;i++){
                for(int j=increasedRuleCount;j<increaseRuleCount;j++){
                    List<RunResult> oneResult = LocalSearchExplorer.runWithConfig(Config.builder()
                            .numRuns(numRuns)
                            .maxNeighborEvaluationsPerStep(maxNeighborEvaluationsPerStep)
                            .initialRuleCount(initialRuleCount+j)
                            .nNonterminals(initialnNonterminals +i)
                            .searchStrategy(FIRST_IMPROVEMENT)
                            .build());
                    collectedResults.add(oneResult);
                }
            }
            // simpleRunResultToCSV(collectedResults);
            simpleExploreRCXTCTToCSV(collectedResults,initialRuleCount,initialnNonterminals,increaseTerminals,increaseRuleCount);
        }

    }



    void largerDataCollectionOfRuleCountXTerminalCountToCSV(int intialRulecount, int initialNTerminalsCount,
                                   int increaseTerminals, int increaseRuleCount) throws IOException {
        largeInitialRuleCount = intialRulecount;
        largeInitialNTerminalsCount = initialNTerminalsCount;
        largeIncreaseTerminals = increaseTerminals;
        largeIncreaseRuleCount = increaseRuleCount;
        String timestamp = java.time.LocalDateTime.now().format(ofPattern("yyyyMMdd_HHmmss"));
        File file = new File("results/exploringTheLargeWorldResults/"+"local-search_"+timestamp+".csv");
        fileWriter = new FileWriter(file);
        bufferedWriter = new BufferedWriter(fileWriter);
        //needs to be updated
        bufferedWriter.write("RunID,RuleCount,NonTerminalCount,RunNumber,step,bitsPerBase,grammarSize,neighborsEvaluated");
        bufferedWriter.flush();
        CsvProgressWriter.createForExploringTheWorld();

    }
    void largerDataCollectionOfRuleCountXTerminalCountAddDataToCSV(int runNumber, int step, double bitsPerBase, int grammarSize, int neighborsEvaluated) throws IOException {
        if (step == -1){
            if(runsMadePerLargeRun >= numRunsPerLargeRun) {
                runsMadePerLargeRun = 0;
                if (largeIncreasedRuleCount < largeIncreaseRuleCount) {
                    largeIncreasedRuleCount++;
                } else {
                    largeIncreasedNTerminals++;
                    largeIncreasedRuleCount = 0;
                }
            } else runsMadePerLargeRun++;
        }
        String largeRunID = runNumber + "_" + (largeInitialRuleCount + largeIncreasedRuleCount) + "_" + (largeInitialNTerminalsCount + largeIncreasedNTerminals);
        bufferedWriter.newLine();
        bufferedWriter.write(
                largeRunID
                        + "," + (largeInitialRuleCount + largeIncreasedRuleCount)
                        + "," + (largeInitialNTerminalsCount + largeIncreasedNTerminals)
                        + "," + runNumber
                        + "," + step
                        + "," + bitsPerBase
                        + "," + grammarSize
                        + "," + neighborsEvaluated);
        bufferedWriter.flush();

    }

    /**
     * writes the result from Method "exploreRuleCountXTerminalCount" to CSV file
     * and assigns unique runIDs since the variable run isn't unique in this case
     * needs a lot of more debugging .-.
     * @throws IOException
     */
    void simpleExploreRCXTCTToCSV(List<List<RunResult>> runResult,int intialRulecount, int initialNTerminalsCount,
                                  int increaseTerminals, int increaseRuleCount) throws IOException {
        String timestamp = java.time.LocalDateTime.now().format(ofPattern("yyyyMMdd_HHmmss"));
        File file = new File("results/exploringTheWorldResults/"+"local-search_"+timestamp+".csv");
        fileWriter = new FileWriter(file);
        bufferedWriter = new BufferedWriter(fileWriter);
        bufferedWriter.write("runID,runNumber,RuleCount,NTerminalCount,seed,stepsTaken,totalNeighborsEvaluated,bestSize,bestBitsPerBase");
        bufferedWriter.flush();
        int increasedNTerminals = 0;
        int increasedRuleCount = 0;
        for(List<RunResult> listRunResult:runResult){
            for(RunResult RunResult:listRunResult){

                String runID = RunResult.getStats().getRunNumber() + "_" + intialRulecount+increasedRuleCount + "_" + initialNTerminalsCount+increasedNTerminals;
                bufferedWriter.newLine();
                bufferedWriter.write(
                            runID
                        + "," + RunResult.getStats().getRunNumber()
                        + "," + intialRulecount+increasedRuleCount
                        + "," + initialNTerminalsCount+increasedNTerminals
                        + "," + RunResult.getStats().getSeed()
                        + "," + RunResult.getStats().getStepsTaken()
                        + "," + RunResult.getStats().getTotalNeighborsEvaluated()
                        + "," + RunResult.getStats().getBestSize()
                        + "," + RunResult.getStats().getBestBitsPerBase());
                bufferedWriter.flush();

            }
            if(increasedRuleCount <= increaseRuleCount ){
                increasedRuleCount++;
            } else{
                increasedNTerminals++;
                increasedRuleCount = 0;
            }

        }
        bufferedWriter.close();
    }


}
