package P2_Lemmatise;

import PX_Helper.JSONIOWrapper;
import PX_Helper.DocumentRow;
import PX_Helper.StanfordLemmatizer;
import PX_Helper.StopWords;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by Tom on 18/04/2018.
 */
public class B2_LemmatiseJSONFile
{
    private ConcurrentHashMap<String, DocumentRow> JSONRows = new ConcurrentHashMap<>();
    private JSONIOWrapper jWrapper;
    private int rowsProcessed = 0, numRows;
    private long startTime;
    private StanfordLemmatizer slem;

    private final static int UPDATE_FREQUENCY = 100;
    private final static boolean RUN_IN_PARALLEL = false;

    private  List<String> rawDataColumns  = new ArrayList<>();

    //Contains the ID of any row that failed, and the reason it failed!
    private ConcurrentHashMap<String, String> failedRetrievals;

    private String inputFilename, outputFilename;
    private int minNumLemmas;
    private boolean removeOriginalText = true;

    /**
     * This is the stage which performs all the lemmatising of the text, before it is passed onto the topic modelling
     * in the next step. In particular, a user specifies which fields they are interested in, these are checked against
     * those created in the P1_Input stage, and if they exist they are concatenated together to create a block of text
     * which is then passed through lemmatisation, using the Stanford NLP library.
     *
     * Once lemmatised, this step fills in the required LemmaStringData field in the JSON file, which the topic modelling
     * will read from in the next step.
     *
     * @param args - [JSON Input Location] [JSON Output Location] [Remove Original Text] [Min Num Lemmas] [Column For Lemma Data 1] [Column For Lemma Data 2] ...
     */
    public static void main (String[] args) throws InterruptedException {
        System.out.println( "\n********************************************\n" +
                            "*                                          *\n" +
                            "* STARTING B2_LemmatiseJSONFile PHASE!     *\n" +
                            "*                                          *\n" +
                            "* B2_LemmatiseJSONFile:START               *\n" +
                            "*                                          * \n" +
                            "********************************************\n");

        B2_LemmatiseJSONFile startClass = new B2_LemmatiseJSONFile();
        startClass.StartLemmatisation(args);

        System.out.println( "\n********************************************\n" +
                            "*                                          *\n" +
                            "* B2_LemmatiseJSONFile PHASE COMPLETE!     *\n" +
                            "*                                          *\n" +
                            "* B2_LemmatiseJSONFile:END                 *\n" +
                            "*                                          * \n" +
                            "********************************************\n");
    }

    /**
     * The main class that sets off the lemmatisation process! In particular, we grab the 'raw data' columns, i.e. the
     * fields that we will concatenate together to create the lemma string, here to use later. We will already have
     * checked that some exists in the checkArgs() method.
     *
     * @param args - The argument list directly from main()
     */
    private void StartLemmatisation(String[] args) throws InterruptedException {
        jWrapper = new JSONIOWrapper();
        checkArgs(args);

        for(int i = 4; i < args.length; i++)
        {
            rawDataColumns.add(args[i]);
        }

        LoadJSONFile(inputFilename);
        LemmatiseRows();
        SaveJSONFile(outputFilename);
    }

    /**
     * Here we check we have all the arguments required. In particular, we check that we have arguments for the files
     * we input from and output to, we set what the minimum number of lemmas should be to be eligible for topic
     * modelling, and we check we have at least one column to pass to the lemmatiser
     *
     * @param args - The argument list directly from main()
     */
    private void checkArgs(String[] args)
    {
        if(args.length < 2)
        {
            System.out.println("\nArguments missing!\n\nPlease supply arguments in the following order: [JSON Input Location] [JSON Output Location] [Remove Original Text] [Min Num Lemmas] [Column For Lemma Data 1] [Column For Lemma Data 2] ...\n");
            System.exit(1);
        }
        else if(args.length < 5)
        {
            System.out.println("\nYou must select at least one column to use for the lemma data!.\n\nPlease supply arguments in the following order: [JSON Input Location] [JSON Output Location] [Remove Original Text] [Min Num Lemmas] [Column For Lemma Data 1] [Column For Lemma Data 2] ...");
            System.exit(1);
        }
        else
        {
            inputFilename = args[0];
            outputFilename = args[1];

            if(args[2].equalsIgnoreCase(("false")))
                removeOriginalText = false;

            try
            {
                minNumLemmas = Integer.parseInt(args[3]);
            }
            catch (NumberFormatException e)
            {
                System.out.println("\nThe first input must be a valid integer!.\n\nPlease supply arguments in the following order: [JSON Input Location] [JSON Output Location] [Min Num Lemmas] [Column For Lemma Data 1] [Column For Lemma Data 2] ...");
                System.exit(1);
            }
        }
    }

    /**
     * Load in the JSON file, using the JSONIOWrapper functionality.
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
     * Here we do the actual lammatising, using the Stanford Lemmatiser which is included in the StanfordNLP library.
     * As lemmatising is a time consuming process, especially for large documents, by default we do this in parallel,
     * but this can be disabled if necessary. We use the Java forEach() style, so each entry in the
     * ConcurrentHashMap is passed individually to another function to do the actual work.
     *
     * N.B. Please note that this will take almost 100% of your CPU!
     */
    private void LemmatiseRows()
    {
        System.out.println("\n**********\nLoading Stanford Lemmatiser!\n***********\n");
        slem = new StanfordLemmatizer();
        System.out.println("\n**********\nStanford Lemmatiser Loaded!\n***********\n");

        System.out.println("\n**********\nStarting Lemmatising!\n***********\n");

        numRows = JSONRows.size();
        startTime = System.currentTimeMillis();

        //Parallel version of the lambada-style code. Please note:
        //1. You need to parallelise the entry set, and pass that to the method
        //2. You should use a ConcurrentHashMap rather than a usual HashMap. Normal HashMap will work (wrapped in a synchronizedMap) but will likely be slower.
        if(RUN_IN_PARALLEL)
            JSONRows.entrySet().parallelStream().forEach(this::LemmatiseRow);
        //Previous, non-parallel version
        else
            JSONRows.entrySet().forEach(this::LemmatiseRow);

        long timeTaken = (System.currentTimeMillis() - startTime) / (long)1000;
        System.out.println("\n**********\nLemmatising Complete!\n***********\n"  +
                Math.floorDiv(timeTaken, 60) + " minutes, " + timeTaken % 60 + " seconds.");
    }

