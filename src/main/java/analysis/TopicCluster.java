package analysis;

import IO.Console;
import IO.Timer;
import analytics.clustering.Agglomerative;
import analytics.clustering.Agglomerative.LinkageType;
import data.LinkageTable;
import data.SimilarityMatrix;
import org.json.simple.parser.ParseException;
import pipeline.config.ModuleConfig;
import pipeline.config.modules.TopicClusterConfig;

import java.io.IOException;
import java.util.ArrayList;
import java.util.IllegalFormatException;
import java.util.List;

/**
 * Class for Topic Clustering module
 *
 * @author P. Le Bras
 * @version 1
 */
public class TopicCluster extends AnalysisModule {

    // module parameters
    private TopicClusterConfig config;

    private TopicCluster(TopicClusterConfig c){
        config = c;
        c.logConfig();
    }

    private SimilarityMatrix similarityMatrix;

    private List<LinkageTable> clusters;

    /**
     * Main module method - processes parameters, loads similarities, calculates and write clusters
     * @param moduleConfig module parameters
     * @throws Exception If the similarities don't load properly or clustering runs into an error
     */
    public static void run(ModuleConfig moduleConfig) throws Exception {
        String MODULE_NAME = moduleConfig.moduleName+" ("+moduleConfig.moduleType+")";
        Console.moduleStart(MODULE_NAME);
        Timer.start(MODULE_NAME);
        TopicCluster instance = new TopicCluster((TopicClusterConfig) moduleConfig);
        try{
            instance.loadSimilarities();
            instance.calculateClusters();
            instance.writeClusters();
        } catch (Exception e){
            Console.moduleFail(MODULE_NAME);
            throw e;
        }
        Console.moduleComplete(MODULE_NAME);
        Timer.stop(MODULE_NAME);
    }

    private void loadSimilarities() throws IOException, ParseException, IllegalArgumentException {
        similarityMatrix = new SimilarityMatrix(config.similaritiesFile);
        if(!similarityMatrix.isSymmetric()){
            throw new IllegalArgumentException("Similarity matrix must be symmetric");
        }
    }

    private void calculateClusters(){
        clusters = new ArrayList<>();
        LinkageType l = switch (config.linkage){
            case "upgma" -> LinkageType.UPGMA;
            case "min" -> LinkageType.MIN;
            case "max" -> LinkageType.MAX;
            default -> {
                Console.error("Unrecognised linkage type "+config.linkage+". Must be: min, max or upgma");
                Console.info("Will default to upgma");
                yield LinkageType.UPGMA;
            }
        };
        if(config.groupsFile == null){
            // clustering only one set of topics
            double[][] distances = similarityMatrix.getDistanceMatrix();
            String[] items = similarityMatrix.getItems().toArray(new String[similarityMatrix.size()]);
            int n;
            if(config.nClusters < 1){
                n = 1;
                Console.error("There must be at least 1 cluster");
                Console.info("Will default number of clusters to 1");
            } else if(config.nClusters > similarityMatrix.size()){
                n = similarityMatrix.size();
                Console.error("There must be at most "+n+" clusters (number of items in the similarity matrix)");
                Console.info("Will default number of clusters to "+n);
            } else {
                n = config.nClusters;
            }
            clusters.add(Agglomerative.cluster(distances, items, n, l));
        } else {
            // todo: cluster groups of topics in assignment
        }
    }

    private void writeClusters() throws IOException{
        if(clusters.size() == 1){
            clusters.get(0).writeClusters(config.outputFile);
        } else {
            // todo: save cluster groups
        }
    }


}
