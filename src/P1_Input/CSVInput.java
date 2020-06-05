package P1_Input;

import P0_Project.InputModuleSpecs;
import PX_Data.DocIOWrapper;
import PX_Data.JSONIOWrapper;
import PY_Helper.LogPrint;
import de.siegmar.fastcsv.reader.CsvParser;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRow;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CSVInput {

    private final static int PROCESS_MAX_ROWS = Integer.MAX_VALUE;

    private ConcurrentHashMap<String, DocIOWrapper> Docs = new ConcurrentHashMap<>();
    private int numDocs;

    // project specs
    private String sourceFile;
    private HashMap<String, String> fields;
    private String outputFile;

    public static void CSVInput(InputModuleSpecs inputSpecs){

        LogPrint.printModuleStart("CSV Input");

        CSVInput startClass = new CSVInput();
        startClass.processSpecs(inputSpecs);
        startClass.LoadCSVFile();
        startClass.OutputJSON();

        LogPrint.printModuleEnd("CSV Input");

    }

    private void processSpecs(InputModuleSpecs inputSpecs){
        LogPrint.printNewStep("Processing arguments", 0);
        sourceFile = inputSpecs.source;
        fields = inputSpecs.fields;
        outputFile = inputSpecs.output;
        LogPrint.printCompleteStep();
    }


    private void LoadCSVFile(){
        File file = new File(sourceFile);
        CsvReader csvReader = new CsvReader();
        csvReader.setContainsHeader(true);

        int rowNum = 0;
        LogPrint.printNewStep("Reading CSV: "+sourceFile, 0);

        try(CsvParser csvParser = csvReader.parse(file, StandardCharsets.UTF_8)){
            CsvRow row;
            while((row = csvParser.nextRow()) != null && rowNum < PROCESS_MAX_ROWS){
                DocIOWrapper doc = new DocIOWrapper(Integer.toString(rowNum), rowNum);
                for(Map.Entry<String, String> entry: fields.entrySet()){
                    doc.addData(entry.getKey(), row.getField(entry.getValue()));
                }
                Docs.put(doc.getId(), doc);
                rowNum++;
            }
        }
        catch (IOException e){
            e.printStackTrace();
        }
        finally {
            numDocs = Docs.size();
            LogPrint.printCompleteStep();
            LogPrint.printNote("Number of documents recovered from file: " + numDocs);
        }
    }

    private void OutputJSON(){
        JSONObject root = new JSONObject();
        JSONArray corpus = new JSONArray();
        JSONObject meta = new JSONObject();
        meta.put("totalDocs", numDocs);
        root.put("metadata", meta);
        for(Map.Entry<String, DocIOWrapper> entry: Docs.entrySet()){
            corpus.add(entry.getValue().toJSON());
        }
        root.put("corpus", corpus);
        JSONIOWrapper.SaveJSON(root, outputFile, 0);
    }
}
