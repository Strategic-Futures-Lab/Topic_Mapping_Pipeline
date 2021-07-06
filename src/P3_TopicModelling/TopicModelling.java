package P3_TopicModelling;

import P0_Project.ModelSpecs;
import P0_Project.TopicModelModuleSpecs;
import P3_TopicModelling.Similarity.TopicsSimilarity;
import P3_TopicModelling.TopicModelCore.*;
import PX_Data.*;
import PY_Helper.LogPrint;
import PY_Helper.Pair;
import PY_Helper.SparseVector;
import de.siegmar.fastcsv.writer.CsvAppender;
import de.siegmar.fastcsv.writer.CsvWriter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class reading a lemma JSON file, generating topics from these documents and saving several model files.
 * Also used by {@link HierarchicalTopicModelling} to run its main and sub models independently.
 * <br>
 * The files saved include:<br>
 * - Topics and Documents JSON files;<br>
 * - (optional) The topic-to-topic similarity matrix CSV file;<br>
 * - (optional) Model's logs of its log-likelihood and topic (JSON files);<br>
 * - (optional) The topic model object serialised (SER file).
 *
 * @author T. Methven, P. Le Bras
 * @version 2
 */
public class TopicModelling {

    /** List of 100 random seeds to initialise the model's sampler. */
    private final int[] RANDOM_SEEDS = {
            1351727940,  1742863098,  -1602079425, 1775435783,  568478633,   -1728550799, -951342906,  201354591,   1964976895,  1996681054,
            -1470540617, 2021180607,  1963517091,  -62811111,   1289694793,  -1538086464, -336032733,  1785570484,  1255020924,  1973504944,
            668901209,   -1994103157, 1499498950,  1863986805,  767661644,   1106985431,  1044245999,  -462881427,  667772453,   -1412242423,
            884961209,   -2010762614, -958108485,  -1949179036, -1730305825, 1389240794,  836782564,   -785551752,  1933688975,  -1999538859,
            -263090972,  -508702554,  1140385921,  1267873921,  -1344871923, -43961586,   -233705489,  628409593,   899215101,   1093870969,
            -961964970,  771817120,   666140854,   -1449071564, -1636392498, -7026344,    1974585266,  -685538084,  366222201,   -1186218688,
            1079183802,  -1051858470, 25585342,    855028013,   1678916685,  1972641650,  202789157,   -1552096200, -1506270777, -1041494119,
            1463369471,  -1055350006, 1349843049,  -1101551872, 1843673222,  -644314819,  -1303113477, -1069086690, 498408088,   -114723521,
            -637117566,  1420898137,  366206483,   213561271,   1791833142,  -919814411,  1104666572,  1089758161,  -513481178,  291291728,
            -1821691956, -1915769653, 132274482,   1199014123,  1864061694,  -1589540732, 295595372,   -131466196,  -2096364649, -699552916};

    /** Number of documents skipped for the model, ie, with too few lemmas. */
    private int skipCount = 0;

    // Data structures

    /** JSON object containing the documents metadata. */
    private JSONObject metadata;
    /** List of documents, read from the lemmas JSON file, and updated by the model. */
    private ConcurrentHashMap<String, DocIOWrapper> Documents;
    /** List of topics, generated from the model. */
    private ConcurrentHashMap<String, TopicIOWrapper> Topics;
    /** List of documents given to the model as input. */
    private List<InputDocument> ModelInput;
    /** Topic-to-topic similarity matrix. */
    private double[][] SimilarityMatrix;
    /** Topic model object. */
    private TopicModel tModel;

    // Model settings

    /** Number of topics to model. */
    private int nTopics;
    /** Number of top words/labels to save in the topic data. */
    private int nWords = 20;
    /** Number of top documents to save in the topic data. */
    private int nDocs = 20;
    /** Number of sampling iterations. */
    private int nIterationsSampling = 1000;
    /** Index of the random seed to use. */
    private int seedIndex = 0;
    /** Sum of the alpha hyperparameters. */
    private double alphaSum = 1.0;
    /** Beta hyperparameters. */
    private double beta = 0.01;
    /** Boolean flag for using symmetrical alpha-values. */
    private boolean symmetricAlpha = false;
    /** Hyperparameters optimisation intervals. */
    private int optimInterval = 50;
    /** Number of maximisation iterations. */
    private int nIterationsMaximisation = 0;

