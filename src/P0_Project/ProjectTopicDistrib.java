package P0_Project;

import PX_Data.JSONIOWrapper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ProjectTopicDistrib implements ModuleSpecs {

    public String documents;
    public String mainTopics;
    public boolean distributeSubTopics = false;
    public String subTopics;
    public List<DistribSpecs> fields;

    public void getSpecs(JSONObject specs){
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
