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

/**
 * Class building an index of labels.
 * Reads data from topic model files (documents and topics).
 * Each unique label gets a list the topic ids where its a top word and the document ids where it can be found.
 * Data is saved it on a label index JSON file.
 *
 * @author P. Le Bras
 * @version 1
 */
public class LabelIndexing {

    /** Boolean flag for index labels from documents. */
    private boolean indexDocuments;
    /** Filename of the document JSON file to index from. */
    private String documentsFile;
    /** Boolean flag for using all documents (true) or just the top documents from topics (false). */
    private boolean useAllDocuments = false;
    /** Boolean from for using all labels (true) or just the top labels from topics (false). */
    private boolean useAllLabels = false;
    /** Filename of the main topic JSON file to index from. */
    private String mainTopicsFile;
    /** Boolean flag for index labels from sub topics. */
    private boolean indexSubTopics;
    /** Filename of the sub topic JSON file to index from. */
    private String subTopicsFile;
    /** Filename of the output index JSON file. */
    private String outputFile;

    /** List of documents. */
    private ConcurrentHashMap<String, DocIOWrapper> documents;
    /** List of main topics. */
    private ConcurrentHashMap<String, TopicIOWrapper> mainTopics;
    /** List of sub topics. */
    private ConcurrentHashMap<String, TopicIOWrapper> subTopics;

    /** Index map. */
    private ConcurrentHashMap<String, LabelIndexEntry> Index;

    /**
     * Main method, reads the specification and launches the sub-methods in order.
     * @param indexSpecs Specifications.
     * @return String indicating the time taken to read the model's JSON file(s), index labels and write the JSON index file.
     */
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

    /**
     * Method processing the specification parameters.
     * @param indexSpecs Specification object.
     */
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

    /**
     * Method loading data from the model's JSON file(s).
     */
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

    /**
     * Method launching the indexing processes.
     */
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

    /**
     * Method indexing labels from a list of documents.
     * @param docs List of documents to index labels from.
     */
    private void indexLabelsFromDocs(ConcurrentHashMap<String, DocIOWrapper> docs){
        LogPrint.printNewStep("Indexing from documents", 1);
        for(Map.Entry<String, DocIOWrapper> doc: docs.entrySet()){
            String docId = doc.getKey();
            for(String lemma: doc.getValue().getLemmas()){
                // first part of if statement adds existing labels from topics
                // second part also adds document labels not in topics
                if(Index.containsKey(lemma)){
                    Index.get(lemma).documents.add(docId);
                } else if(useAllLabels) {
                    LabelIndexEntry entry = new LabelIndexEntry(lemma);
                    entry.documents.add(docId);
                    Index.put(lemma, entry);
                }
            }
        }
        LogPrint.printCompleteStep();
    }

    /**
     * Method indexing labels from a list of topics.
     * @param topics List of topics to index labels from.
     * @param isMain Boolean flag if the list of topics is main one or the sub one.
     */
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
                    LabelIndexEntry entry = new LabelIndexEntry(label);
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

    /**
     * Method removing indexed document if they are not in the top documents list of topics.
     * Only applies if useAllDocuments is false.
     */
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

    /**
     * Method writing the label index on file.
     */
    private void SaveIndex(){
        JSONObject root = new JSONObject();
        for(Map.Entry<String, LabelIndexEntry> indexRow: Index.entrySet()){
            String label = indexRow.getKey();
            LabelIndexEntry indexEntry = indexRow.getValue();
            root.put(label, indexEntry.toJSON(indexSubTopics, indexDocuments));
        }
        JSONIOWrapper.SaveJSON(root, outputFile, 0);
    }
}
