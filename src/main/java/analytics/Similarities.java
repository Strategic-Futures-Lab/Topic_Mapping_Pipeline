package analytics;

import data.SparseVector;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Class providing static methods for computing similarity and distance measures
 */
public class Similarities {

    /**
     * Method calculating the Hellinger distance between two sparse vectors
     * @param a First vector
     * @param b Second vector
     * @return The Hellinger distance between a and b
     */
    public static double HellingerDistance(SparseVector a, SparseVector b){
        // Checking that vector a and b have been normalised
        if(a.total() > 1+SparseVector.espilon || a.total() < 1-SparseVector.espilon){
            a.normalise();
        }
        if(b.total() > 1+SparseVector.espilon || b.total() < 1-SparseVector.espilon){
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
     * Method calculating the Hellinger similarity between two sparse vectors
     * @param a First vector
     * @param b Second vector
     * @return The Hellinger similarity between a and b
     */
    public static double HellingerSimilarity(SparseVector a, SparseVector b){
        // reverse of distance
        return 1 - HellingerDistance(a, b);
    }

    /**
     * Method calculating the Cosine similarity between two sparse vectors
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
     * Method calculating the Cosine distance between two sparse vectors
     * @param a First vector
     * @param b Second vector
     * @return The Cosine distance between a and b
     */
    public static double CosineDistance(SparseVector a, SparseVector b){
        // reverse of similarity
        return 1 - CosineSimilarity(a,b);
    }

    /**
     * Method calculating the Jaccard similarity (Jaccard Index) between two lists
     * @param a First list
     * @param b Second list
     * @return The Jaccard similarity between a and b
     * @param <E> Type of object in the lists, its equals implementation will be used to estimate union and intersection
     */
    public static <E> double JaccardSimilarity(List<E> a, List<E> b){
        // get union of lists
        Set<E> union = new HashSet<>();
        union.addAll(a);
        union.addAll(b);
        // get intersection of lists
        Set<E> intersection = a.stream()
            .distinct()
            .filter(b::contains)
            .collect(Collectors.toSet());
        // return size of intersection over size of union
        return (double) intersection.size() / (double) union.size();
    }

    /**
     * Method calculating the Jaccard distance (reverse Jaccard Index) between two lists
     * @param a First list
     * @param b Second list
     * @return The Jaccard distance between a and b
     * @param <E> Type of object in the lists, its equals implementation will be used to estimate union and intersection
     */
    public static <E> double JaccardDistance(List<E> a, List<E> b){
        // reverse of similarity
        return 1 - JaccardSimilarity(a,b);
    }

    /**
     * Method calculating the Average Jaccard Similarity between two lists, as described by Greene et al. 2014.
     * Will calculate Jaccard Similarity over growing sublist, and produce an average of all measures, hence
     * it prioritises (mis)matches in early sublists (e.g., top words)
     * @param a First list
     * @param b Second list
     * @return The average Jaccard similarity between a and b
     * @param <E> Type of object in the lists, its equals implementation will be used to estimate union and intersection
     */
    public static <E> double AverageJaccardSimilarity(List<E> a, List<E> b){
        int maxIterations = Math.min(a.size(), b.size());
        double sumSimilarities = 0;
        // run Jaccard index over the growing lists
        for(int i = 0; i<maxIterations; i++){
            sumSimilarities += JaccardSimilarity(a.subList(0,i+1),b.subList(0,i+1));
        }
        // return average similarities
        return sumSimilarities / maxIterations;
    }

    /**
     * Method calculating the Average Jaccard Distance between two lists (reverse of similarity)
     * @param a First list
     * @param b Second list
     * @return The average Jaccard distance between a and b
     * @param <E> Type of object in the lists, its equals implementation will be used to estimate union and intersection
     */
    public static <E> double AverageJaccardDistance(List<E> a, List<E> b){
        // reverse of similarity
        return 1 - AverageJaccardSimilarity(a, b);
    }

    /**
     * Method calculating the Minkowski Distance between two SparseVectors
     * @param a First vector
     * @param b Second vector
     * @param p Order of the distance, 1 for L1-norm (Manhattan), 2 for L2-norm (Euclidean), etc.
     * @return The Minskowski distance between a and b for order p
     */
    public static double MinkowskiDistance(SparseVector a, SparseVector b, int p){
        SparseVector diff = a.diff(b);
        List<Double> values = diff.getValues();
        double sum = 0;
        for(int i = 0; i < values.size(); i++){
            sum += Math.pow(Math.abs(values.get(i)), p);
        }
        return Math.pow(sum/values.size(), 1/p);
    }

    /**
     * Method calculating the Minkowski Similarity between two SparseVectors
     * @param a First vector
     * @param b Second vector
     * @param p Order of the similarity, 1 for L1-norm (Manhattan), 2 for L2-norm (Euclidean), etc.
     * @return The Minskowski similarity between a and b for order p
     */
    public static double MinkowskiSimilarity(SparseVector a, SparseVector b, int p){
        // reverse of distance
        return 1 - MinkowskiDistance(a, b, p);
    }
}
