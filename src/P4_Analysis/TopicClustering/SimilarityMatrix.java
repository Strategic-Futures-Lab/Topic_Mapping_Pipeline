package P4_Analysis.TopicClustering;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class SimilarityMatrix {

    HashMap<Integer, HashMap<Integer, Double>> matrix;

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

    public SimilarityMatrix(JSONArray jsonMatrix){
        this(getMatrixFromJSON(jsonMatrix));
    }

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

    public double getValue(int rowIndex, int cellIndex){
        return matrix.get(rowIndex).get(cellIndex);
    }

    public SimilarityMatrix getSubMatrix(int[] indices){
        double[][] copy = new double[indices.length][indices.length];
        for(int i = 0; i < indices.length; i++){
            for(int j = 0; j < indices.length; j++){
                copy[i][j] = getValue(indices[i], indices[j]);
            }
        }
        return new SimilarityMatrix(copy);
    }

    public double[][] getDissimilarityMatrix(){
        double[][] dMatrix = new double[matrix.size()][matrix.size()];
        for(Map.Entry<Integer, HashMap<Integer, Double>> row: matrix.entrySet()){
            for(Map.Entry<Integer, Double> cell: row.getValue().entrySet()){
                dMatrix[row.getKey()][cell.getKey()] = 1 - cell.getValue();
            }
        }
        return dMatrix;
    }

    public double[][] getSissimilarityMatrix(){
        double[][] sMatrix = new double[matrix.size()][matrix.size()];
        for(Map.Entry<Integer, HashMap<Integer, Double>> row: matrix.entrySet()){
            for(Map.Entry<Integer, Double> cell: row.getValue().entrySet()){
                sMatrix[row.getKey()][cell.getKey()] = cell.getValue();
            }
        }
        return sMatrix;
    }

    public JSONArray toJSON(){
        JSONArray root = new JSONArray();
        double[][] sMatrix = getSissimilarityMatrix();
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
