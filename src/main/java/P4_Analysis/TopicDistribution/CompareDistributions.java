package P4_Analysis.TopicDistribution;

import P0_Project.CompareDistributionsModuleSpecs;
import PX_Data.DocIOWrapper;
import PX_Data.JSONIOWrapper;
import PX_Data.TopicIOWrapper;
import PY_Helper.LogPrint;
import de.siegmar.fastcsv.writer.CsvAppender;
import de.siegmar.fastcsv.writer.CsvWriter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class reading two sets of topic JSON files and comparing their distribution values. A set may consist of a single list
 * of topics, or two lists of main and sub topics. One set comes from a previous version of the topics and the other
 * set with updated topics (after inferring documents). Each set must have the same distribution entries.
 * The comparisons data are then exported onto the new topic JSON files, and may also be exported onto CSV files for
 * direct evaluation.
 *
 * @author P. Le Bras
 * @version 1
 */
public class CompareDistributions {

    /** Filename of the new main topic JSON file. */
    private String mainTopicsFile;
    /** Filename fof the previous main topic JSON file. */
    private String previousMainTopicsFile;
    /** Boolean flag for comparing distributions from sub topics. */
    private boolean compareSubTopics;
    /** Filename of the new sub topic JSON file. */
    private String subTopicsFile;
    /** Filename fof the previous sub topic JSON file. */
    private String previousSubTopicsFile;
    /** List of distributions to compare. */
    private String[] distributionNames;
    /** Number of topic top labels to use to identify topics. */
    private int numWordId;
    /** Boolean flag for writing the main topic distribution comparison on file. */
    private boolean outputMain;
    /** Filename of the CSV file where to write the main topic distribution comparison. */
    private String mainOutput;
    /** Boolean flag for writing the sub topic distribution comparison on file. */
    private boolean outputSub;
    /** Filename of the CSV file where to write the sub topic distribution comparison. */
    private String subOutput;
    /** Boolean flag for writing both main and sub topic distribution comparisons on file. */
    private boolean outputAll;
    /** Filename of the CSV file where to write both main and sub topic distribution comparisons. */
    private String output;

    /** List of current main topics. */
    private ConcurrentHashMap<String, TopicIOWrapper> mainTopics;
    /** Current main topics metadata. */
    private JSONObject mainMetadata;
    /** Current main topics similarities. */
    private JSONArray mainSimilarities;
    /** List of previous main topics. */
    private ConcurrentHashMap<String, TopicIOWrapper> previousMainTopics;
    /** List of current sub topics. */
    private ConcurrentHashMap<String, TopicIOWrapper> subTopics;
    /** Current sub topics metadata. */
    private JSONObject subMetadata;
    /** Current sub topics similarities. */
    private JSONArray subSimilarities;
    /** List of previous sub topics. */
    private ConcurrentHashMap<String, TopicIOWrapper> previousSubTopics;

    // comparison map: topicWords: distribName: comparison values
    /** List of distribution comparison for main topics, per topic, per distribution. */
    private ConcurrentHashMap<String,ConcurrentHashMap<String, DistribComparison>> mainTopicsComparison;
    /** List of distribution comparison for sub topics, per topic, per distribution. */
    private ConcurrentHashMap<String,ConcurrentHashMap<String, DistribComparison>> subTopicsComparison;

    /**
     * Main method, reads the specification and launches the sub-methods in order.
     * @param specs Specifications.
     * @return String indicating the time taken to read the topic JSON files, compare distributions, and write
     * the comparison results on new topic JSON files.
     */
    public static String Compare(CompareDistributionsModuleSpecs specs){

        LogPrint.printModuleStart("Compare Distributions");

        long startTime = System.currentTimeMillis();

        CompareDistributions startClass = new CompareDistributions();
        startClass.ProcessArguments(specs);
        startClass.LoadData();
        startClass.CompareDistributions();
        startClass.SaveComparisons();
        startClass.OverwriteTopics();

        long timeTaken = (System.currentTimeMillis() - startTime) / (long)1000;

        LogPrint.printModuleEnd("Compare Distributions");

        return "Comparing Distributions: "+Math.floorDiv(timeTaken, 60) + " m, " + timeTaken % 60 + " s";
    }

