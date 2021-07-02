package P3_TopicModelling;

import P0_Project.TopicModelExportModuleSpecs;
import PX_Data.DocIOWrapper;
import PX_Data.JSONIOWrapper;
import PX_Data.TopicIOWrapper;
import PY_Helper.LogPrint;
import de.siegmar.fastcsv.writer.CsvAppender;
import de.siegmar.fastcsv.writer.CsvWriter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class reading topic and document JSON files to export into several data files.
 * <br>
 * Possible files include:
 * - Main model JSON file, listing main topics with their top words and top documents (with data fields);
 * - (optional) Sub model JSON file, listing main topics with their top words and top documents (with data fields);
 * - (optional) Document CSV file(s), listing all documents, with their main and/or sub topic distributions.
 */
public class ExportTopicModel {

    /** Filename of the input main topic JSON file. */
    private String mainTopicsFile;
    /** Filename of the output main topic JSON file. */
    private String mainOutput;
    /** Boolean flag for exporting the main topic distributions on a CSV file. */
    private boolean exportMainTopicsCSV = false;
    /** Filename of the output main topic distributions CSV file. */
    private String mainOutputCSV;
    /** Boolean flag for exporting the sub topics a JSON file. */
    private boolean exportSubTopics = false;
    /** Filename of the input sub topic JSON file. */
    private String subTopicsFile;
    /** Filename of the output sub topic JSON file. */
    private String subOutput;
    /** Boolean flag for exporting the sub topic distributions on a CSV file. */
    private boolean exportSubTopicsCSV = false;
    /** Filename of the output sub topic distributions CSV file. */
    private String subOutputCSV;
    /** Boolean flag for exporting both main and sub topic distributions on a CSV file. */
    private boolean exportMergedTopicsCSV = false;
    /** Filename of the CSV output of both main and sub topic distributions. */
    private String outputCSV;
    /** Filename of the input documents JSON file. */
    private String documentsFile;
    /** Document fields to append for each documents in the output files. */
    private List<String> docFields;
    /** Number of top words to use to identify topics in the CSV outputs. */
    private int numWordId;

    /** List of documents. */
    private ConcurrentHashMap<String, DocIOWrapper> documents;
    /** Documents metadata. */
    private JSONObject documentsMetadata;
    /** Flag for models containing inferred documents. */
    private boolean hasInferredDocs;
    /** List of main topics. */
    private ConcurrentHashMap<String, TopicIOWrapper> mainTopics;
    /** Main topics metadata. */
    private JSONObject mainTopicsMetadata;
    /** List of sub topics. */
    private ConcurrentHashMap<String, TopicIOWrapper> subTopics;
    /** Sub topics metadata. */
    private JSONObject subTopicsMetadata;

    /**
     * Main method, reads the specification and launches the sub-methods in order.
     * @param exportSpecs Specifications.
     * @return String indicating the time taken to read the model JSON files and process the output files.
     */
    public static String ExportTopicModel(TopicModelExportModuleSpecs exportSpecs){

        LogPrint.printModuleStart("Topic Model Export");

        long startTime = System.currentTimeMillis();

        ExportTopicModel startClass = new ExportTopicModel();
        startClass.ProcessArguments(exportSpecs);
        startClass.LoadFiles();
        startClass.CheckDocFields();
        startClass.BuildModelData();
        startClass.SaveModel();

        long timeTaken = (System.currentTimeMillis() - startTime) / (long)1000;

        LogPrint.printModuleEnd("Topic Model Export");

        return "Model export: "+Math.floorDiv(timeTaken, 60) + " m, " + timeTaken % 60 + " s";

    }

    /**
     * Method processing the specification parameters.
     * @param exportSpecs Specifications.
     */
    private void ProcessArguments(TopicModelExportModuleSpecs exportSpecs){
        LogPrint.printNewStep("Processing arguments", 0);
        mainTopicsFile = exportSpecs.mainTopics;
        mainOutput = exportSpecs.mainOutput;
        exportMainTopicsCSV = exportSpecs.exportMainTopicsCSV;
        if(exportMainTopicsCSV){
            mainOutputCSV = exportSpecs.mainOutputCSV;
        }
        exportSubTopics = exportSpecs.exportSubTopics;
        if(exportSubTopics){
            subTopicsFile = exportSpecs.subTopics;
            subOutput = exportSpecs.subOutput;
            exportSubTopicsCSV = exportSpecs.exportSubTopicsCSV;
            if(exportSubTopicsCSV){
                subOutputCSV = exportSpecs.subOutputCSV;
            }
        }
        exportMergedTopicsCSV = exportSpecs.exportMergedTopicsCSV;
        if(exportMergedTopicsCSV){
            outputCSV = exportSpecs.outputCSV;
        }
        documentsFile = exportSpecs.documents;
        docFields = Arrays.asList(exportSpecs.docFields);
        numWordId = exportSpecs.numWordId;
        LogPrint.printCompleteStep();
        // Logging options
        if(exportMainTopicsCSV) LogPrint.printNote("Exporting main topic model to CSV");
        if(exportSubTopics) LogPrint.printNote("Exporting sub topics");
        if(exportSubTopicsCSV) LogPrint.printNote("Exporting sub topics to CSV");
        if(exportMergedTopicsCSV) LogPrint.printNote("Exporting main and sub topics merged in one CSV file");
        if(exportMainTopicsCSV || exportSubTopicsCSV || exportMergedTopicsCSV) LogPrint.printNote("Identifying topics with "+numWordId+" labels");
    }

