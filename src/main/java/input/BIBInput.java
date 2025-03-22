package input;

import IO.BIBHelper;
import IO.Console;
import IO.Timer;
import pipeline.config.ModuleConfig;
import pipeline.config.ProjectConfig;
import pipeline.config.modules.InputConfigBIB;
import data.Document;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Module generating a corpus from a BibTex file
 *
 * @author P. Le Bras
 * @version 1
 */
public class BIBInput extends InputModule {

    // module parameters
    private HashMap<String, String> docFields;

    /**
     * Main module method - processes parameters, reads BibTex file and write JSON corpus
     * @param moduleParameters module parameters
     * @param projectParameters project meta parameters
     * @throws IOException If the BibTex file cannot be read properly
     */
    public static void run(ModuleConfig moduleParameters, ProjectConfig projectParameters) throws Exception {
        String MODULE_NAME = moduleParameters.moduleName+" ("+moduleParameters.moduleType+")";
        Console.moduleStart(MODULE_NAME);
        Timer.start(MODULE_NAME);
        BIBInput instance = new BIBInput();
        instance.processParameters((InputConfigBIB) moduleParameters, projectParameters);
        try {
            instance.loadBibTex();
            instance.writeCorpus();
        } catch (Exception e) {
            Console.moduleFail(MODULE_NAME);
            throw e;
        }
        Console.moduleComplete(MODULE_NAME);
        Timer.stop(MODULE_NAME);
    }

    // processes project and module parameters
    private void processParameters(InputConfigBIB moduleParameters, ProjectConfig projectParameters){
        Console.log("Processing parameters");
        source = projectParameters.sourceDirectory+moduleParameters.source;
        outputFile = projectParameters.dataDirectory+moduleParameters.output;
        docFields = moduleParameters.fields;
        Console.tick();
        Console.info("Reading BibTex input from "+source+" and saving to "+outputFile, 1);
    }

    // loads documents data from BibTex
    private void loadBibTex() throws Exception {
        BIBHelper.ProcessBIBEntry entryProcessor = (key, entry, entryNum) ->{
            Document doc = new Document(key.toString(), entryNum);
            for(Map.Entry<String, String> field: docFields.entrySet()){
                try {
                    doc.addField(field.getKey(), BIBHelper.getField(entry, field.getValue()));
                } catch (Exception e){
                    Console.error("Error while parsing BibTex file ("+key+")", 1);
                    throw e;
                }
            }
            corpus.add(key.toString(), doc);
        };
        try{
            BIBHelper.loadBIBFile(source, entryProcessor);
        } catch (Exception e) {
            Console.error("Error while reading the BibTex input", 1);
            throw e;
        }
        Console.note("Number of documents loaded from file: "+corpus.size());
    }
}