    // Output settings

    /** Output directory for MALLET files. */
    private String outputDir;
    /** Filename for the documents JSON file. */
    private String docOutput;
    /** Filename for the topics JSON file. */
    private String topicOutput;

    /** Filename for the topic similarity matrix CSV file. */
    private String simOutput = "";
    /** Boolean flag for writing the topic similarity matrix on file. */
    private boolean outputSim = false;
    /** Number of top words to use to identify topics in the topic similarity matrix file. */
    private int numWordId = 3;

    /** Filename for the model's log-likelihood logs JSON file. */
    private String llOutput = "";
    /** Boolean flag for writing the model's log-likelihood logs on file. */
    private boolean outputLL = false;

    /** Filename for the model's topic logs JSON file. */
    private String topicLogOutput = "";
    /** Boolean flag for writing the model's topic logs on file. */
    private boolean outputTopicLog = false;

    /** Filename for the serialised model. */
    private String serialiseFile = "";
    /** Boolean flag for writing the serialised model on file. */
    private boolean serialise = false;

    /**
     * Method running a topic model as sub-module, eg, for hierarchical modelling.
     * @param specs Module specifications.
     * @param modelSpecs Specifications for this topic model.
     * @param reader Reader object to get the lemmas.
     * @return This model sub-module.
     */
    public static TopicModelling Model(TopicModelModuleSpecs specs, ModelSpecs modelSpecs, LemmaReader reader){
        TopicModelling startClass = new TopicModelling();
        startClass.ProcessArguments(specs, modelSpecs, reader);
        startClass.AddLemmasToModel();
        startClass.RunTopicModel();
        startClass.GetAndSetDocumentDistributions();
        startClass.GetAndSetTopicDetails();
        startClass.GetAndSetTopicSimilarity();
        return startClass;
    }

    /**
     * Main method (if class used as a module), reads the specification and launches the sub-methods in order.
     * @param specs Specifications.
     * @return String indicating the time taken to read the lemmas JSON file, model topics and associated data and produce the output files.
     */
    public static String SingleModel(TopicModelModuleSpecs specs){

        LogPrint.printModuleStart("Simple topic modelling");

        long startTime = System.currentTimeMillis();

        TopicModelling startClass = new TopicModelling();
        LemmaReader reader = new LemmaReader(specs.lemmas);
        startClass.ProcessArguments(specs, specs.mainModel, reader);
        startClass.AddLemmasToModel();
        startClass.RunTopicModel();
        startClass.GetAndSetDocumentDistributions();
        startClass.GetAndSetTopicDetails();
        startClass.GetAndSetTopicSimilarity();
        startClass.SaveTopics();
        startClass.SaveDocuments(startClass.nTopics, -1);

        long timeTaken = (System.currentTimeMillis() - startTime) / (long)1000;

        LogPrint.printModuleEnd("Simple topic modelling");

        return "Simple topic modelling: "+Math.floorDiv(timeTaken, 60) + " m, " + timeTaken % 60 + " s";
    }

