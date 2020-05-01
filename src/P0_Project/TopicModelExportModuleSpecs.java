package P0_Project;

import PX_Data.JSONIOWrapper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class TopicModelExportModuleSpecs {

    /** Filename to main topic data (from Topic Model module) */
    public String mainTopics;
    /** Filename for the JSON main topic map file generated */
    public String mainOutput;
    /** Filename to sub topic data (from Topic Model module), optional, defaults to "" */
    public String subTopics;
    /** Flag for mapping sub topics, defaults to false if subTopics = "" */
    public boolean exportSubTopics = false;
    /** Filename for the JSON sub topic maps file generated, only required if subTopics not empty */
    public String subOutput;
    /** Filename to the document data (from Topic Model Module) */
    public String documents;
    /** List of fields in docData to keep, optional, defaults to empty  */
    public String[] docFields;

    /**
     * Constructor: reads the specification from the "mapTopics" entry in the project file
     * @param specs JSON object attached to "mapTopics"
     */
    public TopicModelExportModuleSpecs(JSONObject specs){
        mainTopics = (String) specs.get("mainTopics");
        mainOutput = (String) specs.get("mainOutput");
        subTopics = (String) specs.getOrDefault("subTopics", "");
        exportSubTopics = subTopics.length() > 0;
        if(exportSubTopics){
            subOutput = (String) specs.get("subOutput");
        }
        documents = (String) specs.get("documents");
        docFields = JSONIOWrapper.getStringArray((JSONArray) specs.getOrDefault("docFields", new JSONArray()));
    }
}
