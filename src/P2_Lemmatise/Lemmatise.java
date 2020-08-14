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

public class Lemmatise {

    private JSONObject metadata;
    private ConcurrentHashMap<String, DocIOWrapper> Documents;
    private int docsProcessed = 0;
    private int totalDocs = 0;
    private long lemStartTime;
    private StanfordLemmatizer slem;
    private List<String> lowCounts;

    private final static int UPDATE_FREQUENCY = 100;
    private final static boolean RUN_IN_PARALLEL = true;

    private String corpusFile;
    private String outputFile;
    private List<String> textFields;
    private List<String> docFields;
    private List<String> stopWords;
    private List<String> stopPhrases;
    private int minLemmas;
    private int removeLowCounts;
    private int totalDocRemoved = 0;

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

        return "Lemmatising: "+Math.floorDiv(timeTaken, 60) + " m, " + timeTaken % 60 + " s";
    }

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

    private void LoadCorpusFile(){
        JSONObject input = JSONIOWrapper.LoadJSON(corpusFile, 0);
        metadata = (JSONObject) input.get("metadata");
        JSONArray corpus = (JSONArray) input.get("corpus");
        Documents = new ConcurrentHashMap<>();
        for(JSONObject docEntry: (Iterable<JSONObject>) corpus){
            DocIOWrapper doc = new DocIOWrapper(docEntry);
            Documents.put(doc.getId(), doc);
        }
        // System.out.println("Loaded corpus!");
    }

    private void ProcessDocuments(){
        LogPrint.printNewStep("Processing documents", 0);
        for(Map.Entry<String, DocIOWrapper> doc: Documents.entrySet()){
            doc.getValue().addTexts(textFields);
            doc.getValue().filterData(docFields);
        }
        LogPrint.printCompleteStep();
    }

    private void LemmatiseDocuments(){

        LogPrint.printNewStep("Loading lemmatiser", 0);
        LogPrint.printNote("Following output from Stanford CoreNLP\n");
        slem = new StanfordLemmatizer();
        LogPrint.printNewStep("Loading lemmatiser", 0);
        LogPrint.printCompleteStep();

        LogPrint.printNewStep("Lemmatisation", 0);

        totalDocs = Documents.size();
        lemStartTime = System.currentTimeMillis();

        //Parallel version of the lambada-style code. Please note:
        //1. You need to parallelise the entry set, and pass that to the method
        //2. You should use a ConcurrentHashMap rather than a usual HashMap. Normal HashMap will work (wrapped in a synchronizedMap) but will likely be slower.
        if(RUN_IN_PARALLEL)
            Documents.entrySet().parallelStream().forEach(this::LemmatiseDocument);
        //Previous, non-parallel version
        else
            Documents.entrySet().forEach(this::LemmatiseDocument);

        LogPrint.printNewStep("Lemmatisation", 0);
        LogPrint.printCompleteStep();
    }

    private void LemmatiseDocument(Map.Entry<String, DocIOWrapper> docEntry){

        if(docsProcessed % UPDATE_FREQUENCY == 0 && docsProcessed != 0) {
            long lemTimeTaken = (System.currentTimeMillis() - lemStartTime) / (long)1000;
            String timeTakenStr = "time: " + Math.floorDiv(lemTimeTaken, 60) + " m, " + lemTimeTaken % 60 + " s.";
            float lemTimeLeft = ((float) lemTimeTaken / (float) docsProcessed) * (totalDocs - docsProcessed);
            String timeToGoStr = "remaining (est.)): " + Math.floor(lemTimeLeft / 60) + " m, " + Math.floor(lemTimeLeft % 60) + " s.";
            float percentage = (((float) docsProcessed / (float) totalDocs) * 100);
            LogPrint.printNewStep("Lemmatised: " + docsProcessed +
                    " documents | % complete: " + (Math.round(percentage * 100f) / 100f) + "%", 1);
            LogPrint.printStep(timeTakenStr + " | " + timeToGoStr, 1);
        }

        // getting the text from document
        DocIOWrapper doc = docEntry.getValue();
        String rawText = "";
        for(int i = 0; i < textFields.size(); i++){
            if(doc.getText(textFields.get(i)) != null){
                rawText += " " + doc.getText(textFields.get(i));
            }
        }
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

    private void CleanLemmas(){
        if(removeLowCounts > 0){
            LogPrint.printNewStep("Cleaning low count lemmas", 0);
            HashMap<String, Integer> lemmaCounts = new HashMap<>();
            for(Map.Entry<String, DocIOWrapper> doc: Documents.entrySet()){
                for(String l: doc.getValue().getLemmas()){
                    if(lemmaCounts.containsKey(l)){
                        int v = lemmaCounts.get(l);
                        lemmaCounts.put(l, v+1);
                    } else {
                        lemmaCounts.put(l, 1);
                    }
                }
            }
            lowCounts = lemmaCounts.entrySet().stream()
                    .filter(e -> e.getValue() <= removeLowCounts)
                    .map(e->e.getKey())
                    .collect(Collectors.toList());

            Documents.entrySet().parallelStream().forEach(e->e.getValue().filterOutLemmas(lowCounts));
            LogPrint.printCompleteStep();
            LogPrint.printNote("Found "+lowCounts.size()+" lemmas with count less than "+(removeLowCounts+1));

        }
        Documents.entrySet().parallelStream().forEach(e->{
            DocIOWrapper doc = e.getValue();
            if(doc.getLemmas().size() < minLemmas && !doc.isRemoved()){
                doc.setTooShort(true);
                totalDocRemoved++;
            }
            doc.makeLemmaString();
        });
    }

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
            metadata.put("minNLemmas", removeLowCounts);
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
