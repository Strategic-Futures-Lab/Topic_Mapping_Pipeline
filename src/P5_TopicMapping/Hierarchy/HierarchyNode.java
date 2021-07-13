package P5_TopicMapping.Hierarchy;

import PX_Data.JSONIOWrapper;
import PY_Helper.LogPrint;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

/**
 * Class implementing a node from a topic similarity tree
 *
 * @author P. Le Bras
 * @version 1
 */
public abstract class HierarchyNode {

    /** Flag identifying the node as root */
    public boolean root = false;
    /** Flag identifying the node as leaf */
    private final boolean leaf;
    /** The list of this node's children, if this is not a leaf node */
    private ArrayList<HierarchyNode> children;
    /** The node's parent, if this is not the root node */
    private HierarchyNode parent;
    /** If this is a leaf node, the id of the topic associated with the node */
    private String topicId;
    /** If this is a leaf node, the JSON data of the topic associated with the node */
    protected JSONObject topicData;
    /** The (optional) cluster identifier for this node and its descendants */
    private String clusterId;
    /** Flag identifying the node as root for one of the cluster in the tree */
    private boolean clusterRoot;
    /** The (optional) node value: from the topic distribution total is this is a leaf node
     * or sum of the children's values otherwise */
    private double value;
    /** The node size, i.e. scaled value. */
    private double size;
    /** The node's height, 0 if this is a leaf node */
    private int height;
    /** The node's depth, 0 is this is the root node */
    private int depth;

    /**
     * Leaf constructor
     * @param data topic JSON data
     */
    public HierarchyNode(JSONObject data){
        topicData = data;
        leaf = true;
        value = 1.0;
        size = value;
        topicId = (String) topicData.get("topicId");
        if(topicData.containsKey("clusterId")){
            clusterId = (String) topicData.get("clusterId");
        } else {
            LogPrint.printNoteError("No cluster ID found in topic data. The TopicClustering module must be ran" +
                    "before any mapping is done.");
        }
    }

    /**
     * Non-leaf constructor
     * @param c list of children
     */
    public HierarchyNode(ArrayList<HierarchyNode> c){
        for(HierarchyNode child: c){
            child.setParent(this);
        }
        children = c;
        value = children.stream().map(HierarchyNode::getValue).reduce(0.0, Double::sum);
        leaf = false;
        // topicId = children.stream().map(HierarchyNode::getTopicId).reduce("", (a,b)->a+"-"+b);
    }
    /**
     * Setter for the parent node
     * @param p parent node
     */
    public void setParent(HierarchyNode p){ parent = p; }

    /**
     * Getter for the parent node
     * @return the parent node
     */
    public HierarchyNode getParent(){ return parent; }

    /**
     * Setter for the cluster root flag
     * @param split cluster root flag
     */
    public void setClusterRoot(boolean split){ clusterRoot = split;}

    /**
     * Getter for the cluster root flag
     * @return the cluster root flag
     */
    public boolean isClusterRoot(){ return clusterRoot; }

    /**
     * Getter for node's value
     * @return the node value
     */
    public double getValue(){ return value; }

    /**
     * Getter for node's size
     * @return the node size
     */
    public double getSize(){ return size; }

    /**
     * Getter for the node's depth
     * @return the node's depth
     */
    public int getDepth() { return depth; }

    /**
     * Getter for the node's height
     * @return the node's height
     */
    public int getHeight() { return height; }

    /**
     * Getter for the leaf flag
     * @return the leaf flag
     */
    public boolean isLeaf(){return leaf;}

    /**
     * Getter for the root flag
     * @return the root flag
     */
    public boolean isRoot(){return root;}

    /**
     * Getter for the node's topic id
     * @return the node's topic id
     */
    public String getTopicId(){return topicId;}

