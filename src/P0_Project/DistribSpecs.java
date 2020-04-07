package P0_Project;

import org.json.simple.JSONObject;

public class DistribSpecs {

    public String fieldName;
    public String fieldSeparator = ""; // "" = no separation
    public String output = ""; // "" = save in topic files
    public int topPerTopic = -1; // -1 = all
    public String valueField = ""; // "" = use 1.0

    public DistribSpecs(JSONObject specs){
        fieldName = (String) specs.get("fieldName");
        fieldSeparator = (String) specs.getOrDefault("fieldSeparator", "");
        output = (String) specs.getOrDefault("output", "");
        topPerTopic = Math.toIntExact((long) specs.getOrDefault("topPerTopic", (long) -1));
        valueField = (String) specs.getOrDefault("valueField", "");
    }

}
