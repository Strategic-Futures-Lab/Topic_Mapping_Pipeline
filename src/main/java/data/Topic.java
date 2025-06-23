package data;

import IO.JSONHelper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

/**
 * Class representing a topic.
 * Provides method for loading a typical model topic file.
 *
 * @author P. Le Bras
 * @version 1
 */
public class Topic implements Serializable {

    @Serial
    private static final long serialVersionUID = -1048734038308190794L;

    // List of static fields used when reading/writing JSON files
    private static final String JSON_NUM = "i";
    private static final String JSON_WORDS = "w";
//    private static final String JSON_WORD_WEIGHTS = "ww";
//    private static final String JSON_WORD_IDS = "wi";
    private static final String JSON_DOCS = "d";
//    private static final String JSON_DOC_WEIGHTS = "dw";

    // Topic id
    private final int number;
    private String model;
//    // Sorted list (by assignment count) of unique labels (lemmas) assigned to this topic
//    private String[] words;
//    // List of assignment counts (in order) for each unique label (lemma) assigned to this topic
//    private double[] wordWeights;
//    // Sorted list (by assignment count) of unique labels (lemmas) identifiers assigned to this topic
//    private int [] wordIds;

    private ModelFeature[] words;
    // Sorted list (by weight) of documents ids where this topic is present
    private ModelFeature[] documents;
//    // List of weights (in order) for each document where this topic is present
//    private double[] docWeights;

    /**
     * Initial constructor
     * @param topicNumber topic number used in the model
     */
    public Topic(int topicNumber){
        number = topicNumber;
    }

    /**
     * JSON constructor
     * @param topicObj JSON object representing the topic
     */
    public Topic(JSONObject topicObj){
        number = (int) (long) topicObj.get(JSON_NUM);
        JSONObject[] wordsJSON = JSONHelper.getJSONObjectArray((JSONArray) topicObj.get(JSON_WORDS));
        words = new ModelFeature[wordsJSON.length];
        for(int i=0; i< words.length; i++){
            words[i] = new ModelFeature(wordsJSON[i]);
        }
        JSONObject[] docsJSON = JSONHelper.getJSONObjectArray((JSONArray) topicObj.get(JSON_DOCS));
        documents = new ModelFeature[docsJSON.length];
        for(int i=0; i< documents.length; i++){
            documents[i] = new ModelFeature(docsJSON[i]);
        }
//        wordWeights = JSONHelper.getDoubleArray((JSONArray) topicObj.get(JSON_WORD_WEIGHTS));
//        wordWeights = JSONHelper.getDoubleArray((JSONArray) topicObj.get(JSON_WORD_IDS));
//        documents = JSONHelper.getStringArray((JSONArray) topicObj.get(JSON_DOCS));
//        docWeights = JSONHelper.getDoubleArray((JSONArray) topicObj.get(JSON_DOC_WEIGHTS));
    }

    /**
     * Setter method of the model name.
     * Should only be used for model modules
     * @param modelName
     */
    public void setModelName(String modelName){
        model = modelName;
    }

    /**
     * Getter for the topic number/index
     * @return The topic number
     */
    public int getNumber(){
        return number;
    }

    /**
     * Getter for the topic's identifier (model+number)
     * @return topic identifier
     */
    public String getTopicId(){
        return model+"_"+number;
    }

    /**
     * Getter for the topic's identifier (model+number) and top words
     * @param numWords number of words to append to identifier
     * @return topic identifier + top words
     */
    public String getTopicIdWords(int numWords){
        return model+"_"+number+"-"+String.join("_", getTopWords(numWords));
    }

    /**
     * Getter for the topic's identifier (model+number) and top 3 words
     * @return topic identifier + top words
     */
    public String getTopicIdWords(){
        return getTopicIdWords(3);
    }

    /**
     * Setter method for word assignment
     * @param words list of words
     * @param wordIndices list of word ids in the model
     * @param wordWeights distribution of word weights across the topic
     */
    public void setWordAssignments(String[] words, int[] wordIndices, double[] wordWeights){
        this.words = new ModelFeature[words.length];
        for(int i=0; i<this.words.length; i++){
            this.words[i] = new ModelFeature(words[i], wordIndices[i], wordWeights[i]);
        }
    }

