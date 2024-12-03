package IO;


import org.jbibtex.*;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.List;
import java.util.Map;

/**
 * Helper class for reading and writing BibTex files
 *
 * @author P. Le Bras
 * @version 1
 */
public class BIBHelper {

    /**
     * Interface for BibTex entry processor methods
     */
    public interface ProcessBIBEntry{
        void processEntry(Key key, BibTeXEntry entry, int entryNum) throws ParseException;
    }

    /**
     * Reads a BibTex file and executes the provided processor method on each entry
     * @param filename BibTex file name
     * @param entryProcessor Processor method
     * @return The number of entries processed
     * @throws FileNotFoundException If the file does not exist
     * @throws ParseException If the BibTex file is badly formatted
     */
    public static int loadBIBFile(String filename, ProcessBIBEntry entryProcessor) throws FileNotFoundException, ParseException {
        return loadBIBFile(filename, entryProcessor, 0);
    }

    /**
     * Reads a BibTex file and executes the provided processor method on each entry
     * @param filename BibTex file name
     * @param entryProcessor Processor method
     * @param depth Change access modifier
     * @return The number of entries processed
     * @throws FileNotFoundException If the file does not exist
     * @throws ParseException If the BibTex file is badly formatted
     */
    public static int loadBIBFile(String filename, ProcessBIBEntry entryProcessor, int depth) throws FileNotFoundException, ParseException {
        Reader fileReader = new FileReader(filename);
        BibTeXParser bibTeXParser = new BibTeXParser();
        int entryNum = 0;
        Console.log("Reading BibTex: "+filename, depth);
        BibTeXDatabase db = bibTeXParser.parseFully(fileReader);
        if(!bibTeXParser.getExceptions().isEmpty()) Console.warning(bibTeXParser.getExceptions().size()+" entries skipped due to errors", depth+1);
        Map<Key, BibTeXEntry> bibEntries = db.getEntries();
        for(Map.Entry<Key, BibTeXEntry> bibEntry: bibEntries.entrySet()){
            entryProcessor.processEntry(bibEntry.getKey(),bibEntry.getValue(),entryNum);
            entryNum++;
        }
        return entryNum;
    }

    /**
     * Helper method for retrieving the text value of a BibTex field
     * @param entry BibTex entry to query
     * @param fieldName Field name to retrieve
     * @return String value associated with the field name
     */
    public static String getField(BibTeXEntry entry, String fieldName) throws ParseException {
        Value val = entry.getField(new Key(fieldName));
        if(val == null){
            return "";
        } else {
            String stringVal = val.toUserString();
            if (stringVal.indexOf('\\') > -1 || stringVal.indexOf('{') > -1) {
                LaTeXParser latexParser = new LaTeXParser();
                List<LaTeXObject> latexObjects = latexParser.parse(stringVal);
                LaTeXPrinter laTeXPrinter = new LaTeXPrinter();
                return laTeXPrinter.print(latexObjects);
            } else {
                return stringVal;
            }
        }
    }
}
