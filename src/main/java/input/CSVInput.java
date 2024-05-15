package input;

import IO.CSVHelper;
import IO.Console;
import config.Project;
import config.modules.CorpusCSV;
import data.Document;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CSVInput {

    private final ConcurrentHashMap<String, Document> documents = new ConcurrentHashMap<>();

    private String sourceFile;
    private String outputFile;
    private HashMap<String, String> docFields;


    public static String run(CorpusCSV moduleParameters, Project projectParameters) throws IOException {
        String MODULE_NAME = moduleParameters.moduleName+" ("+moduleParameters.moduleType+")";
        Console.moduleStart(MODULE_NAME);
        long startTime = System.currentTimeMillis();
        CSVInput instance = new CSVInput();
        instance.processParameters(moduleParameters, projectParameters);
        try {
            instance.loadCSV();
            // TODO
//            instance.outputJSON();
        } catch (Exception e) {
            Console.moduleFail(MODULE_NAME);
            throw e;
        }
        long timeTaken = (System.currentTimeMillis() - startTime) / (long)1000;
        Console.moduleComplete(MODULE_NAME);
        return MODULE_NAME+": " + Math.floorDiv(timeTaken, 60) + " m, " + timeTaken % 60 + " s.";
    }

    private void processParameters(CorpusCSV moduleParameters, Project projectParameters){
        Console.log("Processing parameters");
        sourceFile = projectParameters.sourceDirectory+moduleParameters.source;
        outputFile = projectParameters.dataDirectory+moduleParameters.output;
        docFields = moduleParameters.fields;
        Console.tick();
        Console.info("Reading CSV input from "+sourceFile+" and saving to "+outputFile, 1);
    }

    private void loadCSV() throws IOException {
        CSVHelper.ProcessCSVRow rowProcessor = (row, rowNum) -> {
            // TODO integrate with document data
            for(Map.Entry<String, String> entry: docFields.entrySet()){
                Console.log(row.getField(entry.getValue()));
            }
        };
        try {
            CSVHelper.loadCSVFile(sourceFile, rowProcessor);
        } catch (IOException e) {
            Console.error("Error while reading the CSV input");
            throw e;
        }
    }
}
