package PX_Data;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.*;

/**
 * Class representing a document.
 * Provides functionalities to create a new document, read it from JSON formatted objects, set and access attributes
 * and transform it into a JSON format to write on file.
 *
 * @author P. Le Bras
 * @version 1
 */
public class DocIOWrapper {

    /** Indicators of the level of information to save:
     * <br>"" -> basic document data
     * <br>"Lemmas" -> above + lemmatised text
     * <br>"Model" -> above + topic weights */
    private static String ToPrint = "";
    /** Sets {@link DocIOWrapper#ToPrint} to "Lemmas" */
    public static void PrintLemmas(){ToPrint = "Lemmas";}
    /** Sets {@link DocIOWrapper#ToPrint} to "Model" */
    public static void PrintModel(){ToPrint = "Model";}

    /** The document id. */
    private String docId;
    /** The document index. */
    private int docIndex;
    /** The document data, as read by the {@link P1_Input} modules. */
    private HashMap<String, String> docData;
    // used or set by lemmatise module
    /** Temporary structure used by the {@link P2_Lemmatise.Lemmatise} module to construct the text to be lemmatised. */
    private HashMap<String, String> docTexts;
    /** Number of lemmas for the document. */
    private int numLemmas;
    /** The document lemmas. */
    private String lemmaString;
    /** Temporary structure used by the {@link P2_Lemmatise.Lemmatise} module to store individual lemmas in a list a
     * facilitate operations. */
    private List<String> lemmas;
    /** Flag set by the {@link P2_Lemmatise.Lemmatise} module, marking the document as too short, excluding it from
     * the model. */
    private boolean tooShort = false;
    // used or set by topic modelling module
    /** The weight distribution of main topics in the document. */
    private double[] mainTopicDistribution;
    /** The weight distribution of sub topics in the document. */
    private double[] subTopicDistribution;
    // used of set by document inference module
    /** Flag set by the {@link P3_TopicModelling.InferDocuments} module, marking the document as inferred, ie not
     * counting in the elaboration of topics. */
    private boolean inferred = false;

    /**
     * Basic constructor, typically used by Input modules.
     * @param docId Document id.
     * @param docIndex Document index.
     */
    public DocIOWrapper(String docId, int docIndex){
        this.docId = docId;
        this.docIndex = docIndex;
        this.docData = new HashMap<>();
    }

    /**
     * Constructor to load document from an existing JSON file.
     * @param jsonDoc Document as JSON object.
     */
    public DocIOWrapper(JSONObject jsonDoc){
        this.docId = (String) jsonDoc.get("docId");
        this.docIndex = Math.toIntExact((long) jsonDoc.get("docIndex"));
        this.docData = JSONIOWrapper.getStringMap((JSONObject) jsonDoc.get("docData"));
        // set by lemmatise module
        this.tooShort = (boolean) jsonDoc.getOrDefault("tooShort", false);
        this.lemmaString = (String) jsonDoc.getOrDefault("lemmas", "");
        this.numLemmas = Math.toIntExact((long) jsonDoc.getOrDefault("numLemmas", (long) 0));
        // set by model module
        if(!this.isRemoved()){
            JSONArray distrib = (JSONArray) jsonDoc.getOrDefault("mainTopicDistribution", null);
            if(distrib != null){
                this.mainTopicDistribution = JSONIOWrapper.getDoubleArray(distrib);
                distrib = (JSONArray) jsonDoc.getOrDefault("subTopicDistribution", null);
                if(distrib != null){
                    this.subTopicDistribution = JSONIOWrapper.getDoubleArray(distrib);
                }
            }
            // set by inference module
            this.inferred = (boolean) jsonDoc.getOrDefault("inferred", false);
        }
    }

    /**
     * Copy constructor, used by hierarchical topic model to have copies of docs
     * across multiple topic models, not all fields required.
     * @param doc Document to copy.
     */
    public DocIOWrapper(DocIOWrapper doc){
        this.docId = doc.docId;
        this.docIndex = doc.docIndex;
        this.docData = doc.docData;
        this.lemmaString = doc.lemmaString;
        this.numLemmas = doc.numLemmas;
        this.tooShort = doc.tooShort;
        if(doc.mainTopicDistribution != null){
            this.mainTopicDistribution = doc.mainTopicDistribution;
            if(doc.subTopicDistribution != null){
                this.subTopicDistribution = doc.subTopicDistribution;
            }
        }
        this.inferred = doc.inferred;
    }

