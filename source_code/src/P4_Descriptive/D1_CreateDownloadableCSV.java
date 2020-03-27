package P4_Descriptive;

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
 * Created by Tom on 25/04/2018.
 */
public class D1_CreateDownloadableCSV
{
    private JSONIOWrapper jWrapper;
    private ConcurrentHashMap<String, DocumentRow> JSONRows;
    //Contains the ID of any row that failed, and the reason it failed!
    private ConcurrentHashMap<String, String> failedRetrievals;
    private List<List<WordData>> wordsAndWeights;

    private String inputFilename, outputFilename;
    private String[] columns;
    private int numWords = 3;
    private boolean alphabeticalWords = false;

    private List<Integer> modelLookup = new ArrayList<>();

    /**
     * This is an optional part of the pipeline which allows you to create a summary CSV of the topic modelling results.
     * It's the format we often use on the Strategic Futures website, with some customisation options.
     * In particular you can pick which columns appear in the CSV file. The CSV will be in the following format:
     *
     * DocumentID, [USER COLUMNS], WordCount, TextUsed, IncludedInTopicModel, ReasonForRemoval, [TOPIC DISTRIBUTIONS]
     *
     * You can specify as many user columns as you like, referencing the fields set back in the P1_Input stage, and as
     * long as they exist, they will be included. Note that, for obvious reasons, the topic distributions WILL NOT
     * appear for any row in the CSV which was not included in the topic modelling.
     *
     * @param args - [JSON Input Location] [CSV Output Location] [Num Topic Words] [Alphabetic Topic Words] [Column For Output 1] [Column For Output 2] ...
     */
    public static void main (String[] args)
    {
        System.out.println( "\n********************************************\n" +
                            "*                                          *\n" +
                            "* STARTING D1_CreateDownloadableCSV PHASE! *\n" +
                            "*                                          *\n" +
                            "* D1_CreateDownloadableCSV:START           *\n" +
                            "*                                          * \n" +
                            "********************************************\n");

        D1_CreateDownloadableCSV startClass = new D1_CreateDownloadableCSV();
        startClass.CreateCSV(args);

        System.out.println( "\n********************************************\n" +
                            "*                                          *\n" +
                            "* D1_CreateDownloadableCSV PHASE COMPLETE! *\n" +
                            "*                                          *\n" +
                            "* D1_CreateDownloadableCSV:END             *\n" +
                            "*                                          * \n" +
                            "********************************************\n");
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
            System.out.println("\nArguments missing!\n\nPlease supply arguments in the following order: [JSON Input Location] [CSV Output Location] [Num Topic Words] [Alphabetic Topic Words] [Column For Output 1] [Column For Output 2] ...");
            System.exit(1);
        }
        else if (args.length < 5)
        {
            System.out.println("\nArguments missing! You must include at least one column to add to the CSV!\n\nPlease supply arguments in the following order: [JSON Input Location] [CSV Output Location] [Num Topic Words] [Alphabetic Topic Words] [Column For Output 1] [Column For Output 2] ...");
            System.exit(1);
        }
        else
        {
            inputFilename = args[0];
            outputFilename = args[1];

            try
            {
                numWords = Integer.parseInt(args[2]);
                alphabeticalWords = Boolean.parseBoolean(args[3]);
            }
            catch (NumberFormatException e)
            {
                System.out.println("\nThe third argument must be a valid integer! You must include at least one column to add to the CSV!\n\nPlease supply arguments in the following order: [JSON Input Location] [CSV Output Location] [Num Topic Words] [Alphabetic Topic Words] [Column For Output 1] [Column For Output 2] ...");
                System.exit(1);
            }

            columns = new String[args.length - 4];
            System.arraycopy(args, 4, columns, 0, columns.length);
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
    }

    /**
     * Creates a look up array which we'll use for reordering the topics into alphabetical order later, if required.
     */
    private void CreateInitialModelLookupArray()
    {
        int numTopics = Integer.parseInt(jWrapper.GetMetadata().get("numTopics"));

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
        System.out.println("\n**********\nSaving CSV File!\n**********\n");
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

                //Add optional fields, as defined by input args, skipping any which don't exist in the original data
                for (String column : columns)
                {
                    if (fields.containsKey(column))
                    {
                        csvAppender.appendField(doc.getValue(column));
                    }
                }

                //Add required WordCount and TextUsed columns
                csvAppender.appendField(String.valueOf(doc.getNumLemmas()));

                String lemmaString = doc.getLemmaStringData();

                if(lemmaString.length() > 32000)
                    lemmaString = lemmaString.substring(0, 31997) + "...";

                csvAppender.appendField(lemmaString);

                //Add a column saying whether the document was included in the topic model...
                csvAppender.appendField(String.valueOf(doc.isIncludedInModel()));

                //And add a reason if not. Either it was a failed retrieval, which is included in the JSON...
                if(failedRetrievals.containsKey(row.getKey()))
                {
                    csvAppender.appendField(failedRetrievals.get(row.getKey()));
                    //System.out.println("**********\nSKIPPING ROW - The row with ID " + row.getKey() + " was not fully retrieved. Skipped adding it to the csv!.\n**********\n");
                }
                //Or if it wasn't a failed retrieval, then we'll grab the reason for its removal which will have been set earlier in the pipeline!
                else if(!doc.isIncludedInModel())
                    csvAppender.appendField(doc.getRemovalReason());
                else
                    csvAppender.appendField("");

                //Add topic distributions...
                double[] distribution = doc.getTopicDistribution();
                if(distribution != null)
                {
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

        for (String column : columns)
        {
            //Add optional headers, as defined by input args, skipping any which don't exist in the original data
            if(fields.containsKey(column))
            {
                csvAppender.appendField(column);
            }
            else
                System.out.println("\n**********\nWARNING! COLUMN NOT FOUND!\n**********\nThe requested column '" + column + "' was not found in the JSON passed. Skipping.\n");
        }

        //Add required WordCount and TextUsed headers
        csvAppender.appendField("WordCount");
        csvAppender.appendField("TextUsed");
        csvAppender.appendField("IncludedInTopicModel");
        csvAppender.appendField("ReasonForRemoval");

        List<String> labelStrings = new ArrayList<>();

        for(int topic = 0; topic < wordsAndWeights.size(); topic++)
        {
            String labelText = "";
            for(int word = 0; word < numWords; word++)
            {
                labelText += wordsAndWeights.get(topic).get(word).label;

                if(word < numWords - 1)
                    labelText += "-";
            }

            if(alphabeticalWords)
            {
                //Iterate through the list until we find a string we are lexicographically before.
                //At that stage, break, so the 'i' value will represent the necessary location of insertion.
                int i;
                for(i = 0; i < labelStrings.size(); i++)
                {
                    if(labelStrings.get(i).compareTo(labelText) >= 0)
                        break;
                }
                //Insert string and insert original locations so we can use it as a look up later.
                labelStrings.add(i, labelText);
                modelLookup.add(i, topic);
            }
            else
            {
                labelStrings.add(labelText);
            }
        }

        for(int i = 0; i < labelStrings.size(); i++)
            csvAppender.appendField(labelStrings.get(i));

        csvAppender.endLine();
    }
}
