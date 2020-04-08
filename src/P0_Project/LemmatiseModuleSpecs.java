package P0_Project;

import PX_Data.JSONIOWrapper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Class for Lemmatise module project specification
 */
public class LemmatiseModuleSpecs {

    /** Filename to corpus data (from Input module) */
    public String corpus;
    /** List of fields in docData to use for text */
    public String[] textFields;
    /** List of fields in docData to keep after process is done */
    public String[] docFields;
    /** List of stopwords to remove from lemmas, optional, defaults to empty */
    public String[] stopWords;
    /** Minimum number of lemmas a document must have to be kept for topic modelling, optional, defaults to 1 */
    public int minLemmas;
    /** Threshold count of lemmas, if under threshold lemma will be removed from model, optional, defaults to 0 */
    public int removeLowCounts;
    /** Filename for the JSON lemma file generated */
    public String output;

    /**
     * Constructor: reads the specification from the "lemmatise" entry in the project file
     * @param specs JSON object attached to "lemmatise"
     */
    public LemmatiseModuleSpecs(JSONObject specs){
        corpus = (String) specs.get("corpus");
        textFields = JSONIOWrapper.getStringArray((JSONArray) specs.get("textFields"));
        docFields = JSONIOWrapper.getStringArray((JSONArray) specs.get("docFields"));
        stopWords = JSONIOWrapper.getStringArray((JSONArray) specs.getOrDefault("stopWords", new JSONArray()));
        minLemmas = Math.toIntExact((long) specs.getOrDefault("minLemmas", (long) 1));
        removeLowCounts = Math.toIntExact((long) specs.getOrDefault("removeLowCounts", (long) 0));
        output = (String) specs.get("output");
    }
}
