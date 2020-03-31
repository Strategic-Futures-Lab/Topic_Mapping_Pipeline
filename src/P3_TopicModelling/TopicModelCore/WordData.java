package P3_TopicModelling.TopicModelCore;

public class WordData implements java.io.Serializable {

    private static final long serialVersionUID = 8325577336916113274L;
    public int id;
    public int topic;
    public String label;
    public double weight;


    public WordData(){
    }

    public WordData(int id, int topic, String label, double weight){
        this.id = id;
        this.topic = topic;
        this.label = label;
        this.weight = weight;
    }

}