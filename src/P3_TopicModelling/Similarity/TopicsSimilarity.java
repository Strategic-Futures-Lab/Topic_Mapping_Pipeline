package P3_TopicModelling.Similarity;

import P3_TopicModelling.TopicModelCore.TopicData;
import de.siegmar.fastcsv.writer.CsvAppender;
import de.siegmar.fastcsv.writer.CsvWriter;

import java.io.File;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.List;

public class TopicsSimilarity {

    public static double[][] GetSimilarityMatrix(int size, List<TopicData> topicModel){
        return GetSimilarityMatrix(size, topicModel, size, topicModel);
    }

    public static double[][] GetSimilarityMatrix(int sizeX, List<TopicData> topicModelX, int sizeY, List<TopicData> topicModelY){
        System.out.println("Calculating Similarity Matrix "+sizeX+"x"+sizeY+" ...");
        DecimalFormat df = new DecimalFormat("#.####");
        df.setRoundingMode(RoundingMode.UP);

        double[][] matrix = new double[sizeX][sizeY];

        double[][] topicVectorX = getTransposedMatrix(sizeX, topicModelX, 0.1f);
        double[][] topicVectorY = getTransposedMatrix(sizeY, topicModelY, 0.1f);

        for(int yTopic = 0; yTopic < sizeY; yTopic++){
            for(int xTopic = 0; xTopic < sizeX; xTopic++){
                try{
                    matrix[xTopic][yTopic] = Double.parseDouble(df.format(cosineSimilarity(topicVectorX[xTopic], topicVectorY[yTopic])));
                } catch (Exception e){
                    System.out.println("Error while calculating the topic to topic distance.");
                    System.exit(1);
                }
            }
        }
        System.out.println("Similarity Matrix Calculated!");
        return matrix;
    }

    private static double[][] getTransposedMatrix(int size, List<TopicData> topicModel, float threshold){
        double[][] transposedMatrix = new double[size][topicModel.size()];

        for (int document = 0; document < transposedMatrix[0].length; document++) {
            for (int topic = 0; topic < transposedMatrix.length; topic++) {
                transposedMatrix[topic][document] = topicModel.get(document).topicDistributions[topic];

                //Threshold the values. If it is lower than a certain amount, then set it to 0 so it is ignored in the cosine calculations
                if (transposedMatrix[topic][document] < threshold)
                    transposedMatrix[topic][document] = 0;
            }
        }

        return transposedMatrix;
    }

    private static double cosineSimilarity(double[] vectorA, double[] vectorB) {
        double dotProduct = 0.0;
        // Updated by P. Le Bras
        // non-zero initialisation in case of a zero vector
        double normA = 0.00000000000001;
        double normB = 0.00000000000001;
        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += Math.pow(vectorA[i], 2);
            normB += Math.pow(vectorB[i], 2);
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
