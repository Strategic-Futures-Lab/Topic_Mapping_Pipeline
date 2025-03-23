package input;

import data.Corpus;

import java.io.IOException;

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
        corpus.writeCorpus(outputFile);
    }
}
