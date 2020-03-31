package P3_TopicModelling;

import java.math.RoundingMode;
import java.text.DecimalFormat;

public class WordWeight {

    public String label;
    public double weight;


    public WordWeight(String label, double weight){
        this.label = label;
        DecimalFormat df = new DecimalFormat("#.#");
        df.setRoundingMode(RoundingMode.UP);
        this.weight = Double.parseDouble(df.format(weight));
    }

}
