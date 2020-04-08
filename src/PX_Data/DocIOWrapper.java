package PX_Data;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Wrapper class to store document data and write JSON files
 */
public class DocIOWrapper {

    private static String ToPrint = "";
    public static void PrintLemmas(){ToPrint = "Lemmas";}
    public static void PrintModel(){ToPrint = "Model";}

    private String docId;
    private int docIndex;
    private HashMap<String, String> docData;
    // used or set by lemmatise module
    private HashMap<String, String> docTexts;
    private int numLemmas;
    private String lemmaString;
    private List<String> lemmas;
    private boolean removed = false;
    private String removeReason = "";
    // used or set by topic modelling module
    private double[] topicDistribution;
    private double[] subTopicDistribution;

    /**
     * Basic constructor, typically used by Input modules
     * @param docId document id
     * @param docIndex document index
     */
    public DocIOWrapper(String docId, int docIndex){
        this.docId = docId;
        this.docIndex = docIndex;
        this.docData = new HashMap<>();
    }

    /**
     * Constructor to load document from an existing JSON file
     * @param jsonDoc document as JSON object
     */
    public DocIOWrapper(JSONObject jsonDoc){
        this.docId = (String) jsonDoc.get("docId");
        this.docIndex = Math.toIntExact((long) jsonDoc.get("docIndex"));
        this.docData = JSONIOWrapper.getStringMap((JSONObject) jsonDoc.get("docData"));
        // set by lemmatise module
        this.removed = (boolean) jsonDoc.getOrDefault("removed", false);
        this.removeReason = (String) jsonDoc.getOrDefault("removeReason", "");
        this.lemmaString = (String) jsonDoc.getOrDefault("lemmas", "");
        this.numLemmas = Math.toIntExact((long) jsonDoc.getOrDefault("numLemmas", (long) 0));
        // set by model module
        if(!this.removed){
            JSONArray distrib = (JSONArray) jsonDoc.getOrDefault("topicDistribution", null);
            if(distrib != null){
                this.topicDistribution = JSONIOWrapper.getDoubleArray(distrib);
            } else {
                distrib = (JSONArray) jsonDoc.getOrDefault("mainTopicDistribution", null);
                if(distrib != null){
                    this.topicDistribution = JSONIOWrapper.getDoubleArray(distrib);
                    distrib = (JSONArray) jsonDoc.getOrDefault("subTopicDistribution", null);
                    if(distrib != null){
                        this.subTopicDistribution = JSONIOWrapper.getDoubleArray(distrib);
                    }
                }
            }
        }
    }

    /**
     * Copy constructor, used by hierarchical topic model to have copies of docs
     * across multiple topic models, not all fields required
     * @param doc document to copy
     */
    public DocIOWrapper(DocIOWrapper doc){
        this.docId = doc.docId;
        this.docIndex = doc.docIndex;
        this.docData = doc.docData;
        this.lemmaString = doc.lemmaString;
        this.numLemmas = doc.numLemmas;
        this.removed = doc.removed;
        this.removeReason = doc.removeReason;
        if(doc.topicDistribution != null){
            this.topicDistribution = doc.topicDistribution;
            if(doc.subTopicDistribution != null){
                this.subTopicDistribution = doc.subTopicDistribution;
            }
        }
    }

    /**
     * Getter method for the document id
     * @return document id
     */
    public String getId(){
        return docId;
    }

    /**
     * Getter method for the document index
     * @return document index
     */
    public int getIndex(){
        return docIndex;
    }

    /**
     * Adds a new data entry to the document
     * @param key data key
     * @param value data value
     */
    public void addData(String key, String value){
        docData.put(key, value);
    }

    /**
     * Getter method for the whole document data
     * @return document data
     */
    public HashMap<String, String> getDocData(){
        return docData;
    }

    /**
     * Getter method for a particular data value
     * @param key data key
     * @return data value
     */
    public String getData(String key){
        return docData.get(key);
    }

    /**
     * Filter the document data to keep only desirable entries
     * @param keys keys to keep
     */
    public void filterData(List<String> keys){
        docData.entrySet().removeIf(e -> !keys.contains(e.getKey()));
    }

    /**
     * Copy sets of document data to text data for lemmatising
     * @param keys keys to copy
     */
    public void addTexts(List<String> keys){
        docTexts = new HashMap<>();
        keys.forEach(key -> addText(key));
    }

