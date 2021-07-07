package P4_Analysis.TopicDistribution;

/**
 * Class implementing a comparison between distribution values, used to follow the evolution of distribution with
 * inferred documents.
 *
 * @author P. Le Bras
 * @version 1
 */
public class DistribComparison {

    /** Initial value. */
    public double initialValue;
    /** Previous value. */
    public double previousValue;
    /** Current value. */
    public double currentValue;

    /**
     * Constructor.
     * @param init Initial value.
     * @param previous Previous value.
     * @param current Current value.
     */
    public DistribComparison(double init, double previous, double current){
        initialValue = init;
        previousValue = previous;
        currentValue = current;
    }

    /**
     * Method returning the difference since the initial value.
     * @return Difference between current value and initial value.
     */
    public double getInitialDiff(){
        return currentValue - initialValue;
    }

    /**
     * Method returning the difference with the previous value.
     * @return Difference between current value and previous value.
     */
    public double getDiff(){
        return currentValue - previousValue;
    }
}