    /**
     * Method loading all input data files.
     */
    private void LoadFiles(){
        LogPrint.printNewStep("Loading data", 0);
        JSONObject input = JSONIOWrapper.LoadJSON(documentsFile, 1);
        documentsMetadata = (JSONObject) input.get("metadata");
        JSONArray docs = (JSONArray) input.get("documents");
        documents = new ConcurrentHashMap<>();
        for(JSONObject docEntry: (Iterable<JSONObject>) docs){
            DocIOWrapper doc = new DocIOWrapper(docEntry);
            documents.put(doc.getId(), doc);
        }
        hasInferredDocs = documents.values().stream()
                .anyMatch(DocIOWrapper::isInferred);
        input = JSONIOWrapper.LoadJSON(mainTopicsFile, 1);
        mainTopicsMetadata = (JSONObject) input.get("metadata");
        mainTopics = new ConcurrentHashMap<>();
        for(JSONObject topicEntry: (Iterable<JSONObject>) input.get("topics")){
            TopicIOWrapper topic = new TopicIOWrapper(topicEntry);
            mainTopics.put(topic.getId(), topic);
        }
        if(exportSubTopics){
            input = JSONIOWrapper.LoadJSON(subTopicsFile, 1);
            subTopicsMetadata = (JSONObject) input.get("metadata");
            subTopics = new ConcurrentHashMap<>();
            for(JSONObject topicEntry: (Iterable<JSONObject>) input.get("topics")){
                TopicIOWrapper topic = new TopicIOWrapper(topicEntry);
                subTopics.put(topic.getId(), topic);
            }
        }
    }

    /**
     * Method checking the presence of document data field in the list of documents.
     */
    private void CheckDocFields(){
        LogPrint.printNewStep("Checking document fields", 0);
        HashMap<String, Integer> missing = new HashMap<>();
        for(String f: docFields){
            missing.put(f, 0);
        }
        for(Map.Entry<String, DocIOWrapper> doc: documents.entrySet()){
            for(String f: docFields){
                if(!doc.getValue().hasData(f)){
                    int prev = missing.get(f);
                    missing.put(f, prev+1);
                }
            }
        }
        LogPrint.printCompleteStep();
        for(Map.Entry<String, Integer> m: missing.entrySet()){
            if(m.getValue() > 0){
                LogPrint.printNote("Field "+m.getKey()+" missing in "+m.getValue()+" documents");
            }
        }
    }

    /**
     * Method adding document data to the top documents in each topics.
     */
    private void BuildModelData(){
        LogPrint.printNewStep("Building model", 0);
        for(Map.Entry<String, TopicIOWrapper> t: mainTopics.entrySet()){
            TopicIOWrapper topic = t.getValue();
            for(TopicIOWrapper.JSONTopicWeight d: topic.getDocs()){
                DocIOWrapper doc = documents.get(d.ID);
                for(String key: docFields){
                    d.addDataEntry(key, doc.getData(key));
                }
                d.addDataEntry("wordCount", Integer.toString(doc.getNumLemmas()));
            }
        }
        if(exportSubTopics){
            for(Map.Entry<String, TopicIOWrapper> t: subTopics.entrySet()){
                TopicIOWrapper topic = t.getValue();
                for(TopicIOWrapper.JSONTopicWeight d: topic.getDocs()){
                    DocIOWrapper doc = documents.get(d.ID);
                    for(String key: docFields){
                        d.addDataEntry(key, doc.getData(key));
                    }
                    d.addDataEntry("wordCount", Integer.toString(doc.getNumLemmas()));
                }
            }
        }
        LogPrint.printCompleteStep();
    }

