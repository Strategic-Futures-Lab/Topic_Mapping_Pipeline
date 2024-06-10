package P3_TopicModelling.TopicModelCore;

import PY_Helper.SparseVector;

import java.util.List;

/**
 * Class representing a document which has been modelled, ie, has topic assignments sampled.
 * Replaces {@link TopicData}.
 *
 * @author P. Le Bras.
 * @version 1
 */
@Deprecated
public class ModelledDocument implements java.io.Serializable{

    /** Serialisation ID. */
    private static final long serialVersionUID = 1300965786833187509L;

    /** Document's identifier. */
    public String id;
    /** Document's index. */
    public int index;

    /** List of word/lemmas used by the model (might defer from inputLemmas due to stop-words removed by MALLET). */
    public String[] modelLemmas;
    /** List of lemmas unique identifiers. */
    public int[] lemmasIDs;
    /** For each word/lemma in the document, id of the topic assigned to this word. */
    public int[] topicSequence;
    /** For each topic, the number of words/lemmas assigned to that topic. */
    public int[] topicCount;
    /** Distribution of topics (normalised topic counts). */
    public double[] topicDistribution;
    /** Number of topics. */
    public int nTopics;
    /** Number of lemmas (in the model). */
    public int nLemmas;

    /** Full word-distribution distances from topics. */
    public double[] fullTopicDistances;
    /** Component word-distribution distances from topics. */
    public double[] compTopicDistances;

    /**
     * Constructor.
     * @param id Document id.
     * @param index Document index (in the model).
     */
    public ModelledDocument(String id, int index){
        this.id = id;
        this.index = index;
    }

    /**
     * Method setting the lemma information of the document.
     * @param lemmas List of lemma values, as they appear in the document.
     * @param ids List of lemmas identifiers, in order of appearance in the document.
     */
    public void setLemmas(String[] lemmas, int[] ids){
        modelLemmas = lemmas;
        lemmasIDs = ids;
        nLemmas = modelLemmas.length;
    }

    /**
     * Method setting the topic assignment data.
     * @param sequence Sequence of topic assignment, or each lemma.
     * @param count Number of assignment for each topic.
     * @param distribution Distribution of topics in the document.
     */
    public void setTopicAssignment(int[] sequence, int[] count, double[] distribution){
        topicSequence = sequence;
        topicCount = count;
        topicDistribution = distribution;
        nTopics = topicDistribution.length;
    }

    /**
     * Method getting a SparseVector of the lemmas distributions.
     * @param size Size of the vocabulary.
     * @return SparseVector of the lemmas distribution.
     */
    public SparseVector getLemmaDistribVector(int size){
        SparseVector lemmaVec = new SparseVector(size);
        for(int i = 0; i < lemmasIDs.length; i++){
            lemmaVec.put(lemmasIDs[i], lemmaVec.get(lemmasIDs[i])+1.0);
        }
        return lemmaVec.normalise();
    }

    /**
     * Method getting a SparseVector of a component lemmas distributions, ie, only counting lemmas
     * assigned to a given topic.
     * @param size Size of the vocabulary.
     * @param topic Topic to get component from.
     * @return SparseVector of the component lemmas distribution.
     */
    public SparseVector getComponentLemmaDistribVector(int size, int topic){
        SparseVector lemmaVec = new SparseVector(size);
        for(int i = 0; i < topicSequence.length; i++){
            if(topicSequence[i] == topic){
                lemmaVec.put(lemmasIDs[i], lemmaVec.get(lemmasIDs[i])+1.0);
            }
        }
        return lemmaVec.normalise();
    }

    /**
     * Method calculating the document's distances from topics using word vectors.
     * @param topicVectors Topic label vectors to get distances from.
     */
    public void setWordDistancesFromTopics(List<SparseVector> topicVectors){
        SparseVector fullDocVector = getLemmaDistribVector(topicVectors.get(0).size());
        fullTopicDistances = new double[nTopics];
        compTopicDistances = new double[nTopics];
        for(int t = 0; t < nTopics; t++){
            SparseVector topicVec = topicVectors.get(t);
            fullTopicDistances[t] = SparseVector.HellingerDistance(topicVec, fullDocVector);
            if(topicCount[t] > 0) {
                SparseVector compDocVector = getComponentLemmaDistribVector(topicVec.size(), t);
                compTopicDistances[t] = SparseVector.HellingerDistance(topicVec, compDocVector);
            } else {
                // the document component will be empty, so distance is 1
                compTopicDistances[t] = 1;
            }
        }
    }
}
