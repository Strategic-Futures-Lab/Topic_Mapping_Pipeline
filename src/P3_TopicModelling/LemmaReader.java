package P3_TopicModelling;

import PX_Helper.JSONIOWrapper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.concurrent.ConcurrentHashMap;

public class LemmaReader {
    public static JSONObject metadata;
    public static ConcurrentHashMap<String, ModelJSONDocument> Documents;

    public static void ReadLemma(String lemmaFile){
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
}
