package P3_TopicModelling.TopicModelCore;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Class storing the list of logs, from MALLET's topic modelling process, pertaining to the model's log-likelihood.
 *
 * @author P. Le Bras
 * @version 1
 */
public class LogLikelihoodRecord {

    /**
     * Class representing a single log-likelihood log.
     */
    public static class LLEntry {
        /** Iteration number when the log was made. */
        public int iteration;
        /** Value of the total model's log-likelihood logged. */
        public double logLikelihood;
        /** Value of the model's log-likelihood logged per tokens. */
        public double logLikelihoodPerToken;

        /**
         * Constructor.
         * @param i Iteration number.
         * @param ll Total log-likelihood.
         * @param llt Log-likelihood per token.
         */
        public LLEntry(int i, double ll, double llt){
            DecimalFormat df = new DecimalFormat("#.####");
            df.setRoundingMode(RoundingMode.UP);
            iteration = i;
            logLikelihood = Double.parseDouble(df.format(ll));
            logLikelihoodPerToken = Double.parseDouble(df.format(llt));
        }

        /**
         * Method formatting the log into JSON to write on file.
         * @return The log in JSON format.
         */
        public JSONObject toJSON(){
            JSONObject root = new JSONObject();
            root.put("iter", iteration);
            root.put("LL", logLikelihood);
            root.put("LLPerToken", logLikelihoodPerToken);
            return root;
        }
    }

    /** List of log entries throughout the model process. */
    public ArrayList<LLEntry> llEntries;
    /** Number of tokens (words) in the model. */
    public int totalTokens;
    /** Final value of the model's total log-likelihood. */
    public double finalLogLikelihood;
    /** Final value of the model's log-likelihood per token. */
    public double finalLogLikelihoodPerToken;
    /** Total number of iterations the model went through. */
    public int iterations;

    /**
     * Constructor.
     * @param records List of log-likelihood log messages from MALLET.
     * @param totalTokens Total number of tokens (words) in the model.
     * @param finalLogLikelihood Final value of the model's log-likelihood.
     * @param iterations Total number of iterations the model went through.
     */
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

    /**
     * Method formatting the records in JSON to write on file.
     * @return The JSON formatted records.
     */
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
