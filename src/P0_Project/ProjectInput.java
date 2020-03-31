package P0_Project;

import PX_Helper.JSONIOWrapper;
import org.json.simple.JSONObject;

import java.util.HashMap;

public class ProjectInput{
    public String module;
    public String source;
    public HashMap<String, String> fields;
    public String output;

    public void getInputSpecs(JSONObject specs){
        module = (String) specs.get("module");
        source = (String) specs.get("source");
        fields = JSONIOWrapper.getStringMap((JSONObject) specs.get("fields"));
        output = (String) specs.get("output");
    }
}