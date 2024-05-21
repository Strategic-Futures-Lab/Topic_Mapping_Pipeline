package input;

import IO.CSVHelper;
import IO.Console;
import IO.Timer;
import config.ModuleConfig;
import config.ProjectConfig;
import config.modules.InputConfigGTR;
import data.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Module generating a corpus from a CSV file containing Gateway to Research project IDs
 *
 * @author T. Methven, A. Gharavi, P. Le Bras
 * @version 3
 */
public class GTRInput extends InputModule {

    // module parameters
    private HashMap<String, String> docFields;
    private String pidField;
    private HashMap<String, String> gtrFields;

    // crawl variables
    private ConcurrentHashMap<String, String> crawlErrors;
    private int projectsCrawled;

    // Flag for processing PDFs in parallel (may affect order of documents)
    private final static boolean RUN_IN_PARALLEL = true;
    private final static int MAX_RETRIES = 3;

    /**
     * Main module method - processes parameters, reads CSV file, crawl GtR pages and write JSON corpus
     * @param moduleParameters module parameters
     * @param projectParameters project meta parameters
     * @throws IOException If the CSV file cannot be read properly
     */
    public static void run(ModuleConfig moduleParameters, ProjectConfig projectParameters) throws IOException {
        String MODULE_NAME = moduleParameters.moduleName+" ("+moduleParameters.moduleType+")";
        Console.moduleStart(MODULE_NAME);
        Timer.start(MODULE_NAME);
        GTRInput instance = new GTRInput();
        instance.processParameters((InputConfigGTR) moduleParameters, projectParameters);
        try {
            instance.loadCSV();
            instance.crawlGTR();
            instance.writeJSON();
        } catch (Exception e) {
            Console.moduleFail(MODULE_NAME);
            throw e;
        }
        Console.moduleComplete(MODULE_NAME);
        Timer.stop(MODULE_NAME);
    }

    // processes project and module parameters
    private void processParameters(InputConfigGTR moduleParameters, ProjectConfig projectParameters){
        Console.log("Processing parameters");
        source = projectParameters.sourceDirectory+moduleParameters.source;
        outputFile = projectParameters.dataDirectory+moduleParameters.output;
        docFields = moduleParameters.fields;
        pidField = moduleParameters.pidField;
        gtrFields = moduleParameters.gtrFields;
        Console.tick();
        Console.info("Crawling GtR projects listed in "+source+" and saving to "+outputFile, 1);
    }

    // loads document data from CSV
    private void loadCSV() throws IOException {
        CSVHelper.ProcessCSVRow rowProcessor = (row, rowNum) -> {
            Document doc = new Document(Integer.toString(rowNum),rowNum);
            for(Map.Entry<String, String> entry: docFields.entrySet()){
                doc.addField(entry.getKey(), row.getField(entry.getValue()));
            }
            doc.addField("pid", row.getField(pidField));
            documents.put(doc.getId(), doc);
        };
        try {
            CSVHelper.loadCSVFile(source, rowProcessor);
        } catch (IOException e) {
            Console.error("Error while reading the CSV input");
            throw e;
        } finally {
            Console.note("Number of documents loaded from file: "+documents.size());
        }
    }

    // crawls GTR pages to retrieve text
    private void crawlGTR(){
        Console.log("Fetching project data from GtR");
        crawlErrors = new ConcurrentHashMap<>();
        projectsCrawled = 0;
        if(RUN_IN_PARALLEL) documents.entrySet().parallelStream().forEach(this::getGTRData);
        else documents.entrySet().forEach(this::getGTRData);
        if(crawlErrors.size() > 0) retryFailed();
        Console.note("Fetched "+projectsCrawled+" projects successfully", 1);
    }

