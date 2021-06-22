package P0_Project;

import PX_Data.JSONIOWrapper;
import org.json.simple.JSONObject;

import java.util.HashMap;

/**
 * Class reading an validating parameters for the Input modules ({@link P1_Input}).
 *
 * @author P. Le Bras
 * @version 1
 */
public class InputModuleSpecs{

    /** Which module to run: CSV, PDF, GTR or TXT. */
    public String module;
    /** Source file name or directory name. */
    public String source;
    /** Fields to keep as docData, only works if data from CSV or GTR.
     * Eg: ("t", "Title") will lookup "Title" in source and save it has "t" in the docData. */
    public HashMap<String, String> fields;
    /** Fields to lookup in external source (eg server API), only works if data from GTR.
     * Eg: ("t", "Title") will lookup "Title" (as implemented in the input module) and save it has "t" in the docData. */
    public HashMap<String, String> GTR_fields;
    /** Field name containing the GtR project id, only works if data from GtR, defaults to 'ProjectId'.
     * Will be automatically added to the docData. */
    public String GTR_PID = "ProjectId";
    /** Filename for the JSON corpus file generated. */
    public String output;
    /** Number of words limit before splitting a document, only works for PDF or TXT input. */
    public int wordsPerDoc;
    /** Flag for splitting TXT input with every empty line. */
    public boolean TXT_splitEmptyLines = false;

    /**
     * Constructor: reads the specification from the "input" entry in the project file.
     * @param specs JSON object attached to "input".
     */
    public InputModuleSpecs(JSONObject specs, MetaSpecs metaSpecs){
        module = (String) specs.get("module");
        source = metaSpecs.getSourceDir() + (String) specs.get("source");
        output = metaSpecs.getDataDir() + (String) specs.get("output");
        if(module.equals("PDF") || module.equals("TXT")) {
            wordsPerDoc = Math.toIntExact((long) specs.getOrDefault("wordsPerDoc", (long) -1));
        }
        if(module.equals("CSV") || module.equals("GTR")) {
            fields = JSONIOWrapper.getStringMap((JSONObject) specs.get("fields"));
        }
        if(module.equals("GTR")) {
            GTR_fields = JSONIOWrapper.getStringMap((JSONObject) specs.get("GtR_fields"));
            GTR_PID = (String) specs.getOrDefault("GtR_id", "ProjectId");
        }
        if(module.equals("TXT")) {
            TXT_splitEmptyLines = (boolean) specs.getOrDefault("txt_splitEmptyLines", false);
        }
    }
}