package P2_Lemmatise;

import PX_Helper.DocumentRow;
import PX_Helper.JSONIOWrapper;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Tom on 21/05/2018.
 */
public class B1_RemoveStopPhrases
{
    private JSONIOWrapper jWrapper;
    private ConcurrentHashMap<String, DocumentRow> JSONRows = new ConcurrentHashMap<>();
    //Contains the ID of any row that failed, and the reason it failed!
    private ConcurrentHashMap<String, String> failedRetrievals;

    private String inputFilename, outputFilename, stopPhraseFilename;


    private  List<String> rawDataColumns  = new ArrayList<>();
    private List<String> stopPhrases = new ArrayList<>();
    private List<String> compressedStopPhrases = new ArrayList<>();

    private final static boolean RUN_IN_PARALLEL = false;

    /**
     * This is an OPTIONAL step which can be run before the lemmatisation phase. What this module does is to remove
     * any document from the rest of the pipeline if it contains any 'stop phrase' that is listed in the document
     * provided. This is to help with issues like those in GTR where certain projects have generic stock phrases which
     * pollute the topic model itself.
     *
     * The stop phrase file distributed with the pipeline should have instructions within it, but it should contain
     * a phrase per line.
     *
     * IMPORTANT: Anything containing a stop phrase will BE REMOVED COMPLETELY from the topic modelling.
     *
     * @param args - [JSON Input Location] [JSON Output Location] [Stop Phrase File]
     */
    public static void main (String[] args)
    {
        System.out.println( "\n********************************************\n" +
                            "*                                          *\n" +
                            "* STARTING B1_RemoveStopPhrases PHASE!     *\n" +
                            "*                                          *\n" +
                            "* B1_RemoveStopPhrases:START               *\n" +
                            "*                                          * \n" +
                            "********************************************\n");

        B1_RemoveStopPhrases startClass = new B1_RemoveStopPhrases();
        startClass.RemoveStopPhrases(args);

        System.out.println( "\n********************************************\n" +
                            "*                                          *\n" +
                            "* B1_RemoveStopPhrases PHASE COMPLETE!     *\n" +
                            "*                                          * \n" +
                            "* B1_RemoveStopPhrases:END                 *\n" +
                            "*                                          * \n" +
                            "********************************************\n");
    }

    /**
     * Runs the stages of the process, one after another, to remove any document which has a listed stop phrase in.
     *
     * Importantly, this is where we also capture and create a List of the stop phrases we want to search for, after
     * ensuring at least one has been defined in the checkArgs() function.
     *
     * @param args - The argument list directly from main()
     */
    private void RemoveStopPhrases(String[] args)
    {
        jWrapper = new JSONIOWrapper();
        checkArgs(args);

        for(int i = 3; i < args.length; i++)
        {
            rawDataColumns.add(args[i]);
        }

        LoadJSONFile(inputFilename);
        LoadStopPhrases(stopPhraseFilename);
        CheckRowsForStopPhrases();

        SaveJSONFile(outputFilename);
    }

    /**
     * Here we check we have all the arguments required. In particular, we check that we have arguments for the files
     * we input from and output to, but we also check that we have at least ONE column to check for stop phrases.
     *
     * @param args - The argument list directly from main()
     */
    private void checkArgs(String[] args)
    {
        if(args.length < 3)
        {
            System.out.println("\nArguments missing!\n\nPlease supply arguments in the following order: [JSON Input Location] [JSON Output Location] [Stop Phrase File]\n");
            System.exit(1);
        }
        else if(args.length < 4)
        {
            System.out.println("\nYou must select at least one column to check for Stop Phrases! In general, the columns should match the ones passed to the P2_Lemmatise step.\n\nPlease supply arguments in the following order: [JSON Input Location] [JSON Output Location] [Stop Phrase File] [Column For Lemma Data 1] [Column For Lemma Data 2] ...");
            System.exit(1);
        }
        else
        {
            inputFilename = args[0];
            outputFilename = args[1];
            stopPhraseFilename = args[2];
        }
    }

