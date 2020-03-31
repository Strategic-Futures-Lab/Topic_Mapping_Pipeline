package P1_Input;

import PX_Helper.JSONIOWrapper;
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

    private ConcurrentHashMap<String, CorpusJSONDocument> Docs = new ConcurrentHashMap<>();
    private int numDocs;

    public static void CSVInput(String sourceFile, HashMap<String, String> fields, String outputFile){
        System.out.println( "**********************************************************\n" +
                            "* STARTING CSV Input !                                   *\n" +
                            "**********************************************************\n");
        CSVInput startClass = new CSVInput();
        startClass.LoadCSVFile(sourceFile, fields);
        startClass.OutputJSON(outputFile);
        System.out.println( "**********************************************************\n" +
                            "* CSV Input: COMPLETE !                                  *\n" +
                            "**********************************************************\n");
    }


    private void LoadCSVFile(String sourceFile, HashMap<String, String> fields){
        File file = new File(sourceFile);
        CsvReader csvReader = new CsvReader();
        csvReader.setContainsHeader(true);

        int rowNum = 0;
        System.out.println("Reading CSV: "+sourceFile+" ...");

        try(CsvParser csvParser = csvReader.parse(file, StandardCharsets.UTF_8)){
            CsvRow row;
            while((row = csvParser.nextRow()) != null && rowNum < PROCESS_MAX_ROWS){
                CorpusJSONDocument doc = new CorpusJSONDocument(Integer.toString(rowNum), rowNum);
                for(Map.Entry<String, String> entry: fields.entrySet()){
                    doc.addField(entry.getKey(), row.getField(entry.getValue()));
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

    private void OutputJSON(String outputFile){
        JSONObject root = new JSONObject();
        JSONArray corpus = new JSONArray();
        JSONObject meta = new JSONObject();
        meta.put("totalDocs", numDocs);
        root.put("metadata", meta);
        for(Map.Entry<String, CorpusJSONDocument> entry: Docs.entrySet()){
            corpus.add(entry.getValue().toJSON());
        }
        root.put("corpus", corpus);
        JSONIOWrapper.SaveJSON(root, outputFile);
    }
}
