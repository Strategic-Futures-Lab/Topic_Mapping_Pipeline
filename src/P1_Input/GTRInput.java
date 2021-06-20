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
import javax.xml.xpath.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class reading project ids (and other data) from a CSV input file, then proposes to fetch additional
 * data from Gateway to Research (GtR) to fill the corpus and save it as JSON file.
 *
 * @author T. Methven, A. Gharavi, P. Le Bras
 * @version 2
 */
public class GTRInput {

    /** Maximum number of CSV rows the module can process. */
    private final static int PROCESS_MAX_ROWS = Integer.MAX_VALUE;

    /** List of documents read from the CSV file and completed with the GtR crawler. */
    private final ConcurrentHashMap<String, DocIOWrapper> Docs = new ConcurrentHashMap<>();
    /** Number of documents read. */
    private int numDocs;

    // project specs
    /** CSV source file name. */
    private String sourceFile;
    /** Data columns (or fields) to export from the CSV and keep in the corpus: keys are the field names in the
     * corpus, values are the column names in the CSV file. */
    private HashMap<String, String> csvFields;
    /** Data fields to fetch from GtR and keep in the corpus: keys are the field names in the
     * corpus, values are the fields names for GtR.
     * Accepted values: Abstract, TechAbstract, Impact, Title, Funder, Institution, Investigator, StartDate, EndDate.*/
    private HashMap<String, String> xmlFields;
    /** Column name, in the input CSV, with GtR Project IDs. */
    private String PIDField;
    /** File name for the produced JSON corpus. */
    private String outputFile;

    // crawl variables
    /** List of missed retrieval from GtR. */
    private ConcurrentHashMap<String, DocIOWrapper> MissingRows = new ConcurrentHashMap<>();
    /** List of reasons for missed retrieval from GtR. */
    private ConcurrentHashMap<String, String> MissingReasons = new ConcurrentHashMap<>();
    /** Number of successful retrievals. */
    private int grantsProcessed = 0;
    /** Flag for running the crawl in parallel. */
    private final static boolean RUN_IN_PARALLEL = true;

