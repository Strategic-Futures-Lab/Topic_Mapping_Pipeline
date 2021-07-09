package P0_Project;

import PX_Data.JSONIOWrapper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Class reading an validating parameters for the Lemmatise module ({@link P2_Lemmatise}).
 *
 * @author P. Le Bras
 * @version 1
 */
public class LemmatiseModuleSpecs {

    /** Filename of corpus JSON file (from Input module). */
    public String corpus;
    /** List of fields in docData to use for text. */
    public String[] textFields;
    /** List of fields in docData to keep after process is done,
     * optional, defaults to [].  */
    public String[] docFields;
    /** List of stop-phrases to exclude from text before lemmatisation,
     * optional, defaults to []. */
    public String[] stopPhrases;
    /** List of stop-words to remove from lemmas,
     * optional, defaults to []. */
    public String[] stopWords;
    /** Minimum number of lemmas a document must have to be kept for topic modelling,
     * optional, defaults to 1. */
    public int minDocLemmas;
    /** Threshold count of lemmas, if under threshold lemma will be removed from model,
     * optional, defaults to 0. */
    public int minLemmaCount;
    /** Filename for the JSON lemma file generated. */
    public String output;

    /**
     * Constructor: parses and validates the given JSON object to set parameters.
     * @param specs JSON object attached to "lemmatise" in project file.
     * @param metaSpecs Meta-parameter specifications.
     */
    public LemmatiseModuleSpecs(JSONObject specs, MetaSpecs metaSpecs){
        corpus = metaSpecs.getDataDir() + specs.get("corpus");
        textFields = JSONIOWrapper.getStringArray((JSONArray) specs.get("textFields"));
        docFields = metaSpecs.useMetaDocFields() ?
                metaSpecs.docFields :
                JSONIOWrapper.getStringArray((JSONArray) specs.getOrDefault("docFields", new JSONArray()));
        stopPhrases = JSONIOWrapper.getStringArray((JSONArray) specs.getOrDefault("stopPhrases", new JSONArray()));
        stopWords = JSONIOWrapper.getStringArray((JSONArray) specs.getOrDefault("stopWords", new JSONArray()));
        minDocLemmas = Math.toIntExact((long) specs.getOrDefault("minDocLemmas", specs.getOrDefault("minLemmas", (long) 1)));
        minLemmaCount = Math.toIntExact((long) specs.getOrDefault("minLemmaCount", (long) 0));
        output = metaSpecs.getDataDir() + specs.get("output");
    }
}
