package P4_Analysis.TopicClustering;

import java.util.ArrayList;
import java.util.List;

public class AgglomerativeClustering {
    public enum LINKAGE_TYPE {AVERAGE, MIN, MAX};

    public static class ClusterRow {
        public int Node1;
        public int Node2;
        public double Distance;

        public ClusterRow(int Node1, int Node2, double Distance) {
            this.Node1 = Node1;
            this.Node2 = Node2;
            this.Distance = Distance;
        }
    }

    public static class ColumnDetails {
        public int NodeNumber;
        public List<Integer> ContainsNodes = new ArrayList<>();

        public ColumnDetails(int NodeNumber) {
            this.NodeNumber = NodeNumber;
        }

        public ColumnDetails(int NodeNumber, int initialNode) {
            this.NodeNumber = NodeNumber;
            AddContainingNode(initialNode);
        }

        public void AddContainingNode(int node) {
            ContainsNodes.add(node);
        }
    }

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

    public static List<ClusterRow> PerformClustering(double[][] dissimilarityMatrix, LINKAGE_TYPE clusterType) {
        System.out.println("Performing Clustering and Creating Linkage Table ...");

        CheckSimilarityMatrixValidity(dissimilarityMatrix);

        //Output in the Matlab-style linkage table
        List<ClusterRow> linkageTable = new ArrayList<>();

        //Keep track of the nodes which are in each column, as we'll need these for later. It contains the number of the
        //node for the linkage table, and what leaf nodes it contains.
        List<ColumnDetails> columnLabels = new ArrayList<>();

        for(int i = 0; i < dissimilarityMatrix.length; i++) {
            columnLabels.add(new ColumnDetails(i, i));
        }

        //New data structure to keep track of the collapsing similarity matrix we need for UPMGA
        double[][] UPMGAMatrix = new double[dissimilarityMatrix.length][dissimilarityMatrix.length];
        for(int y = 0; y < dissimilarityMatrix.length; y++) {
            for(int x = 0; x < dissimilarityMatrix.length; x++) {
                UPMGAMatrix[x][y] = dissimilarityMatrix[x][y];
            }
        }

        int xLoc = 0, yLoc = 0;
        double shortestDistance;
        int nodeVal = dissimilarityMatrix.length;
        int escape = 0;

        //Iterate through this process until we have reduced the similarity matrix to a single distance
        while(UPMGAMatrix.length > 1 && escape < 1000) {
            //Find the current shortest distance in the collapsed array...
            shortestDistance = Double.MAX_VALUE;
            for(int x = 0; x < UPMGAMatrix.length; x++) {
                for(int y = x + 1; y < UPMGAMatrix.length; y++) {
                    if(UPMGAMatrix[x][y] < shortestDistance) {
                        shortestDistance = UPMGAMatrix[x][y];
                        xLoc = x;
                        yLoc = y;
                    }
                }
            }

            //Add a record in the linkage table for this join!
            linkageTable.add(new ClusterRow(columnLabels.get(xLoc).NodeNumber, columnLabels.get(yLoc).NodeNumber, shortestDistance));

            //Update the label array. First, we change the label where the first item found was to be the new, higher
            //nod value, then we merge the items contained within the two positions.
            columnLabels.get(xLoc).NodeNumber = nodeVal++;
            columnLabels.get(xLoc).ContainsNodes.addAll(columnLabels.get(yLoc).ContainsNodes);

            //Finally, remove the second item found as it has been merged into the first!
            columnLabels.remove(yLoc);

            int tempX = 0, tempY = 0;
            double[][] NewUPMGAMatrix = new double[UPMGAMatrix.length - 1][UPMGAMatrix.length - 1];
            for(int x = 0; x < NewUPMGAMatrix.length; x++) {
                for(int y = x + 1; y < NewUPMGAMatrix.length; y++) {
                    if(y == xLoc) {
                        if(clusterType == LINKAGE_TYPE.AVERAGE)
                            NewUPMGAMatrix[x][y] = GetUPMGAValue(dissimilarityMatrix, columnLabels, xLoc, x);
                        else if(clusterType == LINKAGE_TYPE.MAX)
                            NewUPMGAMatrix[x][y] = GetMaxValue(dissimilarityMatrix, columnLabels, xLoc, x);
                        else if(clusterType == LINKAGE_TYPE.MIN)
                            NewUPMGAMatrix[x][y] = GetMinValue(dissimilarityMatrix, columnLabels, xLoc, x);
                    } else if(x == xLoc) {
                        if(clusterType == LINKAGE_TYPE.AVERAGE)
                            NewUPMGAMatrix[x][y] = GetUPMGAValue(dissimilarityMatrix, columnLabels, xLoc, y);
                        else if(clusterType == LINKAGE_TYPE.MAX)
                            NewUPMGAMatrix[x][y] = GetMaxValue(dissimilarityMatrix, columnLabels, xLoc, y);
                        else if(clusterType == LINKAGE_TYPE.MIN)
                            NewUPMGAMatrix[x][y] = GetMinValue(dissimilarityMatrix, columnLabels, xLoc, y);
                    } else {
                        tempX = x;
                        tempY = y;
                        if(x >= yLoc)
                            tempX++;
                        if(y >= yLoc)
                            tempY++;

                        NewUPMGAMatrix[x][y] = UPMGAMatrix[tempX][tempY];
                    }
                }
            }

            UPMGAMatrix = NewUPMGAMatrix;
            escape++;
        }

        System.out.println("Clustering and Linkage Table Creation Complete!");
        return linkageTable;
    }

