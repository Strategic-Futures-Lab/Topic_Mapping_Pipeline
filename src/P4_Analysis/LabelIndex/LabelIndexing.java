package P4_Analysis.LabelIndex;

import P0_Project.LabelIndexModuleSpecs;
import PX_Data.JSONIOWrapper;
import PX_Data.TopicIOWrapper;
import PY_Helper.LogPrint;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LabelIndexing {

    private String mainTopicsFile;
    private boolean indexSubTopics;
    private String subTopicsFile;
    private String outputFile;

    // private JSONObject mainTopicsMetadata;
    // private JSONObject subTopicsMetadata;

    private ConcurrentHashMap<String, TopicIOWrapper> mainTopics;
    private ConcurrentHashMap<String, TopicIOWrapper> subTopics;

    private ConcurrentHashMap<String, LabelIndexEntry> Index;

    public static String Index(LabelIndexModuleSpecs indexSpecs){

        LogPrint.printModuleStart("Label Indexing");

        long startTime = System.currentTimeMillis();

        LabelIndexing startClass = new LabelIndexing();
        startClass.ProcessArguments(indexSpecs);
        startClass.LoadTopics();
        startClass.IndexLabels();
        startClass.SaveIndex();

        long timeTaken = (System.currentTimeMillis() - startTime) / (long)1000;

        LogPrint.printModuleEnd("Label Indexing");

        return "Label indexing: "+Math.floorDiv(timeTaken, 60) + " m, " + timeTaken % 60 + " s";
    }

    private void ProcessArguments(LabelIndexModuleSpecs indexSpecs){
        LogPrint.printNewStep("Processing arguments", 0);
        mainTopicsFile = indexSpecs.mainTopics;
        outputFile = indexSpecs.indexOutput;
        indexSubTopics = indexSpecs.indexSubTopics;
        if(indexSubTopics){
            subTopicsFile = indexSpecs.subTopics;
        }
        LogPrint.printCompleteStep();
        if(indexSubTopics) LogPrint.printNote("Indexing sub topics");
    }

    private void LoadTopics(){
        LogPrint.printNewStep("Loading data", 0);
        JSONObject input = JSONIOWrapper.LoadJSON(mainTopicsFile, 1);
        // mainTopicsMetadata = (JSONObject) input.get("metadata");
        JSONArray topics = (JSONArray) input.get("topics");
        mainTopics = new ConcurrentHashMap<>();
        for(JSONObject topicEntry: (Iterable<JSONObject>) topics){
            TopicIOWrapper topic = new TopicIOWrapper(topicEntry);
            mainTopics.put(topic.getId(), topic);
        }
        if(indexSubTopics){
            input = JSONIOWrapper.LoadJSON(subTopicsFile, 1);
            // subTopicsMetadata = (JSONObject) input.get("metadata");
            topics = (JSONArray) input.get("topics");
            subTopics = new ConcurrentHashMap<>();
            for(JSONObject topicEntry: (Iterable<JSONObject>) topics){
                TopicIOWrapper topic = new TopicIOWrapper(topicEntry);
                subTopics.put(topic.getId(), topic);
            }
        }
        // System.out.println("Topics Loaded!");
    }

    private void IndexLabels(){
        LogPrint.printNewStep("Indexing labels", 0);
        Index = new ConcurrentHashMap<>();
        indexLabelsFromTopics(mainTopics, true);
        if(indexSubTopics){
            indexLabelsFromTopics(subTopics, false);
        }
        LogPrint.printCompleteStep();
        // System.out.println("Labels Indexed!");
    }

    private void indexLabelsFromTopics(ConcurrentHashMap<String, TopicIOWrapper> topics, boolean isMain){
        for(Map.Entry<String, TopicIOWrapper> topic: topics.entrySet()){
            String topicId = topic.getKey();
            List<TopicIOWrapper.JSONTopicWeight> words = topic.getValue().getWords();
            for(TopicIOWrapper.JSONTopicWeight word: words){
                String label = word.ID;
                if(Index.containsKey(label)){
                    if(isMain) Index.get(label).mainTopics.add(topicId);
                    else {
                        Index.get(label).subTopics.put(topicId, new HashSet<>(topic.getValue().getMainTopicIds()));
                    }
                } else {
                    LabelIndexEntry entry = new LabelIndexEntry();
                    if(isMain) entry.mainTopics.add(topicId);
                    else {
                        entry.subTopics.put(topicId, new HashSet<>(topic.getValue().getMainTopicIds()));
                    }
                    Index.put(label, entry);
                }
            }
        }
    }

    private void SaveIndex(){
        // System.out.println("Saving Index ...");
        JSONObject root = new JSONObject();
        for(Map.Entry<String, LabelIndexEntry> indexRow: Index.entrySet()){
            String label = indexRow.getKey();
            LabelIndexEntry indexEntry = indexRow.getValue();
            JSONObject topicIds = new JSONObject();
            JSONArray mainTopicIds = new JSONArray();
            for(String id : indexEntry.mainTopics){
                mainTopicIds.add(id);
            }
            topicIds.put("mainTopics", mainTopicIds);
            if(indexSubTopics){
                JSONArray subTopicIds = new JSONArray();
                for(Map.Entry<String, Set<String>> subIds : indexEntry.subTopics.entrySet()){
                    JSONArray subTopicId = new JSONArray();
                    JSONArray subTopicMainIds = new JSONArray();
                    subTopicMainIds.addAll(subIds.getValue());
                    subTopicId.add(subIds.getKey());
                    subTopicId.add(subTopicMainIds);
                    subTopicIds.add(subTopicId);
                }
                topicIds.put("subTopics", subTopicIds);
            }
            root.put(label, topicIds);
        }
        JSONIOWrapper.SaveJSON(root, outputFile, 0);
        // System.out.println("Index Saved!");
    }
}
