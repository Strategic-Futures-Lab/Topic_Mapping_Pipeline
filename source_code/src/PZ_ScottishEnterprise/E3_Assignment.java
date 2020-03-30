package PZ_ScottishEnterprise;

import de.siegmar.fastcsv.reader.CsvParser;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.writer.CsvAppender;
import de.siegmar.fastcsv.writer.CsvWriter;
import de.siegmar.fastcsv.reader.CsvRow;

import java.io.File;
import java.io.IOException;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.*;


/**
 * Created by Azimeh on 22/06/2019.
 * It takes 2 sets of csv files which each include topic-document distribution of topic models
 * the idea is to have a SUPER MODEL with small number of topics and a SUB MODEL with large number of topics
 * the algorithm will:
 *  1. Calculate the Cosine similarity of each SUPER TOPIC to each SUB TOPIC --> at this stage will have a similarity matrix between SUPER MODEL and SUB MODEL
 *  2. Use the MUNKRES algorithm to assign each SUB TOPIC to exactly one SUPER TOPIC
 */
public class E3_Assignment
{
    private String superModelFilename, subModelFilename, outputFilename;
    private double[][] simMatrix;
    private int superSize = 60;
    private int subSize = 1000;
    private int maxDocuments = 0;

    List<String> superTopicLabels;
    List<String> subTopicLabels;

    Double [][] superMatrix;
    Double [][] subMatrix;

    //this can beused to extract the subgroups -- to be used with the python part
    ArrayList < ArrayList<Integer>> subs = new ArrayList<>();

    /**
     * This class is created for ScottishEnterprise project
     *
     * @param args - [CSV superModelInput Location] [CSV subModelInput Location] [JSON Output Location] [SuperSize] [SubSize]
     */
    public static void main (String[] args) throws IOException {
        System.out.println( "\n********************************************\n" +
                "*                                                        *\n" +
                "* STARTING E3_Assignment PHASE!                          *\n" +
                "*                                                        *\n" +
                "* E3_Assignment:START                                    *\n" +
                "*                                                        * \n" +
                "**********************************************************\n");

        E3_Assignment startClass = new E3_Assignment();
        startClass.CreateAssignment(args);

        System.out.println( "\n********************************************\n" +
                "*                                                        *\n" +
                "* E3_Assignment PHASE COMPLETE!                          *\n" +
                "*                                                        *\n" +
                "* E3_Assignment:END                                      *\n" +
                "*                                                        * \n" +
                "*********************************************************\n");
    }

    /**
     * The main class that sets off the JSON creation process. We don't do anything special here, as everything
     * is outsourced to it's own method.
     *
     * @param args - The argument list directly from main()
     */
    public void CreateAssignment(String[] args) throws IOException {
        //Round any answers to 15 decimal points as we get floating point errors on the 16th bit (i.e. 1.0000000000000002 instead of 1)
        DecimalFormat df = new DecimalFormat("#.##############");
        df.setRoundingMode(RoundingMode.HALF_UP);
        checkArgs(args);
        LoadFiles(superModelFilename, subModelFilename );
        calculateTopicToTopicCosineDistances(outputFilename, df);
        assignMostSimilar(outputFilename, simMatrix);

    }

    /**
     * Here we ensure we have all the arguments we require to create the JSON file.
     *
     * @param args - The argument list directly from main()
     */
    private void checkArgs(String[] args)
    {
        if(args.length < 2)
        {
            System.out.println("\nArguments missing!\n\nPlease supply arguments in the following order: [CSV superModelInput Location] [CSV subModelInput Location] [JSON Output Location] [JSON Output Location] [SuperSize] [SubSize]");
            System.exit(1);
        }
        else
        {
            superModelFilename = args[0];
            subModelFilename = args[1];
            outputFilename = args[2];
            superSize = Integer.parseInt(args[3]);
            subSize = Integer.parseInt(args[4]);
        }
    }

