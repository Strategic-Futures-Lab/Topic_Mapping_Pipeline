package P5_TopicMapping;

import P0_Project.OverwriteMapModuleSpecs;
import PX_Data.JSONIOWrapper;
import PX_Data.TopicIOWrapper;
import PY_Helper.LogPrint;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

public class OverwriteMap {

    private String mainDistribFile;
    private String mainMapFile;
    private String mainMapOutput;
    private boolean overwriteSubMaps;
    private String subDistribFile;
    private String subMapsFile;
    private String subMapsOutput;

    private boolean overwriteSize;
    private String sizeName;
    private boolean overwriteLabels;

    private ConcurrentHashMap<String, TopicIOWrapper> mainTopics;
    private ConcurrentHashMap<String, TopicIOWrapper> subTopics;
    private JSONObject mainMap;
    private JSONArray subMaps;

    public static String Overwrite(OverwriteMapModuleSpecs specs){
        LogPrint.printModuleStart("Overwrite Map");

        long startTime = System.currentTimeMillis();

        OverwriteMap startClass = new OverwriteMap();
        startClass.ProcessArguments(specs);
        startClass.LoadData();
        startClass.OverwriteMaps();
        startClass.SaveMaps();
        // startClass.OverwriteTopics();

        long timeTaken = (System.currentTimeMillis() - startTime) / (long)1000;

        LogPrint.printModuleEnd("Overwrite Map");

        return "Overwriting Map: "+Math.floorDiv(timeTaken, 60) + " m, " + timeTaken % 60 + " s";
    }

    private void ProcessArguments(OverwriteMapModuleSpecs specs){
        LogPrint.printNewStep("Processing arguments", 0);
        mainDistribFile = specs.mainDistribFile;
        mainMapFile = specs.mainMapFile;
        mainMapOutput = specs.mainMapOutput;
        overwriteSubMaps = specs.overwriteSubMaps;
        if(overwriteSubMaps){
            subDistribFile = specs.subDistribFile;
            subMapsFile = specs.subMapsFile;
            subMapsOutput = specs.subMapsOutput;
        }
        overwriteSize = specs.overwriteSize;
        if(overwriteSize){
            sizeName = specs.sizeName;
        }
        overwriteLabels = specs.overwriteLabels;
        LogPrint.printCompleteStep();
        LogPrint.printNote("Overwriting "+mainMapFile+" with "+mainDistribFile);
        if(overwriteSubMaps) LogPrint.printNote("Overwriting "+subMapsFile+" with "+subDistribFile);
        if(overwriteSize) LogPrint.printNote("Overwriting topics sizes with distribution "+sizeName);
        if(overwriteLabels) LogPrint.printNote("Overwriting topic labels");
    }

    private void LoadData(){
        LogPrint.printNewStep("Loading data", 0);
        mainTopics = loadTopicData(mainDistribFile);
        mainMap = JSONIOWrapper.LoadJSON(mainMapFile, 1);
        if(overwriteSubMaps){
            subTopics = loadTopicData(subDistribFile);
            subMaps = JSONIOWrapper.LoadJSONArray(subMapsFile, 1);
        }
    }

    private ConcurrentHashMap<String, TopicIOWrapper> loadTopicData(String filename){
        JSONObject input = JSONIOWrapper.LoadJSON(filename, 1);
        ConcurrentHashMap<String, TopicIOWrapper> topics = new ConcurrentHashMap<>();
        for(JSONObject topicEntry: (Iterable<JSONObject>) input.get("topics")){
            TopicIOWrapper topic = new TopicIOWrapper(topicEntry);
            topics.put(topic.getId(), topic);
        }
        return topics;
    }

    private void OverwriteMaps(){
        LogPrint.printNewStep("Overwriting map data", 0);
        overwriteMainMap();
        if(overwriteSubMaps) overwriteSubMaps();
        LogPrint.printCompleteStep();
    }

    private void overwriteMainMap(){
        overwriteMap(mainMap, mainTopics);
    }

    private void overwriteSubMaps(){
        JSONObject[] maps = JSONIOWrapper.getJSONObjectArray(subMaps);
        JSONArray newSubMaps = new JSONArray();
        for(JSONObject map: maps){
            JSONObject mapEntry = new JSONObject();
            mapEntry.put("mainTopicId", (String) map.get("mainTopicId"));
            mapEntry.put("subMap", overwriteMap((JSONObject) map.get("subMap"), subTopics));
            newSubMaps.add(mapEntry);
        }
        subMaps = newSubMaps;
    }

    private JSONObject overwriteMap(JSONObject map, ConcurrentHashMap<String, TopicIOWrapper> listOfTopics){
        JSONObject[] topics = JSONIOWrapper.getJSONObjectArray((JSONArray) map.get("topics"));
        JSONArray newTopics = new JSONArray();
        for(JSONObject topic: topics){
            newTopics.add(overwriteTopic(topic, listOfTopics));
        }
        map.put("topics", newTopics);
        return map;
    }

    private JSONObject overwriteTopic(JSONObject topic, ConcurrentHashMap<String, TopicIOWrapper> listOfTopics){
        JSONObject newTopic = (JSONObject) topic.clone();
        String topicId = (String) topic.get("topicId");
        TopicIOWrapper topicEntry = listOfTopics.get(topicId);
        if(overwriteSize){
            try{
                newTopic.put("size", topicEntry.findTotal(sizeName).weight);
            } catch (NoSuchElementException e){
                LogPrint.printNoteError("Error: could not find distribution "+sizeName+" in topics");
                System.exit(1);
            }
        }
        if(overwriteLabels){
            JSONArray wordArray = new JSONArray();
            for(int w = 0; w < topicEntry.getWords().size(); w++){
                wordArray.add(topicEntry.getWords().get(w).toJSON("label"));
            }
            newTopic.put("topWords", wordArray);
        }
        return newTopic;
    }

    private void SaveMaps(){
        LogPrint.printNewStep("Saving new maps", 0);
        JSONIOWrapper.SaveJSON(mainMap, mainMapOutput, 1);
        if(overwriteSubMaps) JSONIOWrapper.SaveJSONArray(subMaps, subMapsOutput, 1);
    }
}
