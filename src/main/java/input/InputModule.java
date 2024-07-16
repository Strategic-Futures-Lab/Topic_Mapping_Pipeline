package input;

import IO.Console;
import IO.JSONHelper;
import data.Corpus;
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
    protected final Corpus corpus = new Corpus();

    // every input module has a source (file or directory name) and output file name
    protected String source;
    protected String outputFile;

    // every input module write the corpus on a JSON file
    protected void writeCorpus() throws IOException {
        corpus.metadata.put("nDocs", corpus.size());
        corpus.writeCorpus(outputFile);
    }
}
