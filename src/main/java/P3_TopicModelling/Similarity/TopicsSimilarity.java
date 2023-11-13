package P3_TopicModelling.Similarity;

import P3_TopicModelling.TopicModelCore.ModelledDocument;
import P3_TopicModelling.TopicModelCore.ModelledTopic;
import PY_Helper.LogPrint;
import PY_Helper.Pair;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Class implementing several similarity measures between sets of topics.<br>
 * - DocumentCosineSimilarity considers the amplitude of topics in document space;<br>
 * - PerceptualSimilarity implements a reward-penalty system of label overlap in topics;<br>
 * - LabelsL1NormSimilarity uses the manhattan distance between the topics' label vectors.
 *
 * @author S. Padilla, T. Methven, P. Le Bras, A. Gharavi
 * @version 3
 */
public class TopicsSimilarity {

    /**
     * Method calculating the cosine similarity of topics in document space.
     * Uses documents with their topic distribution data as inputs.
     * @param documents Set of documents.
     * @return The document cosine similarity matrix.
     */
    public static double[][] DocumentCosineSimilarity(List<ModelledDocument> documents){
        return DocumentCosineSimilarity(documents, documents);
    }

    /**
     * Method calculating the cosine similarity of topics in document space.
     * Uses documents with their topic distribution data as inputs.
     * @param documentsX First set of documents.
     * @param documentsY Second set of documents.
     * @return The document cosine similarity matrix.
     */
    public static double[][] DocumentCosineSimilarity(List<ModelledDocument> documentsX, List<ModelledDocument> documentsY){
        int sizeX = documentsX.get(0).nTopics;
        int sizeY = documentsY.get(0).nTopics;
        LogPrint.printNewStep("Calculating document cosine similarity matrix of size: "+sizeX+"x"+sizeY, 0);
        DecimalFormat df = new DecimalFormat("#.####");
        df.setRoundingMode(RoundingMode.UP);
        double[][] matrix = new double[sizeX][sizeY];
        double[][] topicVectorX = getTransposedMatrix(documentsX, 0.1f);
        double[][] topicVectorY = getTransposedMatrix(documentsY, 0.1f);
        for(int yTopic = 0; yTopic < sizeY; yTopic++){
            for(int xTopic = 0; xTopic < sizeX; xTopic++){
                try{
                    matrix[xTopic][yTopic] = Double.parseDouble(df.format(cosineSimilarity(topicVectorX[xTopic], topicVectorY[yTopic])));
                } catch (Exception e){
                    LogPrint.printNoteError("Error while calculating the topic to topic document cosine similarity\n");
                    System.out.println(e.getStackTrace());
                    System.exit(1);
                }
            }
        }
        LogPrint.printCompleteStep();
        return matrix;
    }

    /**
     * Method transposing a document-topic matrix to a topic-document matrix.
     * @param documents List of documents, containing topic distributions.
     * @param threshold Threshold value to filter-out noise (low weights).
     * @return The transposed matrix.
     */
    private static double[][] getTransposedMatrix(List<ModelledDocument> documents, float threshold){
        double[][] transposedMatrix = new double[documents.get(0).nTopics][documents.size()];
        for (int document = 0; document < transposedMatrix[0].length; document++) {
            for (int topic = 0; topic < transposedMatrix.length; topic++) {
                transposedMatrix[topic][document] = documents.get(document).topicDistribution[topic];
                //Threshold the values. If it is lower than a certain amount, then set it to 0 so it is ignored in the cosine calculations
                if (transposedMatrix[topic][document] < threshold)
                    transposedMatrix[topic][document] = 0;
            }
        }
        return transposedMatrix;
    }

