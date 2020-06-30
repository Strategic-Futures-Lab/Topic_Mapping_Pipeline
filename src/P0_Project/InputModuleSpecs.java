package P0_Project;

import PX_Data.JSONIOWrapper;
import org.json.simple.JSONObject;

import java.util.HashMap;

/**
 * Class for Input module project specification
 */
public class InputModuleSpecs{

    /** Which module to run (eg: csv or txt) */
    // 08/04/2020 Notes : only "csv" supported
    public String module;
    /** Source file or directory*/
    public String source;
    /** Fields to keep as docData, only works if data from csv or json
     * Eg: ("title", "Title") will lookup "Title" in source
     * and save it has "title" in the docData */
    public HashMap<String, String> fields;
    /** Filename for the JSON corpus file generated */
    public String output;
    /** used by pdf intput */
    public int wordsPerDoc;

    /**
     * Constructor: reads the specification from the "input" entry in the project file
     * @param specs JSON object attached to "input"
     */
    public InputModuleSpecs(JSONObject specs, MetaSpecs metaSpecs){
        module = (String) specs.get("module");
        source = metaSpecs.getSourceDir() + (String) specs.get("source");
        fields = JSONIOWrapper.getStringMap((JSONObject) specs.get("fields"));
        output = metaSpecs.getDataDir() + (String) specs.get("output");
        if(module.equals("PDF")){
            wordsPerDoc = Math.toIntExact((long) specs.getOrDefault("wordsPerdoc", 100));
        }
    }
}