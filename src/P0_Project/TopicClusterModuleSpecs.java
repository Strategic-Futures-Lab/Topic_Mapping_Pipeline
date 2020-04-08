package P0_Project;

import org.json.simple.JSONObject;

/**
 * Class for Topic Cluster module project specification
 */
public class TopicClusterModuleSpecs {

    /** Filename to main topic data (from Topic Model module) */
    public String mainTopics;
    /** Filename for the JSON main topic file generated */
    public String mainOutput;
    /** Linkage method to create cluster hierarchy: "min", "max", or "avg",
     * optional, defaults to "avg" */
    public String linkageMethod;
    /** Filename to sub topic data (from Topic Model module), optional, defaults to "" */
    public String subTopics;
    /** Flag for grouping and clustering sub topics, defaults to false if subTopics = "" */
    public boolean clusterSubTopics = false;
    /** Filename for the JSON sub topic file generated, only required if subTopics not empty */
    public String subOutput;

    /**
     * Constructor: reads the specification from the "clusterTopics" entry in the project file
     * @param specs JSON object attached to "clusterTopics"
     */
    public TopicClusterModuleSpecs(JSONObject specs){
        mainTopics = (String) specs.get("mainTopics");
        mainOutput = (String) specs.get("mainOutput");
        linkageMethod = (String) specs.getOrDefault("linkageMethod", "avg"); // max | min | avg
        subTopics = (String) specs.getOrDefault("subTopics", "");
        clusterSubTopics = subTopics.length() > 0;
        if(clusterSubTopics){
            subOutput = (String) specs.get("subOutput");
        }
    }
}