    /**
     * This is the second part of the lemmatising process, where the entries are sent to be processed by the Java
     * forEach() style. In this method, we check a row was successfully retrieved, and if so we first concatenate all
     * the text from every field/column we specified, skipping fields/columns with no text. We then trim these, remove
     * some troublesome symbols, lemmatise the text and remove stopwords as specified in the StopWords file.
     *
     * @param entry - a single entry to lemmatise
     */
    private void LemmatiseRow(Map.Entry<String, DocumentRow> entry)
    {
        if(rowsProcessed % UPDATE_FREQUENCY == 0 && rowsProcessed != 0)
        {
            System.out.println();
            long timeTaken = (System.currentTimeMillis() - startTime) / (long)1000;
            String timeTakenStr = "Time Taken: " + Math.floorDiv(timeTaken, 60) + " minutes, " + timeTaken % 60 + " seconds.";

            float timeLeft = ((float) timeTaken / (float) rowsProcessed) * (numRows - rowsProcessed);
            String timeToGoStr = "Estimated Remaining Time: " + Math.floor(timeLeft / 60) + " minutes, " + Math.floor(timeLeft % 60) + " seconds.";

            float percentage = (((float) rowsProcessed / (float) numRows) * 100);

            System.out.println("Lemmatising Row ID: " + entry.getKey() + " | Number: " + rowsProcessed +
                    " | Percent Complete: " + (Math.round(percentage * 100f) / 100f) + "%");

            System.out.println(timeTakenStr + " | " + timeToGoStr);
        }

        if(failedRetrievals.containsKey(entry.getKey()))
        {
           System.out.println("**********\nSKIPPING ROW - The row with ID " + entry.getKey() + " was not fully retrieved. Lemmatising skipped.\n**********\n");
        }
        else
        {
            DocumentRow row = entry.getValue();

            String rawDataString = "";
            for(int i = 0; i < rawDataColumns.size(); i++)
            {
                if(row.getValue(rawDataColumns.get(i)) != null)
                {
                    rawDataString += " " + row.getValue(rawDataColumns.get(i));

                    /**
                     * NOTE: This will remove ANY column from the JSON which was used to construct the lemma string.
                     *       This is mostly for space reasons, and because the raw text is normally not used further
                     *       down the line.
                     */
                    if(removeOriginalText)
                        row.removeValue(rawDataColumns.get(i));
                }
            }

            rawDataString = rawDataString.trim();

            rawDataString = rawDataString.toLowerCase();                    //Convert everything to lowercase

            /*rawDataString = rawDataString.replace('.', ' ');                //Replace full stops with spaces
            rawDataString = rawDataString.replace('-', ' ');                //Replace hyphens with spaces
            rawDataString = rawDataString.replace('/', ' ');                //Replace slashes with spaces
            rawDataString = rawDataString.replace('\\', ' ');               //Replace slashes with spaces
            rawDataString = rawDataString.replace(',', ' ');                //Replace commas with spaces*/

            /**
             * Remove stop phrases! This list should be kept small, as it will likely have an adverse effect on
             * performance!
             */
            for(String phrase: StopWords.STOPPHRASES)
            {
                rawDataString = rawDataString.replaceAll(phrase.toLowerCase(), " ");
            }

            rawDataString = rawDataString.replaceAll("\\n", " ");
            rawDataString = rawDataString.replaceAll("\\r", " ");

            rawDataString = rawDataString.replaceAll("\\W", " ");

            rawDataString = rawDataString.trim().replaceAll(" +", " ");     //Trim all white space to single spaces

            //Perform the lemmatizing and remove stop words
            List<String> inputLem = StanfordLemmatizer.removeStopWords(slem.lemmatise(rawDataString));

            row.setLemmas(inputLem);

            if(inputLem.size() < minNumLemmas)
                row.setIncludedInModel(false, "Too few words left after lemmatising. Min words was set at " + minNumLemmas);
        }

        rowsProcessed++;
    }

    /**
     * Finally, we save the information back to the JSON file which we specify a path to here using the JSONIOWrapper
     * functionality. In particular, we update the required field in the JSON which stores the lemma text (done by
     * setLemmas() previously) and add the information of what we've done into metadata field.
     *
     * @param JSONFile - location of the JSON file to save to
     */
    private void SaveJSONFile(String JSONFile) throws InterruptedException {
        jWrapper.SetRowData(JSONRows);

        ConcurrentHashMap<String, String> metadata = jWrapper.GetMetadata();
        metadata.put("P2_Used", "LemmatiseJSONFile");
        metadata.put("MinNumLemmas", String.valueOf(minNumLemmas));

        String columnString = "";
        for (String rawDataColumn : rawDataColumns)
            columnString += rawDataColumn + ",";

        columnString = columnString.substring(0, columnString.length() - 1);

        metadata.put("LemmaColumns", columnString);

        jWrapper.SetMetadata(metadata);

        System.gc();
        TimeUnit.MINUTES.sleep(1);

        jWrapper.SaveJSON(JSONFile);
    }
}
