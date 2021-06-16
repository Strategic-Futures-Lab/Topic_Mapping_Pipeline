package PX_Data;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Class representing a topic.
 * Provides functionalities to create a new topic, read it from JSON formatted objects, set and access attributes
 * and transform it into a JSON format to write on file.
 *
 * @author P. Le Bras
 * @version 1.0
 */
public class TopicIOWrapper {

    /**
     * Comparator class to sort JSONTopicWeights
     */
    public class SortByWeight implements Comparator<JSONTopicWeight>{
        public int compare(JSONTopicWeight a, JSONTopicWeight b){
            return Double.compare(b.weight - a.weight, 0.0);
        }
    }

    /**
     * Class to represent an instance with weight in a topic, eg: words or document.
     */
    public static class JSONTopicWeight {

        /** Instance identifier, eg label or docID. */
        public String ID;
        /** Instance weight. */
        public double weight;
        /** Data attached to the instance, eg document fields. */
        public HashMap<String, String> data;
        /** Initial weight of the instance, if it changed (eg after inferring new documents). */
        public double initialWeight;

        /**
         * Constructor.
         * @param ID Id of instance.
         * @param weight Weight in topic.
         */
        public JSONTopicWeight(String ID, double weight){
            this.ID = ID;
            DecimalFormat df = new DecimalFormat("#.####");
            df.setRoundingMode(RoundingMode.UP);
            this.weight = Double.parseDouble(df.format(weight));
            this.data = new HashMap<>();
            this.initialWeight = this.weight;
        }

        /**
         * Constructor from JSON object.
         * @param jsonWeight JSON object of the instance.
         * @param customName Custom name for the instance (id accessor).
         * @param customDataName Custom name for the instance data field (data accessor).
         */
        public JSONTopicWeight(JSONObject jsonWeight, String customName, String customDataName){
            this.ID = (String) jsonWeight.get(customName);
            this.weight = (double) jsonWeight.get("weight");
            this.data = JSONIOWrapper.getStringMap((JSONObject) jsonWeight.getOrDefault(customDataName, new JSONObject()));
            this.initialWeight = (double) jsonWeight.getOrDefault("initial", this.weight);
        }

        /**
         * Constructor from JSON object.
         * customDataName defaults to "data".
         * @param jsonWeight JSON object of the instance.
         * @param customName Custom name for the instance (id accessor).
         */
        public JSONTopicWeight(JSONObject jsonWeight, String customName){
            this(jsonWeight, customName, "data");
        }

        /**
         * Constructor from JSON object.
         * customName defaults to "id" and customDataName defaults to "data".
         * @param jsonWeight JSON object of the instance.
         */
        public JSONTopicWeight(JSONObject jsonWeight){
            this(jsonWeight, "id", "data");
        }

        /**
         * Adds an entry to the instance data.
         * @param key Entry key.
         * @param value Entry value.
         */
        public void addDataEntry(String key, String value){
            this.data.put(key, value);
        }


        /**
         * Creates a JSON object of the instance weight to save in JSON file.
         * customName defaults to "id" and customDataName defaults to "data".
         * @return JSON object of instance weight.
         */
        public JSONObject toJSON(){
            return this.toJSON("id", "data");
        }

        /**
         * Creates a JSON object of the instance weight to save in JSON file.
         * customDataName defaults to "data".
         * @param customName custom instance name to save in JSON
         * @return JSON object of instance weight
         */
        public JSONObject toJSON(String customName){
            return this.toJSON(customName, "data");
        }

        /**
         * Creates a JSON object of the instance weight to save in JSON file.
         * @param customName Custom instance name to save in JSON.
         * @param customDataName Custom instance data name to save in JSON.
         * @return JSON object of instance weight.
         */
        public JSONObject toJSON(String customName, String customDataName){
            JSONObject obj = new JSONObject();
            obj.put(customName, ID);
            obj.put("weight", weight);
            if(!data.isEmpty()){
                JSONObject d = new JSONObject();
                for(Map.Entry<String, String> e: data.entrySet()){
                    d.put(e.getKey(), e.getValue());
                }
                obj.put(customDataName, d);
            }
            if(initialWeight != weight){
                obj.put("initial", initialWeight);
            }
            return obj;
        }
    }

    /**
     * Class to represent a distribution inside a topic.
     */
    public static class JSONTopicDistribution {

        /** Name of the categorical field used to compute the distribution, eg university. */
        public String distributionField;
        /** Distribution entries. */
        public List<JSONTopicWeight> entries;
        /** Name of the numerical value field used to compute the distribution (eg money), defaults to "" (no field). */
        public String distributionValue = "";

        /**
         * Basic Constructor, only sets field.
         * @param distributionField Field of distribution.
         */
        public JSONTopicDistribution(String distributionField){
            this.distributionField = distributionField;
            this.entries = new ArrayList<>();
        }

        // /**
        //  * Complete Constructor, with field and entries.
        //  * @param distributionField Field of distribution.
        //  * @param entries Distribution entries.
        //  */
        // public JSONTopicDistribution(String distributionField, List<JSONTopicWeight> entries){
        //     this.distributionField = distributionField;
        //     this.entries = entries;
        // }

