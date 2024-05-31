package corpus;

import IO.Console;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Super class of corpus module used for cleaning text (lemmatise, stop words, etc.)
 *
 * @author P. Le Bras
 * @version 1
 */
public class CleaningModule extends CorpusModule {

    // reads a txt file and extracts elements for stop phrases, stop words, etc.
    protected List<String> readTextFile(String filename, String type) {
        Console.log("Loading "+type+" from "+filename);
        List<String> elements  = new ArrayList<>();
        File file = new File(filename);
        try{
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while((line = reader.readLine()) != null){
                // skip comment lines
                if(!line.startsWith("//") && !line.startsWith("#")) elements.add(line);
            }
        } catch (Exception e) {
            Console.error("Could not read "+file.getName()+" successfully - returning empty list of "+type, 1);
            return new ArrayList<>();
        }
        Console.tick();
        Console.note("Found "+elements.size()+" "+type, 1);
        return elements;
    }
}