    /**
     * Recursively sets the node's cluster information.
     * If the node's children have the same, non-null, cluster ID, this nodes gets it too.
     * If this node's is the last to get this cluster ID, it becomes the cluster split node.
     * @return the node's cluster ID for its parent.
     */
    public String splitClusters(){
        if(leaf){
            // initialise the cluster roots on the leaves
            clusterRoot = true;
        } else {
            clusterId = "null";
            // get all unique cluster ids from children
            HashSet<String> clusterChildren = children.stream()
                    .map(HierarchyNode::splitClusters)
                    .collect(Collectors.toCollection(HashSet::new));
            if(clusterChildren.size() == 1){ // a single unique cluster id for children
                String c = clusterChildren.iterator().next();
                if(!c.equals("null")){
                    // cluster id is non-null, move the cluster split up in the hierarchy
                    clusterRoot = true;
                    children.forEach(child -> child.setClusterRoot(false));
                    clusterId = c;
                }
            }
        }
        // return the cluster id to the parent node
        return clusterId;
    }

    /**
     * Recursively sets the node's value.
     * If the node is a leaf, get the value from the topic data, using the provide accessor.
     * If the node is not a leaf, get the values from its children and set its value as the sum of the children's values.
     * @param valueId Accessor to get the value in the topic data.
     * @return The list of leaf values to give back to the node's parent.
     */
    public double[] setValues(String valueId){
        if(leaf){
            // if leaf, access value from topic data
            if(topicData.containsKey("totals")){
                JSONObject[] values = JSONIOWrapper.getJSONObjectArray((JSONArray) topicData.get("totals"));
                values = Arrays.stream(values)
                        .filter(t -> t.get("id").equals(valueId))
                        .toArray(JSONObject[]::new);
                if(values.length > 0){
                    value = (double) values[0].get("weight");
                    size = value;
                }
                // return value to parent
                return new double[] {value};
            } else {
                // if valueId cannot be found stop
                LogPrint.printNoteError("No distribution totals found in topic data. The TopicDistribution" +
                        "module must be ran before any mapping is done.");
                System.exit(1);
            }
        } else {
            // if non-leaf node, concatenate children's values
            double[] childrenValues = new double[]{};
            for(HierarchyNode c: children){
                childrenValues = DoubleStream.concat(Arrays.stream(childrenValues), Arrays.stream(c.setValues(valueId))).toArray();
            }
            // set your value to sum of children's values
            value = Arrays.stream(childrenValues).reduce(0, Double::sum);
            size = value;
            return childrenValues;
        }
        return new double[] {};
    }

    /**
     * Method recursively normalising the values attached to leaf nodes on the provided linear scale.
     * Non-leaf nodes reset their values as the sum of their leaf descendants.
     * @param minD Minimum value in the domain.
     * @param ranD Domain range (max value - min value).
     * @param minR Minimum value in the range.
     * @param ranR Range range (max value - min value).
     * @return The list of leaf values to give back to the node's parent.
     */
    public double[] normaliseSizes(double minD, double ranD, double minR, double ranR){
        if(leaf){
            size = minR + ((value - minD) / ranD) * ranR;
            return new double[] {size};
        } else {
            // if non-leaf node, concatenate children's values
            double[] childrenSizes = new double[]{};
            for(HierarchyNode c: children){
                childrenSizes = DoubleStream.concat(Arrays.stream(childrenSizes), Arrays.stream(c.normaliseSizes(minD, ranD, minR, ranR))).toArray();
            }
            // set your value to sum of children's values
            size = Arrays.stream(childrenSizes).reduce(0, Double::sum);
            return childrenSizes;
        }
    }

    /**
     * Method both setting the value of each node recursively and then normalising it in the range provided.
     * This method can only be called on the root node.
     * @param valueId Accessor to get the value in the topic data.
     * @param minR Minimum value for the target range.
     * @param maxR Maximum value for the target range.
     */
    public void setValuesAndNormaliseSizes(String valueId, double minR, double maxR){
        if(root){
            double[] values = setValues(valueId);
            DoubleSummaryStatistics stat = Arrays.stream(values).summaryStatistics();
            double minD = 1;
            double maxD = stat.getMax();
            double ranD = maxD - minD;
            double ranR = maxR - minR;
            if(ranD != 0){
                normaliseSizes(minD, ranD, minR, ranR);
            }
        }
    }

