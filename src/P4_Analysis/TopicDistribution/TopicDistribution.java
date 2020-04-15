package P4_Analysis.TopicDistribution;

import P0_Project.DistribSpecs;
import P0_Project.TopicDistribModuleSpecs;
import PX_Data.DocIOWrapper;
import PX_Data.JSONIOWrapper;
import PX_Data.TopicIOWrapper;
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

    public static void Distribute(TopicDistribModuleSpecs distribSpecs){
        System.out.println( "**********************************************************\n" +
                            "* STARTING Topic Distribution !                          *\n" +
                            "**********************************************************\n");

        TopicDistribution startClass = new TopicDistribution();
        startClass.ProcessArguments(distribSpecs);
        startClass.LoadDocuments();
        startClass.ValidateDocumentData();
        startClass.GetUniqueFields();
        startClass.InitialiseDistributions();
        startClass.CalculateDistributions();
        startClass.SaveDistributions();

        // TODO: Save distribution
//         // create sub groups of topics (and there similarity) based on main topic assignment
//         startClass.GroupTopics();
//         // Run Agglomerative clustering on topics groups
//         startClass.ClusterTopics();
// //        startClass.IndexLabels();
//         startClass.SaveClusters();

        System.out.println( "**********************************************************\n" +
                            "* Topic Distribution COMPLETE !                          *\n" +
                            "**********************************************************\n");
    }

    private void ProcessArguments(TopicDistribModuleSpecs distribSpecs){
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
    }

    private void LoadDocuments(){
        JSONObject input = JSONIOWrapper.LoadJSON(documentsFile);
        JSONArray docs = (JSONArray) input.get("documents");
        documents = new ConcurrentHashMap<>();
        for(JSONObject docEntry: (Iterable<JSONObject>) docs){
            DocIOWrapper doc = new DocIOWrapper(docEntry);
            documents.put(doc.getId(), doc);
        }
        input = JSONIOWrapper.LoadJSON(mainTopicsFile);
        mainTopicsMetadata = (JSONObject) input.get("metadata");
        mainTopicsSimilarities = (JSONArray) input.get("similarities");
        mainTopics = new ConcurrentHashMap<>();
        for(JSONObject topicEntry: (Iterable<JSONObject>) input.get("topics")){
            TopicIOWrapper topic = new TopicIOWrapper(topicEntry);
            mainTopics.put(topic.getIndex(), topic);
        }
        if(distributeSubTopics){
            input = JSONIOWrapper.LoadJSON(subTopicsFile);
            subTopicsMetadata = (JSONObject) input.get("metadata");
            subTopicsSimilarities = (JSONArray) input.get("similarities");
            subTopics = new ConcurrentHashMap<>();
            for(JSONObject topicEntry: (Iterable<JSONObject>) input.get("topics")){
                TopicIOWrapper topic = new TopicIOWrapper(topicEntry);
                subTopics.put(topic.getIndex(), topic);
            }
        }
        System.out.println("Documents Loaded!");
    }

    private void ValidateDocumentData(){
        System.out.println("Checking Document Data ...");
        Set<String> ignoreFields = new HashSet<>();
        Set<String> ignoreValues = new HashSet<>();
        for(Map.Entry<String, DocIOWrapper> documentEntry: documents.entrySet()){
            DocIOWrapper document = documentEntry.getValue();
            if(!document.isRemoved()){
                HashMap<String, String> docData = document.getDocData();
                for(Distribution distrib: distributions){
                    if(distrib.getFieldName().length() > 0) {
                        if (!docData.containsKey(distrib.getFieldName())) {
                            System.out.println("Document Field " + distrib.getFieldName() + " not valid (not found): will be ignored from distribution process!");
                            ignoreFields.add(distrib.getFieldName());
                        }
                    }
                    if(distrib.getValueName().length() > 0) {
                        if (!docData.containsKey(distrib.getValueName())) {
                            System.out.println("Document Value " + distrib.getValueName() + " not valid (not found): will be ignored from distribution process!");
                            ignoreValues.add(distrib.getValueName());
                        } else {
                            try {
                                Double.parseDouble(document.getData(distrib.getValueName()));
                            } catch (NumberFormatException e) {
                                System.out.println("Document Value " + distrib.getValueName() + " not valid (not a number): will be ignored from distribution process!");
                                ignoreValues.add(distrib.getValueName());
                            }
                        }
                    }
                }
            }
        }
        ignoreFields.forEach(f -> distributions.removeIf(d -> d.getFieldName().equals(f)));
        ignoreValues.forEach(v -> distributions.removeIf(d -> d.getValueName().equals(v)));
        if(distributions.size() < 1){
            System.out.println("Error: No distributions left!");
            System.exit(1);
        }
        System.out.println("Data Checked!");
        if(ignoreFields.size() > 0){
            System.out.println(ignoreFields.size()+" fields will be ignored: "+ String.join(", ", ignoreFields));
        }
        if(ignoreValues.size() > 0){
            System.out.println(ignoreValues.size()+" values will be ignored: "+ String.join(", ", ignoreValues));
        }
    }

    private void GetUniqueFields(){
        System.out.println("Finding Unique Field Values ...");
        for(Map.Entry<String, DocIOWrapper> documentEntry: documents.entrySet()){
            DocIOWrapper doc = documentEntry.getValue();
            for(Distribution distrib: distributions){
                String fieldValue = distrib.getFieldName().length() > 0 ? doc.getData(distrib.getFieldName()) : "";
                distrib.addUniqueFieldValues(fieldValue);
            }
        }
        System.out.println("Unique Field Values Identified!");
    }

    private void InitialiseDistributions(){
        System.out.println("Initialising Distributions ...");
        for(Distribution distrib: distributions){
            if(distributeSubTopics){
                distrib.initialiseDistributions(mainTopics.size(), subTopics.size());
            } else {
                distrib.initialiseDistribution(mainTopics.size());
            }
        }
        System.out.println("Distributions Initialisation Complete!");
    }

    private void CalculateDistributions(){
        System.out.println("Calculating Distributions ...");
        for(DocIOWrapper doc: documents.values()){
            if(!doc.isRemoved()){
                for(Distribution distrib: distributions){
                    distrib.updateDistributions(doc);
                }
            }
        }
        System.out.println("Distributions Calculation Complete!");
    }

    private void SaveDistributions(){
        System.out.println("Saving Distributions ...");
        // boolean saveTopicFiles = false;
        for(Distribution distrib: distributions){
            // saveTopicFiles = distrib.saveInTopics();
            if(distributeSubTopics) distrib.saveDistributions(mainTopics, subTopics);
            else distrib.saveDistributions(mainTopics);
        }
        // if(saveTopicFiles){
            saveTopics();
        // }
        System.out.println("Distributions Saved!");
    }

    private void saveTopics(){
        System.out.println("Saving Topics...");
        JSONObject root = new JSONObject();
        root.put("metadata", mainTopicsMetadata);
        root.put("similarities", mainTopicsSimilarities);
        JSONArray topics = new JSONArray();
        for(Map.Entry<Integer, TopicIOWrapper> entry: mainTopics.entrySet()){
            topics.add(entry.getValue().toJSON());
        }
        root.put("topics", topics);
        JSONIOWrapper.SaveJSON(root, mainOutput);
        if(distributeSubTopics){
            root = new JSONObject();
            root.put("metadata", subTopicsMetadata);
            root.put("similarities", subTopicsSimilarities);
            topics = new JSONArray();
            for(Map.Entry<Integer, TopicIOWrapper> entry: subTopics.entrySet()){
                topics.add(entry.getValue().toJSON());
            }
            root.put("topics", topics);
            JSONIOWrapper.SaveJSON(root, subOutput);
        }
        System.out.println("Topics Saved!");
    }
}
