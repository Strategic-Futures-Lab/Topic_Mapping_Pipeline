package config.modules;

import config.ModuleConfig;
import config.ProjectConfigParser;
import pipeline.ModuleType;

import java.util.HashMap;

/**
 * Configuration class for StopWords module
 *
 * @author P. Le Bras
 * @version 1
 */
public class StopWordsConfig extends ModuleConfig {

    private static final String[] MANDATORY_PARAMS = new String[]{"corpus", "stopWords"};

    /** Filenames of the source corpus files */
    public final String corpus;
    /** Filename of the output corpus file */
    public final String output;
    /** Filename of the stop words file */
    public final String stopWords;

    /**
     * Constructor, parses and stores module parameters
     * @param name Module name as described in the YAML config file
     * @param moduleParams Map of unparsed YAML parameters
     * @throws ProjectConfigParser.ParseException If the configuration does not include all mandatory parameters or if a parameter is not found
     */
    public StopWordsConfig(String name, ModuleType type, HashMap<String, Object> moduleParams) throws ProjectConfigParser.ParseException {
        super(name, type);
        // mandatory parameters
        for(String p: MANDATORY_PARAMS){
            if(!moduleParams.containsKey(p)) throw new ProjectConfigParser.ParseException("Module of type \""+moduleType+"\" must have a \""+p+"\" parameter");
        }
        corpus = getStringParam("corpus", moduleParams);
        stopWords = getStringParam("stopWords", moduleParams);
        // optional parameters
        output = getDefaultStringParam("output", moduleParams, corpus);
    }
}
