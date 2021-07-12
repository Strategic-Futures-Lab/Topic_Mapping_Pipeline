package P0_Project;

import org.json.simple.JSONObject;

/**
 * Class reading an validating parameters for the Overwrite Map module ({@link P5_TopicMapping.OverwriteMap}).
 *
 * @author P. Le Bras
 * @version 1
 */
public class OverwriteMapModuleSpecs {

    /** Filename of the main topic JSON file
     * (from {@link P3_TopicModelling.TopicModelling} or {@link P3_TopicModelling.InferDocuments}),
     * with {@link P4_Analysis.TopicDistribution.TopicDistribution} information,
     * from which to overwrite the main map. */
    public String mainDistribFile;
    /** Filename of the sub topic JSON file
     * (from {@link P3_TopicModelling.TopicModelling} or {@link P3_TopicModelling.InferDocuments}),
     * with {@link P4_Analysis.TopicDistribution.TopicDistribution} information,
     * from which to overwrite the sub maps, optional, defaults to "". */
    public String subDistribFile = "";
    /** Flag for overwriting the sub maps, defaults to false if subDistribFile = "". */
    public boolean overwriteSubMaps = false;
    /** Filename of the main map JSON file to overwrite. */
    public String mainMapFile;
    /** Filename of the JSON sub maps JSON file to overwrite, only required if subDistribFile != "". */
    public String subMapsFile = "";
    /** Name of distribution to use if overwriting the size value of topics, optional, defaults to "". */
    public String sizeName = "";
    /** Flag for overwriting the size value of topics, defaults to false if sizeName = "". */
    public boolean overwriteSize = false;
    /** Flag for overwriting the labels of topics, optional, defaults to false. */
    public boolean overwriteLabels = false;
    /** Filename of the main map JSON file to save after edit. */
    public String mainMapOutput;
    /** Filename of the sub maps JSON file to save after edit, only required if subDistribFile != "". */
    public String subMapsOutput = "";

    /**
     * Constructor: parses and validates the given JSON object to set parameters.
     * @param specs JSON object attached to "overwriteMap" in project file.
     * @param metaSpecs Meta-parameter specifications.
     */
    public OverwriteMapModuleSpecs(JSONObject specs, MetaSpecs metaSpecs){
        mainDistribFile = metaSpecs.getDataDir() + specs.getOrDefault("mainDistribution", specs.get("distribution"));
        mainMapFile = metaSpecs.getSourceDir() + specs.getOrDefault("mainMap", specs.get("map"));
        mainMapOutput = metaSpecs.getOutputDir() + specs.getOrDefault("mainMapOutput", specs.get("output"));
        subDistribFile = (String) specs.getOrDefault("subDistribution", "");
        if(subDistribFile.length() > 0){
            subDistribFile = metaSpecs.getDataDir() + subDistribFile;
            subMapsFile = metaSpecs.getSourceDir() + specs.get("subMaps");
            subMapsOutput = metaSpecs.getOutputDir() + specs.get("subMapsOutput");
            overwriteSubMaps = true;
        }
        sizeName = (String) specs.getOrDefault("sizeName", "");
        overwriteSize = sizeName.length() > 0;
        overwriteLabels = (boolean) specs.getOrDefault("overwriteLabels", false);
    }
}
