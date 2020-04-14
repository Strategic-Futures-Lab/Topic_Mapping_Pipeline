package P4_Analysis.LabelIndex;

import P0_Project.LabelIndexModuleSpecs;
import PX_Data.JSONIOWrapper;
import PX_Data.TopicIOWrapper;
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

    public static void Index(LabelIndexModuleSpecs indexSpecs){
        System.out.println( "**********************************************************\n" +
                            "* STARTING Label Index !                                 *\n" +
                            "**********************************************************\n");

        LabelIndexing startClass = new LabelIndexing();
        startClass.ProcessArguments(indexSpecs);
        startClass.LoadTopics();
        startClass.IndexLabels();
        startClass.SaveIndex();

        System.out.println( "**********************************************************\n" +
                            "* Label Index COMPLETE !                                 *\n" +
                            "**********************************************************\n");
    }

    private void ProcessArguments(LabelIndexModuleSpecs indexSpecs){
        mainTopicsFile = indexSpecs.mainTopics;
        outputFile = indexSpecs.indexOutput;
        indexSubTopics = indexSpecs.indexSubTopics;
        if(indexSubTopics){
            subTopicsFile = indexSpecs.subTopics;
        }
    }

    private void LoadTopics(){
        JSONObject input = JSONIOWrapper.LoadJSON(mainTopicsFile);
        // mainTopicsMetadata = (JSONObject) input.get("metadata");
        JSONArray topics = (JSONArray) input.get("topics");
        mainTopics = new ConcurrentHashMap<>();
        for(JSONObject topicEntry: (Iterable<JSONObject>) topics){
            TopicIOWrapper topic = new TopicIOWrapper(topicEntry);
            mainTopics.put(topic.getId(), topic);
        }
        if(indexSubTopics){
            input = JSONIOWrapper.LoadJSON(subTopicsFile);
            // subTopicsMetadata = (JSONObject) input.get("metadata");
            topics = (JSONArray) input.get("topics");
            subTopics = new ConcurrentHashMap<>();
            for(JSONObject topicEntry: (Iterable<JSONObject>) topics){
                TopicIOWrapper topic = new TopicIOWrapper(topicEntry);
                subTopics.put(topic.getId(), topic);
            }
        }
        System.out.println("Topics Loaded!");
    }

    private void IndexLabels(){
        System.out.println("Indexing Labels ...");
        Index = new ConcurrentHashMap<>();
        indexLabelsFromTopics(mainTopics, true);
        if(indexSubTopics){
            indexLabelsFromTopics(subTopics, false);
        }
        System.out.println("Labels Indexed!");
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
                        Index.get(label).subTopics.add(topicId);
                        Index.get(label).mainTopics.addAll(topic.getValue().getMainTopicIds());
                    }
                } else {
                    LabelIndexEntry entry = new LabelIndexEntry();
                    if(isMain) entry.mainTopics.add(topicId);
                    else {
                        entry.subTopics.add(topicId);
                        entry.mainTopics.addAll(topic.getValue().getMainTopicIds());
                    }
                    Index.put(label, entry);
                }
            }
        }
    }

    private void SaveIndex(){
        System.out.println("Saving Index ...");
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
                for(String id : indexEntry.subTopics){
                    subTopicIds.add(id);
                }
                topicIds.put("subTopics", subTopicIds);
            }
            root.put(label, topicIds);
        }
        JSONIOWrapper.SaveJSON(root, outputFile);
        System.out.println("Index Saved!");
    }
}
