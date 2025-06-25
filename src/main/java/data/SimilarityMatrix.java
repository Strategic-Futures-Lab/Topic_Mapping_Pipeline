package data;

import IO.Console;
import IO.JSONHelper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.*;

/**
 * Class representing a similarity matrix.
 * Provides methods for loading/writing similarity files.
 *
 * @author P. Le Bras
 * @version 1
 */
public class SimilarityMatrix {

    // List of static fields used when reading/writing JSON files
    private static final String JSON_ROWS = "rowItems";
    private static final String JSON_COLUMNS = "columnItems";
    private static final String JSON_ITEMS = "items";
    private static final String JSON_MATRIX = "similarities";

    // true if rows and columns are the same instances
    private boolean symmetric;
    private List<String> rowItems;
    private List<String> columnItems;
    private List<String> items;
    private List<List<Double>> matrix;

    /**
     * Initial constructor
     * @param s Flag for symmetric matrix
     */
    public SimilarityMatrix(boolean s){
        symmetric = s;
        if(symmetric){
            items = new ArrayList<>();
        } else {
            rowItems = new ArrayList<>();
            columnItems = new ArrayList<>();
        }
    }

    /**
     * Initial constructor for a symmetric matrix
     * @param size Size of matrix
     */
    public SimilarityMatrix(int size){
        items = new ArrayList<>(size);
        symmetric = true;
    }

    /**
     * Initial constructor for an asymmetric matrix
     * @param nRows Number of rows
     * @param nColumns Number of columns
     */
    public SimilarityMatrix(int nRows, int nColumns){
        rowItems = new ArrayList<>(nRows);
        columnItems = new ArrayList<>(nColumns);
        symmetric = false;
    }

    /**
     * Adds an item to the list of items
     * @param name Name of item to add
     * @throws RuntimeException if the matrix is asymmetric or if the item is already present in list
     */
    public void addItem(String name) throws RuntimeException {
        if(!symmetric) throw new RuntimeException("Invalid method call for asymmetric matrix; use addRowItem or addColumnItem instead");
        if(items.contains(name)) throw new RuntimeException("Item already exists");
        items.add(name);
    }

    /**
     * Adds an item to the list of items
     * @param index Index for item insertion
     * @param name Name of item to add
     * @throws RuntimeException if the matrix is asymmetric or if the item is already present in list
     */
    public void addItem(int index, String name) throws RuntimeException {
        if(!symmetric) throw new RuntimeException("Invalid method call for asymmetric matrix; use addRowItem or addColumnItem instead");
        if(items.contains(name)) throw new RuntimeException("Item already exists");
        items.add(index, name);
    }

    /**
     * Adds an item to the list of row items
     * @param name Name of item to add
     * @throws RuntimeException if the item is already present in list
     */
    public void addRowItem(String name) {
        if(symmetric) addItem(name);
        else {
            if(rowItems.contains(name)) throw new RuntimeException("Item already exists");
            rowItems.add(name);
        }
    }

    /**
     * Adds an item to the list of row items
     * @param index Index for item insertion
     * @param name Name of item to add
     * @throws RuntimeException if the item is already present in list
     */
    public void addRowItem(int index, String name) {
        if(symmetric) addItem(index, name);
        else {
            if(rowItems.contains(name)) throw new RuntimeException("Item already exists");
            rowItems.add(name);
        }
    }

    /**
     * Adds an item to the list of column items
     * @param name Name of item to add
     * @throws RuntimeException if the item is already present in list
     */
    public void addColumnItem(String name) {
        if(symmetric) addItem(name);
        else {
            if(columnItems.contains(name)) throw new RuntimeException("Item already exists");
            columnItems.add(name);
        }
    }

    /**
     * Adds an item to the list of column items
     * @param index Index for item insertion
     * @param name Name of item to add
     * @throws RuntimeException if the item is already present in list
     */
    public void addColumnItem(int index, String name) {
        if(symmetric) addItem(index, name);
        else {
            if(columnItems.contains(name)) throw new RuntimeException("Item already exists");
            columnItems.add(name);
        }
    }

