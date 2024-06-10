package model.ldacore;

import data.SparseVector;

import java.io.Serializable;
import java.util.List;

/**
 * Wrapper class for documents modelled using LDA
 *
 * @author S.Padilla, T. Methven, P. Le Bras
 * @version 2
 */
public class LDADocument implements Serializable {

    // Serialisation ID
    private static final long serialVersionUID = 2244011647262167470L;

    // input fields, document id and lemmas
    private String docId;
    private String text;

    // model fields, set after running the model
    private int docIndex;
    private String[] words;
    private int[] wordIds;
    private int[] topicSequence;
    private int[] topicCount;
    private double[] topicDistribution;

    // analytics
    // TODO see about moving to dedicated module
    private double[] topicDistances;
    private double[] partialTopicDistances;

    /**
     * Constructor
     * @param id document id
     * @param inText document text
     */
    public LDADocument(String id, String inText){
        docId = id;
        text = inText;
    }

    /**
     * Setter method for the document index (in model)
     * @param idx integer index
     */
    public void setIndex(int idx){
        docIndex = idx;
    }

    /**
     * Setter method for the document's words
     * @param labels list of words as they appear in the document (may differ from text)
     * @param ids list of word ids, in order of appearance in the document (set by model)
     */
    public void setWords(String[] labels, int[] ids){
        words = labels;
        wordIds = ids;
    }

    /**
     * Setter method for the document's topic assignment
     * @param sequence topic assignment for each word
     * @param count number of assignments for each topic
     * @param distribution topic weights in the document
     */
    public void setTopicAssignment(int[] sequence, int[] count, double[] distribution){
        topicSequence = sequence;
        topicCount = count;
        topicDistribution = distribution;
    }

    private SparseVector getWordDistribution(int size){
        SparseVector wordVec = new SparseVector(size);
        for(int i=0; i < wordIds.length; i++){
            wordVec.put(wordIds[i], wordVec.get(wordIds[i])+1.0);
        }
        return wordVec.normalise();
    }

    private SparseVector getPartialWordDistribution(int size, int topic){
        SparseVector wordVec = new SparseVector(size);
        for(int i = 0; i < topicSequence.length; i++){
            if(topicSequence[i] == topic){
                wordVec.put(wordIds[i], wordVec.get(wordIds[i])+1.0);
            }
        }
        return wordVec.normalise();
    }

    /**
     * Setter method for the document to topic distance
     * Used for analytics, TODO: export to dedicated module
     * @param topicVectors List of topic vector to calculate distances against
     */
    public void setDistancesFromTopics(List<SparseVector> topicVectors){
        SparseVector fullDocVector = getWordDistribution(topicVectors.get(0).size());
        int nTopics = topicDistribution.length;
        topicDistances = new double[nTopics];
        partialTopicDistances = new double[nTopics];
        for(int t = 0; t < nTopics; t++){
            SparseVector topicVec = topicVectors.get(t);
            topicDistances[t] = SparseVector.HellingerDistance(topicVec, fullDocVector);
            if(topicCount[t] > 0) {
                SparseVector compDocVector = getPartialWordDistribution(topicVec.size(), t);
                partialTopicDistances[t] = SparseVector.HellingerDistance(topicVec, compDocVector);
            } else {
                // the document component will be empty, so distance is 1
                partialTopicDistances[t] = 1;
            }
        }
    }
}
