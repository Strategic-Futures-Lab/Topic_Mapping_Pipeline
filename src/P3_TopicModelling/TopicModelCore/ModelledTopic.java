package P3_TopicModelling.TopicModelCore;

import PY_Helper.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Class representing a topic modelled from documents, represented by a list of weighted lemmas and documents.
 *
 * @author S. Padilla, T. Methven, P. Le Bras
 * @version 1
 */
public class ModelledTopic implements java.io.Serializable{

    /** Serialisation ID. */
    private static final long serialVersionUID = -1048734038308190794L;

    /** Topic index (in topic distributions for example). */
    public int index;
    /** Topic's identifier. */
    public String id;

    /** Number of labels (lemmas) assigned to this topic. */
    public int nLabels;
    /** Sorted list (by assignment count) of unique labels (lemmas) assigned to this topic. */
    public String[] labels;
    // labelsID might be irrelevant
    /** Sorted list (by assignment count) of unique labels (lemmas) identifiers assigned to this topic. */
    public int [] labelsIDs;
    /** List of assignment counts (in order) for each unique label (lemma) assigned to this topic. */
    public double[] labelsWeights;

    /** Number of documents where this topic is present. */
    public int nDocs;
    /** Sorted list (by weight) of documents ids where this topic is present. */
    public String[] docs;
    /** List of weights (in order) for each documents where this topic is present. */
    public double[] docsWeights;

    /**
     * Constructor.
     * @param id Topic id.
     * @param index Topic index.
     */
    public ModelledTopic(String id, int index){
        this.id = id;
        this.index = index;
        this.nLabels = 0;
    }

    /**
     * Method setting the data of labels assigned to this topic.
     * @param labels List of labels values.
     * @param labelsIDs List of labels ids.
     * @param labelsWeights List of labels weights.
     */
    public void setLabelsAssignment(String[] labels, int[] labelsIDs, double[] labelsWeights){
        nLabels = labels.length;
        this.labels = labels;
        this.labelsIDs = labelsIDs;
        this.labelsWeights = labelsWeights;
    }

    /**
     * Method setting the data of documents where this topic is present.
     * @param docs List of document ids.
     * @param docsWeights List of document weights.
     */
    public void setDocumentsAssignment(String[] docs, double[] docsWeights){
        nDocs = docs.length;
        this.docs = docs;
        this.docsWeights = docsWeights;
    }

    /**
     * Method returning the label-weight pair at a given index in the lists of top words.
     * @param idx Index to lookup
     * @return The label-weight pair.
     * @throws ArrayIndexOutOfBoundsException if the index is invalid (negative or above the number of labels).
     */
    public Pair<String, Double> getLabel(int idx) throws ArrayIndexOutOfBoundsException{
        return new Pair<>(labels[idx], labelsWeights[idx]);
    }

    /**
     * Method returning all label-weight pairs in the list of top words.
     * @return The list of label-weight pairs.
     */
    public List<Pair<String,Double>> getLabels(){
        List<Pair<String,Double>> labelPairs = new ArrayList<>();
        for(int i = 0; i < nLabels; i++){
            labelPairs.add(getLabel(i));
        }
        return labelPairs;
    }

    /**
     * Method returning the topics top n words.
     * @param nWords Number of top words.
     * @return List of top words.
     */
    public List<String> getTopWords(int nWords){
        return Arrays.asList(Arrays.copyOfRange(labels, 0, nWords));
    }

    /**
     * Method returning the document-weight pair at a given index in the lists of top documents.
     * @param idx Index to lookup
     * @return The document-weight pair.
     * @throws ArrayIndexOutOfBoundsException if the index is invalid (negative or above the number of documents).
     */
    public Pair<String, Double> getDoc(int idx) throws ArrayIndexOutOfBoundsException{
        return new Pair<>(docs[idx], docsWeights[idx]);
    }

    /**
     * Method returning all document-weight pairs in the list of top documents.
     * @return The list of document-weight pairs.
     */
    public List<Pair<String,Double>> getDocs(){
        List<Pair<String,Double>> documentPairs = new ArrayList<>();
        for(int i = 0; i < nDocs; i++){
            documentPairs.add(getDoc(i));
        }
        return documentPairs;
    }

}