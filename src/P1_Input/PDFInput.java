package P1_Input;

import P0_Project.InputModuleSpecs;
import PX_Data.DocIOWrapper;
import PX_Data.JSONIOWrapper;
import PY_Helper.LogPrint;
import PY_Helper.Pair;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class reading a PDF directory input and writing it as a corpus JSON file.
 *
 * @author T. Methven, P. Le Bras, A. Gharavi
 * @version 2
 */
public class PDFInput {

    /**  List of documents read from PDF directory. */
    private final ConcurrentHashMap<String, DocIOWrapper> Docs = new ConcurrentHashMap<>();
    /** Number of documents read from the CSV file. */
    private int numDocs;
    /** Increment for the count of documents read. */
    private int docCount = 0;

    /** List of PDF files found in the directory,
     * also saves the file's directory name as 'dataset' field in the corpus document data. */
    private final List<Pair<File,String>> fileList = new ArrayList<>();

    /**
     * Flag for parsing PDFs in parallel.
     * WARNING: Running this is parallel will alter the final visualisation due to reordering of processing!
     * For now, this is being set to serial! As of 25/06/2018 it is quick enough that a fix is not forthcoming.
     */
    private final static boolean RUN_IN_PARALLEL = false;

    // project specs
    /** PDF source directory name. */
    private String sourceDirectory;
    /** File name for the produced JSON corpus. */
    private String outputFile;
    /** Number of words limit before splitting a document. */
    private int wordsPerDoc;

    /**
     * Main method, reads the specification and launches the sub-methods in order.
     * @param inputSpecs Specifications.
     * @return String indicating the time taken to read the CSV inout file and produce the JSON corpus file.
     */
    public static String PDFInput(InputModuleSpecs inputSpecs){

        LogPrint.printModuleStart("PDF Input");

        long startTime = System.currentTimeMillis();

        PDFInput startClass = new PDFInput();
        startClass.ProcessArguments(inputSpecs);
        startClass.FindPDFs();
        startClass.ParsePDFs();
        startClass.OutputJSON();

        long timeTaken = (System.currentTimeMillis() - startTime) / (long)1000;

        LogPrint.printModuleEnd("PDF Input");
        return "PDF Input: " + Math.floorDiv(timeTaken, 60) + " m, " + timeTaken % 60 + " s.";
    }

    /**
     * Method processing the specification parameters.
     * @param inputSpecs Specification object.
     */
    private void ProcessArguments(InputModuleSpecs inputSpecs){
        LogPrint.printNewStep("Processing arguments", 0);
        sourceDirectory = inputSpecs.source;
        outputFile = inputSpecs.output;
        wordsPerDoc = inputSpecs.wordsPerDoc;
        LogPrint.printCompleteStep();
    }

    /**
     * Method exploring the source directory, and it's sub-directories, for all PDF files.
     */
    private void FindPDFs(){
        LogPrint.printNewStep("Finding all PDFs in "+sourceDirectory, 0);
        File directory = new File(sourceDirectory);
        if(!directory.isDirectory()){
            // source is not a directory
            LogPrint.printNoteError("Error, provided source is not a directory.");
            System.exit(1);
        } else {
            findPDFsInDirectory(directory);
            if(fileList.size() > 0){
                LogPrint.printCompleteStep();
                LogPrint.printNote("Found "+fileList.size()+" PDF files.", 0);
            } else {
                // did not find any pdf file in the directory
                LogPrint.printNoteError("Error, provided directory source does not contain .pdf files.");
                System.exit(1);
            }
        }
    }

    /**
     * Method recursively exploring a directory for PDF files.
     * @param directory Directory to explore.
     */
    private void findPDFsInDirectory(File directory){
        for(File file : directory.listFiles()){
            if(!file.isDirectory()  ){
                if(file.getName().toLowerCase().endsWith(".pdf"))
                    fileList.add(new Pair<>(file,directory.getName()));
            }else{
                findPDFsInDirectory(file);
            }
        }
    }

    /**
     * Method launching the PDF parsing process.
     */
    private void ParsePDFs(){
        LogPrint.printNewStep("Parsing PDF files:", 0);
        if(RUN_IN_PARALLEL){
            if(wordsPerDoc > 0)
                fileList.parallelStream().forEach(this::ParsePDFDivide);
            else
                fileList.parallelStream().forEach(this::parsePDF);
        } else{
            if(wordsPerDoc > 0)
                fileList.forEach(this::ParsePDFDivide);
            else
                fileList.forEach(this::parsePDF);
        }
        numDocs = Docs.size();
        LogPrint.printNote("Number of documents parsed: " + numDocs, 0);
    }

    /**
     * Method parsing a PDF file WITHOUT dividing it into chunks.
     * @param pdf PDF file to parse, paired with it's dataset value.
     */
    private void parsePDF(Pair<File,String> pdf){
        File file = pdf.getLeft();
        String dataset = pdf.getRight();
        try {
            String rootName = file.getName();
            LogPrint.printNewStep("Processing: "+rootName, 1);
            rootName = rootName.substring(0, rootName.lastIndexOf('.'));
            // load file
            PDDocument document = PDDocument.load(file);
            String text = "";
            // parse text
            for (int pageNumber = 1 ; pageNumber < document.getNumberOfPages(); pageNumber++) {
                PDFTextStripper s = new PDFTextStripper();
                s.setStartPage(pageNumber);
                s.setEndPage(pageNumber);
                String[] contentsOfPage = s.getText(document).split("\\s+");
                for(String word : contentsOfPage) {
                    text += word + " ";
                }
            }
            document.close();
            // create doc entry
            DocIOWrapper newDoc = new DocIOWrapper(Integer.toString(docCount), docCount);
            newDoc.addData("fileName", rootName);
            newDoc.addData("text", text.trim());
            newDoc.addData("dataset", dataset);
            Docs.put(newDoc.getId(), newDoc);
            // increase doc count for next entry
            docCount++;
        } catch (Exception e) {
            LogPrint.printNoteError("Error while parsing PDF.");
            e.printStackTrace();
            System.exit(1);
        } finally {
            LogPrint.printCompleteStep();
        }
    }

