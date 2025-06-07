package model;

import IO.Console;
import IO.JSONHelper;
import IO.SERHelper;
import IO.Timer;
import data.Document;
import data.Model;
import model.ldacore.LDA;
import org.json.simple.parser.ParseException;
import pipeline.config.ModuleConfig;
import pipeline.config.modules.ModelConfigLDA;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Class loading a corpus and generating a number of topics from these documents
 *
 * @author T. Methven, P. Le Bras
 * @version 3
 */
public class LDAModel extends ModelModule {

    // module parameters
    private ModelConfigLDA config;

    // Flag for processing documents in parallel
    private static final boolean RUN_IN_PARALLEL = false;

    // number of documents skipped for the model, typically for having to few lemmas
    private int skipCount = 0;

    // list of documents given to the model
    private List<Document> modelInput;
    private List<Document> skippedDocs;
    // instance of lda topic model
    private LDA tModel;

    private LDAModel(ModelConfigLDA c){
        config = c;
        config.logConfig();
    }

    /**
     * Main module method - processes parameters, loads corpus, model topics and save topics and documents
     * @param moduleParameters module parameters
     * @throws Exception If the corpus cannot load properly, model fails, or saving data fails
     */
    public static void run(ModuleConfig moduleParameters) throws Exception {
        String MODULE_NAME = moduleParameters.moduleName+" ("+moduleParameters.moduleType+")";
        Console.moduleStart(MODULE_NAME);
        Timer.start(MODULE_NAME);
        LDAModel instance = new LDAModel((ModelConfigLDA) moduleParameters);
        try{
            instance.loadDocuments();
            instance.runModel();
            instance.writeModel();
//            instance.writeCorpus();
        } catch (Exception e){
            Console.moduleFail(MODULE_NAME);
            throw e;
        }
        Console.moduleComplete(MODULE_NAME);
        Timer.stop(MODULE_NAME);
    }

    private void loadDocuments() throws IOException, ParseException {
        Console.log("Loading corpus");
        modelInput = new ArrayList<>();
        skippedDocs = new ArrayList<>();
        loadCorpus(config.corpusFile);
        if(RUN_IN_PARALLEL) corpus.documents.entrySet().parallelStream().forEach(this::loadDocument);
        else corpus.documents.entrySet().forEach(this::loadDocument);
        Console.tick();
        if(skipCount > 0){
            Console.warning(skipCount+" documents skipped - not lemmatised or too few lemmas ("+config.minLemmas+")", 1);
            // reseting index of skipped documents
            int nDocs = modelInput.size();
            for(int i = 0; i<skippedDocs.size(); i++) skippedDocs.get(i).setIndex(nDocs+i);
        }
    }

    private void loadDocument(Map.Entry<String, Document> docEntry){
        Document doc = docEntry.getValue();
        if(doc.hasLemmas() && doc.getLemmas().size() > config.minLemmas){
            modelInput.add(doc);
        } else {
            skippedDocs.add(doc);
            skipCount++;
        }
    }

    private void runModel() throws LDA.LDAModelException, IOException {
        Console.log("Running LDA model");
        try {
            Console.note("Following output from Mallet\n", 1);

            // running the Mallet topic model
            tModel = new LDA(modelInput, config.ldaParameters, config.modelName);
            tModel.model(config.logDirectory);

            // Instantiating a new Model data instance
            model = new Model();
            model.name = config.modelName;
            model.corpus = corpus.name;
            model.topics = tModel.getTopics();

            Console.note("Model completed", 1);
            Console.log("LDA model");
            Console.tick();
        } catch (LDA.LDAModelException e){
            Console.error("LDA modelling failed");
            throw e;
        }

        try {
            if (config.saveLogLikelihoods) {
                JSONHelper.saveJSON(tModel.logLikelihoodLogs.toJSON(), config.loglikelihoodLogsFile, 1);
            }
            if (config.saveTopicHistory) {
                JSONHelper.saveJSON(tModel.topicLogs.toJSON(), config.topicLogsFile, 1);
            }
            if (config.serialise) {
                SERHelper.serialiseObject(tModel, config.serialisedFile, 1);
            }
        } catch (IOException e){
            Console.error("Saving LDA model logs or serialisation");
            throw e;
        }

    }

    private void writeModel() throws IOException {
        Console.log("Saving model");
        try{
            model.writeModel(config.topicsFile);
            corpus.writeCorpus(config.documentsFile);
        } catch (IOException e){
            Console.error("Saving model failed");
            throw e;
        }
    }

}
