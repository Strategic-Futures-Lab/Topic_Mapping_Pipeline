package analysis;

import IO.Console;
import IO.JSONHelper;
import IO.Timer;
import analytics.Similarities;
import data.Model;
import data.SimilarityMatrix;
import data.SparseVector;
import data.Topic;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import pipeline.config.ModuleConfig;
import pipeline.config.modules.TopicSimilarityConfig;

import java.io.IOException;
import java.lang.reflect.Array;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;

/**
 * Class for Topic Similarity module
 *
 * @author P. Le Bras
 * @version 1
 */
public class TopicSimilarity extends AnalysisModule {

    // module parameters
    private TopicSimilarityConfig config;

    private TopicSimilarity(TopicSimilarityConfig c){
        config = c;
        config.logConfig();
    }

    private List<Model> models;

    private SimilarityMatrix similarityMatrix;

    /**
     * Main module method - processes parameters, loads topics, calculates and write similarity matrix
     * @param moduleConfig module parameters
     * @throws Exception If the corpus cannot load properly
     */
    public static void run(ModuleConfig moduleConfig) throws Exception {
        String MODULE_NAME = moduleConfig.moduleName+" ("+moduleConfig.moduleType+")";
        Console.moduleStart(MODULE_NAME);
        Timer.start(MODULE_NAME);
        TopicSimilarity instance = new TopicSimilarity((TopicSimilarityConfig) moduleConfig);
        try{
            instance.loadTopics();
            instance.calculateSimilarities();
            instance.writeSimilarities();
        } catch (Exception e){
            Console.moduleFail(MODULE_NAME);
            throw e;
        }
        Console.moduleComplete(MODULE_NAME);
        Timer.stop(MODULE_NAME);
    }

    private void loadTopics() throws IOException, ParseException {
        models = new ArrayList<>(2);
        for(String topicFile: config.topicFiles){
            models.add(new Model(topicFile));
        }
    }

    private void calculateSimilarities(){
        if(models.size() == 1){
            List<Topic> topicSet = models.get(0).topics;
            similarityMatrix = new SimilarityMatrix(topicSet.size());
            for(Topic t: topicSet) similarityMatrix.addItem(t.getTopicId());
            for(int i=0; i<topicSet.size(); i++){
                for(int j=i+1; j<topicSet.size(); j++){
                    similarityMatrix.addSimilarity(j,i,getSimilarity(topicSet.get(j),topicSet.get(i)));
                }
            }
        } else {
            List<Topic> topicSet1 = models.get(0).topics;
            List<Topic> topicSet2 = models.get(1).topics;
            similarityMatrix = new SimilarityMatrix(topicSet1.size(),topicSet2.size());
            for(Topic t: topicSet1) similarityMatrix.addRowItem(t.getTopicId());
            for(Topic t: topicSet2) similarityMatrix.addColumnItem(t.getTopicId());
            for(int i=0; i<topicSet2.size(); i++){
                for(int j=0; j<topicSet1.size(); j++){
                    similarityMatrix.addSimilarity(j,i,getSimilarity(topicSet1.get(j),topicSet2.get(i)));
                }
            }
        }
    }

    private double getSimilarity(Topic topicA, Topic topicB){
        return switch (config.similarity) {
            case "cosine" -> Similarities.CosineSimilarity(getFeatureVector(topicA), getFeatureVector(topicB));
            case "hellinger" -> Similarities.HellingerSimilarity(getFeatureVector(topicA), getFeatureVector(topicB));
            case "jaccard" -> Similarities.JaccardSimilarity(getFeatureList(topicA), getFeatureList(topicB));
            case "average_jaccard" -> Similarities.AverageJaccardSimilarity(getFeatureList(topicA), getFeatureList(topicB));
            default -> throw new IllegalArgumentException("Invalid similarity method: " + config.similarity);
        };
    }

    private SparseVector getFeatureVector(Topic t){
        if(config.useDocuments) return t.getDocumentDistribution();
        else return t.getWordDistribution();
    }

    private List<String> getFeatureList(Topic t){
        if(config.useDocuments) return t.getDocuments();
        else return t.getWords();
    }

    private void writeSimilarities() throws IOException {
        Console.log("Saving similarity matrix");
        try{
            similarityMatrix.writeSimilarities(config.outputFile);
        } catch (IOException e){
            Console.error("Saving similarity matrix failed");
            throw e;
        }
    }
}

