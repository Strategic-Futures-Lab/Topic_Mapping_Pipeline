package P1_Input;

import PX_Helper.CorpusDocument;
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

    private ConcurrentHashMap<String, CorpusDocument> Docs = new ConcurrentHashMap<>();
    private int numDocs;

    public static void CSVInput(String sourceFile, HashMap<String, String> fields, String outputFile){
        System.out.println( "\n**********************************************************\n" +
                            "*                                                          *\n" +
                            "* STARTING A1_CSV Input !                                  *\n" +
                            "*                                                          *\n" +
                            "************************************************************\n");
        CSVInput startClass = new CSVInput();
        startClass.LoadCSVFile(sourceFile, fields);
        startClass.OutputJSON(outputFile);
        System.out.println( "\n**********************************************************\n" +
                            "*                                                          *\n" +
                            "* A1_CSV Input: COMPLETE !                                 *\n" +
                            "*                                                          *\n" +
                            "************************************************************\n");
    }


    private void LoadCSVFile(String sourceFile, HashMap<String, String> fields){
        File file = new File(sourceFile);
        CsvReader csvReader = new CsvReader();
        csvReader.setContainsHeader(true);

        int rowNum = 0;

        try(CsvParser csvParser = csvReader.parse(file, StandardCharsets.UTF_8)){
            CsvRow row;
            while((row = csvParser.nextRow()) != null && rowNum < PROCESS_MAX_ROWS){
                CorpusDocument doc = new CorpusDocument(Integer.toString(rowNum++));
                for(Map.Entry<String, String> entry: fields.entrySet()){
                    doc.addField(entry.getKey(), row.getField(entry.getValue()));
                }
                Docs.put(doc.getId(), doc);
            }
        }
        catch (IOException e){
            e.printStackTrace();
        }
        finally {
            numDocs = Docs.size();
            System.out.println("Number of grants recovered from file: " + numDocs);
        }
    }

    private void OutputJSON(String outputFile){
        JSONObject root = new JSONObject();
        JSONArray corpus = new JSONArray();
        for(Map.Entry<String, CorpusDocument> entry: Docs.entrySet()){
            corpus.add(entry.getValue().toJSON());
        }
        root.put("corpus", corpus);
        JSONIOWrapper.SaveJSON(root, outputFile);
    }
}