    /**
     * Method parsing a PDF file AND dividing it into chunks.
     * @param pdf PDF file to parse, paired with it's dataset value.
     */
    private void ParsePDFDivide(Pair<File,String> pdf) {
        File file = pdf.getLeft();
        String dataset = pdf.getRight();
        int subDocCount = 0;
        try {
            String rootName = file.getName();
            LogPrint.printNewStep("Processing: "+rootName, 1);
            rootName = rootName.substring(0, rootName.indexOf('.'));
            // load file
            PDDocument document = PDDocument.load(file);
            // initialise subtext string, corpus doc object and counters
            String newDocString = "";
            int numWordsInString = 0;
            subDocCount = 0;
            // parse the document and embed page number information in the text
            String text = addPageNumebr(document);
            // Explore the document word by word and
            for (String word : text.split("\\s+")) {
                if (numWordsInString == wordsPerDoc && wordsPerDoc > 0) {
                    // create a new corpus documents when the word limit is reached
                    DocIOWrapper newDoc = new DocIOWrapper(Integer.toString(docCount), docCount);
                    newDoc.addData("originalFileName", rootName);
                    newDoc.addData("splitNumber", String.format("%03d", subDocCount));
                    newDoc.addData("dataset", dataset);
                    newDoc.addData("fileName", rootName + "_" + String.format("%03d", subDocCount));
                    long startPos = (subDocCount * wordsPerDoc);
                    newDoc.addData("wordRange", startPos + " - " + (startPos + numWordsInString));
                    newDoc.addData("pageRange", String.valueOf(returnPageNumbers(newDocString)));
                    newDoc.addData("text", removePageNumber(newDocString.trim()).trim());
                    Docs.put(newDoc.getId(), newDoc);
                    docCount++;
                    // reset counters and substring
                    numWordsInString = 0;
                    newDocString = "";
                    subDocCount++;
                }
                if (word.length() > 1) {
                    newDocString += word + " ";
                    numWordsInString++;
                }
            }
            if(numWordsInString > 0){
                // add the final part of the PDF
                DocIOWrapper newDoc = new DocIOWrapper(Integer.toString(docCount), docCount);
                newDoc.addData("originalFileName", rootName);
                newDoc.addData("splitNumber", String.format("%03d", subDocCount));
                newDoc.addData("dataset", dataset);
                newDoc.addData("fileName", rootName + "_" + String.format("%03d", subDocCount));
                long startPos = ((subDocCount) * wordsPerDoc);
                newDoc.addData("wordRange", startPos + " - " + (startPos + numWordsInString));
                newDoc.addData("pageRange", String.valueOf(returnPageNumbers(newDocString)));
                newDoc.addData("text", removePageNumber(newDocString.trim()).trim());
                Docs.put(newDoc.getId(), newDoc);
                docCount++;
            }
            document.close();
        } catch (Exception e) {
            LogPrint.printNoteError("Error while parsing PDF.");
            e.printStackTrace();
            System.exit(1);
        } finally {
            LogPrint.printCompleteStep();
            LogPrint.printNote("Number of sub-documents recovered: " + subDocCount+1, 1);
        }
    }

    /**
     * Method parsing a PDF document page by page and inserting page numbers info ([PAGENUMBER_...)
     * in the text String to later save the page data in the JSON corpus file.
     * @param document PDF document to parse.
     * @return PDF text String with inserted page numbers.
     * @throws IOException If there is an error while parsing the document.
     */
    private String addPageNumebr(PDDocument document) throws IOException {
        int numberOfPages = document.getNumberOfPages();
        StringBuilder text = new StringBuilder();
        for (int pageNumber = 1 ; pageNumber < numberOfPages; pageNumber++) {
            PDFTextStripper s = new PDFTextStripper();
            s.setStartPage(pageNumber);
            s.setEndPage(pageNumber);
            String[] contentsOfPage = s.getText(document).split("\\s+");
            text.append("[PAGENUMBER_").append(pageNumber).append(" ");
            for(String word : contentsOfPage) {
                text.append(word).append(" ");
            }
        }
        return text.toString();
    }

    /**
     * Method returning the page range of a PDF text String (inserted with addPageNumber).
     * @param text Text to check for page range.
     * @return The page range.
     */
    private List<Integer> returnPageNumbers(String text){

        List<Integer> pagesRanges = new ArrayList<Integer>();
        // add pagenumbers
        for(String word : text.split("\\s+")) {
            if(word.contains("[PAGENUMBER_")){
                pagesRanges.add(Integer.valueOf(word.substring(12)));
            }
        }

        if(!(text.split(" ")[0].contains("[PAGENUMBER_")) && (!pagesRanges.isEmpty()))
            pagesRanges.add(pagesRanges.get(0) -1 );

        Collections.sort(pagesRanges);

        return pagesRanges;
    }

    /**
     * Method removing the page number information (inserted with addPageNumber) from a PDF text String.
     * @param text Text to remove the page number information from.
     * @return Cleaned text.
     */
    private String removePageNumber(String text){
        String outtext = "";
        for(String word : text.split("\\s+")) {
            if(!(word.contentEquals("[PAGENUMBER_")))
                outtext += word + " ";
        }
        return outtext;
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
