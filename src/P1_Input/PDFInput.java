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

public class PDFInput {

    private final static int PROCESS_MAX_ROWS = Integer.MAX_VALUE;
    private ConcurrentHashMap<String, DocIOWrapper> Docs = new ConcurrentHashMap<>();
    private int numDocs;
    private int docCount = 0;


    private List<Pair<File,String>> fileList = new ArrayList<>();
    private List<String> datasetList = new ArrayList<>();

    /**
     * WARNING: Running this is parallel will alter the final visualisation due to reordering of processing!
     * For now, this is being set to serial! As of 25/06/2018 it is quick enough that a fix is not forthcoming.
     */
    private final static boolean RUN_IN_PARALLEL = false;

    // project specs
    private String sourceDirectory;
    private String outputFile;
    private Long wordsPerDoc;

    public static void PDFInput(InputModuleSpecs inputSpecs){

        LogPrint.printModuleStart("PDF Input");

        long startTime = System.currentTimeMillis();

        PDFInput startClass = new PDFInput();
        startClass.processSpecs(inputSpecs);
        startClass.FindPDFs();
        startClass.ParsePDFs();
        startClass.OutputJSON();

        long timeTaken = (System.currentTimeMillis() - startTime) / (long)1000;

        LogPrint.printModuleEnd("PDF Input");
        LogPrint.printNote("PDF Input " + Math.floorDiv(timeTaken, 60) + " m, " + timeTaken % 60 + " s.");
    }

    private void processSpecs(InputModuleSpecs inputSpecs){
        LogPrint.printNewStep("Processing arguments", 0);
        sourceDirectory = inputSpecs.source;
        outputFile = inputSpecs.output;
        wordsPerDoc = inputSpecs.wordsPerDoc;
        LogPrint.printCompleteStep();
    }

    private void FindPDFs(){
        LogPrint.printNewStep("Finding all PDFs", 0);
        File directory = new File(sourceDirectory);
        File[] files = directory.listFiles();
        for(File file : files){
            if(!file.isDirectory()){
                if(file.getName().toLowerCase().endsWith(".pdf"))
                    fileList.add(new Pair<>(file,directory.getName()));
            } else {
                FindPDFsInSubDirectory(file, file.getName());
            }
        }

        LogPrint.printNote("Number if files found: \t" + fileList.size());
        LogPrint.printNote("File crawl complete...");
    }

    private void FindPDFsInSubDirectory(File directory, String dataset){
        datasetList.add(dataset);
        File[] files = directory.listFiles();
        for(File file : files){
            if(!file.isDirectory()  ){
                if(file.getName().toLowerCase().endsWith(".pdf"))
                    fileList.add(new Pair<>(file,dataset));
            }else{
                FindPDFsInSubDirectory(file, file.getName());
            }
        }
    }

    private void ParsePDFs(){
        if(RUN_IN_PARALLEL){
            if(wordsPerDoc > 0)
                fileList.parallelStream().forEach(this::ParsePDFDivide);
            else
                fileList.parallelStream().forEach(this::ParsePDF);
        } else{
            if(wordsPerDoc > 0)
                fileList.forEach(this::ParsePDFDivide);
            else
                fileList.forEach(this::ParsePDF);
        }
    }

    /**
     * This function will be called in case of NOT-dividing PDF files into chunks
     */
    private void ParsePDF(Pair<File,String> pdf){
        File file = pdf.getLeft();
        int subDocCount = 0;
        // Divide the doc into chunks of wordsPerDoc
        DocIOWrapper newDoc = new DocIOWrapper(Integer.toString(docCount), docCount);
        try {
            String rootName = file.getName();
            rootName = rootName.substring(0, rootName.indexOf('.'));
            LogPrint.printNote("Processing document: " + rootName);
            //load file and parse text
            PDDocument document = PDDocument.load(file);
            String text = "";

            for (int pageNumber = 1 ; pageNumber < document.getNumberOfPages(); pageNumber++) {
                PDFTextStripper s = new PDFTextStripper();
                s.setStartPage(pageNumber);
                s.setEndPage(pageNumber);
                String[] contentsOfPage = s.getText(document).split("\\s+");
                for(String word : contentsOfPage) {
                    text += word + " ";
                }
            }

            newDoc.addData("fileName", rootName);
            newDoc.addData("text", text.trim());

            Docs.put(newDoc.getId(), newDoc);
            docCount++;

            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            numDocs = Docs.size();
            LogPrint.printCompleteStep();
            LogPrint.printNote("Number of documents recovered so far: " + numDocs);
        }


    }


