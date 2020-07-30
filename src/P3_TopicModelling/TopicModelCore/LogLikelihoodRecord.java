package P3_TopicModelling.TopicModelCore;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class LogLikelihoodRecord {

    public static class LLEntry {
        public int iteration;
        public double logLikelihood;
        public double logLikelihoodPerToken;

        public LLEntry(int i, double ll, double llt){

            DecimalFormat df = new DecimalFormat("#.####");
            df.setRoundingMode(RoundingMode.UP);

            iteration = i;
            logLikelihood = Double.parseDouble(df.format(ll));
            logLikelihoodPerToken = Double.parseDouble(df.format(llt));
        }

        public JSONObject toJSON(){
            JSONObject root = new JSONObject();
            root.put("iter", iteration);
            root.put("LL", logLikelihood);
            root.put("LLPerToken", logLikelihoodPerToken);
            return root;
        }
    }

    public ArrayList<LLEntry> llEntries;
    public int totalTokens;
    public double finalLogLikelihood;
    public double finalLogLikelihoodPerToken;
    public int iterations;

    public LogLikelihoodRecord(List<String> records, int totalTokens, double finalLogLikelihood, int iterations){

        DecimalFormat df = new DecimalFormat("#.####");
        df.setRoundingMode(RoundingMode.UP);

        this.totalTokens = totalTokens;
        this.finalLogLikelihood = Double.parseDouble(df.format(finalLogLikelihood));
        this.finalLogLikelihoodPerToken = Double.parseDouble(df.format(this.finalLogLikelihood/(double)this.totalTokens));
        this.iterations = iterations;
        this.llEntries = new ArrayList<>();

        for(String r: records){
            String[] split = r.split("<");
            split = split[1].split(">");
            int iteration = Integer.parseInt(split[0]);
            split = split[1].split(" LL/token: ");
            double logLikelihoodPerToken = Double.parseDouble(split[1]);
            double logLikelihood = logLikelihoodPerToken * (double)totalTokens;
            llEntries.add(new LLEntry(iteration, logLikelihood, logLikelihoodPerToken));
        }
    }

    public JSONObject toJSON(){
        JSONObject root = new JSONObject();
        root.put("LL", finalLogLikelihood);
        root.put("LLPerToken", finalLogLikelihoodPerToken);
        root.put("nTokens", totalTokens);
        root.put("nIter", iterations);
        JSONArray rec = new JSONArray();
        for(LLEntry r: llEntries){
            rec.add(r.toJSON());
        }
        root.put("entries", rec);
        return root;
    }


}