    /**
     * Method calculating the cosine similarity between two vectors of identical length.
     * @param vectorA First vector.
     * @param vectorB Second vector.
     * @return The cosine similarity.
     */
    private static double cosineSimilarity(double[] vectorA, double[] vectorB) {
        double dotProduct = 0.0;
        // non-zero initialisation in case of a zero vector
        double normA = 0.00000000000001;
        double normB = 0.00000000000001;
        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += Math.pow(vectorA[i], 2);
            normB += Math.pow(vectorB[i], 2);
        }
        return Math.min(dotProduct / (Math.sqrt(normA) * Math.sqrt(normB)), 1.0);
    }

    /**
     * Method calculating the perceptual (ie, based on words overlap) similarity matrix between two sets of topics.
     * @param topicsX First set of topics (each topic is a list of lemma-weight pairs).
     * @param topicsY Second set of topics (each topic is a list of lemma-weight pairs).
     * @return The perceptual similarity matrix.
     */
    public static double[][] PerceptualSimilarity(List<ModelledTopic> topicsX,
                                                  List<ModelledTopic> topicsY){
        int sizeX = topicsX.size();
        int sizeY = topicsY.size();
        LogPrint.printNewStep("Calculating perceptual similarity matrix of size: " +sizeX+"x"+sizeY, 0);
        DecimalFormat df = new DecimalFormat("#.####");
        df.setRoundingMode(RoundingMode.UP);
        double[][] matrix = new double[sizeX][sizeY];
        for(int y = 0; y < sizeY; y++){
            for(int x = 0; x < sizeX; x++){
                try {
                    matrix[x][y] = Double.parseDouble(df.format(1 - perceptualDistance(normaliseLabelVector(topicsY.get(y)), normaliseLabelVector(topicsX.get(x)))));
                } catch (Exception e){
                    LogPrint.printNoteError("Error while calculating the topic to topic perceptual similarity\n");
                    System.out.println(e.getStackTrace());
                    System.exit(1);
                }
            }
        }
        LogPrint.printCompleteStep();
        return matrix;
    }

    /**
     * Method calculating a similarity score between two label vectors based on overlapping labels.
     * Reward for shared labels (product of weights), penalty for unique labels.
     * @param labelVectorA First label vector.
     * @param labelVectorB Second label vector.
     * @return The similarity score between the two vectors.
     **/
    private static double perceptualDistance(List<Pair<String,Double>> labelVectorA, List<Pair<String,Double>> labelVectorB){
        double sim = 0.00;
        int nShared = 0;
        for (Pair<String,Double> labelA: labelVectorA) {
            for (Pair<String,Double> labelB: labelVectorB) {
                if (labelB.getLeft().equalsIgnoreCase(labelA.getLeft())) {
                    nShared += 1;
                    sim += labelA.getRight() * labelB.getRight();
                }
            }
        }
        // get what would have been the length of both vectors
        // smaller vector implies tail of labels with weight 0.0
        int size = Math.max(labelVectorA.size(),labelVectorB.size());
        sim -= size - nShared;
        return Math.abs(sim)/size;
    }

    /**
     * Method calculating the L1-norm (manhattan distance) similarity matrix between two sets of topics.
     * @param topicsX First set of topics (each topic is a list of lemma-weight pairs).
     * @param topicsY Second set of topics (each topic is a list of lemma-weight pairs).
     * @return The L1-norm similarity matrix.
     */
    public static double[][] LabelsL1NormSimilarity(List<ModelledTopic> topicsX,
                                                    List<ModelledTopic> topicsY){
        int sizeX = topicsX.size();
        int sizeY = topicsY.size();
        LogPrint.printNewStep("Calculating label L1-norm similarity matrix of size: " +sizeX+"x"+sizeY, 0);
        DecimalFormat df = new DecimalFormat("#.####");
        df.setRoundingMode(RoundingMode.UP);
        double[][] matrix = new double[sizeX][sizeY];
        for(int y = 0; y < sizeY; y++){
            for(int x = 0; x < sizeX; x++){
                try {
                    matrix[x][y] = Double.parseDouble(df.format(1 - labelL1NormDistance(normaliseLabelVector(topicsY.get(y)), normaliseLabelVector(topicsX.get(x)))));
                } catch (Exception e){
                    LogPrint.printNoteError("Error while calculating the topic to topic label L1-norm similarity\n");
                    System.out.println(e.getStackTrace());
                    System.exit(1);
                }
            }
        }
        LogPrint.printCompleteStep();
        return matrix;
    }

    /**
     * Method computing the L1-norm (manhattan distance) between two label vectors.
     * @param labelVectorA First label vector.
     * @param labelVectorB Second label vector.
     * @return L1-norm distance between the two vectors.
     */
    private static double labelL1NormDistance(List<Pair<String,Double>> labelVectorA, List<Pair<String,Double>> labelVectorB){
        double dist = 0.0;
        List<String> commonLabels = new ArrayList<>();
        for(Pair<String,Double> labelA: labelVectorA){
            boolean unmatchedLabel = true;
            for(Pair<String,Double> labelB: labelVectorB){
                if(labelB.getLeft().equalsIgnoreCase(labelA.getLeft())){
                    // same labels in both vector
                    // adding the difference of weights
                    unmatchedLabel = false;
                    commonLabels.add(labelB.getLeft().toLowerCase());
                    dist += Math.abs(labelA.getRight()-labelB.getRight());
                    break;
                }
            }
            // adding unmatched weights from vector a
            if(unmatchedLabel){
                dist += labelA.getRight();
            }
        }
        // adding unmatched weights from vector b
        for(Pair<String,Double> labelB: labelVectorB){
            if(!commonLabels.contains(labelB.getLeft().toLowerCase())){
                dist += labelB.getRight();
            }
        }
        return dist;
    }


    /**
     * Method normalising the weights of labels.
     * @param labelVector Vector of labels to normalise.
     * @return The normalised vector.
     */
    private static List<Pair<String,Double>> normaliseLabelVector(List<Pair<String,Double>> labelVector){
        double sum = 0.0;
        for (Pair<String,Double> label: labelVector) {
            sum += label.getRight();
        }
        for (Pair<String,Double> label: labelVector) {
            label.setRight(label.getRight()/sum);
        }
        return labelVector;
    }

    /**
     * Method normalising the weights of labels from a topic.
     * @param topic Topic with list of labels to normalise.
     * @return The normalised vector.
     */
    private static List<Pair<String,Double>> normaliseLabelVector(ModelledTopic topic){
        return normaliseLabelVector(topic.getLabels());
    }
}
