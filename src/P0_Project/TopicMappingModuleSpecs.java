package P0_Project;

import PX_Data.JSONIOWrapper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class TopicMappingModuleSpecs {

    /** Filename to main topic data (from Topic Model module) */
    public String mainTopics;
    /** Filename for the JSON main topic map file generated */
    public String mainOutput;
    /** Map type: "bubble", or "hex",
     * optional, defaults to "bubble" */
    public String mapType;
    /** Bubble size accessor in topics totals, only required for bubble map,
     * defaults to "-" (global distribution total) */
    public String bubbleSize;
    /** Bubble scale boundaries, only for bubble map, optional, defaults to [5, 40] */
    public int[] bubbleScale; // = new int[]{5, 40};
    /** Filename to sub topic data (from Topic Model module), optional, defaults to "" */
    public String subTopics;
    /** Flag for mapping sub topics, defaults to false if subTopics = "" */
    public boolean mapSubTopics = false;
    /** Filename for the JSON sub topic maps file generated, only required if subTopics not empty */
    public String subOutput;
    /** Custom command name for NodeJS, optional, defaults to "node" */
    public String nodeCommand = "node";

    /**
     * Constructor: reads the specification from the "mapTopics" entry in the project file
     * @param specs JSON object attached to "mapTopics"
     */
    public TopicMappingModuleSpecs(JSONObject specs, MetaSpecs metaSpecs){
        mainTopics = metaSpecs.getDataDir() + (String) specs.get("mainTopics");
        mainOutput = metaSpecs.getOutputDir() + (String) specs.get("mainOutput");
        mapType = (String) specs.getOrDefault("linkageMethod", "bubble"); // bubble | hex
        bubbleSize = (String) specs.getOrDefault("bubbleSize", "-");
        bubbleScale = specs.containsKey("bubbleScale") ?
                JSONIOWrapper.getIntArray((JSONArray) specs.get("bubbleScale")) :
                new int[]{5,40};
        subTopics = (String) specs.getOrDefault("subTopics", "");
        mapSubTopics = metaSpecs.useMetaModelType() ? metaSpecs.doHierarchical() : subTopics.length() > 0;
        if(mapSubTopics){
            subTopics = metaSpecs.getDataDir() + subTopics;
            subOutput = metaSpecs.getOutputDir() + (String) specs.get("subOutput");
        }
        nodeCommand = (String) specs.getOrDefault("nodeCommand", "node");
    }
}