    private static double GetUPMGAValue(double[][] similarityMatrix, List<ColumnDetails> columnLabels, int minLoc, int mergeLoc) {
        double totalValue = 0;
        int count = 0;

        for(int labelValue1 = 0; labelValue1 < columnLabels.get(minLoc).ContainsNodes.size(); labelValue1++) {
            for(int labelValue2 = 0; labelValue2 < columnLabels.get(mergeLoc).ContainsNodes.size(); labelValue2++) {
                totalValue += similarityMatrix[columnLabels.get(mergeLoc).ContainsNodes.get(labelValue2)]
                        [columnLabels.get(minLoc).ContainsNodes.get(labelValue1)];
                count++;
            }
        }

        return totalValue / (double)count;
    }

    private static double GetMaxValue(double[][] similarityMatrix, List<ColumnDetails> columnLabels, int minLoc, int mergeLoc) {
        double tempValue = 0, maxValue = 0;

        for(int labelValue1 = 0; labelValue1 < columnLabels.get(minLoc).ContainsNodes.size(); labelValue1++) {
            for(int labelValue2 = 0; labelValue2 < columnLabels.get(mergeLoc).ContainsNodes.size(); labelValue2++) {
                tempValue = similarityMatrix[columnLabels.get(mergeLoc).ContainsNodes.get(labelValue2)]
                        [columnLabels.get(minLoc).ContainsNodes.get(labelValue1)];

                if(tempValue > maxValue)
                    maxValue = tempValue;
            }
        }

        return maxValue;
    }

    private static double GetMinValue(double[][] similarityMatrix, List<ColumnDetails> columnLabels, int minLoc, int mergeLoc) {
        double tempValue = 0, minValue = Double.MAX_VALUE;

        for(int labelValue1 = 0; labelValue1 < columnLabels.get(minLoc).ContainsNodes.size(); labelValue1++) {
            for(int labelValue2 = 0; labelValue2 < columnLabels.get(mergeLoc).ContainsNodes.size(); labelValue2++) {
                tempValue = similarityMatrix[columnLabels.get(mergeLoc).ContainsNodes.get(labelValue2)]
                        [columnLabels.get(minLoc).ContainsNodes.get(labelValue1)];

                if(tempValue < minValue)
                    minValue = tempValue;
            }
        }

        return minValue;
    }

    private static void CheckSimilarityMatrixValidity(double[][] similarityMatrix) {
        System.out.println("Checking similarity matrix for validity...");
        for(int y = 0; y < similarityMatrix.length; y++) {
            for(int x = 0; x < similarityMatrix.length; x++) {
                if(similarityMatrix[x][y] != similarityMatrix[y][x]) {
                    System.out.println("\n**********\nERROR IN SIMILARITY MATRIX!\n**********\nSimilarity matrix is not symmetrical as [x][y] != [y][x] at location x: " + x + " | y: " + y);
                    System.exit(1);
                }
            }
        }

        System.out.println("Similarity matrix is valid!");
    }
}
