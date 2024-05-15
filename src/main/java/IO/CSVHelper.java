package IO;

import de.siegmar.fastcsv.reader.CsvParser;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRow;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Helper class for reading and writing CSV files
 *
 * @author P. Le Bras
 * @version 1
 */
public class CSVHelper {

    private final static int PROCESS_MAX_ROWS = Integer.MAX_VALUE;

    /**
     * Interface for CSV row processor methods
     */
    public interface ProcessCSVRow{
        void processRow(CsvRow row);
    }

    /**
     * Reads a CSV file and executes the provided processor method on each row; will treat the first row as header
     * @param filename CSV file name
     * @param rowProcessor Processor method
     * @return The number of rows processed
     * @throws IOException If the file does not exist
     */
    public static int loadCSVFile(String filename, ProcessCSVRow rowProcessor) throws IOException {
        return loadCSVFile(filename, rowProcessor, true, 0);
    }

    /**
     * Reads a CSV file and executes the provided processor method on each row; will treat the first row as header
     * @param filename CSV file name
     * @param rowProcessor Processor method
     * @param depth Depth level for logs
     * @return The number of rows processed
     * @throws IOException If the file does not exist
     */
    public static int loadCSVFile(String filename, ProcessCSVRow rowProcessor, int depth) throws IOException {
        return loadCSVFile(filename, rowProcessor, true, depth);
    }

    /**
     * Reads a CSV file and executes the provided processor method on each row
     * @param filename CSV file name
     * @param rowProcessor Processor method
     * @param headers Flag for whether the first row should be interpreted as a headers
     * @return The number of rows processed
     * @throws IOException If the file does not exist
     */
    public static int loadCSVFile(String filename, ProcessCSVRow rowProcessor, boolean headers) throws IOException {
        return loadCSVFile(filename, rowProcessor, headers, 0);
    }

    /**
     * Reads a CSV file and executes the provided processor method on each row
     * @param filename CSV file name
     * @param rowProcessor Processor method
     * @param headers Flag for whether the first row should be interpreted as a headers
     * @param depth Depth level for logs
     * @return The number of rows processed
     * @throws IOException If the file does not exist
     */
    public static int loadCSVFile(String filename, ProcessCSVRow rowProcessor, boolean headers, int depth) throws IOException {
        File file = new File(filename);
        CsvReader reader = new CsvReader();
        reader.setContainsHeader(headers);
        int rowNum = 0;
        Console.log("Reading CSV: "+filename, depth);
        CsvParser parser = reader.parse(file, StandardCharsets.UTF_8);
        CsvRow row;
        while((row = parser.nextRow()) != null && rowNum < PROCESS_MAX_ROWS){
            rowProcessor.processRow(row);
            rowNum++;
        }
        return rowNum;
    }
}
