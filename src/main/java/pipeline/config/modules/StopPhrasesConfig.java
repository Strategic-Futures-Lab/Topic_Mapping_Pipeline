package pipeline.config.modules;

import IO.Console;
import pipeline.config.ModuleConfig;
import pipeline.config.ConfigParser;
import pipeline.ModuleType;
import pipeline.config.ProjectConfig;

import java.util.HashMap;

/**
 * Configuration class for StopPhrases module
 *
 * @author P. Le Bras
 * @version 1
 */
public class StopPhrasesConfig extends ModuleConfig {

    private static final String[] MANDATORY_PARAMS = new String[]{"corpus", "stopPhrases"};

    /** Filenames of the source corpus files */
    public final String corpusFile;
    /** Filename of the output corpus file */
    public final String outputFile;
    /** Filename of the stop phrases file */
    public final String stopPhrasesFile;

    /**
     * Constructor, parses and stores module parameters
     * @param name Module name as described in the YAML config file
     * @param moduleParams Map of unparsed YAML parameters
     * @param projectParams Global project parameters
     * @throws ConfigParser.ParseException If the configuration does not include all mandatory parameters or if a parameter is not found
     */
    public StopPhrasesConfig(String name, ModuleType type, HashMap<String, Object> moduleParams, ProjectConfig projectParams) throws ConfigParser.ParseException {
        super(name, type);
        // mandatory parameters
        for(String p: MANDATORY_PARAMS){
            if(!moduleParams.containsKey(p)) throw new ConfigParser.ParseException("Module of type \""+moduleType+"\" must have a \""+p+"\" parameter");
        }
        String c = getPathParam("corpus", moduleParams);
        corpusFile = projectParams.dataDirectory+c;
        stopPhrasesFile = projectParams.sourceDirectory+getPathParam("stopPhrases", moduleParams);
        // optional parameters
        outputFile = projectParams.dataDirectory+getDefaultPathParam("output", moduleParams, c);
    }

    /**
     * Method logging the module parameters
     */
    public void logConfig(){
        String saveDiff = corpusFile.equals(outputFile) ? "" : " and saving to "+outputFile;
        Console.info("Removing stop phrases ("+stopPhrasesFile+") from corpus "+corpusFile+saveDiff);
    }
}
