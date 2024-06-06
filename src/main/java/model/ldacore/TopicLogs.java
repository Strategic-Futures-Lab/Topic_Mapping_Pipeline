package model.ldacore;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Class storing the list of logs, from MALLET's topic modelling process, about the model's topics
 *
 * @author P. Le Bras
 * @version 1
 */
public class TopicLogs {

    /**
     * Class representing a single topic log
     */
    public static class TopicEntry {
        // Topic identifier
        private int id;
        // Iteration number when the log was made
        private int iteration;
        // Topic's alpha value logged
        private double alpha;
        // Topic's top words logged
        private String labels;

        /**
         * Constructor
         * @param id Topic id
         * @param i Iteration number
         * @param a Alpha value
         * @param w Top words
         */
        public TopicEntry(int id, int i, double a, String w){
            DecimalFormat df = new DecimalFormat("#.####");
            df.setRoundingMode(RoundingMode.UP);
            this.id = id;
            this.iteration = i;
            this.alpha = Double.parseDouble(df.format(a));
            this.labels = w;
        }

        /**
         * Method formatting the log into JSON to write on file
         * @return The log in JSON format
         */
        public JSONObject toJSON(){
            JSONObject root = new JSONObject();
            root.put("topic", id);
            root.put("iter", iteration);
            root.put("alpha", alpha);
            root.put("labels", labels);
            return root;
        }
    }

    // List of log entries across topics and throughout the modelling process
    private ArrayList<TopicEntry> topicEntries;
    // Number of topics modelled
    private int nTopics;
    // Total number of iterations the model went through
    private int iterations;

    /**
     * Constructor
     * @param logs List of topic log messages from MALLET
     * @param nTopics Number of topics modelled
     * @param iterations Total number of iterations the model went through
     * @param printInterval Interval at which the topics were logged by MALLET
     */
    public TopicLogs(List<String> logs, int nTopics, int iterations, int printInterval){
        this.nTopics = nTopics;
        this.iterations = iterations;
        this.topicEntries = new ArrayList<>();

        for(int i = 0; i < logs.size(); i++){
            int iter = (i+1) * printInterval;
            for(String t: logs.get(i).split("\n")){
                if(!t.equals("")){
                    String[] split = t.split("\t");
                    int tId = Integer.parseInt(split[0]);
                    double alpha = Double.parseDouble(split[1]);
                    String words = split[2];
                    this.topicEntries.add(new TopicEntry(tId, iter, alpha, words));
                }
            }
        }
    }

    /**
     * Method formatting the records in JSON to write on file
     * @return The JSON formatted records
     */
    public JSONObject toJSON(){
        JSONObject root = new JSONObject();
        root.put("nTopics", nTopics);
        root.put("nIter", iterations);
        JSONArray rec = new JSONArray();
        for(TopicEntry r: topicEntries){
            rec.add(r.toJSON());
        }
        root.put("entries", rec);
        return root;
    }
}
