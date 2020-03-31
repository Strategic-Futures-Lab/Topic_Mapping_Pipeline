package P0_Project;

import org.json.simple.JSONObject;

public class ModelSpecs {
    public int topics;
    public int words;
    public int docs;
    public int iterations;
    public String topicOutput;
    public String similarityOutput;
    public boolean outputSimilarity = false;

    public ModelSpecs(JSONObject specs, String outputDir){
        topics = Math.toIntExact((long) specs.get("topics"));
        words = Math.toIntExact((long) specs.get("words"));
        docs = Math.toIntExact((long) specs.get("docs"));
        iterations = Math.toIntExact((long) specs.get("iterations"));
        topicOutput = outputDir + (String) specs.get("topicOutput");
        similarityOutput = (String) specs.getOrDefault("similarityOutput", "");
        if(!similarityOutput.equals("")){
            outputSimilarity = true;
            similarityOutput = outputDir+similarityOutput;
        }
    }
}
