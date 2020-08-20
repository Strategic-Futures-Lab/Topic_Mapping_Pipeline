package P4_Analysis.TopicDistribution;

public class DistribComparison {

    // public String topicWordsId;
    public double initialValue;
    public double previousValue;
    public double currentValue;

    public DistribComparison(double init, double previous, double current){
        // topicWordsId = topicWords;
        initialValue = init;
        previousValue = previous;
        currentValue = current;
    }

    // public DistributionComparison(String topicWords){
    //     topicWordsId = topicWords;
    // }

    public double getInitialDiff(){
        return currentValue - initialValue;
    }

    public double getDiff(){
        return currentValue - previousValue;
    }
}
