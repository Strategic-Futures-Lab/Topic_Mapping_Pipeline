package P2_Lemmatise;

import P0_Project.LemmatiseModuleSpecs;
import P2_Lemmatise.Lemmatizer.StanfordLemmatizer;
import PX_Data.*;
import PY_Helper.LogPrint;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Class reading a corpus JSON file, processing its document to lemmatise their texts and saving them into a lemma JSON file.
 *
 * @author T. Methven, P. Le Bras
 * @version 2
 */
public class Lemmatise {

    /** JSON object containing the documents metadata. */
    private JSONObject metadata;
    /** List of documents. */
    private ConcurrentHashMap<String, DocIOWrapper> Documents;
    /** Count of documents processed by the lemmatiser. */
    private int docsProcessed = 0;
    /** Total number of documents. */
    private int totalDocs = 0;
    /** Start time for the lemmatiser, to follow progress. */
    private long lemStartTime;
    /** Lemmatiser. */
    private StanfordLemmatizer slem;
    /** List of lemmas with a number of occurrences lower than removeLowCounts. */
    private List<String> lowCounts;

    /** Rate at which we log an update on the lemmatisation progress. */
    private final static int UPDATE_FREQUENCY = 100;
    /** Flag for running the lemmatisation in parallel. */
    private final static boolean RUN_IN_PARALLEL = true;

    /** Filename of the input corpus JSON file. */
    private String corpusFile;
    /** Filename of the output lemmas JSON file. */
    private String outputFile;
    /** List of fields to consider as the document text. */
    private List<String> textFields;
    /** List of fields to keep in the document data after lemmatisation. */
    private List<String> docFields;
    /** List of custom stop words to exclude from lemmas. */
    private List<String> stopWords;
    /** List of custom stop phrases to exclude from document texts. */
    private List<String> stopPhrases;
    /** Minimum number of lemmas a document must have. */
    private int minLemmas;
    /** Minimum amount of time a lemma must be used (across all documents) to be kept. */
    private int removeLowCounts;
    /** Number of documents that will be removed from the topic modelling for being too short. */
    private int totalDocRemoved = 0;

    /**
     * Main method, reads the specification and launches the sub-methods in order.
     * @param lemmaSpecs Specifications.
     * @return String indicating the time taken to read the corpus JSON file, lemmatise documents and produce the JSON lemma file.
     */
    public static String Lemmatise(LemmatiseModuleSpecs lemmaSpecs){

        LogPrint.printModuleStart("Lemmatisation");

        long startTime = System.currentTimeMillis();

        Lemmatise startClass = new Lemmatise();
        startClass.ProcessArguments(lemmaSpecs);
        startClass.LoadCorpusFile();
        startClass.ProcessDocuments();
        startClass.LemmatiseDocuments();
        startClass.CleanLemmas();
        startClass.OutputJSON();

        long timeTaken = (System.currentTimeMillis() - startTime) / (long)1000;

        LogPrint.printModuleEnd("Lemmatisation");

        return "Lemmatisation: "+Math.floorDiv(timeTaken, 60) + " m, " + timeTaken % 60 + " s";
    }

    /**
     * Method processing the specification parameters.
     * @param lemmaSpecs Specification object.
     */
    private void ProcessArguments(LemmatiseModuleSpecs lemmaSpecs){
        LogPrint.printNewStep("Processing arguments", 0);
        corpusFile = lemmaSpecs.corpus;
        outputFile = lemmaSpecs.output;
        textFields = Arrays.asList(lemmaSpecs.textFields);
        docFields = Arrays.asList(lemmaSpecs.docFields);
        stopWords = Arrays.asList(lemmaSpecs.stopWords);
        stopPhrases = Arrays.asList(lemmaSpecs.stopPhrases);
        minLemmas = lemmaSpecs.minLemmas;
        removeLowCounts = lemmaSpecs.removeLowCounts;
        LogPrint.printCompleteStep();
        if(removeLowCounts > 0) LogPrint.printNote("Removing lemmas with count less than "+(removeLowCounts+1));
    }

