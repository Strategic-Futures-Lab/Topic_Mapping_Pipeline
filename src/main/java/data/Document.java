package data;

import IO.JSONHelper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.Serial;
import java.io.Serializable;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.DoubleStream;

/**
 * Class representing a document.
 * Provides functionalities to create a new document, read it from JSON formatted objects, set and access attributes
 * and transform it into a JSON format to write on file.
 *
 * @author P. Le Bras
 * @version 2
 */
public class Document implements Serializable {

    @Serial
    private static final long serialVersionUID = 2244011647262167470L;

    // List of static fields used when reading/writing JSON files
    private static final String JSON_ID = "id";
    private static final String JSON_IDX = "i";
    private static final String JSON_DATA = "d";
    private static final String JSON_TEXT = "txt";
    private static final String JSON_LEMMAS = "l";
    private static final String JSON_WORDS = "w";
    private static final String JSON_TOPICS = "t";
//    private static final String JSON_TOPIC_SEQ = "ts";
//    private static final String JSON_TOPIC_CNT = "tc";
//    private static final String JSON_TOPIC_WEIGHTS = "tw";
//    private static final String JSON_TOPIC_DIST = "td";
//    private static final String JSON_PART_TOPIC_DIST = "ptd";

    // Document  id and index
    private String id;
    private int index;
    // Document data
    private final HashMap<String, String> fields;
    // created by text builder module
    private String text;
    // created by lemmatise module
    private List<List<String>> lemmasList;
    // created by model module
    private ModelFeature[] words;
    private ModelFeature[] topics;
//    private int[] topicCount;
//    private double[] topicDistribution;
    // TODO might not be needed
//    private int[] topicSequence;
    // analytics
    // TODO see about moving to dedicated module
//    private double[] topicDistances;
//    private double[] partialTopicDistances;

    /**
     * Initial constructor, used by input modules
     * @param id Document id
     * @param index Document index
     */
    public Document(String id, int index){
        this.id = id;
        this.index = index;
        this.fields = new HashMap<>();
    }

    /**
     * JSON constructor, used when loading from JSON file
     * @param doc Document to parse
     */
    public Document(JSONObject doc){
        id = (String) doc.get(JSON_ID);
        index = Math.toIntExact((long) doc.get(JSON_IDX));
        fields = JSONHelper.getStringMap((JSONObject) doc.get(JSON_DATA));
        // set by text builder module
        text = (String) doc.get(JSON_TEXT);
        // set by lemmatise module
        parseLemmas((JSONArray) doc.get(JSON_LEMMAS));
        // set by model
        JSONObject[] wordsJSON = JSONHelper.getJSONObjectArray((JSONArray) doc.get(JSON_WORDS));
        if(wordsJSON != null) {
            words = new ModelFeature[wordsJSON.length];
            for (int i = 0; i < words.length; i++) {
                words[i] = new ModelFeature(wordsJSON[i]);
            }
        }
        JSONObject[] topicsJSON = JSONHelper.getJSONObjectArray((JSONArray) doc.get(JSON_TOPICS));
        if(topicsJSON != null) {
            topics = new ModelFeature[topicsJSON.length];
            for (int i = 0; i < topics.length; i++) {
                topics[i] = new ModelFeature(topicsJSON[i]);
            }
        }
//        topicSequence = JSONHelper.getIntArray((JSONArray) doc.get(JSON_TOPIC_SEQ));
//        topicCount = JSONHelper.getIntArray((JSONArray) doc.get(JSON_TOPIC_CNT));
//        topicDistribution = JSONHelper.getDoubleArray((JSONArray) doc.get(JSON_TOPIC_WEIGHTS));
//        topicDistances = JSONHelper.getDoubleArray((JSONArray) doc.get(JSON_TOPIC_DIST));
//        partialTopicDistances = JSONHelper.getDoubleArray((JSONArray) doc.get(JSON_PART_TOPIC_DIST));
    }

    // Parses a string of lemmas (separated by space) and saves into the list of lemmas
    private void parseLemmas(JSONArray lemmas){
        if(lemmas != null && !lemmas.isEmpty()){
            lemmasList = new LinkedList<>();
            for(Object sentence: lemmas){
                lemmasList.add(List.of(sentence.toString().split(" ")));
            }
        }
    }

