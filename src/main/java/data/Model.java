package data;

import IO.Console;
import IO.JSONHelper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Class representing a model (collection of topics).
 * Provides method for loading a typical model file.
 *
 * @author P. Le Bras
 * @version 1
 */
public class Model {

    // List of static fields used when reading/writing JSON files
    private static final String JSON_NAME = "name";
    private static final String JSON_CORPUS = "corpus";
    private static final String JSON_STATS = "stats";
    private static final String JSON_TOPICS = "topics";

    /** Model name */
    public String name;
    /** Corpus name from which the model is computed */
    public String corpus;
    /** List of topics in the model */
    public List<Topic> topics;
    /** Stats attached to the model */
    public JSONObject stats;

    /** Basic constructor, creates empty model */
    public Model(){
        name = "model"+this.hashCode();
        stats = new JSONObject();
        topics = new ArrayList<>();
    }

    /**
     * Constructor loading model from file
     * @param filename File to load model from
     * @throws IOException If there is an error with loading the file
     * @throws ParseException If there is an error with parsing JSON
     */
    public Model(String filename) throws IOException, ParseException {
        loadModel(filename);
    }

    /**
     * Loads a model from a JSON file
     * @param filename File to load model from
     * @throws IOException If there is an error with loading the file
     * @throws ParseException If there is an error with parsing JSON
     */
    public void loadModel(String filename) throws IOException, ParseException {
        try {
            JSONObject input = JSONHelper.loadJSON(filename);
            name = (String) input.get(JSON_NAME);
            corpus = (String) input.getOrDefault(JSON_CORPUS, null);
            stats = (JSONObject) input.getOrDefault(JSON_STATS, new JSONObject());
            JSONArray model = (JSONArray) input.get(JSON_TOPICS);
            topics = new ArrayList<>(model.size());
            for(JSONObject jsonTopic: (Iterable<JSONObject>) model){
                Topic topic = new Topic(jsonTopic);
                topic.setModelName(name);
                topics.add(topic.getNumber(), topic);
            }
            Console.note("Loaded "+topics.size()+" topics", 1);
        } catch (IOException e) {
            Console.error("Loading model file "+filename+" failed");
            throw e;
        } catch (ParseException e) {
            Console.error("Parsing model file "+filename+" failed");
            throw e;
        }
    }

    /**
     * Writes the model on a JSON file
     * @param filename File to write model on
     * @throws IOException If there is an error with writing the file
     */
    public void writeModel(String filename) throws IOException{
        try {
            JSONObject root = new JSONObject();
            JSONArray model = new JSONArray();
            root.put(JSON_NAME, name);
            root.put(JSON_CORPUS, corpus);
            buildStats();
            root.put(JSON_STATS, stats);
            for(Topic topic : topics) {
                model.add(topic.toJSON());
            }
            root.put(JSON_TOPICS, model);
            JSONHelper.saveJSON(root, filename);
        } catch (IOException e){
            Console.error("Saving model file "+filename+" failed");
            throw e;
        }
    }

    private void buildStats(){
        // TODO: improve stats method
        int nTopics = topics.size();
        stats.put("n",nTopics);
    }
}
