package P3_TopicModelling;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ModelJSONTopic {

    private String id;
    private int index;
    private List<WordWeight> words;
    private List<DocWeight> docs;
    private List<String> mainTopicIds;
    private List<String> subTopicIds;

    public ModelJSONTopic(String id, int index, List<WordWeight> words, List<DocWeight> docs){
        this.id = id;
        this.index = index;
        this.words = words;
        this.docs = docs;
        mainTopicIds = new ArrayList<>();
        subTopicIds = new ArrayList<>();
    }

    public void addMainTopicId(String id){
        mainTopicIds.add(id);
    }

    public void addSubTopicId(String id){
        subTopicIds.add(id);
    }

    public JSONObject toJSON(){
        JSONObject root = new JSONObject();
        root.put("topicId", id);
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
            docObj.put("docIc", docs.get(w).docID);
            docObj.put("weight", docs.get(w).weight);
            docArray.add(docObj);
        }
        root.put("topWords", wordArray);
        root.put("topDocs", docArray);
        if(!mainTopicIds.isEmpty()){
            JSONArray mainTopics = new JSONArray();
            for(String id: mainTopicIds){
                mainTopics.add(id);
            }
            root.put("mainTopicIds", mainTopics);
        }
        if(!subTopicIds.isEmpty()){
            JSONArray subTopics = new JSONArray();
            for(String id: subTopicIds){
                subTopics.add(id);
            }
            root.put("subTopicIds", subTopics);
        }
        return root;
    }

    public List<WordWeight> getWords(){
        return words;
    }

    public String getLabelString(int nWords){
        return String.join("-", words.subList(0, nWords).stream().map(w->w.label).collect(Collectors.toList()));
    }

    public String getId(){
        return id;
    }
}
