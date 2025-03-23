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

}
