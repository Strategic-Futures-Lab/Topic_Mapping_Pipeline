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
     * Method inserting a new value at a given index
     * If the value is zero, removes the entry from the internal TreeMap
     * @param i Index to insert the value at
     * @param val Value to insert
     */
    public void put(int i, double val){
        if(i < 0 || i >= N) throw new RuntimeException("Error: SparseVector index is out of bounds");
        if(val == 0.0) st.remove(i);
        else st.put(i, val);
    }

    /**
     * Method accessing a value at a given index.
     * If, internally, the TreeMap has no entry for this index, return 0 by default.
     * @param i Index to get the value from.
     * @return Value found, or 0 by default.
     */
    public double get(int i){
        if(i < 0 || i >= N) throw new RuntimeException("Error: SparseVector index is out of bounds");
        if(st.containsKey(i)) return st.get(i);
        return 0.0;
    }

    /**
     * Getter method for the size of the vector
     * @return The vector size
     */
    public int size(){ return N; }

    /**
     * Method calculating the dot product between this and another SparseVector
     * @param b Other SparseVector of the same size
     * @return Dot product of the two vectors
     */
    public double dot(SparseVector b){
        SparseVector a = this;
        if(a.N != b.N) throw new RuntimeException("Error : SparseVector lengths are not same");
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
     * @return Sum of the two SparseVectors
     */
    public SparseVector sum(SparseVector b) {
        SparseVector a = this;
        if(a.N != b.N) throw new RuntimeException("Error : SparseVector lengths are not same");
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
     * Method calculating the difference between this and another SparseVector
     * @param b Other SparseVector of the same size
     * @return Difference between the two SparseVectors
     */
    public SparseVector diff(SparseVector b) {
        SparseVector a = this;
        if(a.N != b.N) throw new RuntimeException("Error : SparseVector lengths are not same");
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
     * Method returning the norm of the SparseVector
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
     * Method returning the vector normalised (ie, total = 1)
     * @return Normalised vector
     */
    public SparseVector normalise(){
        SparseVector a = new SparseVector(N);
        double n = total();
        for(Map.Entry<Integer, Double> entry: st.entrySet()){
            a.put(entry.getKey(), entry.getValue()/n);
        }
        return a;
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
            throw new RuntimeException("Error : SparseVector is not normalised");
        }
        if(b.total() > 1+espilon || b.total() < 1-espilon){
            throw new RuntimeException("Error : SparseVector is not normalised");
        }
        // Get the square root of both vectors
        SparseVector a_r = a.sqrt();
        SparseVector b_r = b.sqrt();
        // Calculate the difference
        SparseVector d = a_r.diff(b_r);
        // Return the difference norm divided by sqrt(2)
        return d.norm()/Math.sqrt(2);
    }
}
