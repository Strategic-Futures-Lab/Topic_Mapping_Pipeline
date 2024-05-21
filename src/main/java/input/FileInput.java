package input;

import IO.Console;
import data.Document;
import data.Pair;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Superclass for input modules exploring the file system;
 * Contains protected properties/methods for these types of input module
 *
 * @author P. Le Bras
 * @version 1
 */
public abstract class FileInput extends InputModule {

    // list of filename found in a given source
    protected final List<Pair<File, String>> fileList = new ArrayList<>();
    protected int docCount = 0;
    protected String extension;

    // some input modules explore the source and locate all files of a given type
    protected void findFiles() throws IOException {
        Console.log("Locating "+extension+" files");
        File sourceFile = new File(source);
        if(!sourceFile.isDirectory()){
            // source is not a directory, check if it is a txt file
            if(sourceFile.getName().toLowerCase().endsWith(extension)) {
                // source is a txt file, just add this one
                fileList.add(new Pair<>(sourceFile, sourceFile.getName()));
                Console.tick();
            } else {
                // source is not a txt file, throw error
                Console.error("Source is neither a directory nor a "+extension+" file");
                throw new IOException("Unexpected file type");
            }
        } else {
            findFilesInDirectory(sourceFile);
            if(!fileList.isEmpty()){
                Console.tick();
                Console.note("Found "+fileList.size()+" "+extension+" files", 1);
            } else {
                // did not find any txt file in the directory
                Console.warning("Provided directory "+source+" does not contain any "+extension+" files");
            }
        }
    }

    // recursively explores a directory and add all file with given extension to the list of files
    private void findFilesInDirectory(File directory){
        for(File file : Objects.requireNonNull(directory.listFiles())){
            if(!file.isDirectory()){
                if(file.getName().toLowerCase().endsWith(extension))
                    fileList.add(new Pair<>(file,directory.getName()));
            } else {
                findFilesInDirectory(file);
            }
        }
    }

    // launches the loading of files found
    protected void loadFiles(java.util.function.Consumer<Pair<File, String>> action, boolean parallel){
        Console.log("Loading "+extension+" files:");
        if(parallel) fileList.parallelStream().forEach(action);
        else fileList.forEach(action);
        Console.note("Number of documents loaded: "+documents.size());
    }

    protected synchronized void addDocument(HashMap<String, String> docFields){
        Document doc = new Document(Integer.toString(docCount), docCount);
        for(Map.Entry<String, String> entry: docFields.entrySet()){
            doc.addField(entry.getKey(), entry.getValue());
        }
        documents.put(doc.getId(), doc);
        docFields.clear();
        docCount++;
    }
}
