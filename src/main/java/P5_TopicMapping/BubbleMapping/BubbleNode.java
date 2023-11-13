package P5_TopicMapping.BubbleMapping;

import P5_TopicMapping.Hierarchy.HierarchyNode;
import PX_Data.JSONIOWrapper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;

/**
 * Class implementing a bubble node (with position, size and padding) from a topic similarity tree.
 *
 * @author P. Le Bras
 * @version 1
 */
public class BubbleNode extends HierarchyNode {

    /** X coordinate of the bubble. */
    private double x;
    /** Y coordinate of the bubble. */
    private double y;
    /** Radius of the bubble. */
    private double r;

    /** Padding value for drawing a border around the bubble. */
    private double borderPadding;
    /** Padding value for placing bubbles against each other. */
    private double bubblePadding;

    /**
     * Constructor for leaf nodes.
     * @param data JSON topic data to attach to the bubble.
     */
    public BubbleNode(JSONObject data) {
        super(data);
    }

    /**
     * Constructor for non-leaf nodes.
     * @param c List of children nodes.
     */
    public BubbleNode(ArrayList<HierarchyNode> c) {
        super(c);
    }

    /**
     * Setter for the bubble's X coordinate.
     * @param val X coordinate value
     */
    public void setX(double val){x = val;}
    /**
     * Getter for the bubble's X coordinate.
     * @return Value for the X coordinate.
     */
    public double getX(){return x;}
    /**
     * Setter for the bubble's Y coordinate.
     * @param val Y coordinate value
     */
    public void setY(double val){y = val;}
    /**
     * Getter for the bubble's Y coordinate.
     * @return Value for the Y coordinate.
     */
    public double getY(){return y;}
    /**
     * Setter for the bubble's radius.
     * @param val Radius value
     */
    public void setR(double val){r = val;}
    /**
     * Getter for the bubble's radius.
     * @return Value for the radius.
     */
    public double getR(){return r;}
    /**
     * Setter for the bubble's border padding.
     * @param val Padding value
     */
    public void setBorderPadding(double val){
        borderPadding = val;}
    /**
     * Getter for the bubble's border padding.
     * @return Value for the border padding.
     */
    public double getBorderPadding(){return borderPadding;}
    /**
     * Setter for the bubble's position padding.
     * @param val Padding value
     */
    public void setBubblePadding(double val){
        bubblePadding = val;}
    /**
     * Getter for the bubble's position padding.
     * @return Value for the position padding.
     */
    public double getBubblePadding(){return bubblePadding;}

    /**
     * Wrapper method to get all leaf nodes descending from this node.
     * @return The list of bubble leaf nodes.
     */
    public ArrayList<BubbleNode> getBubbleLeaves(){
        ArrayList<HierarchyNode> leaves = this.getLeaves();
        ArrayList<BubbleNode> bubbleLeaves = new ArrayList<>();
        for(HierarchyNode l: leaves){
            bubbleLeaves.add((BubbleNode) l);
        }
        return bubbleLeaves;
    }

    /**
     * Method returning the bubble in JSON format to save on file.
     * @return The JSON formatted bubble.
     */
    public JSONObject toJSON(){
        JSONObject obj = new JSONObject();
        if(isLeaf()){
            DecimalFormat df = new DecimalFormat("#.###");
            df.setRoundingMode(RoundingMode.UP);
            obj.put("topicId", topicData.get("topicId"));
            obj.put("clusterId", topicData.get("clusterId"));
            obj.put("size", Double.valueOf(df.format(getValue())));
            obj.put("labels", topicData.get("topWords"));
            JSONObject mapData = new JSONObject();
            mapData.put("r", Double.valueOf(df.format(r)));
            mapData.put("cx", Double.valueOf(df.format(x)));
            mapData.put("cy", Double.valueOf(df.format(y)));
            obj.put("bubbleMap", mapData);
            if(topicData.containsKey("mainTopicIds")){
                obj.put("mainTopicIds", topicData.get("mainTopicIds"));
            } else if(topicData.containsKey("subTopicIds")){
                obj.put("subTopicIds", topicData.get("subTopicIds"));
            }
        }
        return obj;
    }

    /**
     * Method returning all the leaf nodes descendant from this node in JSON format.
     * @return The list of leaf nodes in JSON format.
     */
    public JSONArray getLeavesJSON(){
        JSONArray res = new JSONArray();
        ArrayList<BubbleNode> leaves = getBubbleLeaves();
        for(BubbleNode l: leaves){
            res.add(l.toJSON());
        }
        return res;
    }

    /**
     * Static method to build a node hierarchy, given a list of topics and a linkage table of
     * their similarities.
     * @param topics List of topics.
     * @param linkageTable Linkage table of the topics similarities.
     * @return The hierarchy root node.
     */
    public static BubbleNode buildHierarchy(JSONArray topics, JSONArray linkageTable){
        JSONObject[] t = JSONIOWrapper.getJSONObjectArray(topics);
        JSONObject[] l = JSONIOWrapper.getJSONObjectArray(linkageTable);
        int n = t.length;
        int topNodeIndex = l.length-1+n;
        BubbleNode root = exploreNode(topNodeIndex, n, t, l);
        root.root = true;
        root.setDepth(0);
        root.splitClusters();
        root.sortChildren();
        return root;
    }

    /**
     * Recursive static method exploring the topics and linkage table to build the  hierarchy.
     * @param nodeIndex Current index to lookup.
     * @param nTopics Total number of topics.
     * @param topics List of topics.
     * @param linkageTable Linkage table of the topics similarities.
     * @return The new hierarchy bubble node just built.
     */
    private static BubbleNode exploreNode(int nodeIndex, int nTopics, JSONObject[] topics, JSONObject[] linkageTable){
        if(nodeIndex >= nTopics){
            // not a leaf node
            int linkIndex = nodeIndex-nTopics;
            int c1 = Math.toIntExact((long) linkageTable[linkIndex].get("node1"));
            int c2 = Math.toIntExact((long) linkageTable[linkIndex].get("node2"));
            ArrayList<HierarchyNode> children = new ArrayList<>();
            children.add(exploreNode(c1, nTopics, topics, linkageTable));
            children.add(exploreNode(c2, nTopics, topics, linkageTable));
            return new BubbleNode(children);
        } else {
            // leaf node
            return new BubbleNode(topics[nodeIndex]);
        }
    }
}
