package analytics.clustering;

import data.LinkageTable;

import java.util.ArrayList;
import java.util.List;

/**
 * Class providing methods to get the agglomerative clusters of items, in the form of a linkage table, given
 * a distance matrix
 *
 * @author T. Methven, P. Le Bras
 * @version 3
 */
public class Agglomerative {

    // used for allowing approximation in floating points
    private static double epsilon = 0.0001;

    /** List of possible linkage types: UPGMA, Minimum (single) and Maximum (complete). */
    public enum LinkageType {UPGMA, MIN, MAX};

    public static LinkageTable cluster(double[][] distanceMatrix, String[] items, int nClusters, LinkageType linkageType) throws IllegalArgumentException{
        // check correct arguments
        if(nClusters > distanceMatrix.length) throw new IllegalArgumentException("Illegal number of clusters, cannot have more clusters than items in the distance matrix");
        if(nClusters < 1) throw new IllegalArgumentException("Illegal number of clusters, cannot have less than 1 cluster");
        // first check the validity of the matrix, may throw exception
        checkMatrix(distanceMatrix);

        // instantiate linkage table/cluster data
        LinkageTable table = new LinkageTable(distanceMatrix.length);

        // initialise list of clusters, start with all leaves
        List<Cluster> tmpClusters = new ArrayList<>(distanceMatrix.length);
        for(int i = 0; i < distanceMatrix.length; i++) tmpClusters.add(new Cluster(i,i));

        // make a copy of the matrix, will be reduced as clustering progresses
        double[][] workingMatrix = new double[distanceMatrix.length][distanceMatrix.length];
        for(int x = 0; x < distanceMatrix.length; x++){
            for(int y = 0; y < distanceMatrix.length; y++){
                workingMatrix[x][y] = distanceMatrix[x][y];
            }
        }

        int X = 0, Y = 0;
        double minDistance;
        int nodeIndex = distanceMatrix.length;

        while(workingMatrix.length > 1){
            // find the shortest remaining distance in the working matrix
            minDistance = Double.MAX_VALUE;
            for(int x = 0; x < distanceMatrix.length-1; x++){
                for(int y = x+1; y < distanceMatrix.length; y++){
                    if(workingMatrix[x][y] < minDistance){
                        minDistance = workingMatrix[x][y];
                        X = x;
                        Y = y;
                    }
                }
            }

            // add entry in linkage table
            table.addNode(tmpClusters.get(X).index, tmpClusters.get(Y).index, minDistance);

            // update the list of clusters
            // change index to incremented node index
            tmpClusters.get(X).index = nodeIndex++;
            // merge leaf nodes
            tmpClusters.get(X).leaves.addAll(tmpClusters.get(Y).leaves);
            // remove second cluster
            tmpClusters.remove(Y);

            // collapse the working matrix
            // make smaller matrix
            double[][] newMatrix = new double[workingMatrix.length-1][workingMatrix.length-1];
            // fill smaller matrix
            for(int x = 0; x < newMatrix.length-1; x++){
                for(int y = x+1; y < newMatrix.length; y++){
                    if(y == X){
                        // update distances where previous X column used to be
                        newMatrix[x][y] = switch (linkageType){
                            case UPGMA -> UPGMAValue(distanceMatrix, tmpClusters.get(X), tmpClusters.get(x));
                            case MAX -> maxValue(distanceMatrix, tmpClusters.get(X), tmpClusters.get(x));
                            case MIN -> minValue(distanceMatrix, tmpClusters.get(X), tmpClusters.get(x));
                        };
                    } else if (x == X){
                        // update distances where previous X row used to be
                        newMatrix[x][y] = switch (linkageType){
                            case UPGMA -> UPGMAValue(distanceMatrix, tmpClusters.get(X), tmpClusters.get(y));
                            case MAX -> maxValue(distanceMatrix, tmpClusters.get(X), tmpClusters.get(y));
                            case MIN -> minValue(distanceMatrix, tmpClusters.get(X), tmpClusters.get(y));
                        };
                    } else {
                        // copy distances
                        int tmpX = x, tmpY = y;
                        // skip previous Y column and row
                        if(x >= Y) tmpX++;
                        if(y >= Y) tmpY++;
                        newMatrix[x][y] = workingMatrix[tmpX][tmpY];
                    }
                }
            }
            // replace working matrix with new one
            workingMatrix = newMatrix;

            // if cluster number is reached, assign clusters
            if(tmpClusters.size() == nClusters) {
                for (Cluster c : tmpClusters) {
                    for (int l : c.leaves) {
                        table.addAssignment(items[l], Integer.toString(c.index));
                    }
                }
            }
        }

        return table;
    }

