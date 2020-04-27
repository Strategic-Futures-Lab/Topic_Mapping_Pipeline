package P1_Input;

import P0_Project.InputModuleSpecs;
import PX_Data.DocIOWrapper;
import PX_Data.JSONIOWrapper;
import PY_Helper.Pair;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
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

    public static void PDFInput(InputModuleSpecs inputSpecs){
        System.out.println( "**********************************************************\n" +
                            "* STARTING PDF Input !                                   *\n" +
                            "**********************************************************\n");
        PDFInput startClass = new PDFInput();
        startClass.processSpecs(inputSpecs);
        startClass.FindPDFs();
        startClass.ParsePDFs();
        startClass.OutputJSON();
        System.out.println( "**********************************************************\n" +
                            "* PDF Input: COMPLETE !                                  *\n" +
                            "**********************************************************\n");
    }

    private void processSpecs(InputModuleSpecs inputSpecs){
        sourceDirectory = inputSpecs.source;
        outputFile = inputSpecs.output;
    }

    private void FindPDFs(){
        File directory = new File(sourceDirectory);
        File[] files = directory.listFiles();
        for(File file : files){
            if(!file.isDirectory()){
                fileList.add(new Pair<>(file,directory.getName()));
            } else {
                FindPDFsInSubDirectory(file, file.getName());
            }
        }
        System.out.println("File crawl complete...");
    }

    private void FindPDFsInSubDirectory(File directory, String dataset){
        datasetList.add(dataset);
        File[] files = directory.listFiles();
        for(File file : files){
            if(!file.isDirectory()){
                fileList.add(new Pair<>(file,dataset));
            }
        }
    }

    private void ParsePDFs(){
        if(RUN_IN_PARALLEL)
            fileList.parallelStream().forEach(this::ParsePDF);
            //Non-parallel version:
        else
            fileList.forEach(this::ParsePDF);
    }

    private void ParsePDF(Pair<File,String> pdf){
        File file = pdf.getLeft();
        String dataset = pdf.getRight();
        try {
            String rootName = file.getName();
            rootName = rootName.substring(0, rootName.indexOf('.'));

            PDDocument document = PDDocument.load(file);
            System.out.println("Processing document: " + rootName);
            PDFTextStripper pdfTextStripper = new PDFTextStripper();
            String text = pdfTextStripper.getText(document);

            int docNum = docCount++;
            DocIOWrapper newDoc = new DocIOWrapper(Integer.toString(docNum), docNum);
            newDoc.addData("text", text);
            newDoc.addData("dataset", dataset);
            Docs.put(newDoc.getId(), newDoc);

            document.close();

        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            numDocs = Docs.size();
            System.out.println("Finished!");
            System.out.println("Number of documents parsed: " + numDocs);
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
        JSONIOWrapper.SaveJSON(root, outputFile);
    }

}


// BELOW FROM OLD PIPELINE - USED TO DIVIDE DOCS IN PARSING

// String newDocString = "";
// int numWordsInString = 0;
// int subDocCount = 0;
// for(String word : text.split("\\s+")){
//     // test updated by P. Le Bras to allow non-subdivision of doc if wordsPerDoc = 0 - 16/05/19
//     if(numWordsInString == wordsPerDoc && wordsPerDoc > 0) {
//         newDoc.setValue("RawText", newDocString.trim());
//         newDoc.setValue("OriginalDocument", rootName);
//         newDoc.setValue("DistributionColumn", String.valueOf(0));
//         int startPos = (subDocCount * wordsPerDoc);
//         newDoc.setValue("WordRange", startPos + " - " + (startPos + numWordsInString));
//         /*------------------------------------------------------------*/
//         // Added by P. Le Bras - 03/12/18
//         newDoc.setValue("StartTime", ""+startPos);
//         newDoc.setValue("EndTime", ""+(startPos + numWordsInString));
//         newDoc.setValue("SplitNumber", String.format("%03d", subDocCount));
//         newDoc.setValue("Dataset",dataset);
//         /*------------------------------------------------------------*/
//         newDoc.setIncludedInModel(true);
//         newDoc.setID(rootName + "_" + String.format("%03d", subDocCount));
//
//         JSONRows.put(newDoc.getID(), newDoc);
//
//         newDoc = new DocumentRow();
//         numWordsInString = 0;
//         newDocString = "";
//         subDocCount++;
//     }
//
//     if(word.length() > 1)  {
//         newDocString += word + " ";
//         numWordsInString++;
//     }
// }

// //Make sure to add the final part too, whatever words are left over!
// newDoc.setValue("RawText", newDocString.trim());
// newDoc.setValue("OriginalDocument", rootName);
// newDoc.setValue("DistributionColumn", String.valueOf(0));
// int startPos = (subDocCount * wordsPerDoc);
// newDoc.setValue("WordRange", startPos + " - " + (startPos + numWordsInString));
// /*------------------------------------------------------------*/
// // Added by P. Le Bras - 03/12/18
// newDoc.setValue("StartTime", ""+startPos);
// newDoc.setValue("EndTime", ""+(startPos + numWordsInString));
// newDoc.setValue("SplitNumber", String.format("%03d", subDocCount));
// newDoc.setValue("Dataset",dataset);
// /*------------------------------------------------------------*/
// newDoc.setIncludedInModel(true);
// newDoc.setID(rootName + "_" + String.format("%03d", subDocCount));
//
// JSONRows.put(newDoc.getID(), newDoc);