    /**
     * Method reading the corpus JSON file and setting the list of documents.
     */
    private void LoadCorpusFile(){
        JSONObject input = JSONIOWrapper.LoadJSON(corpusFile, 0);
        metadata = (JSONObject) input.get("metadata");
        JSONArray corpus = (JSONArray) input.get("corpus");
        Documents = new ConcurrentHashMap<>();
        for(JSONObject docEntry: (Iterable<JSONObject>) corpus){
            DocIOWrapper doc = new DocIOWrapper(docEntry);
            Documents.put(doc.getId(), doc);
        }
        LogPrint.printNote("Found "+Documents.size()+" documents to lemmatise", 0);
    }

    /**
     * Method processing the corpus documents to prepare the text fields for lemmatisation and set data fields to keep.
     */
    private void ProcessDocuments(){
        LogPrint.printNewStep("Processing documents", 0);
        for(Map.Entry<String, DocIOWrapper> doc: Documents.entrySet()){
            // Copy desired entries from doc data to doc text
            doc.getValue().addTexts(textFields);
            // Filter doc data to keep only desired entries
            doc.getValue().filterData(docFields);
        }
        LogPrint.printCompleteStep();
    }

    /**
     * Method instantiating the lemmatiser and launching the lemmatisation process.
     */
    private void LemmatiseDocuments(){
        LogPrint.printNewStep("Loading lemmatiser", 0);
        LogPrint.printNote("Following output from Stanford CoreNLP\n");
        slem = new StanfordLemmatizer();
        LogPrint.printNewStep("Lemmatiser loaded", 0);
        LogPrint.printCompleteStep();

        LogPrint.printNewStep("Starting lemmatisation", 0);

        totalDocs = Documents.size();
        lemStartTime = System.currentTimeMillis();

        //Parallel version of the lambada-style code. Please note:
        //1. You need to parallelise the entry set, and pass that to the method
        //2. You should use a ConcurrentHashMap rather than a usual HashMap. Normal HashMap will work (wrapped in a synchronizedMap) but will likely be slower.
        if(RUN_IN_PARALLEL)
            Documents.entrySet().parallelStream().forEach(this::lemmatiseDocument);
        //Previous, non-parallel version
        else
            Documents.entrySet().forEach(this::lemmatiseDocument);

        LogPrint.printNewStep("Lemmatisation", 0);
        LogPrint.printCompleteStep();
    }

    /**
     * Method lemmatising the document from the corpus.
     * @param docEntry Document to lemmatise.
     */
    private void lemmatiseDocument(Map.Entry<String, DocIOWrapper> docEntry){

        if(docsProcessed % UPDATE_FREQUENCY == 0 && docsProcessed != 0) {
            long lemTimeTaken = (System.currentTimeMillis() - lemStartTime) / (long)1000;
            String timeTakenStr = "time: " + Math.floorDiv(lemTimeTaken, 60) + " m, " + lemTimeTaken % 60 + " s.";
            float lemTimeLeft = ((float) lemTimeTaken / (float) docsProcessed) * (totalDocs - docsProcessed);
            String timeToGoStr = "remaining (est.)): " + Math.floor(lemTimeLeft / 60) + " m, " + Math.floor(lemTimeLeft % 60) + " s.";
            float percentage = (((float) docsProcessed / (float) totalDocs) * 100);
            LogPrint.printNewStep("Lemmatised: " + docsProcessed +
                    " documents | % complete: " + (Math.round(percentage * 100f) / 100f) + "%", 1);
            LogPrint.printLog(timeTakenStr + " | " + timeToGoStr, 1);
        }

        // getting the text from document
        DocIOWrapper doc = docEntry.getValue();
        String rawText = "";
        for (String textField : textFields) {
            if (doc.getText(textField) != null) {
                rawText += " " + doc.getText(textField);
            }
        }
        // trim whit spaces and put everything lower case
        rawText = rawText.trim();
        rawText = rawText.toLowerCase();

        // remove stop-phrases
        for(String phrase: stopPhrases){
            rawText = rawText.replaceAll(phrase.toLowerCase(), " ");
        }

        // removing special characters
        rawText = rawText.replaceAll("\\n", " "); // returns
        rawText = rawText.replaceAll("\\r", " "); // carraige returns
        rawText = rawText.replaceAll("\\W", " "); // non word characters
        rawText = rawText.trim().replaceAll(" +", " "); //Trim all white space to single spaces

        // lemmatising
        List<String> inputLemmas = StanfordLemmatizer.removeStopWords((slem.lemmatise(rawText)));

        // remove stop-words
        inputLemmas.removeAll(stopWords);

        // adding lemmas to document
        doc.setLemmas(inputLemmas);

        // checking length of lemmas
        if(inputLemmas.size() < minLemmas){
            doc.setTooShort(true);
            totalDocRemoved++;
        }

        docsProcessed++;
    }

