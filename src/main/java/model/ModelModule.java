package model;

import data.Corpus;
import org.json.simple.parser.ParseException;

import java.io.IOException;

/**
 * Super class for topic modelling modules, containing typical properties and methods
 *
 * @author P. Le Bras
 * @version 1
 */
public class ModelModule {

    // Model modules will use a corpus
    protected Corpus corpus;

    // typical model module parameters
//    protected String corpusFile;
//    protected String topicsFile;
//    protected String documentsFile;
//    protected String logDirectory;

    // method for reading a corpus JSON file and generating a list of documents using default properties
    protected void loadCorpus(String file) throws IOException, ParseException {
        corpus = new Corpus(file);
    }
}
