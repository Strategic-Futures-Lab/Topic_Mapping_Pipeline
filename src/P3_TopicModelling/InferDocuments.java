package P3_TopicModelling;

import P0_Project.DocumentInferModuleSpecs;
import P3_TopicModelling.TopicModelCore.TopicModel;
import PX_Data.DocIOWrapper;
import PX_Data.JSONIOWrapper;
import PY_Helper.LogPrint;
import cc.mallet.types.Alphabet;
import cc.mallet.types.IDSorter;
import de.siegmar.fastcsv.writer.CsvAppender;
import de.siegmar.fastcsv.writer.CsvWriter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InferDocuments {

    private String modelDir;
    private String mainModelFile;
    private boolean inferFromSubModel;
    private String subModelFile;

    private JSONObject metadata;
    private ConcurrentHashMap<String, DocIOWrapper> Documents;

    private int docsProcessed = 0;
    private int totalDocs = 0;
    private long inferStartTime;

    private final static int UPDATE_FREQUENCY = 100;

    private int iterations;

    private String docOutput;
    private List<String> docFields;
    private boolean exportMainCSV = false;
    private String mainOutputCSV;
    private boolean exportSubCSV = false;
    private String subOutputCSV;
    private boolean exportMergedCSV = false;
    private String outputCSV;
    private int numWordId;

    private TopicModel mainModel;
    private TopicModel subModel;

    private HashMap<Integer, String> mainTopicWords;
    private HashMap<Integer, String> subTopicWords;

    public static String InferDocuments(DocumentInferModuleSpecs specs){

        LogPrint.printModuleStart("Document Inference");

        long startTime = System.currentTimeMillis();

        InferDocuments startClass = new InferDocuments();
        LemmaReader reader = new LemmaReader(specs.lemmas);
        startClass.ProcessArguments(specs, reader);
        startClass.LoadModels();
        startClass.InferDocuments();
        startClass.SaveDocuments();
        // startClass.GetAndSetTopicDetails();
        // startClass.GetAndSetTopicSimilarity();
        // startClass.SaveTopics();
        // startClass.SaveDocuments(startClass.nTopics, -1);

        long timeTaken = (System.currentTimeMillis() - startTime) / (long)1000;

        LogPrint.printModuleEnd("Document Inference");

        return "Document Inference: "+Math.floorDiv(timeTaken, 60) + " m, " + timeTaken % 60 + " s";
    }

    private void ProcessArguments(DocumentInferModuleSpecs specs, LemmaReader reader){
        LogPrint.printNewStep("Processing arguments", 0);
        mainModelFile = specs.mainModel;
        inferFromSubModel = specs.inferFromSubModel;
        if(inferFromSubModel){
            subModelFile = specs.subModel;
        }

        metadata = reader.getMetadata();
        Documents = reader.getDocuments();

        iterations = specs.iterations;

        docOutput = specs.documentOutput;
        docFields = Arrays.asList(specs.docFields);
        exportMainCSV = specs.exportMainCSV;
        if(exportMainCSV){
            mainOutputCSV = specs.mainOutputCSV;
        }
        if(inferFromSubModel){
            exportSubCSV = specs.exportSubCSV;
            if(exportSubCSV){
                subOutputCSV = specs.subOutputCSV;
            }
        }
        exportMergedCSV = specs.exportMergedTopicsCSV;
        if(exportMergedCSV){
            outputCSV = specs.outputCSV;
        }
        numWordId = specs.numWordId;
        LogPrint.printCompleteStep();
        String serSubModelStr = inferFromSubModel ? " and from sub-model" : "";
        LogPrint.printNote("Inferring "+Documents.size()+" document(s) distributions from main model"+serSubModelStr);
        if(exportMainCSV) LogPrint.printNote("Exporting main topic distributions in CSV format too");
        if(exportSubCSV) LogPrint.printNote("Exporting sub topic distributions in CSV format too");

    }

    private void LoadModels(){
        LoadModels(0);
    }

    private void LoadModels(int number){
        LogPrint.printNewStep("Loading main model", 0);
        mainModel = new TopicModel();
        try{
            mainModel = (TopicModel) (new ObjectInputStream(new FileInputStream(mainModelFile))).readObject();
            LogPrint.printCompleteStep();
        } catch (Exception e){
            LogPrint.printNoteError("Enable to load model from "+mainModelFile);
            LogPrint.printNoteError(e.getMessage());
            System.exit(1);
        }
        if(inferFromSubModel){
            LogPrint.printNewStep("Loading sub model", 0);
            subModel = new TopicModel();
            try{
                subModel = (TopicModel) (new ObjectInputStream(new FileInputStream(subModelFile))).readObject();
                LogPrint.printCompleteStep();
            } catch (Exception e){
                LogPrint.printNoteError("Enable to load model from "+subModelFile);
                LogPrint.printNoteError(e.getMessage());
                System.exit(1);
            }
        }
        GetTopicWords();
    }

    private void GetTopicWords(){
        LogPrint.printNewStep("Getting topic labels", 0);
        mainTopicWords = getTopicWords(mainModel);
        if(inferFromSubModel){
            subTopicWords = getTopicWords(subModel);
        }
        LogPrint.printCompleteStep();
    }

    private HashMap<Integer, String> getTopicWords(TopicModel tModel){
        HashMap<Integer, String> topicWords = new HashMap<>();
        ArrayList<TreeSet<IDSorter>> sortedWords = tModel.model.getSortedWords();
        Alphabet alphabet = tModel.model.getAlphabet();
        for(int topic = 0; topic < sortedWords.size(); topic++){
            List<String> words = new ArrayList<>();
            int count = 0;
            for(IDSorter word: sortedWords.get(topic)){
                if(count >= numWordId) break;
                if(word.getWeight() > 0){
                    words.add((String) alphabet.lookupObject(word.getID()));
                }
                count++;
            }
            topicWords.put(topic, String.join("-", words));

        }
        return topicWords;
    }

    private void InferDocuments(){
        LogPrint.printNewStep("Inferring documents", 0);

        totalDocs = Documents.size();
        inferStartTime = System.currentTimeMillis();

        Documents.entrySet().forEach(this::inferDocument);

        LogPrint.printNewStep("Inferring documents", 0);
        LogPrint.printCompleteStep();
    }

    private void inferDocument(Map.Entry<String, DocIOWrapper> document){

        if(docsProcessed % UPDATE_FREQUENCY == 0 && docsProcessed != 0) {
            long inferTimeTaken = (System.currentTimeMillis() - inferStartTime) / (long)1000;
            String timeTakenStr = "time: " + Math.floorDiv(inferTimeTaken, 60) + " m, " + inferTimeTaken % 60 + " s.";

            float inferTimeLeft = ((float) inferTimeTaken / (float) docsProcessed) * (totalDocs - docsProcessed);
            String timeToGoStr = "remaining (est.)): " + Math.floor(inferTimeLeft / 60) + " m, " + Math.floor(inferTimeLeft % 60) + " s.";

            float percentage = (((float) docsProcessed / (float) totalDocs) * 100);

            LogPrint.printNewStep("Inferred: " + docsProcessed +
                    " documents | % complete: " + (Math.round(percentage * 100f) / 100f) + "%", 1);

            LogPrint.printStep(timeTakenStr + " | " + timeToGoStr, 1);
        }

        DocIOWrapper doc = document.getValue();
        doc.setMainTopicDistribution(mainModel.InferTopics(doc.getLemmaString(), iterations));
        if(inferFromSubModel){
            doc.setSubTopicDistribution((subModel.InferTopics(doc.getLemmaString(), iterations)));
        }
        docsProcessed++;
    }

    private void SaveDocuments(){
        LogPrint.printNewStep("Saving data", 0);
        JSONObject root = new JSONObject();
        JSONArray documents = new JSONArray();
        JSONObject meta = (JSONObject) metadata.clone();
        meta.put("nTopicsMain", mainModel.TOPICS);
        if(inferFromSubModel){
            meta.put("nTopicsSub", subModel.TOPICS);
        }
        root.put("metadata", meta);
        DocIOWrapper.PrintModel();
        for(Map.Entry<String, DocIOWrapper> entry: Documents.entrySet()){
            documents.add(entry.getValue().toJSON());
        }
        root.put("documents", documents);
        JSONIOWrapper.SaveJSON(root, docOutput, 1);

        SaveCSV();
    }

    private void SaveCSV(){
        if(exportMainCSV) saveCSV(mainOutputCSV, mainTopicWords, true);
        if(exportSubCSV) saveCSV(subOutputCSV, subTopicWords, false);
        if(exportMergedCSV) saveMergedTopicsCSV();
    }

    private void saveCSV(String filename, HashMap<Integer, String> topicWords, boolean isMain){
        LogPrint.printNewStep("Saving "+filename, 1);
        String[] topicsLabels = new String[topicWords.size()];
        for (Map.Entry<Integer, String> t : topicWords.entrySet()) {
            String prefix = isMain ? "_mainTopic_" : "_subTopic_";
            topicsLabels[t.getKey()] = prefix+t.getValue();
        }
        File file = new File(filename);
        file.getParentFile().mkdirs();
        CsvWriter csvWriter = new CsvWriter();
        csvWriter.setAlwaysDelimitText(true);
        try(CsvAppender csvAppender = csvWriter.append(file, StandardCharsets.UTF_8)){
            createHeader(csvAppender, topicsLabels);
            for(Map.Entry<String, DocIOWrapper> d: Documents.entrySet()){
                DocIOWrapper doc = d.getValue();
                csvAppender.appendField(doc.getId());
                for(String f: docFields){
                    csvAppender.appendField(doc.getData(f));
                }
                csvAppender.appendField(Integer.toString(doc.getNumLemmas()));
                csvAppender.appendField(Boolean.toString(!doc.isRemoved()));
                if(!doc.isRemoved()){
                    double[] weights = isMain ? doc.getMainTopicDistribution() : doc.getSubTopicDistribution();
                    for(double weight: weights){
                        csvAppender.appendField(Double.toString(weight));
                    }
                }
                csvAppender.endLine();
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        LogPrint.printCompleteStep();
    }

    private void createHeader(CsvAppender appender, String[] topicLabels) throws IOException {
        appender.appendField("_docId");
        for(String f: docFields){
            appender.appendField(f);
        }
        appender.appendField("_wordCount");
        appender.appendField("_inModel");
        for(String l:topicLabels){
            appender.appendField(l);
        }
        appender.endLine();
    }

    private void saveMergedTopicsCSV(){
        LogPrint.printNewStep("Saving "+outputCSV, 1);
        String[] topicsLabels = new String[mainTopicWords.size()+subTopicWords.size()];
        for (Map.Entry<Integer, String> t : mainTopicWords.entrySet()) {
            topicsLabels[t.getKey()] = "_mainTopic_"+t.getValue();
        }
        if(inferFromSubModel){
            int offset = mainTopicWords.size();
            for (Map.Entry<Integer, String> t : subTopicWords.entrySet()) {
                topicsLabels[t.getKey()+offset] = "_subTopic_"+t.getValue();
            }
        }
        File file = new File(outputCSV);
        file.getParentFile().mkdirs();
        CsvWriter csvWriter = new CsvWriter();
        csvWriter.setAlwaysDelimitText(true);
        try(CsvAppender csvAppender = csvWriter.append(file, StandardCharsets.UTF_8)){
            createHeader(csvAppender, topicsLabels);
            for(Map.Entry<String, DocIOWrapper> d: Documents.entrySet()){
                DocIOWrapper doc = d.getValue();
                csvAppender.appendField(doc.getId());
                for(String f: docFields){
                    csvAppender.appendField(doc.getData(f));
                }
                csvAppender.appendField(Integer.toString(doc.getNumLemmas()));
                csvAppender.appendField(Boolean.toString(!doc.isRemoved()));
                if(!doc.isRemoved()){
                    for(double weight: doc.getMainTopicDistribution()){
                        csvAppender.appendField(Double.toString(weight));
                    }
                    if(inferFromSubModel){
                        for(double weight: doc.getSubTopicDistribution()){
                            csvAppender.appendField(Double.toString(weight));
                        }
                    }
                }
                csvAppender.endLine();
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        LogPrint.printCompleteStep();
    }
}
