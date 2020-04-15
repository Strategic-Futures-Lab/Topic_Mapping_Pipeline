package PX_Data;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TopicIOWrapper {

    /**
     * Class to represent an instance with weight in a topic, e.g.: words or document.
     */
    public static class JSONTopicWeight {
        public String ID;
        public double weight;

        /**
         * Constructor
         * @param ID id of instance
         * @param weight weight in topic
         */
        public JSONTopicWeight(String ID, double weight){
            this.ID = ID;
            DecimalFormat df = new DecimalFormat("#.####");
            df.setRoundingMode(RoundingMode.UP);
            this.weight = Double.parseDouble(df.format(weight));
        }

        /**
         * Constructor from JSON object
         * @param jsonWeight JSON object of the instance
         */
        public JSONTopicWeight(JSONObject jsonWeight){
            this.ID = (String) jsonWeight.get("id");
            this.weight = (double) jsonWeight.get("weight");
        }

        /**
         * Constructor from JSON object
         * @param jsonWeight JSON object of the instance
         * @param customName custom name of the instance
         */
        public JSONTopicWeight(JSONObject jsonWeight, String customName){
            this.ID = (String) jsonWeight.get(customName);
            this.weight = (double) jsonWeight.get("weight");
        }

        /**
         * Creates a JSON object of the instance weight to save in JSON file
         * @return JSON object of instance weight
         */
        public JSONObject toJSON(){
            JSONObject obj = new JSONObject();
            obj.put("id", ID);
            obj.put("weight", weight);
            return obj;
        }

        /**
         * Creates a JSON object of the instance weight to save in JSON file
         * @param customName custom instance name to save in JSON
         * @return JSON object of instance weight
         */
        public JSONObject toJSON(String customName){
            JSONObject obj = new JSONObject();
            obj.put(customName, ID);
            obj.put("weight", weight);
            return obj;
        }
    }

    /**
     * Class used to represent a distribution inside a topic
     */
    public static class JSONTopicDistribution {
        public String distributionField;
        public List<JSONTopicWeight> values;
        public String distributionValue = "";

        /**
         * Basic Constructor, only sets id
         * @param distributionField id of distribution
         */
        public JSONTopicDistribution(String distributionField){
            this.distributionField = distributionField;
            this.values = new ArrayList<>();
        }

        /**
         * Complete Constructor, with id and values
         * @param distributionField id of distribution
         * @param values values of distribution
         */
        public JSONTopicDistribution(String distributionField, List<JSONTopicWeight> values){
            this.distributionField = distributionField;
            this.values = values;
        }

        /**
         * Constructor from JSON object
         * @param jsonDistribution JSON object
         */
        public JSONTopicDistribution(JSONObject jsonDistribution){
            distributionField = (String) jsonDistribution.get("field");
            values = new ArrayList<>();
            JSONArray jsonValues = (JSONArray) jsonDistribution.get("topWeights");
            for(JSONObject d: (Iterable<JSONObject>) jsonValues){
                values.add(new JSONTopicWeight(d));
            }
            distributionValue = (String) jsonDistribution.getOrDefault("value", "");
        }

        /**
         * Creates a JSON object to print ot file
         * @return JSON object to print
         */
        public JSONObject toJSON(){
            JSONObject obj = new JSONObject();
            obj.put("field", distributionField);
            if(distributionValue.length() > 0) obj.put("value", distributionValue);
            JSONArray array = new JSONArray();
            for(int v = 0; v < values.size(); v++){
                array.add(values.get(v).toJSON());
            }
            obj.put("topWeights", array);
            return obj;
        }
    }

    /**
     * MAIN JSON TOPIC OBJECT
     */

    // set by topic modelling module:
    private String topicId; // id of topic
    private int topicIndex; // index of topic (in similarity matrix and doc distribution arrays)
    private List<JSONTopicWeight> words; // list of top words: labels + weights
    private List<JSONTopicWeight> docs; // list of top docs: ids + weights
    // set by hierarchical topic module:
    // default: empty list, only saved to JSON if filled
    private List<String> mainTopicIds = new ArrayList<>(); // list of main topic ids topic belongs to if topic is sub
    private List<String> subTopicIds = new ArrayList<>(); // list of sub topic ids topic has if topic is main

    // set by distribution module:
    // default: empty list, only saved to JSON if filled
    private List<JSONTopicDistribution> distributions = new ArrayList<>(); // list of distribution to save in topic data
    private List<JSONTopicWeight> distributionTotals = new ArrayList<>(); // list of distribution totals to save in topic data

    // set by clustering module if sub-topic:
    // default: empty string and non-possible group, only saved to JSON if changed
    private String groupTopicId = ""; // id of topic inside a sub-group
    private int groupTopicIndex = -1; // index of topic inside a sub-group

    /**
     * Basic constructor, typically used by Topic Modelling modules
     * @param id topic id
     * @param index topic index
     * @param words list of top words
     * @param docs list of top documents
     */
    public TopicIOWrapper(String id, int index, List<JSONTopicWeight> words, List<JSONTopicWeight> docs){
        // necessary data
        this.topicId = id;
        this.topicIndex = index;
        this.words = words;
        this.docs = docs;
    }

    /**
     * Constructor to load topic from an existing JSON file
     * @param jsonTopic topic as JSON object
     */
    public TopicIOWrapper(JSONObject jsonTopic){
        // necessary data
        this.topicId = (String) jsonTopic.get("topicId");
        this.topicIndex = Math.toIntExact((long)jsonTopic.get("topicIndex"));
        JSONArray words = (JSONArray) jsonTopic.get("topWords");
        this.words = new ArrayList<>();
        for(JSONObject w: (Iterable<JSONObject>) words){
            this.words.add(new JSONTopicWeight(w, "label"));
        }
        JSONArray docs = (JSONArray) jsonTopic.get("topDocs");
        this.docs = new ArrayList<>();
        for(JSONObject d: (Iterable<JSONObject>) docs){
            this.docs.add(new JSONTopicWeight(d, "docId"));
        }
        // non-necessary data only fill if exists in JSON
        JSONArray mainTopicIds = (JSONArray) jsonTopic.getOrDefault("mainTopicIds", new JSONArray());
        for(String id: (Iterable<String>) mainTopicIds){
            this.mainTopicIds.add(id);
        }
        JSONArray subTopicIds = (JSONArray) jsonTopic.getOrDefault("subTopicIds", new JSONArray());
        for(String id: (Iterable<String>) subTopicIds){
            this.subTopicIds.add(id);
        }
        JSONArray distributions = (JSONArray) jsonTopic.getOrDefault("distributions", new JSONArray());
        for(JSONObject distribObj: (Iterable<JSONObject>) distributions){
            this.distributions.add(new JSONTopicDistribution(distribObj));
        }
        JSONArray totals = (JSONArray) jsonTopic.getOrDefault("totals", new JSONArray());
        for(JSONObject totalObj: (Iterable<JSONObject>) totals){
            this.distributionTotals.add(new JSONTopicWeight(totalObj));
        }
        this.groupTopicId = (String) jsonTopic.getOrDefault("subTopicId", "");
        this.groupTopicIndex = Math.toIntExact((long)jsonTopic.getOrDefault("groupTopicIndex", (long) -1));
    }

    /**
     * Copy constructor, used by topic clustering for sub topics
     * @param topic topic to copy
     */
    public TopicIOWrapper(TopicIOWrapper topic){
        this.topicId = topic.topicId;
        this.topicIndex = topic.topicIndex;
        this.docs = topic.docs;
        this.words = topic.words;
        this.mainTopicIds = topic.mainTopicIds;
        this.subTopicIds = topic.subTopicIds;
        this.distributions = topic.distributions;
        this.distributionTotals = topic.distributionTotals;
        this.groupTopicId = topic.groupTopicId;
        this.groupTopicIndex = topic.groupTopicIndex;
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
     * Setter method for the topic index
     * @param index index to set
     */
    public void setGroupTopicIndex(int index){
        groupTopicIndex = index;
    }

    /**
     * Getter method for the index if topic is sub-topic
     * @return sub topic index
     */
    public int getGroupTopicIndex(){
        return groupTopicIndex;
    }

    /**
     * Setter method for the id if topic is sub-topic
     * @param id sup topic id to set
     */
    public void setGroupTopicId(String id){
        groupTopicId = id;
    }

    /**
     * Getter method for the id if topic is sub-topic
     * @return sub topic id
     */
    public String getGroupTopicId(){
        return groupTopicId;
    }

    /**
     * Getter method for the topic words
     * @return topic words
     */
    public List<JSONTopicWeight> getWords(){
        return words;
    }

    /**
     * Return a string of labels to identify the topic
     * @param nWords number of words
     * @return string of labels
     */
    public String getLabelString(int nWords){
        return String.join("-", words.subList(0, nWords).stream().map(w->w.ID).collect(Collectors.toList()));
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
     * Getter method for the sub topic ids
     * @return sub topic ids
     */
    public List<String> getSubTopicIds(){
        return subTopicIds;
    }

    /**
     * Add an entry to the topic list of distributions
     * @param distribution distribution to add
     */
    public void addDistribution(JSONTopicDistribution distribution){
        distributions.add(distribution);
    }

    /**
     * Add an entry to the topic distribution totals
     * @param total total to add
     */
    public void addTotal(JSONTopicWeight total){
        distributionTotals.add(total);
    }

    /**
     * Create a JSON object of the topic to save in a JSON file
     * @return JSON object of the topic
     */
    public JSONObject toJSON(){
        JSONObject root = new JSONObject();
        // Save necessary data
        root.put("topicId", topicId);
        root.put("topicIndex", topicIndex);
        JSONArray wordArray = new JSONArray();
        for(int w = 0; w < words.size(); w++){
            wordArray.add(words.get(w).toJSON("label"));
        }
        root.put("topWords", wordArray);
        JSONArray docArray = new JSONArray();
        for(int d = 0; d < docs.size(); d++){
            docArray.add(docs.get(d).toJSON("docId"));
        }
        root.put("topDocs", docArray);
        // Save non-necessary data
        // Save if non-empty
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
        if(!distributions.isEmpty()) {
            JSONArray distribs = new JSONArray();
            distributions.forEach(d -> distribs.add(d.toJSON()));
            root.put("distributions", distribs);
        }
        if(!distributionTotals.isEmpty()){
            JSONArray totals = new JSONArray();
            distributionTotals.forEach(t -> totals.add(t.toJSON()));
            root.put("totals", totals);
        }
        // Save if non-default
        if(groupTopicId.length() > 0){
            root.put("groupTopicId", groupTopicId);
        }
        if(groupTopicIndex > -1){
            root.put("groupTopicIndex", groupTopicIndex);
        }
        return root;
    }
}
