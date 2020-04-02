package P2_Lemmatise;

import P0_Project.ProjectLemmatise;
import P2_Lemmatise.Lemmatizer.StanfordLemmatizer;
import PX_Data.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Lemmatise {

    private JSONObject metadata;
    private ConcurrentHashMap<String, JSONDocument> Documents;
    private int docsProcessed = 0;
    private int totalDocs = 0;
    private long startTime;
    private StanfordLemmatizer slem;
    private HashMap<String, Integer> lemmaCounts = new HashMap<>();

    private final static int UPDATE_FREQUENCY = 100;
    private final static boolean RUN_IN_PARALLEL = true;

    private String corpusFile;
    private String outputFile;
    private List<String> textFields;
    private List<String> docFields;
    private List<String> stopWords;
    private int minLemmas;
    private int removeLowCounts;
    private int totalDocRemoved = 0;

    public static void Lemmatise(ProjectLemmatise lemmaSpecs){
        System.out.println( "**********************************************************\n" +
                            "* STARTING Lemmatise !                                   *\n" +
                            "**********************************************************\n");

        Lemmatise startClass = new Lemmatise();
        startClass.ProcessArguments(lemmaSpecs);
        startClass.LoadCorpusFile();
        startClass.ProcessDocuments();
        startClass.LemmatiseDocuments();
        startClass.CleanLemmas();
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
        removeLowCounts = lemmaSpecs.removeLowCounts;
    }

    private void LoadCorpusFile(){
        JSONObject input = JSONIOWrapper.LoadJSON(corpusFile);
        metadata = (JSONObject) input.get("metadata");
        JSONArray corpus = (JSONArray) input.get("corpus");
        Documents = new ConcurrentHashMap<>();
        for(JSONObject docEntry: (Iterable<JSONObject>) corpus){
            JSONDocument doc = new JSONDocument(docEntry);
            Documents.put(doc.getId(), doc);
        }
        System.out.println("Loaded corpus!");
    }

    private void ProcessDocuments(){
        for(Map.Entry<String, JSONDocument> doc: Documents.entrySet()){
            doc.getValue().addTexts(textFields);
            doc.getValue().filterData(docFields);
        }
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

    private void LemmatiseDocument(Map.Entry<String, JSONDocument> docEntry){

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

        JSONDocument doc = docEntry.getValue();
        String rawText = "";
        for(int i = 0; i < textFields.size(); i++){
            if(doc.getText(textFields.get(i)) != null){
                rawText += " " + doc.getText(textFields.get(i));
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
            totalDocRemoved++;
        }

        docsProcessed++;
    }

    private void CleanLemmas(){
        if(removeLowCounts > 0){
            System.out.println("Cleaning Low Count Lemmas ...");
            startTime = System.currentTimeMillis();
            for(Map.Entry<String, JSONDocument> doc: Documents.entrySet()){
                for(String l: doc.getValue().getLemmas()){
                    if(lemmaCounts.containsKey(l)){
                        int v = lemmaCounts.get(l);
                        lemmaCounts.put(l, v+1);
                    } else {
                        lemmaCounts.put(l, 1);
                    }
                }
            }
            List<String> lowCounts = lemmaCounts.entrySet().stream()
                    .filter(e -> e.getValue() <= removeLowCounts)
                    .map(e->e.getKey())
                    .collect(Collectors.toList());
            System.out.println("Found "+lowCounts.size()+" lemmas with count less or equal to "+removeLowCounts);
            Documents.entrySet().parallelStream().forEach(e->e.getValue().filterOutLemmas(lowCounts));
            long timeTaken = (System.currentTimeMillis() - startTime) / (long)1000;
            System.out.println("Low Count Lemmas Cleaned!"  +
                    Math.floorDiv(timeTaken, 60) + " minutes, " + timeTaken % 60 + " seconds.");
        }
        Documents.entrySet().parallelStream().forEach(e->{
            JSONDocument doc = e.getValue();
            if(doc.getLemmas().size() < minLemmas && !doc.isRemoved()){
                doc.remove("Too short");
                totalDocRemoved++;
            }
            doc.makeLemmaString();
        });
    }

    private void OutputJSON(){
        JSONObject root = new JSONObject();
        JSONArray lemmas = new JSONArray();
        metadata.put("nDocsRemoved", totalDocRemoved);
        metadata.put("stopWords", String.join(",", stopWords));
        root.put("metadata", metadata);
        JSONDocument.PrintLemmas();
        for(Map.Entry<String, JSONDocument> entry: Documents.entrySet()){
            lemmas.add(entry.getValue().toJSON());
        }
        root.put("lemmas", lemmas);
        JSONIOWrapper.SaveJSON(root, outputFile);
    }
}
