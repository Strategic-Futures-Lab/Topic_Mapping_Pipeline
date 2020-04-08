package P0_Project;

import org.json.simple.JSONObject;

/**
 * Class for a Distribution specification
 */
public class DistribSpecs {

    /** Name of field in docData to get distribution for */
    public String fieldName;
    /** Separator to use on field to get unique field values
     * (if more than one value stored in a document),
     * optional, defaults to "": no separation */
    public String fieldSeparator = ""; // "" = no separation
    /** Filename of JSON distribution file to generate, optional, defaults to "": save in topic data */
    public String output = ""; // "" = save in topic files
    /** Maximum number of unique value weights to output, optional, defaults to -1: all values,
     * if sets to 0: nothing saved but the total topic weight */
    public int topPerTopic = -1; // -1 = all
    /** Name of field in docData to weight distribution, has to be numerical values,
     * optional, defaults to "": use 1.0 (document count) */
    public String valueField = ""; // "" = use 1.0

    /**
     * Constructor: reads the specification from a JSON object passed from TopicDistribModuleSpecs
     * @param specs JSON object where specifications are written
     */
    public DistribSpecs(JSONObject specs){
        fieldName = (String) specs.get("fieldName");
        fieldSeparator = (String) specs.getOrDefault("fieldSeparator", "");
        output = (String) specs.getOrDefault("output", "");
        topPerTopic = Math.toIntExact((long) specs.getOrDefault("topPerTopic", (long) -1));
        valueField = (String) specs.getOrDefault("valueField", "");
    }

}
