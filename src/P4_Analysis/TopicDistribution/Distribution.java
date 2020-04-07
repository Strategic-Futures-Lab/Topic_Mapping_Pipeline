package P4_Analysis.TopicDistribution;

import P0_Project.DistribSpecs;
import PX_Data.JSONDocument;
import PX_Data.JSONIOWrapper;
import PX_Data.JSONTopic;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Distribution {

    private static class MutableValue implements Comparable<MutableValue>{
        private double value;
        public MutableValue(){this(0.0);}
        public MutableValue(double v){value = v;}
        public void incrementValue(double inc){value += inc;}
        public void setValue(double v){value = v;}
        public double getValue(){return value;}
        public int compareTo(MutableValue v2){
            if(v2.getValue() > value) return -1;
            if(v2.getValue() < value) return 1;
            return 0;
        }
    }

    private String fieldName;
    private String separator;
    private HashSet<String> uniqueFieldValues;
    private String valueName;
    private String output;
    private int topPerTopic;

    private HashMap<String, MutableValue>[] mainDistribution;
    private boolean distributeSubTopics = false;
    private HashMap<String, MutableValue>[] subDistribution;

    public Distribution(DistribSpecs specs){
        fieldName = specs.fieldName;
        separator = specs.fieldSeparator;
        valueName = specs.valueField;
        output = specs.output;
        topPerTopic = specs.topPerTopic;
        uniqueFieldValues = new HashSet<>();
    }

    public String getFieldName(){
        return fieldName;
    }

    public String getValueName(){
        return valueName;
    }

    public boolean saveInTopics(){
        return output.length() == 0;
    }

    public void addUniqueFieldValues(String fieldValue){
        if(separator.length() > 0){
            Arrays.stream(fieldValue.split(separator))
                    .map(String::trim)
                    .forEach(v -> uniqueFieldValues.add(v));
        } else {
            uniqueFieldValues.add(fieldValue);
        }
    }

    public void initialiseDistribution(int nTopics){
        mainDistribution = new HashMap[nTopics];
        for(int i = 0; i < nTopics; i++){
            mainDistribution[i] = new HashMap<>();
            for(String fieldValue: uniqueFieldValues){
                mainDistribution[i].put(fieldValue, new MutableValue());
            }
        }
    }

    public void initialiseDistributions(int mainNTopics, int subNTopics){
        this.initialiseDistribution(mainNTopics);
        distributeSubTopics = true;
        subDistribution = new HashMap[subNTopics];
        for(int i = 0; i < subNTopics; i++){
            subDistribution[i] = new HashMap<>();
            for(String fieldValue: uniqueFieldValues){
                subDistribution[i].put(fieldValue, new MutableValue());
            }
        }
    }

    public void updateDistributions(JSONDocument doc){
        updateDistribution(doc, mainDistribution);
        if(distributeSubTopics){
            updateDistribution(doc, subDistribution);
        }
    }

    private void updateDistribution(JSONDocument doc, HashMap<String, MutableValue>[] distribution){
        double[] topicWeights = doc.getTopicDistribution();
        for(int i = 0; i < topicWeights.length; i++){
            double topicWeight = topicWeights[i];
            double value = valueName.equals("") ? 1.0 : Double.parseDouble(doc.getData(valueName));
            String docFieldValue = doc.getData(fieldName);
            if(separator.length() > 0){
                int finalI = i;
                Arrays.stream(docFieldValue.split(separator))
                        .map(String::trim)
                        .forEach(v -> distribution[finalI].get(v).incrementValue(topicWeight*value));
            } else {
                distribution[i].get(docFieldValue).incrementValue(topicWeight*value);
            }

        }
    }

    public void saveDistributions(ConcurrentHashMap<Integer, JSONTopic> mainTopics){
        if(this.saveInTopics()){
            saveDistributionInTopics(mainTopics, mainDistribution);
        } else {
            JSONObject root = initialiseJSON();
            saveDistributionInFile(root, "topics", mainTopics, mainDistribution);
            JSONIOWrapper.SaveJSON(root, output);
        }
    }

    public void saveDistributions(ConcurrentHashMap<Integer, JSONTopic> mainTopics, ConcurrentHashMap<Integer, JSONTopic> subTopics){
        if(this.saveInTopics()){
            saveDistributionInTopics(mainTopics, mainDistribution);
            saveDistributionInTopics(subTopics, subDistribution);
        } else {
            JSONObject root = initialiseJSON();
            saveDistributionInFile(root, "mainTopics", mainTopics, mainDistribution);
            saveDistributionInFile(root, "subTopics", subTopics, subDistribution);
            JSONIOWrapper.SaveJSON(root, output);
        }
    }

    private void saveDistributionInTopics(ConcurrentHashMap<Integer, JSONTopic> topics, HashMap<String, MutableValue>[] distribution){
        DecimalFormat df = new DecimalFormat("#.####");
        df.setRoundingMode(RoundingMode.UP);
        for(int i = 0; i < distribution.length; i++){
            JSONTopic topic = topics.get(i);
            JSONTopic.JSONTopicDistribution jsonDistribution = new JSONTopic.JSONTopicDistribution(fieldName);
            if(valueName.length() > 0){
                jsonDistribution.distributionValue = valueName;
            }
            double topicTotalWeigth = 0.0;
            Map<String, MutableValue> sortedTopicDistribution = distribution[i].entrySet().stream()
                    .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1,e2)->e2, LinkedHashMap::new));
            int countAdded = 0;
            for(Map.Entry<String, MutableValue> distribEntry: sortedTopicDistribution.entrySet()){
                double weight = Double.parseDouble(df.format(distribEntry.getValue().getValue()));
                if(countAdded < topPerTopic || topPerTopic ==-1){
                    JSONTopic.JSONTopicWeight jsonWeight = new JSONTopic.JSONTopicWeight(distribEntry.getKey(), weight);
                    jsonDistribution.addWeight(jsonWeight);
                    countAdded++;
                }
                topicTotalWeigth += weight;
            }
            jsonDistribution.total = Double.parseDouble(df.format(topicTotalWeigth));
            topic.addDistribution(jsonDistribution);
        }
    }

    private JSONObject initialiseJSON(){
        JSONObject root = new JSONObject();
        root.put("distributionField", fieldName);
        if(valueName.length() > 0){
            root.put("distributionValue", valueName);
        }
        return root;
    }

    private void saveDistributionInFile(JSONObject root, String key, ConcurrentHashMap<Integer, JSONTopic> topics, HashMap<String, MutableValue>[] distribution){
        DecimalFormat df = new DecimalFormat("#.####");
        df.setRoundingMode(RoundingMode.UP);
        JSONArray jsonDistributionArray = new JSONArray();
        for(int i = 0; i < distribution.length; i++){
            String topicId = topics.get(i).getId();
            double topicTotalWeigth = 0.0;
            JSONArray jsonTopicDistribution = new JSONArray();
            Map<String, MutableValue> sortedTopicDistribution = distribution[i].entrySet().stream()
                    .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1,e2)->e2, LinkedHashMap::new));
            int countAdded = 0;
            for(Map.Entry<String, MutableValue> distribEntry: sortedTopicDistribution.entrySet()){
                double weight = Double.parseDouble(df.format(distribEntry.getValue().getValue()));
                if(countAdded < topPerTopic || topPerTopic ==-1){
                    JSONObject jsonDistributionEntry = new JSONObject();
                    jsonDistributionEntry.put("id", distribEntry.getKey());
                    jsonDistributionEntry.put("weight", weight);
                    jsonTopicDistribution.add(jsonDistributionEntry);
                    countAdded++;
                }
                topicTotalWeigth += weight;
            }
            topicTotalWeigth = Double.parseDouble(df.format(topicTotalWeigth));
            JSONObject jsonEntry = new JSONObject();
            jsonEntry.put("topicId", topicId);
            jsonEntry.put("total", topicTotalWeigth);
            jsonEntry.put("distribution", jsonTopicDistribution);
            jsonDistributionArray.add(jsonEntry);
        }
        root.put(key, jsonDistributionArray);
    }
}
