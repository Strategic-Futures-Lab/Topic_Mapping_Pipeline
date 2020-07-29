package P3_TopicModelling.TopicModelCore;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class LogLikelihood {

    public static class LLRecord{
        public int iteration;
        public double logLikelihood;
        public double logLikelihoodPerToken;

        public LLRecord(int i, double ll, double llt){

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

    public ArrayList<LLRecord> llRecords;
    public int totalTokens;
    public double finalLogLikelihood;
    public double finalLogLikelihoodPerToken;
    public int iterations;

    public LogLikelihood(List<String> records, int totalTokens, double finalLogLikelihood, int iterations){

        DecimalFormat df = new DecimalFormat("#.####");
        df.setRoundingMode(RoundingMode.UP);

        this.totalTokens = totalTokens;
        this.finalLogLikelihood = Double.parseDouble(df.format(finalLogLikelihood));
        this.finalLogLikelihoodPerToken = Double.parseDouble(df.format(this.finalLogLikelihood/(double)this.totalTokens));
        this.iterations = iterations;
        this.llRecords = new ArrayList<>();

        for(String r: records){
            String[] split = r.split("<");
            split = split[1].split(">");
            int iteration = Integer.parseInt(split[0]);
            split = split[1].split(" LL/token: ");
            double logLikelihoodPerToken = Double.parseDouble(split[1]);
            double logLikelihood = logLikelihoodPerToken * (double)totalTokens;
            llRecords.add(new LLRecord(iteration, logLikelihood, logLikelihoodPerToken));
        }
    }

    public JSONObject toJSON(){
        JSONObject root = new JSONObject();
        root.put("LL", finalLogLikelihood);
        root.put("LLPerToken", finalLogLikelihoodPerToken);
        root.put("nTokens", totalTokens);
        root.put("nIter", iterations);
        JSONArray rec = new JSONArray();
        for(LLRecord r: llRecords){
            rec.add(r.toJSON());
        }
        root.put("records", rec);
        return root;
    }


}
