package corpus;

import IO.Console;
import IO.Timer;
import pipeline.config.ModuleConfig;
import pipeline.config.ProjectConfig;
import pipeline.config.modules.BuildTextConfig;
import data.Document;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Module loading a corpus JSON file and building a text string from given document fields
 *
 * @author P. Le Bras
 * @version 1
 */
public class BuildText extends CorpusModule {

    // module parameters
    private List<String> textFields;

    // for logging purposes
    private int missingTextField = 0;
    private int emptyText = 0;

    // Flag for processing documents in parallel
    private final static boolean RUN_IN_PARALLEL = true;

    /**
     * Main module method - processes parameters, loads corpus, builds texts and save corpus again
     * @param moduleParameters module parameters
     * @param projectParameters project meta parameters
     * @throws Exception If the corpus cannot load properly
     */
    public static void run(ModuleConfig moduleParameters, ProjectConfig projectParameters) throws Exception {
        String MODULE_NAME = moduleParameters.moduleName+" ("+moduleParameters.moduleType+")";
        Console.moduleStart(MODULE_NAME);
        Timer.start(MODULE_NAME);
        BuildText instance = new BuildText();
        instance.processParameters((BuildTextConfig) moduleParameters, projectParameters);
        try{
            instance.loadCorpus();
            instance.buildTexts();
            instance.writeCorpus();
        } catch (Exception e){
            Console.moduleFail(MODULE_NAME);
            throw e;
        }
        Console.moduleComplete(MODULE_NAME);
        Timer.stop(MODULE_NAME);
    }

    // processes project and module parameters
    private void processParameters(BuildTextConfig moduleParameters, ProjectConfig projectParameters){
        Console.log("Processing parameters");
        corpusFile = projectParameters.dataDirectory+moduleParameters.corpus;
        outputFile = projectParameters.dataDirectory+moduleParameters.output;
        docFields = moduleParameters.docFields == null ? projectParameters.docFields : moduleParameters.docFields;
        textFields = Arrays.stream(moduleParameters.textFields).toList();
        Console.tick();
        String saveDiff = corpusFile.equals(outputFile) ? "" : " and saving to "+outputFile;
        Console.info("Building text for documents in corpus "+corpusFile+saveDiff, 1);
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
        for(String textField: textFields){
            if(!doc.hasField(textField)){
                missingTextField++;
                break;
            }
        }
        doc.addTexts(textFields);
        if(doc.emptyText()) emptyText++;
        filterDocumentFields(doc);
    }

}