    // class representing a cluster in the hierarchy
    private static class Cluster{
        // cluster number (index in linkage table)
        public int index;
        // list of leaf node in the cluster
        public List<Integer> leaves;
        // initial constructor for leaves
        public Cluster(int i, int l){
            index = i;
            leaves = new ArrayList<>();
            leaves.add(l);
        }
    }

    // method returning the maximum distance between 2 clusters
    private static double maxValue(double[][] distances, Cluster cluster1, Cluster cluster2){
        double tmp = 0, max = 0;
        for(int leaf1: cluster1.leaves){
            for(int leaf2: cluster2.leaves){
                tmp = distances[leaf1][leaf2];
                if(tmp > max) max = tmp;
            }
        }
        return max;
    }

    // method returning the minimum distance between 2 clusters
    private static double minValue(double[][] distances, Cluster cluster1, Cluster cluster2){
        double tmp = 0, min = Double.MAX_VALUE;
        for(int leaf1: cluster1.leaves){
            for(int leaf2: cluster2.leaves){
                tmp = distances[leaf1][leaf2];
                if(tmp < min) min = tmp;
            }
        }
        return min;
    }

    // method returning the unweighted average distance between 2 clusters
    private static double UPGMAValue(double[][] distances, Cluster cluster1, Cluster cluster2){
        double sum = 0;
        int cnt = 0;
        for(int leaf1: cluster1.leaves){
            for(int leaf2: cluster2.leaves){
                sum += distances[leaf1][leaf2];
                cnt ++;
            }
        }
        return sum/(double)cnt;
    }

    // Method checking the validity of the distance matrix, throws exception if invalid
    private static void checkMatrix(double[][] distanceMatrix) throws IllegalArgumentException{
        if(!checkSquare(distanceMatrix)) throw new IllegalArgumentException("Distance matrix is not square");
        if(!checkIdentity(distanceMatrix)) throw new IllegalArgumentException("Distance matrix violates identity: d(x,x) = 0");
        if(!checkRange(distanceMatrix)) throw new IllegalArgumentException("Distance matrix violates range: 0 < d(x,y) < 1");
        if(!checkSymmetry(distanceMatrix)) throw new IllegalArgumentException("Distance matrix violates symmetry: d(x,y) = d(y,x");
        if(!checkTriangleInequality(distanceMatrix)) throw new IllegalArgumentException("Distance matrix violates triangle inequality: d(x,z) <= d(x,y) + d(y,z)");
    }

    // method check square matrix
    private static boolean checkSquare(double[][] matrix){
        int s = matrix.length;
        for(double[] row: matrix){
            if(row.length != s) return false;
        }
        return true;
    }

    // method checking identity d(x,x) = 1
    private static boolean checkIdentity(double[][] distance){
        for(int x = 0; x < distance.length; x++){
            if(approxInequal(distance[x][x], 0)) return false;
        }
        return true;
    }

    // method checking range 0 < d(x,y) < 1
    private static boolean checkRange(double[][] matrix){
        for(int x = 0; x < matrix.length-1; x++){
            for(int y = x+1; y < matrix.length; y++){
                if(matrix[x][y] < 0-epsilon || matrix[x][y] > 1+epsilon) return false;
            }
        }
        return true;
    }

    // method checking for symmetry d(x,y) = d(y,x)
    private static boolean checkSymmetry(double[][] matrix){
        for(int x = 0; x < matrix.length-1; x++){
            for(int y = x+1; y < matrix.length; y++){
                if(approxInequal(matrix[x][y], matrix[y][x])) return false;
            }
        }
        return true;
    }

    // method checking for triangle inequality d(x,z) <= d(x,y) + d(y,z)
    private static boolean checkTriangleInequality(double[][] distance){
        for (int i = 0; i < distance.length - 1; i++) {
            for (int j = i + 1; j < distance.length; j++) {
                for (int k = 0; k < distance.length; k++) {
                    if (k == i || k == j) continue;
                    if (distance[i][j] > distance[i][k] + distance[k][j] + epsilon) return false;
                }
            }
        }
        return true;
    }

    private static boolean approxInequal(double a, double b){
        return a > b+epsilon || a < b-epsilon;
    }


}
