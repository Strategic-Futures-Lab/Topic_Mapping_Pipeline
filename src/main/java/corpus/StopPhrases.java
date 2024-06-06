package corpus;

import IO.Console;
import IO.Timer;
import config.ModuleConfig;
import config.ProjectConfig;
import config.modules.StopPhrasesConfig;
import data.Document;

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
    private String stopPhrasesFile;

    private List<String> stopPhrases;

    // for logging purposes
    int noText;
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
        StopPhrases instance = new StopPhrases();
        instance.processParameters((StopPhrasesConfig) moduleParameters, projectParameters);
        try{
            instance.loadCorpus();
            instance.loadStopPhrases(instance.stopPhrasesFile);
            instance.removeStopPhrases();
            instance.writeCorpus();
        } catch (Exception e){
            Console.moduleFail(MODULE_NAME);
            throw e;
        }
        Console.moduleComplete(MODULE_NAME);
        Timer.stop(MODULE_NAME);
    }

    // processes project and module parameters
    private void processParameters(StopPhrasesConfig moduleParameters, ProjectConfig projectParameters){
        Console.log("Processing parameters");
        corpus = projectParameters.dataDirectory+moduleParameters.corpus;
        output = projectParameters.dataDirectory+moduleParameters.output;
        stopPhrasesFile = projectParameters.sourceDirectory+moduleParameters.stopPhrases;
        Console.tick();
        String saveDiff = corpus.equals(output) ? "" : " and saving to "+output;
        Console.info("Removing stop phrases ("+stopPhrasesFile+") from corpus "+corpus+saveDiff, 1);
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
        Console.log("Removing stop words");
        noText = 0;
        if(RUN_IN_PARALLEL) documents.entrySet().parallelStream().forEach(this::removeStopPhrases);
        else documents.entrySet().forEach(this::removeStopPhrases);
        if(noText>0) Console.warning(noText+" documents had no lemmatised text to remove stop words from");
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
