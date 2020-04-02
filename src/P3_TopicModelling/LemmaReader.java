package P3_TopicModelling;

import PX_Helper.JSONIOWrapper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LemmaReader {
    private JSONObject metadata;
    private ConcurrentHashMap<String, ModelJSONDocument> Documents;

    public LemmaReader(String lemmaFile){
        System.out.println("Loading corpus ...");

        JSONObject input = JSONIOWrapper.LoadJSON(lemmaFile);
        metadata = (JSONObject) input.get("metadata");
        JSONArray lemmas = (JSONArray) input.get("lemmas");
        Documents = new ConcurrentHashMap<>();
        for(JSONObject docEntry: (Iterable<JSONObject>) lemmas){
            ModelJSONDocument doc = new ModelJSONDocument(docEntry);
            Documents.put(doc.getId(), doc);
        }

        System.out.println("Loaded corpus!");
    }

    public ConcurrentHashMap<String, ModelJSONDocument> getDocuments(){
        ConcurrentHashMap<String, ModelJSONDocument> copy = new ConcurrentHashMap<>();
        for(Map.Entry<String, ModelJSONDocument> entry: Documents.entrySet()){
            String key = entry.getKey();
            ModelJSONDocument doc = entry.getValue();
            copy.put(key, new ModelJSONDocument(doc));
        }
        return copy;
    }

    public JSONObject getMetadata(){
        return (JSONObject) metadata.clone();
    }
}
