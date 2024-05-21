package config.modules;

import config.ModuleConfig;
import config.ProjectConfigParser;
import pipeline.ModuleType;

import java.util.HashMap;

/**
 * Class for parsing and storing GTR corpus input parameters
 *
 * @author P. Le Bras
 * @version 1
 */
public class InputConfigGTR extends ModuleConfig {

    private static final String[] MANDATORY_PARAMS = new String[]{"source","output","pidField","gtrFields"};

    /** Filename of the source CSV file */
    public final String source;
    /** Filename of the output corpus JSON file */
    public final String output;
    /** List of CSV fields to store in the  corpus JSON file; key is the name stored in the corpus JSON file,
     * value is the name found in the source CSV file */
    public final HashMap<String, String> fields;
    /** CSV field where the project ID of the GtR project can be found */
    public final String pidField;
    /** List of GtR fields to query and store in the corpus JSON file; key is the name stored in the corpus JSON file,
     * value is the GtR field name */
    public final HashMap<String, String> gtrFields;

    /**
     * Constructor, parses and stores module parameters
     * @param moduleName Module name as described in the YAML config file
     * @param moduleParams Map of unparsed YAML parameters
     * @throws ProjectConfigParser.ParseException If the configuration does not include all mandatory parameters or if a parameter is not found
     */
    public InputConfigGTR(String moduleName, ModuleType type, HashMap<String, Object> moduleParams) throws ProjectConfigParser.ParseException{
        super(moduleName, type);
        for(String p: MANDATORY_PARAMS){
            if(!moduleParams.containsKey(p)) throw new ProjectConfigParser.ParseException("Module of type \""+moduleType+"\" must have a \""+p+"\" parameter");
        }
        source = getStringParam("source", moduleParams);
        output = getStringParam("output", moduleParams);
        pidField = getStringParam("pidField", moduleParams);
        gtrFields = new HashMap<>();
        HashMap<String, Object> gtrFieldsMap = getMapParam("gtrFields", moduleParams);
        for(String k: gtrFieldsMap.keySet()){
            gtrFields.put(k, ProjectConfigParser.parseString(gtrFieldsMap.get(k), moduleName+"/gtrFields/"+k));
        }
        fields = new HashMap<>();
        if(moduleParams.containsKey("fields")){
            HashMap<String,Object> fieldsMap = getMapParam("fields", moduleParams);
            for(String k: fieldsMap.keySet()){
                fields.put(k, ProjectConfigParser.parseString(fieldsMap.get(k), moduleName+"/fields/"+k));
            }
        }
    }
}