    /**
     * Method removing lemmas from the corpus if their total count falls under the given threshold.
     * It essentially cleans the corpus of lemmas with a low number of occurrences.
     */
    private void CleanLemmas(){
        // Removing lemmas with count in vocabulary less than removeLowCounts.
        if(removeLowCounts > 0) {
            LogPrint.printNewStep("Cleaning low count lemmas", 0);
            HashMap<String, Integer> lemmaCounts = new HashMap<>();
            for (Map.Entry<String, DocIOWrapper> doc : Documents.entrySet()) {
                for (String l : doc.getValue().getLemmas()) {
                    if (lemmaCounts.containsKey(l)) {
                        int v = lemmaCounts.get(l);
                        lemmaCounts.put(l, v + 1);
                    } else {
                        lemmaCounts.put(l, 1);
                    }
                }
            }
            lowCounts = lemmaCounts.entrySet().stream()
                    .filter(e -> e.getValue() <= removeLowCounts)
                    .map(e -> e.getKey())
                    .collect(Collectors.toList());

            Documents.entrySet().parallelStream().forEach(e -> e.getValue().removeLemmas(lowCounts));
            LogPrint.printCompleteStep();
            LogPrint.printNote("Found and removed " + lowCounts.size() + " lemmas with count less than " + (removeLowCounts + 1));
        }
        // Re-checking that documents have the required number of lemmas.
        Documents.entrySet().parallelStream().forEach(e->{
            DocIOWrapper doc = e.getValue();
            if(doc.getLemmas().size() < minLemmas && !doc.isRemoved()){
                doc.setTooShort(true);
                totalDocRemoved++;
            }
            doc.makeLemmaString();
        });
    }

    /**
     * Method writing the list of documents onto the JSON lemma file.
     */
    private void OutputJSON(){
        JSONObject root = new JSONObject();
        JSONArray lemmas = new JSONArray();
        metadata.put("nDocsTooShort", totalDocRemoved);
        metadata.put("minDocSize", minLemmas);
        JSONArray stopWordsArray = new JSONArray();
        for(String w: stopWords){
            stopWordsArray.add(w);
        }
        metadata.put("stopWords", stopWordsArray);
        JSONArray stopPhrasesArray = new JSONArray();
        for(String p: stopPhrases){
            stopPhrasesArray.add(p);
        }
        metadata.put("stopPhrases", stopPhrasesArray);
        if(removeLowCounts > 0) {
            metadata.put("nLemmasRemoved", lowCounts.size());
            metadata.put("minLemmaCount", removeLowCounts);
        }
        root.put("metadata", metadata);
        DocIOWrapper.PrintLemmas();
        for(Map.Entry<String, DocIOWrapper> entry: Documents.entrySet()){
            lemmas.add(entry.getValue().toJSON());
        }
        root.put("lemmas", lemmas);
        JSONIOWrapper.SaveJSON(root, outputFile, 0);
    }
}
