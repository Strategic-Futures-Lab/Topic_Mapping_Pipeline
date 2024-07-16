package config.modules;

import config.ModuleConfig;
import config.ProjectConfigParser;
import pipeline.ModuleType;

import java.util.HashMap;

public class ModelConfigLDA extends ModuleConfig {

    private static final String[] MANDATORY_PARAMS = new String[]{"corpus", "model", "topics"};

    /** Filename of the source corpus file */
    public final String corpus;
    /** Filename of the output model file */
    public final String output;
    /** Number of topics to generate */
    public final int topics;
    /** Number of modelling iterations to perform */
    public final int iter;
    /** Number of maximisation iterations to perform */
    public final int iterMax;
    /** Sum of alpha dirichlet priors (topics over documents):
     * High alpha = document mix of more topics;
     * Low alpha = document mixture of few/one topics */
    public final double alphaSum;
    /** Flag for running a symmetrical optimization of alpha */
    public final boolean symmetricAlpha;
    /** Beta dirichlet prior (words over topics):
     * High beta = topic mix of more words;
     * Low beta = topic mix of few words */
    public final double beta;
    /** Iteration interval between hyperparameters optimisation */
    public final int optiInterval;
    /** Model initialisation seed */
    public final int seed;
    /** Flag for generating document to topic distance based on word distributions */
    public final boolean wordDistances;
    /** Name of directory where to save log files */
    public final String logDir;
    /** Filename of serialised model object */
    public final String serialised;
    /** Filename of Log-Likelihood logs */
    public final String loglikelihoodLogs;
    /** Filename of topic logs */
    public final String topicLogs;

    public ModelConfigLDA(String moduleName, ModuleType type, HashMap<String, Object> moduleParams) throws ProjectConfigParser.ParseException{
        super(moduleName, type);
        for(String p: MANDATORY_PARAMS){
            if(!moduleParams.containsKey(p)) throw new ProjectConfigParser.ParseException("Module of type \""+moduleType+"\" must have a \""+p+"\" parameter");
        }
        corpus = getStringParam("corpus", moduleParams);
        output = getStringParam("model", moduleParams);
        topics = getIntParam("topics", moduleParams);
        iter = getDefaultIntParam("iterations", moduleParams, 2000);
        iterMax = getDefaultIntParam("maximisations", moduleParams, 50);
        alphaSum = getDefaultDoubleParam("alphaSum", moduleParams, 1.0);
        symmetricAlpha = getDefaultBooleanParam("symmetricAlpha", moduleParams, false);
        beta = getDefaultDoubleParam("beta", moduleParams, 0.01);
        optiInterval = getDefaultIntParam("optimisationInterval", moduleParams, 50);
        seed = getDefaultIntParam("seed", moduleParams, 151);
        wordDistances = getDefaultBooleanParam("wordDistances", moduleParams, false);
        logDir = ProjectConfigParser.checkDirectory(getDefaultStringParam("logs", moduleParams, ""));
        serialised = getDefaultStringParam("serialised", moduleParams, null);
        loglikelihoodLogs = getDefaultStringParam("loglikelihoodLogs", moduleParams, null);
        topicLogs = getDefaultStringParam("topicLogs", moduleParams, null);
    }

}
