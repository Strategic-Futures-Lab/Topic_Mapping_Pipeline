package PY_TopicModelCore;

import java.util.ArrayList;

/**
 * Created by Tom on 25/05/2017.
 */
public class TopicWordAndWeights
{
    ArrayList<String> labels;
    ArrayList<Double> weights;

    public TopicWordAndWeights()
    {
        labels = new ArrayList<>();
        weights = new ArrayList<>();
    }

    public void addNewWord(String label, double weight)
    {
        for(int i = 0; i < weights.size(); i++)
        {
            if(weight > weights.get(i))
            {
                labels.add(i, label);
                weights.add(i, weight);
                return;
            }
        }

        //If our weight is smaller than all the others, add it at the end!
        labels.add(label);
        weights.add(weight);
    }

    public String getLabelString()
    {
        String string = "";

        for(int i = 0; i < labels.size(); i++)
            string += string + " ";

        return string.trim();
    }

    public String getLabelAt(int index) { return labels.get(index); }
    public Double getWeightAt(int index) { return weights.get(index); }
    public ArrayList<String> getAllLabels() { return labels; }
    public ArrayList<Double> getAllWeights() { return weights; }
}
