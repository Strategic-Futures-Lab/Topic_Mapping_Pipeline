package P4_Analysis.TopicClustering;

import P0_Project.TopicClusterModuleSpecs;
import PX_Data.JSONIOWrapper;
import PX_Data.TopicIOWrapper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TopicClustering {

    public static class TopicGroup{

        public ConcurrentHashMap<String, TopicIOWrapper> topics;
        public SimilarityMatrix similarities;
        public List<AgglomerativeClustering.ClusterRow> linkageTable;

        public TopicGroup(ConcurrentHashMap<String, TopicIOWrapper> topics, SimilarityMatrix similarities){
            this.topics = topics;
            this.similarities = similarities;
        }

        public JSONObject toJSON(){
            JSONObject root = new JSONObject();
            JSONArray t = new JSONArray();
            for(Map.Entry<String, TopicIOWrapper> entry: topics.entrySet()){
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

    public static void Cluster(TopicClusterModuleSpecs clusterSpecs){
        System.out.println( "**********************************************************\n" +
                            "* STARTING Topic Cluster !                               *\n" +
                            "**********************************************************\n");

        TopicClustering startClass = new TopicClustering();
        startClass.ProcessArguments(clusterSpecs);
        startClass.LoadTopics();
        // create sub groups of topics (and there similarity) based on main topic assignment
        startClass.GroupTopics();
        // Run Agglomerative clustering on topics groups
        startClass.ClusterTopics();
//        startClass.IndexLabels();
        startClass.SaveClusters();

        System.out.println( "**********************************************************\n" +
                            "* Topic Cluster COMPLETE !                               *\n" +
                            "**********************************************************\n");
    }

    private void ProcessArguments(TopicClusterModuleSpecs clusterSpecs){
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
                System.out.println("Error: Linkage method provided not recognised!\nUse one of the following: min, max, or avg");
                System.exit(1);
        }
        mainTopicsFile = clusterSpecs.mainTopics;
        mainOutput = clusterSpecs.mainOutput;
        clusterSubTopics = clusterSpecs.clusterSubTopics;
        if(clusterSubTopics){
            subTopicsFile = clusterSpecs.subTopics;
            subOutput = clusterSpecs.subOutput;
        }
    }

    private void LoadTopics(){
        JSONObject input = JSONIOWrapper.LoadJSON(mainTopicsFile);
        mainTopicsMetadata = (JSONObject) input.get("metadata");
        JSONArray topics = (JSONArray) input.get("topics");
        mainTopics = new ConcurrentHashMap<>();
        for(JSONObject topicEntry: (Iterable<JSONObject>) topics){
            TopicIOWrapper topic = new TopicIOWrapper(topicEntry);
            mainTopics.put(topic.getId(), topic);
        }
        mainSimilarityMatrix = new SimilarityMatrix((JSONArray) input.get("similarities"));
        if(clusterSubTopics){
            input = JSONIOWrapper.LoadJSON(subTopicsFile);
            subTopicsMetadata = (JSONObject) input.get("metadata");
            topics = (JSONArray) input.get("topics");
            subTopics = new ConcurrentHashMap<>();
            for(JSONObject topicEntry: (Iterable<JSONObject>) topics){
                TopicIOWrapper topic = new TopicIOWrapper(topicEntry);
                subTopics.put(topic.getId(), topic);
            }
            subSimilarityMatrix = new SimilarityMatrix((JSONArray) input.get("similarities"));
        }
        System.out.println("Topics Loaded!");
    }

    private void GroupTopics(){
        System.out.println("Grouping Topics ...");
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
        System.out.println("Topics Grouped!");
    }

    private void ClusterTopics(){
        System.out.println("Clustering Topics ...");
        System.out.println("Clustering main topics");
        clusterTopicGroup(mainTopicGroup);
        if(clusterSubTopics){
            for(Map.Entry<String, TopicGroup> group: subTopicGroups.entrySet()){
                System.out.println("Clustering sub-topics for main topic "+group.getKey());
                clusterTopicGroup(group.getValue());
            }
        }
        System.out.println("Topics Clustered!");
    }

    private void clusterTopicGroup(TopicGroup group){
        group.linkageTable = AgglomerativeClustering.PerformClustering(group.similarities.getDissimilarityMatrix(), linkageMethod);
    }

    private void SaveClusters(){
        System.out.println("Saving Topic Clusters ...");
        JSONObject main = mainTopicGroup.toJSON();
        main.put("metadata", mainTopicsMetadata);
        JSONIOWrapper.SaveJSON(main, mainOutput);
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
            JSONIOWrapper.SaveJSON(root, subOutput);
        }
        System.out.println("Topic Cluster Saved!");
    }
}
