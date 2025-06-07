package analysis;

import data.Corpus;
import data.Model;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.util.List;

/**
 * Super class for model analysis modules, containing typical properties and methods
 *
 * @author P. Le Bras
 * @version 1
 */
public class AnalysisModule {

    // list of topics
    protected Model model;
    // list of (modelled) documents
    protected Corpus corpus;

    // filename for list of topics
    protected String topicsFile;
    // filename for corpus
    protected String documentsFile;

    protected void loadModel() throws IOException, ParseException {
        model = new Model(topicsFile);
    }

    protected void loadCorpus() throws IOException, ParseException {
        corpus = new Corpus(documentsFile);
    }
}