    /**
     * Copy constructor, used to have multiple copies of documents across models;
     * Might deprecate
     * @param doc Document to copy
     */
    public Document(Document doc){
        id = doc.id;
        index = doc.index;
        fields = doc.fields;
        // set by text builder module
        text = doc.text;
        // set by lemmatise module
        lemmasList = doc.lemmasList;
        // set by model
        words = doc.words;
        topics = doc.topics;
    }

    /**
     * Getter method for the document id
     * @return The document id
     */
    public String getId(){ return id; }

    /**
     * Setter method for the document id
     * **WARNING**: USE WITH CAUTION, IDEALLY ONLY BEFORE SAVING ON FILE
     * @param id The new id
     */
    public void setId(String id){ this.id = id; }

    /**
     * Adds a prefix to the doc id, e.g. for inferred documents
     * @param p Prefix to add
     */
    public void prefixId(String p){ id = p+id; }

    /**
     * Adds a suffix to the doc id, e.g. for split documents
     * @param s Suffix to add
     */
    public void suffixId(String s){ id = id+s; }

    /**
     * Getter method for the document index
     * @return The document index
     */
    public int getIndex(){ return index; }

    /**
     * Setter method for the document index
     * **WARNING**: USE WITH CAUTION, IDEALLY ONLY BEFORE SAVING ON FILE
     * @param index The new index
     */
    public void setIndex(int index){ this.index = index; }

    /*******************************************************************
     * FIELDS OPERATIONS
     *******************************************************************/

    /**
     * Adds a new data entry to the document
     * @param key Data key
     * @param value Data value
     */
    public void addField(String key, String value){ fields.put(key, value); }

    /**
     * Getter method for the whole document data
     * @return The document data
     */
    public HashMap<String, String> getFields(){ return fields; }

    /**
     * Getter method for the document data keys
     * @return The document data keys
     */
    public Set<String> getFieldsKey() { return fields.keySet(); }

    /**
     * Getter method for a particular data value, returns null if not found
     * @param key Data key
     * @return The data value or null if key is not found
     */
    public String getField(String key){ return fields.get(key); }

    /**
     * Getter method for a particular data value, returns a default value if not found
     * @param key Data key
     * @param def Default value to return
     * @return The data value
     */
    public String getFieldOr(String key, String def){ return fields.getOrDefault(key, def); }

    /**
     * Checks for a particular data key
     * @param key Data key to check
     * @return Boolean for key existing
     */
    public boolean hasField(String key){ return fields.containsKey(key); }

    /**
     * Filters the document data to keep only desirable entries
     * @param keys Data keys to keep
     */
    public void filterFields(List<String> keys){ fields.entrySet().removeIf(e -> !keys.contains(e.getKey())); }

    /*******************************************************************
     * TEXT OPERATIONS
     *******************************************************************/

    private void initText(){ if(text==null) text = ""; }

    /**
     * Copies a set of fields to the text String;
     * Ignores keys not found in fields
     * @param keys Field keys to copy
     */
    public void addTexts(List<String> keys){
        initText();
        keys.forEach(this::addText);
    }

    /**
     * Copies a field to the text String;
     * Does nothing if the key is not found in hte fields
     * @param key Field key to copy
     */
    public void addText(String key){
        if(fields.containsKey(key)) {
            initText();
            text += (text.isEmpty() ? "" : " \n\n ") + fields.get(key);
        }
    }

    /**
     * Getter for the document text String
     * @return The text String
     */
    public String getText(){ return text; }

    /**
     * Setter for the document text String
     * @param txt Text to set as document text
     */
    public void setText(String txt){ text = txt; }

    /**
     * Checks if the document's text is missing or empty
     * @return Boolean for text empty/missing
     */
    public boolean emptyText(){ return text == null || text.isEmpty(); }

    /*******************************************************************
     * LEMMAS OPERATIONS
     *******************************************************************/

    /**
     * Returns the list of lemmas as one String, concatenated with a space;
     * Returns and empty String if not lemmas are set
     * @return The lemma String
     */
    public String getLemmasString(){
        StringBuilder lemmas = new StringBuilder();
        if(lemmasList!=null){
            for(List<String> sentence: lemmasList){
                lemmas.append(String.join(" ", sentence));
                lemmas.append(" ");
            }
        }
        return lemmas.toString();
    }

