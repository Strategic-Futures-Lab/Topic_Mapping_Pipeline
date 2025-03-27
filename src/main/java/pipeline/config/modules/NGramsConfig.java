package pipeline.config.modules;

import pipeline.ModuleType;
import pipeline.config.ConfigParser;
import pipeline.config.ModuleConfig;

import java.util.HashMap;

/**
 * Configuration class for NGrams module
 *
 * @author P. Le Bras
 * @version 1
 */
public class NGramsConfig extends ModuleConfig {

    private static final String[] MANDATORY_PARAMS = new String[]{"corpus"};

    /** Filenames of the source corpus files */
    public final String corpus;
    /** Filename of the output corpus file */
    public final String output;
    /** Frequency threshold for considering a nGram */
    public final double threshold;
    /** Filename of known nGrams file */
    public final String nGrams;

    /**
     * Constructor, parses and stores module parameters
     * @param name Module name as described in the YAML config file
     * @param moduleParams Map of unparsed YAML parameters
     * @throws ConfigParser.ParseException If the configuration does not include all mandatory parameters or if a parameter is not found
     */
    public NGramsConfig(String name, ModuleType type, HashMap<String, Object> moduleParams) throws ConfigParser.ParseException {
        super(name, type);
        // mandatory parameters
        for(String p: MANDATORY_PARAMS){
            if(!moduleParams.containsKey(p)) throw new ConfigParser.ParseException("Module of type \""+moduleType+"\" must have a \""+p+"\" parameter");
        }
        corpus = getPathParam("corpus", moduleParams);
        // optional parameters
        output = getDefaultPathParam("output", moduleParams, corpus);
        // TODO: check default threshold
        threshold = getDefaultDoubleParam("threshold", moduleParams, 0.9);
        nGrams = getDefaultPathParam("nGrams", moduleParams, null);
    }
}
