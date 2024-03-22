package P0_Project;

import PX_Data.JSONIOWrapper;
import PY_Helper.LogPrint;
import org.json.simple.JSONObject;

import java.util.HashMap;

/**
 * Class reading and validating parameters for the Input modules ({@link P1_Input}).
 *
 * @author P. Le Bras
 * @version 1
 */
public class InputModuleSpecs{

    /** Which module to run: CSV, PDF, HTML, GTR or TXT. */
    public String module;
    /** Source file name or directory name. */
    public String source;
    /** Fields to keep as docData, only works if data from CSV, HTML or GTR.
     * Eg: ("t", "Title") will lookup "Title" in source and save it has "t" in the docData. */
    public HashMap<String, String> fields;
    /** Fields to lookup in external source (eg server API), only works if data from GTR,
     * optional, defaults to empty list.
     * Eg: ("t", "Title") will lookup "Title" (as implemented in the input module) and save it has "t" in the docData. */
    public HashMap<String, String> GTR_fields;
    /** Field name containing the GtR project id, only works if data from GtR,
     * optional, defaults to 'ProjectId'.
     * Will be automatically added to the docData. */
    public String GTR_PID = "ProjectId";
    /** Field name containing the HTML urls, only works if data from HTML,
     * optional, defaults to 'URL'.
     * Will be automatically added to the docData. */
    public String HTML_URL = "URL";
    /** Selector from which to parse HTML text, only works if data from HTML,
     * optional, defaults to 'body'. */
    public String HTML_selector = "body";
    /** Filename for the JSON corpus file generated. */
    public String output;
    /** Number of words limit before splitting a document, only works for PDF or TXT input,
     * optional, defaults to -1. */
    public int wordsPerDoc;
    /** Flag for splitting TXT input with every empty line,
     * optional, defaults to false. */
    public boolean TXT_splitEmptyLines = false;

    /**
     * Constructor: parses and validates the given JSON object to set parameters.
     * @param specs JSON object attached to "input" in project file.
     * @param metaSpecs Meta-parameter specifications.
     */
    public InputModuleSpecs(JSONObject specs, MetaSpecs metaSpecs){
        module = (String) specs.get("module");
        source = metaSpecs.getSourceDir() + specs.get("source");
        output = metaSpecs.getDataDir() + specs.get("output");
        if(module.equals("PDF") || module.equals("TXT")) {
            wordsPerDoc = Math.toIntExact((long) specs.getOrDefault("wordsPerDoc", (long) -1));
            if(wordsPerDoc < -1){
                LogPrint.printNote("Input module: wordsPerDoc must be greater than -2, parameter was set to "+wordsPerDoc+", will be set to default: -1 (no document split)");
                wordsPerDoc = -1;
            }
        }
        if(module.equals("CSV") || module.equals("HTML") || module.equals("GTR")) {
            fields = JSONIOWrapper.getStringMap((JSONObject) specs.get("fields"));
        }
        if(module.equals("GTR")) {
            GTR_fields = JSONIOWrapper.getStringMap((JSONObject) specs.getOrDefault("GtR_fields", new JSONObject()));
            GTR_PID = (String) specs.getOrDefault("GtR_id", "ProjectId");
        }
        if(module.equals("HTML")) {
            HTML_URL = (String) specs.getOrDefault("url", "URL");
            HTML_selector = (String) specs.getOrDefault("dom_selector", "body");
        }
        if(module.equals("TXT")) {
            TXT_splitEmptyLines = (boolean) specs.getOrDefault("txt_splitEmptyLines", false);
        }
    }
}