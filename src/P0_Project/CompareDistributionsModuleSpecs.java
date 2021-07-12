package P0_Project;

import PX_Data.JSONIOWrapper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Class reading and validating parameters for the Compare Distribution module
 * ({@link P4_Analysis.TopicDistribution.CompareDistributions}).
 *
 * @author P. Le Bras
 * @version 1
 */
public class CompareDistributionsModuleSpecs {

    /** Filename for main topics, containing the distribution to compare
     * (from {@link P4_Analysis.TopicDistribution.TopicDistribution}). */
    public String mainTopics;
    /** Filename for sub topics, containing the distribution to compare
     * (from {@link P4_Analysis.TopicDistribution.TopicDistribution}),
     * optional, defaults to "". */
    public String subTopics = "";
    /** Flag for comparing sub topics, defaults to false if subTopics = "". */
    public boolean compareSubTopics = false;
    /** Filename for main topics to compare distribution against. */
    public String previousMainTopics;
    /** Filename for sub topics to compare against, only required if subTopics = "". */
    public String previousSubTopics;
    /** List of distribution ids to compare. */
    public String[] distributions;
    /** Filename for distribution comparison output between main topics, optional, defaults to "". */
    public String mainOutput = "";
    /** Flag to output the distribution comparison between main topics, defaults to false if mainOutput = "". */
    public boolean outputMain = false;
    /** Filename for distribution comparison output between sub topics,
     * optional, defaults to "", only required if subTopics != "". */
    public String subOutput = "";
    /** Flag to output the distribution comparison between sub topics,
     * defaults to false if subOutput = "", or subTopics = "". */
    public boolean outputSub = false;
    /** Filename for distribution comparison output between all topics, optional, defaults to "". */
    public String output = "";
    /** Flag to output the distribution comparison between all topics, defaults to false if output = "". */
    public boolean outputAll = false;
    /** Number of words to identify a topic in CSV outputs, optional, defaults to 3. */
    public int numWordId = 3;

    /**
     * Constructor: parses and validates the given JSON object to set parameters.
     * @param specs JSON object attached to "compareDistributions" in project file.
     * @param metaSpecs Meta-parameter specifications.
     */
    public CompareDistributionsModuleSpecs(JSONObject specs, MetaSpecs metaSpecs){
        mainTopics = metaSpecs.getDataDir() + specs.getOrDefault("mainTopics", specs.get("topics"));
        previousMainTopics = metaSpecs.getSourceDir() + specs.getOrDefault("previousMainTopics", specs.get("previousTopics"));
        subTopics = (String) specs.getOrDefault("subTopics", "");
        if(subTopics.length() > 0){
            compareSubTopics = true;
            subTopics = metaSpecs.getDataDir() + subTopics;
            previousSubTopics = metaSpecs.getSourceDir() + specs.get("previousSubTopics");
        }
        distributions = JSONIOWrapper.getStringArray((JSONArray) specs.getOrDefault("distributions", new JSONArray()));
        numWordId = Math.toIntExact((long) specs.getOrDefault("numWordId", (long) 3));
        mainOutput = (String) specs.getOrDefault("mainOutput", "");
        if(mainOutput.length() > 0){
            mainOutput = metaSpecs.getDataDir() + mainOutput;
            outputMain = true;
        }
        if(compareSubTopics){
            subOutput = (String) specs.getOrDefault("subOutput", "");
            if(subOutput.length() > 0){
                subOutput = metaSpecs.getDataDir() + subOutput;
                outputSub = true;
            }
        }
        output = (String) specs.getOrDefault("output", "");
        if(output.length() > 0){
            output = metaSpecs.getDataDir() + output;
            outputAll = true;
        }
    }
}
