package input;

import IO.Console;
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
 *
 * @author P. Le Bras
 * @version 1
 */
public abstract class InputModule {

    // every input module fills a list with document
    protected final ConcurrentHashMap<String, Document> documents = new ConcurrentHashMap<>();

    // every input module has a source (file or directory name) and output file name
    protected String source;
    protected String outputFile;

    // every input module write the corpus on a JSON file
    protected void writeJSON() throws IOException {
        try {
            JSONObject root = new JSONObject();
            JSONArray corpus = new JSONArray();
            JSONObject meta = new JSONObject();
            meta.put("nDocs", documents.size());
            root.put("metadata", meta);
            for (Map.Entry<String, Document> doc : documents.entrySet()) {
                corpus.add(doc.getValue().toJSON());
            }
            root.put("corpus", corpus);
            JSONHelper.saveJSON(root, outputFile);
        } catch (IOException e){
            Console.error("Saving corpus file "+outputFile+" failed");
            throw e;
        }
    }
}
