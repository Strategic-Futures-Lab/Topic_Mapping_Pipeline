package P0_Project;

import org.json.simple.JSONObject;

/**
 * Class for a Topic Model specification
 */
public class ModelSpecs {

    /** Number of topics to generate */
    public int topics;
    /** Maximum number of words to save in topic data, optional, defaults to 20 */
    public int words = 20;
    /** Maximum number of document to save in topic data, optional, defaults to 20 */
    public int docs = 20;
    /** Number of iteration for Gibbs Sampling, optional, defaults to 1000 */
    public int iterations = 1000;
    /** Name of topic model when serialising (used if documents inferred from this model later), optional, defaults to "" (no serialisation) */
    public String serialiseFile = "";
    /** Flag for serialising model, defaults to false if serialiseName = "" */
    public boolean serialise = false;
    /** Filename for the JSON topic file generated, not including directory */
    public String topicOutput;
    /** Filename for the CSV similarity file, not including directory:
     * similarity between topics, optional, defaults to "" */
    public String similarityOutput = "";
    /** Flag for writing similarity between topics, defaults to false if similarityOuput = "" */
    public boolean outputSimilarity = false;
    /** Filename for the JSON Log-Likelihood file, not including directory, optional, defaults to "" */
    public String llOutput = "";
    /** Flag for writing Log-Likelihood records, defaults to false if llOutput = "" */
    public boolean outputLL = false;
    /** Number of words to identify a topic in similarity or assignment outputs,
     * optional, defaults to 3 */
    public int numWordId = 3;

    /**
     * Constructor: reads the specification from a JSON object passed from TopicModelModuleSpecs
     * @param specs JSON object where specifications are written
     * @param dataDir Output directory name to attach to filenames
     */
    public ModelSpecs(JSONObject specs, String dataDir){
        topics = Math.toIntExact((long) specs.get("topics"));
        words = Math.toIntExact((long) specs.getOrDefault("words", (long) 20));
        docs = Math.toIntExact((long) specs.getOrDefault("docs", (long) 20));
        iterations = Math.toIntExact((long) specs.getOrDefault("iterations", (long) 1000));
        serialiseFile = (String) specs.getOrDefault("serialiseName", "");
        if(!serialiseFile.equals("")){
            serialise = true;
            serialiseFile = dataDir + serialiseFile;
        }
        topicOutput = dataDir + (String) specs.get("topicOutput");
        similarityOutput = (String) specs.getOrDefault("topicSimOutput", "");
        if(!similarityOutput.equals("")){
            outputSimilarity = true;
            similarityOutput = dataDir + similarityOutput;
        }
        llOutput = (String) specs.getOrDefault("llOutput", "");
        if(!llOutput.equals("")){
            outputLL = true;
            llOutput = dataDir + llOutput;
        }
        numWordId = Math.toIntExact((long) specs.getOrDefault("numWordId", (long) 3));
    }
}
