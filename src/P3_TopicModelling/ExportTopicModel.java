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

public class ExportTopicModel {

    private String mainTopicsFile;
    private String mainOutput;
    private boolean exportMainTopicsCSV = false;
    private String mainOutputCSV;
    private boolean exportSubTopics = false;
    private String subTopicsFile;
    private String subOutput;
    private boolean exportSubTopicsCSV = false;
    private String subOutputCSV;
    private String documentsFile;
    private List<String> docFields;
    private int numWordId;

    private ConcurrentHashMap<String, DocIOWrapper> documents;
    private JSONObject documentsMetadata;
    private ConcurrentHashMap<String, TopicIOWrapper> mainTopics;
    private JSONObject mainTopicsMetadata;
    private ConcurrentHashMap<String, TopicIOWrapper> subTopics;
    private JSONObject subTopicsMetadata;

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
        documentsFile = exportSpecs.documents;
        docFields = Arrays.asList(exportSpecs.docFields);
        numWordId = exportSpecs.numWordId;
        LogPrint.printCompleteStep();
        // Logging options
        if(exportMainTopicsCSV) LogPrint.printNote("exporting main topic model to CSV");
        if(exportSubTopics) LogPrint.printNote("exporting sub topics");
        if(exportSubTopicsCSV) LogPrint.printNote("exporting sub topics to CSV");
        if(exportMainTopicsCSV || exportSubTopicsCSV) LogPrint.printNote("identifying topics with "+numWordId+" labels");
    }

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
        // LogPrint.printClose(" Done.");
    }

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

    private void SaveCSV(){
        if(exportMainTopicsCSV) {
            LogPrint.printNewStep("Saving "+mainOutputCSV, 1);
            String[] mainTopicsLabels = new String[mainTopics.size()];
            for (Map.Entry<String, TopicIOWrapper> t : mainTopics.entrySet()) {
                mainTopicsLabels[t.getValue().getIndex()] = t.getValue().getLabelString(numWordId);
            }
            File file = new File(mainOutputCSV);
            CsvWriter csvWriter = new CsvWriter();
            csvWriter.setAlwaysDelimitText(true);
            try(CsvAppender csvAppender = csvWriter.append(file, StandardCharsets.UTF_8)){
                createHeader(csvAppender, mainTopicsLabels);
                for(Map.Entry<String, DocIOWrapper> d: documents.entrySet()){
                    DocIOWrapper doc = d.getValue();
                    csvAppender.appendField(doc.getId());
                    for(String f: docFields){
                        csvAppender.appendField(doc.getData(f));
                    }
                    csvAppender.appendField(Integer.toString(doc.getNumLemmas()));
                    csvAppender.appendField(Boolean.toString(!doc.isRemoved()));
                    csvAppender.appendField(doc.getRemoveReason());
                    if(!doc.isRemoved()){
                        for(double weight: doc.getMainTopicDistribution()){
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
        if(exportSubTopicsCSV){
            LogPrint.printNewStep("Saving "+subOutputCSV, 1);
            String[] subTopicsLabels = new String[subTopics.size()];
            for (Map.Entry<String, TopicIOWrapper> t : subTopics.entrySet()) {
                subTopicsLabels[t.getValue().getIndex()] = t.getValue().getLabelString(numWordId);
            }
            File file = new File(subOutputCSV);
            CsvWriter csvWriter = new CsvWriter();
            csvWriter.setAlwaysDelimitText(true);
            try(CsvAppender csvAppender = csvWriter.append(file, StandardCharsets.UTF_8)){
                createHeader(csvAppender, subTopicsLabels);
                for(Map.Entry<String, DocIOWrapper> d: documents.entrySet()){
                    DocIOWrapper doc = d.getValue();
                    csvAppender.appendField(doc.getId());
                    for(String f: docFields){
                        csvAppender.appendField(doc.getData(f));
                    }
                    csvAppender.appendField(Integer.toString(doc.getNumLemmas()));
                    csvAppender.appendField(Boolean.toString(!doc.isRemoved()));
                    csvAppender.appendField(doc.getRemoveReason());
                    if(!doc.isRemoved()){
                        for(double weight: doc.getSubTopicDistribution()) {
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
    }

    private void createHeader(CsvAppender appender, String[] topicLabels) throws IOException{
        appender.appendField("docId");
        for(String f: docFields){
            appender.appendField(f);
        }
        appender.appendField("wordCount");
        appender.appendField("includedInModel");
        appender.appendField("reasonForRemoval");
        for(String l:topicLabels){
            appender.appendField(l);
        }
        appender.endLine();
    }

}
