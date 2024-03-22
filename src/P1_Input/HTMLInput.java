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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class reading URLs (and other data) from a CSV input file, then fetches HTML documents
 * to fill the corpus and save it as JSON file.
 *
 * @author P. Le Bras
 * @version 1
 */
public class HTMLInput {

    /** Maximum number of CSV rows the module can process. */
    private final static int PROCESS_MAX_ROWS = Integer.MAX_VALUE;

    /** List of documents read from the CSV file and completed with the HTML crawler. */
    private final ConcurrentHashMap<String, DocIOWrapper> Docs = new ConcurrentHashMap<>();
    /** Number of documents read. */
    private int numDocs;

    // project specs
    /** CSV source file name. */
    private String sourceFile;
    /** Data columns (or fields) to export from the CSV and keep in the corpus: keys are the field names in the
     * corpus, values are the column names in the CSV file. */
    private HashMap<String, String> csvFields;
    /** Column name, in the input CSV, with URLs. */
    private String urlField;
    /** HTML selector from which to extract the text (to exclude headers, footers, etc) */
    private String domSelector;
    /** File name for the produced JSON corpus. */
    private String outputFile;

    // crawl variables
    /** List of missed retrieval. */
    private ConcurrentHashMap<String, DocIOWrapper> MissingRows = new ConcurrentHashMap<>();
    /** List of reasons for missed retrieval. */
    private ConcurrentHashMap<String, String> MissingReasons = new ConcurrentHashMap<>();
    /** Number of successful retrievals. */
    private int pagesProcessed = 0;
    /** Flag for running the crawl in parallel. */
    private final static boolean RUN_IN_PARALLEL = true;

    /**
     * Main method, reads the specification and launches the sub-methods in order.
     * @param inputSpecs Specifications.
     * @return String indicating the time taken to read the CSV input file, crawl, and produce the JSON corpus file.
     */
    public static String HTMLInput(InputModuleSpecs inputSpecs){

        LogPrint.printModuleStart("HTML Input");

        long startTime = System.currentTimeMillis();

        HTMLInput startClass = new HTMLInput();
        startClass.ProcessArguments(inputSpecs);
        startClass.LoadCSVFile();
        startClass.CrawlHTML();
        startClass.OutputJSON();

        long timeTaken = (System.currentTimeMillis() - startTime) / (long)1000;

        LogPrint.printModuleEnd("HTML Input");

        return "GtR Input: "+Math.floorDiv(timeTaken, 60) + " m, " + timeTaken % 60 + " s.";

    }

    /**
     * Method processing the specification parameters.
     * @param inputSpecs Specification object.
     */
    private void ProcessArguments(InputModuleSpecs inputSpecs){
        LogPrint.printNewStep("Processing arguments", 0);
        sourceFile = inputSpecs.source;
        csvFields = inputSpecs.fields;
        urlField = inputSpecs.HTML_URL;
        domSelector = inputSpecs.HTML_selector;
        outputFile = inputSpecs.output;
        LogPrint.printCompleteStep();
    }

    /**
     * Method reading the CSV input file and populating the list of documents.
     * Automatically reads the Project ID column.
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
                for(Map.Entry<String, String> entry: csvFields.entrySet()){
                    doc.addData(entry.getKey(), row.getField(entry.getValue()));
                }
                doc.addData("URL", row.getField(urlField));
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
            LogPrint.printNote("Number of documents recovered from file: " + numDocs);
        }
    }

    /**
     * Method lauching the HTML fecthing process for all documents.
     */
    private void CrawlHTML(){
        LogPrint.printNewStep("Fetching text from HTML", 0);
        MissingRows = new ConcurrentHashMap<>();
        MissingReasons = new ConcurrentHashMap<>();
        if(RUN_IN_PARALLEL){
            Docs.entrySet().parallelStream().forEach(this::getHTML);
        } else {
            Docs.entrySet().forEach(this::getHTML);
        }
        if(MissingRows.size() > 0){
            retryFailed();
        } else {
            LogPrint.printCompleteStep();
        }
    }

    /**
     * Method fetching the HTML for a document's url, parsing the result to only keep the text under the DOM selector.
     * @param entry The document to fetch the text for.
     */
    private void getHTML(Map.Entry<String, DocIOWrapper> entry){
        String id = entry.getKey();
        DocIOWrapper doc = entry.getValue();
        String url = doc.getData("URL");
        try{
            Document HTMLDoc = Jsoup.connect(url).get();
            Element HTMLBody = HTMLDoc.selectFirst(domSelector);
            String text = HTMLBody.text();
            doc.addData("text",text);
            pagesProcessed++;
        } catch (IOException e) {
            MissingRows.put(id, doc);
            MissingReasons.put(id, e.toString());
        }
    }

    /**
     * Method to retry fetching HTML is failed, retries up to 3 times.
     */
    private void retryFailed(){
        int retries = 0;
        while(MissingRows.size() > 0 && retries < 3) {
            retries++;
            LogPrint.printNewStep(MissingRows.size()+" failed retrieval. Retrying ("+retries+"/3)", 1);
            ConcurrentHashMap<String, DocIOWrapper> prevMissingRows = MissingRows;
            MissingRows = new ConcurrentHashMap<>();
            MissingReasons = new ConcurrentHashMap<>();
            if(RUN_IN_PARALLEL) { prevMissingRows.entrySet().parallelStream().forEach(this::getHTML); }
            else { prevMissingRows.entrySet().forEach(this::getHTML); }
        }
        if(MissingRows.size() > 0) {
            LogPrint.printNote(pagesProcessed+" successful retrievals", 1);
            LogPrint.printNoteError(MissingRows.size()+" failed retrieval after 3 tries");
            for(Map.Entry<String,String> e: MissingReasons.entrySet()){
                LogPrint.printNoteError(e.getKey(), 0);
                LogPrint.printNoteError(e.getValue(), 1);
            }
            System.exit(1);
        }
        else {
            LogPrint.printCompleteStep();
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
