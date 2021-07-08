package P4_Analysis.TopicClustering;

import PY_Helper.LogPrint;
import org.json.simple.JSONObject;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Class providing methods to get the agglomerative clusters, in the form of a linkage table, of items given
 * their distance matrix.
 *
 * @author T. Methven, P. Le Bras
 * @version 2
 */
public class AgglomerativeClustering {

    /** List of possible linkage types: Average (UPGMA), Minimum and Maximum. */
    public enum LINKAGE_TYPE {AVERAGE, MIN, MAX};

    /**
     * Class implementing a node from a linkage table.
     */
    public static class LinkageNode {
        /** Index of child node 1. */
        public int Node1;
        /** Index of child node 2. */
        public int Node2;
        /** Distance between the two children nodes. */
        public double Distance;

        /**
         * Constructor.
         * @param Node1 Index of first child node.
         * @param Node2 index of second child node.
         * @param Distance Distance between the two nodes.
         */
        public LinkageNode(int Node1, int Node2, double Distance) {
            this.Node1 = Node1;
            this.Node2 = Node2;
            DecimalFormat df = new DecimalFormat("#.####");
            df.setRoundingMode(RoundingMode.UP);
            this.Distance = Double.parseDouble(df.format(Distance));
        }

        /**
         * Method returning the node in JSON format.
         * @return JSON formatted node.
         */
        public JSONObject toJSON(){
            JSONObject res = new JSONObject();
            res.put("node1", Node1);
            res.put("node2", Node2);
            res.put("distance", Distance);
            return res;
        }
    }

    /**
     * Class implementing a cluster of leaf nodes from a linkage table.
     */
    public static class NodeCluster {
        /** Node/cluster index in the linkage table. */
        public int NodeNumber;
        /** List of indices of leaf nodes in this cluster. */
        public List<Integer> ContainsNodes = new ArrayList<>();

        /**
         * Constructor.
         * @param NodeNumber Node index.
         */
        public NodeCluster(int NodeNumber) {
            this.NodeNumber = NodeNumber;
        }

        /**
         * Constructor for a leaf cluster, contains only one leaf node from the linkage table.
         * @param NodeNumber Node index.
         * @param initialNode Initial leaf node index.
         */
        public NodeCluster(int NodeNumber, int initialNode) {
            this.NodeNumber = NodeNumber;
            AddContainingNode(initialNode);
        }

        /**
         * Method adding a leaf node index to this cluster.
         * @param node Leaf node index to add.
         */
        public void AddContainingNode(int node) {
            ContainsNodes.add(node);
        }

        /**
         * Method adding a list of leaf node indices to this cluster.
         * @param nodes List of nodes to add.
         */
        public void AddContainingNodes(List<Integer> nodes){
            ContainsNodes.addAll(nodes);
        }
    }

    /**
     * Method removing topics from the distance matrix, eg, generic topics, to have them excluded from
     * the linkage table, and by extension, map layout.
     * @param dissimilarityMatrix Original distance matrix.
     * @param genericTopics List of topic ids to exclude.
     * @return The new distance matrix.
     * @deprecated Not used with the bubble map layout.
     */
    @Deprecated
    public static double[][] RemoveTopicsFromMatrix(double[][] dissimilarityMatrix, List<Integer> genericTopics) {
        //Convert array to list of lists
        List<List<Double>> dissimList = new ArrayList<>();

        //Simpler topic removal method, where we just skip any rows/columns that shouldn't be included when we build the lists!
        for(int y = 0; y < dissimilarityMatrix.length; y++) {
            if(genericTopics.contains(y))
                continue;

            List<Double> row = new ArrayList<>();
            for(int x = 0; x < dissimilarityMatrix.length; x++) {
                if(genericTopics.contains(x))
                    continue;

                row.add(dissimilarityMatrix[x][y]);
            }
            dissimList.add(row);
        }

        //Convert list of lists back to array
        double[][] newDissimMatrix = new double[dissimList.size()][dissimList.size()];
        //String row;

        for(int y = 0; y < newDissimMatrix.length; y++) {
            //row = "";
            for(int x = 0; x < newDissimMatrix.length; x++) {
                newDissimMatrix[x][y] = dissimList.get(y).get(x);
                //row += dissimList.get(y).get(x) + ",";
            }
            //System.out.println(row.substring(0, row.length() - 1));
        }

        return newDissimMatrix;
    }

