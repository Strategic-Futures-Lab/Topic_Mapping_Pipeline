package model.ldacore;

import cc.mallet.topics.ParallelTopicModel;
import cc.mallet.types.InstanceList;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class LDAModel implements Serializable {

    private static final int PROC = 8;

    @Serial
    private static final long serialVersionUID = -8983749417082119056L;

    // MALLET options
    /** Number of topics to model */
    public int nTopics = 50;
    /** Random seed for the sampler */
    public int seed = 1;
    /** Number of sampling iterations */
    public int samplingIterations = 500;
    /** Number of maximisation iterations */
    public int maximisationIterations = 50;
    /** Sum of alpha dirichlet priors (topics over documents):
     * High alpha = document mix of more topics;
     * Low alpha = document mixture of few/one topics */
    public double alphaSum = 1.0;
    /** Beta dirichlet prior (words over topics):
     * High beta = topic mix of more words;
     * Low beta = topic mix of few words */
    public double beta = 0.01;
    /** Flag for making alpha values symmetric:
     * if asymmetric, some topics more likely to appear across documents */
    public boolean symmetricAlpha = false;
    /** Number of iterations between hyperparameters optimisations */
    public int optimisationInterval = 50;

    /** Flag for calculating the word distribution differences between documents and topics */
    public boolean getWordDistances = false;

    /** List of documents */
    private List<LDADocument> documents;
    /** Map of document index (in the model) to document ID */
    private List<String> numIDtoStringID = new ArrayList<>();
    /** Map of document ID to document index (in the model) */
    public HashMap<String, Integer> stringIDtoNumID = new HashMap<>();
    /** Instances of documents for the model */
    private InstanceList instances;
    /** Model object */
    private ParallelTopicModel model;
    /** List of modelled topic, with ID and lemmas sorted by weight */
    private List<LDATopic> topics;
    /** Total number of unique words in the vocabulary */
    private int vocabularySize;

    /** Un-serialised record of log-likelihood throughout the modelling process */
    public transient LikelihoodLogs logLikelihoodLogs;
    /** Un-serialised record of the topics evolution throughout the modelling process */
    public transient TopicLogs topicLogs;
}