    /**
     * Method processing the specification parameters.
     * @param specs Module specifications.
     * @param modelSpecs Specifications for the model.
     * @param reader Reader object to get the lemmas.
     */
    private void ProcessArguments(TopicModelModuleSpecs specs, ModelSpecs modelSpecs, LemmaReader reader){
        LogPrint.printNewStep("Processing arguments", 0);
        outputDir = specs.dataDir;
        docOutput = specs.documentOutput;

        Documents = reader.getDocuments();
        metadata = reader.getMetadata();

        nTopics = modelSpecs.topics;
        nWords = modelSpecs.words;
        nDocs = modelSpecs.docs;
        nIterationsSampling = modelSpecs.iterations;
        nIterationsMaximisation = modelSpecs.iterationsMax;
        alphaSum = modelSpecs.alphaSum;
        beta = modelSpecs.beta;
        symmetricAlpha = modelSpecs.symmetricAlpha;
        optimInterval = modelSpecs.optimInterval;
        seedIndex=modelSpecs.seedIndex;

        topicOutput = modelSpecs.topicOutput;

        serialise = modelSpecs.serialise;
        if(serialise){
            serialiseFile = modelSpecs.serialiseFile;
        }
        outputSim = modelSpecs.outputSimilarity;
        if(outputSim){
            simOutput = modelSpecs.similarityOutput;
            numWordId = modelSpecs.numWordId;
        }
        outputLL = modelSpecs.outputLL;
        if(outputLL){
            llOutput = modelSpecs.llOutput;
        }
        outputTopicLog = modelSpecs.outputTopicLog;
        if(outputTopicLog){
            topicLogOutput = modelSpecs.topicLogOutput;
        }

        LogPrint.printCompleteStep();
        String iter = " in "+nIterationsSampling+" sampling iterations";
        if(nIterationsMaximisation > 0) iter += " and "+nIterationsMaximisation+" maximisation iterations";
        LogPrint.printNote("Modelling "+nTopics+" topics"+iter);
        LogPrint.printNote("Saving "+nWords+" words and "+nDocs+" docs");
        String sym = "";
        if(symmetricAlpha) sym += " (symmetrical) ";
        LogPrint.printNote("Using AlphaSum of "+alphaSum+sym+" and Beta of "+beta);
        if(outputSim){
            LogPrint.printNote("Saving topic to topic similarity, identifying topics with "+numWordId+" labels");
        }
        if(outputLL){
            LogPrint.printNote("Saving model Log-Likelihood records");
        }
        if(outputTopicLog){
            LogPrint.printNote("Saving topic history records");
        }
        if(serialise){
            LogPrint.printNote("Serialising model");
        }
    }

    /**
     * Method reading the lemmas from the input lemmatised corpus and adding them to a list of documents for the model.
     */
    private void AddLemmasToModel(){
        LogPrint.printNewStep("Adding lemmas to model", 0);
        ModelInput = new ArrayList<>();
        for(int i = 0; i < Documents.size(); i++){
            ModelInput.add(null);
        }
        Documents.entrySet().forEach(this::addLemmaToModel);
        LogPrint.printCompleteStep();
        if(skipCount > 0){
            LogPrint.printNote("Skipped "+skipCount+" documents");
        }
    }

    /**
     * Method checking the validity of a document from the input corpus and adding it to the list of documents for the model.
     * Valid documents are lemmatised and have sufficient number of lemmas.
     * @param entry Lemmatised document entry to check and add to the list of documents for the model.
     */
    private void addLemmaToModel(Map.Entry<String, DocIOWrapper> entry){
        DocIOWrapper doc = entry.getValue();
        if(doc.getLemmaString() == null){
            LogPrint.printNoteError("Cannot find lemmas\n");
            System.exit(1);
        } else {
            if(!doc.isRemoved() && doc.getLemmaString().length() > 0){
                ModelInput.set(doc.getIndex(), new InputDocument(doc.getId(), doc.getLemmaString()));
            } else {
                skipCount++;
            }
        }
    }

    /**
     * Method running the topic model.
     */
    private void RunTopicModel(){
        LogPrint.printNewStep("Topic modelling", 0);
        LogPrint.printNote("Following output from Mallet\n");

        tModel = new TopicModel(ModelInput);
        tModel.TOPICS = nTopics;
        tModel.SEED = RANDOM_SEEDS[seedIndex];
        tModel.ITERSAMPLING = nIterationsSampling;
        tModel.ITERMAXIMISE = nIterationsMaximisation;
        tModel.OPTIMINTERVAL = optimInterval;
        tModel.ALPHASUM = alphaSum;
        tModel.BETA = beta;
        tModel.SYMMETRICALPHA = symmetricAlpha;

        tModel.Model(outputDir);

        if(tModel.modelledDocuments == null || tModel.modelledDocuments.isEmpty()){
            LogPrint.printNote("Model failed! Trying again");
            RunTopicModel();
        } else {
            LogPrint.printNote("Model completed");
        }

        LogPrint.printNewStep("Topic modelling", 0);
        LogPrint.printCompleteStep();

        if(outputLL) saveLogLikehood();
        if(outputTopicLog) saveTopicLog();
        if(serialise) serialiseModel();
    }