    /**
     * Method processing the specification parameters.
     * @param specs Specification object.
     */
    private void ProcessArguments(CompareDistributionsModuleSpecs specs){
        LogPrint.printNewStep("Processing arguments", 0);
        mainTopicsFile = specs.mainTopics;
        previousMainTopicsFile = specs.previousMainTopics;
        compareSubTopics = specs.compareSubTopics;
        if(compareSubTopics){
            subTopicsFile = specs.subTopics;
            previousSubTopicsFile = specs.previousSubTopics;
        }
        distributionNames = specs.distributions;
        numWordId = specs.numWordId;
        outputMain = specs.outputMain;
        outputSub = specs.outputSub;
        outputAll = specs.outputAll;
        if(outputMain){
            mainOutput = specs.mainOutput;
        }
        if(compareSubTopics && outputSub){
            subOutput = specs.subOutput;
        }
        if(outputAll){
            output = specs.output;
        }
        if(distributionNames.length < 1){
            // should have been checked in specs class
            LogPrint.printNoteError("Error: No distributions to compare.");
            System.exit(1);
        }
        LogPrint.printCompleteStep();
        LogPrint.printNote("Comparing distributions from "+mainTopicsFile+" with "+previousMainTopicsFile);
        if(compareSubTopics) LogPrint.printNote("And comparing distributions from "+subTopicsFile+" with "+previousSubTopicsFile);
        LogPrint.printNote("Distributions to compare: "+String.join(", ", distributionNames));
    }

    /**
     * Method loading the topic JSON files.
     */
    private void LoadData(){
        LogPrint.printNewStep("Loading data", 0);
        JSONObject input = JSONIOWrapper.LoadJSON(mainTopicsFile, 1);
        mainMetadata = (JSONObject) input.get("metadata");
        mainSimilarities = (JSONArray) input.get("similarities");
        mainTopics = loadTopicData(input);
        input = JSONIOWrapper.LoadJSON(previousMainTopicsFile, 1);
        previousMainTopics = loadTopicData(input);
        if(compareSubTopics){
            input = JSONIOWrapper.LoadJSON(subTopicsFile, 1);
            subMetadata = (JSONObject) input.get("metadata");
            subSimilarities = (JSONArray) input.get("similarities");
            subTopics = loadTopicData(input);
            input = JSONIOWrapper.LoadJSON(previousSubTopicsFile, 1);
            previousSubTopics = loadTopicData(input);
        }
    }

    /**
     * Method extracting a list of topics from an input JSON object.
     * @param input Input JSON object.
     * @return The list of the topics.
     */
    private ConcurrentHashMap<String, TopicIOWrapper> loadTopicData(JSONObject input){
        ConcurrentHashMap<String, TopicIOWrapper> topics = new ConcurrentHashMap<>();
        for(JSONObject topicEntry: (Iterable<JSONObject>) input.get("topics")){
            TopicIOWrapper topic = new TopicIOWrapper(topicEntry);
            topics.put(topic.getId(), topic);
        }
        return topics;
    }

    /**
     * Method launching the comparison process.
     */
    private void CompareDistributions(){
        LogPrint.printNewStep("Comparing distributions", 0);
        mainTopicsComparison = compareDistribution(mainTopics, previousMainTopics);
        if(compareSubTopics) subTopicsComparison = compareDistribution(subTopics, previousSubTopics);
        LogPrint.printCompleteStep();
    }

    /**
     * Method comparing distribution values from two sets of topics.
     * @param currentTopics First set of topics, with new distribution values.
     * @param previousTopics Second set of topics, with the previous distribution values.
     * @return The list of distribution comparison, per topic, per distribution.
     */
    private ConcurrentHashMap<String, ConcurrentHashMap<String, DistribComparison>> compareDistribution(
            ConcurrentHashMap<String, TopicIOWrapper> currentTopics,
            ConcurrentHashMap<String, TopicIOWrapper> previousTopics){
        if(currentTopics.size() != previousTopics.size()){
            LogPrint.printNoteError("Error: models have different numbers of topics ("+currentTopics.size()+" / "+previousTopics.size()+")");
            System.exit(1);
        }
        ConcurrentHashMap<String, ConcurrentHashMap<String, DistribComparison>> topicComparison = new ConcurrentHashMap<>();
        for(Map.Entry<String, TopicIOWrapper> t: currentTopics.entrySet()) {
            TopicIOWrapper currentTopic = t.getValue();
            TopicIOWrapper previousTopic = previousTopics.get(currentTopic.getId());
            String currentTopicWords = currentTopic.getLabelString(numWordId);
            if (!currentTopicWords.equals(previousTopic.getLabelString(numWordId))) {
                LogPrint.printNoteError("Error: topic labels show differences");
                LogPrint.printNoteError(currentTopicWords + " / " + previousTopic.getLabelString(numWordId));
                System.exit(1);
            }
            ConcurrentHashMap<String, DistribComparison> distribComparison = new ConcurrentHashMap<>();
            for (String distrib : distributionNames) {
                try {
                    TopicIOWrapper.JSONTopicWeight currentDistribTotal = currentTopic.findTotal(distrib);
                    TopicIOWrapper.JSONTopicWeight previousDistribTotal = previousTopic.findTotal(distrib);
                    distribComparison.put(distrib, new DistribComparison(previousDistribTotal.initialWeight,
                            previousDistribTotal.weight, currentDistribTotal.weight));
                    currentDistribTotal.initialWeight = previousDistribTotal.initialWeight;
                } catch (NoSuchElementException e) {
                    LogPrint.printNoteError("Error no distribution with id " + distrib + " in the topics");
                    System.exit(1);
                }
            }
            topicComparison.put(currentTopicWords, distribComparison);
        }
        return topicComparison;
    }

