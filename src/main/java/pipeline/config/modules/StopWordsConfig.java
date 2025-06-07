package pipeline.config.modules;

import IO.Console;
import pipeline.ModuleType;
import pipeline.config.ConfigParser;
import pipeline.config.ModuleConfig;
import pipeline.config.ProjectConfig;

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
    public final String corpusFile;
    /** Filename of the output corpus file */
    public final String outputFile;
    /** Filename of the stop words file */
    public final String stopWordsFile;

    /**
     * Constructor, parses and stores module parameters
     * @param name Module name as described in the YAML config file
     * @param moduleParams Map of unparsed YAML parameters
     * @param projectParams Global project parameters
     * @throws ConfigParser.ParseException If the configuration does not include all mandatory parameters or if a parameter is not found
     */
    public StopWordsConfig(String name, ModuleType type, HashMap<String, Object> moduleParams, ProjectConfig projectParams) throws ConfigParser.ParseException {
        super(name, type, moduleParams.keySet(), MANDATORY_PARAMS);
        // mandatory parameters
        String c = getPathParam("corpus", moduleParams);
        corpusFile = projectParams.dataDirectory+c;
        stopWordsFile = projectParams.sourceDirectory+getPathParam("stopWords", moduleParams);
        // optional parameters
        outputFile = projectParams.dataDirectory+getDefaultPathParam("output", moduleParams, c);
    }

    /**
     * Method logging the module parameters
     */
    public void logConfig(){
        String saveDiff = corpusFile.equals(outputFile) ? "" : " and saving to "+outputFile;
        Console.info("Removing stop words ("+stopWordsFile+") from corpus "+corpusFile+saveDiff);
    }
}