    /**
     * Method writing the model's log-likelihood logs on file (JSON).
     */
    private void saveLogLikehood() { JSONIOWrapper.SaveJSON(tModel.logLikelihoodRecord.toJSON(), llOutput, 0); }

    /**
     * Method writing the model's topic logs on file (JSON).
     */
    private void saveTopicLog() { JSONIOWrapper.SaveJSON(tModel.topicRecord.toJSON(), topicLogOutput, 0); }

    /**
     * Method serialising the model on file (SER).
     */
    private void serialiseModel(){
        LogPrint.printNewStep("Serialising model", 0);
        try{
            FileOutputStream fileOut = new FileOutputStream(serialiseFile);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(tModel);
            out.close();
            fileOut.close();
            LogPrint.printCompleteStep();
        } catch (IOException e) {
            LogPrint.printNoteError("Error: Could not serialize data!");
            LogPrint.printNoteError(e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Method adding the topic distribution to the documents.
     */
    private void GetAndSetDocumentDistributions(){
        LogPrint.printNewStep("Adding topic distributions to documents", 0);
        for(Map.Entry<String, DocIOWrapper> entry: Documents.entrySet()){
            if(tModel.stringIDtoNumID.containsKey(entry.getKey())){
                ModelledDocument doc = tModel.modelledDocuments.get(tModel.stringIDtoNumID.get(entry.getKey()));
                entry.getValue().setMainTopicDistribution(doc.topicDistribution);
                // update the lemmas used in the model
                entry.getValue().setLemmas(Arrays.asList(doc.modelLemmas));
                System.out.println("test");
            }
        }
        LogPrint.printCompleteStep();
    }

    /**
     * Method generating the list of topics.
     */
    private void GetAndSetTopicDetails(){
        LogPrint.printNewStep("Creating topic data", 0);
        Topics = new ConcurrentHashMap<>();
        for(int t = 0; t < tModel.TOPICS; t++){
            ModelledTopic topic = tModel.modelledTopics.get(t);
            // Get the top nWords words
            List<TopicIOWrapper.JSONTopicWeight> topicWords = new ArrayList<>();
            int count = 0;
            for(int l = 0; l < topic.nLabels; l++){
                if(count >= nWords) break;
                Pair<String, Double> label = topic.getLabel(l);
                topicWords.add(new TopicIOWrapper.JSONTopicWeight(label.getLeft(), label.getRight()));
            }
            // Get the top nDocs documents
            List<TopicIOWrapper.JSONTopicWeight> topicDocs = new ArrayList<>();
            count = 0;
            for(int d = 0; d < topic.nDocs; d++){
                if(count > nDocs) break;
                Pair<String, Double> doc = topic.getDoc(d);
                topicDocs.add(new TopicIOWrapper.JSONTopicWeight(doc.getLeft(), doc.getRight()));
            }
            // Create the new topic and add to our list
            Topics.put(Integer.toString(t), new TopicIOWrapper(Integer.toString(t), t, topicWords, topicDocs));
        }
        LogPrint.printCompleteStep();
    }

    /**
     * Method calculating the similarity between the model's topics.
     */
    private void GetAndSetTopicSimilarity(){
        SimilarityMatrix = TopicsSimilarity.DocumentCosineSimilarity(tModel.modelledDocuments);
        if(outputSim){
            saveSimilarityMatrix();
        }
    }

    /**
     * Getter method for the model's inner list of documents.
     * @return The list of documents (from the model object).
     */
    public List<ModelledDocument> getModelledDocuments(){
        return tModel.modelledDocuments;
    }

    /**
     * Getter method for the model's inner list of topics.
     * @return The list of topics (from the model object).
     */
    public List<ModelledTopic> getModelledTopics(){
        return tModel.modelledTopics;
    }

    /**
     * Getter method for the list of topics.
     * @return The list of topics.
     */
    public ConcurrentHashMap<String, TopicIOWrapper> getTopics(){
        return Topics;
    }

    /**
     * Getter method for the list of documents.
     * @return The list of documents.
     */
    public ConcurrentHashMap<String, DocIOWrapper> getDocuments(){
        return Documents;
    }

    /**
     * Getter method for the topics' concatenated list of top labels (for identification).
     * @return List of topics' concatenated top labels.
     */
    public String[] getTopicsLabels(){
        String[] labels = new String[nTopics];
        for(int t = 0; t < nTopics; t++){
            labels[t] = Topics.get(Integer.toString(t)).getLabelString(numWordId);
        }
        return labels;
    }

    /**
     * Method writing the topic-to-topic similarity matrix on file (CSV).
     */
    public void saveSimilarityMatrix(){
        LogPrint.printNewStep("Saving topic similarities", 0);
        File file = new File(simOutput);
        file.getParentFile().mkdirs();
        CsvWriter writer = new CsvWriter();
        writer.setAlwaysDelimitText(true);
        try(CsvAppender appender = writer.append(file, StandardCharsets.UTF_8)){
            String[] labels = getTopicsLabels();
            appender.appendField("");
            for(int t = 0; t < nTopics; t++){
                appender.appendField(labels[t]);
            }
            appender.endLine();
            for(int y = 0; y < SimilarityMatrix.length; y++){
                appender.appendField(labels[y]);
                for(int x = 0; x < SimilarityMatrix.length; x++){
                    appender.appendField(String.valueOf(SimilarityMatrix[x][y]));
                }
                appender.endLine();
            }
            LogPrint.printCompleteStep();
        } catch (Exception e){
            LogPrint.printNoteError("Error while saving similarity matrix\n");
            e.printStackTrace();
        }
    }

    /**
     * Method writing the list of topics on file (JSON).
     */
    public void SaveTopics(){
        JSONObject root = new JSONObject();
        JSONArray topics = new JSONArray();
        JSONObject meta = (JSONObject) metadata.clone();
        meta.put("nTopics", nTopics);
        meta.put("nWords", nWords);
        meta.put("nDocs", nDocs);
        root.put("metadata", meta);
        for(Map.Entry<String, TopicIOWrapper> entry: Topics.entrySet()){
            topics.add(entry.getValue().toJSON());
        }
        root.put("topics", topics);
        JSONArray topicsSimilarities = new JSONArray();
        for(int y = 0; y < SimilarityMatrix.length; y++){
            JSONArray SimRow = new JSONArray();
            for(int x = 0; x < SimilarityMatrix.length; x++){
                SimRow.add(SimilarityMatrix[x][y]);
            }
            topicsSimilarities.add(SimRow);
        }
        root.put("similarities", topicsSimilarities);
        JSONIOWrapper.SaveJSON(root, topicOutput, 0);
    }

    /**
     * Method writing the list of documents on file (JSON).
     */
    public void SaveDocuments(int nTopicsMain, int nTopicsSub){
        JSONObject root = new JSONObject();
        JSONArray documents = new JSONArray();
        JSONObject meta = (JSONObject) metadata.clone();
        if(nTopicsSub < 0){
            meta.put("nTopicsMain", nTopics);
        }
        else {
            meta.put("nTopicsMain", nTopicsMain);
            meta.put("nTopicsSub", nTopicsSub);
        }
        root.put("metadata", meta);
        DocIOWrapper.PrintModel();
        for(Map.Entry<String, DocIOWrapper> entry: Documents.entrySet()){
            documents.add(entry.getValue().toJSON());
        }
        root.put("documents", documents);
        JSONIOWrapper.SaveJSON(root, docOutput, 0);
    }
}