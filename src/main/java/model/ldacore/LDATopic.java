package model.ldacore;

import PY_Helper.SparseVector;
import data.Pair;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Wrapper class for topics modelled using LDA
 *
 * @author S. Padilla, T. Methven, P. Le Bras
 * @version 2
 */
public class LDATopic implements Serializable {

    // Serialisation ID
    private static final long serialVersionUID = -1048734038308190794L;

    // topic index or number
    private int number;

    // Sorted list (by assignment count) of unique labels (lemmas) assigned to this topic
    private String[] words;
    // labelsID might be irrelevant
    // Sorted list (by assignment count) of unique labels (lemmas) identifiers assigned to this topic
    private int [] wordIds;
    // List of assignment counts (in order) for each unique label (lemma) assigned to this topic
    private double[] wordWeights;

    // Sorted list (by weight) of documents ids where this topic is present
    private String[] documents;
    // List of weights (in order) for each document where this topic is present
    private double[] docWeights;

    /**
     * Constructor
     * @param topicNumber topic number used in the model
     */
    public LDATopic(int topicNumber){
        number = topicNumber;
    }

    /**
     * Setter method for word assignment
     * @param labels list of labels
     * @param labelIds list of label ids in the model
     * @param labelWeights distribution of label weights across the topic
     */
    public void setWordAssignments(String[] labels, int[] labelIds, double[] labelWeights){
        words = labels;
        wordIds = labelIds;
        wordWeights = labelWeights;
    }

    /**
     * Setter method for document assignment
     * @param documentIds list of document ids
     * @param documentWeights distribution of this topic's weight across documents
     */
    public void setDocumentAssignments(String[] documentIds, double[] documentWeights){
        documents = documentIds;
        docWeights = documentWeights;
    }

    /**
     * Returns a single word-weight pair given a word index
     * @param index Index of word to retrieve
     * @return Pair of word and associated weight
     * @throws ArrayIndexOutOfBoundsException If the index provided is out of range
     */
    public Pair<String, Double> getWord(int index) throws ArrayIndexOutOfBoundsException {
        return new Pair<>(words[index], wordWeights[index]);
    }

    /**
     * Returns a list of word-weight pairs
     * @param maxWords Maximum number of words to return
     * @return List of word and their associated weights
     */
    public List<Pair<String, Double>> getWords(int maxWords){
        List<Pair<String, Double>> wordPairs = new ArrayList<>();
        for(int i = 0; i < maxWords; i++){
            wordPairs.add(getWord(i));
        }
        return wordPairs;
    }

    /**
     * Returns a list of word-weight pairs
     * @return List of word and their associated weights
     */
    public List<Pair<String, Double>> getWords(){
        return getWords(words.length);
    }

    /**
     * Returns an array of top words
     * @param maxWords maximum number of words to return
     * @return Array of top words in the topic
     */
    public String[] topWords(int maxWords){
        return Arrays.copyOfRange(words, 0, maxWords);
    }

    /**
     * Method generating a SparseVector of the words' distribution
     * @param size Size of the vocabulary
     * @return SparseVector of the words' distribution
     */
    public SparseVector getWordDistribution(int size){
        SparseVector wordVec = new SparseVector(size);
        for(int i = 0; i < words.length; i++){
            wordVec.put(wordIds[i], wordWeights[i]);
        }
        return wordVec.normalise();
    }

    /**
     * Returns a single document-weight pair given a document index
     * @param index Index of document to retrieve
     * @return Pair of document and associated weight
     * @throws ArrayIndexOutOfBoundsException If the index provided is out of range
     */
    public Pair<String, Double> getDocument(int index) throws ArrayIndexOutOfBoundsException {
        return new Pair<>(documents[index], docWeights[index]);
    }

    /**
     * Returns a list of document-weight pairs
     * @param maxDocuments Maximum number of documents to return
     * @return List of document and their associated weights
     */
    public List<Pair<String, Double>> getDocuments(int maxDocuments){
        List<Pair<String, Double>> docPairs = new ArrayList<>();
        for(int i = 0; i < maxDocuments; i++){
            docPairs.add(getDocument(i));
        }
        return docPairs;
    }

    /**
     * Returns a list of document-weight pairs
     * @return List of document and their associated weights
     */
    public List<Pair<String, Double>> getDocuments(){
        return getDocuments(documents.length);
    }

    /**
     * Returns an array of top document ids
     * @param maxDocuments maximum number of documents to return
     * @return Array of top document ids in the topic
     */
    public String[] topDocuments(int maxDocuments){
        return Arrays.copyOfRange(documents, 0, maxDocuments);
    }

}
