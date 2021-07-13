package P5_TopicMapping.BubbleMapping;

import P0_Project.TopicMappingModuleSpecs;
import PX_Data.JSONIOWrapper;
import PY_Helper.LogPrint;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.Arrays;

/**
 * Class coordinating the mapping of topics (distributed and clustered) into a bubble map.
 *
 * @author P. Le Bras
 * @version 1
 */
public class BubbleMap {

    /** Filename with the list of main topics, as output by {@link P4_Analysis.TopicClustering} module. */
    private String mainTopicsFile;
    /** Filename for the output of the main map data. */
    private String mainOutput;
    /** Distribution key to use to estimate the size of bubbles. */
    private String bubbleSize;
    /** Boundaries within which to scale the bubble sizes, [min, max]. */
    private int[] bubbleScale;
    /** Target size for the layout, [width, height]. */
    private int[] targetSize;
    /** Base padding value between bubbles and between a cluster and its border. */
    private int padding;
    /** Curvature value for tangent/convex border arc sections. */
    private int curvature;
    /** Flag indicating if sub topic groups should also be mapped. */
    private boolean mapSubTopics;
    /** Filename with the list of sub topics, as output by {@link P4_Analysis.TopicClustering} module. */
    private String subTopicsFile;
    /** Filename for the output of the sub maps data. */
    private String subOutput;

    /** Topic data read from mainTopicsFile. */
    private JSONObject mainTopicsData;
    /** Topic data read from subTopicsFile. */
    private JSONObject subTopicsData;

    /**
     * Main method, reads the specification and launches the sub-methods in order.
     * @param mapSpecs Specifications.
     * @return String indicating the time taken to map topics.
     */
    public static String MapTopics(TopicMappingModuleSpecs mapSpecs){

        LogPrint.printModuleStart("Bubble map");

        long startTime = System.currentTimeMillis();

        BubbleMap startClass = new BubbleMap();
        startClass.ProcessArguments(mapSpecs);
        startClass.LoadTopics();
        startClass.StartMapping();

        long timeTaken = (System.currentTimeMillis() - startTime) / (long)1000;

        LogPrint.printModuleEnd("Bubble map");

        return "Topic mapping (bubbles): "+Math.floorDiv(timeTaken, 60) + " m, " + timeTaken % 60 + " s";

    }

    /**
     * Method processing the specification parameters.
     * @param mapSpecs Specification object.
     */
    private void ProcessArguments(TopicMappingModuleSpecs mapSpecs){
        LogPrint.printNewStep("Processing arguments", 0);
        mainTopicsFile = mapSpecs.mainTopics;
        mainOutput = mapSpecs.mainOutput;
        bubbleSize = mapSpecs.bubbleSize;
        bubbleScale = mapSpecs.bubbleScale;
        targetSize = mapSpecs.targetSize;
        padding = mapSpecs.padding;
        curvature = mapSpecs.curvature;
        mapSubTopics = mapSpecs.mapSubTopics;
        if(mapSubTopics){
            subTopicsFile = mapSpecs.subTopics;
            subOutput = mapSpecs.subOutput;
        }
        LogPrint.printCompleteStep();
        if(mapSubTopics) LogPrint.printNote("Mapping sub topics");
        LogPrint.printNote("Using "+bubbleSize+" distribution for bubble sizes");
        LogPrint.printNote("Using "+ Arrays.toString(bubbleScale) +" scale for bubble sizes");
        LogPrint.printNote("Padding set to "+padding+" and curvature to "+curvature);
    }

    /**
     * Method loading the topic data from the topic files.
     */
    private void LoadTopics(){
        LogPrint.printNewStep("Loading data", 0);
        mainTopicsData = JSONIOWrapper.LoadJSON(mainTopicsFile, 1);
        if(mapSubTopics){
            subTopicsData = JSONIOWrapper.LoadJSON(subTopicsFile, 1);
        }
    }

    /**
     * Method launching the mapping processes.
     */
    private void StartMapping(){
        mappingMainTopics();
        if(mapSubTopics){
            mappingSubTopics();
        }
    }

    /**
     * Mapping process for the main topics.
     */
    private void mappingMainTopics(){
        LogPrint.printNewStep("Mapping main topics:", 0);
        JSONArray topics = (JSONArray) mainTopicsData.get("topics");
        JSONArray linkageTable = (JSONArray) mainTopicsData.get("linkageTable");
        JSONObject mapData = mapTopics(topics, linkageTable, 1);
        JSONIOWrapper.SaveJSON(mapData, mainOutput, 1);
    }

    /**
     * Mapping process for the sub topics.
     */
    private void mappingSubTopics(){
        LogPrint.printNewStep("Mapping sub topics:", 0);
        JSONArray groups = (JSONArray) subTopicsData.get("subTopicGroups");
        JSONArray maps = new JSONArray();
        for(JSONObject group: JSONIOWrapper.getJSONObjectArray(groups)){
            String mainTopic = (String) group.get("mainTopicId");
            JSONArray topics = (JSONArray) group.get("topics");
            JSONArray linkageTable = (JSONArray) group.get("linkageTable");
            LogPrint.printNewStep("Sub map for main topic "+mainTopic+":", 1);
            JSONObject mapData = mapTopics(topics, linkageTable, 2);
            JSONObject map = new JSONObject();
            map.put("subMap", mapData);
            map.put("mainTopicId", mainTopic);
            maps.add(map);
        }
        JSONIOWrapper.SaveJSONArray(maps, subOutput, 1);
    }

    /**
     * Method mapping a group of topics.
     * @param topics The list of topics to map, in JSON format.
     * @param linkageTable The topics' similarity linkage table, in JSON format.
     * @param depth Depth information for the logs.
     * @return The computed map data, in JSON format: {topics:[...],bubbleMapBorder:[...]}.
     */
    private JSONObject mapTopics(JSONArray topics, JSONArray linkageTable, int depth){
        JSONObject res = new JSONObject();
        if(topics.isEmpty()){
            LogPrint.printNote("Empty topic group, returning empty map.", depth);
            res.put("topics", new JSONArray());
            res.put("bubbleMapBorder", new JSONArray());
        } else {
            LogPrint.printNewStep("Building the hierarchy", depth);
            BubbleNode root = BubbleNode.buildHierarchy(topics, linkageTable);
            root.setValuesAndNormaliseSizes(bubbleSize, bubbleScale[0], bubbleScale[1]);
            LogPrint.printCompleteStep();
            LogPrint.printNewStep("Laying out bubbles", depth);
            BubblePack.layoutHierarchy(root, padding, targetSize[0], targetSize[1]);
            JSONArray bubbles = root.getLeavesJSON();
            LogPrint.printCompleteStep();
            LogPrint.printNewStep("Calculating borders", depth);
            JSONArray borders = BubbleBorder.borderHierarchy(root, padding, curvature);
            LogPrint.printCompleteStep();
            res.put("topics", bubbles);
            res.put("bubbleMapBorder", borders);
        }
        return res;
    }
}
