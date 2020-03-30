package PZ_ScottishEnterprise;

import PX_Helper.DocumentRow;
import PX_Helper.JSONIOWrapper;
import PY_TopicModelCore.WordData;
import de.siegmar.fastcsv.writer.CsvAppender;
import de.siegmar.fastcsv.writer.CsvWriter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created Azimeh 12/07/2019.
 */
public class D4_CreateDownloadableCSV_For_SuperSubModelAssignment
{
    private JSONIOWrapper jWrapper;
    private ConcurrentHashMap<String, DocumentRow> JSONRows;
    //Contains the ID of any row that failed, and the reason it failed!
    private ConcurrentHashMap<String, String> failedRetrievals;
    private List<List<WordData>> wordsAndWeights;

    private String inputFilename, outputFilename;
    private int numTopics;
    private String[] columns;
    private int numWords = 3;
    private boolean alphabeticalWords = false;

    private List<Integer> modelLookup = new ArrayList<>();

    /**
     * This is an optional part of the pipeline which allows you to create a CSV of the document-topic distribution.
     * this is created for ScottishEnterprise project and the output of this class will be used in the Super-Sub models assignment...
     *
     * The CSV will be in the following format:
     *
     * DocumentID, WordCount, [TOPIC DISTRIBUTIONS]
     *
     *
     * @param args - [JSON Input Location] [CSV Output Location]
     */
    public static void main (String[] args)
    {
        System.out.println( "\n**********************************************************************\n" +
                            "*                                                                      *\n" +
                            "* STARTING D4_CreateDownloadableCSV_For_SuperSubModelAssignment PHASE! *\n" +
                            "*                                                                      *\n" +
                            "* D4_CreateDownloadableCSV_For_SuperSubModelAssignment:START           *\n" +
                            "*                                                                      * \n" +
                            "************************************************************************\n");

        D4_CreateDownloadableCSV_For_SuperSubModelAssignment startClass = new D4_CreateDownloadableCSV_For_SuperSubModelAssignment();
        startClass.CreateCSV(args);

        System.out.println( "\n**********************************************************************\n" +
                            "*                                                                      *\n" +
                            "* D4_CreateDownloadableCSV_For_SuperSubModelAssignment PHASE COMPLETE! *\n" +
                            "*                                                                      *\n" +
                            "* D4_CreateDownloadableCSV_For_SuperSubModelAssignment:END             *\n" +
                            "*                                                                      * \n" +
                            "************************************************************************\n");
    }

    /**
     * The main class that sets off the CSV creation process. We don't do anything special here, as everything is out-
     * sourced to it's own method.
     *
     * @param args - The argument list directly from main()
     */
    private void CreateCSV(String[] args)
    {
        checkArgs(args);
        LoadJSON(inputFilename);
        CreateInitialModelLookupArray();
        OutputCSVFile(outputFilename);
        //SaveJSON(outputFilename);
    }

    /**
     * Here we ensure we have all the arguments we require to create the CSV. These include the input and output
     * location, the number of words to use for labelling each topic and whether to order the topics alphabetically, or
     * in the order from the topic modeller. In addition, currently you have to include one column to add to the CSV,
     * although this might be subject to change in the future.
     *
     * @param args - The argument list directly from main()
     */
    private void checkArgs(String[] args)
    {
        if(args.length < 2)
        {
            System.out.println("\nArguments missing!\n\nPlease supply arguments in the following order: [JSON Input Location] [CSV Output Location]");
            System.exit(1);
        }
        else
        {
            inputFilename = args[0];
            outputFilename = args[1];
        }
    }

    /**
     * Loads the information from the JSON file, using the JSONIOWrapper interface
     *
     * @param JSONFile - location of the JSON file to load
     */
    private void LoadJSON(String JSONFile)
    {
        jWrapper = new JSONIOWrapper();
        jWrapper.LoadJSON(JSONFile);
        JSONRows = jWrapper.GetRowData();
        failedRetrievals = jWrapper.GetFailedRetrievals();
        wordsAndWeights = jWrapper.GetTopicWords();
      //  numTopics = 1000;
        numTopics = Integer.parseInt(jWrapper.GetMetadata().get("numTopics"));
    }

    /**
     * Creates a look up array which we'll use for reordering the topics into alphabetical order later, if required.
     */
    private void CreateInitialModelLookupArray()
    {
        for(int i = 0; i < numTopics; i++)
            modelLookup.add(i);
    }

    /**
     * Create the CSV structure here, before outputting it to file. The column structure of this file was defined at
     * the top of the file, so won't be repeated here. Each document or input file will get a row, whether it appeared
     * in the topic model or not, for transparency reasons!
     *
     * @param CSVFile
     */
    private void OutputCSVFile(String CSVFile)
    {
        System.out.println("\n**********\nSaving CSV File!\n**********\n" + numTopics);
        Map.Entry<String, DocumentRow> field = JSONRows.entrySet().iterator().next();
        HashMap<String, String> fields = field.getValue().getValues();

        File file = new File(CSVFile);
        CsvWriter csvWriter = new CsvWriter();
        csvWriter.setAlwaysDelimitText(true);

        try (CsvAppender csvAppender = csvWriter.append(file, StandardCharsets.UTF_8))
        {
            CreateHeaderRow(fields, csvAppender);

            //Iterate through each document...
            for(Map.Entry<String, DocumentRow> row : JSONRows.entrySet())
            {
                //Get the document...
                DocumentRow doc = row.getValue();

                //Add REQUIRED ID...
                csvAppender.appendField(doc.getID());

                //Add topic distributions...
                double[] distribution = doc.getTopicDistribution();
                if(distribution != null)
                {
                    System.out.println(distribution.length);
                    for (int i = 0; i < distribution.length; i++)
                        csvAppender.appendField(String.valueOf(distribution[modelLookup.get(i)]));
                }

                csvAppender.endLine();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        System.out.println("\n**********\nCSV file saved!\n**********\n");
    }

    /**
     * Create the header row for the top line of the CSV. Importantly this is where we update the modelLookup structure
     * if we are putting the topics alphabetically, so that we use it both to look up the topic labels, and to reorder
     * the distributions for each row.
     *
     * @param fields - a list of fields, usually the first returned in the ConcurrentHashMap JSONRows structure.
     *                 Should contain the 'optional' fields set in P1_Input
     * @param csvAppender - the appender used for the rest of the CSV, so we can add the header row to it.
     * @throws IOException
     */
    private void CreateHeaderRow(HashMap<String, String> fields, CsvAppender csvAppender) throws IOException
    {
        //Write the header row, starting with the REQUIRED ID field
        csvAppender.appendField("DocumentID");

        for(int topic = 0; topic < wordsAndWeights.size(); topic++)
        {
            csvAppender.appendField(String.valueOf(topic));
        }

        csvAppender.endLine();
    }
}
