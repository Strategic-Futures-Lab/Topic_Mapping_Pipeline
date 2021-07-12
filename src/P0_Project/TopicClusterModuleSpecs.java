package P0_Project;

import org.json.simple.JSONObject;

/**
 * Class reading and validating parameters for the Topic Clustering module
 * ({@link P4_Analysis.TopicClustering.TopicClustering}).
 *
 * @author P. Le Bras
 * @version 1
 */
public class TopicClusterModuleSpecs {

    /** Filename of main topic JSON file (from Topic Model or Document Infer modules). */
    public String mainTopics;
    /** Filename of the main topic cluster JSON file generated. */
    public String mainOutput;
    /** Linkage method to create cluster hierarchy: "min", "max", or "avg", optional, defaults to "avg". */
    public String linkageMethod;
    /** Number of clusters in main topics, optional, defaults to 1. */
    public int clusters;
    /** Filename of sub topic JSON file (from Topic Model or Document Infer modules), optional, defaults to "". */
    public String subTopics;
    /** Flag for grouping and "clustering" sub topics, defaults to false if subTopics = "". */
    public boolean groupingSubTopics = false;
    /** Filename of the sub topic cluster JSON file generated, only required if subTopics != "". */
    public String subOutput;

    /**
     * Constructor: parses and validates the given JSON object to set parameters.
     * @param specs JSON object attached to "clusterTopics" in project file.
     * @param metaSpecs Meta-parameter specifications.
     */
    public TopicClusterModuleSpecs(JSONObject specs, MetaSpecs metaSpecs){
        mainTopics = metaSpecs.getDataDir() + specs.getOrDefault("mainTopics", specs.get("topics"));
        mainOutput = metaSpecs.getDataDir() + specs.getOrDefault("mainOutput", specs.get("output"));
        linkageMethod = (String) specs.getOrDefault("linkageMethod", "avg"); // max | min | avg
        clusters = Math.toIntExact((long) specs.getOrDefault("clusters", (long) 1));
        subTopics = (String) specs.getOrDefault("subTopics", "");
        groupingSubTopics = metaSpecs.useMetaModelType() ? metaSpecs.doHierarchical() : subTopics.length() > 0;
        if(groupingSubTopics){
            subTopics = metaSpecs.getDataDir() + subTopics;
            subOutput = metaSpecs.getDataDir() + specs.get("subOutput");
        }
    }
}
