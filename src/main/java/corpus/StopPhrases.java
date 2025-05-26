package corpus;

import IO.Console;
import IO.Timer;
import pipeline.config.ModuleConfig;
import pipeline.config.ProjectConfig;
import pipeline.config.modules.StopPhrasesConfig;
import data.Document;
import pipeline.config.modules.StopWordsConfig;

import java.util.List;
import java.util.Map;

/**
 * Module loading a corpus JSON file and removing stop phrases from texts
 *
 * @author P. Le Bras
 * @version 1
 */
public class StopPhrases extends CleaningModule {

    // module parameters
    private StopPhrasesConfig config;

    private List<String> stopPhrases;

    // for logging purposes
    int noText;
    // Flag for processing documents in parallel
    private final static boolean RUN_IN_PARALLEL = true;

    private StopPhrases(StopPhrasesConfig c){
        config = c;
        config.logConfig();
    }

    /**
     * Constructor for usage outside the module (e.g., if embedded in lemmatisation)
     */
    public StopPhrases(){}

    /**
     * Main module method - processes parameters, loads corpus, removes stop words and save corpus again
     * @param moduleParameters module parameters
     * @throws Exception If the corpus cannot load properly
     */
    public static void run(ModuleConfig moduleParameters) throws Exception {
        String MODULE_NAME = moduleParameters.moduleName+" ("+moduleParameters.moduleType+")";
        Console.moduleStart(MODULE_NAME);
        Timer.start(MODULE_NAME);
        StopPhrases instance = new StopPhrases((StopPhrasesConfig) moduleParameters);
        try{
            instance.loadCorpus(instance.config.corpusFile);
            instance.loadStopPhrases(instance.config.stopPhrasesFile);
            instance.removeStopPhrases();
            instance.writeCorpus(instance.config.outputFile);
        } catch (Exception e){
            Console.moduleFail(MODULE_NAME);
            throw e;
        }
        Console.moduleComplete(MODULE_NAME);
        Timer.stop(MODULE_NAME);
    }

    /**
     * Loads the list of stop phrases from a text file
     * @param filename text file with stop phrases
     */
    public void loadStopPhrases(String filename){
        stopPhrases = readTextFile(filename, "stop phrase(s)");
        stopPhrases = stopPhrases.stream().map(s -> s.trim().toLowerCase()).toList();
    }

    // launches the stop phrase removal process
    private void removeStopPhrases(){
        Console.log("Removing stop phrases");
        noText = 0;
        if(RUN_IN_PARALLEL) corpus.documents.entrySet().parallelStream().forEach(this::removeStopPhrases);
        else corpus.documents.entrySet().forEach(this::removeStopPhrases);
        if(noText>0) Console.warning(noText+" documents had no lemmatised text to remove stop phrases from");
        else Console.tick();
    }

    // removes stop phrases from one document
    private void removeStopPhrases(Map.Entry<String, Document> docEntry){
        Document doc = docEntry.getValue();
        if(!doc.emptyText()) doc.setText(removeStopPhrases(doc.getText()));
        else noText++;
    }

    /**
     * Removes the stop phrases from the given text
     * @param rawText Text to clean
     * @return Cleaned text
     */
    public String removeStopPhrases(String rawText){
        String text = rawText;
        for(String phrase: stopPhrases){
            text = text.replaceAll(phrase, " ");
        }
        return text;
    }
}
