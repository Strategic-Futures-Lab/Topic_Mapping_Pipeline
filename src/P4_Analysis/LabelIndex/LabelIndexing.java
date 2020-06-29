package P4_Analysis.LabelIndex;

import P0_Project.LabelIndexModuleSpecs;
import PX_Data.DocIOWrapper;
import PX_Data.JSONIOWrapper;
import PX_Data.TopicIOWrapper;
import PY_Helper.LogPrint;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class LabelIndexing {

    private boolean indexDocuments;
    private String documentsFile;
    private boolean useAllDocuments = false;
    private boolean useAllLabels = false;
    private String mainTopicsFile;
    private boolean indexSubTopics;
    private String subTopicsFile;
    private String outputFile;

    // private JSONObject mainTopicsMetadata;
    // private JSONObject subTopicsMetadata;

    private ConcurrentHashMap<String, DocIOWrapper> documents;
    private ConcurrentHashMap<String, TopicIOWrapper> mainTopics;
    private ConcurrentHashMap<String, TopicIOWrapper> subTopics;

    private ConcurrentHashMap<String, LabelIndexEntry> Index;

    public static String Index(LabelIndexModuleSpecs indexSpecs){

        LogPrint.printModuleStart("Label Indexing");

        long startTime = System.currentTimeMillis();

        LabelIndexing startClass = new LabelIndexing();
        startClass.ProcessArguments(indexSpecs);
        startClass.LoadData();
        startClass.IndexLabels();
        startClass.FilterDocs();
        startClass.SaveIndex();

        long timeTaken = (System.currentTimeMillis() - startTime) / (long)1000;

        LogPrint.printModuleEnd("Label Indexing");

        return "Label indexing: "+Math.floorDiv(timeTaken, 60) + " m, " + timeTaken % 60 + " s";
    }

    private void ProcessArguments(LabelIndexModuleSpecs indexSpecs){
        LogPrint.printNewStep("Processing arguments", 0);
        indexDocuments = indexSpecs.indexDocuments;
        if(indexDocuments){
            documentsFile = indexSpecs.documents;
            useAllDocuments = indexSpecs.useAllDocuments;
            useAllLabels = indexSpecs.useAllLabels;
        }
        mainTopicsFile = indexSpecs.mainTopics;
        outputFile = indexSpecs.indexOutput;
        indexSubTopics = indexSpecs.indexSubTopics;
        if(indexSubTopics){
            subTopicsFile = indexSpecs.subTopics;
        }
        LogPrint.printCompleteStep();
        if(indexSubTopics) LogPrint.printNote("Indexing sub topics");
    }

    private void LoadData(){
        LogPrint.printNewStep("Loading data", 0);
        JSONObject input;
        if(indexDocuments){
            input = JSONIOWrapper.LoadJSON(documentsFile, 1);
            JSONArray docs = (JSONArray) input.get("documents");
            documents = new ConcurrentHashMap<>();
            for(JSONObject docEntry: (Iterable<JSONObject>) docs){
                DocIOWrapper doc = new DocIOWrapper(docEntry);
                documents.put(doc.getId(), doc);
            }
        }
        input = JSONIOWrapper.LoadJSON(mainTopicsFile, 1);
        JSONArray topics = (JSONArray) input.get("topics");
        mainTopics = new ConcurrentHashMap<>();
        for(JSONObject topicEntry: (Iterable<JSONObject>) topics){
            TopicIOWrapper topic = new TopicIOWrapper(topicEntry);
            mainTopics.put(topic.getId(), topic);
        }
        if(indexSubTopics){
            input = JSONIOWrapper.LoadJSON(subTopicsFile, 1);
            topics = (JSONArray) input.get("topics");
            subTopics = new ConcurrentHashMap<>();
            for(JSONObject topicEntry: (Iterable<JSONObject>) topics){
                TopicIOWrapper topic = new TopicIOWrapper(topicEntry);
                subTopics.put(topic.getId(), topic);
            }
        }
    }

    private void IndexLabels(){
        LogPrint.printNewStep("Indexing labels", 0);
        Index = new ConcurrentHashMap<>();
        indexLabelsFromTopics(mainTopics, true);
        if(indexSubTopics){
            indexLabelsFromTopics(subTopics, false);
        }
        if(indexDocuments){
            indexLabelsFromDocs(documents);
        }
    }

    private void indexLabelsFromDocs(ConcurrentHashMap<String, DocIOWrapper> docs){
        LogPrint.printNewStep("Indexing from documents", 1);
        for(Map.Entry<String, DocIOWrapper> doc: docs.entrySet()){
            String docId = doc.getKey();
            for(String lemma: doc.getValue().getLemmas()){
                // P Le Bras, 25/06/2020:
                // first part of if statement adds existing labels from topics
                // second part also adds labels from document not in topics
                if(Index.containsKey(lemma)){
                    Index.get(lemma).documents.add(docId);
                } else if(useAllLabels) {
                    LabelIndexEntry entry = new LabelIndexEntry();
                    entry.documents.add(docId);
                    Index.put(lemma, entry);
                }
            }
        }
        LogPrint.printCompleteStep();
    }

    private void indexLabelsFromTopics(ConcurrentHashMap<String, TopicIOWrapper> topics, boolean isMain){
        String msg = isMain ? "Indexing from main topics" : "Indexing from sub topics";
        LogPrint.printNewStep(msg, 1);
        for(Map.Entry<String, TopicIOWrapper> topic: topics.entrySet()){
            String topicId = topic.getKey();
            List<TopicIOWrapper.JSONTopicWeight> words = topic.getValue().getWords();
            for(TopicIOWrapper.JSONTopicWeight word: words){
                String label = word.ID;
                if(Index.containsKey(label)){
                    if(isMain) Index.get(label).mainTopics.add(topicId);
                    else {
                        Index.get(label).subTopics.put(topicId, new HashSet<>(topic.getValue().getMainTopicIds()));
                    }
                } else {
                    LabelIndexEntry entry = new LabelIndexEntry();
                    if(isMain) entry.mainTopics.add(topicId);
                    else {
                        entry.subTopics.put(topicId, new HashSet<>(topic.getValue().getMainTopicIds()));
                    }
                    Index.put(label, entry);
                }
            }
        }
        LogPrint.printCompleteStep();
    }

    private void FilterDocs(){
        if(indexDocuments && !useAllDocuments) {
            // Lets you filter out documents not referenced in topics from the indexed labels
            LogPrint.printNewStep("Removing reference from unused documents", 0);
            for(Map.Entry<String, LabelIndexEntry> entry: Index.entrySet()) {

                Set<String> finalDocIds = new HashSet<>();

                LabelIndexEntry indexEntry = entry.getValue();
                Set<String> indexDocIds = indexEntry.documents;
                Set<String> mainTopicIds = indexEntry.mainTopics;
                Map<String, Set<String>> subTopicsEntries = indexEntry.subTopics;
                Set<String> subTopicIds = new HashSet<>();
                for (Map.Entry<String, Set<String>> subTopicEntry : subTopicsEntries.entrySet()) {
                    subTopicIds.add(subTopicEntry.getKey());
                    mainTopicIds.addAll(subTopicEntry.getValue());
                }

                for (String topicId : mainTopicIds) {
                    List<String> topicDocs = mainTopics.get(topicId).getDocs().stream().map(w -> w.ID).collect(Collectors.toList());
                    for (String docId : indexDocIds) {
                        if (topicDocs.contains(docId)) {
                            finalDocIds.add(docId);
                        }
                    }
                }
                if (indexSubTopics) {
                    for (String topicId : subTopicIds) {
                        List<String> topicDocs = subTopics.get(topicId).getDocs().stream().map(w -> w.ID).collect(Collectors.toList());
                        for (String docId : indexDocIds) {
                            if (topicDocs.contains(docId)) {
                                finalDocIds.add(docId);
                            }
                        }
                    }
                }
                indexEntry.documents = finalDocIds;
            }
            LogPrint.printCompleteStep();
        }
    }

    private void SaveIndex(){
        JSONObject root = new JSONObject();
        for(Map.Entry<String, LabelIndexEntry> indexRow: Index.entrySet()){
            String label = indexRow.getKey();
            LabelIndexEntry indexEntry = indexRow.getValue();
            JSONObject ids = new JSONObject();
            JSONArray mainTopicIds = new JSONArray();
            for(String id : indexEntry.mainTopics){
                mainTopicIds.add(id);
            }
            ids.put("mainTopics", mainTopicIds);
            if(indexSubTopics){
                JSONArray subTopicIds = new JSONArray();
                for(Map.Entry<String, Set<String>> subIds : indexEntry.subTopics.entrySet()){
                    JSONArray subTopicId = new JSONArray();
                    JSONArray subTopicMainIds = new JSONArray();
                    subTopicMainIds.addAll(subIds.getValue());
                    subTopicId.add(subIds.getKey());
                    subTopicId.add(subTopicMainIds);
                    subTopicIds.add(subTopicId);
                }
                ids.put("subTopics", subTopicIds);
            }
            if(indexDocuments){
                JSONArray docIds = new JSONArray();
                for(String id : indexEntry.documents){
                    docIds.add(id);
                }
                ids.put("documents", docIds);
            }
            root.put(label, ids);
        }
        JSONIOWrapper.SaveJSON(root, outputFile, 0);
    }
}
