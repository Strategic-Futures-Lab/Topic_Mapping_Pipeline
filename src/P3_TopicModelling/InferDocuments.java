package P3_TopicModelling;

import P0_Project.DocumentInferModuleSpecs;
import P3_TopicModelling.TopicModelCore.ModelledTopic;
import P3_TopicModelling.TopicModelCore.TopicModel;
import PX_Data.DocIOWrapper;
import PX_Data.JSONIOWrapper;
import PX_Data.TopicIOWrapper;
import PY_Helper.LogPrint;
import de.siegmar.fastcsv.writer.CsvAppender;
import de.siegmar.fastcsv.writer.CsvWriter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Class reading a lemma JSON file and existing (main and/or sub) topic model data, inferring the new documents topic distributions
 * and saving/updating several data files.
 * <br>
 * The files saved include:<br>
 * - (optional) Updated Topics JSON files (include the inferred documents in their top document list if appropriate);
 * - (optional) Updated Document JSON file (include the inferred documents);
 * - (optional) Inferred documents CSV file.
 *
 * @author P. Le Bras
 * @version 2
 */
public class InferDocuments {

    /** Filename for the serialised main model. */
    private String mainModelFile;
    /** Instance of the main topic model. */
    private TopicModel mainModel;
    /** Boolean flag for inferring documents with the sub model. */
    private boolean inferFromSubModel;
    /** Filename for the serialised sub model. */
    private String subModelFile;
    /** Instance of the sub topic model. */
    private TopicModel subModel;

    /** Boolean flag for exporting the documents' inferences into a CSV file. */
    private boolean exportCSV;
    /** Filename of the CSV file where to save the documents' inferences. */
    private String csvOutput;
    /** List of data fields to include in the documents' inferences CSV file. */
    private List<String> docFields;
    /** Number of words to use when generating topic identifiers in the documents' inferences CSV file. */
    private int numWordId;

    /** Boolean flag for merging the inferred documents into the existing main topics. */
    private boolean mergeMainTopics;
    /** Filename of the JSON file with the existing main topics. */
    private String mainTopicsFile;
    /** Filename of the JSON file where to saved the merged main topics data. */
    private String mainTopicsOutput;
    /** Boolean flag for merging the inferred documents into the existing sub topics. */
    private boolean mergeSubTopics;
    /** Filename of the JSON file with the existing sub topics. */
    private String subTopicsFile;
    /** Filename of the JSON file where to saved the merged sub topics data. */
    private String subTopicsOutput;
    /** Boolean flag for merging the inferred documents with the existing model's documents. */
    private boolean mergeDocuments;
    /** Filename of the JSON file with the existing model's documents. */
    private String documentsFile;
    /** Filename of the JSON file where to saved the merged documents data. */
    private String documentsOutput;

    /** Filename of the lemma JSON file containing the documents to infer. */
    private String lemmaFile;
    /** List of documents to infer. */
    private ConcurrentHashMap<String, DocIOWrapper> DocumentsToInfer;

    /** JSON object containing metadata about the existing model's documents. */
    private JSONObject ModelDocumentsMetadata;
    /** List of documents which have already been modelled. */
    private ConcurrentHashMap<String, DocIOWrapper> ModelDocuments;

    /** JSON object containing metadata about the existing model's main topics. */
    private JSONObject ModelMainTopicsMetadata;
    /** List of main topics. */
    private ConcurrentHashMap<String, TopicIOWrapper> ModelMainTopics;
    /** JSON array of the main topics' similarities. */
    private JSONArray ModelMainTopicsSimilarities;
    /** JSON object containing metadata about the existing model's sub topics. */
    private JSONObject ModelSubTopicsMetadata;
    /** List of sub topics. */
    private ConcurrentHashMap<String, TopicIOWrapper> ModelSubTopics;
    /** JSON array of the sub topics' similarities. */
    private JSONArray ModelSubTopicsSimilarities;

    /** Number of documents processed by the inferencer. */
    private int docsProcessed = 0;
    /** Total number of documents to infer. */
    private int totalDocs = 0;
    /** Start time for of the inference process. */
    private long inferStartTime;
    /** Frequency, in number of documents, at which the module logs the inference progress. */
    private final static int UPDATE_FREQUENCY = 100;

    /** Number of iterations the inference process has to run. */
    private int iterations;

    /** List of main topics top words to use as identifier. */
    private HashMap<Integer, String> mainTopicWords;
    /** List of sub topics top words to use as identifier. */
    private HashMap<Integer, String> subTopicWords;

