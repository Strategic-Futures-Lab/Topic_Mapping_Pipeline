package P4_Analysis.TopicDistribution;

import P0_Project.DistribSpecs;
import PX_Data.DocIOWrapper;
import PX_Data.JSONIOWrapper;
import PX_Data.TopicIOWrapper;
import PY_Helper.LogPrint;
import de.siegmar.fastcsv.reader.CsvParser;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRow;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.File;
import java.io.IOException;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Class implementing a topic distribution.
 *
 * @author P. Le Bras
 * @version 1
 */
public class Distribution {

    /**
     * Class implementing a mutable comparable value.
     */
    private static class MutableValue implements Comparable<MutableValue>{
        /** The value. */
        private double value;

        /**
         * Constructor, sets the value to 0.
         */
        public MutableValue(){this(0.0);}

        /**
         * Constructor, with an initial value.
         * @param v Initial value.
         */
        public MutableValue(double v){value = v;}

        /**
         * Method incrementing the value.
         * @param inc Increment value.
         */
        public void incrementValue(double inc){value += inc;}

        /**
         * Setter method for the value.
         * @param v Value to set.
         */
        public void setValue(double v){value = v;}

        /**
         * Getter method for the value
         * @return The value.
         */
        public double getValue(){return value;}

        /**
         * Comparison method.
         * @param v2 Value to compare to.
         * @return Comparison result (-1, 0 or 1).
         */
        public int compareTo(MutableValue v2){
            return Double.compare(value, v2.getValue());
        }
    }

    /** Name of the document data field used to get the distribution's domain. */
    private final String fieldName;
    /** String used to split fieldName values from the document data into the final distribution's domain. */
    private final String separator;
    /** Set of unique values for the distribution's domain. */
    private final HashSet<String> uniqueDomainValues;
    /** Name of the document data field used to weight the distribution's values. */
    private final String valueName;
    /** Filename of the distribution JSON output. */
    private final String output;
    /** Number of entries in the distribution's domain to save per topic. */
    private final int topPerTopic;

    /** Filename of the CSV file containing data about the distribution domain. */
    private String domainDataFile;
    /** Boolean flag for incorporating data about the distribution domain. */
    private boolean includeDomainData = false;
    /** In the domain data file, which field identifies a domain entry. */
    private String domainDataId;
    /** In the domain data file, which fields to read as data. */
    private HashMap<String, String> domainDataFields;
    /** Distribution domain data, for each domain entry, list of data keys and values. */
    private HashMap<String, HashMap<String, String>> domainData;

    /** Distribution over main topics. */
    private HashMap<String, MutableValue>[] mainDistribution;
    /** Boolean flag for getting the distribution over sub topics. */
    private boolean distributeSubTopics = false;
    /** Distribution over sub topics. */
    private HashMap<String, MutableValue>[] subDistribution;

    /**
     * Constructor, uses a distribution specification instance to set attributes.
     * @param specs Specifications.
     */
    public Distribution(DistribSpecs specs){
        fieldName = specs.fieldName;
        separator = specs.fieldSeparator;
        valueName = specs.valueField;
        output = specs.output;
        topPerTopic = specs.topPerTopic;
        uniqueDomainValues = new HashSet<>();
        includeDomainData = specs.includeDomainData;
        if(includeDomainData && !this.saveInTopics()){
            domainDataFile = specs.domainDataFile;
            domainDataId = specs.domainDataId;
            domainDataFields = specs.domainDataFields;
            // loadDomainData();
        }
    }

    /**
     * Getter method for the distribution's field name.
     * @return The distribution's field name (in the document data).
     */
    public String getFieldName(){
        return fieldName;
    }

    /**
     * Getter method for the distribution's value name.
     * @return The distribution's value name (in the document data).
     */
    public String getValueName(){
        return valueName;
    }

    /**
     * Getter method for the distribution's field name separator.
     * @return The distribution's field name separator.
     */
    public String getSeparator(){ return separator;}

    /**
     * Getter method for the number of distribution entries to keep per topic.
     * @return The number of entries to save per topic.
     */
    public int getTopPerTopic(){return topPerTopic;}

    /**
     * Getter method for the distribution output filename.
     * @return The filename of the distribution output.
     */
    public String getOutput(){return output;}

    /**
     * Method to check if the distribution should be saved with topics or on a different file.
     * @return Boolean flag for saving with topics (true) or separately (false).
     */
    public boolean saveInTopics(){
        return output.length() == 0;
    }

    /**
     * Method returning a string detailing the distribution in plain English.
     * @return Plain English string detailing the distribution's settings.
     */
    public String getDescription(){
        String f = fieldName.length() > 0 ? "Distributing topics across "+fieldName : "Getting topics total";
        String s = separator.length() > 0 ? " separating field by \""+separator+"\"" : "";
        String v = valueName.length() > 0 ? ", using "+valueName+" value" : ", counting documents";
        String t = topPerTopic > 0 ? ", saving "+topPerTopic+" entries"
                : topPerTopic < 0 ? ", saving all entries" : ", only saving totals";
        String o = output.length() > 0 ? ", saving separately" : ", saving with topics";
        return f+s+v+t+o;
    }

