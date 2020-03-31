package P3_TopicModelling;

import java.math.RoundingMode;
import java.text.DecimalFormat;

public class DocWeight {

    public String docID;
    public double weight;


    public DocWeight(String docID, double weight){
        this.docID = docID;
        DecimalFormat df = new DecimalFormat("#.####");
        df.setRoundingMode(RoundingMode.UP);
        this.weight = Double.parseDouble(df.format(weight));
    }

}
