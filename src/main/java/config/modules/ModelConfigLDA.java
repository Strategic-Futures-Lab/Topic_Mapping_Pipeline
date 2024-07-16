package config.modules;

import config.ModuleConfig;
import config.ProjectConfigParser;
import model.ldacore.LDAParameters;
import pipeline.ModuleType;

import java.util.HashMap;

public class ModelConfigLDA extends ModuleConfig {

    private static final String[] MANDATORY_PARAMS = new String[]{"corpus", "model", "topics"};

    /** Filename of the source corpus file */
    public final String corpus;
    /** Filename of the output model file */
    public final String output;
    /** LDA parameters */
    public final LDAParameters ldaParams;
    /** Minimum number of lemmas for a document to be included in the model */
    public final int minLemmas;
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
        ldaParams = new LDAParameters(getIntParam("topics", moduleParams));
        ldaParams.samplingIterations = getDefaultIntParam("iterations", moduleParams, 2000);
        ldaParams.maximisationIterations = getDefaultIntParam("maximisations", moduleParams, 50);
        ldaParams.alphaSum = getDefaultDoubleParam("alphaSum", moduleParams, 1.0);
        ldaParams.symmetricAlpha = getDefaultBooleanParam("symmetricAlpha", moduleParams, false);
        ldaParams.beta = getDefaultDoubleParam("beta", moduleParams, 0.01);
        ldaParams.optimisationInterval = getDefaultIntParam("optimisationInterval", moduleParams, 50);
        ldaParams.seed = getDefaultIntParam("seed", moduleParams, 151);
        minLemmas = getDefaultIntParam("minLemmas", moduleParams, 10);
        wordDistances = getDefaultBooleanParam("wordDistances", moduleParams, false);
        logDir = ProjectConfigParser.checkDirectory(getDefaultStringParam("logs", moduleParams, ""));
        serialised = getDefaultStringParam("serialised", moduleParams, null);
        loglikelihoodLogs = getDefaultStringParam("loglikelihoodLogs", moduleParams, null);
        topicLogs = getDefaultStringParam("topicLogs", moduleParams, null);
    }

}
