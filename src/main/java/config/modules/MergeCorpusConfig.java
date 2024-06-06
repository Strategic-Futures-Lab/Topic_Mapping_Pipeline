package config.modules;

import config.ModuleConfig;
import config.ProjectConfigParser;
import pipeline.ModuleType;

import java.util.HashMap;

/**
 * Configuration class for MergeCorpus module
 *
 * @author P. Le Bras
 * @version 1
 */
public class MergeCorpusConfig extends ModuleConfig {

    private static final String[] MANDATORY_PARAMS = new String[]{"corpora", "output"};

    /** Filenames of the source corpus files */
    public final String[] corpora;
    /** Filename of the output corpus file */
    public final String output;
    /** List of document fields to keep */
    public final String[] docFields;

    /**
     * Constructor, parses and stores module parameters
     * @param name Module name as described in the YAML config file
     * @param moduleParams Map of unparsed YAML parameters
     * @throws ProjectConfigParser.ParseException If the configuration does not include all mandatory parameters or if a parameter is not found
     */
    public MergeCorpusConfig(String name, ModuleType type, HashMap<String, Object> moduleParams) throws ProjectConfigParser.ParseException {
        super(name, type);
        // mandatory parameters
        for(String p: MANDATORY_PARAMS){
            if(!moduleParams.containsKey(p)) throw new ProjectConfigParser.ParseException("Module of type \""+moduleType+"\" must have a \""+p+"\" parameter");
        }
        corpora = getStringListParam("corpora", moduleParams).toArray(new String[0]);
        if(corpora.length < 2) throw new ProjectConfigParser.ParseException("Module of type \""+moduleType+"\" must have a more than 1 corpus to merge");
        // optional parameters
        output = getStringParam("output", moduleParams);
        if(moduleParams.containsKey("docFields")){
            docFields = getStringListParam("docFields", moduleParams).toArray(new String[0]);
        } else {
            docFields = null;
        }
    }
}
