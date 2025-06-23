package analytics;

import data.SparseVector;

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




    // TODO: L1-norm L2-norm distance, Jaccard and Average Jaccard (Greene)
}
