package data;

import IO.JSONHelper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.*;

/**
 * Class representing a document.
 * Provides functionalities to create a new document, read it from JSON formatted objects, set and access attributes
 * and transform it into a JSON format to write on file.
 *
 * @author P. Le Bras
 * @version 2
 */
public class Document {

    private String id;
    private int idx;
    private final HashMap<String, String> fields;
    // created by text builder module
    private String text;
    // created by lemmatise module
    private List<String> lemmasList;

    /**
     * Initial constructor, used by input modules
     * @param id Document id
     * @param idx Document index
     */
    public Document(String id, int idx){
        this.id = id;
        this.idx = idx;
        this.fields = new HashMap<>();
    }

    /**
     * JSON constructor, used when loading from JSON file
     * @param doc
     */
    public Document(JSONObject doc){
        id = (String) doc.get("id");
        idx = Math.toIntExact((long) doc.get("index"));
        fields = JSONHelper.getStringMap((JSONObject) doc.get("data"));
        // set by text builder module
        text = (String) doc.get("text");
        // set by lemmatise module
        parseLemmas((String) doc.get("lemmas"));
    }

    /**
     * Copy constructor, used to have multiple copies of documents across models;
     * Might deprecate
     * @param doc Document to copy
     */
    public Document(Document doc){
        id = doc.id;
        idx = doc.idx;
        fields = doc.fields;
        // set by text builder module
        text = doc.text;
        // set by lemmatise module
        lemmasList = doc.lemmasList;
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
    public int getIndex(){ return idx; }

    /**
     * Setter method for the document index
     * **WARNING**: USE WITH CAUTION, IDEALLY ONLY BEFORE SAVING ON FILE
     * @param index The new index
     */
    public void setIndex(int index){ idx = index; }

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
            text += (text.isEmpty() ? "" : " ---- ") + fields.get(key);
        }
    }

    /**
     * Getter for the document text String
     * @return The text String
     */
    public String getText(){ return text; }

    /**
     * Checks if the document's text is missing or empty
     * @return Boolean for text empty/missing
     */
    public boolean emptyText(){ return text == null || text.isEmpty(); }

    // Parses a string of lemmas (separated by space) and saves into the list of lemmas
    private void parseLemmas(String lemmas){
        if(lemmas != null && !lemmas.isEmpty()){
            lemmasList = List.of(lemmas.split(" "));
        }
    }

    /**
     * Returns the list of lemmas as one String, concatenated with a space;
     * Returns and empty String if not lemmas are set
     * @return The lemma String
     */
    public String getLemmasString(){
        String lemmas = "";
        if(lemmasList!=null){
            lemmas = String.join(" ", lemmasList);
        }
        return lemmas;
    }

    /**
     * Returns the number of lemmas
     * @return The number of lemmas
     */
    public int getNumLemmas(){ return lemmasList.size(); }

    /**
     * Setter for the list of lemmas
     * @param lemmas List of lemmas to set
     */
    public void setLemmas(List<String> lemmas){
        lemmasList = lemmas;
    }

    /**
     * Getter for the list of lemmas
     * @return The lemmas list
     */
    public List<String> getLemmas(){
        if(lemmasList != null) return lemmasList;
        return new ArrayList<>();
    }

    /**
     * Removes a single lemma from the lemmas list
     * @param lemma Lemma to remove
     */
    public void removeLemma(String lemma){
        if(lemmasList!=null){
            lemmasList.removeIf(lemma::equals);
        }
    }

    /**
     * Removes a set of lemmas from the lemmas list
     * @param lemmas List of lemmas to remove
     */
    public void removeLemmas(List<String> lemmas){
        if(lemmasList!=null){
            lemmasList.removeIf(lemmas::contains);
        }
    }

    /**
     * Formats the document into a JSON object to write on file
     * @return The JSON formatted document
     */
    public JSONObject toJSON(){
        JSONObject root = new JSONObject();
        // Saving id and index
        root.put("id", id);
        root.put("index", idx);
        // Saving fields
        JSONObject data = new JSONObject();
        for(Map.Entry<String, String> entry: fields.entrySet()){
            data.put(entry.getKey(), entry.getValue());
        }
        root.put("data", data);
        // Saving text
        if(text!=null && !text.isEmpty()){
            root.put("text", text);
        }
        // Saving Lemmas
        if(lemmasList!=null && !lemmasList.isEmpty()){
            root.put("lemmas", getLemmasString());
        }
        return root;
    }

}