    /**
     * Loads the information from the JSON file, using the JSONIOWrapper interface
     *
     * @param JSONFile - location of the JSON file to load
     */
    private void LoadJSONFile(String JSONFile)
    {
        jWrapper.LoadJSON(JSONFile);
        JSONRows = jWrapper.GetRowData();
        failedRetrievals = jWrapper.GetFailedRetrievals();
    }

    /**
     * Loads the stop phrases from the file provided. These are 'compressed' so they are easier to compare, by which
     * I mean they have all non-alphanumeric characters removed. The text to be checked will be compressed identically
     * too, so we can do a simple check.
     *
     * @param stopPhraseFilename - the file where the stop phrases are defined.
     */
    private void LoadStopPhrases(String stopPhraseFilename)
    {
        System.out.println("\n**********\nLoading Stop Phrases!\n***********\n");

        try (BufferedReader br = new BufferedReader(new FileReader(stopPhraseFilename)))
        {
            String line;
            while ((line = br.readLine()) != null)
            {
                //Check for comments, and skip any line starting with #
                if(line.charAt(0) != '#')
                {
                    //Save the 'compressed' version of the stop phrase, i.e. one without white space or punctuation
                    compressedStopPhrases.add(line.replaceAll("[\\W]", ""));
                    stopPhrases.add(line);
                }
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
            System.exit(1);
        }

        System.out.println("\n**********\nStop Phrases Loaded! " + compressedStopPhrases.size() + " were found!\n***********\n");
    }

    /**
     * Fires the stop phrase checks in the same way as the GTR_Crawler, by using Java's forEach() structure.
     * Currently, as this works so quickly, we don't run this in parallel as there is no point.
     */
    private void CheckRowsForStopPhrases()
    {
        System.out.println("\n**********\nChecking Rows For Stop Phrases!\n***********\n");
        if(RUN_IN_PARALLEL)
            JSONRows.entrySet().parallelStream().forEach(this::ProcessRow);
        else
            JSONRows.entrySet().forEach(this::ProcessRow);
        System.out.println("\n**********\nStop Phrase Processing Complete!\n***********\n");
    }

    /**
     * This method does the actual processing work, and is passed individual entries to check. First, we concatenate
     * all the values we are supposed to check, we then compress the string in the same way as we did with the stop
     * phrases, and then we check each compressed stop phrase against our compressed text. If we find a match,
     * we set this entry to be excluded from the modelling.
     *
     * @param entry - a single entry to check
     */
    private void ProcessRow(Map.Entry<String, DocumentRow> entry)
    {
        if(failedRetrievals.containsKey(entry.getKey()))
        {
            System.out.println("**********\nSKIPPING ROW - The row with ID " + entry.getKey() + " was not fully retrieved. Stop Phrase Processing skipped.\n**********\n");
        }
        else
        {
            DocumentRow row = entry.getValue();

            //Get a concatenated string containing the columns passed via program arguments
            String rawDataString = "";
            for(int i = 0; i < rawDataColumns.size(); i++)
            {
                if(row.getValue(rawDataColumns.get(i)) != null)
                    rawDataString += " " + row.getValue(rawDataColumns.get(i));
            }

            //Get the 'compressed' version of the text to be lemmatised, i.e. one without white space or punctuation
            String compressedText = rawDataString.replaceAll("[\\W]", "");

            //Got through each stop phrase
            for(int i = 0; i < compressedStopPhrases.size(); i++)
            {
                if(compressedText.contains(compressedStopPhrases.get(i)))
                {
                    row.setIncludedInModel(false, "Document was removed for containing the following Stop Phrase: " + stopPhrases.get(i));
                    return;
                }
            }
        }
    }

    /**
     * Finally we update the JSON file using the JSONIOWrapper interface, and add the number of stop phrases used in
     * to the meta data for future reference as needed.
     *
     * @param JSONFile - the JSON file to save the results to
     */
    private void SaveJSONFile(String JSONFile)
    {
        jWrapper.SetRowData(JSONRows);

        ConcurrentHashMap<String, String> metadata = jWrapper.GetMetadata();
        metadata.put("NumStopPhrasesRemoved", String.valueOf(stopPhrases.size()));

        jWrapper.SetMetadata(metadata);

        jWrapper.SaveJSON(JSONFile);
    }
}
