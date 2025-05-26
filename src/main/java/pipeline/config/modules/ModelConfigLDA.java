package pipeline.config.modules;

import IO.Console;
import model.ldacore.LDAParameters;
import pipeline.ModuleType;
import pipeline.config.ConfigParser;
import pipeline.config.ModuleConfig;
import pipeline.config.ProjectConfig;

import java.util.HashMap;

/**
 * Configuration class for LDA Model module
 *
 * @author P. Le Bras
 * @version 1
 */
public class ModelConfigLDA extends ModuleConfig {

    private static final String[] MANDATORY_PARAMS = new String[]{"corpus", "topics", "documents", "nTopics"};

    /** Filename of the source corpus file */
    public final String corpusFile;
    /** Filename of the output topic file */
    public final String topicsFile;
    /** Filename of the output document file */
    public final String documentsFile;
    /** LDA parameters */
    public final LDAParameters ldaParameters;
    /** Minimum number of lemmas for a document to be included in the model */
    public final int minLemmas;
    /** Flag for generating document to topic distance based on word distributions */
    public final boolean wordDistances;
    /** Name of directory where to save log files */
    public final String logDirectory;
    /** Filename of serialised model object */
    public final String serialisedFile;
    /** Filename of Log-Likelihood logs */
    public final String loglikelihoodLogsFile;
    /** Filename of topic logs */
    public final String topicLogsFile;
    /** Flag for serialising the model */
    public final boolean serialise;
    /** Flag for saving log-likelihoods */
    public final boolean saveLogLikelihoods;
    /** Flag for saving topic allocation logs */
    public final boolean saveTopicHistory;


    /**
     * Constructor, parses and stores module parameters
     * @param name Module name as described in the YAML config file
     * @param moduleParams Map of unparsed YAML parameters
     * @param projectParams Global project parameters
     * @throws ConfigParser.ParseException If the configuration does not include all mandatory parameters or if a parameter is not found
     */
    public ModelConfigLDA(String name, ModuleType type, HashMap<String, Object> moduleParams, ProjectConfig projectParams) throws ConfigParser.ParseException{
        super(name, type);
        for(String p: MANDATORY_PARAMS){
            if(!moduleParams.containsKey(p)) throw new ConfigParser.ParseException("Module of type \""+moduleType+"\" must have a \""+p+"\" parameter");
        }
        // mandatory parameters
        corpusFile = projectParams.dataDirectory+getPathParam("corpus", moduleParams);
        topicsFile = projectParams.dataDirectory+getPathParam("topics", moduleParams);
        documentsFile = projectParams.dataDirectory+getPathParam("documents", moduleParams);
        ldaParameters = new LDAParameters(getIntParam("nTopics", moduleParams));
        // optional parameters
        // model options
        ldaParameters.samplingIterations = getDefaultIntParam("iterations", moduleParams, 2000);
        ldaParameters.maximisationIterations = getDefaultIntParam("maximisations", moduleParams, 50);
        ldaParameters.alphaSum = getDefaultDoubleParam("alphaSum", moduleParams, 1.0);
        ldaParameters.symmetricAlpha = getDefaultBooleanParam("symmetricAlpha", moduleParams, false);
        ldaParameters.beta = getDefaultDoubleParam("beta", moduleParams, 0.01);
        ldaParameters.optimisationInterval = getDefaultIntParam("optimisationInterval", moduleParams, 50);
        ldaParameters.seed = getDefaultIntParam("seed", moduleParams, 151);
        minLemmas = getDefaultIntParam("minLemmas", moduleParams, 10);
        wordDistances = getDefaultBooleanParam("wordDistances", moduleParams, false);
        // log options
        logDirectory = projectParams.dataDirectory+ConfigParser.checkDirectory(getDefaultPathParam("logs", moduleParams, ""));
        String p = getDefaultPathParam("serialised", moduleParams, null);
        serialise = p!=null;
        serialisedFile = serialise ? logDirectory+p : null;
        p = getDefaultPathParam("loglikelihoodLogs", moduleParams, null);
        saveLogLikelihoods = p!=null;
        loglikelihoodLogsFile = saveLogLikelihoods ? logDirectory+p : null;
        p = getDefaultPathParam("topicLogs", moduleParams, null);
        saveTopicHistory = p!=null;
        topicLogsFile = saveTopicHistory ? logDirectory+p : null;
    }

    /**
     * Method logging the module parameters
     */
    public void logConfig(){
        Console.info("Modelling "+ldaParameters.nTopics+" topics from corpus "+corpusFile);
        Console.info("Saving topics in "+topicsFile, 1);
        Console.info("Saving documents in "+documentsFile, 1);
        Console.info("Ignoring documents with less than "+minLemmas+" lemmas", 1);
        if(serialise) Console.info("Serialising model to "+serialisedFile, 1);
        if(saveLogLikelihoods) Console.info("Saving log-likelihoods to "+loglikelihoodLogsFile, 1);
        if(saveTopicHistory) Console.info("Saving topic logs to "+topicLogsFile, 1);
    }

}
