package P4_Analysis.TopicDistribution;

import P0_Project.CompareDistributionsModuleSpecs;
import PX_Data.DocIOWrapper;
import PX_Data.JSONIOWrapper;
import PX_Data.TopicIOWrapper;
import PY_Helper.LogPrint;
import de.siegmar.fastcsv.writer.CsvAppender;
import de.siegmar.fastcsv.writer.CsvWriter;
import org.apache.commons.logging.Log;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.File;
import java.io.IOException;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

public class CompareDistributions {

    private String mainTopicsFile;
    private String previousMainTopicsFile;
    private boolean compareSubTopics;
    private String subTopicsFile;
    private String previousSubTopicsFile;
    private String[] distributionNames;
    private int numWordId;
    private boolean outputMain;
    private String mainOutput;
    private boolean outputSub;
    private String subOutput;
    private boolean outputAll;
    private String output;

    private ConcurrentHashMap<String, TopicIOWrapper> mainTopics;
    private JSONObject mainMetadata;
    private JSONArray mainSimilarities;
    private ConcurrentHashMap<String, TopicIOWrapper> previousMainTopics;
    private ConcurrentHashMap<String, TopicIOWrapper> subTopics;
    private JSONObject subMetadata;
    private JSONArray subSimilarities;
    private ConcurrentHashMap<String, TopicIOWrapper> previousSubTopics;

    // comparison map: topicWords: distribName: comparison values
    private ConcurrentHashMap<String,ConcurrentHashMap<String, DistribComparison>> mainTopicsComparison;
    private ConcurrentHashMap<String,ConcurrentHashMap<String, DistribComparison>> subTopicsComparison;

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
            LogPrint.printNoteError("Error: No distributions to compare.");
            System.exit(1);
        }
        LogPrint.printCompleteStep();
        LogPrint.printNote("Comparing distributions from "+mainTopicsFile+" with "+previousMainTopicsFile);
        if(compareSubTopics) LogPrint.printNote("And comparing distributions from "+subTopicsFile+" with "+previousSubTopicsFile);
        LogPrint.printNote("Distributions to compare: "+String.join(", ", distributionNames));
    }

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

    private ConcurrentHashMap<String, TopicIOWrapper> loadTopicData(JSONObject input){
        ConcurrentHashMap<String, TopicIOWrapper> topics = new ConcurrentHashMap<>();
        for(JSONObject topicEntry: (Iterable<JSONObject>) input.get("topics")){
            TopicIOWrapper topic = new TopicIOWrapper(topicEntry);
            topics.put(topic.getId(), topic);
        }
        return topics;
    }

    private void CompareDistributions(){
        LogPrint.printNewStep("Comparing distributions", 0);
        mainTopicsComparison = compareDistribution(mainTopics, previousMainTopics);
        if(compareSubTopics) subTopicsComparison = compareDistribution(subTopics, previousSubTopics);
        LogPrint.printCompleteStep();
    }

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

    private void SaveComparisons(){
        if(outputAll || outputMain || outputSub) LogPrint.printNewStep("Saving comparison files:", 0);
        if(outputMain) saveComparison(mainOutput, mainTopicsComparison);
        if(outputSub) saveComparison(subOutput, subTopicsComparison);
        if(outputAll){
            if(compareSubTopics) saveComparison(output, mergeComparisons(mainTopicsComparison, subTopicsComparison));
            else saveComparison(output, mainTopicsComparison);
        }
    }

    private void saveComparison(String filename, ConcurrentHashMap<String, ConcurrentHashMap<String, DistribComparison>> topicComparison){
        LogPrint.printNewStep("Saving "+filename, 1);
        DecimalFormat df = new DecimalFormat("#.######");
        df.setRoundingMode(RoundingMode.UP);
        File file = new File(filename);
        file.getParentFile().mkdirs();
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

    private ConcurrentHashMap<String, ConcurrentHashMap<String, DistribComparison>> mergeComparisons(
            ConcurrentHashMap<String, ConcurrentHashMap<String, DistribComparison>> mainComparison,
            ConcurrentHashMap<String, ConcurrentHashMap<String, DistribComparison>> subComparison
    ){
        for(Map.Entry<String, ConcurrentHashMap<String, DistribComparison>> subC: subComparison.entrySet()){
            mainComparison.put(subC.getKey(), subC.getValue());
        }
        return mainComparison;
    }

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