        /**
         * Constructor from JSON object.
         * @param jsonDistribution JSON object.
         */
        public JSONTopicDistribution(JSONObject jsonDistribution){
            distributionField = (String) jsonDistribution.get("field");
            entries = new ArrayList<>();
            JSONArray jsonValues = (JSONArray) jsonDistribution.get("topWeights");
            for(JSONObject d: (Iterable<JSONObject>) jsonValues){
                entries.add(new JSONTopicWeight(d));
            }
            distributionValue = (String) jsonDistribution.getOrDefault("value", "");
        }

        /**
         * Setter for the distributionValue name.
         * @param value distributionValue name to set.
         */
        public void setValueName(String value){
            distributionValue = value;
        }

        /**
         * Formats the distribution in JSON to print on file.
         * @return The distribution in JSON format.
         */
        public JSONObject toJSON(){
            JSONObject obj = new JSONObject();
            obj.put("field", distributionField);
            if(distributionValue.length() > 0) obj.put("value", distributionValue);
            JSONArray array = new JSONArray();
            for (JSONTopicWeight entry : entries) {
                array.add(entry.toJSON());
            }
            obj.put("topWeights", array);
            return obj;
        }
    }

    /* ===========================================================================
       MAIN JSON TOPIC OBJECT
    =========================================================================== */

    // set by topic modelling module:
    /** Topic id, set by the topic modelling module. */
    private String topicId;
    /** Topic index, set by the topic modelling module. */
    private int topicIndex;
    /** Topic top words, set by the topic modelling module. */
    private List<JSONTopicWeight> words;
    /** Topic top documents, set by the topic modelling module. */
    private List<JSONTopicWeight> docs;

    // set by hierarchical topic module:
    /** If sub topic, list of main topics, set by the hierarchical topic modelling module, defaults to empty. */
    private List<JSONTopicWeight> mainTopicIds = new ArrayList<>();
    /** If main topic, list of sub topics, set by the hierarchical topic modelling module, defaults to empty. */
    private List<JSONTopicWeight> subTopicIds = new ArrayList<>();

    // set by distribution module:
    /** List of distributions, set by the distribution module, defaults to empty. */
    private List<JSONTopicDistribution> distributions = new ArrayList<>();
    /** List of distribution totals (sum of distribution entries), set by the distribution module, defaults to empty. */
    private List<JSONTopicWeight> distributionTotals = new ArrayList<>();

    // set by clustering module
    /** Cluster id, set by the clustering module, defaults to "". */
    private String clusterId = "";
    /** If sub topic, topic id in group (one group = one sub map), defaults to "". */
    private String groupTopicId = "";
    /** If sub topic, topic index in group (one group = one sub map), defaults to -1. */
    private int groupTopicIndex = -1;

    /**
     * Basic constructor, typically used by Topic Modelling modules.
     * @param id Topic id.
     * @param index Topic index.
     * @param words List of top words.
     * @param docs List of top documents.
     */
    public TopicIOWrapper(String id, int index, List<JSONTopicWeight> words, List<JSONTopicWeight> docs){
        // necessary data
        this.topicId = id;
        this.topicIndex = index;
        this.words = words;
        this.docs = docs;
    }

    /**
     * Constructor to load topic from an existing JSON file.
     * @param jsonTopic Topic as JSON object.
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
            this.docs.add(new JSONTopicWeight(d, "docId", "docData"));
        }
        // non-necessary data only fill if exists in JSON
        JSONArray mainTopicIds = (JSONArray) jsonTopic.getOrDefault("mainTopicIds", new JSONArray());
        for(JSONObject id: (Iterable<JSONObject>) mainTopicIds){
            this.mainTopicIds.add(new JSONTopicWeight(id));
        }
        JSONArray subTopicIds = (JSONArray) jsonTopic.getOrDefault("subTopicIds", new JSONArray());
        for(JSONObject id: (Iterable<JSONObject>) subTopicIds){
            this.subTopicIds.add(new JSONTopicWeight(id));
        }
        JSONArray distributions = (JSONArray) jsonTopic.getOrDefault("distributions", new JSONArray());
        for(JSONObject distribObj: (Iterable<JSONObject>) distributions){
            this.distributions.add(new JSONTopicDistribution(distribObj));
        }
        JSONArray totals = (JSONArray) jsonTopic.getOrDefault("totals", new JSONArray());
        for(JSONObject totalObj: (Iterable<JSONObject>) totals){
            this.distributionTotals.add(new JSONTopicWeight(totalObj));
        }
        this.clusterId = (String) jsonTopic.getOrDefault("clusterId", "");
        this.groupTopicId = (String) jsonTopic.getOrDefault("subTopicId", "");
        this.groupTopicIndex = Math.toIntExact((long)jsonTopic.getOrDefault("groupTopicIndex", (long) -1));
    }

    /**
     * Copy constructor, used by topic clustering for sub topics.
     * @param topic Topic to copy.
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
        this.clusterId = topic.clusterId;
        this.groupTopicId = topic.groupTopicId;
        this.groupTopicIndex = topic.groupTopicIndex;
    }

    /**
     * Getter method for the topic id.
     * @return The topic id.
     */
    public String getId(){
        return topicId;
    }

