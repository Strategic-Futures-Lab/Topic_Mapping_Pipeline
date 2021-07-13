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

/**
 * Class reading a CSV file data input and writing it as a corpus JSON file.
 *
 * @author T. Methven, P. Le Bras
 * @version 2
 */
public class CSVInput {

    /** Maximum number of rows the module can process. */
    private final static int PROCESS_MAX_ROWS = Integer.MAX_VALUE;

    /** List of documents read from the CSV file. */
    private final ConcurrentHashMap<String, DocIOWrapper> Docs = new ConcurrentHashMap<>();
    /** Number of documents read from the CSV file. */
    private int numDocs;

    // project specs
    /** CSV source file name. */
    private String sourceFile;
    /** Data columns (or fields) to export from the CSV and keep in the corpus: keys are the field names in the
     * corpus, values are the column names in the CSV file. */
    private HashMap<String, String> fields;
    /** File name for the produced JSON corpus. */
    private String outputFile;

    /**
     * Main method, reads the specification and launches the sub-methods in order.
     * @param inputSpecs Specifications.
     * @return String indicating the time taken to read the CSV input file and produce the JSON corpus file.
     */
    public static String CSVInput(InputModuleSpecs inputSpecs){

        LogPrint.printModuleStart("CSV Input");

        long startTime = System.currentTimeMillis();

        CSVInput startClass = new CSVInput();
        startClass.ProcessArguments(inputSpecs);
        startClass.LoadCSVFile();
        startClass.OutputJSON();

        long timeTaken = (System.currentTimeMillis() - startTime) / (long)1000;

        LogPrint.printModuleEnd("CSV Input");

        return "CSV Input: " + Math.floorDiv(timeTaken, 60) + " m, " + timeTaken % 60 + " s.";

    }

    /**
     * Method processing the specification parameters.
     * @param inputSpecs Specification object.
     */
    private void ProcessArguments(InputModuleSpecs inputSpecs){
        LogPrint.printNewStep("Processing arguments", 0);
        sourceFile = inputSpecs.source;
        fields = inputSpecs.fields;
        outputFile = inputSpecs.output;
        LogPrint.printCompleteStep();
    }

    /**
     * Method reading the CSV input file and populating the list of documents.
     */
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
            LogPrint.printNoteError("Error while reading the CSV input.");
            e.printStackTrace();
            System.exit(1);
        }
        finally {
            numDocs = Docs.size();
            LogPrint.printCompleteStep();
            LogPrint.printNote("Number of documents recovered from file: " + numDocs, 0);
        }
    }

    /**
     * Method writing the list of documents onto the JSON corpus file.
     */
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