    /**
     * Recursively sets the depth and height information.
     * Given the node's depth, set its and it's children's depth (d+1).
     * Also gets the height back from the children.
     * @param d node's depth
     * @return the node's height for it's parent
     */
    public int setDepth(int d){
        // set depth
        depth = d;
        // initialise height
        height = 0;
        // if non-leaf node
        if(!leaf){
            // set height to be max height among children + 1
            for(HierarchyNode c: children){
                height = Integer.max(height, c.setDepth(d+1));
            }
            height += 1;
        }
        return height;
    }

    /**
     * Getter for the node's children. if this is a leaf node, return an empty list
     * @return list of children
     */
    public ArrayList<HierarchyNode> getChildren(){
        if(leaf){
            return new ArrayList<HierarchyNode>();
        }
        return children;
    }

    /**
     * Creates a stack of the node and its children and then executes the given callback on every node in LIFO order
     * @param cb callback to execute
     */
    public void eachAfter(NodeCallBack cb){
        Stack<HierarchyNode> nodes = new Stack<>();
        nodes.push(this);
        Stack<HierarchyNode> next = new Stack<>();
        while(!nodes.empty()){
            HierarchyNode node = nodes.pop();
            next.push(node);
            if(!node.isLeaf()){
                for(HierarchyNode c: node.getChildren()){
                    nodes.push(c);
                }
            }
        }
        while(!next.empty()){
            cb.method(next.pop());
        }
    }

    /**
     * Executes the given callback on all nodes in LIFO order while creating a stack of the node and its children
     * @param cb callback to execute
     */
    public void eachBefore(NodeCallBack cb){
        Stack<HierarchyNode> nodes = new Stack<>();
        nodes.push(this);
        while(!nodes.empty()){
            HierarchyNode node = nodes.pop();
            cb.method(node);
            if(!node.isLeaf()){
                for(HierarchyNode c: node.getChildren()){
                    nodes.push(c);
                }
            }
        }
    }

    /**
     * Gets the list of the all descendant nodes, sorted by depth
     * @return list of descendants
     */
    public ArrayList<HierarchyNode> descendants(){
        ArrayList<HierarchyNode> res = new ArrayList<>();
        res.add(this);
        if(!leaf){
            for(HierarchyNode c: children){
                res.addAll(c.descendants());
            }
        }
        res.sort(Comparator.comparingInt(HierarchyNode::getDepth));
        return res;
    }

    /**
     * Gets the list of all ancestor nodes, sorted by height
     * @return list of ancestors
     */
    public ArrayList<HierarchyNode> ancestors(){
        ArrayList<HierarchyNode> res = new ArrayList<>();
        res.add(this);
        if(!root) {
            res.addAll(parent.ancestors());
        }
        res.sort(Comparator.comparingInt(HierarchyNode::getHeight));
        return res;
    }

    /**
     * Gets the list of all leaf nodes descending from this node
     * @return list of leaves
     */
    public ArrayList<HierarchyNode> getLeaves(){
        ArrayList<HierarchyNode> res = new ArrayList<>();
        if(leaf){
            res.add(this);
        }
        else{
            for(HierarchyNode c: children){
                res.addAll(c.getLeaves());
            }
        }
        return res;
    }

    // public ArrayList<HierarchyNode> flattenHierarchy(boolean full){
    //     ArrayList<HierarchyNode> res = new ArrayList<>();
    //     if(clusterRoot){
    //         // when we reach a cluster node,
    //         if(full){
    //             // if fully flattening, set its leaves as children
    //             ArrayList<HierarchyNode> leaves = this.getLeaves();
    //             leaves.forEach(l -> l.setParent(this));
    //             children = leaves;
    //         }
    //         // stop the recursion
    //         res.add(this);
    //     } else {
    //         // otherwise recursively flatten children
    //         for(HierarchyNode c: children){
    //             res.addAll(c.flattenHierarchy(full));
    //         }
    //     }
    //     if(root && !clusterRoot){
    //         // if root element, set cluster nodes as children
    //         res.forEach(n -> n.setParent(this));
    //         children = res;
    //     }
    //     return res;
    // }

    /**
     * Sorts the node's children by value
     */
    public void sortChildren(){
        if(!leaf){
            children.forEach(HierarchyNode::sortChildren);
            children.sort((n1, n2) -> Double.compare(n2.getValue(), n1.getValue()));
        }
    }
}
