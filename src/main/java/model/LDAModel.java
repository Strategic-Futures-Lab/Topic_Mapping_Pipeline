package model;

import IO.Console;
import IO.JSONHelper;
import IO.SERHelper;
import IO.Timer;
import data.Topic;
import data.Document;
import pipeline.config.ModuleConfig;
import pipeline.config.ProjectConfig;
import pipeline.config.modules.ModelConfigLDA;
import model.ldacore.LDA;
import model.ldacore.LDAParameters;
import org.json.simple.parser.ParseException;

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

    // Flag for processing documents in parallel
    private static final boolean RUN_IN_PARALLEL = false;

    // number of documents skipped for the model, typically for having to few lemmas
    private int skipCount = 0;

    // list of documents given to the model
    private List<Document> modelInput;
    private List<Document> skippedDocs;
    // instance of lda topic model
    private LDA tModel;

    // model settings
    private LDAParameters ldaParameters;

    private int minLemmas;
    private boolean wordDistances;

    // log settings
    private String logDir;
    private String serialisedFile;
    private String loglikelihoodLogFile;
    private String topicLogFile;

    public static void run(ModuleConfig moduleParameters, ProjectConfig projectParameters) throws Exception {
        String MODULE_NAME = moduleParameters.moduleName+" ("+moduleParameters.moduleType+")";
        Console.moduleStart(MODULE_NAME);
        Timer.start(MODULE_NAME);
        LDAModel instance = new LDAModel();
        instance.processParameters((ModelConfigLDA) moduleParameters, projectParameters);
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

    private void processParameters(ModelConfigLDA moduleParameters, ProjectConfig projectParameters){
        Console.log("Processing parameters");
        corpusFile = projectParameters.dataDirectory+moduleParameters.corpus;
        topicsFile = projectParameters.dataDirectory+moduleParameters.topics;
        documentsFile = projectParameters.dataDirectory+moduleParameters.documents;
        ldaParameters = moduleParameters.ldaParams;
        minLemmas = moduleParameters.minLemmas;
        wordDistances = moduleParameters.wordDistances;
        logDir = projectParameters.dataDirectory+moduleParameters.logDir;
        // keep the following as is for now
        // module will check for null if they need to be skipped
        serialisedFile = moduleParameters.serialised;
        loglikelihoodLogFile = moduleParameters.loglikelihoodLogs;
        topicLogFile = moduleParameters.topicLogs;
        Console.tick();
        Console.info("Modelling "+ldaParameters.nTopics+" topics from corpus "+corpusFile, 1);
        Console.info("Saving topics in "+topicsFile, 1);
        Console.info("Saving documents in "+documentsFile, 1);
        if(serialisedFile != null) Console.info("Serialising model to "+logDir+serialisedFile, 2);
        if(loglikelihoodLogFile != null) Console.info("Saving log-likelihoods to "+logDir+loglikelihoodLogFile, 2);
        if(topicLogFile != null) Console.info("Saving topic logs to "+logDir+topicLogFile, 2);
    }

    private void loadDocuments() throws IOException, ParseException {
        Console.log("Loading corpus");
        modelInput = new ArrayList<>();
        skippedDocs = new ArrayList<>();
        loadCorpus();
        if(RUN_IN_PARALLEL) corpus.documents.entrySet().parallelStream().forEach(this::loadDocument);
        else corpus.documents.entrySet().forEach(this::loadDocument);
        Console.tick();
        if(skipCount > 0){
            Console.warning(skipCount+" documents skipped - not lemmatised or too few lemmas ("+minLemmas+")", 1);
            // resting index of skipped documents
            int nDocs = modelInput.size();
            for(int i = 0; i<skippedDocs.size(); i++) skippedDocs.get(i).setIndex(nDocs+i);
        }
    }

    private void loadDocument(Map.Entry<String, Document> docEntry){
        Document doc = docEntry.getValue();
        if(doc.hasLemmas() && doc.getLemmas().size() > minLemmas){
//            LDADocument inputDoc = new LDADocument(doc.getId(), doc.getLemmasString());
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

            tModel = new LDA(modelInput, ldaParameters);
            tModel.getWordDistances = wordDistances;
            tModel.model(logDir);

            Console.note("Model completed", 1);
            Console.log("LDA model");
            Console.tick();
        } catch (LDA.LDAModelException e){
            Console.error("LDA modelling failed");
            throw e;
        }

        try {
            if (loglikelihoodLogFile != null) {
                JSONHelper.saveJSON(tModel.logLikelihoodLogs.toJSON(), logDir+loglikelihoodLogFile, 1);
            }
            if (topicLogFile != null) {
                JSONHelper.saveJSON(tModel.topicLogs.toJSON(), logDir+topicLogFile, 1);
            }
            if (serialisedFile != null) {
                SERHelper.serialiseObject(tModel, logDir+serialisedFile, 1);
            }
        } catch (IOException e){
            Console.error("Saving LDA model logs or serialisation");
            throw e;
        }

    }

    private void writeModel() throws IOException {
        Console.log("Saving model");
        try{
            Topic.writeTopics(topicsFile, tModel.getTopics());
            corpus.writeCorpus(documentsFile);
//            JSONArray documents = new JSONArray();
//            for(Document d: modelInput){
//                documents.add(d.toJSON());
//            }
////            root.put("topics", topics);
////            root.put("documents",documents);
//            JSONHelper.saveJSONArray(documents, documentsFile, 1);
        } catch (IOException e){
            Console.error("Saving model failed");
            throw e;
        }
    }

}
