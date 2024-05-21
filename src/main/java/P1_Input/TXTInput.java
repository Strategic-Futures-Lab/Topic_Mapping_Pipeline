package P1_Input;

import P0_Project.InputModuleSpecs;
import PX_Data.DocIOWrapper;
import PX_Data.JSONIOWrapper;
import PY_Helper.LogPrint;
import PY_Helper.Pair;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class reading a TXT directory input and writing it as a corpus JSON file.
 *
 * @author P. Le Bras
 * @version 1
 * @deprecated
 */
@Deprecated
public class TXTInput {

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
     */
    private final static boolean RUN_IN_PARALLEL = false;

    // project specs
    /** PDF source directory name. */
    private String sourceDirectory;
    /** File name for the produced JSON corpus. */
    private String outputFile;
    /** Number of words limit before splitting a document. */
    private int wordsPerDoc;
    /** Flag for splitting documents using empty lines in the TXT inputs. */
    private boolean splitEmptyLines;

    /**
     * Main method, reads the specification and launches the sub-methods in order.
     * @param inputSpecs Specifications.
     * @return String indicating the time taken to read the CSV inout file and produce the JSON corpus file.
     */
    public static String TXTInput(InputModuleSpecs inputSpecs){

        LogPrint.printModuleStart("TXT Input");

        long startTime = System.currentTimeMillis();

        TXTInput startClass = new TXTInput();
        startClass.ProcessArguments(inputSpecs);
        startClass.FindTXTs();
        startClass.ParseTXTs();
        startClass.OutputJSON();

        long timeTaken = (System.currentTimeMillis() - startTime) / (long)1000;

        LogPrint.printModuleEnd("TXT Input");
        return "TXT Input: " + Math.floorDiv(timeTaken, 60) + " m, " + timeTaken % 60 + " s.";
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
        splitEmptyLines = inputSpecs.TXT_splitEmptyLines;
        LogPrint.printCompleteStep();
    }

    /**
     * Method exploring the source directory, and it's sub-directories, for all TXT files.
     */
    private void FindTXTs(){
        LogPrint.printNewStep("Finding all TXTs in "+sourceDirectory, 0);
        File directory = new File(sourceDirectory);
        if(!directory.isDirectory()){
            // source is not a directory, perhaps a txt file
            if(directory.getName().toLowerCase().endsWith(".txt")) {
                // source is a txt file, just add this one
                fileList.add(new Pair<>(directory, directory.getName()));
                LogPrint.printCompleteStep();
            } else {
                // source is not a txt file, throw error
                LogPrint.printNoteError("Error, provided source is neither a directory nor a .txt file.");
                System.exit(1);
            }
        } else {
            findTXTsInDirectory(directory);
            if(fileList.size() > 0){
                LogPrint.printCompleteStep();
                LogPrint.printNote("Found "+fileList.size()+" TXT files.", 0);
            } else {
                // did not find any txt file in the directory
                LogPrint.printNoteError("Error, provided directory source does not contain .txt files.");
                System.exit(1);
            }
        }
    }

    /**
     * Method recursively exploring a directory for TXT files.
     * @param directory Directory to explore.
     */
    private void findTXTsInDirectory(File directory){
        for(File file : directory.listFiles()){
            if(!file.isDirectory()){
                if(file.getName().toLowerCase().endsWith(".txt"))
                    fileList.add(new Pair<>(file,directory.getName()));
            } else {
                findTXTsInDirectory(file);
            }
        }
    }

    /**
     * Method launching the TXT parsing process.
     */
    private void ParseTXTs(){
        LogPrint.printNewStep("Parsing TXT files:", 0);
        if(RUN_IN_PARALLEL){
            fileList.parallelStream().forEach(this::parseTXT);
        } else{
            fileList.forEach(this::parseTXT);
        }
        numDocs = Docs.size();
        LogPrint.printNote("Number of documents parsed: " + numDocs, 0);
    }

    /**
     * Method parsing a single TXT file.
     * @param txt TXT file to parse, paired with it's dataset value.
     */
    private void parseTXT(Pair<File, String> txt){
        File file = txt.getLeft();
        String dataset = txt.getRight();
        String rootName = file.getName();
        LogPrint.printNewStep("Processing: "+rootName, 1);
        rootName = rootName.substring(0, rootName.indexOf('.'));
        int subDocCount = 0;
        try {
            // load file
            BufferedReader reader = new BufferedReader(new FileReader(file));
            // initialise subtext string and counters
            int numWordsInString = 0;
            String newDocString = "";
            String line;
            // read the text line by line
            while ((line = reader.readLine()) != null){
                if(splitEmptyLines && line.length() == 0){
                    // if it's an empty line and we need to split
                    addNewDoc(newDocString, rootName, dataset, subDocCount);
                    numWordsInString = 0;
                    subDocCount++;
                    newDocString = "";
                } else {
                    // otherwise go through the line word by word
                    for(String word: line.split("\\s+")){
                        if(numWordsInString == wordsPerDoc && wordsPerDoc > 0){
                            // if we've reach the maximum number of words per document, and it's a positive limit
                            addNewDoc(newDocString, rootName, dataset, subDocCount);
                            numWordsInString = 0;
                            subDocCount++;
                            newDocString = "";
                        }
                        if(word.length() >= 1){
                            // append the new word to the string
                            newDocString += word + " ";
                            numWordsInString++;
                        }
                    }
                }
            }
            if(numWordsInString > 0){
                // if we have some words leftover
                if(subDocCount == 0){
                    // the document was never split
                    addNewDoc(newDocString, rootName, dataset);
                } else {
                    // the document has been split
                    addNewDoc(newDocString, rootName, dataset, subDocCount);
                }

            }
        } catch (Exception e) {
            LogPrint.printNoteError("Error while reading TXT.");
            e.printStackTrace();
            System.exit(1);
        } finally {
            LogPrint.printCompleteStep();
            if(subDocCount > 0){
                LogPrint.printNote("Number of sub-documents recovered: " + subDocCount+1, 1);
            }
        }
    }

    /**
     * Method adding a new corpus document to our list, which has been split by the parser.
     * @param text Text content of the document.
     * @param rootName Name of the file the document comes from.
     * @param dataset Name of the directory where the document's file comes from.
     * @param subDocNum Sub-document index.
     */
    private void addNewDoc(String text, String rootName, String dataset, int subDocNum){
        DocIOWrapper doc = new DocIOWrapper(Integer.toString(docCount), docCount);
        doc.addData("originalFileName", rootName);
        doc.addData("splitNumber", String.format("%03d", subDocNum));
        doc.addData("dataset", dataset);
        doc.addData("fileName", rootName + "_" + String.format("%03d", subDocNum));
        doc.addData("text", text.trim());
        Docs.put(doc.getId(), doc);
        docCount++;
    }

    /**
     * Method adding a new corpus document to our list, complete.
     * @param text Text content of the document.
     * @param rootName Name of the file the document comes from.
     * @param dataset Name of the directory where the document's file comes from.
     */
    private void addNewDoc(String text, String rootName, String dataset){
        DocIOWrapper doc = new DocIOWrapper(Integer.toString(docCount), docCount);
        doc.addData("fileName", rootName);
        doc.addData("dataset", dataset);
        doc.addData("text", text.trim());
        Docs.put(doc.getId(), doc);
        docCount++;
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