    /**
     * Method adding a given document data field value to the list of distribution domain entries.
     * If a separator has been given, the given value is split before adding entries.
     * @param fieldValue Document data field value to add to the distribution domain.
     */
    public void addUniqueFieldValues(String fieldValue){
        if(separator.length() > 0){
            Arrays.stream(fieldValue.split(separator))
                    .map(String::trim)
                    .forEach(uniqueDomainValues::add);
        } else {
            uniqueDomainValues.add(fieldValue);
        }
    }

    /**
     * Method initialising the distribution over main topics, this assumes there will not be distribution over sub topics.
     * @param nTopics Number of main topics.
     */
    public void initialiseDistribution(int nTopics){
        mainDistribution = new HashMap[nTopics];
        for(int i = 0; i < nTopics; i++){
            mainDistribution[i] = new HashMap<>();
            for(String fieldValue: uniqueDomainValues){
                mainDistribution[i].put(fieldValue, new MutableValue());
            }
        }
    }

    /**
     * Method initialising the distribution over main and sub topics.
     * @param mainNTopics Number of main topics.
     * @param subNTopics Number of sub topics.
     */
    public void initialiseDistributions(int mainNTopics, int subNTopics){
        this.initialiseDistribution(mainNTopics);
        distributeSubTopics = true;
        subDistribution = new HashMap[subNTopics];
        for(int i = 0; i < subNTopics; i++){
            subDistribution[i] = new HashMap<>();
            for(String fieldValue: uniqueDomainValues){
                subDistribution[i].put(fieldValue, new MutableValue());
            }
        }
    }

    /**
     * Method updating the distribution(s) given a document entry.
     * @param doc Document entry to update the distribution(s).
     */
    public void updateDistributions(DocIOWrapper doc){
        updateDistribution(doc, doc.getMainTopicDistribution(), mainDistribution);
        if(distributeSubTopics){
            updateDistribution(doc, doc.getSubTopicDistribution(), subDistribution);
        }
    }

