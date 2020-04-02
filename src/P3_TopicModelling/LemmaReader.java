package P3_TopicModelling;

import PX_Data.JSONDocument;
import PX_Data.JSONIOWrapper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LemmaReader {
    private JSONObject metadata;
    private ConcurrentHashMap<String, JSONDocument> Documents;

    public LemmaReader(String lemmaFile){
        System.out.println("Loading corpus ...");

        JSONObject input = JSONIOWrapper.LoadJSON(lemmaFile);
        metadata = (JSONObject) input.get("metadata");
        JSONArray lemmas = (JSONArray) input.get("lemmas");
        Documents = new ConcurrentHashMap<>();
        for(JSONObject docEntry: (Iterable<JSONObject>) lemmas){
            JSONDocument doc = new JSONDocument(docEntry);
            Documents.put(doc.getId(), doc);
        }

        System.out.println("Loaded corpus!");
    }

    public ConcurrentHashMap<String, JSONDocument> getDocuments(){
        ConcurrentHashMap<String, JSONDocument> copy = new ConcurrentHashMap<>();
        for(Map.Entry<String, JSONDocument> entry: Documents.entrySet()){
            String key = entry.getKey();
            JSONDocument doc = entry.getValue();
            copy.put(key, new JSONDocument(doc));
        }
        return copy;
    }

    public JSONObject getMetadata(){
        return (JSONObject) metadata.clone();
    }
}