    /**
     * Copy document data entry to text data for lemmatising
     * @param key data key
     */
    public void addText(String key){
        if(docTexts == null) docTexts = new HashMap<>();
        docTexts.put(key, docData.get(key));
    }

    /**
     * Getter methods for a text entry
     * @param key entry key
     * @return text value
     */
    public String getText(String key){
        return docTexts.get(key);
    }

    /**
     * Setter method for the document lemma string
     * @param inputLemmas list of lemmas to add
     */
    public void setLemmas(List<String> inputLemmas){
        numLemmas = inputLemmas.size();
        lemmas = inputLemmas;
    }

    /**
     * Getter method for the list of lemmas
     * @return lemmas list
     */
    public List<String> getLemmas(){
        return lemmas;
    }

    /**
     * Removes a single lemma from the lemmas list
     * @param lemmaToRemove lemma to remove
     */
    public void removeLemma(String lemmaToRemove){
        lemmas.removeIf(lemmaToRemove::equals);
        numLemmas = lemmas.size();
    }

    public void filterOutLemmas(List<String> lemmasToRemove){
        lemmas.removeIf(lemmasToRemove::contains);
        numLemmas = lemmas.size();
    }

    /**
     * Construct the lemma string from the list of lemmas
     */
    public void makeLemmaString(){
        lemmaString = "";
        lemmas.forEach(text -> lemmaString += text + " ");
        lemmaString = lemmaString.trim();
    }

    /**
     * Getter for lemmas
     * @return lemma string
     */
    public String getLemmaString(){
        return lemmaString;
    }

    /**
     * Removes the document from modelling, giving a reason
     * @param reason reason for removal
     */
    public void remove(String reason){
        removed = true;
        removeReason = reason;
    }

    /**
     * Getter for removed value
     * @return removed value
     */
    public boolean isRemoved(){
        return removed;
    }

    /**
     * Setter for the distribution over main topics
     * @param distribution topic distribution
     */
    public void setTopicDistribution(double[] distribution){
        DecimalFormat df = new DecimalFormat("#.####");
        df.setRoundingMode(RoundingMode.UP);
        topicDistribution = new double[distribution.length];
        for(int i = 0; i < distribution.length; i++){
            topicDistribution[i] = Double.parseDouble(df.format(distribution[i]));
        }
    }

    /**
     * Getter for the distribution over main topics
     * @return main topic distribution
     */
    public double[] getTopicDistribution(){
        return topicDistribution;
    }

    /**
     * Setter for the distribution over sub topics
     * @param distribution topic distribution
     */
    public void setSubTopicDistribution(double[] distribution){
        // sub topic model should have already formatted values
        subTopicDistribution = distribution;
    }

    /**
     * Getter for the distribution over sub topics
     * @return sub topic distribution
     */
    public double[] getSubTopicDistribution(){
        return topicDistribution;
    }

    /**
     * Creates a JSON object of the document to save in JSON file
     * @return JSON object of the document
     */
    public JSONObject toJSON(){
        JSONObject root = new JSONObject();
        // Saving id and index
        root.put("docId", docId);
        root.put("docIndex", docIndex);
        // Saving doc data
        JSONObject data = new JSONObject();
        for(Map.Entry<String, String> entry: docData.entrySet()){
            data.put(entry.getKey(), entry.getValue());
        }
        root.put("docData", data);
        // Saving removed data
        if(removed){
            root.put("removed", true);
            root.put("removeReason", removeReason);
        }
        // Saving Lemmas
        if(ToPrint.equals("Lemmas")){
            root.put("numLemmas", numLemmas);
            root.put("lemmas", lemmaString);
        }
        // Saving Model
        else if(ToPrint.equals("Model") && !removed){
            root.put("numLemmas", numLemmas);
            JSONArray topicDistrib = getDistribJSON(topicDistribution);
            if(subTopicDistribution == null){
                root.put("topicDistribution", topicDistrib);
            } else {
                root.put("mainTopicDistribution", topicDistrib);
                topicDistrib = getDistribJSON(subTopicDistribution);
                root.put("subTopicDistribution", topicDistrib);
            }
        }
        return root;
    }

    /**
     * Generates a JSON Array from a topic distribution
     * @param distrib distribution to convert
     * @return JSON array of distribution
     */
    private JSONArray getDistribJSON(double[] distrib){
        JSONArray res = new JSONArray();
        for(double value: distrib){
            res.add(value);
        }
        return res;
    }
}
