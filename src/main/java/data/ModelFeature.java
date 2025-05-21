package data;

import org.json.simple.JSONObject;

import java.io.Serial;
import java.io.Serializable;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.stream.DoubleStream;

/**
 * Class representing a model feature within a topic model (instance with weight)
 *
 * @author P. Le Bras
 * @version 1
 */
public class ModelFeature implements Serializable {

    @Serial
    private static final long serialVersionUID = -8278523929210359780L;

    // List of static fields used when reading/writing JSON files
    private static final String JSON_INDEX = "i";
    private static final String JSON_LABEL = "l";
    private static final String JSON_WEIGHTS = "w";

    // Attributes
    private String label;
    private int index;
    private double weight;

    /**
     * Initial constructor
     * @param l label/word
     * @param i id in the model vocabulary, can be used to create sparse vectors
     * @param w weight in the document/topic
     */
    public ModelFeature(String l, int i, double w){
        label = l;
        index = i;
        weight = w;
    }

    /**
     * JSON constructor
     * @param obj
     */
    public ModelFeature(JSONObject obj){
        label = obj.get(JSON_LABEL).toString();
        index = (int) obj.get(JSON_INDEX);
        weight = (double) obj.get(JSON_WEIGHTS);
    }

    /**
     * Getter for label
     * @return the label/word
     */
    public String getLabel() {
        return label;
    }

    /**
     * Getter for id
     * @return the id within the model vocabulary, can be used to create sparse vectors
     */
    public int getIndex() {
        return index;
    }

    /**
     * Getter for weight
     * @return the weight (or count) in the attached document or topic
     */
    public double getWeight() {
        return weight;
    }

    /**
     * Method making JSON representation
     * @return a JSON object containing the word data
     */
    public JSONObject toJSON(){
        JSONObject root = new JSONObject();
        root.put(JSON_INDEX, index);
        root.put(JSON_LABEL, label);
        root.put(JSON_WEIGHTS, formatDouble(weight));
        return root;
    }

    private double formatDouble(double in){
        DecimalFormat df = new DecimalFormat("#.#####");
        df.setRoundingMode(RoundingMode.HALF_UP);
        return Double.parseDouble(df.format(in));
    }
}
