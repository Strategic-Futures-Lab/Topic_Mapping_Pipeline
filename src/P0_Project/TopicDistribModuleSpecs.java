package P0_Project;

import PX_Data.JSONIOWrapper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Class for Topic Distribution module project specification
 */
public class TopicDistribModuleSpecs {

    /** Filename to document data (from Topic Model module) */
    public String documents;
    /** Filename to main topic data (from Topic Model module) */
    public String mainTopics;
    /** Filename to sub topic data (from Topic Model module), optional, defaults to "" */
    public String subTopics;
    /** Flag for distributing over sub topics, defaults to false if subTopics = "" */
    public boolean distributeSubTopics = false;
    /** List of specifications for each distribution to calculate */
    public List<DistribSpecs> fields;

    /**
     * Constructor: reads the specification from the "distributeTopics" entry in the project file
     * @param specs JSON object attached to "distributeTopics"
     */
    public TopicDistribModuleSpecs(JSONObject specs){
        documents = (String) specs.get("documents");
        mainTopics = (String) specs.get("mainTopics");
        subTopics = (String) specs.getOrDefault("subTopics", "");
        distributeSubTopics = subTopics.length() > 0;
        fields = new ArrayList<>();
        for(JSONObject field: JSONIOWrapper.getJSONObjectArray((JSONArray) specs.get("fields"))){
            fields.add(new DistribSpecs(field));
        };
    }

}