    /**
     * Setter method for document assignment
     * @param documentIds list of document ids
     * @param documentWeights distribution of this topic's weight across documents
     */
    public void setDocumentAssignments(String[] documentIds, int[] documentIndices, double[] documentWeights){
        documents = new ModelFeature[documentIds.length];
        for(int i=0; i<documents.length; i++){
            documents[i] = new ModelFeature(documentIds[i], documentIndices[i], documentWeights[i]);
        }
//        documents = documentIds;
//        docWeights = documentWeights;
    }

//    /**
//     * Returns a single word-weight pair given a word index
//     * @param index Index of word to retrieve
//     * @return Pair of word and associated weight
//     * @throws ArrayIndexOutOfBoundsException If the index provided is out of range
//     */
//    public Pair<String, Double> getWord(int index) throws ArrayIndexOutOfBoundsException {
//        return new Pair<>(words[index].getLabel(), words[index].getWeight());
//    }
//
//    /**
//     * Returns a list of word-weight pairs
//     * @param maxWords Maximum number of words to return
//     * @return List of word and their associated weights
//     */
//    public List<Pair<String, Double>> getWords(int maxWords){
//        List<Pair<String, Double>> wordPairs = new ArrayList<>();
//        for(int i = 0; i < maxWords; i++){
//            wordPairs.add(getWord(i));
//        }
//        return wordPairs;
//    }
//
//    /**
//     * Returns a list of word-weight pairs
//     * @return List of word and their associated weights
//     */
//    public List<Pair<String, Double>> getWords(){
//        return getWords(words.length);
//    }

    /**
     * Returns a list of top words, sorted by weight
     * @param maxWords maximum number of words to return
     * @return List of top words in the topic
     */
    public List<String> getTopWords(int maxWords){
        return Arrays.stream(Arrays.copyOfRange(words, 0, maxWords)).map(w->w.getLabel()).toList();
    }

    /**
     * Returns a list of all words, sorted by weight
     * @return List of all words in the topic
     */
    public List<String> getWords(){
        return Arrays.stream(words).map(w->w.getLabel()).toList();
    }

    /**
     * Method generating a SparseVector of the words' distribution
     * @return SparseVector of the words' distribution
     */
    public SparseVector getWordDistribution(){
        SparseVector wordVec = new SparseVector(0);
        for(int i = 0; i < words.length; i++){
            wordVec.put(words[i].getIndex(), words[i].getWeight());
        }
        wordVec.normalise();
        return wordVec;
    }

//    /**
//     * Returns a single document-weight pair given a document index
//     * @param index Index of document to retrieve
//     * @return Pair of document and associated weight
//     * @throws ArrayIndexOutOfBoundsException If the index provided is out of range
//     */
//    public Pair<String, Double> getDocument(int index) throws ArrayIndexOutOfBoundsException {
//        return new Pair<>(documents[index].getLabel(), documents[index].getWeight());
//    }
//
//    /**
//     * Returns a list of document-weight pairs
//     * @param maxDocuments Maximum number of documents to return
//     * @return List of document and their associated weights
//     */
//    public List<Pair<String, Double>> getDocuments(int maxDocuments){
//        List<Pair<String, Double>> docPairs = new ArrayList<>();
//        for(int i = 0; i < maxDocuments; i++){
//            docPairs.add(getDocument(i));
//        }
//        return docPairs;
//    }
//
//    /**
//     * Returns a list of document-weight pairs
//     * @return List of document and their associated weights
//     */
//    public List<Pair<String, Double>> getDocuments(){
//        return getDocuments(documents.length);
//    }

    /**
     * Returns a list of top document ids, sorted by weight
     * @param maxDocuments maximum number of documents to return
     * @return List of top document ids in the topic
     */
    public List<String> getTopDocuments(int maxDocuments){
        return Arrays.stream(Arrays.copyOfRange(documents, 0, maxDocuments)).map(d->d.getLabel()).toList();
    }

    /**
     * Returns a list of all document ids, sorted by weight
     * @return List of all document ids in the topic
     */
    public List<String> getDocuments(){
        return Arrays.stream(documents).map(d->d.getLabel()).toList();
    }

    /**
     * Method generating a SparseVector of the documents' distribution
     * @return SparseVector of the documents' distribution
     */
    public SparseVector getDocumentDistribution(){
        SparseVector docVec = new SparseVector(0);
        for(int i = 0; i < documents.length; i++){
            docVec.put(documents[i].getIndex(), documents[i].getWeight());
        }
        docVec.normalise();
        return docVec;
    }

    /**
     * @return The JSON formatted topic
     */
    public JSONObject toJSON(){
        JSONObject topicJSON = new JSONObject();
        topicJSON.put(JSON_NUM, number);
        JSONArray wordsJSON = new JSONArray();
        for(ModelFeature w: words){
            wordsJSON.add(w.toJSON());
        }
        topicJSON.put(JSON_WORDS, wordsJSON);
        JSONArray documentsJSON = new JSONArray();
        for(ModelFeature d: documents){
            documentsJSON.add(d.toJSON());
        }
        topicJSON.put(JSON_DOCS, documentsJSON);
//        topicJSON.put(JSON_WORD_WEIGHTS, JSONHelper.toJSONArray(formatArray(wordWeights)));
//        topicJSON.put(JSON_WORD_IDS, JSONHelper.toJSONArray(wordIds));
//        topicJSON.put(JSON_DOCS, JSONHelper.toJSONArray(documents));
//        topicJSON.put(JSON_DOC_WEIGHTS, JSONHelper.toJSONArray(formatArray(docWeights)));
        return topicJSON;
    }

}
