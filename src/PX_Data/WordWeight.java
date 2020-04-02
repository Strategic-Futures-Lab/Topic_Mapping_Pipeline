package PX_Data;

import org.json.simple.JSONObject;

import java.math.RoundingMode;
import java.text.DecimalFormat;

public class WordWeight {

    public String label;
    public double weight;

    /**
     * Constructor
     * @param label Word label
     * @param weight Word weight
     */
    public WordWeight(String label, double weight){
        this.label = label;
        DecimalFormat df = new DecimalFormat("#.#");
        df.setRoundingMode(RoundingMode.UP);
        this.weight = Double.parseDouble(df.format(weight));
    }

    /**
     * Constructor from JSON object
     * @param jsonWord JSON object of the word
     */
    public WordWeight(JSONObject jsonWord){
        this.label = (String) jsonWord.get("label");
        this.weight = (double) jsonWord.get("weight");

    }

    /**
     * Creates a JSON object of the word weight to save in JSON file
     * @return JSON object of word weight
     */
    public JSONObject toJSON(){
        JSONObject wordObj = new JSONObject();
        wordObj.put("label", label);
        wordObj.put("weight", weight);
        return wordObj;
    }

}
