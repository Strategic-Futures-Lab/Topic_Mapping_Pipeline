package data;

import java.util.Map;
import java.util.TreeMap;

/**
 * Class implementing a sparse vector data structure: while the vector has a fixed theoretical size, in practice,
 * only non-zero elements are saved.
 *
 * @author P. Le Bras
 * @version 1
 */
public class SparseVector {

    private static final double espilon = 0.0001;

    // container for non-zero values
    private TreeMap<Integer, Double> st;
    // theoretical/maximum size
    private int N;

    /**
     * Constructor
     * @param size Theoretical/maximum size of the vector
     */
    public SparseVector(int size){
        N = size;
        st = new TreeMap<>();
    }

    /**
     * Method inserting a new value at a given index.
     * If the value is zero, removes the entry from the internal TreeMap.
     * @param i Index to insert the value at
     * @param val Value to insert
     * @param safe Flag for safe insertion: if true and index is out of bound, throw an exception, otherwise (false) automatically grow the theoretical size
     */
    public void put(int i, double val, boolean safe){
        if(i < 0 || (i >= N && safe)) throw new RuntimeException("Error: SparseVector index is out of bounds");
        if(i >= N) N = i+1;
        if(val == 0.0) st.remove(i);
        else st.put(i, val);
    }

    /**
     * Method inserting a new value at a given index.
     * If the value is zero, removes the entry from the internal TreeMap.
     * Automatically grows the theoretical size if index is out of bounds.
     * @param i Index to insert the value at
     * @param val Value to insert
     */
    public void put(int i, double val){
        put(i, val, false);
    }

    /**
     * Method accessing a value at a given index.
     * If, internally, the TreeMap has no entry for this index, returns 0 by default.
     * If the index is out of bound (greater than theoretical size), returns 0 by default.
     * @param i Index to get the value from.
     * @param safe Flag for safe retrieval: if true and index is out of bound, throw an exception, otherwise (false) automatically return 0.
     * @return Value found, or 0 by default.
     */
    public double get(int i, boolean safe){
        if(i < 0 || (i >= N && safe)) throw new RuntimeException("Error: SparseVector index is out of bounds");
        if(st.containsKey(i)) return st.get(i);
        return 0.0;
    }

    /**
     * Method accessing a value at a given index.
     * If, internally, the TreeMap has no entry for this index, returns 0 by default.
     * If the index is out of bound (greater than theoretical size), returns 0 by default.
     * @param i Index to get the value from.
     * @return Value found, or 0 by default.
     */
    public double get(int i){
        return get(i, false);
    }

    /**
     * Getter method for the size of the vector
     * @return The vector size
     */
    public int size(){ return N; }

    /**
     * Method calculating the dot product between this and another SparseVector
     * @param b Other SparseVector of the same size
     * @param safe Flag for safe product: if true throws exception if the two vectors are not of the same size, if false theoretical sizes are automatically adjusted to match the greater
     * @return Dot product of the two vectors
     */
    public double dot(SparseVector b, boolean safe){
        SparseVector a = this;
        if(safe && a.N != b.N) throw new RuntimeException("Error : SparseVector lengths are not same");
        else if(a.N != b.N){
            int max = Math.max(a.N, b.N);
            a.N = max;
            b.N = max;
        }
        double sum = 0.0;
        // go through the smallest vector and only add products of keys in both sparse vectors
        if(a.st.size() <= b.st.size()) {
            for(Map.Entry<Integer, Double> entry: a.st.entrySet()) {
                if(b.st.containsKey(entry.getKey())){
                    sum += a.get(entry.getKey()) * b.get(entry.getKey());
                }
            }
        } else {
            for(Map.Entry<Integer, Double> entry: b.st.entrySet()) {
                if(a.st.containsKey(entry.getKey())){
                    sum += a.get(entry.getKey()) * b.get(entry.getKey());
                }
            }
        }
        return sum;
    }

    /**
     * Method calculating the dot product between this and another SparseVector.
     * Automatically adjusts Vector sizes to match the greater
     * @param b Other SparseVector of the same size
     * @return Dot product of the two vectors
     */
    public double dot(SparseVector b){
        return this.dot(b, false);
    }

    /**
     * Method multiplying this SparseVector by a scalar
     * @param r Scalar to multiply by
     * @return Multiplied SparseVector
     */
    public SparseVector mult(double r){
        SparseVector a = this;
        SparseVector b = new SparseVector(N);
        for(Map.Entry<Integer, Double> entry: a.st.entrySet()) {
            b.put(entry.getKey(), a.get(entry.getKey())*r);
        }
        return b;
    }

