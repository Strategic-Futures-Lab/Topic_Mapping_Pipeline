package P3_TopicModelling;

import P0_Project.DocumentInferModuleSpecs;
import P3_TopicModelling.TopicModelCore.TopicModel;
import PX_Data.DocIOWrapper;
import PX_Data.JSONIOWrapper;
import PX_Data.TopicIOWrapper;
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
import java.util.stream.Collectors;

public class InferDocuments {

    private String mainModelFile;
    private boolean inferFromSubModel;
    private String subModelFile;

    private boolean exportCSV;
    private String csvOutput;
    private List<String> docFields;
    private int numWordId;
    private boolean mergeMainTopics;
    private String mainTopicsFile;
    private String mainTopicsOutput;
    private boolean mergeSubTopics;
    private String subTopicsFile;
    private String subTopicsOutput;
    private boolean mergeDocuments;
    private String documentsFile;
    private String documentsOutput;

    private JSONObject InferMetadata;
    private ConcurrentHashMap<String, DocIOWrapper> DocumentsToInfer;

    private JSONObject ModelDocumentsMetadata;
    private ConcurrentHashMap<String, DocIOWrapper> ModelDocuments;

    private JSONObject ModelMainTopicsMetadata;
    private ConcurrentHashMap<String, TopicIOWrapper> ModelMainTopics;
    private JSONArray ModelMainTopicsSimilarities;
    private JSONObject ModelSubTopicsMetadata;
    private ConcurrentHashMap<String, TopicIOWrapper> ModelSubTopics;
    private JSONArray ModelSubTopicsSimilarities;

    private int docsProcessed = 0;
    private int totalDocs = 0;
    private long inferStartTime;
    private final static int UPDATE_FREQUENCY = 100;

    private int iterations;

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
        startClass.LoadData();
        startClass.InferDocuments();
        startClass.SaveData();

        long timeTaken = (System.currentTimeMillis() - startTime) / (long)1000;

        LogPrint.printModuleEnd("Document Inference");

