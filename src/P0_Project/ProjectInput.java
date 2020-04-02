package P0_Project;

import PX_Data.JSONIOWrapper;
import org.json.simple.JSONObject;

import java.util.HashMap;

public class ProjectInput implements ModuleSpecs{
    public String module;
    public String source;
    public HashMap<String, String> fields;
    public String output;

    public void getSpecs(JSONObject specs){
        module = (String) specs.get("module");
        source = (String) specs.get("source");
        fields = JSONIOWrapper.getStringMap((JSONObject) specs.get("fields"));
        output = (String) specs.get("output");
    }
}