    /**
     * Returns the number of lemmas
     * @return The number of lemmas
     */
    public int getNumLemmas(){
        if (lemmasList==null) return 0;
        else{
            int count = 0;
            for(List<String> sentence: lemmasList) count += sentence.size();
            return count;
        }
    }

    /**
     * Setter for the list of lemmas
     * @param lemmas List of lemmas to set
     */
    public void setLemmas(List<List<String>> lemmas){
        lemmasList = lemmas;
    }

    /**
     * Getter for the list of lemmas
     * @return The lemmas list
     */
    public List<List<String>> getLemmas(){
        if(lemmasList != null) return lemmasList;
        return new ArrayList<>();
    }

    /**
     * Setter for the list of lemmas sentences
     * @param sentences List of lemmas sentences to set
     */
    public void setLemmaSentences(List<String> sentences){
        lemmasList = sentences.stream().map(s -> List.of(s.split(" "))).toList();
    }

    /**
     * Getter for the list of lemmas sentences
     * @return The list of lemmas sentences
     */
    public List<String> getLemmaSentences(){
        if(lemmasList != null) return lemmasList.stream().map(l -> String.join(" ", l)).toList();
        return new ArrayList<>();
    }

    /**
     * Removes a single lemma sentence from the lemmas list
     * @param lemma Lemma to remove
     */
    public void removeLemma(String lemma){
        if(lemmasList!=null){
            for(List<String> sentence: lemmasList){
                sentence.removeIf(lemma::equals);
            }
        }
    }

    /**
     * Removes a set of lemmas from the lemmas list
     * @param lemmas List of lemmas to remove
     */
    public void removeLemmas(List<String> lemmas){
        if(lemmasList!=null){
            for(List<String> sentence: lemmasList){
                sentence.removeIf(lemmas::contains);
            }
        }
    }

    /**
     * Checks if the document's list of lemmas is missing
     * @return Boolean for lemma list present
     */
    public boolean hasLemmas() { return lemmasList != null; }

    /*******************************************************************
     * MODEL OPERATIONS
     *******************************************************************/

    /**
     * Setter method for the document's words
     * @param labels list of words as they appear in the document (may differ from text)
     * @param ids list of word ids, in order of appearance in the document (set by model)
     */
    public void setWords(String[] labels, int[] ids){
        Map<String, Integer> labelIds = new HashMap<>();
        Map<String, Integer> labelCounts = new HashMap<>();
        for(int i=0; i<labels.length; i++) {
            if (!labelIds.containsKey(labels[i])) {
                labelIds.put(labels[i], ids[i]);
                labelCounts.put(labels[i], 1);
            } else {
                labelCounts.put(labels[i], labelCounts.get(labels[i]) + 1);
            }
        }
        words = new ModelFeature[labelIds.size()];
        int i = 0;
        for(String label: labelIds.keySet()){
            words[i] = new ModelFeature(label, labelIds.get(label), labelCounts.get(label));
            i++;
        }
    }

    /**
     * Setter method for the document's topic distribution
     * @param distribution list of topic weights
     */
    public void setTopics(double[] distribution){
        topics = new ModelFeature[distribution.length];
        for(int i=0; i<distribution.length; i++){
            topics[i] = new ModelFeature(Integer.toString(i), i, distribution[i]);
        }
    }

    /**
     * Getter for vector representation of word distribution
     * @param size SparseVector theoretical size (vocabulary size)
     * @return SparseVector of word distribution
     */
    public SparseVector getWordsVector(int size){
        SparseVector wordVec = new SparseVector(size);
        for(ModelFeature w: words){
            wordVec.put(w.getIndex(), w.getWeight());
        }
        return wordVec;
    }

    /**
     * Getter for vector representation of topic distribution.
     * The SparseVector theoretical size is automatically derived from the number of topics.
     * @return SparseVector of topic distribution
     */
    public SparseVector getTopicsVector(){
        SparseVector topicVec = new SparseVector(topics.length);
        for(ModelFeature t: topics){
            topicVec.put(t.getIndex(), t.getWeight());
        }
        return topicVec;
    }

