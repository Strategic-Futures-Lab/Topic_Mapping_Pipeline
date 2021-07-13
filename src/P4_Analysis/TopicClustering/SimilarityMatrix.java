package P4_Analysis.TopicClustering;

import org.json.simple.JSONArray;

import java.util.HashMap;
import java.util.Map;

/**
 * Class implementing a similarity matrix between topics.
 *
 * @author P. Le Bras
 * @version 1
 */
public class SimilarityMatrix {

    /** Matrix. */
    HashMap<Integer, HashMap<Integer, Double>> matrix;

    /**
     * Constructor.
     * @param matrix Matrix as a 2 dimensional array.
     */
    public SimilarityMatrix(double[][] matrix){
        this.matrix = new HashMap<>();
        for(int i = 0; i < matrix.length; i++){
            HashMap<Integer, Double> row = new HashMap<>();
            for(int j = 0; j < matrix[i].length; j++){
                row.put(j, matrix[i][j]);
            }
            this.matrix.put(i, row);
        }
    }

    /**
     * Constructor.
     * @param jsonMatrix Matrix as a 2 dimensional JSON array.
     */
    public SimilarityMatrix(JSONArray jsonMatrix){
        this(getMatrixFromJSON(jsonMatrix));
    }

    /**
     * Method reading a JSON matrix.
     * @param jsonMatrix Matrix as a 2 dimensional JSON array.
     * @return The matrix as a 2 dimensional array.
     */
    private static double[][] getMatrixFromJSON(JSONArray jsonMatrix){
        double[][] m = new double[jsonMatrix.size()][jsonMatrix.size()];
        int r = 0;
        for(JSONArray row: (Iterable<JSONArray>) jsonMatrix){
            int c = 0;
            for(double cell: (Iterable<Double>) row){
                m[r][c++] = cell;
            }
            r += 1;
        }
        return m;
    }

    /**
     * Method getting a value in the matrix at given indices.
     * @param rowIndex Row index.
     * @param cellIndex Cell/column index.
     * @return The value at the given indices.
     */
    public double getValue(int rowIndex, int cellIndex){
        return matrix.get(rowIndex).get(cellIndex);
    }

    /**
     * Method getting the sub matrix corresponding to the given indices.
     * @param indices Indices to get sub matrix from.
     * @return The sub matrix.
     */
    public SimilarityMatrix getSubMatrix(int[] indices){
        double[][] copy = new double[indices.length][indices.length];
        for(int i = 0; i < indices.length; i++){
            for(int j = 0; j < indices.length; j++){
                copy[i][j] = getValue(indices[i], indices[j]);
            }
        }
        return new SimilarityMatrix(copy);
    }

    /**
     * Method returning the dissimilarity/distance matrix as a 2 dimensional array.
     * @return The dissimilarity matrix.
     */
    public double[][] getDissimilarityMatrix(){
        double[][] dMatrix = new double[matrix.size()][matrix.size()];
        for(Map.Entry<Integer, HashMap<Integer, Double>> row: matrix.entrySet()){
            for(Map.Entry<Integer, Double> cell: row.getValue().entrySet()){
                dMatrix[row.getKey()][cell.getKey()] = 1 - cell.getValue();
            }
        }
        return dMatrix;
    }

    /**
     * Method returning the similarity matrix as a 2 dimensional array.
     * @return The similarity matrix.
     */
    public double[][] getSimilarityMatrix(){
        double[][] sMatrix = new double[matrix.size()][matrix.size()];
        for(Map.Entry<Integer, HashMap<Integer, Double>> row: matrix.entrySet()){
            for(Map.Entry<Integer, Double> cell: row.getValue().entrySet()){
                sMatrix[row.getKey()][cell.getKey()] = cell.getValue();
            }
        }
        return sMatrix;
    }

    /**
     * Method returning the similarity matrix as a 2 dimensional JSON array.
     * @return The similarity matrix, in JSON format.
     */
    public JSONArray toJSON(){
        JSONArray root = new JSONArray();
        double[][] sMatrix = getSimilarityMatrix();
        for(int y = 0; y < sMatrix.length; y++){
            JSONArray SimRow = new JSONArray();
            for(int x = 0; x < sMatrix.length; x++){
                SimRow.add(sMatrix[x][y]);
            }
            root.add(SimRow);
        }
        return root;
    }
}
