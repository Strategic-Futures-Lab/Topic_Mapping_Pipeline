package P3_TopicModelling;

import P0_Project.TopicModelExportModuleSpecs;
import PX_Data.DocIOWrapper;
import PX_Data.JSONIOWrapper;
import PX_Data.TopicIOWrapper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ExportTopicModel {

    private String mainTopicsFile;
    private String mainOutput;
    private boolean exportSubTopics;
    private String subTopicsFile;
    private String subOutput;
    private String documentsFile;
    private List<String> docFields;

    private ConcurrentHashMap<String, DocIOWrapper> documents;
    private JSONObject documentsMetadata;
    private ConcurrentHashMap<String, TopicIOWrapper> mainTopics;
    private JSONObject mainTopicsMetadata;
    private ConcurrentHashMap<String, TopicIOWrapper> subTopics;
    private JSONObject subTopicsMetadata;

    public static void ExportTopicModel(TopicModelExportModuleSpecs exportSpecs){
        System.out.println( "**********************************************************\n" +
                            "* STARTING Topic Model Export !                       *\n" +
                            "**********************************************************\n");

        ExportTopicModel startClass = new ExportTopicModel();
        startClass.ProcessArguments(exportSpecs);
        startClass.LoadFiles();
        startClass.BuildModelData();
        startClass.SaveModel();

        System.out.println( "**********************************************************\n" +
                            "* Topic Model Export Complete !                          *\n" +
                            "**********************************************************\n");
    }

    private void ProcessArguments(TopicModelExportModuleSpecs exportSpecs){
        mainTopicsFile = exportSpecs.mainTopics;
        mainOutput = exportSpecs.mainOutput;
        exportSubTopics = exportSpecs.exportSubTopics;
        if(exportSubTopics){
            subTopicsFile = exportSpecs.subTopics;
            subOutput = exportSpecs.subOutput;
        }
        documentsFile = exportSpecs.documents;
        docFields = Arrays.asList(exportSpecs.docFields);
    }

    private void LoadFiles(){
        System.out.println("Loading data ...");
        JSONObject input = JSONIOWrapper.LoadJSON(documentsFile);
        documentsMetadata = (JSONObject) input.get("metadata");
        JSONArray docs = (JSONArray) input.get("documents");
        documents = new ConcurrentHashMap<>();
        for(JSONObject docEntry: (Iterable<JSONObject>) docs){
            DocIOWrapper doc = new DocIOWrapper(docEntry);
            documents.put(doc.getId(), doc);
        }
        input = JSONIOWrapper.LoadJSON(mainTopicsFile);
        mainTopicsMetadata = (JSONObject) input.get("metadata");
        mainTopics = new ConcurrentHashMap<>();
        for(JSONObject topicEntry: (Iterable<JSONObject>) input.get("topics")){
            TopicIOWrapper topic = new TopicIOWrapper(topicEntry);
            mainTopics.put(topic.getId(), topic);
        }
        if(exportSubTopics){
            input = JSONIOWrapper.LoadJSON(subTopicsFile);
            subTopicsMetadata = (JSONObject) input.get("metadata");
            subTopics = new ConcurrentHashMap<>();
            for(JSONObject topicEntry: (Iterable<JSONObject>) input.get("topics")){
                TopicIOWrapper topic = new TopicIOWrapper(topicEntry);
                subTopics.put(topic.getId(), topic);
            }
        }
        System.out.println("Data Loaded!");
    }

    private void BuildModelData(){
        System.out.println("Building model ...");
        for(Map.Entry<String, TopicIOWrapper> t: mainTopics.entrySet()){
            TopicIOWrapper topic = t.getValue();
            for(TopicIOWrapper.JSONTopicWeight d: topic.getDocs()){
                DocIOWrapper doc = documents.get(d.ID);
                for(String key: docFields){
                    d.addDataEntry(key, doc.getData(key));
                }
                d.addDataEntry("wordCount", Integer.toString(doc.getNumLemmas()));
            }
        }
        if(exportSubTopics){
            for(Map.Entry<String, TopicIOWrapper> t: subTopics.entrySet()){
                TopicIOWrapper topic = t.getValue();
                for(TopicIOWrapper.JSONTopicWeight d: topic.getDocs()){
                    DocIOWrapper doc = documents.get(d.ID);
                    for(String key: docFields){
                        d.addDataEntry(key, doc.getData(key));
                    }
                }
            }
        }
        System.out.println("Model Built!");
    }

    private void SaveModel(){
        JSONObject root = new JSONObject();
        for(Map.Entry<String, Object> m: JSONIOWrapper.getJSONObjectMap(documentsMetadata).entrySet()){
            mainTopicsMetadata.put(m.getKey(), m.getValue());
        }
        root.put("metadata", mainTopicsMetadata);
        JSONArray topics = new JSONArray();
        for(Map.Entry<String, TopicIOWrapper> t: mainTopics.entrySet()){
            topics.add(t.getValue().toJSON());
        }
        root.put("topics", topics);
        JSONIOWrapper.SaveJSON(root, mainOutput);
        if(exportSubTopics){
            root = new JSONObject();
            for(Map.Entry<String, Object> m: JSONIOWrapper.getJSONObjectMap(documentsMetadata).entrySet()){
                subTopicsMetadata.put(m.getKey(), m.getValue());
            }
            root.put("metadata", subTopicsMetadata);
            topics = new JSONArray();
            for(Map.Entry<String, TopicIOWrapper> t: subTopics.entrySet()){
                topics.add(t.getValue().toJSON());
            }
            root.put("topics", topics);
            JSONIOWrapper.SaveJSON(root, subOutput);
        }
    }

}