    /**
     * Method checking if the document was part of the model, i.e., has a topic distribution
     * @return True if a topic distribution is present
     */
    public boolean isModelled(){
        return topics != null;
    }

//    /**
//     * Setter method for the document's topic assignment
//     * @param sequence topic assignment for each word
//     * @param count number of assignments for each topic
//     * @param distribution topic weights in the document
//     */
//    public void setTopicAssignment(int[] sequence, int[] count, double[] distribution){
//        topicSequence = sequence;
//        topicCount = count;
//        topicDistribution = distribution;
//    }

//    private SparseVector getWordDistribution(int size){
//        SparseVector wordVec = new SparseVector(size);
//        for(int i=0; i < wordIds.length; i++){
//            wordVec.put(wordIds[i], wordVec.get(wordIds[i])+1.0);
//        }
//        return wordVec.normalise();
//    }

//    private SparseVector getPartialWordDistribution(int size, int topic){
//        SparseVector wordVec = new SparseVector(size);
//        for(int i = 0; i < topicSequence.length; i++){
//            if(topicSequence[i] == topic){
//                wordVec.put(wordIds[i], wordVec.get(wordIds[i])+1.0);
//            }
//        }
//        return wordVec.normalise();
//    }

//    /**
//     * Setter method for the document to topic distance
//     * Used for analytics, TODO: export to dedicated module
//     * @param topicVectors List of topic vector to calculate distances against
//     */
//    public void setDistancesFromTopics(List<SparseVector> topicVectors){
//        SparseVector fullDocVector = getWordDistribution(topicVectors.get(0).size());
//        int nTopics = topicDistribution.length;
//        topicDistances = new double[nTopics];
//        partialTopicDistances = new double[nTopics];
//        for(int t = 0; t < nTopics; t++){
//            SparseVector topicVec = topicVectors.get(t);
//            topicDistances[t] = SparseVector.HellingerDistance(topicVec, fullDocVector);
//            if(topicCount[t] > 0) {
//                SparseVector compDocVector = getPartialWordDistribution(topicVec.size(), t);
//                partialTopicDistances[t] = SparseVector.HellingerDistance(topicVec, compDocVector);
//            } else {
//                // the document component will be empty, so distance is 1
//                partialTopicDistances[t] = 1;
//            }
//        }
//    }

    /**
     * Formats the document into a JSON object to write on file
     * @return The JSON formatted document
     */
    public JSONObject toJSON(){
        JSONObject root = new JSONObject();
        // Saving id and index
        root.put(JSON_ID, id);
        root.put(JSON_IDX, index);
        // Saving fields
        JSONObject data = new JSONObject();
        data.putAll(fields);
        root.put(JSON_DATA, data);
        // Saving text
        if(text!=null && !text.isEmpty()){
            root.put(JSON_TEXT, text);
        }
        // Saving Lemmas
        if(lemmasList!=null){
            JSONArray lemmas = new JSONArray();
            for(List<String> sentence: lemmasList){
                lemmas.add(String.join(" ", sentence));
            }
            root.put(JSON_LEMMAS, lemmas);
        }
        // saving model data
        if(words!=null){
            JSONArray wordsJSON = new JSONArray();
            for(ModelFeature w: words){
                wordsJSON.add(w.toJSON());
            }
            root.put(JSON_WORDS, wordsJSON);
        }
        if(topics!=null){
            JSONArray topicsJSON = new JSONArray();
            for(ModelFeature t: topics){
                topicsJSON.add(t.toJSON());
            }
            root.put(JSON_TOPICS, topicsJSON);
        }
//        if(topicSequence!=null){
//            root.put(JSON_TOPIC_SEQ, JSONHelper.toJSONArray(topicSequence));
//        }
//        if(topicCount!=null){
//            root.put(JSON_TOPIC_CNT, JSONHelper.toJSONArray(topicCount));
//        }
//        if(topicDistribution!=null) {
//            root.put(JSON_TOPIC_WEIGHTS, JSONHelper.toJSONArray(formatArray(topicDistribution)));
//        }
//        if(topicDistances != null){
//            root.put(JSON_TOPIC_DIST, JSONHelper.toJSONArray(formatArray(topicDistances)));
//        }
//        if(partialTopicDistances != null){
//            root.put(JSON_PART_TOPIC_DIST, JSONHelper.toJSONArray(formatArray(partialTopicDistances)));
//        }
        return root;
    }

    // private method for formating double array
//    private double[] formatArray(double[] arr){
//        DecimalFormat df = new DecimalFormat("#.#####");
//        df.setRoundingMode(RoundingMode.HALF_UP);
//        return DoubleStream.of(arr)
//                .mapToObj(df::format)
//                .mapToDouble(Double::parseDouble)
//                .toArray();
//    }

}