    /**
     * Method creating a linkage table, ie, hierarchical cluster, from a distance/dissimilarity matrix.
     * @param dissimilarityMatrix Matrix to build linkage table from.
     * @param clusterType Type of merging method.
     * @return The linkage table of matrix items.
     */
    public static List<LinkageNode> PerformClustering(double[][] dissimilarityMatrix, LINKAGE_TYPE clusterType) {
        // Checking that the matrix is valid
        CheckMatrixValidity(dissimilarityMatrix);

        LogPrint.printNewStep("Clustering and creating linkage table", 2);

        // Instantiating our linkage table, Matlab-style, ie, list of linkage nodes
        List<LinkageNode> linkageTable = new ArrayList<>();

        // To keep track of the node clusters.
        // Each cluster has their node index in the linkage table + the list of leaf node indices.
        // Initially there's only one cluster per leaf node, but these merge as we explore and "collapse" the
        // distance matrix.
        List<NodeCluster> clusters = new ArrayList<>();
        for(int i = 0; i < dissimilarityMatrix.length; i++) {
            clusters.add(new NodeCluster(i, i));
        }

        // Make a copy of our distance matrix, this copy will be the one collapsing as merge clusters.
        double[][] matrix = new double[dissimilarityMatrix.length][dissimilarityMatrix.length];
        for(int y = 0; y < dissimilarityMatrix.length; y++) {
            for(int x = 0; x < dissimilarityMatrix.length; x++) {
                matrix[x][y] = dissimilarityMatrix[x][y];
            }
        }

        int xLoc = 0, yLoc = 0;
        double shortestDistance;
        int nodeVal = dissimilarityMatrix.length;
        int escape = 0;

        // Iterate until we have reduced the matrix reduced to a single node/cluster
        while(matrix.length > 1 && escape < 1000) {
            // Find the current shortest distance in the collapsed array...
            shortestDistance = Double.MAX_VALUE;
            for(int x = 0; x < matrix.length; x++) {
                for(int y = x + 1; y < matrix.length; y++) {
                    if(matrix[x][y] < shortestDistance) {
                        shortestDistance = matrix[x][y];
                        xLoc = x;
                        yLoc = y;
                    }
                }
            }

            // Add a record in the linkage table for this join
            linkageTable.add(new LinkageNode(clusters.get(xLoc).NodeNumber, clusters.get(yLoc).NodeNumber, shortestDistance));

            // Update the list of clusters.
            // First, we change the label of the first item to the new, higher, node value.
            clusters.get(xLoc).NodeNumber = nodeVal++;
            // Second, we merge the leaf nodes from the second item to the first.
            clusters.get(xLoc).AddContainingNodes(clusters.get(yLoc).ContainsNodes);
            // Finally, we remove the second item (it has been merged into the first)
            clusters.remove(yLoc);

            // Collapse the temporary distance matrix.
            int tempX = 0, tempY = 0;
            // make a new, smaller, matrix
            double[][] newMatrix = new double[matrix.length - 1][matrix.length - 1];
            // fill it
            for(int x = 0; x < newMatrix.length; x++) {
                for(int y = x + 1; y < newMatrix.length; y++) {
                    if(y == xLoc) {
                        // Where the previous x column was, update the distances
                        if(clusterType == LINKAGE_TYPE.AVERAGE)
                            newMatrix[x][y] = GetUPMGAValue(dissimilarityMatrix, clusters, xLoc, x);
                        else if(clusterType == LINKAGE_TYPE.MAX)
                            newMatrix[x][y] = GetMaxValue(dissimilarityMatrix, clusters, xLoc, x);
                        else if(clusterType == LINKAGE_TYPE.MIN)
                            newMatrix[x][y] = GetMinValue(dissimilarityMatrix, clusters, xLoc, x);
                    } else if(x == xLoc) {
                        // Where the previous x row was, update the distances
                        if(clusterType == LINKAGE_TYPE.AVERAGE)
                            newMatrix[x][y] = GetUPMGAValue(dissimilarityMatrix, clusters, xLoc, y);
                        else if(clusterType == LINKAGE_TYPE.MAX)
                            newMatrix[x][y] = GetMaxValue(dissimilarityMatrix, clusters, xLoc, y);
                        else if(clusterType == LINKAGE_TYPE.MIN)
                            newMatrix[x][y] = GetMinValue(dissimilarityMatrix, clusters, xLoc, y);
                    } else {
                        tempX = x; tempY = y;
                        // skip the previous y column and row
                        if(x >= yLoc) tempX++;
                        if(y >= yLoc) tempY++;
                        // and copy the previous distances
                        newMatrix[x][y] = matrix[tempX][tempY];
                    }
                }
            }
            // make it our new distance matrix
            matrix = newMatrix;
            escape++;
        }

        LogPrint.printCompleteStep();
        return linkageTable;
    }

