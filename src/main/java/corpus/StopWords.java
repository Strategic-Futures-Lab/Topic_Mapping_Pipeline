package corpus;

import IO.Console;
import IO.Timer;
import config.ModuleConfig;
import config.ProjectConfig;
import config.modules.StopWordsConfig;
import data.Document;

import java.util.List;
import java.util.Map;

/**
 * Module loading a corpus JSON file and removing stop words from lemmas
 *
 * @author P. Le Bras
 * @version 1
 */
public class StopWords extends CleaningModule {

    // module parameters
    private String stopWordsFile;

    private List<String> stopWords;

    // for logging purposes
    int noLemmas;
    // Flag for processing documents in parallel
    private final static boolean RUN_IN_PARALLEL = true;

    /**
     * Main module method - processes parameters, loads corpus, removes stop words and save corpus again
     * @param moduleParameters module parameters
     * @param projectParameters project meta parameters
     * @throws Exception If the corpus cannot load properly
     */
    public static void run(ModuleConfig moduleParameters, ProjectConfig projectParameters) throws Exception {
        String MODULE_NAME = moduleParameters.moduleName+" ("+moduleParameters.moduleType+")";
        Console.moduleStart(MODULE_NAME);
        Timer.start(MODULE_NAME);
        StopWords instance = new StopWords();
        instance.processParameters((StopWordsConfig) moduleParameters, projectParameters);
        try{
            instance.loadCorpus();
            instance.loadStopWords(instance.stopWordsFile);
            instance.removeStopWords();
            instance.writeCorpus();
        } catch (Exception e){
            Console.moduleFail(MODULE_NAME);
            throw e;
        }
        Console.moduleComplete(MODULE_NAME);
        Timer.stop(MODULE_NAME);
    }

    // processes project and module parameters
    private void processParameters(StopWordsConfig moduleParameters, ProjectConfig projectParameters){
        Console.log("Processing parameters");
        corpus = projectParameters.dataDirectory+moduleParameters.corpus;
        output = projectParameters.dataDirectory+moduleParameters.output;
        stopWordsFile = projectParameters.sourceDirectory+moduleParameters.stopWords;
        Console.tick();
        String saveDiff = corpus.equals(output) ? "" : " and saving to "+output;
        Console.info("Removing stop words ("+stopWordsFile+") from corpus "+corpus+saveDiff, 1);
    }

    /**
     * Loads the list of stop words from a text file
     * @param filename text file with stop words
     */
    public void loadStopWords(String filename){
        stopWords = readTextFile(filename, "stop word(s)");
        stopWords = stopWords.stream().map(s -> s.trim().toLowerCase()).toList();
    }

    // launches the stop word removal process
    private void removeStopWords(){
        Console.log("Removing stop words");
        noLemmas = 0;
        if(RUN_IN_PARALLEL) documents.entrySet().parallelStream().forEach(this::removeStopWords);
        else documents.entrySet().forEach(this::removeStopWords);
        if(noLemmas>0) Console.warning(noLemmas+" documents had no lemmatised text to remove stop words from");
        else Console.tick();
    }

    // removes stop words from one document
    private void removeStopWords(Map.Entry<String, Document> docEntry){
        Document doc = docEntry.getValue();
        if(doc.hasLemmas()) doc.removeLemmas(stopWords);
        else noLemmas++;
    }

    /**
     * Removes the stop words from the given list of lemmas
     * @param lemmas List of the lemmas to clean
     */
    public void removeStopWords(List<String> lemmas){
        lemmas.removeAll(stopWords);
    }
}
