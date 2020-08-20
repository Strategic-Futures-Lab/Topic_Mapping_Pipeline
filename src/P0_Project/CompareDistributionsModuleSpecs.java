package P0_Project;

import PX_Data.JSONIOWrapper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class CompareDistributionsModuleSpecs {

    /** Filename for main topics, containing the distribution to compare, from TopicDistrib module */
    public String mainTopics;
    /** Filename for sub topics, containing the distribution to compare, from TopicDistrib module,
     * optional, defaults to "" */
    public String subTopics = "";
    /** Flag for comparing sub topics, defaults to false if subTopics = "" */
    public boolean compareSubTopics = false;
    /** Filename for main topics to compare distribution against */
    public String previousMainTopics;
    /** Filename for sub topics to compare against, only required if subTopics = "" */
    public String previousSubTopics;
    /** List of distribution ids to compare */
    public String[] distributions;
    /** Filename for distrib comparison between main topics, optional, defaults to "" */
    public String mainOutput = "";
    /** Flag to output distrib comparison between main topics, defaults to false if mainOutput = "" */
    public boolean outputMain = false;
    /** Filename for distrib comparison between main topics, optional, defaults to "", only required if subTopics != ""  */
    public String subOutput = "";
    /** Flag to output distrib comparison between sub topics, defaults to false if subOutput = "", or subTopics = "" */
    public boolean outputSub = false;
    /** Filename for distrib comparison between all topics, optional, defaults to "" */
    public String output = "";
    /** Flag to output distrib comparison between all topics, defaults to false if output = "" */
    public boolean outputAll = false;
    /** Number of words to identify a topic in csv outputs,
     * optional, defaults to 3 */
    public int numWordId = 3;

    public CompareDistributionsModuleSpecs(JSONObject specs, MetaSpecs metaSpecs){
        mainTopics = metaSpecs.getDataDir() + (String) specs.get("mainTopics");
        previousMainTopics = metaSpecs.getSourceDir() + (String) specs.get("previousMainTopics");
        subTopics = (String) specs.getOrDefault("subTopics", "");
        if(subTopics.length() > 0){
            compareSubTopics = true;
            subTopics = metaSpecs.getDataDir() + subTopics;
            previousSubTopics = metaSpecs.getSourceDir() + (String) specs.get("previousSubTopics");
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
