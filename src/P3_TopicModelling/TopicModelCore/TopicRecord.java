package P3_TopicModelling.TopicModelCore;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class TopicRecord {

    public static class TopicEntry {
        public int id;
        public int iteration;
        public double alpha;
        public String words;

        public TopicEntry(int id, int i, double a, String w){
            DecimalFormat df = new DecimalFormat("#.####");
            df.setRoundingMode(RoundingMode.UP);
            this.id = id;
            this.iteration = i;
            this.alpha = Double.parseDouble(df.format(a));
            this.words = w;
        }

        public JSONObject toJSON(){
            JSONObject root = new JSONObject();
            root.put("topic", id);
            root.put("iter", iteration);
            root.put("alpha", alpha);
            root.put("words", words);
            return root;
        }
    }

    public ArrayList<TopicEntry> topicEntries;
    public int nTopics;
    public int iterations;

    public TopicRecord(List<String> records, int nTopics, int iterations, int printInterval){
        this.nTopics = nTopics;
        this.iterations = iterations;

        this.topicEntries = new ArrayList<>();

        for(int i = 0; i < records.size(); i++){
            int iter = (i+1) * printInterval;
            for(String t: records.get(i).split("\n")){
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
