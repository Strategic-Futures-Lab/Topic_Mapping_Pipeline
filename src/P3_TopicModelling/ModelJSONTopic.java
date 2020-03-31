package P3_TopicModelling;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.List;

public class ModelJSONTopic {

    private String id;
    private int index;
    private List<WordWeight> words;
    private List<DocWeight> docs;

    public ModelJSONTopic(String id, int index, List<WordWeight> words, List<DocWeight> docs){
        this.id = id;
        this.index = index;
        this.words = words;
        this.docs = docs;
    }

    public JSONObject toJSON(){
        JSONObject root = new JSONObject();
        root.put("id", id);
        root.put("index", index);
        JSONArray wordArray = new JSONArray();
        for(int w = 0; w < words.size(); w++){
            JSONObject wordObj = new JSONObject();
            wordObj.put("label", words.get(w).label);
            wordObj.put("weight", words.get(w).weight);
            wordArray.add(wordObj);
        }
        JSONArray docArray = new JSONArray();
        for(int w = 0; w < docs.size(); w++){
            JSONObject docObj = new JSONObject();
            docObj.put("id", docs.get(w).docID);
            docObj.put("weight", docs.get(w).weight);
            docArray.add(docObj);
        }
        root.put("topWords", wordArray);
        root.put("topDocs", docArray);
        return root;
    }
}