    /**
     * Main method, reads the specification and launches the sub-methods in order.
     * @param inputSpecs Specifications.
     * @return String indicating the time taken to read the CSV input file, crawl GtR, and produce the JSON corpus file.
     */
    public static String GTRInput(InputModuleSpecs inputSpecs){

        LogPrint.printModuleStart("GtR Input");

        long startTime = System.currentTimeMillis();

        GTRInput startClass = new GTRInput();
        startClass.ProcessArguments(inputSpecs);
        startClass.LoadCSVFile();
        startClass.CrawlGtR();
        startClass.OutputJSON();

        long timeTaken = (System.currentTimeMillis() - startTime) / (long)1000;

        LogPrint.printModuleEnd("GtR Input");

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
        xmlFields = inputSpecs.extraFields;
        PIDField = inputSpecs.GTR_PID;
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
                doc.addData("PID", row.getField(PIDField));
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
     * Method crawling GtR to fetch additional data for each documents.
     */
    private void CrawlGtR(){
        LogPrint.printNewStep("Fetching additional data from GtR", 0);
        MissingRows = new ConcurrentHashMap<>();
        MissingReasons = new ConcurrentHashMap<>();
        if(RUN_IN_PARALLEL) { Docs.entrySet().parallelStream().forEach(this::getAdditionalData);}
        else { Docs.entrySet().forEach(this::getAdditionalData);}
        if(MissingRows.size() > 0){
            retryFailed();
        } else {
            LogPrint.printCompleteStep();
        }
    }

    /**
     * Method to retry fetching data from GtR, launches 3 retries maximum.
     */
    private void retryFailed(){
        int retries = 0;
        while(MissingRows.size() > 0 && retries < 3) {
            retries++;
            LogPrint.printNewStep(MissingRows.size()+" failed retrieval. Retrying ("+retries+"/3)", 1);
            ConcurrentHashMap<String, DocIOWrapper> prevMissingRows = MissingRows;
            MissingRows = new ConcurrentHashMap<>();
            MissingReasons = new ConcurrentHashMap<>();
            if(RUN_IN_PARALLEL) { prevMissingRows.entrySet().parallelStream().forEach(this::getAdditionalData); }
            else { prevMissingRows.entrySet().forEach(this::getAdditionalData); }
        }
        if(MissingRows.size() > 0) {
            LogPrint.printNote(grantsProcessed+" successful retrievals", 1);
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
     * Method getting additional data for a specific grant/document.
     * If the data fetch fails. Adds this document to the list of missing fetches for retries.
     * @param entry Document to get additional data for.
     */
    private void getAdditionalData(Map.Entry<String, DocIOWrapper> entry){
        String id = entry.getKey();
        DocIOWrapper doc = entry.getValue();
        String url = "https://gtr.ukri.org/gtr/api/projects/" + doc.getData("PID") +"/?format=xml";
        try{
            // Get XML from GtR
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document overviewXML = db.parse(new URL(url).openStream());
            overviewXML.getDocumentElement().normalize();
            getAdditionalDataFromXML(doc, overviewXML);
            grantsProcessed++;
        } catch (IOException | ParserConfigurationException | SAXException | XPathExpressionException e){
            MissingRows.put(id, doc);
            MissingReasons.put(id, e.toString());
        }
    }

    /**
     * Method exploring a given XML document to fill a (corpus) document's data entries using the XML fields from
     * the module's specifications.
     * @param doc Corpus document to fill with data.
     * @param xml XML document to explore.
     * @throws ParserConfigurationException If error while connecting to GtR.
     * @throws IOException If error while connecting to GtR.
     * @throws SAXException If error while connecting to GtR.
     * @throws XPathExpressionException If error while exploring the XML document.
     */
    private void getAdditionalDataFromXML(DocIOWrapper doc, Document xml) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {
        for(Map.Entry<String, String> entry: xmlFields.entrySet()){
            String key = entry.getKey();
            String val = entry.getValue();
            switch (val){
                case "Abstract":
                    doc.addData(key, readNode(xml.getElementsByTagName("ns2:abstractText")));
                    break;
                case "TechAbstract":
                    doc.addData(key, readNode(xml.getElementsByTagName("ns2:techAbstractText")));
                    break;
                case "Impact":
                    doc.addData(key, readNode(xml.getElementsByTagName("ns2:potentialImpact")));
                    break;
                case "Title":
                    doc.addData(key, readNode(xml.getElementsByTagName("ns2:title")));
                    break;
                case "Funder":
                    doc.addData(key, readNode(xml.getElementsByTagName("ns2:leadFunder")));
                    break;
                case "Institution":
                    doc.addData(key, getSecondaryXml(xml, "LEAD_ORG", new String[]{"ns2:name"}));
                    break;
                case "Investigator":
                    doc.addData(key, getSecondaryXml(xml, "PI_PER", new String[]{"ns2:firstName","ns2:surname"}));
                    break;
                case "StartDate":
                    doc.addData(key, readNodeAttribute(getLinkNodes(xml, "FUND"), "ns1:start"));
                    break;
                case "EndDate":
                    doc.addData(key, readNodeAttribute(getLinkNodes(xml, "FUND"), "ns1:end"));
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Method finding a link in the main XML document to a secondary XML document and retrieving data from there too.
     * @param origXml Main XML document.
     * @param rel Relationship value to find the link.
     * @param tagNames List of tag names in the secondary XML document to get the value of.
     * @return The concatenated string of all the values found.
     * @throws ParserConfigurationException If error while connecting to GtR.
     * @throws IOException If error while connecting to GtR.
     * @throws SAXException If error while connecting to GtR.
     * @throws XPathExpressionException If error while exploring the XML document.
     */
    private String getSecondaryXml(Document origXml, String rel, String[] tagNames) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {
        StringBuilder res = new StringBuilder();
        // get the link from original xml doc
        NodeList nl = getLinkNodes(origXml, rel);
        if(nl.getLength() > 0){
            String url = readNodeAttribute(nl, "ns1=href");
            // with the url fetch the secondary xml and grab the tags' values
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document xml = db.parse(new URL(url).openStream());
            xml.getDocumentElement().normalize();
            for(String s: tagNames){
                if(res.length() > 0){ res.append(" "); }
                res.append(readNode(xml.getElementsByTagName(s)));
            }
        }
        return res.toString();
    }

    /**
     * Method finding a link node in the XML document with the specified relationship value.
     * @param xml XML document to explore.
     * @param rel Relationship value to filter link nodes.
     * @return The list of XML link nodes corresponding to the relationship value.
     * @throws XPathExpressionException If error while exploring the XML document.
     */
    private NodeList getLinkNodes(Document xml, String rel) throws XPathExpressionException {
        XPathFactory xPathfactory = XPathFactory.newInstance();
        XPath xpath = xPathfactory.newXPath();
        XPathExpression expr = xpath.compile(String.format("//ns1:link[@ns1:rel=\"%s\"]", rel));
        return (NodeList) expr.evaluate(xml, XPathConstants.NODESET);
    }

    /**
     * Method reading an attribute's value from an XML node. Returns "" by default.
     * @param nl List of nodes to get the attribute from.
     * @param attr Attribute's name.
     * @return Attribute's value, or "" if nl is empty.
     */
    private String readNodeAttribute(NodeList nl, String attr){
        if(nl.getLength() > 0){ return nl.item(0).getAttributes().getNamedItem(attr).getNodeValue(); }
        else { return ""; }
    }

    /**
     * Method reading an XML node's content. Returns "" by default.
     * @param nl List of nodes to get the content from.
     * @return Node's value, or "" if nl is empty.
     */
    private String readNode(NodeList nl){
        if(nl.getLength() > 0){ return nl.item(0).getTextContent(); }
        else { return ""; }
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
