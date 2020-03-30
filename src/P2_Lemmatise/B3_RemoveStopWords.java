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
 * Created by P. Le Bras on 05/12/18
 */
public class B3_RemoveStopWords {

    private JSONIOWrapper jWrapper;
    private ConcurrentHashMap<String, DocumentRow> JSONRows = new ConcurrentHashMap<>();
    //Contains the ID of any row that failed, and the reason it failed!
    private ConcurrentHashMap<String, String> failedRetrievals;

    private String inputFilename, outputFilename, stopWordFilename;

    private List<String> stopWords = new ArrayList<>();

    private final static boolean RUN_IN_PARALLEL = false;

    /**
     * This is an optional step, which cane be run after the lemmatisation phase. This modules removes from the
     * document rows any word specified in the StopWords.txt file. In particular it aim at removing words 'polluting'
     * the topic model when they are too generic for the current dataset, e.g.:
     *  - "ieee", "acm", "doi", "conference" in academic publications
     *  - "investment", "benefit", ... for a financial company
     *
     * The StopWord file should follow the same organisation as the StopPhrase file: one word per line, and lines
     * starting with # will be discarded as comments.
     *
     * @param args - [JSON Input File] [JSON Output File] [Stop Word File]
     */
    public static void main(String[] args){

        System.out.println( "\n********************************************\n" +
                            "*                                          *\n" +
                            "* STARTING B3_RemoveStopWords PHASE!       *\n" +
                            "*                                          *\n" +
                            "* B3_RemoveStopWords:START                 *\n" +
                            "*                                          *\n" +
                            "********************************************\n");

        B3_RemoveStopWords startClass = new B3_RemoveStopWords();
        startClass.RemoveStopWords(args);

        System.out.println( "\n********************************************\n" +
                            "*                                          *\n" +
                            "* B3_RemoveStopWords PHASE COMPLETE!       *\n" +
                            "*                                          *\n" +
                            "* B3_RemoveStopWords:END                   *\n" +
                            "*                                          *\n" +
                            "********************************************\n");
    }

    /**
     * Runs the stages of the process, to remove stop words from the document lemmas.
     *
     * @param args - The argument list from main().
     */
    private void RemoveStopWords(String[] args){

        jWrapper = new JSONIOWrapper();
        checkArgs(args);

        LoadJSONFile(inputFilename);
        LoadStopWords(stopWordFilename);
        CheckRowsForStopWords();

        SaveJSONFile(outputFilename);
    }

    /**
     * Checks that the right number of arguments has been passed to the module.
     *
     * @param args - Arguments from main().
     */
    private void checkArgs(String[] args){

        if(args.length < 3){
            System.out.println("\nArguments missing!\n\nPlease supply arguments in the following order: [JSON Input Location] [JSON Output Location] [Stop Word File]\n");
            System.exit(1);
        } else {
            inputFilename = args[0];
            outputFilename = args[1];
            stopWordFilename = args[2];
        }
    }

    /**
     * Loads the information from the JSON file, using the JSONIOWrapper interface
     *
     * @param JSONFile - location of the JSON file to load
     */
    private void LoadJSONFile(String JSONFile){

        jWrapper.LoadJSON(JSONFile);
        JSONRows = jWrapper.GetRowData();
        failedRetrievals = jWrapper.GetFailedRetrievals();
    }

    /**
     * Loads the stop words from the file provided. For safe measures those are compressed, keeping only alphanumeric
     * characters to check agains the lemmas.
     *
     * @param stopWordFilename - the file containg the stop words.
     */
    private void LoadStopWords(String stopWordFilename){

        System.out.println("\n**********\nLoading Stop Words!\n***********\n");

        try(BufferedReader br = new BufferedReader(new FileReader(stopWordFilename))){
            String line;
            while((line = br.readLine()) != null){
                // Check for comment lines, starting with #
                if(line.charAt(0) != '#'){
                    stopWords.add(line.replaceAll("[\\W]", ""));
                }
            }
        } catch (IOException e){
            e.printStackTrace();
            System.exit(1);
        }

        System.out.println("\n**********\nStop Words Loaded! " + stopWords.size() + " were found!\n***********\n");
    }

    /**
     * Launched the stop word check for each row. Can run in parallel like B1_RemoveStopPhrases.
     */
    private void CheckRowsForStopWords(){
        System.out.println("\n**********\nChecking Rows For Stop Words!\n***********\n");
        if(RUN_IN_PARALLEL)
            JSONRows.entrySet().parallelStream().forEach(this::ProcessRow);
        else
            JSONRows.entrySet().forEach(this::ProcessRow);
        System.out.println("\n**********\nStop Word Processing Complete!\n***********\n");
    }

    /**
     * main process method, given a document row, it gets its lemma string, and replace every occurrence of the stop
     * words (surrounded by spaces) with one space.
     *
     * @param entry - a single entry to process
     */
    private void ProcessRow(Map.Entry<String, DocumentRow> entry){

        if(failedRetrievals.containsKey(entry.getKey())){
            System.out.println("**********\nSKIPPING ROW - The row with ID " + entry.getKey() + " was not fully retrieved. Stop Word Processing skipped.\n**********\n");
        } else {

            DocumentRow row = entry.getValue();
            String lemmas = row.getLemmaStringData();
            for(String stopword: stopWords){
                lemmas = lemmas.replaceAll(" "+stopword+" ", " ");
            }
            row.setLemmaStringData(lemmas);
        }
    }

    private void SaveJSONFile(String JSONFile){

        jWrapper.SetRowData(JSONRows);

        ConcurrentHashMap<String, String> metadata = jWrapper.GetMetadata();

        String stopwordsString = "";
        for(String stopword: stopWords){
            stopwordsString += stopword+",";
        }
        stopwordsString = stopwordsString.substring(0, stopwordsString.length()-1);

        metadata.put("StopWords", stopwordsString);

        jWrapper.SetMetadata(metadata);

        jWrapper.SaveJSON(JSONFile);
    }
}
