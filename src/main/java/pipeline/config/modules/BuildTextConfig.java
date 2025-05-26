package pipeline.config.modules;

import IO.Console;
import pipeline.config.ModuleConfig;
import pipeline.config.ConfigParser;
import pipeline.ModuleType;
import pipeline.config.ProjectConfig;

import java.util.HashMap;
import java.util.List;

/**
 * Configuration class for BuildText module
 *
 * @author P. Le Bras
 * @version 1
 */
public class BuildTextConfig extends ModuleConfig {

    private static final String[] MANDATORY_PARAMS = new String[]{"corpus", "textFields"};

    /** Filename of the source corpus file */
    public final String corpusFile;
    /** Filename of the output corpus file */
    public final String outputFile;
    /** List of document fields to build text string with */
    public final String[] textFields;
    /** List of document fields to keep as such */
    public final String[] docFields;

    /**
     * Constructor, parses and stores module parameters
     * @param name Module name as described in the YAML config file
     * @param moduleParams Map of unparsed YAML parameters
     * @param projectParams Global project parameters
     * @throws ConfigParser.ParseException If the configuration does not include all mandatory parameters or if a parameter is not found
     */
    public BuildTextConfig(String name, ModuleType type, HashMap<String, Object> moduleParams, ProjectConfig projectParams) throws ConfigParser.ParseException {
        super(name, type);
        // mandatory parameters
        for(String p: MANDATORY_PARAMS){
            if(!moduleParams.containsKey(p)) throw new ConfigParser.ParseException("Module of type \""+moduleType+"\" must have a \""+p+"\" parameter");
        }
        String c = getPathParam("corpus", moduleParams);
        corpusFile = projectParams.dataDirectory+c;
        textFields = getStringListParam("textFields", moduleParams).toArray(new String[0]);
        // optional parameters
        outputFile = projectParams.dataDirectory+getDefaultPathParam("output", moduleParams, c);
        if(moduleParams.containsKey("docFields")){
            docFields = getStringListParam("docFields", moduleParams).toArray(new String[0]);
        } else {
            docFields = projectParams.docFields;
        }
    }

    /**
     * Method logging the module parameters
     */
    public void logConfig(){
        String saveDiff = corpusFile.equals(outputFile) ? "" : " and saving to "+outputFile;
        Console.info("Building text for documents in corpus "+corpusFile+saveDiff);
        Console.info("Building text using: ");
        for(String f: textFields) Console.step(f, 1);
    }
}
