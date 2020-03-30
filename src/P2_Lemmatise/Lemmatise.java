package P2_Lemmatise;

import PX_Helper.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Lemmatise {

    private ConcurrentHashMap<String, LemmaDocument> Documents;
    private int docsProcessed = 0;
    private int totalDocs = 0;
    private long startTime;
    private StanfordLemmatizer slem;

    private final static int UPDATE_FREQUENCY = 100;
    private final static boolean RUN_IN_PARALLEL = true;

    private List<String> textFields;
    private List<String> docFields;
    private String[] stopWords;
    private int minLemmas;
    private int totalRemoved = 0;

    public static void Lemmatise(String corpusFile,
                                 String[] texts,
                                 String[] docDetails,
                                 String[] stopW,
                                 int minL,
                                 String outputFile){
        Lemmatise startClass = new Lemmatise();
        startClass.ProcessArguments(texts, docDetails, stopW, minL);
        startClass.LoadCorpusFile(corpusFile);
        startClass.LemmatiseDocuments();
        startClass.OutputJSON(outputFile);
    }

    private void ProcessArguments(String[] texts, String[] docDetails, String[] stopW, int minL){
        textFields = Arrays.asList(texts);
        docFields = Arrays.asList(docDetails);
        stopWords = stopW;
        minLemmas = minL;
    }

    private void LoadCorpusFile(String corpusFile){
        JSONArray corpus = (JSONArray) JSONIOWrapper.LoadJSON(corpusFile).get("corpus");
        Documents = new ConcurrentHashMap<>();
        for(JSONObject docEntry: (Iterable<JSONObject>) corpus){
            LemmaDocument doc = new LemmaDocument(docEntry, textFields, docFields);
            Documents.put(doc.getId(), doc);
        }
        System.out.println("Loaded corpus");
    }

    private void LemmatiseDocuments(){

        System.out.println("\n**********\nLoading Stanford Lemmatiser!\n***********\n");
        slem = new StanfordLemmatizer();
        System.out.println("\n**********\nStanford Lemmatiser Loaded!\n***********\n");

        System.out.println("\n**********\nStarting Lemmatising!\n***********\n");

        totalDocs = Documents.size();
        startTime = System.currentTimeMillis();

        //Parallel version of the lambada-style code. Please note:
        //1. You need to parallelise the entry set, and pass that to the method
        //2. You should use a ConcurrentHashMap rather than a usual HashMap. Normal HashMap will work (wrapped in a synchronizedMap) but will likely be slower.
        if(RUN_IN_PARALLEL)
            Documents.entrySet().parallelStream().forEach(this::LemmatiseDocument);
        //Previous, non-parallel version
        else
            Documents.entrySet().forEach(this::LemmatiseDocument);

        long timeTaken = (System.currentTimeMillis() - startTime) / (long)1000;
        System.out.println("\n**********\nLemmatising Complete!\n***********\n"  +
                Math.floorDiv(timeTaken, 60) + " minutes, " + timeTaken % 60 + " seconds.");
    }

    private void LemmatiseDocument(Map.Entry<String, LemmaDocument> docEntry){

        if(docsProcessed % UPDATE_FREQUENCY == 0 && docsProcessed != 0)
        {
            System.out.println();
            long timeTaken = (System.currentTimeMillis() - startTime) / (long)1000;
            String timeTakenStr = "Time Taken: " + Math.floorDiv(timeTaken, 60) + " minutes, " + timeTaken % 60 + " seconds.";

            float timeLeft = ((float) timeTaken / (float) docsProcessed) * (totalDocs - docsProcessed);
            String timeToGoStr = "Estimated Remaining Time: " + Math.floor(timeLeft / 60) + " minutes, " + Math.floor(timeLeft % 60) + " seconds.";

            float percentage = (((float) docsProcessed / (float) totalDocs) * 100);

            System.out.println("Lemmatising Row ID: " + docEntry.getKey() + " | Number: " + docsProcessed +
                    " | Percent Complete: " + (Math.round(percentage * 100f) / 100f) + "%");

            System.out.println(timeTakenStr + " | " + timeToGoStr);
        }

        LemmaDocument doc = docEntry.getValue();
        String rawText = "";
        for(int i = 0; i < textFields.size(); i++){
            if(doc.getTextFieldValue(textFields.get(i)) != null){
                rawText += " " + doc.getTextFieldValue(textFields.get(i));
            }
        }
        rawText = rawText.trim();
        rawText = rawText.toLowerCase();
        for(String phrase: StopWords.STOPPHRASES) {
            rawText = rawText.replaceAll(phrase.toLowerCase(), " ");
        }

        rawText = rawText.replaceAll("\\n", " "); // returns
        rawText = rawText.replaceAll("\\r", " "); // carraige returns
        rawText = rawText.replaceAll("\\W", " "); // non word characters
        rawText = rawText.trim().replaceAll(" +", " ");     //Trim all white space to single spaces

        List<String> inputLemmas = StanfordLemmatizer.removeStopWords((slem.lemmatise(rawText)));

        doc.setLemmas(inputLemmas);
        if(inputLemmas.size() < minLemmas){
            doc.remove("Too short");
            totalRemoved++;
        }

        docsProcessed++;
    }

    private void OutputJSON(String outputFile){
        JSONObject root = new JSONObject();
        JSONArray lemmas = new JSONArray();
        JSONObject meta = new JSONObject();
        meta.put("nDocsRemoved", totalRemoved);
        root.put("metadata", meta);
        for(Map.Entry<String, LemmaDocument> entry: Documents.entrySet()){
            lemmas.add(entry.getValue().toJSON());
        }
        root.put("lemmas", lemmas);
        JSONIOWrapper.SaveJSON(root, outputFile);
    }
}