        return "Document Inference: "+Math.floorDiv(timeTaken, 60) + " m, " + timeTaken % 60 + " s";
    }

    private void ProcessArguments(DocumentInferModuleSpecs specs, LemmaReader reader){
        LogPrint.printNewStep("Processing arguments", 0);

        documentsFile = specs.documents;
        mainModelFile = specs.mainModel;
        inferFromSubModel = specs.inferFromSubModel;
        if(inferFromSubModel) subModelFile = specs.subModel;

        exportCSV = specs.exportCSV;
        if(exportCSV){
            csvOutput = specs.csvOutput;
            docFields = Arrays.asList(specs.docFields);
            numWordId = specs.numWordId;
        }
        mergeMainTopics = specs.mergeMainTopics;
        if(mergeMainTopics){
            mainTopicsFile = specs.mainTopics;
            mainTopicsOutput = specs.mainTopicsOutput;
        }
        mergeSubTopics = specs.mergeSubTopics && inferFromSubModel;
        if(mergeSubTopics){
            subTopicsFile = specs.subTopics;
            subTopicsOutput = specs.subTopicsOutput;
        }
        mergeDocuments = specs.mergeDocuments;
        if(mergeDocuments){
            documentsOutput = specs.documentsOutput;
        }

        InferMetadata = reader.getMetadata();
        DocumentsToInfer = reader.getDocuments();

        iterations = specs.iterations;

        LogPrint.printCompleteStep();
        String serSubModelStr = inferFromSubModel ? " and from sub-model" : "";
        LogPrint.printNote("Inferring "+ DocumentsToInfer.size()+" document(s) distributions from main model"+serSubModelStr);
        if(exportCSV) LogPrint.printNote("Exporting inferred topic distributions in CSV format");
        if(mergeMainTopics) LogPrint.printNote("Merging inferred main topic data with model main topic data");
        if(mergeSubTopics) LogPrint.printNote("Merging inferred sub topic data with model sub topic data");
        if(mergeDocuments) LogPrint.printNote("Merging inferred document data with model document data");

    }

    private void LoadData(){
        loadModels();
        loadDataFiles();
    }

    private void loadDataFiles(){
        LogPrint.printNewStep("Loading data", 0);
        // loading previous documents
        JSONObject input = JSONIOWrapper.LoadJSON(documentsFile, 1);
        ModelDocumentsMetadata = (JSONObject) input.get("metadata");
        JSONArray docs = (JSONArray) input.get("documents");
        ModelDocuments = new ConcurrentHashMap<>();
        for(JSONObject docEntry: (Iterable<JSONObject>) docs) {
            DocIOWrapper doc = new DocIOWrapper(docEntry);
            ModelDocuments.put(doc.getId(), doc);
        }
        adjustDocIds();
        // loading previous models, only if merging later
        if(mergeMainTopics){
            input = JSONIOWrapper.LoadJSON(mainTopicsFile, 1);
            ModelMainTopicsMetadata = (JSONObject) input.get("metadata");
            ModelMainTopics = new ConcurrentHashMap<>();
            for(JSONObject topicEntry: (Iterable<JSONObject>) input.get("topics")){
                TopicIOWrapper topic = new TopicIOWrapper(topicEntry);
                ModelMainTopics.put(topic.getId(), topic);
            }
            ModelMainTopicsSimilarities = (JSONArray) input.get("similarities");
        }
        if(mergeSubTopics){
            input = JSONIOWrapper.LoadJSON(subTopicsFile, 1);
            ModelSubTopicsMetadata = (JSONObject) input.get("metadata");
            ModelSubTopics = new ConcurrentHashMap<>();
            for(JSONObject topicEntry: (Iterable<JSONObject>) input.get("topics")){
                TopicIOWrapper topic = new TopicIOWrapper(topicEntry);
                ModelSubTopics.put(topic.getId(), topic);
            }
            ModelSubTopicsSimilarities = (JSONArray) input.get("similarities");
        }
    }

    private void adjustDocIds(){
        int inferIdCount = -1;
        int indexCount = -1;
        // check if previous list of docs already had inferred docs and save the max id
        for(Map.Entry<String, DocIOWrapper> entry: ModelDocuments.entrySet()){
            String id = entry.getKey();
            if(id.startsWith("infer_")){
                int docId = Integer.parseInt(id.replace("infer_", ""));
                if(docId>inferIdCount){
                    inferIdCount = docId;
                }
            }
            int docIndex = entry.getValue().getIndex();
            if(docIndex>indexCount){
                indexCount = docIndex;
            }
        }
        ConcurrentHashMap<String, DocIOWrapper> newMap = new ConcurrentHashMap<>();
        // adjust the ids for inferred docs
        for(Map.Entry<String, DocIOWrapper> entry: DocumentsToInfer.entrySet()){
            String id = entry.getKey();
            DocIOWrapper doc = entry.getValue();
            int docIndex = doc.getIndex();
            // increment doc id to avoid conflict with previous inferred docs
            doc.setId(String.valueOf(Integer.parseInt(id)+inferIdCount+1));
            doc.setIndex(docIndex+indexCount+1);
            // add prefix to id
            doc.prefixId("infer_");
            newMap.put(doc.getId(), doc);
            // clear the doc from the previous list
            DocumentsToInfer.remove(id);
        }
        // replace the map with updated ids
        DocumentsToInfer = newMap;

    }

    private void loadModels(){
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
        getTopicWords();
    }

    private void getTopicWords(){
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

        totalDocs = DocumentsToInfer.size();
        inferStartTime = System.currentTimeMillis();

        DocumentsToInfer.entrySet().forEach(this::inferDocument);

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
        doc.setInferred(true);
        if(!doc.isRemoved()){
            doc.setMainTopicDistribution(mainModel.InferTopics(doc.getLemmaString(), iterations));
            if(inferFromSubModel){
                doc.setSubTopicDistribution((subModel.InferTopics(doc.getLemmaString(), iterations)));
            }
        }
        docsProcessed++;
    }

    private void SaveData(){
        LogPrint.printNewStep("Saving data", 0);
        // saving only the inferred docs in csv
        if(exportCSV) saveCSV();
        //
        if(mergeDocuments) saveMergedDocuments();
        if(mergeMainTopics) saveMergedTopics(ModelMainTopics, ModelMainTopicsMetadata, ModelMainTopicsSimilarities, mainTopicsOutput, true);
        if(mergeSubTopics) saveMergedTopics(ModelSubTopics, ModelSubTopicsMetadata, ModelSubTopicsSimilarities, subTopicsOutput, false);
    }

    private void saveCSV(){
        LogPrint.printNewStep("Saving "+ csvOutput, 1);
        int subTopicsSize = inferFromSubModel ? subTopicWords.size() : 0;
        String[] topicsLabels = new String[mainTopicWords.size()+subTopicsSize];
        for (Map.Entry<Integer, String> t : mainTopicWords.entrySet()) {
            topicsLabels[t.getKey()] = "_mainTopic_"+t.getValue();
        }
        if(inferFromSubModel){
            int offset = mainTopicWords.size();
            for (Map.Entry<Integer, String> t : subTopicWords.entrySet()) {
                topicsLabels[t.getKey()+offset] = "_subTopic_"+t.getValue();
            }
        }
        File file = new File(csvOutput);
        file.getParentFile().mkdirs();
        CsvWriter csvWriter = new CsvWriter();
        csvWriter.setAlwaysDelimitText(true);
        try(CsvAppender csvAppender = csvWriter.append(file, StandardCharsets.UTF_8)){
            createHeader(csvAppender, topicsLabels);
            for(Map.Entry<String, DocIOWrapper> d: DocumentsToInfer.entrySet()){
                DocIOWrapper doc = d.getValue();
                csvAppender.appendField(doc.getId());
                for(String f: docFields){
                    csvAppender.appendField(doc.getData(f));
                }
                csvAppender.appendField(Integer.toString(doc.getNumLemmas()));
                csvAppender.appendField(Boolean.toString(!doc.isRemoved()));
                csvAppender.appendField(Boolean.toString(doc.isInferred()));
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

    private void createHeader(CsvAppender appender, String[] topicLabels) throws IOException {
        appender.appendField("_docId");
        for(String f: docFields){
            appender.appendField(f);
        }
        appender.appendField("_wordCount");
        appender.appendField("_inModel");
        appender.appendField("_inferred");
        for(String l:topicLabels){
            appender.appendField(l);
        }
        appender.endLine();
    }

    private void saveMergedDocuments(){
        JSONObject root = new JSONObject();
        JSONArray documents = new JSONArray();
        JSONObject meta = (JSONObject) ModelDocumentsMetadata.clone();
        meta.put("nDocsInferred", getNDocsInferred(DocumentsToInfer) + getNDocsInferred(ModelDocuments));
        meta.put("nDocsTooShort", getNDocsRemoved(DocumentsToInfer) + getNDocsRemoved(ModelDocuments));
        meta.put("totalDocs", DocumentsToInfer.size()+ModelDocuments.size());
        root.put("metadata", meta);
        DocIOWrapper.PrintModel();
        for(Map.Entry<String, DocIOWrapper> entry: ModelDocuments.entrySet()){
            documents.add(entry.getValue().toJSON());
        }
        for(Map.Entry<String, DocIOWrapper> entry: DocumentsToInfer.entrySet()){
            documents.add(entry.getValue().toJSON());
        }
        root.put("documents", documents);
        JSONIOWrapper.SaveJSON(root, documentsOutput, 1);
    }

    private int getNDocsRemoved(ConcurrentHashMap<String, DocIOWrapper> docs){
        return docs.entrySet().stream()
                .map(d -> d.getValue())
                .filter(d -> d.isRemoved())
                .collect(Collectors.toList())
                .size();
    }

    private int getNDocsInferred(ConcurrentHashMap<String, DocIOWrapper> docs){
        return docs.entrySet().stream()
                .map(d -> d.getValue())
                .filter(d -> d.isInferred())
                .collect(Collectors.toList())
                .size();
    }

    private void saveMergedTopics(ConcurrentHashMap<String, TopicIOWrapper> topics, JSONObject metadata, JSONArray similarities, String output, boolean isMain) {
        mergeInferredDocsWithTopics(topics, isMain);
        JSONObject root = new JSONObject();
        JSONArray JsonTopics = new JSONArray();
        JSONObject meta = (JSONObject) metadata.clone();
        int topicDocsTooShort = getIntJson(metadata, "nDocsTooShort", 0);
        int topicTotalDocs = getIntJson(metadata, "totalDocs", 0);
        int topicDocsInferred = getIntJson(metadata, "nDocsInferred", 0);
        meta.put("nDocsInferred", getNDocsInferred(DocumentsToInfer) + topicDocsInferred);
        meta.put("nDocsTooShort", getNDocsRemoved(DocumentsToInfer) + topicDocsTooShort);
        meta.put("totalDocs", DocumentsToInfer.size() + topicTotalDocs);
        root.put("metadata", meta);
        for (Map.Entry<String, TopicIOWrapper> entry : topics.entrySet()) {
            JsonTopics.add(entry.getValue().toJSON());
        }
        root.put("topics", JsonTopics);
        root.put("similarities", similarities);
        JSONIOWrapper.SaveJSON(root, output, 1);
    }

    private void mergeInferredDocsWithTopics(ConcurrentHashMap<String, TopicIOWrapper> topics, boolean isMain){
        for(Map.Entry<String, TopicIOWrapper> t: topics.entrySet()){
            TopicIOWrapper topic = t.getValue();
            int index = topic.getIndex();
            List<TopicIOWrapper.JSONTopicWeight> topicInferredDocs = new ArrayList<>();
            for(Map.Entry<String, DocIOWrapper> d: DocumentsToInfer.entrySet()){
                DocIOWrapper doc = d.getValue();
                if(doc.isInferred() && !doc.isRemoved()){
                    double[] topicDistribution = isMain ? doc.getMainTopicDistribution() : doc.getSubTopicDistribution();
                    double topicWeight = topicDistribution[index];
                    if(topicWeight > 0){
                        topicInferredDocs.add(new TopicIOWrapper.JSONTopicWeight(doc.getId(), topicDistribution[index]));
                    }
                }
            }
            topic.addToDocs(topicInferredDocs);
        }
    }

    private int getIntJson(JSONObject jsonObject, String name, int def){
        return Math.toIntExact((long) jsonObject.getOrDefault(name, (long) def));
    }
}
