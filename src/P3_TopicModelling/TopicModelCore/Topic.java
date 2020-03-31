package P3_TopicModelling.TopicModelCore;

public class Topic implements java.io.Serializable{

    private static final long serialVersionUID = -1048734038308190794L;
    public int id;
    public int labels = 0;
    public String[] topicLabels;
    public int [] topicLabelsIDs;
    public double[] topicWeights;

    public Topic(){
        topicLabels = new String[0];
        topicWeights = new double[0];
        topicLabelsIDs = new int[0];
    }

    public Topic(int labels){
        this.labels = labels;
        topicLabels = new String[100];      // Store only 100 topic labels
        topicWeights = new double[100];
        topicLabelsIDs = new int[100];
    }

    public Topic(int id, int labels, String[] topicLabels, int [] topicLabelsIDs, double[] topicWeights){
        this.id = id;
        this.labels = labels;
        this.topicLabels = topicLabels;
        this.topicLabelsIDs = topicLabelsIDs;
        this.topicWeights = topicWeights;
    }


}