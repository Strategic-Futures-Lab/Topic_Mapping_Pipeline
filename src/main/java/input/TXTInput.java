package input;

import IO.Console;
import IO.Timer;
import config.Project;
import config.modules.CorpusTXT;
import data.Document;
import data.Pair;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Module generating a corpus from a TXT file or directory of TXT files
 *
 * @author P. Le Bras
 * @version 2
 */
public class TXTInput extends InputModule{

    // list of txt files found in a directory (if input is directory)
    private final List<Pair<File, String>> fileList = new ArrayList<>();
    private int docCount = 0;

    // module parameters
    private String source;
    private String outputFile;
    private boolean splitEmptyLines;

    // Flag for processing TXTs in parallel (may affect order of documents)
    private final static boolean RUN_IN_PARALLEL = true;

    /**
     * Main module method - processes parameters, reads TXT file/folder and write JSON corpus
     * @param moduleParameters module parameters
     * @param projectParameters project meta parameters
     * @throws IOException If the file(s) type is unexpected
     */
    public static void run(CorpusTXT moduleParameters, Project projectParameters) throws IOException {
        String MODULE_NAME = moduleParameters.moduleName+" ("+moduleParameters.moduleType+")";
        Console.moduleStart(MODULE_NAME);
        Timer.start(MODULE_NAME);
        TXTInput instance = new TXTInput();
        instance.processParameters(moduleParameters, projectParameters);
        try {
            instance.findTXTs();
            instance.loadTXTs();
            instance.writeJSON(instance.outputFile);
        } catch (Exception e) {
            Console.moduleFail(MODULE_NAME);
            throw e;
        }
        Console.moduleComplete(MODULE_NAME);
        Timer.stop(MODULE_NAME);
    }

    // processes project and module parameters
    private void processParameters(CorpusTXT moduleParameters, Project projectParameters){
        Console.log("Processing parameters");
        source = projectParameters.sourceDirectory+moduleParameters.source;
        outputFile = projectParameters.dataDirectory+moduleParameters.output;
        splitEmptyLines = moduleParameters.splitEmptyLines;
        Console.tick();
        Console.info("Reading TXT input from "+source+" and saving to "+outputFile, 1);
    }

    // explore the source and locate all .txt files in it
    private void findTXTs() throws IOException {
        Console.log("Locating TXT files");
        File sourceFile = new File(source);
        if(!sourceFile.isDirectory()){
            // source is not a directory, check if it is a txt file
            if(sourceFile.getName().toLowerCase().endsWith(".txt")) {
                // source is a txt file, just add this one
                fileList.add(new Pair<>(sourceFile, sourceFile.getName()));
                Console.tick();
            } else {
                // source is not a txt file, throw error
                Console.error("TXT source is neither a directory nor a .txt file");
                throw new IOException("Unexpected file type");
            }
        } else {
            findTXTsInDirectory(sourceFile);
            if(fileList.size() > 0){
                Console.tick();
                Console.note("Found "+fileList.size()+" TXT files", 1);
            } else {
                // did not find any txt file in the directory
                Console.warning("Provided directory "+source+" does not contain any .txt files");
            }
        }
    }

    // recursively explores a directory and add all .txt file to the list of files
    private void findTXTsInDirectory(File directory){
        for(File file : Objects.requireNonNull(directory.listFiles())){
            if(!file.isDirectory()){
                if(file.getName().toLowerCase().endsWith(".txt"))
                    fileList.add(new Pair<>(file,directory.getName()));
            } else {
                findTXTsInDirectory(file);
            }
        }
    }

    // processes the .txt files found
    private void loadTXTs(){
        Console.log("Loading TXT files:");
        if(RUN_IN_PARALLEL) fileList.parallelStream().forEach(this::loadTXT);
        else fileList.forEach(this::loadTXT);
        Console.note("Number of documents loaded from file: "+documents.size());
    }

    // process one .txt file
    private void loadTXT(Pair<File, String> txt){
        File file = txt.getLeft();
        String directory = txt.getRight();
        String rootName = file.getName();
        Console.log("Processing: "+rootName, 1);
        rootName = rootName.substring(0, rootName.lastIndexOf('.'));
        int subDocCount = 0;
        try {
            // load file
            BufferedReader reader = new BufferedReader(new FileReader(file));
            // initialise subtext string
            String newDocString = "";
            String line;
            // read the text line by line
            while ((line = reader.readLine()) != null){
                if(splitEmptyLines && line.length() == 0){
                    // if it's an empty line and we need to split
                    addNewDoc(newDocString, rootName, directory, subDocCount);
                    subDocCount++;
                    newDocString = "";
                } else {
                    // otherwise add the line to the document string
                    newDocString += line + " ";
                }
            }
            if(!newDocString.isEmpty()){
                // some text left to save
                if(subDocCount == 0){
                    // the document was never split
                    addNewDoc(newDocString, rootName, directory);
                } else {
                    // the document has been split
                    addNewDoc(newDocString, rootName, directory, subDocCount);
                }
            }
        } catch (Exception e) {
            Console.error("Could not read text file - skipping");
        } finally {
            Console.tick();
            if(subDocCount > 0){
                Console.note("Number of sub-documents recovered: " + subDocCount+1, 2);
            }
        }
    }

    // register the text recovered from a .txt file as a document
    private void addNewDoc(String text, String rootName, String directory, int subDoc){
        Document doc = new Document(Integer.toString(docCount), docCount);
        doc.addField("file", rootName);
        doc.addField("subDoc", String.format("%03d", subDoc));
        doc.addField("folder", directory);
        doc.addField("text", text.trim());
        documents.put(doc.getId(), doc);
        docCount++;
    }

    private void addNewDoc(String text, String rootName, String directory){
        Document doc = new Document(Integer.toString(docCount), docCount);
        doc.addField("file", rootName);
        doc.addField("folder", directory);
        doc.addField("text", text.trim());
        documents.put(doc.getId(), doc);
        docCount++;
    }
}
