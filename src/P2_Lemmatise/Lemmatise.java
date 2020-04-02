package P2_Lemmatise;

import P0_Project.ProjectLemmatise;
import PX_Helper.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Lemmatise {

    private JSONObject metadata;
    private ConcurrentHashMap<String, LemmaJSONDocument> Documents;
    private int docsProcessed = 0;
    private int totalDocs = 0;
    private long startTime;
    private StanfordLemmatizer slem;

    private final static int UPDATE_FREQUENCY = 100;
    private final static boolean RUN_IN_PARALLEL = true;

    private String corpusFile;
    private String outputFile;
    private List<String> textFields;
    private List<String> docFields;
    private List<String> stopWords;
    private int minLemmas;
    private int totalRemoved = 0;

    public static void Lemmatise(ProjectLemmatise lemmaSpecs){
//                                String corpusFile,
//                                 String[] texts,
//                                 String[] docDetails,
//                                 String[] stopW,
//                                 int minL,
//                                 String outputFile){
        System.out.println( "**********************************************************\n" +
                            "* STARTING Lemmatise !                                   *\n" +
                            "**********************************************************\n");

        Lemmatise startClass = new Lemmatise();
        startClass.ProcessArguments(lemmaSpecs);
        startClass.LoadCorpusFile();
        startClass.LemmatiseDocuments();
        startClass.OutputJSON();

        System.out.println( "**********************************************************\n" +
                            "* Lemmatise COMPLETE !                                   *\n" +
                            "**********************************************************\n");
    }

    private void ProcessArguments(ProjectLemmatise lemmaSpecs){
        corpusFile = lemmaSpecs.corpus;
        outputFile = lemmaSpecs.output;
        textFields = Arrays.asList(lemmaSpecs.textFields);
        docFields = Arrays.asList(lemmaSpecs.docFields);
        stopWords = Arrays.asList(lemmaSpecs.stopWords);
        minLemmas = lemmaSpecs.minLemmas;
    }

    private void LoadCorpusFile(){
        JSONObject input = JSONIOWrapper.LoadJSON(corpusFile);
        metadata = (JSONObject) input.get("metadata");
        JSONArray corpus = (JSONArray) input.get("corpus");
        Documents = new ConcurrentHashMap<>();
        for(JSONObject docEntry: (Iterable<JSONObject>) corpus){
            LemmaJSONDocument doc = new LemmaJSONDocument(docEntry, textFields, docFields);
            Documents.put(doc.getId(), doc);
        }
        System.out.println("Loaded corpus!");
    }

    private void LemmatiseDocuments(){

        System.out.println("Loading Stanford Lemmatiser");
        slem = new StanfordLemmatizer();
        System.out.println("Stanford Lemmatiser Loaded");

        System.out.println("Starting Lemmatising!");

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
        System.out.println("Lemmatising Complete!"  +
                Math.floorDiv(timeTaken, 60) + " minutes, " + timeTaken % 60 + " seconds.");
    }

    private void LemmatiseDocument(Map.Entry<String, LemmaJSONDocument> docEntry){

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

        LemmaJSONDocument doc = docEntry.getValue();
        String rawText = "";
        for(int i = 0; i < textFields.size(); i++){
            if(doc.getTextFieldValue(textFields.get(i)) != null){
                rawText += " " + doc.getTextFieldValue(textFields.get(i));
            }
        }
        rawText = rawText.trim();
        rawText = rawText.toLowerCase();
//        for(String phrase: StopWords.STOPPHRASES) {
//            rawText = rawText.replaceAll(phrase.toLowerCase(), " ");
//        }

        rawText = rawText.replaceAll("\\n", " "); // returns
        rawText = rawText.replaceAll("\\r", " "); // carraige returns
        rawText = rawText.replaceAll("\\W", " "); // non word characters
        rawText = rawText.trim().replaceAll(" +", " ");     //Trim all white space to single spaces

        List<String> inputLemmas = StanfordLemmatizer.removeStopWords((slem.lemmatise(rawText)));

        inputLemmas.removeAll(stopWords);

        doc.setLemmas(inputLemmas);
        if(inputLemmas.size() < minLemmas){
            doc.remove("Too short");
            totalRemoved++;
        }

        docsProcessed++;
    }

    private void OutputJSON(){
        JSONObject root = new JSONObject();
        JSONArray lemmas = new JSONArray();
        metadata.put("nDocsRemoved", totalRemoved);
        metadata.put("stopWords", String.join(",", stopWords));
        root.put("metadata", metadata);
        for(Map.Entry<String, LemmaJSONDocument> entry: Documents.entrySet()){
            lemmas.add(entry.getValue().toJSON());
        }
        root.put("lemmas", lemmas);
        JSONIOWrapper.SaveJSON(root, outputFile);
    }
}
