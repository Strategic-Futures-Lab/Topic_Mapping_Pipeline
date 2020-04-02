package PX_Data;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class JSONTopic {

    private String topicId;
    private int topicIndex;
    private List<WordWeight> words;
    private List<DocWeight> docs;
    private List<String> mainTopicIds;
    private List<String> subTopicIds;

    /**
     * Basic constructor, typically used by Topic Modelling modules
     * @param id topic id
     * @param index topic index
     * @param words list of top words
     * @param docs list of top documents
     */
    public JSONTopic(String id, int index, List<WordWeight> words, List<DocWeight> docs){
        this.topicId = id;
        this.topicIndex = index;
        this.words = words;
        this.docs = docs;
        this.mainTopicIds = new ArrayList<>();
        this.subTopicIds = new ArrayList<>();
    }

    /**
     * Constructor to load topic from an existing JSON file
     * @param jsonTopic topic as JSON object
     */
    public JSONTopic(JSONObject jsonTopic){
        this.topicId = (String) jsonTopic.get("topicId");
        this.topicIndex = Math.toIntExact((long)jsonTopic.get("topicIndex"));
        JSONArray words = (JSONArray) jsonTopic.get("topWords");
        this.words = new ArrayList<>();
        for(JSONObject w: (Iterable<JSONObject>) words){
            this.words.add(new WordWeight(w));
        }
        JSONArray docs = (JSONArray) jsonTopic.get("topDocs");
        this.docs = new ArrayList<>();
        for(JSONObject d: (Iterable<JSONObject>) docs){
            this.docs.add(new DocWeight(d));
        }
        JSONArray mainTopicIds = (JSONArray) jsonTopic.getOrDefault("mainTopicIds", new JSONArray());
        this.mainTopicIds = new ArrayList<>();
        for(String id: (Iterable<String>) mainTopicIds){
            this.mainTopicIds.add(id);
        }
        JSONArray subTopicIds = (JSONArray) jsonTopic.getOrDefault("subTopicIds", new JSONArray());
        this.subTopicIds = new ArrayList<>();
        for(String id: (Iterable<String>) subTopicIds){
            this.subTopicIds.add(id);
        }
    }

    /**
     * Getter method for the topic id
     * @return topic id
     */
    public String getId(){
        return topicId;
    }

    /**
     * Getter method for the topic index
     * @return topic index
     */
    public int getIndex(){
        return topicIndex;
    }

    /**
     * Getter method for the topic words
     * @return topic words
     */
    public List<WordWeight> getWords(){
        return words;
    }

    /**
     * Return a string of labels to identify the topic
     * @param nWords number of words
     * @return string of labels
     */
    public String getLabelString(int nWords){
        return String.join("-", words.subList(0, nWords).stream().map(w->w.label).collect(Collectors.toList()));
    }

    /**
     * Add a topic id as main topic
     * @param id main topic id
     */
    public void addMainTopicId(String id){
        mainTopicIds.add(id);
    }

    /**
     * Getter method for the main topic ids
     * @return main topic ids
     */
    public List<String> getMainTopicIds(){
        return mainTopicIds;
    }

    /**
     * Add a topic id as sub topic
     * @param id sub topic id
     */
    public void addSubTopicId(String id){
        subTopicIds.add(id);
    }

    /**
     * Create a JSON object of the topic to save in a JSON file
     * @return JSON object of the topic
     */
    public JSONObject toJSON(){
        JSONObject root = new JSONObject();
        root.put("topicId", topicId);
        root.put("topicIndex", topicIndex);
        JSONArray wordArray = new JSONArray();
        for(int w = 0; w < words.size(); w++){
            wordArray.add(words.get(w).toJSON());
        }
        root.put("topWords", wordArray);
        JSONArray docArray = new JSONArray();
        for(int d = 0; d < docs.size(); d++){
            docArray.add(docs.get(d).toJSON());
        }
        root.put("topDocs", docArray);
        if(!mainTopicIds.isEmpty()){
            JSONArray mainTopics = new JSONArray();
            mainTopicIds.forEach(id->mainTopics.add(id));
            root.put("mainTopicIds", mainTopics);
        }
        if(!subTopicIds.isEmpty()){
            JSONArray subTopics = new JSONArray();
            subTopicIds.forEach(id->subTopics.add(id));
            root.put("subTopicIds", subTopics);
        }
        return root;
    }
}
