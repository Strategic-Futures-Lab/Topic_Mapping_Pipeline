package IO;

import de.siegmar.fastcsv.reader.CsvParser;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRow;
import de.siegmar.fastcsv.writer.CsvAppender;
import de.siegmar.fastcsv.writer.CsvWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

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
        void processRow(CsvRow row, int rowNum);
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
            rowProcessor.processRow(row, rowNum);
            rowNum++;
        }
        return rowNum;
    }

    /**
     * Method writing a CSV file, with headers
     * @param filename Filename of the CSV file to save
     * @param headers List of headers
     * @param rows List of rows
     * @param depth Depth level for logs
     */
    public static void saveCSVFile(String filename, String[] headers, List<String[]> rows, int depth) throws Exception {
        Console.log("Saving "+filename, depth);
        File file = new File(filename);
        file.getParentFile().mkdirs();
        // this will erase the content of the file before appending data to it.
        new FileWriter(file.getPath(), false).close();
        CsvWriter csvWriter = new CsvWriter();
        csvWriter.setAlwaysDelimitText(true);
        CsvAppender csvAppender = csvWriter.append(file, StandardCharsets.UTF_8);
        if(headers != null) {
            for (String h : headers) {
                csvAppender.appendField(h);
            }
            csvAppender.endLine();
        }
        for(String[] row: rows) {
            for(String v: row) {
                csvAppender.appendField(v);
            }
            csvAppender.endLine();
        }
        Console.tick();
    }

    /**
     * Method writing a CSV file
     * @param filename Filename of the CSV file to save
     * @param rows List of rows
     * @param depth Depth level for logs
     */
    public static void saveCSVFile(String filename, List<String[]> rows, int depth) throws Exception{
        saveCSVFile(filename, null, rows, depth);
    }

    /**
     * Method writing a CSV file, with headers
     * @param filename Filename of the CSV file to save
     * @param headers List of headers
     * @param rows List of rows
     */
    public static void saveCSVFile(String filename, String[] headers, List<String[]> rows) throws Exception {
        saveCSVFile(filename, headers, rows, 0);
    }

    /**
     * Method writing a CSV file
     * @param filename Filename of the CSV file to save
     * @param rows List of rows
     */
    public static void saveCSVFile(String filename, List<String[]> rows) throws Exception{
        saveCSVFile(filename, rows, 0);
    }

}
