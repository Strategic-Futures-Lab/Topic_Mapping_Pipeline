package pipeline.config.modules;

import pipeline.config.ModuleConfig;
import pipeline.config.ConfigParser;
import pipeline.ModuleType;

import java.util.HashMap;

/**
 * Configuration class for BuildText module
 *
 * @author P. Le Bras
 * @version 1
 */
public class BuildTextConfig extends ModuleConfig {

    private static final String[] MANDATORY_PARAMS = new String[]{"corpus", "textFields"};

    /** Filename of the source corpus file */
    public final String corpus;
    /** Filename of the output corpus file */
    public final String output;
    /** List of document fields to build text string with */
    public final String[] textFields;
    /** List of document fields to keep as such */
    public final String[] docFields;

    /**
     * Constructor, parses and stores module parameters
     * @param name Module name as described in the YAML config file
     * @param moduleParams Map of unparsed YAML parameters
     * @throws ConfigParser.ParseException If the configuration does not include all mandatory parameters or if a parameter is not found
     */
    public BuildTextConfig(String name, ModuleType type, HashMap<String, Object> moduleParams) throws ConfigParser.ParseException {
        super(name, type);
        // mandatory parameters
        for(String p: MANDATORY_PARAMS){
            if(!moduleParams.containsKey(p)) throw new ConfigParser.ParseException("Module of type \""+moduleType+"\" must have a \""+p+"\" parameter");
        }
        corpus = getPathParam("corpus", moduleParams);
        textFields = getStringListParam("textFields", moduleParams).toArray(new String[0]);
        // optional parameters
        output = getDefaultPathParam("output", moduleParams, corpus);
        if(moduleParams.containsKey("docFields")){
            docFields = getStringListParam("docFields", moduleParams).toArray(new String[0]);
        } else {
            docFields = null;
        }
    }
}
