package pipeline.config.modules;

import IO.Console;
import pipeline.config.ModuleConfig;
import pipeline.config.ConfigParser;
import pipeline.ModuleType;
import pipeline.config.ProjectConfig;

import java.util.HashMap;

/**
 * Configuration class for GTRInput module
 *
 * @author P. Le Bras
 * @version 1
 */
public class InputConfigGTR extends ModuleConfig {

    private static final String[] MANDATORY_PARAMS = new String[]{"source","output","pidField","gtrFields"};

    /** Filename of the source CSV file */
    public final String sourceFile;
    /** Filename of the output corpus JSON file */
    public final String outputFile;
    /** List of CSV fields to store in the  corpus JSON file; key is the name stored in the corpus JSON file,
     * value is the name found in the source CSV file */
    public final HashMap<String, String> documentFields;
    /** CSV field where the project ID of the GtR project can be found */
    public final String pidField;
    /** List of GtR fields to query and store in the corpus JSON file; key is the name stored in the corpus JSON file,
     * value is the GtR field name */
    public final HashMap<String, String> gtrFields;

    /**
     * Constructor, parses and stores module parameters
     * @param moduleName Module name as described in the YAML config file
     * @param moduleParams Map of unparsed YAML parameters
     * @param projectParams Global project parameters
     * @throws ConfigParser.ParseException If the configuration does not include all mandatory parameters or if a parameter is not found
     */
    public InputConfigGTR(String moduleName, ModuleType type, HashMap<String, Object> moduleParams, ProjectConfig projectParams) throws ConfigParser.ParseException{
        super(moduleName, type);
        for(String p: MANDATORY_PARAMS){
            if(!moduleParams.containsKey(p)) throw new ConfigParser.ParseException("Module of type \""+moduleType+"\" must have a \""+p+"\" parameter");
        }
        sourceFile = projectParams.sourceDirectory+getPathParam("source", moduleParams);
        outputFile = projectParams.dataDirectory+getPathParam("output", moduleParams);
        pidField = getStringParam("pidField", moduleParams);
        gtrFields = new HashMap<>();
        HashMap<String, Object> gtrFieldsMap = getMapParam("gtrFields", moduleParams);
        for(String k: gtrFieldsMap.keySet()){
            gtrFields.put(k, ConfigParser.parseString(gtrFieldsMap.get(k), moduleName+"/gtrFields/"+k));
        }
        documentFields = new HashMap<>();
        if(moduleParams.containsKey("fields")){
            HashMap<String,Object> fieldsMap = getMapParam("fields", moduleParams);
            for(String k: fieldsMap.keySet()){
                documentFields.put(k, ConfigParser.parseString(fieldsMap.get(k), moduleName+"/fields/"+k));
            }
        }
    }

    /**
     * Method logging the module parameters
     */
    public void logConfig(){
        Console.info("Crawling GtR projects listed in "+sourceFile+" and saving to "+outputFile);
    }
}