    /**
     * Main method, reads the specification and launches the sub-methods in order.
     * @param specs Specifications.
     * @return String indicating the time taken to read the lemma JSON file, infer the documents' topics from existing model(s)
     * and process the output files.
     */
    public static String InferDocuments(DocumentInferModuleSpecs specs){

        LogPrint.printModuleStart("Document Inference");

        long startTime = System.currentTimeMillis();

        InferDocuments startClass = new InferDocuments();
        startClass.ProcessArguments(specs);
        startClass.LoadData();
        startClass.InferDocuments();
        startClass.SaveData();

        long timeTaken = (System.currentTimeMillis() - startTime) / (long)1000;

        LogPrint.printModuleEnd("Document Inference");

        return "Document Inference: "+Math.floorDiv(timeTaken, 60) + " m, " + timeTaken % 60 + " s";
    }

    /**
     * Method processing the specification parameters.
     * @param specs Specifications.
     */
    private void ProcessArguments(DocumentInferModuleSpecs specs){
        LogPrint.printNewStep("Processing arguments", 0);

        lemmaFile = specs.lemmas;

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
            documentsFile = specs.documents;
            documentsOutput = specs.documentsOutput;
        }

        iterations = specs.iterations;

        LogPrint.printCompleteStep();
        String serSubModelStr = inferFromSubModel ? " and from sub-model" : "";
        LogPrint.printNote("Inferring "+ DocumentsToInfer.size()+" document(s) distributions from main model"+serSubModelStr);
        if(exportCSV) LogPrint.printNote("Exporting inferred topic distributions in CSV format");
        if(mergeMainTopics) LogPrint.printNote("Merging inferred main topic data with model main topic data");
        if(mergeSubTopics) LogPrint.printNote("Merging inferred sub topic data with model sub topic data");
        if(mergeDocuments) LogPrint.printNote("Merging inferred document data with model document data");

    }

    /**
     * Method launching all necessary data loading methods.
     */
    private void LoadData(){
        loadDocumentsToInfer();
        loadModels();
        loadDataFiles();
        // after all is loaded, adjust the ids of docs to infer
        adjustDocIds();
    }

    /**
     * Method loading the documents to infer.
     */
    private void loadDocumentsToInfer(){
        LemmaReader reader = new LemmaReader(lemmaFile);
        DocumentsToInfer = reader.getDocuments();
    }

    /**
     * Method loading the existing model's data files: documents and topics (if necessary).
     */
    private void loadDataFiles(){
        LogPrint.printNewStep("Loading model data", 0);
        JSONObject input;
        // loading previous documents, only if merging later
        ModelDocuments = new ConcurrentHashMap<>();
        if(mergeDocuments) {
            input = JSONIOWrapper.LoadJSON(documentsFile, 1);
            ModelDocumentsMetadata = (JSONObject) input.get("metadata");
            JSONArray docs = (JSONArray) input.get("documents");
            for (JSONObject docEntry : (Iterable<JSONObject>) docs) {
                DocIOWrapper doc = new DocIOWrapper(docEntry);
                ModelDocuments.put(doc.getId(), doc);
            }
        }
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

    /**
     * Method adjusting the ids of documents to infer, based on the existing model's documents.
     */
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

    /**
     * Method loading the serialised topic model instances.
     */
    private void loadModels(){
        LogPrint.printNewStep("Loading serialised main model", 0);
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
            LogPrint.printNewStep("Loading serialised sub model", 0);
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

    /**
     * Method extracting and saving the top words from each topics in the existing main and sub models.
     */
    private void getTopicWords(){
        LogPrint.printNewStep("Getting topic labels", 0);
        mainTopicWords = getTopicWords(mainModel);
        if(inferFromSubModel){
            subTopicWords = getTopicWords(subModel);
        }
        LogPrint.printCompleteStep();
    }

    /**
     * Method extracting and saving the top words from each topics in the given model.
     * @param tModel Model to extract topic words from.
     * @return The list of each topic top words.
     */
    private HashMap<Integer, String> getTopicWords(TopicModel tModel){
        HashMap<Integer, String> topicWords = new HashMap<>();
        List<ModelledTopic> modelTopics = tModel.modelledTopics;
        for(int t = 0; t < modelTopics.size(); t++){
            topicWords.put(t, String.join("-", modelTopics.get(t).getTopWords(numWordId)));
        }
        return topicWords;
    }

    /**
     * Method launching the inference process for each documents to infer.
     */
    private void InferDocuments(){
        LogPrint.printNewStep("Inferring documents", 0);

        totalDocs = DocumentsToInfer.size();
        inferStartTime = System.currentTimeMillis();

        DocumentsToInfer.entrySet().forEach(this::inferDocument);

        LogPrint.printNewStep("Inferring documents", 0);
        LogPrint.printCompleteStep();
    }

    /**
     * Method inferring a given document.
     * @param document Document entry to infer.
     */
    private void inferDocument(Map.Entry<String, DocIOWrapper> document){

        if(docsProcessed % UPDATE_FREQUENCY == 0 && docsProcessed != 0) {
            long inferTimeTaken = (System.currentTimeMillis() - inferStartTime) / (long)1000;
            String timeTakenStr = "time: " + Math.floorDiv(inferTimeTaken, 60) + " m, " + inferTimeTaken % 60 + " s.";

            float inferTimeLeft = ((float) inferTimeTaken / (float) docsProcessed) * (totalDocs - docsProcessed);
            String timeToGoStr = "remaining (est.)): " + Math.floor(inferTimeLeft / 60) + " m, " + Math.floor(inferTimeLeft % 60) + " s.";

            float percentage = (((float) docsProcessed / (float) totalDocs) * 100);

            LogPrint.printNewStep("Inferred: " + docsProcessed +
                    " documents | % complete: " + (Math.round(percentage * 100f) / 100f) + "%", 1);

            LogPrint.printLog(timeTakenStr + " | " + timeToGoStr, 1);
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

    /**
     * Method launching the processes writing data on file.
     */
    private void SaveData(){
        LogPrint.printNewStep("Saving data", 0);
        // saving only the inferred docs in csv
        if(exportCSV) saveCSV();
        //
        if(mergeDocuments) saveMergedDocuments();
        if(mergeMainTopics) saveMergedTopics(true);
        if(mergeSubTopics) saveMergedTopics(false);
    }

    /**
     * Method writing the inference data on a CSV file.
     */
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
        try {
            // this will erase the content of the file before appending data to it.
            new FileWriter(file.getPath(), false).close();
        } catch (IOException e) {
            LogPrint.printNoteError("Error while saving similarity matrix\n");
            e.printStackTrace();
        }
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

    /**
     * Method generating the headers for the inference data CSV file.
     * @param appender CSV appender instance to add headers to.
     * @param topicLabels List of topic identifiers, using their top words.
     * @throws IOException If an error occurs with the CSV appender.
     */
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

    /**
     * Method writing all documents (the originally modelled and the inferred) in one single document JSON file.
     */
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

    /**
     * Method returning the number of documents marked as removed (eg, too short for modelling) from a list of documents.
     * @param docs List of documents to check.
     * @return Number of documents marked as removed.
     */
    private int getNDocsRemoved(ConcurrentHashMap<String, DocIOWrapper> docs){
        return docs.entrySet().stream()
                .map(d -> d.getValue())
                .filter(d -> d.isRemoved())
                .collect(Collectors.toList())
                .size();
    }

    /**
     * Method returning the number of documents marked as inferred in a list of documents.
     * @param docs List of documents to check.
     * @return Number of documents marked as inferred.
     */
    private int getNDocsInferred(ConcurrentHashMap<String, DocIOWrapper> docs){
        return docs.entrySet().stream()
                .map(d -> d.getValue())
                .filter(d -> d.isInferred())
                .collect(Collectors.toList())
                .size();
    }

    /**
     * Method writing a given list of topics on file, appending the inferred documents to it if necessary.
     * @param isMain Whether the topics to write on file are the main topics (true) or sub topics (false).
     */
    private void saveMergedTopics(boolean isMain) {
        ConcurrentHashMap<String, TopicIOWrapper> topics = isMain ? ModelMainTopics : ModelSubTopics;
        JSONObject metadata = isMain ? ModelMainTopicsMetadata : ModelSubTopicsMetadata;
        JSONArray similarities = isMain ? ModelMainTopicsSimilarities : ModelSubTopicsSimilarities;
        String output = isMain ? mainTopicsOutput : subTopicsOutput;
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

    /**
     * Method appending the inferred documents to the list of top documents in each topic of the list provided.
     * @param topics List of topics to append the document to.
     * @param isMain Whether the list of topics in the main one or not, to find the topic distribution in the documents.
     */
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

    /**
     * Method getting an integer field from a JSON object.
     * @param jsonObject JSON object to extract integer from.
     * @param name Field name where the integer is.
     * @param def Default integer value in case nothing is found.
     * @return The integer value.
     */
    private int getIntJson(JSONObject jsonObject, String name, int def){
        return Math.toIntExact((long) jsonObject.getOrDefault(name, (long) def));
    }
}