    private void initialiseMatrix(){
        if(symmetric){
            if(items.isEmpty()) throw new RuntimeException("The list of items needs to be set before initialising the matrix");
            // symmetric matrices will only save items below the diagonal for efficiency
            matrix = new ArrayList<>(items.size()-1);
            for(int r = 1; r < items.size(); r++){
                matrix.add(new ArrayList<>(r));
            }
        } else {
            if(rowItems.isEmpty()) throw new RuntimeException("The list of row items needs to be set before initialising the matrix");
            if(columnItems.isEmpty()) throw new RuntimeException("The list of column items needs to be set before initialising the matrix");
            matrix = new ArrayList<>(rowItems.size());
            for(int r = 0; r < rowItems.size(); r++){
                matrix.add(new ArrayList<>(columnItems.size()));
            }
        }
    }

    /**
     * Methods to add a similarity value in the matrix
     * @param rowIndex Index of matrix row where the value should be added
     * @param columnIndex Index of matrix column where the value should be added
     * @param similarity Value to add
     */
    public void addSimilarity(int rowIndex, int columnIndex, double similarity){
        if(matrix == null) initialiseMatrix();
        if(symmetric){
            if(columnIndex > rowIndex){
                matrix.get(columnIndex-1).add(rowIndex, similarity);
            } else if(rowIndex > columnIndex){
                matrix.get(rowIndex-1).add(columnIndex, similarity);
            }
        } else {
            matrix.get(rowIndex).add(columnIndex, similarity);
        }
    }

    /**
     * Methods to add a similarity value in the matrix
     * @param rowItem Name of item in the matrix rows where the value should be added
     * @param columnItem Name of item in the matrix columns where the value should be added
     * @param similarity Value to add
     */
    public void addSimilarity(String rowItem, String columnItem, double similarity){
        int rowIndex; int columnIndex;
        if(symmetric){
            rowIndex = items.lastIndexOf(rowItem);
            columnIndex = items.lastIndexOf(columnItem);
        } else {
            rowIndex = rowItems.lastIndexOf(rowItem);
            columnIndex = columnItems.lastIndexOf(columnItem);
        }
        addSimilarity(rowIndex, columnIndex, similarity);
    }

    /**
     * Method returning a similarity given row and column indices
     * @param rowIndex Row index to get similarity from
     * @param columnIndex Column index to get similarity from
     * @return The similarity
     */
    public double getSimilarity(int rowIndex, int columnIndex){
        if(matrix == null) throw new RuntimeException("Matrix is empty");
        if(symmetric){
            if(columnIndex > rowIndex){
                return matrix.get(columnIndex-1).get(rowIndex);
            } else if(rowIndex > columnIndex){
                return matrix.get(rowIndex-1).get(columnIndex);
            } else {
                return 1;
            }
        } else {
            return matrix.get(rowIndex).get(columnIndex);
        }
    }

    /**
     * Method returning a similarity given row and column indices
     * @param rowItem Name of item in the matrix rows to get similarity from
     * @param columnItem Name of item in the matrix column to get similarity from
     * @return The similarity
     */
    public double getSimilarity(String rowItem, String columnItem){
        int rowIndex; int columnIndex;
        if(symmetric){
            rowIndex = items.lastIndexOf(rowItem);
            columnIndex = items.lastIndexOf(columnItem);
        } else {
            rowIndex = rowItems.lastIndexOf(rowItem);
            columnIndex = columnItems.lastIndexOf(columnItem);
        }
        return getSimilarity(rowIndex, columnIndex);
    }

    /**
     * Method returning the similarity matrix as a 2-dimensional array
     * @return The similarity matrix
     */
    public double[][] getSimilarityMatrix(){
        double[][] sMatrix;
        if(symmetric) {
            sMatrix = new double[items.size()][items.size()];
            for(int i = 0; i < items.size(); i++){
                for(int j = i; j < items.size(); j++){
                    sMatrix[i][j] = getSimilarity(i, j);
                    sMatrix[j][i] = getSimilarity(i, j);
                }
            }
        } else {
            sMatrix = new double[rowItems.size()][columnItems.size()];
            for(int i = 0; i < rowItems.size(); i++){
                for(int j = 0; j < columnItems.size(); j++){
                    sMatrix[i][j] = getSimilarity(i, j);
                }
            }
        }
        return sMatrix;
    }