    // fetches an GtR XML page
    private void getGTRData(Map.Entry<String, Document> docEntry){
        String id = docEntry.getKey();
        Document doc = docEntry.getValue();
        String url = "https://gtr.ukri.org/gtr/api/projects/" + doc.getField("pid") +"/?format=xml";
        try{
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            org.w3c.dom.Document overviewXML = db.parse(new URL(url).openStream());
            overviewXML.getDocumentElement().normalize();
            getDataFromXML(doc, overviewXML);
            projectsCrawled++;
        } catch (IOException | ParserConfigurationException | SAXException | XPathExpressionException e){
            crawlErrors.put(id, e.toString());
        }
    }

    // parses XML for document data
    private void getDataFromXML(Document doc, org.w3c.dom.Document xml) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {
        for(Map.Entry<String, String> entry: gtrFields.entrySet()){
            String key = entry.getKey();
            String val = entry.getValue();
            switch (val){
                case "Abstract":
                    doc.addField(key, readNode(xml.getElementsByTagName("ns2:abstractText")));
                    break;
                case "TechAbstract":
                    doc.addField(key, readNode(xml.getElementsByTagName("ns2:techAbstractText")));
                    break;
                case "Impact":
                    doc.addField(key, readNode(xml.getElementsByTagName("ns2:potentialImpact")));
                    break;
                case "Title":
                    doc.addField(key, readNode(xml.getElementsByTagName("ns2:title")));
                    break;
                case "Funder":
                    doc.addField(key, readNode(xml.getElementsByTagName("ns2:leadFunder")));
                    break;
                case "Institution":
                    doc.addField(key, getSecondaryXml(xml, "LEAD_ORG", new String[]{"ns2:name"}));
                    break;
                case "Investigator":
                    doc.addField(key, getSecondaryXml(xml, "PI_PER", new String[]{"ns2:firstName","ns2:surname"}));
                    break;
                case "StartDate":
                    doc.addField(key, readNodeAttribute(getLinkNodes(xml, "FUND"), "ns1:start"));
                    break;
                case "EndDate":
                    doc.addField(key, readNodeAttribute(getLinkNodes(xml, "FUND"), "ns1:end"));
                    break;
                default:
                    break;
            }
        }
    }

    // reads text from XML node
    private String readNode(NodeList nl){
        if(nl.getLength() > 0){ return nl.item(0).getTextContent(); }
        else { return ""; }
    }
    // reads attribute from XML node
    private String readNodeAttribute(NodeList nl, String attr){
        if(nl.getLength() > 0){ return nl.item(0).getAttributes().getNamedItem(attr).getNodeValue(); }
        else { return ""; }
    }
    // reads link node from XML
    private NodeList getLinkNodes(org.w3c.dom.Document xml, String rel) throws XPathExpressionException {
        XPathFactory xPathfactory = XPathFactory.newInstance();
        XPath xpath = xPathfactory.newXPath();
        XPathExpression expr = xpath.compile(String.format("//ns1:link[@ns1:rel=\"%s\"]", rel));
        return (NodeList) expr.evaluate(xml, XPathConstants.NODESET);
    }
    // reads from linked XML page
    private String getSecondaryXml(org.w3c.dom.Document origXml, String rel, String[] tagNames) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {
        StringBuilder res = new StringBuilder();
        // get the link from original xml doc
        NodeList nl = getLinkNodes(origXml, rel);
        if(nl.getLength() > 0){
            String url = readNodeAttribute(nl, "ns1=href");
            // with the url fetch the secondary xml and grab the tags' values
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            org.w3c.dom.Document xml = db.parse(new URL(url).openStream());
            xml.getDocumentElement().normalize();
            for(String s: tagNames){
                if(res.length() > 0){ res.append(" "); }
                res.append(readNode(xml.getElementsByTagName(s)));
            }
        }
        return res.toString();
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
            if(RUN_IN_PARALLEL) documents.entrySet().parallelStream().forEach(this::getGTRData);
            else documents.entrySet().forEach(this::getGTRData);
        }
        if(!crawlErrors.isEmpty()){
            Console.error(crawlErrors.size()+" projects could not be fetched successfully", 1);
            for(Map.Entry<String, String> e: crawlErrors.entrySet()){
                Console.error("Document "+e.getKey()+": "+e.getValue(), 2);
            }
        }
    }

}
