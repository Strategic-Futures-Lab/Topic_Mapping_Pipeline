package P3_TopicModelling.Similarity;

import PX_Data.TopicIOWrapper;
import PY_Helper.LogPrint;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.List;

/**
 * Class computing the perceptual similarity between topics, ie, based on top words overlap.
 * 
 * @author A. Gharavi, P. Le Bras
 * @version 1
 * @deprecated Incorporated in {@link TopicsSimilarity}
 */
@Deprecated
public class PerceptualTopicsSimilarity {

    /*
     * Similarity matrix based on the perceptual measure based on topics topwords and weights
     * */
    public static double[][] GetSimilarityMatrixPerceptual(int sizeX, List<List<TopicIOWrapper.JSONTopicWeight>> topicModelX,
                                                           int sizeY, List<List<TopicIOWrapper.JSONTopicWeight>> topicModelY, String metric){

        LogPrint.printNewStep("Calculating similarity matrix " + metric + " " +sizeX+"x"+sizeY, 0);
        DecimalFormat df = new DecimalFormat("#.####");
        df.setRoundingMode(RoundingMode.UP);

        double[][] matrix = new double[sizeX][sizeY];

        //get the topic labels and weights
        for(int yTopic = 0; yTopic < sizeY; yTopic++){
            for(int xTopic = 0; xTopic < sizeX; xTopic++){
                try{
                    switch (metric){
                        case "Perceptual":
                            matrix[xTopic][yTopic]=  1- sumOfDotProductsWithPenalty(getNormalizedVector(topicModelY.get(yTopic)),
                                    getNormalizedVector(topicModelX.get(xTopic)))/sizeX;
                            break;
                        case "L1Norm":
                            matrix[xTopic][yTopic]=  l1Norm(getNormalizedVector(topicModelY.get(yTopic)),
                                    getNormalizedVector(topicModelX.get(xTopic)));
                            break;
                    }
                }catch (Exception e){
                    LogPrint.printNoteError("Error while calculating the topic to topic Perceptual distance\n");
                    System.out.println(e.getStackTrace());
                    System.exit(1);
                }

            }
        }

        LogPrint.printCompleteStep();
        return matrix;
    }





    /**based on number of overlapping words
     * positive reward for shared top words + negative reward for unique words
     * + --> dot product of the weights
     * - --> -1 for each non-overlapping word
     **/
    private static double sumOfDotProductsWithPenalty(List<TopicIOWrapper.JSONTopicWeight> vectorA, List<TopicIOWrapper.JSONTopicWeight> vectorB){
        double sim = 0.00;
        String labelToCheckA, labelToCheckB;
        int noShared = 0;

        for (TopicIOWrapper.JSONTopicWeight jsonTopicWeight : vectorA) {
            labelToCheckA = jsonTopicWeight.ID;

            for (TopicIOWrapper.JSONTopicWeight topicWeight : vectorB) {
                labelToCheckB = topicWeight.ID;

                if (labelToCheckB.equalsIgnoreCase(labelToCheckA)) {
                    noShared += 1;
                    sim += jsonTopicWeight.weight * topicWeight.weight;
                }
            }
        }
        sim -= vectorA.size() - noShared;

        return Math.abs(sim);
    }




    /**
     * L1 Norm difference between weights of top words
     */
    private static double l1Norm(List<TopicIOWrapper.JSONTopicWeight> vectorA, List<TopicIOWrapper.JSONTopicWeight> vectorB){
        double dist = 0.0;
        String labelToCheckA, labelToCheckB;
        for (TopicIOWrapper.JSONTopicWeight jsonTopicWeight : vectorA) {
            labelToCheckA = jsonTopicWeight.ID;

            for (TopicIOWrapper.JSONTopicWeight topicWeight : vectorB) {
                labelToCheckB = topicWeight.ID;

                if (labelToCheckB.equalsIgnoreCase(labelToCheckA)) {
                    dist = dist + Math.abs(jsonTopicWeight.weight - topicWeight.weight);

                } else if (!labelToCheckB.equalsIgnoreCase(labelToCheckA)) {
                    dist += jsonTopicWeight.weight + topicWeight.weight;
                }
            }
        }
        return dist;
    }

    //normalize a vector topics words/weights
    private static List<TopicIOWrapper.JSONTopicWeight> getNormalizedVector(List<TopicIOWrapper.JSONTopicWeight> vectorA){
        double sum = 0.0;

        for (TopicIOWrapper.JSONTopicWeight wA: vectorA)
            sum += wA.weight;

        for (TopicIOWrapper.JSONTopicWeight wA: vectorA)
            wA.weight = wA.weight/sum;

        return vectorA;
    }

}