    /**
     * Method returning the distance matrix as a 2-dimensional array
     * @return The distance matrix
     */
    public double[][] getDistanceMatrix(){
        double[][] dMatrix;
        if(symmetric) {
            dMatrix = new double[items.size()][items.size()];
            for(int i = 0; i < items.size(); i++){
                for(int j = i; j < items.size(); j++){
                    dMatrix[i][j] = 1 - getSimilarity(i, j);
                    dMatrix[j][i] = 1 - getSimilarity(i, j);
                }
            }
        } else {
            dMatrix = new double[rowItems.size()][columnItems.size()];
            for(int i = 0; i < rowItems.size(); i++){
                for(int j = 0; j < columnItems.size(); j++){
                    dMatrix[i][j] = 1 - getSimilarity(i, j);
                }
            }
        }
        return dMatrix;
    }

    /**
     * Loads a similarity matrix from a JSON file
     * @param filename File to load similarity matrix from
     * @throws IOException If there is an error with loading the file
     * @throws ParseException If there is an error with parsing JSON
     */
    public void loadSimilarities(String filename) throws IOException, ParseException {
        try {
            JSONObject input = JSONHelper.loadJSON(filename);
            if(input.containsValue(JSON_ITEMS)){
                symmetric = true;
                items = Arrays.stream(JSONHelper.getStringArray((JSONArray) input.get(JSON_ITEMS))).toList();
            } else {
                symmetric = false;
                rowItems = Arrays.stream(JSONHelper.getStringArray((JSONArray) input.get(JSON_ROWS))).toList();
                columnItems = Arrays.stream(JSONHelper.getStringArray((JSONArray) input.get(JSON_COLUMNS))).toList();
            }
            matrix = new ArrayList<>();
            for(JSONArray row: JSONHelper.getJSONArrayArray((JSONArray) input.get(JSON_MATRIX))){
                ArrayList<Double> r = new ArrayList<>();
                for(double d: JSONHelper.getDoubleArray(row)) r.add(d);
                matrix.add(r);
            }
        } catch (IOException e) {
            Console.error("Loading similarity file "+filename+" failed");
            throw e;
        } catch (ParseException e) {
            Console.error("Parsing similarity file "+filename+" failed");
            throw e;
        }
    }

    /**
     * Writes the similarity matrix on a JSON file
     * @param filename File to write similarity matrix on
     * @throws IOException If there is an error with writing the file
     */
    public void writeSimilarities(String filename) throws IOException {
        try {
            JSONObject root = new JSONObject();
            if(symmetric){
                JSONArray jsonItems = new JSONArray();
                for(String s: items) jsonItems.add(s);
                root.put(JSON_ITEMS, jsonItems);
            } else {
                JSONArray jsonRows = new JSONArray();
                for(String s: rowItems) jsonRows.add(s);
                root.put(JSON_ROWS, jsonRows);
                JSONArray jsonColumns = new JSONArray();
                for(String s: columnItems) jsonColumns.add(s);
                root.put(JSON_COLUMNS, jsonColumns);
            }
            JSONArray matrixJson = new JSONArray();
            for(List<Double> row: matrix) {
                JSONArray rowJson = new JSONArray();
                rowJson.addAll(row.stream().map(this::formatDouble).toList());
                matrixJson.add(rowJson);
            }
            root.put(JSON_MATRIX, matrixJson);
            JSONHelper.saveJSON(root, filename);
        } catch (IOException e){
            Console.error("Saving model file "+filename+" failed");
            throw e;
        }
    }

    private double formatDouble(double in){
        DecimalFormat df = new DecimalFormat("#.#####");
        df.setRoundingMode(RoundingMode.HALF_UP);
        return Double.parseDouble(df.format(in));
    }

}
