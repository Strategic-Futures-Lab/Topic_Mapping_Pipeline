package pipeline.config.modules;

import IO.Console;
import pipeline.config.ModuleConfig;
import pipeline.config.ConfigParser;
import pipeline.ModuleType;
import pipeline.config.ProjectConfig;

import java.util.HashMap;

/**
 * Configuration class for BibTex Input module
 *
 * @author P. Le Bras
 * @version 1
 */
public class InputConfigBIB extends ModuleConfig {

    private static final String[] MANDATORY_PARAMS = new String[]{"source","output","fields"};

    /** Filename of the source BibTex file */
    public final String sourceFile;
    /** Filename of the output corpus JSON file */
    public final String outputFile;
    /** List of BibTex fields to store in the  corpus JSON file; key is the name stored in the corpus JSON file,
     * value is the name found in the source BibTex file */
    public final HashMap<String, String> documentFields;

    /**
     * Constructor, parses and stores module parameters
     * @param name Module name as described in the YAML config file
     * @param moduleParams Map of unparsed YAML parameters
     * @param projectParams Global project parameters
     * @throws ConfigParser.ParseException If the configuration does not include all mandatory parameters or if a parameter is not found
     */
    public InputConfigBIB(String name, ModuleType type, HashMap<String, Object> moduleParams, ProjectConfig projectParams) throws ConfigParser.ParseException{
        super(name, type);
        for(String p: MANDATORY_PARAMS){
            if(!moduleParams.containsKey(p)) throw new ConfigParser.ParseException("Module of type \""+moduleType+"\" must have a \""+p+"\" parameter");
        }
        sourceFile = projectParams.sourceDirectory+getPathParam("source", moduleParams);
        outputFile = projectParams.dataDirectory+getPathParam("output", moduleParams);
        HashMap<String,Object> fieldsMap = getMapParam("fields", moduleParams);
        documentFields = new HashMap<>();
        for(String k: fieldsMap.keySet()){
            documentFields.put(k, ConfigParser.parseString(fieldsMap.get(k), moduleName+"/fields/"+k));
        }
    }

    /**
     * Method logging the module parameters
     */
    public void logConfig(){
        Console.info("Reading BibTex input from "+sourceFile+" and saving to "+outputFile);
    }
}