    /**
     * Getter method for the document id.
     * @return The document id.
     */
    public String getId(){
        return docId;
    }

    /**
     * Setter method for the document id.
     * **WARNING**: USE WITH CAUTION, IDEALLY ONLY BEFORE SAVING ON FILE
     * @param id The new id.
     */
    public void setId(String id){ docId = id; }

    /**
     * Adds a prefix to the doc id, eg for inferred documents.
     * @param p Prefix to add.
     */
    public void prefixId(String p){
        docId = p + docId;
    }

    /**
     * Getter method for the document index.
     * @return The document index
     */
    public int getIndex(){
        return docIndex;
    }

    /**
     * Setter method for the document index.
     * **WARNING**: USE WITH CAUTION, IDEALLY ONLY BEFORE SAVING ON FILE
     * @param index The new index.
     */
    public void setIndex(int index){ docIndex = index; }

    /**
     * Adds a new data entry to the document.
     * @param key Data key.
     * @param value Data value.
     */
    public void addData(String key, String value){
        docData.put(key, value);
    }

    /**
     * Getter method for the whole document data.
     * @return The document data.
     */
    public HashMap<String, String> getDocData(){
        return docData;
    }

    /**
     * Getter method for a particular data value, returns an empty string value if not found.
     * @param key Data key.
     * @return The data value.
     */
    public String getData(String key){
        return docData.getOrDefault(key, "");
    }

    /**
     * Getter method for a particular data value, returns a default value if not found.
     * @param key Data key.
     * @param def Default value to return.
     * @return The data value.
     */
    public String getDataOr(String key, String def){
        return docData.getOrDefault(key, def);
    }

    /**
     * Checks for a particular data key.
     * @param key Data key to check.
     * @return Boolean for key existing.
     */
    public boolean hasData(String key){
        return docData.containsKey(key);
    }

    /**
     * Filters the document data to keep only desirable entries.
     * @param keys Data keys to keep.
     */
    public void filterData(List<String> keys){
        docData.entrySet().removeIf(e -> !keys.contains(e.getKey()));
    }

    /**
     * Copies sets of document data to text data for lemmatisation.
     * @param keys Data keys to copy.
     */
    public void addTexts(List<String> keys){
        docTexts = new HashMap<>();
        keys.forEach(this::addText);
    }

    /**
     * Copies a document data entry to text data for lemmatisation.
     * @param key Data key to copy.
     */
    public void addText(String key){
        if(docTexts == null) docTexts = new HashMap<>();
        docTexts.put(key, docData.get(key));
    }

    /**
     * Getter methods for a text entry.
     * @param key Entry key.
     * @return The text value.
     */
    public String getText(String key){
        return docTexts.get(key);
    }

    /**
     * Setter method for the document lemmas.
     * @param inputLemmas List of lemmas to set.
     */
    public void setLemmas(List<String> inputLemmas){
        numLemmas = inputLemmas.size();
        lemmas = inputLemmas;
    }

    /**
     * Getter method for the document's list of lemmas.
     * @return The lemmas list.
     */
    public List<String> getLemmas(){
        if(lemmas != null){
            return lemmas;
        } else if(lemmaString != null){
            return Arrays.asList(lemmaString.split(" "));
        }
        return new ArrayList<>();
    }

    /**
     * Removes a single lemma from the document's lemmas list.
     * @param lemmaToRemove Lemma to remove.
     */
    public void removeLemma(String lemmaToRemove){
        lemmas.removeIf(lemmaToRemove::equals);
        numLemmas = lemmas.size();
    }

    /**
     * Removes a set of lemmas from the document's lemmas list.
     * @param lemmasToRemove List of lemmas to remove.
     */
    public void removeLemmas(List<String> lemmasToRemove){
        lemmas.removeIf(lemmasToRemove::contains);
        numLemmas = lemmas.size();
    }

