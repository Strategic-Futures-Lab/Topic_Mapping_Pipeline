package input;

import IO.CSVHelper;
import IO.Console;
import IO.Timer;
import config.Project;
import config.modules.CorpusHTML;
import data.Document;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Module generating a corpus from a CSV file containing url to HTML pages
 *
 * @author P. Le Bras
 * @version 2
 */
public class HTMLInput extends InputModule {

    private String sourceFile;
    private String outputFile;
    private HashMap<String, String> docFields;
    private String urlField;
    private String domSelector;

    // crawl variables
    private ConcurrentHashMap<String, Document> missingDocs;
    private ConcurrentHashMap<String, String> crawlErrors;
    private int pagesCrawled;

    private final static boolean RUN_IN_PARALLEL = true;
    private final static int MAX_RETRIES = 3;

    /**
     * Main module method - processes parameters, reads CSV file, crawl HTML pages and write JSON corpus
     * @param moduleParameters module parameters
     * @param projectParameters project meta parameters
     * @throws IOException If the CSV file cannot be read properly
     */
    public static void run(CorpusHTML moduleParameters, Project projectParameters) throws IOException {
        String MODULE_NAME = moduleParameters.moduleName+" ("+moduleParameters.moduleType+")";
        Console.moduleStart(MODULE_NAME);
        Timer.start(MODULE_NAME);
        HTMLInput instance = new HTMLInput();
        instance.processParameters(moduleParameters, projectParameters);
        try {
            instance.loadCSV();
            instance.crawlHTML();
            instance.writeJSON(instance.outputFile);
        } catch (Exception e) {
            Console.moduleFail(MODULE_NAME);
            throw e;
        }
        Console.moduleComplete(MODULE_NAME);
        Timer.stop(MODULE_NAME);
    }

    // processes project and module parameters
    private void processParameters(CorpusHTML moduleParameters, Project projectParameters){
        Console.log("Processing parameters");
        sourceFile = projectParameters.sourceDirectory+moduleParameters.source;
        outputFile = projectParameters.dataDirectory+moduleParameters.output;
        docFields = moduleParameters.fields;
        urlField = moduleParameters.urlField;
        domSelector = moduleParameters.domSelector;
        Console.tick();
        Console.info("Crawling HTML pages listed in "+sourceFile+" and saving to "+outputFile, 1);
    }

    // loads document data from CSV
    private void loadCSV() throws IOException {
        CSVHelper.ProcessCSVRow rowProcessor = (row, rowNum) -> {
            Document doc = new Document(Integer.toString(rowNum),rowNum);
            for(Map.Entry<String, String> entry: docFields.entrySet()){
                doc.addField(entry.getKey(), row.getField(entry.getValue()));
            }
            doc.addField("url", row.getField(urlField));
            documents.put(doc.getId(), doc);
        };
        try {
            CSVHelper.loadCSVFile(sourceFile, rowProcessor);
        } catch (IOException e) {
            Console.error("Error while reading the CSV input");
            throw e;
        } finally {
            Console.note("Number of documents loaded from file: "+documents.size());
        }
    }

    // crawls HTML pages to retrieve text
    private void crawlHTML(){
        Console.log("Fetching text from HTML pages");
        crawlErrors = new ConcurrentHashMap<>();
        pagesCrawled = 0;
        if(RUN_IN_PARALLEL) documents.entrySet().parallelStream().forEach(this::fetchHTML);
        else documents.entrySet().forEach(this::fetchHTML);
        if(crawlErrors.size() > 0) retryFailed();
        Console.note("Fetched "+pagesCrawled+" pages successfully", 1);
    }

    // fetches an HTML page
    private void fetchHTML(Map.Entry<String, Document> docEntry){
        String id = docEntry.getKey();
        Document doc = docEntry.getValue();
        String url = doc.getField("url");
        try{
            org.jsoup.nodes.Document HTMLDoc = Jsoup.connect(url).get();
            org.jsoup.nodes.Element HTMLBody = HTMLDoc.selectFirst(domSelector);
            String text = "";
            if (HTMLBody != null) text = HTMLBody.text();
            else Console.warning("The HTML page associated with document "+id+" returned an empty text, check that the domSelector is correct", 1);
            doc.addField("text", text);
            pagesCrawled++;
        } catch (IOException e){
            crawlErrors.put(id, e.toString());
        }
    }

    // retries fetching text up to MAX_RETRIES times
    private void retryFailed(){
        int retries = 0;
        while(!crawlErrors.isEmpty() && retries < MAX_RETRIES){
            retries++;
            Console.log(crawlErrors.size()+" failed retrieval - retrying ("+retries+"/"+MAX_RETRIES+")",1);
            ConcurrentHashMap<String, Document> missingRows = new ConcurrentHashMap<>();
            for(String id: crawlErrors.keySet()){
                missingRows.put(id, documents.get(id));
            }
            crawlErrors.clear();
            if(RUN_IN_PARALLEL) documents.entrySet().parallelStream().forEach(this::fetchHTML);
            else documents.entrySet().forEach(this::fetchHTML);
        }
        if(!crawlErrors.isEmpty()){
            Console.error(crawlErrors.size()+" pages could not be fetched successfully", 1);
            for(Map.Entry<String, String> e: crawlErrors.entrySet()){
                Console.error("Document "+e.getKey()+": "+e.getValue(), 2);
            }
        }
    }

}
