package P4_Analysis.TopicClustering;

import P0_Project.TopicClusterModuleSpecs;
import PX_Data.JSONIOWrapper;
import PX_Data.TopicIOWrapper;
import PY_Helper.LogPrint;
import org.apache.commons.logging.Log;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TopicClustering {

    public static class TopicGroup{

        public ConcurrentHashMap<Integer, TopicIOWrapper> topics;
        public SimilarityMatrix similarities;
        public List<AgglomerativeClustering.ClusterRow> linkageTable;

        public TopicGroup(ConcurrentHashMap<String, TopicIOWrapper> topics, SimilarityMatrix similarities){
            this.topics = new ConcurrentHashMap<>();
            for(Map.Entry<String, TopicIOWrapper> topic: topics.entrySet()){
                TopicIOWrapper t = topic.getValue();
                int idx = (t.getGroupTopicIndex() == -1) ? t.getIndex() : t.getGroupTopicIndex();
                this.topics.put(idx, t);
            }
            this.similarities = similarities;
        }

        public void addClusterIds(int nClusters){
            LogPrint.printNewStep("Adding cluster ids", 2);
            int linkageSplitHeight = linkageTable.size() - nClusters;
            if(linkageTable.size() > 0){
                exploreLinkageNode(linkageTable.size()-1, 0, linkageSplitHeight);
            } else {
                if(topics.size() > 0) topics.get(0).setClusterId("0");
            }
            LogPrint.printCompleteStep();
        }

        private int exploreLinkageNode(int nodeIndex, int clusterNumber, int linkageSplitHeight){
            AgglomerativeClustering.ClusterRow node = linkageTable.get(nodeIndex);
            int newClusterNum = (nodeIndex > linkageSplitHeight) ? clusterNumber+1 : clusterNumber;
            if(node.Node1 < topics.size()){ // one leaf
                topics.get(node.Node1).setClusterId(Integer.toString(clusterNumber));
                if(node.Node2 < topics.size()){ // two leaves
                    topics.get(node.Node2).setClusterId(Integer.toString(clusterNumber));
                } else { // second node not leaf
                    return exploreLinkageNode(node.Node2 - topics.size(), newClusterNum, linkageSplitHeight);
                }
            } else if(node.Node2 < topics.size()){ // one leaf (first node not leaf)
                topics.get(node.Node2).setClusterId(Integer.toString(clusterNumber));
                return exploreLinkageNode(node.Node1 - topics.size(), newClusterNum, linkageSplitHeight);
            } else { // no leaf
                int prevClusterNum = exploreLinkageNode(node.Node1 - topics.size(), clusterNumber, linkageSplitHeight);
                newClusterNum = (nodeIndex > linkageSplitHeight) ? prevClusterNum+1 : prevClusterNum;
                return exploreLinkageNode(node.Node2 - topics.size(), newClusterNum, linkageSplitHeight);
            }
            return clusterNumber;
        }

        public JSONObject toJSON(){
            JSONObject root = new JSONObject();
            JSONArray t = new JSONArray();
            for(Map.Entry<Integer, TopicIOWrapper> entry: topics.entrySet()){
                t.add(entry.getValue().toJSON());
            }
            root.put("topics", t);
            root.put("similarities", similarities.toJSON());
            JSONArray l = new JSONArray();
            for(AgglomerativeClustering.ClusterRow row : linkageTable){
                JSONObject linkageTableRow = new JSONObject();
                linkageTableRow.put("node1", row.Node1);
                linkageTableRow.put("node2", row.Node2);
                linkageTableRow.put("distance", row.Distance);
                l.add(linkageTableRow);
            }
            root.put("linkageTable", l);
            return root;
        }
    }

    private String mainTopicsFile;
    private String mainOutput;
    private int mainNClusters;
    private AgglomerativeClustering.LINKAGE_TYPE linkageMethod;
    private boolean clusterSubTopics;
    private String subTopicsFile;
    private String subOutput;

    private JSONObject mainTopicsMetadata;
    private ConcurrentHashMap<String, TopicIOWrapper> mainTopics;
    private SimilarityMatrix mainSimilarityMatrix;
    private JSONObject subTopicsMetadata;
    private ConcurrentHashMap<String, TopicIOWrapper> subTopics;
    private SimilarityMatrix subSimilarityMatrix;

    private TopicGroup mainTopicGroup;
    private HashMap<String, TopicGroup> subTopicGroups;

    public static String Cluster(TopicClusterModuleSpecs clusterSpecs){

        LogPrint.printModuleStart("Topic cluster");

        long startTime = System.currentTimeMillis();

        TopicClustering startClass = new TopicClustering();
        startClass.ProcessArguments(clusterSpecs);
        startClass.LoadTopics();
        // create sub groups of topics (and there similarity) based on main topic assignment
        startClass.GroupTopics();
        // Run Agglomerative clustering on topics groups
        startClass.ClusterTopics();
        startClass.SaveClusters();

        long timeTaken = (System.currentTimeMillis() - startTime) / (long)1000;

        LogPrint.printModuleEnd("Topic cluster");

        return "Topic clustering: "+Math.floorDiv(timeTaken, 60) + " m, " + timeTaken % 60 + " s";

    }

    private void ProcessArguments(TopicClusterModuleSpecs clusterSpecs){
        LogPrint.printNewStep("Processing arguments", 0);
        switch (clusterSpecs.linkageMethod){
            case "max":
                linkageMethod = AgglomerativeClustering.LINKAGE_TYPE.MAX;
                break;
            case "min":
                linkageMethod = AgglomerativeClustering.LINKAGE_TYPE.MIN;
                break;
            case "avg":
                linkageMethod = AgglomerativeClustering.LINKAGE_TYPE.AVERAGE;
                break;
            default:
                LogPrint.printNoteError("Error: Linkage method provided not recognised!\nUse one of the following: min, max, or avg\n");
                System.exit(1);
        }
        mainTopicsFile = clusterSpecs.mainTopics;
        mainOutput = clusterSpecs.mainOutput;
        mainNClusters = clusterSpecs.clusters;
        clusterSubTopics = clusterSpecs.clusterSubTopics;
        if(clusterSubTopics){
            subTopicsFile = clusterSpecs.subTopics;
            subOutput = clusterSpecs.subOutput;
        }
        LogPrint.printCompleteStep();
        LogPrint.printNote("Producing "+mainNClusters+" clusters using "+linkageMethod+" linkage");
        if(clusterSubTopics) LogPrint.printNote("Grouping sub topics");
    }

    private void LoadTopics(){
        LogPrint.printNewStep("Loading data", 0);
        JSONObject input = JSONIOWrapper.LoadJSON(mainTopicsFile, 1);
        mainTopicsMetadata = (JSONObject) input.get("metadata");
        JSONArray topics = (JSONArray) input.get("topics");
        mainTopics = new ConcurrentHashMap<>();
        for(JSONObject topicEntry: (Iterable<JSONObject>) topics){
            TopicIOWrapper topic = new TopicIOWrapper(topicEntry);
            mainTopics.put(topic.getId(), topic);
        }
        mainSimilarityMatrix = new SimilarityMatrix((JSONArray) input.get("similarities"));
        if(clusterSubTopics){
            input = JSONIOWrapper.LoadJSON(subTopicsFile, 1);
            subTopicsMetadata = (JSONObject) input.get("metadata");
            topics = (JSONArray) input.get("topics");
            subTopics = new ConcurrentHashMap<>();
            for(JSONObject topicEntry: (Iterable<JSONObject>) topics){
                TopicIOWrapper topic = new TopicIOWrapper(topicEntry);
                subTopics.put(topic.getId(), topic);
            }
            subSimilarityMatrix = new SimilarityMatrix((JSONArray) input.get("similarities"));
        }
        // System.out.println("Topics Loaded!");
    }

    private void GroupTopics(){
        LogPrint.printNewStep("Creating topic groups", 0);
        mainTopicGroup = new TopicGroup(mainTopics, mainSimilarityMatrix);
        if(clusterSubTopics){
            subTopicGroups = new HashMap<>();
            for(Map.Entry<String, TopicIOWrapper> mainTopic: mainTopics.entrySet()){
                ConcurrentHashMap<String, TopicIOWrapper> subs = new ConcurrentHashMap<>();
                List<String> subTopicIds = mainTopic.getValue().getSubTopicIds();
                int[] indices = new int[subTopicIds.size()];
                for(int i = 0; i < subTopicIds.size(); i++){
                    subs.put(subTopicIds.get(i), new TopicIOWrapper(subTopics.get(subTopicIds.get(i))));
                    indices[i] = subTopics.get(subTopicIds.get(i)).getIndex();
                }
                Arrays.sort(indices);
                for(int i = 0; i < indices.length; i++){
                    for(Map.Entry<String, TopicIOWrapper> sub: subs.entrySet()){
                        if(sub.getValue().getIndex() == indices[i]){
                            sub.getValue().setGroupTopicIndex(i);
                            sub.getValue().setGroupTopicId(String.valueOf(i));
                        }
                    }
                }
                SimilarityMatrix subsSimilarities = subSimilarityMatrix.getSubMatrix(indices);
                subTopicGroups.put(mainTopic.getKey(), new TopicGroup(subs, subsSimilarities));
            }
        }
        LogPrint.printCompleteStep();
    }

    private void ClusterTopics(){
        LogPrint.printNewStep("Clustering topics", 0);
        LogPrint.printNewStep("Clustering main topics", 1);
        clusterTopicGroup(mainTopicGroup, mainNClusters);
        if(clusterSubTopics){
            for(Map.Entry<String, TopicGroup> group: subTopicGroups.entrySet()){
                LogPrint.printNewStep("Clustering sub topics for main topic "+group.getKey(), 1);
                clusterTopicGroup(group.getValue(), 1);
            }
        }
        // System.out.println("Topics Clustered!");
    }

    private void clusterTopicGroup(TopicGroup group, int nClusters){
        group.linkageTable = AgglomerativeClustering.PerformClustering(group.similarities.getDissimilarityMatrix(), linkageMethod);
        group.addClusterIds(nClusters);
    }

    private void SaveClusters(){
        LogPrint.printNewStep("Saving topic clusters", 0);
        JSONObject main = mainTopicGroup.toJSON();
        main.put("metadata", mainTopicsMetadata);
        JSONIOWrapper.SaveJSON(main, mainOutput, 1);
        if(clusterSubTopics){
            JSONObject root = new JSONObject();
            root.put("metadata", subTopicsMetadata);
            JSONArray groups = new JSONArray();
            for(Map.Entry<String, TopicGroup> group: subTopicGroups.entrySet()){
                JSONObject jsonGroup = group.getValue().toJSON();
                jsonGroup.put("mainTopicId", group.getKey());
                groups.add(jsonGroup);
            }
            root.put("subTopicGroups", groups);
            JSONIOWrapper.SaveJSON(root, subOutput, 1);
        }
    }
}