    /**
     * Constructs the lemma string from the list of lemmas.
     */
    public void makeLemmaString(){
        lemmaString = "";
        lemmas.forEach(text -> lemmaString += text + " ");
        lemmaString = lemmaString.trim();
    }

    /**
     * Getter for the document's lemmas string.
     * @return The lemma string.
     */
    public String getLemmaString(){
        return lemmaString;
    }

    /**
     * Getter for number of lemmas.
     * @return The number of lemmas.
     */
    public int getNumLemmas(){
        return numLemmas;
    }

    // /**
    //  * Removes the document from modelling, giving a reason
    //  * @param reason reason for removal
    //  */
    // public void remove(String reason){
    //     removed = true;
    //     removeReason = reason;
    // }

    /**
     * Setter for tooShort flag.
     * @param b Boolean value to set.
     */
    public void setTooShort(boolean b){
        tooShort = b;
    }

    /**
     * Getter for removed flag.
     * @return The removed flag.
     */
    public boolean isRemoved(){
        return tooShort;
    }

    // /**
    //  * Getter for removed reason
    //  * @return remove reason
    //  */
    // public String getRemoveReason(){
    //     return removeReason;
    // }

    /**
     * Setter for the distribution over main topics.
     * @param distribution The topic distribution to set.
     */
    public void setMainTopicDistribution(double[] distribution){
        DecimalFormat df = new DecimalFormat("#.####");
        df.setRoundingMode(RoundingMode.UP);
        mainTopicDistribution = new double[distribution.length];
        for(int i = 0; i < distribution.length; i++){
            mainTopicDistribution[i] = Double.parseDouble(df.format(distribution[i]));
        }
    }

    /**
     * Getter for the distribution over main topics.
     * @return The main topics distribution.
     */
    public double[] getMainTopicDistribution(){
        return mainTopicDistribution;
    }

    /**
     * Setter for the distribution over sub topics.
     * @param distribution Topic distribution to set.
     */
    public void setSubTopicDistribution(double[] distribution){
        DecimalFormat df = new DecimalFormat("#.####");
        df.setRoundingMode(RoundingMode.UP);
        if(distribution != null){
            subTopicDistribution = new double[distribution.length];
            for(int i = 0; i < distribution.length; i++){
                subTopicDistribution[i] = Double.parseDouble(df.format(distribution[i]));
            }
        }

    }

    /**
     * Getter for the distribution over sub topics.
     * @return The sub topics distribution.
     */
    public double[] getSubTopicDistribution(){
        return subTopicDistribution;
    }

    /**
     * Setter for inferred flag.
     * @param b Boolean value to set.
     */
    public void setInferred(boolean b){
        inferred = b;
    }

    /**
     * Getter for inferred flag.
     * @return Boolean indicating an inferred document.
     */
    public boolean isInferred(){
        return inferred;
    }

    /**
     * Formats the document into a JSON object to write on file.
     * @return The JSON formatted document.
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
        if(tooShort){
            root.put("tooShort", true);
        }
        // Saving inferred data
        if(inferred){
            root.put("inferred", true);
        }
        // Saving Lemmas
        if(ToPrint.equals("Lemmas")){
            root.put("numLemmas", numLemmas);
            root.put("lemmas", lemmaString);
        }
        // Saving Model
        else if(ToPrint.equals("Model") && !this.isRemoved()){
            root.put("numLemmas", numLemmas);
            JSONArray topicDistrib = getDistribJSON(mainTopicDistribution);
            root.put("mainTopicDistribution", topicDistrib);
            root.put("lemmas", lemmaString);
            if(subTopicDistribution != null){
                topicDistrib = getDistribJSON(subTopicDistribution);
                root.put("subTopicDistribution", topicDistrib);
            }
        }
        return root;
    }

    /**
     * Generates a JSON Array from a topic distribution.
     * @param distrib Distribution to convert.
     * @return The JSON formatted distribution array.
     */
    private JSONArray getDistribJSON(double[] distrib){
        JSONArray res = new JSONArray();
        for(double value: distrib){
            res.add(value);
        }
        return res;
    }
}
