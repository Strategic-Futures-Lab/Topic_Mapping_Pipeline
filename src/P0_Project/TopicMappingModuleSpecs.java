package P0_Project;

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
    /** Filename to sub topic data (from Topic Model module), optional, defaults to "" */
    public String subTopics;
    /** Flag for mapping sub topics, defaults to false if subTopics = "" */
    public boolean mapSubTopics = false;
    /** Filename for the JSON sub topic maps file generated, only required if subTopics not empty */
    public String subOutput;

    /**
     * Constructor: reads the specification from the "clusterTopics" entry in the project file
     * @param specs JSON object attached to "clusterTopics"
     */
    public TopicMappingModuleSpecs(JSONObject specs){
        mainTopics = (String) specs.get("mainTopics");
        mainOutput = (String) specs.get("mainOutput");
        mapType = (String) specs.getOrDefault("linkageMethod", "bubble"); // bubble | hex
        bubbleSize = (String) specs.getOrDefault("bubbleSize", "-");
        subTopics = (String) specs.getOrDefault("subTopics", "");
        mapSubTopics = subTopics.length() > 0;
        if(mapSubTopics){
            subOutput = (String) specs.get("subOutput");
        }
    }
}
