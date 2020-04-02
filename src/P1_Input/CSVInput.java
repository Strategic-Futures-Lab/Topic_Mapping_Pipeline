package P1_Input;

import P0_Project.ProjectInput;
import PX_Data.JSONDocument;
import PX_Data.JSONIOWrapper;
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

    private ConcurrentHashMap<String, JSONDocument> Docs = new ConcurrentHashMap<>();
    private int numDocs;

    // project specs
    private String sourceFile;
    private HashMap<String, String> fields;
    private String outputFile;

    public static void CSVInput(ProjectInput inputSpecs){
        System.out.println( "**********************************************************\n" +
                            "* STARTING CSV Input !                                   *\n" +
                            "**********************************************************\n");
        CSVInput startClass = new CSVInput();
        startClass.processSpecs(inputSpecs);
        startClass.LoadCSVFile();
        startClass.OutputJSON();
        System.out.println( "**********************************************************\n" +
                            "* CSV Input: COMPLETE !                                  *\n" +
                            "**********************************************************\n");
    }

    private void processSpecs(ProjectInput inputSpecs){
        sourceFile = inputSpecs.source;
        fields = inputSpecs.fields;
        outputFile = inputSpecs.output;
    }


    private void LoadCSVFile(){
        File file = new File(sourceFile);
        CsvReader csvReader = new CsvReader();
        csvReader.setContainsHeader(true);

        int rowNum = 0;
        System.out.println("Reading CSV: "+sourceFile+" ...");

        try(CsvParser csvParser = csvReader.parse(file, StandardCharsets.UTF_8)){
            CsvRow row;
            while((row = csvParser.nextRow()) != null && rowNum < PROCESS_MAX_ROWS){
                JSONDocument doc = new JSONDocument(Integer.toString(rowNum), rowNum);
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
            System.out.println("Finished!");
            System.out.println("Number of documents recovered from file: " + numDocs);
        }
    }

    private void OutputJSON(){
        JSONObject root = new JSONObject();
        JSONArray corpus = new JSONArray();
        JSONObject meta = new JSONObject();
        meta.put("totalDocs", numDocs);
        root.put("metadata", meta);
        for(Map.Entry<String, JSONDocument> entry: Docs.entrySet()){
            corpus.add(entry.getValue().toJSON());
        }
        root.put("corpus", corpus);
        JSONIOWrapper.SaveJSON(root, outputFile);
    }
}
