package corpus;

import IO.Console;
import IO.Timer;
import data.Document;
import pipeline.config.ModuleConfig;
import pipeline.config.modules.BuildTextConfig;

import java.util.Map;

/**
 * Module loading a corpus JSON file and building a text string from given document fields
 *
 * @author P. Le Bras
 * @version 1
 */
public class BuildText extends CorpusModule {

    // module parameters
    private BuildTextConfig config;

    // for logging purposes
    private int missingTextField = 0;
    private int emptyText = 0;

    // Flag for processing documents in parallel
    private final static boolean RUN_IN_PARALLEL = true;

    private BuildText(BuildTextConfig c){
        config = c;
        config.logConfig();
    }

    /**
     * Main module method - processes parameters, loads corpus, builds texts and save corpus again
     * @param moduleConfig module parameters
     * @throws Exception If the corpus cannot load properly
     */
    public static void run(ModuleConfig moduleConfig) throws Exception {
        String MODULE_NAME = moduleConfig.moduleName+" ("+moduleConfig.moduleType+")";
        Console.moduleStart(MODULE_NAME);
        Timer.start(MODULE_NAME);
        BuildText instance = new BuildText((BuildTextConfig) moduleConfig);
        try{
            instance.loadCorpus(instance.config.corpusFile);
            instance.buildTexts();
            instance.writeCorpus(instance.config.outputFile);
        } catch (Exception e){
            Console.moduleFail(MODULE_NAME);
            throw e;
        }
        Console.moduleComplete(MODULE_NAME);
        Timer.stop(MODULE_NAME);
    }

    // launches the build text process on all documents
    private void buildTexts(){
        Console.log("Building texts");
        if(RUN_IN_PARALLEL) corpus.documents.entrySet().parallelStream().forEach(this::buildText);
        else corpus.documents.entrySet().forEach(this::buildText);
        Console.tick();
        if(missingTextField > 0) Console.warning(missingTextField+" documents were missing one or more text fields", 1);
        if(emptyText > 0) Console.warning(emptyText+" documents have an empty text", 1);
    }

    // builds the text for the given document
    private void buildText(Map.Entry<String, Document> docEntry){
        Document doc = docEntry.getValue();
        for(String textField: config.textFields){
            if(!doc.hasField(textField)){
                missingTextField++;
                break;
            }
        }
        doc.addTexts(config.textFields);
        if(doc.emptyText()) emptyText++;
        filterDocumentFields(doc, config.docFields);
    }

}
