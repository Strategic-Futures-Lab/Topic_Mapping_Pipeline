package corpus;

import IO.Console;
import IO.JSONHelper;
import data.Corpus;
import data.Document;
import data.Pair;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Super class for corpus management modules, containing typical properties and methods
 *
 * @author P. Le Bras
 * @version 1
 */
public abstract class CorpusModule {

    // corpus modules typically handle a list of documents and keep track of the corpus metadata
    protected Corpus corpus;

    // typical corpus module parameters
    protected String corpusFile;
    protected String outputFile;
    protected String[] docFields;

    // method for reading a corpus JSON file and generating a list of documents using default properties
    protected void loadCorpus() throws IOException, ParseException {
        corpus = new Corpus(corpusFile);
    }

    // Method for filtering document data
    protected void filterDocumentFields(Document doc){
        List<String> fieldsFilter = docFields == null ? doc.getFieldsKey().stream().toList() : Arrays.stream(docFields).toList();
        doc.filterFields(fieldsFilter);
    }

    // method for writing the (transformed) corpus JSON file using default properties
    protected void writeCorpus() throws IOException {
        corpus.writeCorpus(outputFile);
    }

    // method for writing the (transformed) corpus JSON file
    protected void writeCorpus(JSONObject meta, ConcurrentHashMap<String, Document> documentList, String filename) throws IOException {
        try {
            JSONObject root = new JSONObject();
            JSONArray corpus = new JSONArray();
            root.put("metadata", meta);
            for (Map.Entry<String, Document> doc : documentList.entrySet()) {
                corpus.add(doc.getValue().toJSON());
            }
            root.put("corpus", corpus);
            JSONHelper.saveJSON(root, filename);
        } catch (IOException e){
            Console.error("Saving corpus file "+filename+" failed");
            throw e;
        }
    }

}
