package P3_TopicModelling;

import PX_Helper.JSONIOWrapper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

public class ModelJSONDocument {
    private String id;
    private int index;
    private HashMap<String, String> docData;
    private String lemmas;
    private boolean removed;
    private String removeReason;
    private double[] topicDistribution;
    private double[] subTopicDistribution = null;

    public ModelJSONDocument(JSONObject jsonDoc){
        id = (String) jsonDoc.get("docId");
        index = Math.toIntExact((long) jsonDoc.get("index"));
        docData = JSONIOWrapper.getStringMap((JSONObject) jsonDoc.get("docData"));
        lemmas = (String) jsonDoc.get("lemmas");
        removed = (boolean) jsonDoc.getOrDefault("removed", false);
        removeReason = (String) jsonDoc.getOrDefault("removeReason", "");
    }

    public ModelJSONDocument(ModelJSONDocument doc){
        id = doc.id;
        index = doc.index;
        docData = doc.docData;
        lemmas = doc.lemmas;
        removed = doc.removed;
        removeReason = doc.removeReason;
    }

    public String getId(){
        return id;
    }

    public int getIndex(){ return index;}

    public HashMap<String, String> getDocData(){
        return docData;
    }

    public String getDocDataValue(String key){
        return docData.get(key);
    }

    public String getLemmas(){
        return lemmas;
    }

    public void remove(String reason){
        removed = true;
        removeReason = reason;
    }

    public boolean isRemoved(){
        return removed;
    }

    public void setTopicDistribution(double[] distribution) {
        DecimalFormat df = new DecimalFormat("#.####");
        df.setRoundingMode(RoundingMode.UP);
        topicDistribution = new double[distribution.length];
        for(int i = 0; i < distribution.length; i++){
            topicDistribution[i] = Double.parseDouble(df.format(distribution[i]));
        }
    }

    public double[] getTopicDistribution(){
        return topicDistribution;
    }

    public void setSubTopicDistribution(double[] distribution){
        // sub topic model should have already format values
        subTopicDistribution = distribution;
    }

    public JSONObject toJSON(){
        JSONObject root = new JSONObject();
        root.put("docId", id);
        JSONObject docDataObj = new JSONObject();
        for(Map.Entry<String, String> entry: docData.entrySet()){
            docDataObj.put(entry.getKey(), entry.getValue());
        }
        root.put("docData", docDataObj);
        if(removed){
            root.put("removed", true);
            root.put("removeReason", removeReason);
        } else {
            JSONArray topicDistrib = new JSONArray();
            for(double value: topicDistribution){
                topicDistrib.add(value);
            }
            if(subTopicDistribution == null){
                root.put("topicDistribution", topicDistrib);
            } else {
                root.put("mainTopicDistribution", topicDistrib);
                topicDistrib = new JSONArray();
                for(double value: subTopicDistribution){
                    topicDistrib.add(value);
                }
                root.put("subTopicDistribution", topicDistrib);
            }
        }
        return root;
    }
}
