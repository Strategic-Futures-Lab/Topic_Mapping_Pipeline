package P5_TopicMapping.BubbleMapping;

import java.util.ArrayList;

/**
 * Class representing a cluster within a {@link BubbleNode} hierarchy.
 *
 * @author P. Le Bras
 * @version 1
 */
public class BubbleCluster {

    /** List of leaf nodes in that cluster */
    public ArrayList<BubbleNode> nodes;
    /** Parent node for that cluster */
    public BubbleNode parent;

    /**
     * Constructor
     * @param n List of leaf nodes
     * @param p Parent node
     */
    public BubbleCluster(ArrayList<BubbleNode> n, BubbleNode p){
        nodes = n; parent = p;
    }

    /**
     * Static method to build layer clusters from a {@link BubbleNode} at a desired depth.
     * @param root Hierarchy root to get the clusters from
     * @param layerDepth Layer depth at which to get the clusters
     * @param padding Base padding value to append to the nodes
     * @return The list of layer clusters in the hierarchy at the specified depth
     */
    public static ArrayList<BubbleCluster> getLayerClusters(BubbleNode root, int layerDepth, int padding){
        // list of layer clusters
        ArrayList<BubbleCluster> clusters = new ArrayList<>();
        // getting the list of nodes at the desired layer depth
        ArrayList<BubbleNode> layerNodes = (ArrayList<BubbleNode>)(ArrayList<?>) root.descendants();
        layerNodes.removeIf(n -> n.getDepth() != layerDepth);
        // these nodes will be the clusters parents, for each of them:
        for(BubbleNode clusterParent: layerNodes){
            // get the leaves
            ArrayList<BubbleNode> clusterLeaves = clusterParent.getBubbleLeaves();
            // for each of the cluster leaves
            for(BubbleNode leaf: clusterLeaves){
                // check if there is a cluster split between the leaf and the cluster parent
                BubbleNode tmp = leaf;
                double clusterSplit = 0.0;
                while(tmp != clusterParent) {
                    tmp = (BubbleNode) tmp.getParent();
                    if(tmp.isClusterRoot()){ clusterSplit = 1.0; break; }
                }
                // setup padding values
                double contourParent = clusterParent.isClusterRoot() ? 0.5 : 0.0;
                double forceParent = (leaf != clusterParent && clusterParent.isClusterRoot()) ? 1.0 : 0.0;
                double interClusterSpace = clusterLeaves.size() == 1 ? 0.0 : padding / 2.0;
                // register padding values to the leaf node
                leaf.setBorderPadding((leaf.getDepth()-clusterParent.getDepth())*padding+clusterSplit+contourParent);
                leaf.setBubblePadding((leaf.getDepth()-clusterParent.getDepth())*padding+clusterSplit+forceParent+interClusterSpace);
            }
            clusters.add(new BubbleCluster(clusterLeaves, clusterParent));
        }
        return clusters;
    }
}
