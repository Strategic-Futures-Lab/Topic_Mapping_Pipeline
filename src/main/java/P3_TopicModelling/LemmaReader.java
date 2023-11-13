package P3_TopicModelling;

import PX_Data.DocIOWrapper;
import PX_Data.JSONIOWrapper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class reading a lemmas JSON file and providing {@link TopicModelling} instances access to it.
 *
 * @author: P. Le Bras
 * @version 1
 */
public class LemmaReader {

    /** Corpus metadata read from the lemmas JSON file. */
    private JSONObject metadata;
    /** List of lemmatised documents read from the lemmas JSON file. */
    private ConcurrentHashMap<String, DocIOWrapper> Documents;

    /**
     * Constructor.
     * @param lemmaFile Filename for the lemmas JSON file.
     */
    public LemmaReader(String lemmaFile){
        JSONObject input = JSONIOWrapper.LoadJSON(lemmaFile, 0);
        metadata = (JSONObject) input.get("metadata");
        JSONArray lemmas = (JSONArray) input.get("lemmas");
        Documents = new ConcurrentHashMap<>();
        for(JSONObject docEntry: (Iterable<JSONObject>) lemmas){
            DocIOWrapper doc = new DocIOWrapper(docEntry);
            Documents.put(doc.getId(), doc);
        }
    }

    /**
     * Method returning a <strong>copy</strong> of the list of lemmatised documents.
     * @return Copy of the lemmatised documents list.
     */
    public ConcurrentHashMap<String, DocIOWrapper> getDocuments(){
        ConcurrentHashMap<String, DocIOWrapper> copy = new ConcurrentHashMap<>();
        for(Map.Entry<String, DocIOWrapper> entry: Documents.entrySet()){
            String key = entry.getKey();
            DocIOWrapper doc = entry.getValue();
            copy.put(key, new DocIOWrapper(doc));
        }
        return copy;
    }

    /**
     * Method returning a <strong>copy</strong> of the lemmatised corpus metadata.
     * @return Copy of the lemmatised corpus metadata.
     */
    public JSONObject getMetadata(){
        return (JSONObject) metadata.clone();
    }
}
