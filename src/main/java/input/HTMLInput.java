package input;

import IO.CSVHelper;
import IO.Console;
import IO.Timer;
import data.Document;
import org.jsoup.Jsoup;
import pipeline.config.ModuleConfig;
import pipeline.config.modules.InputConfigHTML;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Module generating a corpus from a CSV file containing url to HTML pages
 *
 * @author P. Le Bras
 * @version 2
 */
public class HTMLInput extends InputModule {

    // module parameters
    private final InputConfigHTML config;

    // crawl variables
    private ConcurrentHashMap<String, String> crawlErrors;
    private int pagesCrawled;

    // Flag for processing PDFs in parallel (may affect order of documents)
    private final static boolean RUN_IN_PARALLEL = true;
    private final static int MAX_RETRIES = 3;

    private HTMLInput(InputConfigHTML c){
        config = c;
        config.logConfig();
        corpus.name = config.corpusName;
    }

    /**
     * Main module method - processes parameters, reads CSV file, crawl HTML pages and write JSON corpus
     * @param moduleParameters module parameters
     * @throws IOException If the CSV file cannot be read properly
     */
    public static void run(ModuleConfig moduleParameters) throws IOException {
        String MODULE_NAME = moduleParameters.moduleName+" ("+moduleParameters.moduleType+")";
        Console.moduleStart(MODULE_NAME);
        Timer.start(MODULE_NAME);
        HTMLInput instance = new HTMLInput((InputConfigHTML) moduleParameters);
        try {
            instance.loadCSV();
            instance.crawlHTML();
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
            doc.addField("url", row.getField(config.urlField));
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

    // crawls HTML pages to retrieve text
    private void crawlHTML(){
        Console.log("Fetching text from HTML pages");
        crawlErrors = new ConcurrentHashMap<>();
        pagesCrawled = 0;
        if(RUN_IN_PARALLEL) corpus.documents.entrySet().parallelStream().forEach(this::fetchHTML);
        else corpus.documents.entrySet().forEach(this::fetchHTML);
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
            org.jsoup.nodes.Element HTMLBody = HTMLDoc.selectFirst(config.domSelector);
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
                missingRows.put(id, corpus.documents.get(id));
            }
            crawlErrors.clear();
            if(RUN_IN_PARALLEL) corpus.documents.entrySet().parallelStream().forEach(this::fetchHTML);
            else corpus.documents.entrySet().forEach(this::fetchHTML);
        }
        if(!crawlErrors.isEmpty()){
            Console.error(crawlErrors.size()+" pages could not be fetched successfully", 1);
            for(Map.Entry<String, String> e: crawlErrors.entrySet()){
                Console.error("Document "+e.getKey()+": "+e.getValue(), 2);
            }
        }
    }

}
