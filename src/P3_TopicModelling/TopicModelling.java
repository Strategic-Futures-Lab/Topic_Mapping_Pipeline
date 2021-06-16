package P3_TopicModelling;

import P0_Project.ModelSpecs;
import P0_Project.TopicModelModuleSpecs;
import P3_TopicModelling.Similarity.TopicsSimilarity;
import P3_TopicModelling.TopicModelCore.*;
import PX_Data.*;
import PY_Helper.LogPrint;
import cc.mallet.types.Alphabet;
import cc.mallet.types.IDSorter;
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

public class TopicModelling {

    private final int[] RANDOM_SEEDS = {1351727940, 1742863098, -1602079425, 1775435783, 568478633, -1728550799, -951342906, 201354591, 1964976895, 1996681054,
                                        -1470540617, 2021180607, 1963517091, -62811111, 1289694793, -1538086464, -336032733, 1785570484, 1255020924, 1973504944,
                                        668901209, -1994103157, 1499498950, 1863986805, 767661644, 1106985431, 1044245999, -462881427, 667772453, -1412242423,
                                        884961209, -2010762614, -958108485, -1949179036, -1730305825, 1389240794, 836782564, -785551752, 1933688975, -1999538859,
                                        -263090972, -508702554, 1140385921, 1267873921, -1344871923, -43961586, -233705489, 628409593, 899215101, 1093870969,
                                        -961964970, 771817120, 666140854, -1449071564, -1636392498, -7026344, 1974585266, -685538084, 366222201, -1186218688,
                                        1079183802, -1051858470, 25585342, 855028013, 1678916685, 1972641650, 202789157, -1552096200, -1506270777, -1041494119,
                                        1463369471, -1055350006, 1349843049, -1101551872, 1843673222, -644314819, -1303113477, -1069086690, 498408088, -114723521,
                                        -637117566, 1420898137, 366206483, 213561271, 1791833142, -919814411, 1104666572, 1089758161, -513481178, 291291728,
                                        -1821691956, -1915769653, 132274482, 1199014123, 1864061694, -1589540732, 295595372, -131466196, -2096364649, -699552916};

    private int skipCount = 0;

    private JSONObject metadata;
    private ConcurrentHashMap<String, DocIOWrapper> Documents;
    private ConcurrentHashMap<String, TopicIOWrapper> Topics;
    private List<Document> DocumentList = new ArrayList<>();
    private double[][] SimilarityMatrix;
    private List<List<WordData>> wordsAndWeights;
    private TopicModel tModel;

    private int nTopics;
    private int nWords = 20;
    private int nDocs = 20;
    private int nIterations = 1000;
    private String serialiseFile = "";
    private boolean serialise = false;
    private String outputDir;
    private String docOutput;
    private String topicOutput;

    private String simOutput = "";
    private boolean outputSim = false;
    private int numWordId = 3;
    private String llOutput = "";
    private boolean outputLL = false;
    private int seedIndex = 0;
    private double alphaSum = 1.0;
    private double beta = 0.01;
    private boolean symmetricAlpha = false;
    private int optimInterval = 50;
    private String topicLogOutput = "";
    private boolean outputTopicLog = false;

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

