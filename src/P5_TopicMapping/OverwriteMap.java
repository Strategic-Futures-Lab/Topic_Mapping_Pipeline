package P5_TopicMapping;

import P0_Project.OverwriteMapModuleSpecs;
import PX_Data.JSONIOWrapper;
import PX_Data.TopicIOWrapper;
import PY_Helper.LogPrint;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class overwriting existing map files (from other {@link P5_TopicMapping} modules) with new distribution values
 * (from {@link P4_Analysis.TopicDistribution.TopicDistribution}) without changing the mapping information (position,
 * or size).
 * This class should mostly be used in the Document Inference Pipeline.
 *
 * @author P. Le Bras
 * @version 1
 */
public class OverwriteMap {

    /** Filename for JSON Distribution file, from which to overwrite main map. */
    private String mainDistribFile;
    /** Filename for JSON main map file to overwrite. */
    private String mainMapFile;
    /** Filename for JSON main map file to save after edit. */
    private String mainMapOutput;
    /** Flag for overwriting the sub maps. */
    private boolean overwriteSubMaps;
    /** Filename for JSON Distribution file, from which to overwrite sub maps. */
    private String subDistribFile;
    /** Filename for JSON sub maps file to overwrite. */
    private String subMapsFile;
    /** Filename for JSON sub maps file to save after edit, only required if subDistribFile != "" */
    private String subMapsOutput;

    /** Flag for overwriting the size value of topics. */
    private boolean overwriteSize;
    /** Name of distribution to use if overwriting the size value of topics. */
    private String sizeName;
    /** Flag for overwriting the labels of topics. */
    private boolean overwriteLabels;

    /** List of main topics. */
    private ConcurrentHashMap<String, TopicIOWrapper> mainTopics;
    /** List of sub topics. */
    private ConcurrentHashMap<String, TopicIOWrapper> subTopics;
    /** Main map data in JSON format. */
    private JSONObject mainMap;
    /** Sub map data in JSON format. */
    private JSONArray subMaps;

    /**
     * Main method, reads the specification and launches the sub-methods in order.
     * @param specs Specifications.
     * @return String indicating the time taken to overwrite the map files.
     */
    public static String Overwrite(OverwriteMapModuleSpecs specs){
        LogPrint.printModuleStart("Overwrite Map");

        long startTime = System.currentTimeMillis();

        OverwriteMap startClass = new OverwriteMap();
        startClass.ProcessArguments(specs);
        startClass.LoadData();
        startClass.OverwriteMaps();
        startClass.SaveMaps();

        long timeTaken = (System.currentTimeMillis() - startTime) / (long)1000;

        LogPrint.printModuleEnd("Overwrite Map");

        return "Overwriting Map: "+Math.floorDiv(timeTaken, 60) + " m, " + timeTaken % 60 + " s";
    }

    /**
     * Method processing the specification parameters.
     * @param specs Specifications object.
     */
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

    /**
     * Method loading topic data from the new distribution files and existing mapping data from the map files.
     */
    private void LoadData(){
        LogPrint.printNewStep("Loading data", 0);
        mainTopics = loadTopicData(mainDistribFile);
        mainMap = JSONIOWrapper.LoadJSON(mainMapFile, 1);
        if(overwriteSubMaps){
            subTopics = loadTopicData(subDistribFile);
            subMaps = JSONIOWrapper.LoadJSONArray(subMapsFile, 1);
        }
    }

    /**
     * Method reading from a distribution file and loading a list of topics.
     * @param filename Filename where to get the distribution data.
     * @return The list of topics read from the file.
     */
    private ConcurrentHashMap<String, TopicIOWrapper> loadTopicData(String filename){
        JSONObject input = JSONIOWrapper.LoadJSON(filename, 1);
        ConcurrentHashMap<String, TopicIOWrapper> topics = new ConcurrentHashMap<>();
        for(JSONObject topicEntry: (Iterable<JSONObject>) input.get("topics")){
            TopicIOWrapper topic = new TopicIOWrapper(topicEntry);
            topics.put(topic.getId(), topic);
        }
        return topics;
    }

    /**
     * Method launching the overwriting process.
     */
    private void OverwriteMaps(){
        LogPrint.printNewStep("Overwriting map data", 0);
        overwriteMainMap();
        if(overwriteSubMaps) overwriteSubMaps();
        LogPrint.printCompleteStep();
    }

    /**
     * Method overwriting the main map.
     */
    private void overwriteMainMap(){
        overwriteMap(mainMap, mainTopics);
    }

    /**
     * Method overwriting each of the sub maps.
     */
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

    /**
     * Method overwriting a given JSON map data with the provided list of topics.
     * @param map Map JSON data to overwrite.
     * @param listOfTopics List of topics with updated information.
     * @return The updated map JSON data.
     */
    private JSONObject overwriteMap(JSONObject map, ConcurrentHashMap<String, TopicIOWrapper> listOfTopics){
        JSONObject[] topics = JSONIOWrapper.getJSONObjectArray((JSONArray) map.get("topics"));
        JSONArray newTopics = new JSONArray();
        for(JSONObject topic: topics){
            newTopics.add(overwriteTopic(topic, listOfTopics));
        }
        map.put("topics", newTopics);
        return map;
    }

    /**
     * Method overwriting a specific topic (from a map data) in JSON format with information from the provided
     * list of topics.
     * @param topic Topic JSON data to overwrite.
     * @param listOfTopics List of topics with updated information.
     * @return The updated topic JSON data.
     */
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

    /**
     * Method writing the updated map JSON data on file(s).
     */
    private void SaveMaps(){
        LogPrint.printNewStep("Saving new maps", 0);
        JSONIOWrapper.SaveJSON(mainMap, mainMapOutput, 1);
        if(overwriteSubMaps) JSONIOWrapper.SaveJSONArray(subMaps, subMapsOutput, 1);
    }
}
