package pipeline.config.modules;

import IO.Console;
import pipeline.config.ModuleConfig;
import pipeline.config.ConfigParser;
import pipeline.ModuleType;
import pipeline.config.ProjectConfig;

import java.util.HashMap;

/**
 * Configuration class for Lemmatise module
 *
 * @author P. Le Bras
 * @version 1
 */
public class LemmatiseConfig extends ModuleConfig {

    private static final String[] MANDATORY_PARAMS = new String[]{"corpus"};

    /** Filenames of the source corpus files */
    public final String corpusFile;
    /** Filename of the output corpus file */
    public final String outputFile;
    /** Filename of the stop phrases file */
    public final String stopPhrasesFile;
    /** Flag for removing stop phrases */
    public final boolean removeStopPhrases;
    /** Filename of the stop words file */
    public final String stopWordsFile;
    /** Flag for removing stop words */
    public final boolean removeStopWords;

    /**
     * Constructor, parses and stores module parameters
     * @param name Module name as described in the YAML config file
     * @param moduleParams Map of unparsed YAML parameters
     * @param projectParams Global project parameters
     * @throws ConfigParser.ParseException If the configuration does not include all mandatory parameters or if a parameter is not found
     */
    public LemmatiseConfig(String name, ModuleType type, HashMap<String, Object> moduleParams, ProjectConfig projectParams) throws ConfigParser.ParseException {
        super(name, type);
        // mandatory parameters
        for(String p: MANDATORY_PARAMS){
            if(!moduleParams.containsKey(p)) throw new ConfigParser.ParseException("Module of type \""+moduleType+"\" must have a \""+p+"\" parameter");
        }
        String c = getPathParam("corpus", moduleParams);
        corpusFile = projectParams.dataDirectory+c;
        // optional parameters
        outputFile = projectParams.dataDirectory+getDefaultPathParam("output", moduleParams, c);
        String p = getDefaultPathParam("stopPhrases", moduleParams, null);
        removeStopPhrases = p!=null;
        stopPhrasesFile = removeStopPhrases ? projectParams.sourceDirectory+p : null;
        String w = getDefaultPathParam("stopWords", moduleParams, null);
        removeStopWords = w!=null;
        stopWordsFile = removeStopWords ? projectParams.sourceDirectory+w : null;
    }

    /**
     * Method logging the module parameters
     */
    public void logConfig(){
        String saveDiff = corpusFile.equals(outputFile) ? "" : " and saving to "+outputFile;
        Console.info("Lemmatising texts from corpus "+corpusFile+saveDiff);
        if(removeStopPhrases) Console.step("Removing stop phrases in "+stopPhrasesFile,1);
        if(removeStopWords) Console.step("Removing stop words in "+stopWordsFile,1);
    }
}
