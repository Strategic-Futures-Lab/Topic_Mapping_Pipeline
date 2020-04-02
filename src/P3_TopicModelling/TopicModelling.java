package P3_TopicModelling;

import P0_Project.ModelSpecs;
import P0_Project.ProjectModel;
import P3_TopicModelling.Similarity.TopicsSimilarity;
import P3_TopicModelling.TopicModelCore.*;
import PX_Data.*;
import cc.mallet.types.Alphabet;
import cc.mallet.types.IDSorter;
import de.siegmar.fastcsv.writer.CsvAppender;
import de.siegmar.fastcsv.writer.CsvWriter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TopicModelling {



    private final int[] RANDOM_SEEDS = {1351727940, 1742863098, -1602079425, 1775435783, 568478633, -1728550799, -951342906, 201354591, 1964976895, 1996681054, -1470540617, 2021180607, 1963517091, -62811111, 1289694793, -1538086464, -336032733, 1785570484, 1255020924, 1973504944, 668901209, -1994103157, 1499498950, 1863986805, 767661644, 1106985431, 1044245999, -462881427, 667772453, -1412242423, 884961209, -2010762614, -958108485, -1949179036, -1730305825, 1389240794, 836782564, -785551752, 1933688975, -1999538859, -263090972, -508702554, 1140385921, 1267873921, -1344871923, -43961586, -233705489, 628409593, 899215101, 1093870969, -961964970, 771817120, 666140854, -1449071564, -1636392498, -7026344, 1974585266, -685538084, 366222201, -1186218688, 1079183802, -1051858470, 25585342, 855028013, 1678916685, 1972641650, 202789157, -1552096200, -1506270777, -1041494119, 1463369471, -1055350006, 1349843049, -1101551872, 1843673222, -644314819, -1303113477, -1069086690, 498408088, -114723521, -637117566, 1420898137, 366206483, 213561271, 1791833142, -919814411, 1104666572, 1089758161, -513481178, 291291728, -1821691956, -1915769653, 132274482, 1199014123, 1864061694, -1589540732, 295595372, -131466196, -2096364649, -699552916};

    private int skipCount = 0;

    private JSONObject metadata;
    private ConcurrentHashMap<String, JSONDocument> Documents;
    private ConcurrentHashMap<String, JSONTopic> Topics;
    private List<Document> DocumentList = new ArrayList<>();
    private double[][] SimilarityMatrix;
    private List<List<WordData>> wordsAndWeights;
    private TopicModel tModel;
    private int nTopics;
    private int nWords;
    private int nDocs;
    private int nIterations;
    private String outputDir;
    private String docOutput;
    private String topicOutput;
    private String simOutput = null;
    private int numWordId;

    public static TopicModelling Model(ProjectModel specs, ModelSpecs modelSpecs, LemmaReader reader){
        TopicModelling startClass = new TopicModelling();
        startClass.ProcessArguments(specs, modelSpecs, reader);
        startClass.AddLemmasToModel();
        startClass.RunTopicModel(0);
        startClass.GetAndSetDocumentDistributions();
        startClass.GetAndSetTopicDetails();
        startClass.GetAndSetTopicSimilarity();
        return startClass;
    }

    public static void SingleModel(ProjectModel specs){
        System.out.println( "**********************************************************\n" +
                            "* STARTING Topic Modelling !                             *\n" +
                            "**********************************************************\n");

        TopicModelling startClass = new TopicModelling();
        LemmaReader reader = new LemmaReader(specs.lemmas);
        startClass.ProcessArguments(specs, specs.mainModel, reader);
        startClass.AddLemmasToModel();
        startClass.RunTopicModel(0);
        startClass.GetAndSetDocumentDistributions();
        startClass.GetAndSetTopicDetails();
        startClass.GetAndSetTopicSimilarity();
        startClass.SaveTopics();
        startClass.SaveDocuments(startClass.nTopics, -1);

        System.out.println( "**********************************************************\n" +
                            "* Topic Modelling COMPLETE !                             *\n" +
                            "**********************************************************\n");
    }

    private void ProcessArguments(ProjectModel specs, ModelSpecs modelSpecs, LemmaReader reader){
        outputDir = specs.outputDir;
        docOutput = specs.documentOutput;

        Documents = reader.getDocuments();
        metadata = reader.getMetadata();

        nTopics = modelSpecs.topics;
        nWords = modelSpecs.words;
        nDocs = modelSpecs.docs;
        nIterations = modelSpecs.iterations;
        topicOutput = modelSpecs.topicOutput;
        if(modelSpecs.outputSimilarity){
            simOutput = modelSpecs.similarityOutput;
        }
        numWordId = modelSpecs.numWordId;
    }

    private void AddLemmasToModel(){
        System.out.println("Adding Lemmas to Model ...");
        for(int i = 0; i < Documents.size(); i++){
            DocumentList.add(null);
        }
        Documents.entrySet().forEach(this::addLemmaToModel);
        if(skipCount > 0){
            System.out.println("Skipped "+skipCount+" documents!!");
        }
        System.out.println("Lemmas Added to Model!");
    }

    private void addLemmaToModel(Map.Entry<String, JSONDocument> entry){
        String key = entry.getKey();
        JSONDocument doc = entry.getValue();
        if(doc.getLemmaString() == null){
            System.out.println("\n************\nERROR! Cannot find Lemmas.\n************\n");
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

    private void RunTopicModel(int number){
        System.out.println("Starting Topic Modelling ...\nFollowing output from Mallet.");

        tModel = new TopicModel(DocumentList);
        tModel.numTopics = nTopics;
        tModel.name = "topicModelNum"+number;
        tModel.SEED = RANDOM_SEEDS[number];
        tModel.ITER = nIterations;

        if(number == 0){
            tModel.Model(false, true, outputDir);
        } else {
            tModel.Model(false, false, outputDir);
        }

        if(tModel.topicDistributions == null || tModel.topicDistributions.isEmpty()){
            System.out.println("Model "+number+" failed!\nTrying again...");
            RunTopicModel(number);
        } else {
            System.out.println("Model "+number+" completed!");
        }

        System.out.println("Topic Modelling Complete!");
    }

    private void GetAndSetDocumentDistributions(){
        System.out.println("Adding Topic Distributions to Documents ...");

        //If you change the first value to false, you get the actual number of words distributed to each topic from each document!
        double[][] distributions = tModel.model.getDocumentTopics(true, false);

        for(Map.Entry<String, JSONDocument> entry: Documents.entrySet()){
            if(tModel.stringIDtoNumID.containsKey(entry.getKey())){
                int modelPosition = tModel.stringIDtoNumID.get(entry.getKey());
                entry.getValue().setTopicDistribution(distributions[modelPosition]);
            }
        }

        System.out.println("Topic Distributions Added to Documents!");
    }

    private void GetAndSetTopicDetails(){
        System.out.println("Creating Topic Data ...");
        Topics = new ConcurrentHashMap<>();

        ArrayList<TreeSet<IDSorter>> sortedWords = tModel.model.getSortedWords();
        Alphabet alphabet = tModel.model.getAlphabet();

        ArrayList<TreeSet<IDSorter>> topicDocuments = tModel.model.getTopicDocuments(0);

        for(int topic = 0; topic < sortedWords.size(); topic++){
            List<WordWeight> topicWords = new ArrayList<>();
            int count = 0;
            for(IDSorter word: sortedWords.get(topic)){
                if(count >= nWords) break;
                if(word.getWeight() > 0){
                    topicWords.add(new WordWeight((String) alphabet.lookupObject(word.getID()), word.getWeight()));
                }
                count++;
            }

            List<DocWeight> topicDocs = new ArrayList<>();
            count = 0;
            for(IDSorter document: topicDocuments.get(topic)){
                if(count >= nDocs) break;
                if(document.getWeight() > 0){
                    topicDocs.add(new DocWeight(tModel.numIDtoStringID.get(document.getID()), document.getWeight()));
                }
                count++;
            }

            Topics.put(Integer.toString(topic), new JSONTopic(Integer.toString(topic), topic, topicWords, topicDocs));
        }

        System.out.println("Topic Data Created!");
    }

    private void GetAndSetTopicSimilarity(){
        SimilarityMatrix = TopicsSimilarity.GetSimilarityMatrix(nTopics, tModel.topicDistributions);
        if(simOutput != null){
            SaveSimilarityMatrix();
        }
    }

    public List<TopicData> getTopicDistributions(){
        return tModel.topicDistributions;
    }

    public ConcurrentHashMap<String, JSONTopic> getTopics(){
        return Topics;
    }

    public ConcurrentHashMap<String, JSONDocument> getDocuments(){
        return Documents;
    }

    public String[] getTopicsLabels(){
        String[] labels = new String[nTopics];
        for(int t = 0; t < nTopics; t++){
            labels[t] = Topics.get(Integer.toString(t)).getLabelString(numWordId);
        }
        return labels;
    }

    public void SaveSimilarityMatrix(){
        System.out.println("Saving Topic Similarities ...");
        File file = new File(simOutput);
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
        } catch (Exception e){
            e.printStackTrace();
        }
        System.out.println("Topic Similarities Saved!");
    }

    public void SaveTopics(){
        System.out.println("Saving Topics...");
        JSONObject root = new JSONObject();
        JSONArray topics = new JSONArray();
        JSONObject meta = (JSONObject) metadata.clone();
        meta.put("nTopics", nTopics);
        meta.put("nWords", nWords);
        meta.put("nDocs", nDocs);
        root.put("metadata", meta);
        for(Map.Entry<String, JSONTopic> entry: Topics.entrySet()){
            topics.add(entry.getValue().toJSON());
        }
        root.put("topics", topics);
        JSONArray topicSimilarity = new JSONArray();
        for(int y = 0; y < SimilarityMatrix.length; y++){
            JSONArray SimRow = new JSONArray();
            for(int x = 0; x < SimilarityMatrix.length; x++){
                SimRow.add(SimilarityMatrix[x][y]);
            }
            topicSimilarity.add(SimRow);
        }
        root.put("topicSimilarity", topicSimilarity);
        JSONIOWrapper.SaveJSON(root, topicOutput);
        System.out.println("Topics Saved!");
    }

    public void SaveDocuments(int nTopicsMain, int nTopicsSub){
        System.out.println("Saving Documents...");
        JSONObject root = new JSONObject();
        JSONArray documents = new JSONArray();
        JSONObject meta = (JSONObject) metadata.clone();
        if(nTopicsSub < 0){
            meta.put("nTopics", nTopics);
        }
        else {
            meta.put("nTopicsMain", nTopicsMain);
            meta.put("nTopicsSub", nTopicsSub);
        }
        root.put("metadata", meta);
        JSONDocument.PrintModel();
        for(Map.Entry<String, JSONDocument> entry: Documents.entrySet()){
            documents.add(entry.getValue().toJSON());
        }
        root.put("documents", documents);
        JSONIOWrapper.SaveJSON(root, docOutput);
        System.out.println("Documents Saved!");
    }
}