    /**
     * Method updating a given distribution with a document entry.
     * @param doc Document entry to update the distribution with.
     * @param topicWeights Document's topic weights.
     * @param distribution Distribution to update.
     */
    private void updateDistribution(DocIOWrapper doc, double[] topicWeights, HashMap<String, MutableValue>[] distribution){
        for(int i = 0; i < topicWeights.length; i++){
            double topicWeight = topicWeights[i];
            double value = valueName.length() > 0 ? Double.parseDouble(doc.getData(valueName)) : 1.0;
            String docFieldValue = fieldName.length() > 0 ? doc.getData(fieldName) : "";
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

    /**
     * Method launching the writing process for distribution over main topics. It assumes that there are no
     * distributions over sub topics.
     * @param mainTopics List of main topics.
     */
    public void saveDistributions(ConcurrentHashMap<Integer, TopicIOWrapper> mainTopics){
        if(this.saveInTopics()){
            saveDistributionInTopics(mainTopics, mainDistribution);
        } else {
            JSONObject root = initialiseJSON();
            saveDistributionInFile(root, "mainTopics", mainTopics, mainDistribution);
            JSONIOWrapper.SaveJSON(root, output, 1);
        }
    }

    /**
     * Method launching the writing process for the distributions over main and sub topics.
     * @param mainTopics List of main topics.
     * @param subTopics List of sub topics.
     */
    public void saveDistributions(ConcurrentHashMap<Integer, TopicIOWrapper> mainTopics, ConcurrentHashMap<Integer, TopicIOWrapper> subTopics){
        if(this.saveInTopics()){
            saveDistributionInTopics(mainTopics, mainDistribution);
            saveDistributionInTopics(subTopics, subDistribution);
        } else {
            JSONObject root = initialiseJSON();
            saveDistributionInFile(root, "mainTopics", mainTopics, mainDistribution);
            saveDistributionInFile(root, "subTopics", subTopics, subDistribution);
            if(includeDomainData){
                saveDistributionDomain(root);
            }
            JSONIOWrapper.SaveJSON(root, output,1);
        }
    }

    /**
     * Method saving a given distribution in a list of topics.
     * @param topics List of topics.
     * @param distribution Distribution to save.
     */
    private void saveDistributionInTopics(ConcurrentHashMap<Integer, TopicIOWrapper> topics, HashMap<String, MutableValue>[] distribution){
        DecimalFormat df = new DecimalFormat("#.####");
        df.setRoundingMode(RoundingMode.UP);
        for(int i = 0; i < distribution.length; i++){
            TopicIOWrapper topic = topics.get(i);
            TopicIOWrapper.JSONTopicDistribution jsonDistribution = new TopicIOWrapper.JSONTopicDistribution(fieldName);
            if(valueName.length() > 0){
                jsonDistribution.setValueName(valueName);
            }
            double topicTotalWeight = 0.0;
            Map<String, MutableValue> sortedTopicDistribution = distribution[i].entrySet().stream()
                    .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1,e2)->e2, LinkedHashMap::new));
            int countAdded = 0;
            for(Map.Entry<String, MutableValue> distribEntry: sortedTopicDistribution.entrySet()){
                double weight = Double.parseDouble(df.format(distribEntry.getValue().getValue()));
                if(fieldName.length() > 0 && (countAdded < topPerTopic || topPerTopic ==-1)){
                    TopicIOWrapper.JSONTopicWeight jsonWeight = new TopicIOWrapper.JSONTopicWeight(distribEntry.getKey(), weight);
                    jsonDistribution.entries.add(jsonWeight);
                    countAdded++;
                }
                topicTotalWeight += weight;
            }
            String totalId=fieldName+"-"+valueName;
            topic.addTotal(new TopicIOWrapper.JSONTopicWeight(totalId, Double.parseDouble(df.format(topicTotalWeight))));
            if(topPerTopic != 0 && fieldName.length() > 0) topic.addDistribution(jsonDistribution);
        }
    }

    /**
     * Method initialising a JSON object to write distribution on.
     * @return The initialised JSON object.
     */
    private JSONObject initialiseJSON(){
        JSONObject root = new JSONObject();
        if(fieldName.length() > 0) {
            root.put("distributionField", fieldName);
        }
        if(valueName.length() > 0){
            root.put("distributionValue", valueName);
        }
        return root;
    }

    /**
     * Method writing a given distribution on a JSON file.
     * @param root JSON object to write on.
     * @param key Name of the distribution, to save the distribution under.
     * @param topics List of topics.
     * @param distribution Distribution to save.
     */
    private void saveDistributionInFile(JSONObject root, String key, ConcurrentHashMap<Integer, TopicIOWrapper> topics, HashMap<String, MutableValue>[] distribution){
        DecimalFormat df = new DecimalFormat("#.####");
        df.setRoundingMode(RoundingMode.UP);
        JSONArray jsonDistributionArray = new JSONArray();
        for(int i = 0; i < distribution.length; i++){
            String topicId = topics.get(i).getId();
            double topicTotalWeight = 0.0;
            JSONArray jsonTopicDistribution = new JSONArray();
            Map<String, MutableValue> sortedTopicDistribution = distribution[i].entrySet().stream()
                    .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1,e2)->e2, LinkedHashMap::new));
            int countAdded = 0;
            for(Map.Entry<String, MutableValue> distribEntry: sortedTopicDistribution.entrySet()){
                double weight = Double.parseDouble(df.format(distribEntry.getValue().getValue()));
                if(fieldName.length() > 0 && (countAdded < topPerTopic || topPerTopic ==-1)){
                    JSONObject jsonDistributionEntry = new JSONObject();
                    jsonDistributionEntry.put("id", distribEntry.getKey());
                    jsonDistributionEntry.put("weight", weight);
                    jsonTopicDistribution.add(jsonDistributionEntry);
                    countAdded++;
                }
                topicTotalWeight += weight;
            }
            topicTotalWeight = Double.parseDouble(df.format(topicTotalWeight));
            JSONObject jsonEntry = new JSONObject();
            jsonEntry.put("topicId", topicId);
            jsonEntry.put("total", topicTotalWeight);
            if(topPerTopic != 0 && fieldName.length() > 0) jsonEntry.put("distribution", jsonTopicDistribution);
            jsonDistributionArray.add(jsonEntry);
        }
        root.put(key, jsonDistributionArray);
    }

    /**
     * Method loading the distribution domain data from file, if applicable.
     * @param depth Log depth level.
     */
    public void loadDomainData(int depth){
        if(includeDomainData && !this.saveInTopics()){
            domainData = new HashMap<>();
            File file = new File(domainDataFile);
            CsvReader csvReader = new CsvReader();
            csvReader.setContainsHeader(true);
            LogPrint.printNewStep("Reading distribution domain data: "+domainDataFile, depth);
            try(CsvParser csvParser = csvReader.parse(file, StandardCharsets.UTF_8)){
                CsvRow row;
                while((row = csvParser.nextRow()) != null){
                    String domainId = row.getField(domainDataId);
                    HashMap<String,String> domainDataEntry = new HashMap<>();
                    for(Map.Entry<String, String> fieldsEntry: domainDataFields.entrySet()){
                        domainDataEntry.put(fieldsEntry.getKey(), row.getField(fieldsEntry.getValue()));
                    }
                    domainData.put(domainId, domainDataEntry);
                }
            } catch (IOException e){
                LogPrint.printNoteError("Error while reading the distribution domain data CSV file.");
                e.printStackTrace();
                System.exit(1);
            } finally {
                LogPrint.printCompleteStep();
            }
        }
    }

    /**
     * Method appending the distribution domain data to the given JSON object.
     * @param root JSON object to append data to.
     */
    private void saveDistributionDomain(JSONObject root){
        JSONObject distribDomainObj = new JSONObject();
        for(Map.Entry<String, HashMap<String, String>> domDataEntry: domainData.entrySet()){
            JSONObject entryData = new JSONObject();
            for(Map.Entry<String, String> data: domDataEntry.getValue().entrySet()){
                entryData.put(data.getKey(), data.getValue());
            }
            distribDomainObj.put(domDataEntry.getKey(), entryData);
        }
        root.put("domainData", distribDomainObj);
    }
}
