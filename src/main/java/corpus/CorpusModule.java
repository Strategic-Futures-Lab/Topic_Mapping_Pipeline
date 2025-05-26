package corpus;

import data.Corpus;
import data.Document;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Super class for corpus management modules, containing typical properties and methods
 *
 * @author P. Le Bras
 * @version 1
 */
public abstract class CorpusModule {

    // corpus modules typically handle a list of documents and keep track of the corpus metadata
    protected Corpus corpus;

    // method for reading a corpus JSON file and generating a list of documents using default properties
    protected void loadCorpus(String file) throws IOException, ParseException {
        corpus = new Corpus(file);
    }

    // Method for filtering document data
    protected void filterDocumentFields(Document doc, String[] fields){
        List<String> fieldsFilter = fields == null ? doc.getFieldsKey().stream().toList() : Arrays.stream(fields).toList();
        doc.filterFields(fieldsFilter);
    }

    // method for writing the (transformed) corpus JSON file using default properties
    protected void writeCorpus(String file) throws IOException {
        corpus.writeCorpus(file);
    }

}
