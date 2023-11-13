package P3_TopicModelling.TopicModelCore;

/**
 * Class storing, for one document, its topic distributions.
 *
 * @author S. Padilla, T. Methven
 * @version 1
 * @deprecated Replaced by {@link ModelledDocument}
 */
@Deprecated
public class TopicData implements java.io.Serializable{

    private static final long serialVersionUID = 1300965786833187509L;

    public int id;
    public String name;
    public int topics;
    public int[] topicLabels;
    public double[] topicDistributions;

    public TopicData() {
        topicLabels = new int[0];
        topicDistributions = new double[0];
    }

    public TopicData(int topics){
        this.topics = topics;
        topicLabels = new int[topics];
        topicDistributions = new double[topics];
    }

    public TopicData(int id, String name, int topics, int[] topicLabels, double[] topicDistributions){
        this.id = id;
        this.name = name;
        this.topics = topics;
        this.topicLabels = topicLabels;
        this.topicDistributions = topicDistributions;
    }
}