package data;

import IO.Console;
import IO.JSONHelper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.Serializable;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.DoubleStream;

/**
 * Class representing a topic.
 * Provides method for loading a typical model topic file.
 *
 * @author P. Le Bras
 * @version 1
 */
public class Topic implements Serializable {

    // Serialisation ID
    private static final long serialVersionUID = -1048734038308190794L;

    // List of static fields used when reading/writing JSON files
    private static final String JSON_NUM = "id";
    private static final String JSON_WORDS = "w";
    private static final String JSON_WORD_WEIGHTS = "ww";
    private static final String JSON_WORD_IDS = "wi";
    private static final String JSON_DOCS = "d";
    private static final String JSON_DOC_WEIGHTS = "dw";

    // Topic id
    private int number;
    // Sorted list (by assignment count) of unique labels (lemmas) assigned to this topic
    private String[] words;
    // List of assignment counts (in order) for each unique label (lemma) assigned to this topic
    private double[] wordWeights;
    // Sorted list (by assignment count) of unique labels (lemmas) identifiers assigned to this topic
    private int [] wordIds;
    // Sorted list (by weight) of documents ids where this topic is present
    private String[] documents;
    // List of weights (in order) for each document where this topic is present
    private double[] docWeights;

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
        number = (int) topicObj.get(JSON_NUM);
        words = JSONHelper.getStringArray((JSONArray) topicObj.get(JSON_WORDS));
        wordWeights = JSONHelper.getDoubleArray((JSONArray) topicObj.get(JSON_WORD_WEIGHTS));
        wordWeights = JSONHelper.getDoubleArray((JSONArray) topicObj.get(JSON_WORD_IDS));
        documents = JSONHelper.getStringArray((JSONArray) topicObj.get(JSON_DOCS));
        docWeights = JSONHelper.getDoubleArray((JSONArray) topicObj.get(JSON_DOC_WEIGHTS));
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

    /**
     * @return The JSON formatted topic
     */
    public JSONObject toJSON(){
        JSONObject topicJSON = new JSONObject();
        topicJSON.put(JSON_NUM, number);
        topicJSON.put(JSON_WORDS, JSONHelper.toJSONArray(words));
        topicJSON.put(JSON_WORD_WEIGHTS, JSONHelper.toJSONArray(formatArray(wordWeights)));
        topicJSON.put(JSON_WORD_IDS, JSONHelper.toJSONArray(formatArray(wordWeights)));
        topicJSON.put(JSON_DOCS, JSONHelper.toJSONArray(documents));
        topicJSON.put(JSON_DOC_WEIGHTS, JSONHelper.toJSONArray(formatArray(docWeights)));
        return topicJSON;
    }

    // used to format an array of doubles into 4 decimals max
    private double[] formatArray(double[] arr){
        DecimalFormat df = new DecimalFormat("#.#####");
        df.setRoundingMode(RoundingMode.HALF_UP);
        return DoubleStream.of(arr)
                .mapToObj(df::format)
                .mapToDouble(Double::parseDouble)
                .toArray();
    }

    /**
     * Method to load a list of topics from a JSON file
     * @param filename Name of topic JSON file
     * @return The list of topics
     * @throws IOException If reading the file fails
     * @throws ParseException If parsing JSON fails
     */
    public static List<Topic> loadTopics(String filename) throws IOException, ParseException {
        ArrayList<Topic> topics = new ArrayList<>();
        try {
            JSONArray input = JSONHelper.loadJSONArray(filename);
            for(int i = 0; i<input.size(); i++){
                JSONObject t = (JSONObject) input.get(i);
                topics.add(new Topic(t));
            }
            Console.note("Loaded "+topics.size()+" topics", 1);
        } catch (IOException e) {
            Console.error("Loading model topic file "+filename+" failed");
            throw e;
        } catch (ParseException e) {
            Console.error("Parsing model topic file "+filename+" failed");
            throw e;
        }

        return topics;
    }

    /**
     * Method to write a list of topics on a JSON file
     * @param filename Name of topic JSON file
     * @param topics The list of topics to save
     * @throws IOException If writing the file fails
     */
    public static void writeTopics(String filename, List<Topic> topics) throws IOException {
        Console.log("Saving topics");
        try{
            JSONArray topicsArray = new JSONArray();
            for(Topic t: topics){
                topicsArray.add(t.toJSON());
            }
            JSONHelper.saveJSONArray(topicsArray, filename, 1);
        } catch (IOException e){
            Console.error("Saving topics failed");
            throw e;
        }
    }
}
