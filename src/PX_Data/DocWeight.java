package PX_Data;

import org.json.simple.JSONObject;

import java.math.RoundingMode;
import java.text.DecimalFormat;

public class DocWeight {

    public String docID;
    public double weight;

    /**
     * Constructor
     * @param docID document id
     * @param weight document weight in topic
     */
    public DocWeight(String docID, double weight){
        this.docID = docID;
        DecimalFormat df = new DecimalFormat("#.####");
        df.setRoundingMode(RoundingMode.UP);
        this.weight = Double.parseDouble(df.format(weight));
    }

    /**
     * Constructor from JSON object
     * @param jsonDoc JSON object of the document
     */
    public DocWeight(JSONObject jsonDoc){
        this.docID = (String) jsonDoc.get("docId");
        this.weight = (double) jsonDoc.get("weight");
    }

    /**
     * Creates a JSON object of the document weight to save in JSON file
     * @return JSON object of document weight
     */
    public JSONObject toJSON(){
        JSONObject docObj = new JSONObject();
        docObj.put("docId", docID);
        docObj.put("weight", weight);
        return docObj;
    }

}