    private void ProcessArguments(TopicModelModuleSpecs specs, ModelSpecs modelSpecs, LemmaReader reader){
        LogPrint.printNewStep("Processing arguments", 0);
        outputDir = specs.dataDir;
        docOutput = specs.documentOutput;

        Documents = reader.getDocuments();
        metadata = reader.getMetadata();

        nTopics = modelSpecs.topics;
        nWords = modelSpecs.words;
        nDocs = modelSpecs.docs;
        nIterations = modelSpecs.iterations;
        serialise = modelSpecs.serialise;
        if(serialise){
            serialiseFile = modelSpecs.serialiseFile;
        }
        topicOutput = modelSpecs.topicOutput;

        outputSim = modelSpecs.outputSimilarity;
        if(outputSim){
            simOutput = modelSpecs.similarityOutput;
        }
        numWordId = modelSpecs.numWordId;
        outputLL = modelSpecs.outputLL;
        if(outputLL){
            llOutput = modelSpecs.llOutput;
        }
        alphaSum = modelSpecs.alphaSum;
        beta = modelSpecs.beta;
        symmetricAlpha = modelSpecs.symmetricAlpha;
        optimInterval = modelSpecs.optimInterval;
        seedIndex=modelSpecs.seedIndex;
        outputTopicLog = modelSpecs.outputTopicLog;
        if(outputTopicLog){
            topicLogOutput = modelSpecs.topicLogOutput;
        }
        LogPrint.printCompleteStep();
        LogPrint.printNote("Modelling "+nTopics+" topics in "+nIterations+" iterations");
        LogPrint.printNote("Using AlphaSum of "+alphaSum+" and beta of "+beta);
        LogPrint.printNote("Saving "+nWords+" words and "+nDocs+" docs");
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

    private void AddLemmasToModel(){
        LogPrint.printNewStep("Adding lemmas to model", 0);
        for(int i = 0; i < Documents.size(); i++){
            DocumentList.add(null);
        }
        Documents.entrySet().forEach(this::addLemmaToModel);
        LogPrint.printCompleteStep();
        if(skipCount > 0){
            LogPrint.printNote("Skipped "+skipCount+" documents");
        }
    }

    private void addLemmaToModel(Map.Entry<String, DocIOWrapper> entry){
        DocIOWrapper doc = entry.getValue();
        if(doc.getLemmaString() == null){
            LogPrint.printNoteError("Cannot find lemmas\n");
            System.exit(1);
        } else {
            if(doc.getLemmaString().length() > 0){
                if(doc.isRemoved()){
                    skipCount++;
                } else {
                    DocumentList.set(doc.getIndex(), new Document(doc.getId(), doc.getLemmaString()));
                }
            } else {
                skipCount++;
            }
        }
    }

    private void RunTopicModel(){
        LogPrint.printNewStep("Topic modelling", 0);
        LogPrint.printNote("Following output from Mallet\n");

        tModel = new TopicModel(DocumentList);
        tModel.TOPICS = nTopics;
        tModel.SEED = RANDOM_SEEDS[seedIndex];
        tModel.ITER = nIterations;
        tModel.OPTIMINTERVAL = optimInterval;
        tModel.ALPHASUM = alphaSum;
        tModel.BETA = beta;
        tModel.SYMMETRICALPHA = symmetricAlpha;

        tModel.Model(outputDir);

        if(tModel.topicDistributions == null || tModel.topicDistributions.isEmpty()){
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

    private void saveLogLikehood() { JSONIOWrapper.SaveJSON(tModel.logLikelihoodRecord.toJSON(), llOutput, 0); }

    private void saveTopicLog() { JSONIOWrapper.SaveJSON(tModel.topicRecord.toJSON(), topicLogOutput, 0); }

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

    private void GetAndSetDocumentDistributions(){
        LogPrint.printNewStep("Adding topic distributions to documents", 0);

        //If you change the first value to false, you get the actual number of words distributed to each topic from each document!
        double[][] distributions = tModel.model.getDocumentTopics(true, false);

        for(Map.Entry<String, DocIOWrapper> entry: Documents.entrySet()){
            if(tModel.stringIDtoNumID.containsKey(entry.getKey())){
                int modelPosition = tModel.stringIDtoNumID.get(entry.getKey());
                entry.getValue().setMainTopicDistribution(distributions[modelPosition]);
            }
        }

        LogPrint.printCompleteStep();
    }

    private void GetAndSetTopicDetails(){
        LogPrint.printNewStep("Creating topic data", 0);
        Topics = new ConcurrentHashMap<>();

        ArrayList<TreeSet<IDSorter>> sortedWords = tModel.model.getSortedWords();
        Alphabet alphabet = tModel.model.getAlphabet();

        ArrayList<TreeSet<IDSorter>> topicDocuments = tModel.model.getTopicDocuments(0);

        for(int topic = 0; topic < sortedWords.size(); topic++){
            List<TopicIOWrapper.JSONTopicWeight> topicWords = new ArrayList<>();
            int count = 0;
            for(IDSorter word: sortedWords.get(topic)){
                if(count >= nWords) break;
                if(word.getWeight() > 0){
                    topicWords.add(new TopicIOWrapper.JSONTopicWeight((String) alphabet.lookupObject(word.getID()), word.getWeight()));
                }
                count++;
            }

            List<TopicIOWrapper.JSONTopicWeight> topicDocs = new ArrayList<>();
            count = 0;
            for(IDSorter document: topicDocuments.get(topic)){
                if(count >= nDocs) break;
                if(document.getWeight() > 0){
                    topicDocs.add(new TopicIOWrapper.JSONTopicWeight(tModel.numIDtoStringID.get(document.getID()), document.getWeight()));
                }
                count++;
            }

            Topics.put(Integer.toString(topic), new TopicIOWrapper(Integer.toString(topic), topic, topicWords, topicDocs));
        }

        LogPrint.printCompleteStep();
    }



    public List<List<TopicIOWrapper.JSONTopicWeight>> getTopicLabelsAndWeights()
    {
        List<List<TopicIOWrapper.JSONTopicWeight>> wordsAndWeights = new ArrayList<>();


        for(int t = 0; t < nTopics; t++){

            List<WordData> wordDataList = new ArrayList<>();
            TopicIOWrapper topicData = Topics.get(Integer.toString(t));
            List<TopicIOWrapper.JSONTopicWeight> topicWords = topicData.getWords();

            wordsAndWeights.add(topicWords);
        }

        return wordsAndWeights;
    }

    private void GetAndSetTopicSimilarity(){
        SimilarityMatrix = TopicsSimilarity.GetSimilarityMatrix(nTopics, tModel.topicDistributions);
        if(outputSim){
            saveSimilarityMatrix();
        }
    }

    public List<TopicData> getTopicDistributions(){
        return tModel.topicDistributions;
    }

    public ConcurrentHashMap<String, TopicIOWrapper> getTopics(){
        return Topics;
    }

    public ConcurrentHashMap<String, DocIOWrapper> getDocuments(){
        return Documents;
    }

    public String[] getTopicsLabels(){
        String[] labels = new String[nTopics];
        for(int t = 0; t < nTopics; t++){
            labels[t] = Topics.get(Integer.toString(t)).getLabelString(numWordId);
        }
        return labels;
    }

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
        // System.out.println("Topic Similarities Saved!");
    }

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