    /**
     * Getter method for the topic index.
     * @return The topic index.
     */
    public int getIndex(){
        return topicIndex;
    }

    /**
     * Setter method for the topic index in a group.
     * @param index Group index to set.
     */
    public void setGroupTopicIndex(int index){
        groupTopicIndex = index;
    }

    /**
     * Getter method for the topic index iin a group.
     * @return The group topic index.
     */
    public int getGroupTopicIndex(){
        return groupTopicIndex;
    }

    /**
     * Setter method for the topic id in a group.
     * @param id Group topic id to set.
     */
    public void setGroupTopicId(String id){
        groupTopicId = id;
    }

    /**
     * Getter method for the topic id in a group.
     * @return The group topic id.
     */
    public String getGroupTopicId(){
        return groupTopicId;
    }

    /**
     * Setter method for the cluster id.
     * @param id Cluster id value to set.
     */
    public void setClusterId(String id){
        clusterId = id;
    }

    /**
     * Getter method for the cluster id.
     * @return The cluster id.
     */
    public String getClusterId(){
        return clusterId;
    }

    /**
     * Getter method for the topic's top words.
     * @return The topic's top words list.
     */
    public List<JSONTopicWeight> getWords(){
        return words;
    }

    /**
     * Gets a string concatenating the top n words, can be used to identify a topic.
     * @param nWords Number of top words to concatenate.
     * @return The concatenated string of words.
     */
    public String getLabelString(int nWords){
        return String.join("-", words.subList(0, nWords).stream().map(w->w.ID).collect(Collectors.toList()));
    }

    /**
     * Getter method for the topic's top documents.
     * @return The topic's top documents.
     */
    public List<JSONTopicWeight> getDocs(){
        return docs;
    }

    /**
     * Adds new entries to the list of top documents, mainly used after inference.
     * The list is then reordered and filtered to keep the initial length.
     * @param newDocs The additional list of documents to consider in the top documents.
     */
    public void addToDocs(List<JSONTopicWeight> newDocs){
        int nDocs = docs.size();
        docs.addAll(newDocs);
        docs.sort(new SortByWeight());
        docs = docs.subList(0, nDocs);
    }

    /**
     * Adds a topic id to the list of assigned main topics.
     * @param id Main topic id to add.
     * @param weight Similarity with the main topic.
     */
    public void addMainTopicId(String id, double weight){
        mainTopicIds.add(new JSONTopicWeight(id, weight));
    }

    /**
     * Getter method for the list of main topic ids (ids only, no weight).
     * @return The list of main topic ids.
     */
    public List<String> getMainTopicIds(){
        return mainTopicIds.stream().map(t -> t.ID).collect(Collectors.toList());
    }

    /**
     * Adds a topic id to the list of assigned sub topics.
     * @param id Sub topic id to add.
     * @param weight Similarity with the sub topic.
     */
    public void addSubTopicId(String id, double weight){
        subTopicIds.add(new JSONTopicWeight(id, weight));
    }

    /**
     * Getter method for the sub topic ids (ids only, no weight).
     * @return The list of sub topic ids.
     */
    public List<String> getSubTopicIds(){
        return subTopicIds.stream().map(t -> t.ID).collect(Collectors.toList());
    }

    /**
     * Adds a distribution to the topic's list of distributions.
     * @param distribution Distribution to add.
     */
    public void addDistribution(JSONTopicDistribution distribution){
        distributions.add(distribution);
    }

    /**
     * Adds an entry to the topic's distribution totals.
     * @param total Total to add.
     */
    public void addTotal(JSONTopicWeight total){
        distributionTotals.add(total);
    }

    /**
     * Finds an entry from the topic's distribution totals.
     * @param id ID of the distribution to find.
     * @return The distribution total found.
     * @throws NoSuchElementException if no match found.
     */
    public JSONTopicWeight findTotal(String id) throws NoSuchElementException{
        Optional<JSONTopicWeight> total = distributionTotals.stream()
                .filter(t -> t.ID.equals(id))
                .findAny();
        return total.get();
    }

    /**
     * Formats the topic into a JSON object to write on file.
     * @return The JSON formatted topic.
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
            docArray.add(docs.get(d).toJSON("docId", "docData"));
        }
        root.put("topDocs", docArray);
        // Save non-necessary data
        // Save if non-empty
        if(!mainTopicIds.isEmpty()){
            JSONArray mainTopics = new JSONArray();
            mainTopicIds.forEach(id->mainTopics.add(id.toJSON()));
            root.put("mainTopicIds", mainTopics);
        }
        if(!subTopicIds.isEmpty()){
            JSONArray subTopics = new JSONArray();
            subTopicIds.forEach(id->subTopics.add(id.toJSON()));
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
        if(clusterId.length() > 0){
            root.put("clusterId", clusterId);
        }
        if(groupTopicId.length() > 0){
            root.put("groupTopicId", groupTopicId);
        }
        if(groupTopicIndex > -1){
            root.put("groupTopicIndex", groupTopicIndex);
        }
        return root;
    }
}