    /**
     * Method writing the model(s) on JSON file(s).
     * Each model is a list of topics, each topic has top words and top documents with data fields.
     */
    private void SaveModel(){
        LogPrint.printNewStep("Saving model", 0);
        JSONObject root = new JSONObject();
        for(Map.Entry<String, Object> m: JSONIOWrapper.getJSONObjectMap(documentsMetadata).entrySet()){
            mainTopicsMetadata.put(m.getKey(), m.getValue());
        }
        root.put("metadata", mainTopicsMetadata);
        JSONArray topics = new JSONArray();
        for(Map.Entry<String, TopicIOWrapper> t: mainTopics.entrySet()){
            topics.add(t.getValue().toJSON());
        }
        root.put("topics", topics);
        JSONIOWrapper.SaveJSON(root, mainOutput, 1);
        if(exportSubTopics){
            root = new JSONObject();
            for(Map.Entry<String, Object> m: JSONIOWrapper.getJSONObjectMap(documentsMetadata).entrySet()){
                subTopicsMetadata.put(m.getKey(), m.getValue());
            }
            root.put("metadata", subTopicsMetadata);
            topics = new JSONArray();
            for(Map.Entry<String, TopicIOWrapper> t: subTopics.entrySet()){
                topics.add(t.getValue().toJSON());
            }
            root.put("topics", topics);
            JSONIOWrapper.SaveJSON(root, subOutput, 1);
        }
        SaveCSV();
    }

    /**
     * Method launching the writing process for CSV exports.
     */
    private void SaveCSV(){
        if(exportMainTopicsCSV) {
            saveCSV(mainOutputCSV, mainTopics, true);
        }
        if(exportSubTopicsCSV){
            saveCSV(subOutputCSV, subTopics, false);
        }
        if(exportMergedTopicsCSV){
            saveMergedTopicsCSV();
        }
    }

    /**
     * Method writing a topic distributions, for every document, on a CSV file.
     * @param filename Filename of the CSV file to export to.
     * @param topics List of topics to include.
     * @param isMain Whether the list of topics is from the main model (true) or sub model (false).
     */
    private void saveCSV(String filename, ConcurrentHashMap<String, TopicIOWrapper> topics, boolean isMain){
        LogPrint.printNewStep("Saving "+filename, 1);
        String[] topicsLabels = new String[topics.size()];
        for (Map.Entry<String, TopicIOWrapper> t : topics.entrySet()) {
            String prefix = isMain ? "_mainTopic_" : "_subTopic_";
            topicsLabels[t.getValue().getIndex()] = prefix+t.getValue().getLabelString(numWordId);
        }
        File file = new File(filename);
        file.getParentFile().mkdirs();
        CsvWriter csvWriter = new CsvWriter();
        csvWriter.setAlwaysDelimitText(true);
        try(CsvAppender csvAppender = csvWriter.append(file, StandardCharsets.UTF_8)){
            createHeader(csvAppender, topicsLabels);
            for(Map.Entry<String, DocIOWrapper> d: documents.entrySet()){
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

    /**
     * Method creating the header for a CSV file.
     * @param appender CSV appender instance to add headers to.
     * @param topicLabels List of topic identifiers, using their top words.
     * @throws IOException If an error occurs with the CSV appender.
     */
    private void createHeader(CsvAppender appender, String[] topicLabels) throws IOException{
        appender.appendField("_docId");
        for(String f: docFields){
            appender.appendField(f);
        }
        appender.appendField("_wordCount");
        appender.appendField("_inModel");
        if(hasInferredDocs) appender.appendField("_inferred");
        for(String l:topicLabels){
            appender.appendField(l);
        }
        appender.endLine();
    }

    /**
     * Method writing both main and sub topic distributions, for every document, on a CSV file.
     */
    private void saveMergedTopicsCSV(){
        LogPrint.printNewStep("Saving "+outputCSV, 1);
        int size = exportSubTopics ? mainTopics.size()+subTopics.size() : mainTopics.size();
        String[] topicsLabels = new String[size];
        for (Map.Entry<String, TopicIOWrapper> t : mainTopics.entrySet()) {
            topicsLabels[t.getValue().getIndex()] = "_mainTopic_"+t.getValue().getLabelString(numWordId);
        }
        if(exportSubTopics){
            int offset = mainTopics.size();
            for (Map.Entry<String, TopicIOWrapper> t : subTopics.entrySet()) {
                topicsLabels[t.getValue().getIndex()+offset] = "_subTopic_"+t.getValue().getLabelString(numWordId);
            }
        }
        File file = new File(outputCSV);
        file.getParentFile().mkdirs();
        CsvWriter csvWriter = new CsvWriter();
        csvWriter.setAlwaysDelimitText(true);
        try(CsvAppender csvAppender = csvWriter.append(file, StandardCharsets.UTF_8)){
            createHeader(csvAppender, topicsLabels);
            for(Map.Entry<String, DocIOWrapper> d: documents.entrySet()){
                DocIOWrapper doc = d.getValue();
                csvAppender.appendField(doc.getId());
                for(String f: docFields){
                    csvAppender.appendField(doc.getData(f));
                }
                csvAppender.appendField(Integer.toString(doc.getNumLemmas()));
                csvAppender.appendField(Boolean.toString(!doc.isRemoved()));
                if(hasInferredDocs) csvAppender.appendField(Boolean.toString(doc.isInferred()));
                if(!doc.isRemoved()){
                    for(double weight: doc.getMainTopicDistribution()){
                        csvAppender.appendField(Double.toString(weight));
                    }
                    if(exportSubTopics){
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
