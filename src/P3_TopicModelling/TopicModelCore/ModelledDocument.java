package P3_TopicModelling.TopicModelCore;

/**
 * Class representing a document which has been modelled, ie, has topic assignments sampled.
 * Replaces {@link TopicData}.
 *
 * @author P. Le Bras.
 * @version 1
 */
public class ModelledDocument implements java.io.Serializable{

    /** Serialisation ID. */
    private static final long serialVersionUID = 1300965786833187509L;

    /** Document's identifier. */
    public String id;
    /** Document's index. */
    public int index;

    /** List of word/lemmas used by the model (might defer from inputLemmas due to stop-words removed by MALLET). */
    public String[] modelLemmas;
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
     * Method setting the topic assignment data.
     * @param lemmas List of lemmas used by the model (might differ from the input).
     * @param sequence Sequence of topic assignment, or each lemma.
     * @param count Number of assignment for each topic.
     * @param distribution Distribution of topics in the document.
     */
    public void setTopicAssignment(String[] lemmas, int[] sequence, int[] count, double[] distribution){
        modelLemmas = lemmas;
        nLemmas = modelLemmas.length;
        topicSequence = sequence;
        topicCount = count;
        topicDistribution = distribution;
        nTopics = topicDistribution.length;
    }
}
