package model.ldacore;

/** Class storing parameters for LDA
 *
 * @author P. Le Bras
 * @version 1
 */
public class LDAParameters {

    /** Number of topics to generate */
    public int nTopics;
    /** Number of modelling iterations to perform */
    public int samplingIterations = 2000;
    /** Number of maximisation iterations to perform */
    public int maximisationIterations = 50;
    /** Sum of alpha dirichlet priors (topics over documents):
     * High alpha = document mix of more topics;
     * Low alpha = document mixture of few/one topics */
    public double alphaSum = 1.0;
    /** Flag for running a symmetrical optimization of alpha */
    public boolean symmetricAlpha = false;
    /** Beta dirichlet prior (words over topics):
     * High beta = topic mix of more words;
     * Low beta = topic mix of few words */
    public double beta = 0.01;
    /** Iteration interval between hyperparameters optimisation */
    public int optimisationInterval = 50;
    /** Model initialisation seed */
    public int seed = 151;

    public LDAParameters(int nTopics){
        this.nTopics = nTopics;
    }
}
