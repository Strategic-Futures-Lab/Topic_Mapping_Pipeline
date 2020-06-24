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
    /** Filename for the JSON main topic file generated */
    public String mainOutput;
    /** Filename to sub topic data (from Topic Model module), optional, defaults to "" */
    public String subTopics;
    /** Flag for distributing over sub topics, defaults to false if subTopics = "" */
    public boolean distributeSubTopics = false;
    /** Filename for the JSON sub topic file generated, only required if subTopics not empty */
    public String subOutput;
    /** List of specifications for each distribution to calculate */
    public List<DistribSpecs> fields;

    /**
     * Constructor: reads the specification from the "distributeTopics" entry in the project file
     * @param specs JSON object attached to "distributeTopics"
     */
    public TopicDistribModuleSpecs(JSONObject specs, MetaSpecs metaSpecs){
        documents = metaSpecs.getDataDir() + (String) specs.get("documents");
        mainTopics = metaSpecs.getDataDir() + (String) specs.get("mainTopics");
        mainOutput = metaSpecs.getDataDir() + (String) specs.get("mainOutput");
        subTopics = (String) specs.getOrDefault("subTopics", "");
        distributeSubTopics = metaSpecs.useMetaModelType() ? metaSpecs.doHierarchical() : subTopics.length() > 0;
        if(distributeSubTopics){
            subTopics = metaSpecs.getDataDir() + subTopics;
            subOutput = metaSpecs.getDataDir() + (String) specs.get("subOutput");
        }
        fields = new ArrayList<>();
        for(JSONObject field: JSONIOWrapper.getJSONObjectArray((JSONArray) specs.get("distributions"))){
            fields.add(new DistribSpecs(field, metaSpecs));
        };
    }

}