    /**
     * Method launching the processes writing the comparison distribution on file..
     */
    private void SaveComparisons(){
        if(outputAll || outputMain || outputSub) LogPrint.printNewStep("Saving comparison files:", 0);
        if(outputMain) saveComparison(mainOutput, mainTopicsComparison);
        if(outputSub) saveComparison(subOutput, subTopicsComparison);
        if(outputAll){
            if(compareSubTopics) saveComparison(output, mergeComparisons(mainTopicsComparison, subTopicsComparison));
            else saveComparison(output, mainTopicsComparison);
        }
    }

    /**
     * Method writing a distribution comparison on a CSV file.
     * @param filename Filename of the CSV file to write.
     * @param topicComparison Comparison to write.
     */
    private void saveComparison(String filename, ConcurrentHashMap<String, ConcurrentHashMap<String, DistribComparison>> topicComparison){
        LogPrint.printNewStep("Saving "+filename, 1);
        DecimalFormat df = new DecimalFormat("#.######");
        df.setRoundingMode(RoundingMode.UP);
        File file = new File(filename);
        file.getParentFile().mkdirs();
        try {
            // this will erase the content of the file before appending data to it.
            new FileWriter(file.getPath(), false).close();
        } catch (IOException e) {
            LogPrint.printNoteError("Error while saving similarity matrix\n");
            e.printStackTrace();
        }
        CsvWriter csvWriter = new CsvWriter();
        csvWriter.setAlwaysDelimitText(true);
        try(CsvAppender csvAppender = csvWriter.append(file, StandardCharsets.UTF_8)){
            createHeader(csvAppender);
            for(Map.Entry<String, ConcurrentHashMap<String, DistribComparison>> tC: topicComparison.entrySet()){
                String topicWords = tC.getKey();
                csvAppender.appendField(topicWords);
                for(String d: distributionNames){
                    DistribComparison c = tC.getValue().get(d);
                    csvAppender.appendField(df.format(c.initialValue));
                    csvAppender.appendField(df.format(c.previousValue));
                    csvAppender.appendField(df.format(c.currentValue));
                    csvAppender.appendField(df.format(c.getInitialDiff()));
                    csvAppender.appendField(df.format(c.getDiff()));
                }
                csvAppender.endLine();
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        LogPrint.printCompleteStep();
    }

    /**
     * Method generating headers for a CSV file.
     * @param appender CSV appender instance where to add the headers.
     * @throws IOException If an error occurs with the CSV appender.
     */
    private void createHeader(CsvAppender appender) throws IOException {
        appender.appendField("topic");
        for(String distrib: distributionNames){
            appender.appendField(distrib+"_initial");
            appender.appendField(distrib+"_previous");
            appender.appendField(distrib+"_current");
            appender.appendField(distrib+"_diffInitial");
            appender.appendField(distrib+"_diffPrevious");
        }
        appender.endLine();
    }

    /**
     * Method merging two distribution comparison, eg main topics and sub topics, into one.
     * @param mainComparison First distribution comparison.
     * @param subComparison Second distribution comparison.
     * @return The merged list of distribution comparison.
     */
    private ConcurrentHashMap<String, ConcurrentHashMap<String, DistribComparison>> mergeComparisons(
            ConcurrentHashMap<String, ConcurrentHashMap<String, DistribComparison>> mainComparison,
            ConcurrentHashMap<String, ConcurrentHashMap<String, DistribComparison>> subComparison
    ){
        for(Map.Entry<String, ConcurrentHashMap<String, DistribComparison>> subC: subComparison.entrySet()){
            mainComparison.put(subC.getKey(), subC.getValue());
        }
        return mainComparison;
    }

    /**
     * Method overwriting the current topic JSON file with the distribution comparison data.
     */
    private void OverwriteTopics(){
        LogPrint.printNewStep("Overwriting topic distribution files:", 0);
        JSONObject root = new JSONObject();
        root.put("metadata", mainMetadata);
        root.put("similarities", mainSimilarities);
        JSONArray topics = new JSONArray();
        for(Map.Entry<String, TopicIOWrapper> entry: mainTopics.entrySet()){
            topics.add(entry.getValue().toJSON());
        }
        root.put("topics", topics);
        JSONIOWrapper.SaveJSON(root, mainTopicsFile, 1);
        if(compareSubTopics){
            root = new JSONObject();
            root.put("metadata", subMetadata);
            root.put("similarities", subSimilarities);
            topics = new JSONArray();
            for(Map.Entry<String, TopicIOWrapper> entry: subTopics.entrySet()){
                topics.add(entry.getValue().toJSON());
            }
            root.put("topics", topics);
            JSONIOWrapper.SaveJSON(root, subTopicsFile, 1);
        }
    }
}