    /**
     * Method getting the unweighted mean distance between two clusters of leaf nodes.
     * @param dissimilarityMatrix Distance matrix between leaf nodes.
     * @param clusters Sets of leaf nodes clusters.
     * @param cluster1 Index of first cluster.
     * @param cluster2 Index of second cluster.
     * @return The unweighted mean distance (UPMGA) between the two clusters.
     */
    private static double GetUPMGAValue(double[][] dissimilarityMatrix, List<NodeCluster> clusters, int cluster1, int cluster2) {
        double totalValue = 0;
        int count = 0;

        for(int labelValue1 = 0; labelValue1 < clusters.get(cluster1).ContainsNodes.size(); labelValue1++) {
            for(int labelValue2 = 0; labelValue2 < clusters.get(cluster2).ContainsNodes.size(); labelValue2++) {
                totalValue += dissimilarityMatrix[clusters.get(cluster2).ContainsNodes.get(labelValue2)]
                        [clusters.get(cluster1).ContainsNodes.get(labelValue1)];
                count++;
            }
        }

        return totalValue / (double)count;
    }

    /**
     * Method getting the maximum distance between two clusters of leaf nodes.
     * @param dissimilarityMatrix Distance matrix between leaf nodes.
     * @param clusters Sets of leaf nodes clusters.
     * @param cluster1 Index of first cluster.
     * @param cluster2 Index of second cluster.
     * @return The maximum distance between the two clusters.
     */
    private static double GetMaxValue(double[][] dissimilarityMatrix, List<NodeCluster> clusters, int cluster1, int cluster2) {
        double tempValue = 0, maxValue = 0;

        for(int labelValue1 = 0; labelValue1 < clusters.get(cluster1).ContainsNodes.size(); labelValue1++) {
            for(int labelValue2 = 0; labelValue2 < clusters.get(cluster2).ContainsNodes.size(); labelValue2++) {
                tempValue = dissimilarityMatrix[clusters.get(cluster2).ContainsNodes.get(labelValue2)]
                        [clusters.get(cluster1).ContainsNodes.get(labelValue1)];

                if(tempValue > maxValue)
                    maxValue = tempValue;
            }
        }

        return maxValue;
    }

    /**
     * Method getting the minimum distance between two clusters of leaf nodes.
     * @param dissimilarityMatrix Distance matrix between leaf nodes.
     * @param clusters Sets of leaf nodes clusters.
     * @param cluster1 Index of first cluster.
     * @param cluster2 Index of second cluster.
     * @return The minimum distance between the two clusters.
     */
    private static double GetMinValue(double[][] dissimilarityMatrix, List<NodeCluster> clusters, int cluster1, int cluster2) {
        double tempValue = 0, minValue = Double.MAX_VALUE;

        for(int labelValue1 = 0; labelValue1 < clusters.get(cluster1).ContainsNodes.size(); labelValue1++) {
            for(int labelValue2 = 0; labelValue2 < clusters.get(cluster2).ContainsNodes.size(); labelValue2++) {
                tempValue = dissimilarityMatrix[clusters.get(cluster2).ContainsNodes.get(labelValue2)]
                        [clusters.get(cluster1).ContainsNodes.get(labelValue1)];

                if(tempValue < minValue)
                    minValue = tempValue;
            }
        }

        return minValue;
    }

    /**
     * Method ensuring that a given matrix is valid.
     * In particular, check for correct values and for symmetry.
     * @param matrix Matrix to check.
     */
    private static void CheckMatrixValidity(double[][] matrix) {
        LogPrint.printNewStep("Checking similarity matrix validity", 2);
        for(int y = 0; y < matrix.length; y++) {
            for(int x = 0; x < matrix.length; x++) {
                if(matrix[x][y] != matrix[y][x]) {
                    LogPrint.printNoteError("Error in similarity matrix, not symmetrical at location x: "+x+"; y: "+y);
                    System.exit(1);
                }
                if(matrix[x][y] > 1 || matrix[x][y] < 0) {
                    LogPrint.printNoteError("Error in similarity matrix, not normal at location x: "+x+"; y: "+y);
                    System.exit(1);
                }
            }
        }
        LogPrint.printCompleteStep();
    }
}
