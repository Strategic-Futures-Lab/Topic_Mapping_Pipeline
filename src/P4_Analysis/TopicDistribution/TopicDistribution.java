package P4_Analysis.TopicDistribution;

import P0_Project.DistribSpecs;
import P0_Project.TopicDistribModuleSpecs;
import PX_Data.DocIOWrapper;
import PX_Data.JSONIOWrapper;
import PX_Data.TopicIOWrapper;
import PY_Helper.LogPrint;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TopicDistribution {

    private String documentsFile;
    private String mainTopicsFile;
    private String mainOutput;
    private String subTopicsFile;
    private boolean distributeSubTopics;
    private String subOutput;

    private ConcurrentHashMap<String, DocIOWrapper> documents;
    private ConcurrentHashMap<Integer, TopicIOWrapper> mainTopics;
    private JSONObject mainTopicsMetadata;
    private JSONArray mainTopicsSimilarities;
    private ConcurrentHashMap<Integer, TopicIOWrapper> subTopics;
    private JSONObject subTopicsMetadata;
    private JSONArray subTopicsSimilarities;

    private List<Distribution> distributions;

    // private HashMap<String, HashSet<String>> uniqueFields;

    // private ConcurrentHashMap<Integer, TopicDistrib> mainTopicDistrib;
    // private ConcurrentHashMap<Integer, TopicDistrib> subTopicDistrib;

    public static String Distribute(TopicDistribModuleSpecs distribSpecs){

        LogPrint.printModuleStart("Topic distribution");

        long startTime = System.currentTimeMillis();

        TopicDistribution startClass = new TopicDistribution();
        startClass.ProcessArguments(distribSpecs);
        startClass.LoadDocuments();
        startClass.ValidateDocumentData();
        startClass.GetUniqueFields();
        startClass.InitialiseDistributions();
        startClass.CalculateDistributions();
        startClass.SaveDistributions();

        long timeTaken = (System.currentTimeMillis() - startTime) / (long)1000;

        LogPrint.printModuleEnd("Topic distribution");

        return "Topic distribution: "+Math.floorDiv(timeTaken, 60) + " m, " + timeTaken % 60 + " s";

    }

    private void ProcessArguments(TopicDistribModuleSpecs distribSpecs){
        LogPrint.printNewStep("Processing arguments", 0);
        documentsFile = distribSpecs.documents;
        mainTopicsFile = distribSpecs.mainTopics;
        mainOutput = distribSpecs.mainOutput;
        distributeSubTopics = distribSpecs.distributeSubTopics;
        if(distributeSubTopics){
            subTopicsFile = distribSpecs.subTopics;
            subOutput = distribSpecs.subOutput;
        }
        distributions = new ArrayList<>();
        for(DistribSpecs specs: distribSpecs.fields){
            distributions.add(new Distribution(specs));
        }
        LogPrint.printCompleteStep();
        if(distributeSubTopics) LogPrint.printNote("Distributing sub topics");
        LogPrint.printNote(distributions.size()+" distributions:");
        for(Distribution d: distributions){
            LogPrint.printNote(d.getDescription(), 1);
        }
    }

    private void LoadDocuments(){
        LogPrint.printNewStep("Loading data", 0);
        JSONObject input = JSONIOWrapper.LoadJSON(documentsFile, 1);
        JSONArray docs = (JSONArray) input.get("documents");
        documents = new ConcurrentHashMap<>();
        for(JSONObject docEntry: (Iterable<JSONObject>) docs){
            DocIOWrapper doc = new DocIOWrapper(docEntry);
            documents.put(doc.getId(), doc);
        }
        input = JSONIOWrapper.LoadJSON(mainTopicsFile, 1);
        mainTopicsMetadata = (JSONObject) input.get("metadata");
        mainTopicsSimilarities = (JSONArray) input.get("similarities");
        mainTopics = new ConcurrentHashMap<>();
        for(JSONObject topicEntry: (Iterable<JSONObject>) input.get("topics")){
            TopicIOWrapper topic = new TopicIOWrapper(topicEntry);
            mainTopics.put(topic.getIndex(), topic);
        }
        if(distributeSubTopics){
            input = JSONIOWrapper.LoadJSON(subTopicsFile, 1);
            subTopicsMetadata = (JSONObject) input.get("metadata");
            subTopicsSimilarities = (JSONArray) input.get("similarities");
            subTopics = new ConcurrentHashMap<>();
            for(JSONObject topicEntry: (Iterable<JSONObject>) input.get("topics")){
                TopicIOWrapper topic = new TopicIOWrapper(topicEntry);
                subTopics.put(topic.getIndex(), topic);
            }
        }
        // System.out.println("Documents Loaded!");
    }

    private void ValidateDocumentData(){
        LogPrint.printNewStep("Checking document data", 0);
        Set<String> ignoreFields = new HashSet<>();
        Set<String> ignoreValues = new HashSet<>();
        List<String> invalids = new ArrayList<>();
        for(Map.Entry<String, DocIOWrapper> documentEntry: documents.entrySet()){
            DocIOWrapper document = documentEntry.getValue();
            if(!document.isRemoved()){
                HashMap<String, String> docData = document.getDocData();
                for(Distribution distrib: distributions){
                    if(distrib.getFieldName().length() > 0) {
                        if (!docData.containsKey(distrib.getFieldName()) && !ignoreFields.contains(distrib.getFieldName())) {
                            invalids.add("Field " + distrib.getFieldName() + " not found: will be ignored from distribution process");
                            ignoreFields.add(distrib.getFieldName());
                        }
                    }
                    if(distrib.getValueName().length() > 0) {
                        if (!docData.containsKey(distrib.getValueName()) && !ignoreValues.contains(distrib.getValueName())) {
                            invalids.add("Value " + distrib.getValueName() + " not found: will be ignored from distribution process");
                            ignoreValues.add(distrib.getValueName());
                        } else {
                            try {
                                Double.parseDouble(document.getData(distrib.getValueName()));
                            } catch (NumberFormatException e) {
                                if(!ignoreValues.contains(distrib.getValueName())) {
                                    invalids.add("Value " + distrib.getValueName() + " not a number: will be ignored from distribution process");
                                    ignoreValues.add(distrib.getValueName());
                                }
                            }
                        }
                    }
                }
            }
        }
        ignoreFields.forEach(f -> distributions.removeIf(d -> d.getFieldName().equals(f)));
        ignoreValues.forEach(v -> distributions.removeIf(d -> d.getValueName().equals(v)));
        if(distributions.size() < 1){
            LogPrint.printNoteError("Error: no distributions left\n");
            System.exit(1);
        }
        LogPrint.printCompleteStep();
        if(ignoreFields.size() > 0){
            LogPrint.printNote(ignoreFields.size()+" fields will be ignored");
        }
        if(ignoreValues.size() > 0){
            LogPrint.printNote(ignoreValues.size()+" values will be ignored");
        }
        for(String i: invalids){
            LogPrint.printNote(i, 1);
        }
    }

    private void GetUniqueFields(){
        LogPrint.printNewStep("Finding unique field entries", 0);
        for(Map.Entry<String, DocIOWrapper> documentEntry: documents.entrySet()){
            DocIOWrapper doc = documentEntry.getValue();
            for(Distribution distrib: distributions){
                String fieldValue = distrib.getFieldName().length() > 0 ? doc.getData(distrib.getFieldName()) : "";
                distrib.addUniqueFieldValues(fieldValue);
            }
        }
        LogPrint.printCompleteStep();
    }

    private void InitialiseDistributions(){
        LogPrint.printNewStep("Initialising distributions", 0);
        for(Distribution distrib: distributions){
            if(distributeSubTopics){
                distrib.initialiseDistributions(mainTopics.size(), subTopics.size());
            } else {
                distrib.initialiseDistribution(mainTopics.size());
            }
        }
        LogPrint.printCompleteStep();
    }

    private void CalculateDistributions(){
        LogPrint.printNewStep("Calculating distributions", 0);
        for(DocIOWrapper doc: documents.values()){
            if(!doc.isRemoved()){
                for(Distribution distrib: distributions){
                    distrib.updateDistributions(doc);
                }
            }
        }
        LogPrint.printCompleteStep();
    }

    private void SaveDistributions(){
        LogPrint.printNewStep("Saving distributions", 0);
        // boolean saveTopicFiles = false;
        for(Distribution distrib: distributions){
            // saveTopicFiles = distrib.saveInTopics();
            if(distributeSubTopics) distrib.saveDistributions(mainTopics, subTopics);
            else distrib.saveDistributions(mainTopics);
        }
        // if(saveTopicFiles){
            saveTopics();
        // }
        // System.out.println("Distributions Saved!");
    }

    private void saveTopics(){
        // System.out.println("Saving Topics...");
        JSONObject root = new JSONObject();
        root.put("metadata", mainTopicsMetadata);
        root.put("similarities", mainTopicsSimilarities);
        JSONArray topics = new JSONArray();
        for(Map.Entry<Integer, TopicIOWrapper> entry: mainTopics.entrySet()){
            topics.add(entry.getValue().toJSON());
        }
        root.put("topics", topics);
        JSONIOWrapper.SaveJSON(root, mainOutput, 1);
        if(distributeSubTopics){
            root = new JSONObject();
            root.put("metadata", subTopicsMetadata);
            root.put("similarities", subTopicsSimilarities);
            topics = new JSONArray();
            for(Map.Entry<Integer, TopicIOWrapper> entry: subTopics.entrySet()){
                topics.add(entry.getValue().toJSON());
            }
            root.put("topics", topics);
            JSONIOWrapper.SaveJSON(root, subOutput, 1);
        }
        // System.out.println("Topics Saved!");
    }
}
