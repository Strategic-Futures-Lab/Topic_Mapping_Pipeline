package P0_Project;

import PX_Data.JSONIOWrapper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Class reading an validating parameters for the Topic Mapping module ({@link P5_TopicMapping}).
 *
 * @author P. Le Bras
 * @version 1
 */
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
    public int[] bubbleScale;
    /** Target size for the map, only for bubble map, optional, defaults to [1000, 1000] */
    public int[] targetSize;
    /** Base padding value between bubbles and with border, defaults to 1 */
    public int padding;
    /** Curvature of tangent/convex border, defaults to 8 */
    public int curvature;
    /** Filename to sub topic data (from Topic Model module), optional, defaults to "" */
    public String subTopics;
    /** Flag for mapping sub topics, defaults to false if subTopics = "" */
    public boolean mapSubTopics = false;
    /** Filename for the JSON sub topic maps file generated, only required if subTopics not empty */
    public String subOutput;

    /** Custom command name for NodeJS, optional, defaults to "node",
     * @deprecated */
    @Deprecated
    public String nodeCommand = "node";

    /**
     * Constructor: reads the specification from the "mapTopics" entry in the project file
     * @param specs JSON object attached to "mapTopics"
     */
    public TopicMappingModuleSpecs(JSONObject specs, MetaSpecs metaSpecs){
        mainTopics = metaSpecs.getDataDir() + (String) specs.get("mainTopics");
        mainOutput = metaSpecs.getOutputDir() + (String) specs.get("mainOutput");
        mapType = (String) specs.getOrDefault("mapType", "bubble"); // bubble | hex
        bubbleSize = (String) specs.getOrDefault("bubbleSize", "-");
        bubbleScale = specs.containsKey("bubbleScale") ?
                JSONIOWrapper.getIntArray((JSONArray) specs.get("bubbleScale")) :
                new int[]{5,40};
        targetSize = specs.containsKey("targetSize") ?
                JSONIOWrapper.getIntArray((JSONArray) specs.get("targetSize")) :
                new int[]{1000,1000};
        padding = Math.toIntExact((long) specs.getOrDefault("bubblePadding", (long) 1));
        curvature = Math.toIntExact((long) specs.getOrDefault("borderCurvature", (long) 8));
        subTopics = (String) specs.getOrDefault("subTopics", "");
        mapSubTopics = metaSpecs.useMetaModelType() ? metaSpecs.doHierarchical() : subTopics.length() > 0;
        if(mapSubTopics){
            subTopics = metaSpecs.getDataDir() + subTopics;
            subOutput = metaSpecs.getOutputDir() + (String) specs.get("subOutput");
        }
        nodeCommand = (String) specs.getOrDefault("nodeCommand", "node");
    }
}
