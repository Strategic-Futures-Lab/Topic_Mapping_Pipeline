package P4_Analysis.TopicClustering;

import P0_Project.TopicClusterModuleSpecs;
import PX_Data.JSONIOWrapper;
import PX_Data.TopicIOWrapper;
import PY_Helper.LogPrint;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class generating the cluster data for topics.
 * Reads data from topic file(s).
 * If sub topics are provided, these are grouped first based on their main topic assignment.
 * Main topics are considered as a single group.
 * Each group of topics gets their linkage table calculated, based on the topics' similarity matrix.
 * From the linkage table, cluster ids are also assigned to topics.
 * Data is saved as topic JSON file(s).
 *
 * @author P. Le Bras
 * @version 1
 */
public class TopicClustering {

    /**
     * Class implementing a group of topics, ie, set of topics from which to get a linkage table.
     * Typically, there should be one group for all main topics, and several groups of sub topics.
     * Each group of sub topics should have sub topics which have been assigned to the same main topic.
     */
    public static class TopicGroup{

        /** List of topics in the group. */
        public ConcurrentHashMap<Integer, TopicIOWrapper> topics;
        /** Similarity matrix between the topcis of this group. */
        public SimilarityMatrix similarities;
        /** Linkage table for the group of topics. */
        public List<AgglomerativeClustering.LinkageNode> linkageTable;

        /**
         * Constructor.
         * @param topics List of topics.
         * @param similarities Similarity matrix between topics.
         */
        public TopicGroup(ConcurrentHashMap<String, TopicIOWrapper> topics, SimilarityMatrix similarities){
            this.topics = new ConcurrentHashMap<>();
            for(Map.Entry<String, TopicIOWrapper> topic: topics.entrySet()){
                TopicIOWrapper t = topic.getValue();
                int idx = (t.getGroupTopicIndex() == -1) ? t.getIndex() : t.getGroupTopicIndex();
                this.topics.put(idx, t);
            }
            this.similarities = similarities;
        }

        /**
         * Method setting the cluster ids of each topic. This assumes that the linkage table has been set prior.
         * @param nClusters Number of clusters to set.
         */
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

        /**
         * Method recursively exploring the linkage table to set the cluster id of topics.
         * @param nodeIndex Index of the node to explore in the linkage table.
         * @param cluster Current cluster number.
         * @param linkageSplitHeight Index, in the linkage table, where clusters split.
         * @return Last cluster number used by this node.
         */
        private int exploreLinkageNode(int nodeIndex, int cluster, int linkageSplitHeight){
            // get the linkage node
            AgglomerativeClustering.LinkageNode node = linkageTable.get(nodeIndex);
            // if above the split height, increase cluster id for second branches, otherwise keep it the same
            int newCluster = (nodeIndex > linkageSplitHeight) ? cluster+1 : cluster;
            if(node.Node1 < topics.size()){
                // at least one leaf, set the cluster id
                topics.get(node.Node1).setClusterId(Integer.toString(cluster));
                if(node.Node2 < topics.size()){
                    // two leaves, set the cluster using the (new) id
                    topics.get(node.Node2).setClusterId(Integer.toString(newCluster));
                } else {
                    // second branch is not a leaf, set its clusters, starting with the (new) id
                    return exploreLinkageNode(node.Node2 - topics.size(), newCluster, linkageSplitHeight);
                }
            } else if(node.Node2 < topics.size()){
                // second branch is the only leaf, set the cluster id
                topics.get(node.Node2).setClusterId(Integer.toString(cluster));
                // first branch is not a leaf, set its clusters, starting with the (new) id
                return exploreLinkageNode(node.Node1 - topics.size(), newCluster, linkageSplitHeight);
            } else {
                // no leaves
                // explore the first branch, set its cluster ids, and get the last id used
                int prevClusterNum = exploreLinkageNode(node.Node1 - topics.size(), cluster, linkageSplitHeight);
                // if above the split height, increase cluster id for the second branch
                newCluster = (nodeIndex > linkageSplitHeight) ? prevClusterNum+1 : prevClusterNum;
                // explore the second branch and set its clusters, starting with the (new) id
                return exploreLinkageNode(node.Node2 - topics.size(), newCluster, linkageSplitHeight);
            }
            // by default return the current cluster id
            return cluster;
        }

