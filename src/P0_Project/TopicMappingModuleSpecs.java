package P0_Project;

import PX_Data.JSONIOWrapper;
import PY_Helper.LogPrint;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Class reading and validating parameters for the Topic Mapping module ({@link P5_TopicMapping.BubbleMapping.BubbleMap}).
 *
 * @author P. Le Bras
 * @version 1
 */
public class TopicMappingModuleSpecs {

    /** Filename of the main topic JSON file
     * (from {@link P3_TopicModelling.TopicModelling} or {@link P3_TopicModelling.InferDocuments})
     * with {@link P4_Analysis.TopicDistribution.TopicDistribution} (for bubble maps) and
     * {@link P4_Analysis.TopicClustering.TopicClustering} information. */
    public String mainTopics;
    /** Filename of the main topic map JSON file generated. */
    public String mainOutput;
    /** Map type: "bubble", optional, defaults to "bubble". */
    public String mapType;
    /** Bubble size accessor in topics totals, only required for bubble map,
     * defaults to "-" (global distribution total). */
    public String bubbleSize;
    /** Bubble size scale, only for bubble map, optional, defaults to [5, 40]. */
    public int[] bubbleScale;
    /** Target size for the map, only for bubble map, optional, defaults to [1000, 1000]. */
    public int[] targetSize;
    /** Base padding value between bubbles and with border, only for bubble map, defaults to 1. */
    public int padding;
    /** Curvature of tangent/convex border, only for bubble map, defaults to 8. */
    public int curvature;
    /** Filename of the sub topic JSON file
     * (from {@link P3_TopicModelling.TopicModelling} or {@link P3_TopicModelling.InferDocuments})
     * with {@link P4_Analysis.TopicDistribution.TopicDistribution} (for bubble maps) and
     * {@link P4_Analysis.TopicClustering.TopicClustering} information, optional, defaults to "". */
    public String subTopics;
    /** Flag for mapping sub topics, defaults to false if subTopics = "". */
    public boolean mapSubTopics = false;
    /** Filename of the sub topic map JSON file generated, only required if subTopics != "". */
    public String subOutput;

    /** Custom command name for NodeJS, optional, defaults to "node",
     * @deprecated since mapping module has been integrated in Java. */
    @Deprecated
    public String nodeCommand = "node";

    /**
     * Constructor: parses and validates the given JSON object to set parameters.
     * @param specs JSON object attached to "mapTopics" in project file.
     * @param metaSpecs Meta-parameter specifications.
     */
    public TopicMappingModuleSpecs(JSONObject specs, MetaSpecs metaSpecs){
        mainTopics = metaSpecs.getDataDir() + specs.getOrDefault("mainTopics", specs.get("topics"));
        mainOutput = metaSpecs.getOutputDir() + specs.getOrDefault("mainOutput", specs.get("output"));
        mapType = (String) specs.getOrDefault("mapType", "bubble");
        if(mapType.equals("bubble")){
            bubbleSize = (String) specs.getOrDefault("bubbleSize", "-");
            bubbleScale = specs.containsKey("bubbleScale") ?
                    JSONIOWrapper.getIntArray((JSONArray) specs.get("bubbleScale")) :
                    new int[]{5,40};
            targetSize = specs.containsKey("targetSize") ?
                    JSONIOWrapper.getIntArray((JSONArray) specs.get("targetSize")) :
                    new int[]{1000,1000};
            padding = Math.toIntExact((long) specs.getOrDefault("bubblePadding", (long) 1));
            curvature = Math.toIntExact((long) specs.getOrDefault("borderCurvature", (long) 8));

            // validations
            if(bubbleScale[0] < 1){
                LogPrint.printNote("Topic Mapping module: bubbleScale lower bound must be greater than 0, parameter was set to "+bubbleScale[0]+", will be set to 1");
                bubbleScale[0] = 1;
            }
            if(bubbleScale[1] < bubbleScale[0]){
                LogPrint.printNote("Topic Mapping module: bubbleScale upper bound must be greater than lowerBound, parameter was set to "+bubbleScale[1]+", will be set to 2");
                bubbleScale[1] = 2;
            }
            if(targetSize[0] < 100){
                LogPrint.printNote("Topic Mapping module: targetSize width must be greater than 100, parameter was set to "+targetSize[0]+", will be set to 100");
                targetSize[0] = 100;
            }
            if(targetSize[1] < 100){
                LogPrint.printNote("Topic Mapping module: targetSize height must be greater than 100, parameter was set to "+targetSize[1]+", will be set to 100");
                targetSize[1] = 100;
            }
            if(padding < 1){
                LogPrint.printNote("Topic Mapping module: padding must be greater than 0, parameter was set to "+padding+", will be set to default: 1");
                padding = 1;
            }
            if(curvature < 1){
                LogPrint.printNote("Topic Mapping module: curvature must be greater than 0, parameter was set to "+curvature+", will be set to 1");
                curvature = 1;
            }
        }

        subTopics = (String) specs.getOrDefault("subTopics", "");
        mapSubTopics = metaSpecs.useMetaModelType() ? metaSpecs.doHierarchical() : subTopics.length() > 0;
        if(mapSubTopics){
            subTopics = metaSpecs.getDataDir() + subTopics;
            subOutput = metaSpecs.getOutputDir() + specs.get("subOutput");
        }
        nodeCommand = (String) specs.getOrDefault("nodeCommand", "node");

        // validation
        if(!mapType.equals("bubble")){
            LogPrint.printNote("Topic Mapping module: mapType currently only support \"bubble\", parameter was set to \""+mapType+"\", will be set to default: \"bubble\"");
            mapType = "bubble";
        }
    }
}
