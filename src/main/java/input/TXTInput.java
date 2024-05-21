package input;

import IO.Console;
import IO.Timer;
import config.ModuleConfig;
import config.ProjectConfig;
import config.modules.InputConfigTXT;
import data.Pair;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

/**
 * Module generating a corpus from a TXT file or directory of TXT files
 *
 * @author P. Le Bras
 * @version 2
 */
public class TXTInput extends FileInput {

    // module parameters
    private boolean splitEmptyLines;

    // Flag for processing TXTs in parallel (may affect order of documents)
    private final static boolean RUN_IN_PARALLEL = true;

    /**
     * Main module method - processes parameters, reads TXT file/folder and writes JSON corpus
     * @param moduleParameters module parameters
     * @param projectParameters project meta parameters
     * @throws IOException If the file(s) type is unexpected
     */
    public static void run(ModuleConfig moduleParameters, ProjectConfig projectParameters) throws IOException {
        String MODULE_NAME = moduleParameters.moduleName+" ("+moduleParameters.moduleType+")";
        Console.moduleStart(MODULE_NAME);
        Timer.start(MODULE_NAME);
        TXTInput instance = new TXTInput();
        instance.processParameters((InputConfigTXT) moduleParameters, projectParameters);
        instance.extension = ".txt";
        try {
            instance.findFiles();
            instance.loadFiles(instance::loadTXT, RUN_IN_PARALLEL);
            instance.writeJSON();
        } catch (Exception e) {
            Console.moduleFail(MODULE_NAME);
            throw e;
        }
        Console.moduleComplete(MODULE_NAME);
        Timer.stop(MODULE_NAME);
    }

    // processes project and module parameters
    private void processParameters(InputConfigTXT moduleParameters, ProjectConfig projectParameters){
        Console.log("Processing parameters");
        source = projectParameters.sourceDirectory+moduleParameters.source;
        outputFile = projectParameters.dataDirectory+moduleParameters.output;
        splitEmptyLines = moduleParameters.splitEmptyLines;
        Console.tick();
        Console.info("Reading TXT input from "+source+" and saving to "+outputFile, 1);
    }

    // process one .txt file
    private void loadTXT(Pair<File, String> txt){
        File file = txt.getLeft();
        String directory = txt.getRight();
        String rootName = file.getName().substring(0, file.getName().lastIndexOf('.'));
        try {
            // load file
            BufferedReader reader = new BufferedReader(new FileReader(file));
            // initialise subtext string
            String newDocString = "";
            String line;
            int subDocCount = 0;
            HashMap<String, String> docFields = new HashMap<>();
            // read the text line by line
            while ((line = reader.readLine()) != null){
                if(splitEmptyLines && line.isEmpty() && !newDocString.isEmpty()){
                    // if it's an empty line and we need to split
                    docFields.put("text", newDocString);
                    docFields.put("folder", directory);
                    docFields.put("file", rootName);
                    docFields.put("subDoc", Integer.toString(subDocCount));
                    addDocument(docFields);
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
                    docFields.put("text", newDocString);
                    docFields.put("folder", directory);
                    docFields.put("file", rootName);
                    addDocument(docFields);
                } else {
                    // the document has been split
                    docFields.put("text", newDocString);
                    docFields.put("folder", directory);
                    docFields.put("file", rootName);
                    docFields.put("subDoc", Integer.toString(subDocCount));
                    addDocument(docFields);
                    subDocCount++;
                }
            }
            Console.log(file.getName()+" read successfully"+(subDocCount>0?" ("+subDocCount+" sub-documents)":""), 1);
        } catch (Exception e) {
            Console.error("Could not read "+file.getName()+" - skipping", 1);
        }
    }
}
