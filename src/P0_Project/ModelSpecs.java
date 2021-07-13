package P0_Project;

import PY_Helper.LogPrint;
import org.json.simple.JSONObject;

/**
 * Class reading and validating parameters for one topic model.
 *
 * @author P. Le Bras, A. Vidal.
 * @version 2
 */
public class ModelSpecs {

    /** Number of topics to generate. */
    public int topics;
    /** Maximum number of words to save in topic data, optional, defaults to 20. */
    public int words = 20;
    /** Maximum number of document to save in topic data, optional, defaults to 20. */
    public int docs = 20;
    /** Number of iteration for Sampling, optional, defaults to 1000. */
    public int iterations = 1000;
    /** Number of iteration for Maximisation, optional, defaults to 0. */
    public int iterationsMax = 0;
    /** Name of topic model when serialising (used if documents inferred from this model later),
     * optional, defaults to "" (no serialisation). */
    public String serialiseFile = "";
    /** Flag for serialising model, defaults to false if serialiseName = "". */
    public boolean serialise = false;
    /** Filename for the JSON topic file generated, not including directory. */
    public String topicOutput;

    // ADVANCED PARAMETERS, I.E. NON-ESSENTIAL FOR OTHER MODULES
    /** Filename for the CSV topic-similarity file, not including directory, optional, defaults to "". */
    public String similarityOutput = "";
    /** Flag for writing similarity between topics, defaults to false if similarityOuput = "" */
    public boolean outputSimilarity = false;
    /** Number of words to identify a topic in similarity or assignment outputs, optional, defaults to 3. */
    public int numWordId = 3;
    /** Filename for the JSON Log-Likelihood file, not including directory, optional, defaults to "". */
    public String llOutput = "";
    /** Flag for writing Log-Likelihood records, defaults to false if llOutput = "". */
    public boolean outputLL = false;
    /** Filename for the JSON topic log file, not including directory, optional, defaults to "". */
    public String topicLogOutput = "";
    /** Flag for writing the topic log file, defaults to false if topicLogOutput = "". */
    public boolean outputTopicLog = false;
    /** Flag for calculating the word distribution differences between documents and topics,
     * optional, defaults to false. */
    public boolean getWordDistances = false;

    /** Index of random seed to use, optional, defaults to 0, must be set between 0-99. */
    public int seedIndex = 0;
    /** Sum of alpha (doc to topic distrib dirichlet prior), optional, defaults to 1.0. */
    public double alphaSum = 1.0;
    /** Beta value (topic to word distrib dirichlet prior), optional, defaults to 0.01. */
    public double beta = 0.01;
    /** Flag for running a symmetrical optimization of alpha, optional, defaults to false. */
    public boolean symmetricAlpha = false;
    /** Number of iterations between parameter optimisation. */
    public int optimInterval = 50;

    /**
     * Constructor: reads the specification from a JSON object passed from TopicModelModuleSpecs.
     * @param specs JSON object where the model specifications are written.
     * @param dataDir Output directory name to attach to filenames.
     */
    public ModelSpecs(JSONObject specs, String dataDir){
        topics = Math.toIntExact((long) specs.get("topics"));
        words = Math.toIntExact((long) specs.getOrDefault("words", (long) 20));
        docs = Math.toIntExact((long) specs.getOrDefault("docs", (long) 20));
        iterations = Math.toIntExact((long) specs.getOrDefault("iterations", (long) 1000));
        iterationsMax = Math.toIntExact((long) specs.getOrDefault("iterationsMax", (long) 0));
        serialiseFile = (String) specs.getOrDefault("serialise", "");
        if(!serialiseFile.equals("")){
            serialise = true;
            serialiseFile = dataDir + serialiseFile;
        }
        topicOutput = dataDir + specs.get("topicOutput");
        similarityOutput = (String) specs.getOrDefault("topicSimOutput", "");
        if(!similarityOutput.equals("")){
            outputSimilarity = true;
            similarityOutput = dataDir + similarityOutput;
            numWordId = Math.toIntExact((long) specs.getOrDefault("numWordId", (long) 3));
            // validation
            numWordId = validate("numWordId", numWordId, 1);
        }
        llOutput = (String) specs.getOrDefault("llOutput", "");
        if(!llOutput.equals("")){
            outputLL = true;
            llOutput = dataDir + llOutput;
        }
        getWordDistances = (boolean) specs.getOrDefault("wordDistances", false);
        seedIndex = Math.toIntExact((long) specs.getOrDefault("seed", (long) 0));
        optimInterval = Math.toIntExact((long) specs.getOrDefault("optimInterval", (long) 50));
        alphaSum = (double) specs.getOrDefault("alphaSum", 1.0);
        beta = (double) specs.getOrDefault("beta", 0.01);
        symmetricAlpha = (boolean) specs.getOrDefault("symmetricAlpha", false);
        topicLogOutput = (String) specs.getOrDefault("topicLogOutput", "");
        if(!topicLogOutput.equals("")){
            outputTopicLog = true;
            topicLogOutput = dataDir + topicLogOutput;
        }

        // validations
        validateError("topics", topics, 1);
        words = validate("words", words, 3);
        docs = validate("docs", docs, 3);
        iterations = validate("iterations", iterations, 50);
        iterationsMax = validate("iterationsMax", iterationsMax, 0);
        seedIndex = validate("seed", seedIndex, 0, 99);
        optimInterval = validate("optimInterval", optimInterval, 0);
        alphaSum = validatePositive("alphaSum", alphaSum, 1.0);
        beta = validatePositive("beta", beta, 0.01);
    }

    /**
     * Method validating that an integer parameter is greater than a given minimum.
     * @param name Name of parameter to check.
     * @param value Value of parameter to check.
     * @param minimum Minimum value to check against.
     * @return Value corrected if necessary.
     */
    private int validate(String name, int value, int minimum){
        if(value < minimum){
            LogPrint.printNote("Topic Model module: "+name+" must be greater than "+(minimum-1)+", parameter was set to "+value+", will be set to "+minimum);
            return minimum;
        }
        return value;
    }

    /**
     * Method validating that an integer parameter is greater than a given minimum and less than a given maximum.
     * @param name Name of parameter to check.
     * @param value Value of parameter to check.
     * @param minimum Minimum value to check against.
     * @param maximum Maximum value to check against.
     * @return Value corrected if necessary.
     */
    private int validate(String name, int value, int minimum, int maximum){
        if(value < minimum || value > maximum){
            LogPrint.printNote("Topic Model module: "+name+" must be greater than "+(minimum-1)+" and less than "+(maximum+1)+", parameter was set to "+value+", will be set to "+minimum);
            return minimum;
        }
        return value;
    }

    /**
     * Method validating that an double parameter is greater than 0.
     * @param name Name of parameter to check.
     * @param value Value of parameter to check.
     * @param defValue Default value to correct the parameter to.
     * @return Value corrected if necessary.
     */
    private double validatePositive(String name, double value, double defValue){
        if(value <= 0){
            LogPrint.printNote("Topic Model module: "+name+" must be greater than 0, parameter was set to "+value+", will be set to "+defValue);
            return defValue;
        }
        return value;
    }

    /**
     * Method validating that an integer parameter is greater than a given minimum, prints an error and halt process otherwise.
     * @param name Name of parameter to check.
     * @param value Value of parameter to check.
     * @param minimum Minimum value to check against.
     */
    private void validateError(String name, int value, int minimum){
        if(value < minimum){
            LogPrint.printNoteError("Topic Model module: "+name+" must be greater than "+(minimum-1)+", parameter was set to "+value);
            System.exit(1);
        }
    }
}