    /**
     * Load in the 2 input files.
     * @param superFile, subFile
     */
    private void LoadFiles(String superFile, String subFile) throws IOException {

        //Parse superModel csv and save document distributions
        File superF = new File(superFile);
        CsvReader superCSVReader = new CsvReader();
        superCSVReader.setContainsHeader(true);

        File subF = new File(subFile);
        CsvReader subCSVReader = new CsvReader();
        subCSVReader.setContainsHeader(true);

        int rowNum = 0;

        //count the number of documents (or the rows in csv files)
        //get a list of super topic labels
        try (CsvParser superParser = superCSVReader.parse(superF, StandardCharsets.UTF_8))
        {
            while (superParser.nextRow() != null) { ++maxDocuments; }

            superParser.nextRow();
            superTopicLabels = superParser.getHeader();
            superMatrix = new Double[superTopicLabels.size()-3][maxDocuments];
        }
        try (CsvParser subParser = subCSVReader.parse(subF, StandardCharsets.UTF_8))
        {
            subParser.nextRow();
            subTopicLabels = subParser.getHeader();
            subMatrix = new Double[subTopicLabels.size()-3][maxDocuments];

        }

        //Store document distribution for superModel to superMatrix;
        try (CsvParser superParser = superCSVReader.parse(superF, StandardCharsets.UTF_8))
        {
            CsvRow superRow;
            while ((superRow = superParser.nextRow()) != null && rowNum <maxDocuments )
            {
                for(int i = 0; i < superTopicLabels.size()-3; i++){
                    if(superRow.getField(i+3).length() > 0)
                        superMatrix[i][rowNum] = Double.parseDouble(superRow.getField(i+3));
                    else
                        superMatrix[i][rowNum] = 0.00;
                }
                rowNum++;
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }


        //Store document distribution for subModel to subMatrix;
        try (CsvParser subParser = subCSVReader.parse(subF, StandardCharsets.UTF_8))
        {
            rowNum =0;
            CsvRow subRow;
            while ((subRow = subParser.nextRow()) != null && rowNum < maxDocuments)
            {
                for(int i = 0; i < subTopicLabels.size()-3; i++){
                    if(subRow.getField(i+3).length() > 0)
                        subMatrix[i][rowNum] = Double.parseDouble(subRow.getField(i+3));
                    else
                        subMatrix[i][rowNum] = 0.00;
                }
                rowNum++;
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Here we calculate the topic to topic distances, and output them as a similarity matrix to a CSV file. This was
     * to allow people to put the similarity data easily into the Well Sorted dev tools and estimate a good number of
     * clusters for the hex layout later in the pipeline. We use cosine distance to calculate the distance values!
     *
     * @param outputFilename - The file path where we want to save the distances! .json will be replaced with .csv
     * @param df             - A DecimalFormat with < 16 decimal points, in order to help with floating point issues on the 16th bit
     */
    private void calculateTopicToTopicCosineDistances(String outputFilename, DecimalFormat df) {
        System.out.println("\n**********\nBeginning topic to topic cosine distance calculation!\n**********\n");

        //To store topic-topic cosine similarity matrix for super and sub models
        simMatrix = new double[subSize][superSize];

        for (int xTopic = 0; xTopic < subSize; xTopic++) {
            for (int yTopic = 0; yTopic < superSize; yTopic++) {
                try{
                    simMatrix[xTopic][yTopic] = Double.parseDouble(df.format(cosineSimilarity(subMatrix[xTopic], superMatrix[yTopic])));

                } catch (Exception e){
                    System.out.println("Error while calculating the topic to topic distance "  );
                    e.printStackTrace();
                    System.exit(1);
                }
            }
        }
        saveCosineDistanceCSV(outputFilename, simMatrix);
        System.out.println("\n**********\nTopic to topic cosine distance calculation complete!\n**********\n");
    }

    /**
     * Here is where we actually calculate the cosine similarity/distance between two vectors. This code is a pretty
     * standard way of calculating it, which you can look up online!
     *
     * @param vectorA - the first vector
     * @param vectorB - the second vector
     * @return the similarity/distance between the two vectors
     */
    private double cosineSimilarity(Double[] vectorA, Double[] vectorB) {
       // System.out.println(vectorA.length + " " + vectorB.length);
        double dotProduct = 0.0;
        // Updated by P. Le Bras
        // non-zero initialisation in case of a zero vector
        double normA = 0.00000000000001;
        double normB = 0.00000000000001;
        for (int i = 0; i < vectorA.length-1; i++) {
           // System.out.println(vectorA[i] + " " + vectorB[i]);
            dotProduct += vectorA[i] * vectorB[i];
            normA += Math.pow(vectorA[i], 2);
            normB += Math.pow(vectorB[i], 2);
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * For each subTopic find the most similar superTopic
     *
     * * @param outputFilename - The file path where we want to save the distances! .json will be replaced with .csv
     * * @param simMatrix      - the matrix with the cosine distances in, to save to a file.
     * **/

    private void assignMostSimilar(String outputFilename, double[][] simMatrix) {
        String newFilename1, newFilename2;
        String[][] assignment = new String[simMatrix.length][simMatrix[0].length];
        HashMap<String,ArrayList<String>> superSubGroups = new HashMap<>();

        if (outputFilename.contains(".json")){
            newFilename1 = outputFilename.replace(".json", "1To1Assignment.csv");
            newFilename2 = outputFilename.replace(".json", "AssignmentGroups.csv");
        }
        else if (outputFilename.contains(".JSON")) {
            newFilename1 = outputFilename.replace(".JSON", "1To1Assignment.csv");
            newFilename2 = outputFilename.replace(".JSON", "AssignmentGroups.csv");
        }
        else {
            newFilename1 = "1To1Assignment.csv";
            newFilename2 = "AssignmentGroups.csv";
        }

        //Calcualtes the 1 to 1 assignment between super and sub topics
        for (int y = 0; y < simMatrix.length; y++) {
            double[] currentRow = simMatrix[y];

            double currentMax = 0.00;
            int currentMaxIndex= 0;
            for (int x = 0; x < currentRow.length; x++){
                if(currentRow[x] > currentMax) {
                    currentMax = currentRow[x];
                    currentMaxIndex = x;
                }
            }

            assignment[y][0] = subTopicLabels.get(y+3);
            assignment[y][1] = superTopicLabels.get(currentMaxIndex+3);

        }

        //Group subTopics based on superTopics
        for(int i = 0; i < superSize; i++){
            ArrayList<String> tempList = new ArrayList<>();
            for(int j =0; j < subSize; j++){
                if(assignment[j][1].equalsIgnoreCase(superTopicLabels.get(i+3))){
                    tempList.add(assignment[j][0]);
                }
            }
            superSubGroups.put(superTopicLabels.get(i+3), tempList);
        }

        /*
        //Save a 2 column csv where column 0 is the sub topic and column 1 is the super topic - This file is not really needed it is used just for checking
        File file = new File(newFilename1);
        CsvWriter csvWriter = new CsvWriter();
        csvWriter.setAlwaysDelimitText(true);

        try (CsvAppender csvAppender = csvWriter.append(file, StandardCharsets.UTF_8)) {
            for(String[] g : assignment){
                csvAppender.appendField(g[0]);
                csvAppender.appendField(g[1]);
                csvAppender.endLine();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        */

        //Save second csv file where column 0 is the super topic and column 1, column 2, ... are the sub topics
        File file2 = new File(newFilename2);
        CsvWriter csvWriter2 = new CsvWriter();
        csvWriter2.setAlwaysDelimitText(true);
        try (CsvAppender csvAppender = csvWriter2.append(file2, StandardCharsets.UTF_8)) {

            for(int i = 0; i < superSize; i++){
                ArrayList<String> t = superSubGroups.get(superTopicLabels.get(i+3));
                ArrayList<Integer> s = new ArrayList<>();
                csvAppender.appendField(superTopicLabels.get(i+3));
                //size of subgroups
                csvAppender.appendField(String.valueOf(t.size()));

                for(String sub: t){
                    for(int j= 0;  j< subSize; j++ ){
                        if( sub.equalsIgnoreCase(String.valueOf(subTopicLabels.get(j+3)))){
                            // csvAppender.appendField(String.valueOf(j));
                            s.add(j);
                            csvAppender.appendField(sub);
                        }
                    }
                }
                csvAppender.appendLine();
                subs.add(s);
            }


        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    /**
     * This is where we actually save the cosine distances to a CSV file. In short, this is where we construct the lines
     * and output them to a file. The similarity/distance matrix should already be created by this point.
     *
     * @param outputFilename - The file path where we want to save the distances! .json will be replaced with .csv
     * @param simMatrix      - the matrix with the cosine distances in, to save to a file.
     *
     */

    private void saveCosineDistanceCSV(String outputFilename, double[][] simMatrix) {
        String newFilename;

        if (outputFilename.contains(".json"))
            newFilename = outputFilename.replace(".json", "T2TCosineDistances.csv");
        else if (outputFilename.contains(".JSON"))
            newFilename = outputFilename.replace(".JSON", "T2TCosineDistances.csv");
        else if (outputFilename.contains(".csv"))
            newFilename = outputFilename.replace(".csv", "T2TCosineDistances.csv");
        else
            newFilename = "TopicToTopicCosineDistances.csv";

        File file = new File(newFilename);
        CsvWriter csvWriter = new CsvWriter();
        csvWriter.setAlwaysDelimitText(true);

        try (CsvAppender csvAppender = csvWriter.append(file, StandardCharsets.UTF_8)) {
            String[] superLabels = new String[superSize];
            String[] subLabels = new String[subSize];
            csvAppender.appendField("");
            //Create the labels...
            for (int superTopic = 0; superTopic < superSize; superTopic++) {
                superLabels[superTopic] = superTopicLabels.get(superTopic+3);
                csvAppender.appendField(superLabels[superTopic]);
            }

            csvAppender.endLine();

            for (int y = 0; y < simMatrix.length; y++) {
                 subLabels[y] = subTopicLabels.get(y+3);
                 csvAppender.appendField(subLabels[y]);

                for (int x = 0; x < simMatrix[0].length; x++)
                    csvAppender.appendField(String.valueOf(simMatrix[y][x]));

                csvAppender.endLine();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
