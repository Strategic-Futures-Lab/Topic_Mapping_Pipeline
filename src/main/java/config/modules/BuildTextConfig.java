package config.modules;

import config.ModuleConfig;
import config.ProjectConfigParser;
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

    public BuildTextConfig(String name, ModuleType type, HashMap<String, Object> moduleParams) throws ProjectConfigParser.ParseException {
        super(name, type);
        // mandatory parameters
        for(String p: MANDATORY_PARAMS){
            if(!moduleParams.containsKey(p)) throw new ProjectConfigParser.ParseException("Module of type \""+moduleType+"\" must have a \""+p+"\" parameter");
        }
        corpus = getStringParam("corpus", moduleParams);
        textFields = getStringListParam("textFields", moduleParams).toArray(new String[0]);
        // optional parameters
        output = getDefaultStringParam("output", moduleParams, corpus);
        if(moduleParams.containsKey("docFields")){
            docFields = getStringListParam("docFields", moduleParams).toArray(new String[0]);
        } else {
            docFields = null;
        }
    }
}
