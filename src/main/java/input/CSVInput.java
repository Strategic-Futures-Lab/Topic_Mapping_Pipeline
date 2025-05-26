package input;

import IO.CSVHelper;
import IO.Console;
import IO.Timer;
import pipeline.config.ModuleConfig;
import pipeline.config.modules.InputConfigCSV;
import data.Document;

import java.io.IOException;
import java.util.Map;

/**
 * Module generating a corpus from a CSV file
 *
 * @author T. Methven, P. Le Bras
 * @version 3
 */
public class CSVInput extends InputModule {

    // module parameters
    private InputConfigCSV config;

    private CSVInput(InputConfigCSV c){
        config = c;
        config.logConfig();
    }

    /**
     * Main module method - processes parameters, reads CSV file and write JSON corpus
     * @param moduleParameters module parameters
     * @throws IOException If the CSV file cannot be read properly
     */
    public static void run(ModuleConfig moduleParameters) throws IOException {
        String MODULE_NAME = moduleParameters.moduleName+" ("+moduleParameters.moduleType+")";
        Console.moduleStart(MODULE_NAME);
        Timer.start(MODULE_NAME);
        CSVInput instance = new CSVInput((InputConfigCSV) moduleParameters);
        try {
            instance.loadCSV();
            instance.writeCorpus(instance.config.outputFile);
        } catch (Exception e) {
            Console.moduleFail(MODULE_NAME);
            throw e;
        }
        Console.moduleComplete(MODULE_NAME);
        Timer.stop(MODULE_NAME);
    }

    // loads document data from CSV
    private void loadCSV() throws IOException {
        CSVHelper.ProcessCSVRow rowProcessor = (row, rowNum) -> {
            Document doc = new Document(Integer.toString(rowNum),rowNum);
            for(Map.Entry<String, String> entry: config.documentFields.entrySet()){
                doc.addField(entry.getKey(), row.getField(entry.getValue()));
            }
            corpus.add(doc.getId(), doc);
        };
        try {
            CSVHelper.loadCSVFile(config.sourceFile, rowProcessor);
        } catch (IOException e) {
            Console.error("Error while reading the CSV input");
            throw e;
        } finally {
            Console.note("Number of documents loaded from file: "+corpus.size());
        }
    }
}
