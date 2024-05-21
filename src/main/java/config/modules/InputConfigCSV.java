package config.modules;

import config.ProjectConfigParser;
import config.ModuleConfig;
import pipeline.ModuleType;

import java.util.HashMap;

/**
 * Class for parsing and storing CSV corpus input parameters
 *
 * @author P. Le Bras
 * @version 1
 */
public class InputConfigCSV extends ModuleConfig {

    private static final String[] MANDATORY_PARAMS = new String[]{"source","output","fields"};

    /** Filename of the source CSV file */
    public final String source;
    /** Filename of the output corpus JSON file */
    public final String output;
    /** List of CSV fields to store in the  corpus JSON file; key is the name stored in the corpus JSON file,
     * value is the name found in the source CSV file */
    public final HashMap<String, String> fields;

    /**
     * Constructor, parses and stores module parameters
     * @param name Module name as described in the YAML config file
     * @param moduleParams Map of unparsed YAML parameters
     * @throws ProjectConfigParser.ParseException If the configuration does not include all mandatory parameters or if a parameter is not found
     */
    public InputConfigCSV(String name, ModuleType type, HashMap<String, Object> moduleParams) throws ProjectConfigParser.ParseException{
        super(name, type);
        for(String p: MANDATORY_PARAMS){
            if(!moduleParams.containsKey(p)) throw new ProjectConfigParser.ParseException("Module of type \""+moduleType+"\" must have a \""+p+"\" parameter");
        }
        source = getStringParam("source", moduleParams);
        output = getStringParam("output", moduleParams);
        HashMap<String,Object> fieldsMap = getMapParam("fields", moduleParams);
        fields = new HashMap<>();
        for(String k: fieldsMap.keySet()){
            fields.put(k, ProjectConfigParser.parseString(fieldsMap.get(k), moduleName+"/fields/"+k));
        }
    }
}
