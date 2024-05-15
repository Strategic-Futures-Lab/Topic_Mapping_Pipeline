package config.modules;

import config.ProjectConfigParser;
import config.Module;

import java.util.HashMap;

/**
 * Class for parsing and storing HTML corpus input parameters
 */
public class CorpusHTML extends Module {

    private static final String[] MANDATORY_PARAMS = new String[]{"source","output","urlField"};

    /** Filename of the source CSV file */
    public final String source;
    /** Filename of the output corpus JSON file */
    public final String output;
    /** List of CSV fields to store in the  corpus JSON file; key is the name stored in the corpus JSON file,
     * value is the name found in the source CSV file */
    public final HashMap<String, String> fields;
    /** CSV field where the URL of the HTML page can be found */
    public final String urlField;
    /** DOM selector from which to parse text on the HTML file */
    public final String domSelector;

    /**
     * Constructor, parses and stores module parameters
     * @param moduleName Module name as described in the YAML config file
     * @param moduleParams Map of unparsed YAML parameters
     * @throws ProjectConfigParser.ParseException If the configuration does not include all mandatory parameters or if a parameter is not found
     */
    public CorpusHTML(String moduleName, HashMap<String, Object> moduleParams) throws ProjectConfigParser.ParseException{
        super(moduleName, "corpusHTML");
        for(String p: MANDATORY_PARAMS){
            if(!moduleParams.containsKey(p)) throw new ProjectConfigParser.ParseException("Module of type \""+moduleType+"\" must have a \""+p+"\" parameter");
        }
        source = getStringParam("source", moduleParams);
        output = getStringParam("output", moduleParams);
        urlField = getStringParam("urlField", moduleParams);
        domSelector = getDefaultStringParam("domSelector", moduleParams, "body");
        fields = new HashMap<>();
        if(moduleParams.containsKey("fields")){
            HashMap<String,Object> fieldsMap = getMapParam("fields", moduleParams);
            for(String k: fieldsMap.keySet()){
                fields.put(k, ProjectConfigParser.parseString(fieldsMap.get(k), moduleName+"/fields/"+k));
            }
        }
    }
}
