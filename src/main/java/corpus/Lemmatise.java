package corpus;

import IO.Console;
import IO.Timer;
import pipeline.config.ModuleConfig;
import pipeline.config.ProjectConfig;
import pipeline.config.modules.LemmatiseConfig;
import corpus.lemmatizer.StanfordLemmatizer;
import data.Document;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Module loading a corpus JSON file and lemmatising its texts
 *
 * @author P. Le Bras
 * @version 1
 */
public class Lemmatise extends CleaningModule {

    // module parameters
    private String stopPhrasesFile;
    private String stopWordsFile;

    // cleaning options
    private boolean removeStopPhrases = false;
    private StopPhrases stopPhrasesModule;
    private boolean removeStopWords = false;
    private StopWords stopWordsModule;

    // for logging purposes
    long lemStartTime;
    int documentsProcessed;
    int noText;
    private final static int UPDATE_FREQUENCY = 100;
    // Stanford Lemmatizer instance
    StanfordLemmatizer slem;
    // Flag for processing documents in parallel
    private final static boolean RUN_IN_PARALLEL = true;

    /**
     * Main module method - processes parameters, loads corpus, lemmatise texts and save corpus again
     * @param moduleParameters module parameters
     * @param projectParameters project meta parameters
     * @throws Exception If the corpus cannot load properly
     */
    public static void run(ModuleConfig moduleParameters, ProjectConfig projectParameters) throws Exception {
        String MODULE_NAME = moduleParameters.moduleName+" ("+moduleParameters.moduleType+")";
        Console.moduleStart(MODULE_NAME);
        Timer.start(MODULE_NAME);
        Lemmatise instance = new Lemmatise();
        instance.processParameters((LemmatiseConfig) moduleParameters, projectParameters);
        try{
            instance.loadCorpus();
            instance.lemmatise();
            instance.writeCorpus();
        } catch (Exception e){
            Console.moduleFail(MODULE_NAME);
            throw e;
        }
        Console.moduleComplete(MODULE_NAME);
        Timer.stop(MODULE_NAME);
    }

    // processes project and module parameters
    private void processParameters(LemmatiseConfig moduleParameters, ProjectConfig projectParameters){
        Console.log("Processing parameters");
        corpusFile = projectParameters.dataDirectory+moduleParameters.corpus;
        outputFile = projectParameters.dataDirectory+moduleParameters.output;
        stopPhrasesFile = moduleParameters.stopPhrases == null ? null : projectParameters.sourceDirectory+moduleParameters.stopPhrases;
        stopWordsFile = moduleParameters.stopWords == null ? null : projectParameters.sourceDirectory+moduleParameters.stopWords;
        Console.tick();
        String saveDiff = corpusFile.equals(outputFile) ? "" : " and saving to "+outputFile;
        Console.info("Lemmatising texts from corpus "+corpusFile+saveDiff, 1);
        if(stopPhrasesFile != null) Console.info("Removing stop phrases in "+stopPhrasesFile, 2);
        if(stopWordsFile != null) Console.info("Removing words in "+stopWordsFile, 2);
    }

    // launches lemmatisation process
    private void lemmatise(){
        Console.log("Loading lemmatiser, following output from Stanford CoreNLP\n");
        slem = new StanfordLemmatizer();
        Console.log("Lemmatiser loaded");
        Console.tick();
        Console.submoduleStart("Lemmatisation");
        documentsProcessed = 0;
        noText = 0;
        lemStartTime = System.currentTimeMillis();
        // set up stop phrase removal
        if(stopPhrasesFile!=null){
            removeStopPhrases = true;
            stopPhrasesModule = new StopPhrases();
            stopPhrasesModule.loadStopPhrases(stopPhrasesFile);
        }
        // set up stop word removal
        if(stopWordsFile!=null){
            removeStopWords = true;
            stopWordsModule = new StopWords();
            stopWordsModule.loadStopWords(stopWordsFile);
        }
        // launching lemmatisation
        if(RUN_IN_PARALLEL) corpus.documents.entrySet().parallelStream().forEach(this::lemmatiseDocument);
        else corpus.documents.entrySet().forEach(this::lemmatiseDocument);
        if(noText>0){
            Console.warning(noText+" documents had no text to lemmatise");
        }
        Console.submoduleComplete("Lemmatisation");
    }

    // lemmatises one document
    private void lemmatiseDocument(Map.Entry<String, Document> docEntry){
        if(documentsProcessed % UPDATE_FREQUENCY == 0 && documentsProcessed != 0) {
            long lemTimeTaken = (System.currentTimeMillis() - lemStartTime) / (long)1000;
            float lemTimeLeft = ((float) lemTimeTaken / (float) documentsProcessed) * (corpus.size() - documentsProcessed);
            float percentage = Math.round((((float) documentsProcessed / (float) corpus.size()) * 100) * 100f) / 100f;
            Console.info("Lemmatised: "+documentsProcessed+" documents (" +percentage+ "%) - "+Timer.convert(lemTimeTaken)+" / "+Timer.convert(lemTimeLeft)+" (est.)", 1);
        }

        Document doc = docEntry.getValue();
        if(doc.emptyText()){
            noText++;
            Console.warning("Document "+doc.getId()+" has no text - skipping lemmatisation");
        } else {
            String text = doc.getText().trim().toLowerCase();
            // removing stop phrases
            if(removeStopPhrases){
                text = stopPhrasesModule.removeStopPhrases(text);
            }
            // removing special characters
            text = text.replaceAll("\\n", " "); // returns
            text = text.replaceAll("\\r", " "); // carriage returns
            text = text.trim().replaceAll(" +"," "); // Trim all white space to single space
            // lemmatising
            List<String> lemmas = StanfordLemmatizer.removeCommonStopWords(slem.lemmatise(text));
            // remove stop words
            if(removeStopWords){
                stopWordsModule.removeStopWords(lemmas);
            }
            doc.setLemmas(lemmas);
        }
        documentsProcessed++;
    }
}
