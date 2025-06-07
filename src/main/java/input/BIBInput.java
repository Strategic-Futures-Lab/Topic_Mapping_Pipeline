package input;

import IO.BIBHelper;
import IO.Console;
import IO.Timer;
import data.Document;
import pipeline.config.ModuleConfig;
import pipeline.config.modules.InputConfigBIB;

import java.io.IOException;
import java.util.Map;

/**
 * Module generating a corpus from a BibTex file
 *
 * @author P. Le Bras
 * @version 1
 */
public class BIBInput extends InputModule {

    // module parameters
    private final InputConfigBIB config;

    private BIBInput(InputConfigBIB c){
        config = c;
        config.logConfig();
        corpus.name = config.corpusName;
    }

    /**
     * Main module method - processes parameters, reads BibTex file and write JSON corpus
     * @param moduleParameters module parameters
     * @throws IOException If the BibTex file cannot be read properly
     */
    public static void run(ModuleConfig moduleParameters) throws Exception {
        String MODULE_NAME = moduleParameters.moduleName+" ("+moduleParameters.moduleType+")";
        Console.moduleStart(MODULE_NAME);
        Timer.start(MODULE_NAME);
        BIBInput instance = new BIBInput((InputConfigBIB) moduleParameters);
        try {
            instance.loadBibTex();
            instance.writeCorpus(instance.config.outputFile);
        } catch (Exception e) {
            Console.moduleFail(MODULE_NAME);
            throw e;
        }
        Console.moduleComplete(MODULE_NAME);
        Timer.stop(MODULE_NAME);
    }

    // loads documents data from BibTex
    private void loadBibTex() throws Exception {
        BIBHelper.ProcessBIBEntry entryProcessor = (key, entry, entryNum) ->{
            Document doc = new Document(key.toString(), entryNum);
            for(Map.Entry<String, String> field: config.documentFields.entrySet()){
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
            BIBHelper.loadBIBFile(config.sourceFile, entryProcessor);
        } catch (Exception e) {
            Console.error("Error while reading the BibTex input", 1);
            throw e;
        }
        Console.note("Number of documents loaded from file: "+corpus.size());
    }
}