    /**
     * Method calculating the sum of this and another SparseVector
     * @param b Other SparseVector of the same size
     * @param safe Flag for safe product: if true throws exception if the two vectors are not of the same size, if false theoretical sizes are automatically adjusted to match the greater
     * @return Sum of the two SparseVectors
     */
    public SparseVector sum(SparseVector b, boolean safe) {
        SparseVector a = this;
        if(safe && a.N != b.N) throw new RuntimeException("Error : SparseVector lengths are not same");
        else if(a.N != b.N){
            int max = Math.max(a.N, b.N);
            a.N = max;
            b.N = max;
        }
        SparseVector c = new SparseVector(N);
        // copy all from a
        for(Map.Entry<Integer, Double> entry: a.st.entrySet()) {
            c.put(entry.getKey(), a.get(entry.getKey()));
        }
        // add all from b on top
        for(Map.Entry<Integer, Double> entry: b.st.entrySet()) {
            c.put(entry.getKey(), b.get(entry.getKey()) + c.get(entry.getKey()));
        }
        return c;
    }

    /**
     * Method calculating the sum of this and another SparseVector.
     * Automatically adjusts Vector sizes to match the greater
     * @param b Other SparseVector of the same size
     * @return Sum of the two vectors
     */
    public SparseVector sum(SparseVector b){
        return this.sum(b, false);
    }

    /**
     * Method calculating the difference between this and another SparseVector
     * @param b Other SparseVector of the same size
     * @param safe Flag for safe product: if true throws exception if the two vectors are not of the same size, if false theoretical sizes are automatically adjusted to match the greater
     * @return Difference between the two SparseVectors
     */
    public SparseVector diff(SparseVector b, boolean safe) {
        SparseVector a = this;
        if(safe && a.N != b.N) throw new RuntimeException("Error : SparseVector lengths are not same");
        else if(a.N != b.N){
            int max = Math.max(a.N, b.N);
            a.N = max;
            b.N = max;
        }
        // return a.sum(b.mult(-1));
        SparseVector c = new SparseVector(N);
        // copy all from a
        for(Map.Entry<Integer, Double> entry: a.st.entrySet()) {
            c.put(entry.getKey(), a.get(entry.getKey()));
        }
        // add all from b on top
        for(Map.Entry<Integer, Double> entry: b.st.entrySet()) {
            c.put(entry.getKey(), c.get(entry.getKey()) - b.get(entry.getKey()));
        }
        return c;
    }

    /**
     * Method calculating the difference between this and another SparseVector.
     * Automatically adjusts Vector sizes to match the greater
     * @param b Other SparseVector of the same size
     * @return Difference between the two vectors
     */
    public SparseVector diff(SparseVector b){
        return this.diff(b, false);
    }

    /**
     * Method returning the norm/magnitude of the SparseVector
     * @return The vector's norm
     */
    public double norm(){
        double sum = 0.0;
        for(Map.Entry<Integer, Double> entry: st.entrySet()){
            sum += entry.getValue() * entry.getValue();
        }
        return Math.sqrt(sum);
    }

    /**
     * Method returning the square-root of this SparseVector
     * @return New vector with square-rooted values
     */
    public SparseVector sqrt(){
        SparseVector a = new SparseVector(N);
        for(Map.Entry<Integer, Double> entry: st.entrySet()){
            a.put(entry.getKey(), Math.sqrt(entry.getValue()));
        }
        return a;
    }

    /**
     * Method returning the sum of all vector components
     * @return Sum of sparse vector components
     */
    public double total(){
        double tot=0;
        for(Map.Entry<Integer, Double> entry: st.entrySet()){
            tot += entry.getValue();
        }
        return tot;
    }

    /**
     * Method normalising the vector (ie, total = 1)
     */
    public void normalise(){
        double n = total();
        for(Map.Entry<Integer, Double> entry: st.entrySet()){
            this.put(entry.getKey(), entry.getValue()/n);
        }
    }

    /**
     * Static method calculating the Hellinger distance between two sparse vectors
     * @param a First vector
     * @param b Second vector
     * @return The Hellinger distance between a and b
     */
    public static double HellingerDistance(SparseVector a, SparseVector b){
        // Checking that vector a and b have been normalised
        if(a.total() > 1+espilon || a.total() < 1-espilon){
            a.normalise();
        }
        if(b.total() > 1+espilon || b.total() < 1-espilon){
            b.normalise();
        }
        // Get the square root of both vectors
        SparseVector a_r = a.sqrt();
        SparseVector b_r = b.sqrt();
        // Calculate the difference
        SparseVector d = a_r.diff(b_r);
        // Return the difference norm divided by sqrt(2)
        return d.norm()/Math.sqrt(2);
    }

    /**
     * Static method calculating the Cosine similarity between two sparse vectors
     * @param a First vector
     * @param b Second vector
     * @return The Cosine similarity between a and b
     */
    public static double CosineSimilarity(SparseVector a, SparseVector b){
        // get numerator: dot product
        double num = a.dot(b);
        // get denominator: product of magnitudes
        double denom = a.norm() * b.norm();
        // Return dot product over product of magnitudes
        return num/denom;
    }

    /**
     * Static method calculating the Cosine distance between two sparse vectors
     * @param a First vector
     * @param b Second vector
     * @return The Cosine distance between a and b
     */
    public static double CosineDistance(SparseVector a, SparseVector b){
        // reverse of similarity
        return 1 - CosineSimilarity(a,b);
    }
}
