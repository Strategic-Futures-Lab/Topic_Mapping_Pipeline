package P3_TopicModelling;

import P2_Lemmatise.LemmaJSONDocument;
import P3_TopicModelling.TopicModelCore.*;
import PX_Helper.JSONIOWrapper;
import cc.mallet.types.Alphabet;
import cc.mallet.types.IDSorter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TopicModelling {


    private ConcurrentHashMap<String, ModelJSONTopic> Topics;
    private List<Document> DocumentList = new ArrayList<>();
    private double[][] SimilarityMatrix;
    private List<List<WordData>> wordsAndWeights;
    private TopicModel tModel;
    private final int[] RANDOM_SEEDS = {1351727940, 1742863098, -1602079425, 1775435783, 568478633, -1728550799, -951342906, 201354591, 1964976895, 1996681054, -1470540617, 2021180607, 1963517091, -62811111, 1289694793, -1538086464, -336032733, 1785570484, 1255020924, 1973504944, 668901209, -1994103157, 1499498950, 1863986805, 767661644, 1106985431, 1044245999, -462881427, 667772453, -1412242423, 884961209, -2010762614, -958108485, -1949179036, -1730305825, 1389240794, 836782564, -785551752, 1933688975, -1999538859, -263090972, -508702554, 1140385921, 1267873921, -1344871923, -43961586, -233705489, 628409593, 899215101, 1093870969, -961964970, 771817120, 666140854, -1449071564, -1636392498, -7026344, 1974585266, -685538084, 366222201, -1186218688, 1079183802, -1051858470, 25585342, 855028013, 1678916685, 1972641650, 202789157, -1552096200, -1506270777, -1041494119, 1463369471, -1055350006, 1349843049, -1101551872, 1843673222, -644314819, -1303113477, -1069086690, 498408088, -114723521, -637117566, 1420898137, 366206483, 213561271, 1791833142, -919814411, 1104666572, 1089758161, -513481178, 291291728, -1821691956, -1915769653, 132274482, 1199014123, 1864061694, -1589540732, 295595372, -131466196, -2096364649, -699552916};

    private int skipCount = 0;

    private JSONObject metadata;
    private ConcurrentHashMap<String, ModelJSONDocument> Documents;
    private int nTopics;
    private int nWords;
    private int nDocs;
    private int nIterations;

    public static TopicModelling Model(ConcurrentHashMap<String, ModelJSONDocument> inputDocs,
                             JSONObject meta,
                             int topics,
                             int words,
                             int docs,
                             int iterations,
                             String outputDir){
        System.out.println( "**********************************************************\n" +
                            "* STARTING Topic Modelling !                             *\n" +
                            "**********************************************************\n");

        TopicModelling startClass = new TopicModelling();
        startClass.ProcessArguments(inputDocs, meta, topics, words, docs, iterations);
        startClass.AddLemmasToModel();
        startClass.RunTopicModel(0, outputDir);
        startClass.GetAndSetDocumentDistributions();
        startClass.GetAndSetTopicDetails();
//        startClass.LemmatiseDocuments();
//        startClass.OutputJSON(outputFile);

        System.out.println( "**********************************************************\n" +
                            "* Topic Modelling COMPLETE !                             *\n" +
                            "**********************************************************\n");
        return startClass;
    }

    private void ProcessArguments(ConcurrentHashMap<String, ModelJSONDocument> inputDocs,
                                  JSONObject meta,
                                  int topics, int words, int docs, int iterations){
        Documents = inputDocs;
        metadata = meta;
        nTopics = topics;
        nWords = words;
        nDocs = docs;
        nIterations = iterations;
    }

    private void LoadLemmaFile(String lemmaFile){
        JSONArray lemmas = (JSONArray) JSONIOWrapper.LoadJSON(lemmaFile).get("lemmas");
        Documents = new ConcurrentHashMap<>();
        for(JSONObject docEntry: (Iterable<JSONObject>) lemmas){
            ModelJSONDocument doc = new ModelJSONDocument(docEntry);
            Documents.put(doc.getId(), doc);
        }
        System.out.println("Loaded corpus!");
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

    private void addLemmaToModel(Map.Entry<String, ModelJSONDocument> entry){
        String key = entry.getKey();
        ModelJSONDocument doc = entry.getValue();
        if(doc.getLemmas() == null){
            System.out.println("\n************\nERROR! Cannot find Lemmas.\n************\n");
            System.exit(1);
        } else {
            if(doc.getLemmas().length() > 0){
                if(doc.isRemoved()){
                    skipCount++;
                } else {
                    DocumentList.set(doc.getIndex(), new Document(doc.getId(), doc.getLemmas()));
                }
            } else {
                skipCount++;
            }
        }
    }

    private void RunTopicModel(int number, String corpusLoc){
        System.out.println("Starting Topic Modelling ...\nFollowing output from Mallet.");

        tModel = new TopicModel(DocumentList);
        tModel.numTopics = nTopics;
        tModel.name = "topicModelNum"+number;
        tModel.SEED = RANDOM_SEEDS[number];
        tModel.ITER = nIterations;

        if(number == 0){
            tModel.Model(false, true, corpusLoc);
        } else {
            tModel.Model(false, false, corpusLoc);
        }

        if(tModel.topicDistributions == null || tModel.topicDistributions.isEmpty()){
            System.out.println("Model "+number+" failed!\nTrying again...");
            RunTopicModel(number, corpusLoc);
        } else {
            System.out.println("Model "+number+" completed!");
        }

        System.out.println("Topic Modelling Complete!");
    }

    private void GetAndSetDocumentDistributions(){
        System.out.println("Adding Topic Distributions to Documents ...");

        //If you change the first value to false, you get the actual number of words distributed to each topic from each document!
        double[][] distributions = tModel.model.getDocumentTopics(true, false);

        for(Map.Entry<String, ModelJSONDocument> entry: Documents.entrySet()){
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

            Topics.put(Integer.toString(topic), new ModelJSONTopic(Integer.toString(topic), topic, topicWords, topicDocs));
        }

        System.out.println("Topic Data Created!");
    }

    public List<TopicData> getTopicDistributions(){
        return tModel.topicDistributions;
    }

    public void setTopicsSimilarity(double[][] matrix){
        SimilarityMatrix = matrix;
    }

    public void SaveTopics(String topicFile){
        JSONObject root = new JSONObject();
        JSONArray topics = new JSONArray();
        metadata.put("nTopics", nTopics);
        metadata.put("nWords", nWords);
        metadata.put("nDocs", nDocs);
        root.put("metadata", metadata);
        for(Map.Entry<String, ModelJSONTopic> entry: Topics.entrySet()){
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
        JSONIOWrapper.SaveJSON(root, topicFile);
    }

    public void SaveDocuments(String documentFile){
        JSONObject root = new JSONObject();
        JSONArray documents = new JSONArray();
        metadata.put("nTopics", nTopics);
        root.put("metadata", metadata);
        for(Map.Entry<String, ModelJSONDocument> entry: Documents.entrySet()){
            documents.add(entry.getValue().toJSON());
        }
        root.put("documents", documents);
        JSONIOWrapper.SaveJSON(root, documentFile);
    }
}