        /**
         * Method returning the group in JSON format.
         * @return Topic group in JSON format.
         */
        public JSONObject toJSON(){
            JSONObject root = new JSONObject();
            JSONArray t = new JSONArray();
            for(Map.Entry<Integer, TopicIOWrapper> entry: topics.entrySet()){
                t.add(entry.getValue().toJSON());
            }
            root.put("topics", t);
            root.put("similarities", similarities.toJSON());
            JSONArray l = new JSONArray();
            for(AgglomerativeClustering.LinkageNode row : linkageTable){
                l.add(row.toJSON());
            }
            root.put("linkageTable", l);
            return root;
        }
    }

    /** Filename of the main topic JSON file. */
    private String mainTopicsFile;
    /** Filename of the output main topic JSON file. */
    private String mainOutput;
    /**  Number of clusters to get in the group of main topic files. */
    private int mainNClusters;
    /** Linkage method for the hierarchical clustering. */
    private AgglomerativeClustering.LINKAGE_TYPE linkageMethod;
    /** Boolean flag for processing sub topics. */
    private boolean groupSubTopics;
    /** Filename of the sub topic JSON file. */
    private String subTopicsFile;
    /** Filename of the output sub topic JSON file. */
    private String subOutput;

    /** Main topics metadata JSON object. */
    private JSONObject mainTopicsMetadata;
    /** List of main topics. */
    private ConcurrentHashMap<String, TopicIOWrapper> mainTopics;
    /** Similarity matrix between main topics. */
    private SimilarityMatrix mainSimilarityMatrix;
    /** Sub topics metadata JSON object. */
    private JSONObject subTopicsMetadata;
    /** List of sub topics. */
    private ConcurrentHashMap<String, TopicIOWrapper> subTopics;
    /** Similarity matrix between sub topics. */
    private SimilarityMatrix subSimilarityMatrix;

    /** Main topics group. */
    private TopicGroup mainTopicGroup;
    /** List of sub topics groups. */
    private HashMap<String, TopicGroup> subTopicGroups;

    /**
     * Main method, reads the specification and launches the sub-methods in order.
     * @param clusterSpecs Specifications.
     * @return String indicating the time taken to read the topic JSON file(s), estimate groups and clusters and
     * write the JSON cluster file(s).
     */
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

    /**
     * Method processing the specification parameters.
     * @param clusterSpecs Specification object.
     */
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
        groupSubTopics = clusterSpecs.groupingSubTopics;
        if(groupSubTopics){
            subTopicsFile = clusterSpecs.subTopics;
            subOutput = clusterSpecs.subOutput;
        }
        LogPrint.printCompleteStep();
        LogPrint.printNote("Producing "+mainNClusters+" clusters using "+linkageMethod+" linkage");
        if(groupSubTopics) LogPrint.printNote("Grouping sub topics");
    }

    /**
     * Method reading and loading topics from files.
     */
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
        if(groupSubTopics){
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
    }

    /**
     * Method generating the topic group(s).
     */
    private void GroupTopics(){
        LogPrint.printNewStep("Creating topic groups", 0);
        mainTopicGroup = new TopicGroup(mainTopics, mainSimilarityMatrix);
        if(groupSubTopics){
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

    /**
     * Method launching the clustering process.
     */
    private void ClusterTopics(){
        LogPrint.printNewStep("Clustering topics", 0);
        LogPrint.printNewStep("Clustering main topics", 1);
        clusterTopicGroup(mainTopicGroup, mainNClusters);
        if(groupSubTopics){
            for(Map.Entry<String, TopicGroup> group: subTopicGroups.entrySet()){
                LogPrint.printNewStep("Clustering sub topics for main topic "+group.getKey(), 1);
                clusterTopicGroup(group.getValue(), 1);
            }
        }
    }

    /**
     * Method estimating the linkage table of a topic group, and assigning cluster ids to the topics.
     * @param group Group of topics to process.
     * @param nClusters Number of clusters to generate in the group.
     */
    private void clusterTopicGroup(TopicGroup group, int nClusters){
        group.linkageTable = AgglomerativeClustering.PerformClustering(group.similarities.getDissimilarityMatrix(), linkageMethod);
        group.addClusterIds(nClusters);
    }

    /**
     * Method writing the grouped and clustered topics on JSON file(s).
     */
    private void SaveClusters(){
        LogPrint.printNewStep("Saving topic clusters", 0);
        JSONObject main = mainTopicGroup.toJSON();
        main.put("metadata", mainTopicsMetadata);
        JSONIOWrapper.SaveJSON(main, mainOutput, 1);
        if(groupSubTopics){
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
