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
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GTRCrawler {

    private final static int PROCESS_MAX_ROWS = Integer.MAX_VALUE;

    private ConcurrentHashMap<String, DocIOWrapper> Docs = new ConcurrentHashMap<>();
    private int numDocs;

    // project specs
    private String sourceFile;
    private HashMap<String, String> fields;
    private String outputFile;

    public static String CSVInput(InputModuleSpecs inputSpecs){

        LogPrint.printModuleStart("GTR Crawler Input");

        long startTime = System.currentTimeMillis();

        GTRCrawler startClass = new GTRCrawler();
        startClass.processSpecs(inputSpecs);
        startClass.LoadCSVFile();
        startClass.OutputJSON();

        long timeTaken = (System.currentTimeMillis() - startTime) / (long)1000;

        LogPrint.printModuleEnd("GTR Crawler Input");

        return "CSV Input: "+Math.floorDiv(timeTaken, 60) + " m, " + timeTaken % 60 + " s.";

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

                //crawl for additional fields like abstract from GTR api
                String URLString = "https://gtr.ukri.org/gtr/api/projects/" + row.getField("ProjectId") +"/?format=xml";

                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();

                Document overviewXML = db.parse(new URL(URLString).openStream());

                overviewXML.getDocumentElement().normalize();

                //Abstract:
                NodeList nList = overviewXML.getElementsByTagName("ns2:abstractText");
                if(nList.getLength() > 0)
                    doc.addData("abstract", nList.item(0).getTextContent());

                else
                    doc.addData("abstract", "");


                Docs.put(doc.getId(), doc);
                rowNum++;
            }
        }
        catch (IOException | ParserConfigurationException | SAXException e){
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
