package input;

import IO.JSONHelper;
import data.Document;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Superclass for input modules;
 * Contains protected properties/methods for each input module to use
 */
public abstract class InputModule {

    // every input module will fill a list of document
    protected final ConcurrentHashMap<String, Document> documents = new ConcurrentHashMap<>();

    // every input module write the corpus on a JSON file
    protected void writeJSON(String filename) throws IOException {
        JSONObject root = new JSONObject();
        JSONArray corpus = new JSONArray();
        JSONObject meta = new JSONObject();
        meta.put("nDocs", documents.size());
        root.put("metadata", meta);
        for(Map.Entry<String, Document> doc: documents.entrySet()){
            corpus.add(doc.getValue().toJSON());
        }
        root.put("corpus", corpus);
        JSONHelper.saveJSON(root, filename);
    }

}
