package compression.grammargenerator.localsearch;

import compression.grammargenerator.localsearch.dataclasses.Config;
import compression.grammargenerator.localsearch.dataclasses.RunResult;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static java.time.format.DateTimeFormatter.ofPattern;

public class ExploringTheWorld {


    BufferedWriter bufferedWriter;
    FileWriter fileWriter;
    private static ExploringTheWorld INSTANCE;

    //for largerDataTOCSV

    int largeIncreasedNTerminals = 0;
    int largeIncreaseTerminals;
    int largeIncreasedRuleCount = 0;
    int largeInitialRuleCount;
    int largeInitialNTerminalsCount;


    public static void main(String[] args) {
//        try {
//            LocalSearchExplorer.runWithConfig(Config.builder().build());
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
        ExploringTheWorld exploringTheWorld = ExploringTheWorld.getInstance();
        try {
            exploringTheWorld.exploreRuleCountXTerminalCount(3,75,20,3,1,1,true);
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
//        if(increaseTerminals - initialnNonterminals < 0) return;
//        if(increaseRuleCount - initialRuleCount < 0) return;
        int increasedNonterminals = 0;
        int increasedRuleCount = 0;
      // String timestamp = java.time.LocalDateTime.now().format(ofPattern("yyyyMMdd_HHmmss"));
        List<List<RunResult>> collectedResults = new ArrayList<>();
        if(runLarge){

            try {
                largerDataCollectionOfRuleCountXTerminalCountToCSV(initialRuleCount,initialnNonterminals,increaseTerminals,increaseRuleCount);
            } catch (IOException e) {
                System.out.println("larger data collection failed at initialization");
                throw e;
            }
            for(int i=increasedNonterminals;i<=increaseTerminals;i++){
                for(int j=increasedRuleCount;j<=increaseRuleCount;j++){
                    List<RunResult> oneResult = LocalSearchExplorer.runWithConfig(Config.builder()
                            .numRuns(numRuns)
                            .maxNeighborEvaluationsPerStep(maxNeighborEvaluationsPerStep)
                            .initialRuleCount(initialRuleCount+j)
                            .nNonterminals(initialnNonterminals +i)
                            .RunWithLargerDataCollection(true)
                            .build());
                    collectedResults.add(oneResult);
                }
            }
            simpleExploreRCXTCTToCSV(collectedResults,initialRuleCount,initialnNonterminals,increaseTerminals,increaseRuleCount);


        } else{
            for(int i=increasedNonterminals;i<=increaseTerminals;i++){
                for(int j=increasedRuleCount;j<=increaseRuleCount;j++){
                    List<RunResult> oneResult = LocalSearchExplorer.runWithConfig(Config.builder()
                            .numRuns(numRuns)
                            .maxNeighborEvaluationsPerStep(maxNeighborEvaluationsPerStep)
                            .initialRuleCount(initialRuleCount+j)
                            .nNonterminals(initialnNonterminals +i)
                            .build());
                    collectedResults.add(oneResult);
                }
            }
            // simpleRunResultToCSV(collectedResults);
            simpleExploreRCXTCTToCSV(collectedResults,initialRuleCount,initialnNonterminals,increaseTerminals,increaseRuleCount);
        }

    }
    void exploreRCXTCToCSV(List<List<RunResult>> collectedResults, int numRuns, int maxNeighborEvaluationsPerStep,
                           int initialRuleCount, int nNonterminals, int totalIncreaseTerminals, int totalIncreaseRuleCount,String Timestamp) throws Exception {

//        fileWriter = new FileWriter()
//        bufferedWriter = new BufferedWriter(fileWriter);

    }

    void simpleRunResultToCSV(List<List<RunResult>> runResult) throws IOException {
        String timestamp = java.time.LocalDateTime.now().format(ofPattern("yyyyMMdd_HHmmss"));
        File file = new File("results/exploringTheWorldResults/"+"local-search_"+timestamp+".csv");
        fileWriter = new FileWriter(file);
        bufferedWriter = new BufferedWriter(fileWriter);
        bufferedWriter.write("runNumber,seed,stepsTaken,totalNeighborsEvaluated,bestSize,bestBitsPerBase");
        for(List<RunResult> listRunResult:runResult){
            for(RunResult RunResult:listRunResult){
                bufferedWriter.newLine();
                bufferedWriter.write(RunResult.getStats().getRunNumber() + "," + RunResult.getStats().getSeed()
                        + "," + RunResult.getStats().getStepsTaken() + "," + RunResult.getStats().getTotalNeighborsEvaluated() +
                        "," + RunResult.getStats().getBestSize() + "," + RunResult.getStats().getBestBitsPerBase());

            }
        }
    }
    void largerDataCollectionOfRuleCountXTerminalCountToCSV(int intialRulecount, int initialNTerminalsCount,
                                   int increaseTerminals, int increaseRuleCount) throws IOException {
        largeInitialRuleCount = intialRulecount;
        largeInitialNTerminalsCount = initialNTerminalsCount;
        largeIncreaseTerminals = increaseTerminals;
        String timestamp = java.time.LocalDateTime.now().format(ofPattern("yyyyMMdd_HHmmss"));
        File file = new File("results/exploringTheLargeWorldResults/"+"local-search_"+timestamp+".csv");
        fileWriter = new FileWriter(file);
        bufferedWriter = new BufferedWriter(fileWriter);
        //needs to be updated
        bufferedWriter.write("RunID,RuleCount,NonTerminalCount,RunNumber,step,bitsPerBase,grammarSize,neighborsEvaluated");
        bufferedWriter.flush();
        CsvProgressWriter.createForExploringTheWorld();

    }
    void largerDataCollectionOfRuleCountXTerminalCountAddData(int runNumber, int step, double bitsPerBase, int grammarSize, int neighborsEvaluated) throws IOException {
        String largeRunID = runNumber + "_" + largeInitialRuleCount+largeIncreasedRuleCount + "_" + largeInitialNTerminalsCount+largeIncreasedNTerminals;
        bufferedWriter.newLine();
        bufferedWriter.write(
                            largeRunID
                        + "," + largeInitialRuleCount+largeIncreasedRuleCount
                        + "," + largeInitialNTerminalsCount+largeIncreasedNTerminals
                        + "," + runNumber
                        + "," + step
                        + "," + bitsPerBase
                        + "," + grammarSize
                        + "," + neighborsEvaluated);
        bufferedWriter.flush();
        if(largeIncreasedNTerminals <= largeIncreaseTerminals) {
            largeIncreasedNTerminals++;
        } else{
            largeIncreasedRuleCount++;
            largeIncreasedNTerminals = 0;
        }
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
                        + "," + RunResult.getStats().getSeed()
                        + "," + RunResult.getStats().getStepsTaken()
                        + "," + RunResult.getStats().getTotalNeighborsEvaluated()
                        + "," + RunResult.getStats().getBestSize()
                        + "," + RunResult.getStats().getBestBitsPerBase());
                bufferedWriter.flush();
                if(increasedNTerminals <= increaseTerminals) {
                    increasedNTerminals++;
                } else{
                    increasedRuleCount++;
                    increasedNTerminals = 0;
                }

            }

        }
        bufferedWriter.close();
    }


}
