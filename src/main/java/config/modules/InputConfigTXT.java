package config.modules;

import config.ModuleConfig;
import config.ProjectConfigParser;
import pipeline.ModuleType;

import java.util.HashMap;

/**
 * Configuration class for TXTInput module
 *
 * @author P. Le Bras
 * @version 1
 */
public class InputConfigTXT extends ModuleConfig {

    private static final String[] MANDATORY_PARAMS = new String[]{"source","output"};

    /** Filename of the source CSV file */
    public final String source;
    /** Filename of the output corpus JSON file */
    public final String output;
    /** Flag for considering empty lines as document separators */
    public final boolean splitEmptyLines;

    /**
     * Constructor, parses and stores module parameters
     * @param name Module name as described in the YAML config file
     * @param moduleParams Map of unparsed YAML parameters
     * @throws ProjectConfigParser.ParseException If the configuration does not include all mandatory parameters or if a parameter is not found
     */
    public InputConfigTXT(String name, ModuleType type, HashMap<String, Object> moduleParams) throws ProjectConfigParser.ParseException{
        super(name, type);
        for(String p: MANDATORY_PARAMS){
            if(!moduleParams.containsKey(p)) throw new ProjectConfigParser.ParseException("Module of type \""+moduleType+"\" must have a \""+p+"\" parameter");
        }
        source = getStringParam("source", moduleParams);
        output = getStringParam("output", moduleParams);
        splitEmptyLines = getDefaultBooleanParam("emptyLineSplit", moduleParams, false);
    }
}