    private void ParsePDFDivide(Pair<File,String> pdf) {
        File file = pdf.getLeft();
        String dataset = pdf.getRight();

        int subDocCount = 0;
        try {
            String rootName = file.getName();
            rootName = rootName.substring(0, rootName.indexOf('.'));

            //load file and parse text
            PDDocument document = PDDocument.load(file);
            String newDocString = "";
            int numWordsInString = 0;
            subDocCount = 0;

            LogPrint.printNote("Processing document: " + rootName);

            /* Add the "[PAGENUMBER_"+ pageNumber + "]" to the text
             * this will be used to save the page number to json file */
            String text = addPageNumebr(document);

            // Divide the doc into chunks of wordsPerDoc
            DocIOWrapper newDoc = new DocIOWrapper(Integer.toString(docCount), docCount);

            for (String word : text.split("\\s+")) {
                // to allow non-subdivision of doc if wordsPerDoc = 0
                if (numWordsInString == wordsPerDoc && wordsPerDoc > 0) {
                    newDoc.addData("originalFileName", rootName);
                    newDoc.addData("splitNumber", String.format("%03d", subDocCount));
                    newDoc.addData("dataset", dataset);
                    newDoc.addData("fileName", rootName + "_" + String.format("%03d", subDocCount));
                    long startPos = (subDocCount * wordsPerDoc);
                    newDoc.addData("wordRange", startPos + " - " + (startPos + numWordsInString));
                    newDoc.addData("pageRange", String.valueOf(returnPageNumbers(newDocString)));
                    newDoc.addData("text", removePageNumber(newDocString.trim()).trim());

                    numWordsInString = 0;
                    newDocString = "";
                    subDocCount++;

                    Docs.put(newDoc.getId(), newDoc);
                    newDoc = new DocIOWrapper(Integer.toString(docCount), docCount);
                    docCount++;
                }
                if (word.length() > 1) {
                    newDocString += word + " ";
                    numWordsInString++;
                }
            }

            //Make sure to add the final part too, whatever words are left over!
            newDoc.addData("originalFileName", rootName);
            newDoc.addData("splitNumber", String.format("%03d", subDocCount));
            newDoc.addData("dataset", dataset);
            newDoc.addData("fileName", rootName + "_" + String.format("%03d", subDocCount));
            long startPos = ((subDocCount) * wordsPerDoc);
            newDoc.addData("wordRange", startPos + " - " + (startPos + numWordsInString));
            newDoc.addData("pageRange", String.valueOf(returnPageNumbers(newDocString)));
            newDoc.addData("text", removePageNumber(newDocString.trim()).trim());
            Docs.put(newDoc.getId(), newDoc);

            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            numDocs = Docs.size();
            LogPrint.printCompleteStep();
            LogPrint.printNote("Number of documents recovered from current file: " + subDocCount, 1);
            LogPrint.printNote("Number of documents recovered so far: " + numDocs);        }
    }







    private String addPageNumebr(PDDocument document) throws IOException {
        /* Add the "[PAGENUMBER_"+ pageNumber + "]" to the text
         * this will be used to save the page number to json file */
        int numberOfPages = document.getNumberOfPages();
        String text = "";

        for (int pageNumber = 1 ; pageNumber < numberOfPages; pageNumber++) {
            PDFTextStripper s = new PDFTextStripper();
            s.setStartPage(pageNumber);
            s.setEndPage(pageNumber);
            String[] contentsOfPage = s.getText(document).split("\\s+");
            text += "[PAGENUMBER_"+ pageNumber + " ";
            for(String word : contentsOfPage) {
                text += word + " ";
            }
        }

        return text;
    }

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

    private String removePageNumber(String text){
        String outtext = "";
        for(String word : text.split("\\s+")) {
            if(!(word.contentEquals("[PAGENUMBER_")))
                outtext += word + " ";
        }
        return outtext;
    }

    private void OutputJSON(){
        LogPrint.printNote("Saving to JSON file");
        JSONObject root = new JSONObject();
        JSONArray corpus = new JSONArray();
        JSONObject meta = new JSONObject();
        meta.put("totalDocs", numDocs);
        root.put("metadata", meta);
        for(Map.Entry<String, DocIOWrapper> entry: Docs.entrySet()){
            corpus.add(entry.getValue().toJSON());
        }
        root.put("corpus", corpus);
        JSONIOWrapper.SaveJSON(root, outputFile);
    }

}
