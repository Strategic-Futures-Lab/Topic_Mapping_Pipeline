package P3_TopicModelling;

import PX_Helper.JSONIOWrapper;
import PX_Helper.DocumentRow;
import PY_TopicModelCore.TopicData;
import PY_TopicModelCore.TopicModel;
import PY_TopicModelCore.TopicRowContainer;
import PY_TopicModelCore.WordData;
import cc.mallet.types.Alphabet;
import cc.mallet.types.IDSorter;
import de.siegmar.fastcsv.writer.CsvAppender;
import de.siegmar.fastcsv.writer.CsvWriter;

import java.io.File;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Tom on 19/04/2018.
 */
public class C1_TopicModelFromJSON {
    private JSONIOWrapper jWrapper;
    private ConcurrentHashMap<String, DocumentRow> JSONRows;
    private List<TopicRowContainer> topicMap = new ArrayList<>();
    private List<List<WordData>> wordsAndWeights;
    private TopicModel tModel;
    private final int[] RANDOM_SEEDS = {1351727940, 1742863098, -1602079425, 1775435783, 568478633, -1728550799, -951342906, 201354591, 1964976895, 1996681054, -1470540617, 2021180607, 1963517091, -62811111, 1289694793, -1538086464, -336032733, 1785570484, 1255020924, 1973504944, 668901209, -1994103157, 1499498950, 1863986805, 767661644, 1106985431, 1044245999, -462881427, 667772453, -1412242423, 884961209, -2010762614, -958108485, -1949179036, -1730305825, 1389240794, 836782564, -785551752, 1933688975, -1999538859, -263090972, -508702554, 1140385921, 1267873921, -1344871923, -43961586, -233705489, 628409593, 899215101, 1093870969, -961964970, 771817120, 666140854, -1449071564, -1636392498, -7026344, 1974585266, -685538084, 366222201, -1186218688, 1079183802, -1051858470, 25585342, 855028013, 1678916685, 1972641650, 202789157, -1552096200, -1506270777, -1041494119, 1463369471, -1055350006, 1349843049, -1101551872, 1843673222, -644314819, -1303113477, -1069086690, 498408088, -114723521, -637117566, 1420898137, 366206483, 213561271, 1791833142, -919814411, 1104666572, 1089758161, -513481178, 291291728, -1821691956, -1915769653, 132274482, 1199014123, 1864061694, -1589540732, 295595372, -131466196, -2096364649, -699552916};

    private int numTopics, numIterations, numWords, numDocuments;

    private String inputFilename, outputFilename;

    private int skipCount = 0;

    //Contains the ID of any row that failed, and the reason it failed!
    private ConcurrentHashMap<String, String> failedRetrievals;

    /**
     * This is the step which performs the topic modelling itself. Importantly, before this step you should have set
     * the required LemmaStringData field in the JSON file, as that is the field the topic modelling reads! This part
     * of the pipeline then puts lots of new information into the JSON file, such as the distributions and top words
     * and documents for each topic. It also outputs a CSV file with the distributions in a format which can be used
     * with Well Sorted in order to explore the best number of clusters.
     *
     * @param args - [JSON Location] [Output Location] [Number of Topics] [Number of Iterations] [Number of Words] [Number of Documents]
     */
    public static void main(String[] args) {
        System.out.println("\n********************************************\n" +
                "*                                          *\n" +
                "* STARTING C1_TopicModelFromJSON PHASE!    *\n" +
                "*                                          *\n" +
                "* C1_TopicModelFromJSON:START              *\n" +
                "*                                          * \n" +
                "********************************************\n");

        C1_TopicModelFromJSON startClass = new C1_TopicModelFromJSON();
        startClass.StartTopicModelling(args);

        System.out.println("\n********************************************\n" +
                "*                                          *\n" +
                "* C1_TopicModelFromJSON PHASE COMPLETE!    *\n" +
                "*                                          *\n" +
                "* C1_TopicModelFromJSON:END                *\n" +
                "*                                          * \n" +
                "********************************************\n");
    }

    /**
     * This is the method which fires all the different processes off in order to do all the topic modelling and various
     * processes which are required for this.
     * <p>
     * Please note, the topic modelling used is MALLET and that is already parallelised.
     *
     * @param args - The argument list directly from main()
     */
    private void StartTopicModelling(String[] args) {
        //Round any answers to 15 decimal points as we get floating point errors on the 16th bit (i.e. 1.0000000000000002 instead of 1)
        DecimalFormat df = new DecimalFormat("#.##############");
        df.setRoundingMode(RoundingMode.HALF_UP);

        checkArgs(args);
        LoadJSON(inputFilename);
        runTopicModels();
        GetAndSetDocumentDistributions();
        GetAndSetTopicDetails();
        calculateTopicToTopicCosineDistances(outputFilename, df);
        SaveJSON(outputFilename);
    }

    /**
     * Check that we have the arguments required for the topic modelling. The only required ones are the input and
     * output file. If you only specify these, however, all the topic modelling settings will be set to defaults. If you
     * instead specify all the arguments, you can set the number of topics, iterations, and the number of words and
     * documents which will be listed for each topic. These are listed from most related, to least, and you want to
     * not set this too high as it will make the JSON file massive! Finally, we also check that numeric inputs are
     * valid, and if not we crash out.
     *
     * @param args - The argument list directly from main()
     */
    private void checkArgs(String[] args) {
        if (args.length < 2) {
            System.out.println("\nArguments missing! Please supply arguments in the following order: [JSON Location] [Output Location] [Number of Topics] [Number of Iterations] [Number of Words] [Number of Documents]");
            System.exit(1);
        } else if (args.length < 6) {
            System.out.println("\nTopic Model arguments missing. Continuing with defaults -> 30 topics, 2000 iterations, 50 words, 100 Documents.\n\nPlease supply arguments in the following order: [JSON Location] [Output Location] [Number of Topics] [Number of Iterations] [Number of Words] [Number of Documents]");
            inputFilename = args[0];
            outputFilename = args[1];
            numTopics = 30;
            numIterations = 2000;
            numWords = 50;
            numDocuments = 100;
        } else {
            inputFilename = args[0];
            outputFilename = args[1];
            try {
                numTopics = Integer.parseInt(args[2]);
                numIterations = Integer.parseInt(args[3]);
                numWords = Integer.parseInt(args[4]);
                numDocuments = Integer.parseInt(args[5]);
            } catch (NumberFormatException e) {
                System.out.println("\nNumber conversion failed for final three args. Continuing with defaults -> 30 topics, 2000 iterations, 50 words, 100 Documents.\n\nPlease supply arguments in the following order: [JSON Location] [Output Location] [Number of Topics] [Number of Iterations] [Number of Words] [Number of Documents]");
                numTopics = 30;
                numIterations = 2000;
                numWords = 50;
                numDocuments = 100;
            }
        }
    }

    /**
     * Load in the JSON file, using the JSONIOWrapper functionality.
     *
     * @param JSONFile - location of the JSON file to load
     */
    private void LoadJSON(String JSONFile) {
        jWrapper = new JSONIOWrapper();
        jWrapper.LoadJSON(JSONFile);
        JSONRows = jWrapper.GetRowData();
        failedRetrievals = jWrapper.GetFailedRetrievals();
    }

    /**
     * This is where we set up the environment to get all the data in line so we can run the topic modelling. In
     * particular, we go through each row/document in the JSON data and add it to a data structure in a format the
     * topic modelling expects. We check to ensure each document is still allowed to be included in the model first.
     * As before, we use the Java forEach() style for this.
     * <p>
     * Finally, we fire the topic modelling method to get the process going!
     */
    private void runTopicModels() {
        System.out.println("\n**********\nAdding Lemmas to Model!\n***********\n");

        //To ensure things map to the array order in the JSON file, we'll use this technique of building an ArrayList
        //And adding the items to it via the JSONRow value
        for (int i = 0; i < JSONRows.size(); i++) {
            topicMap.add(null);
        }
        JSONRows.entrySet().forEach(this::addLemmaToModel);

        if (skipCount > 0)
            System.out.println("**********\nSKIPPED " + skipCount + " ROWS!\nThis could be for reasons of not having enough lemmas, stop phrases, or failed retrievals.\nTo discover exact reasons per document, run D1_CreateDownloadableCSV and examine the 'ReasonForRemoval' column\n**********\n");

        System.out.println("\n**********\nLemmas added to Model!\n***********\n");

        runSingleTopicModel(0);
    }

    /**
     * This is the method where we actually add the lemmas to the model. Mostly, we simply check whether early parts
     * of the pipeline have flagged for this document/row to be removed, and if not we add it to the List which will
     * contain the documents in a format the topic modelling understands.
     *
     * @param entry - a single entry to check and add to the data structure to be topic modelled
     */
    private void addLemmaToModel(Map.Entry<String, DocumentRow> entry) {
        String key = entry.getKey();
        DocumentRow value = entry.getValue();

        if (failedRetrievals.containsKey(key)) {
            skipCount++;
        } else {
            if (value.getLemmaStringData() == null) {
                System.out.println("\n***********\nERROR! Cannot find the REQUIRED [REQ]LemmaStringData field in the JSON file. Have you run the P2_Lemmatise step yet?\nHALTING!\n**********");
                System.exit(1);
            } else {

                if (value.getLemmaStringData().length() > 0) {
                    if (value.isIncludedInModel())
                        topicMap.set(value.getJSONRow(), new TopicRowContainer(value.getID(), value.getLemmaStringData()));
                    else
                        skipCount++;
                } else
                    skipCount++;
            }
        }
    }

    /**
     * Here we start the topic modelling going. First we set all the values that the topic model needs to know, such as
     * number of topics, seed, and iterations. Then we run the topic modelling and finally we test it succeeded.
     * <p>
     * N.B. The seed should ensure that if you pass the same data, with the same value for the number argument, you
     * should get the same results. If you want to get different results, make sure you change the number from 0.
     *
     * @param number - the number to label the topic model. This also alters the seed given to the topic model!
     */
    private void runSingleTopicModel(int number) {
        System.out.println("\n**********\nBeginning Topic Modelling!\nFollowing output will be from MALLET:\n***********\n");

        tModel = new TopicModel(topicMap);
        tModel.numTopics = numTopics;
        tModel.name = "topicModelNum" + number;
        tModel.SEED = RANDOM_SEEDS[number];
        tModel.ITER = numIterations;

        if (number == 0)
            tModel.Model(false, true);
        else
            tModel.Model(false, false);

        //Check the topic model has completed successfully! If not, fire this to run again.
        if (tModel.topicDistributions == null || tModel.topicDistributions.isEmpty()) {
            System.out.println("\n**********\nModel " + number + " FAILED! \nTrying again...\n**********\n");
            runSingleTopicModel(number);
        } else {
            System.out.println("Model " + number + " completed!");
        }

        System.out.println("\n**********\nTopic Modelling complete!\n***********\n");
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

        double[][] simMatrix = new double[numTopics][numTopics];

        double[][] topicVector = getTransposedMatrix(tModel.topicDistributions, 0.1f);

        int x, y;

        for (int yTopic = 0; yTopic < numTopics; yTopic++) {
            for (int xTopic = 0; xTopic < numTopics; xTopic++) {

                try{
                    simMatrix[xTopic][yTopic] = Double.valueOf(df.format(cosineSimilarity(topicVector[xTopic], topicVector[yTopic])));

                    //Fill in the other half as a guarantee that we fill the whole matrix.
                    simMatrix[yTopic][xTopic] = simMatrix[xTopic][yTopic];
                } catch (Exception e){
                    System.out.println("Error while calculating the topic to topic distance");
                    System.exit(1);
                }
            }
        }

        jWrapper.SetTopicSimilarities(simMatrix);
        saveCosineDistanceCSV(outputFilename, simMatrix);

        System.out.println("\n**********\nTopic to topic cosine distance calculation complete!\n**********\n");
    }

    /**
     * This is where we actually save the cosine distances to a CSV file. In short, this is where we construct the lines
     * and output them to a file. The similarity/distance matrix should already be created by this point.
     *
     * @param outputFilename - The file path where we want to save the distances! .json will be replaced with .csv
     * @param simMatrix      - the matrix with the cosine distances in, to save to a file.
     */
    private void saveCosineDistanceCSV(String outputFilename, double[][] simMatrix) {
        String newFilename;

        if (outputFilename.contains(".json"))
            newFilename = outputFilename.replace(".json", "T2TCosineDistances.csv");
        else if (outputFilename.contains(".JSON"))
            newFilename = outputFilename.replace(".JSON", "T2TCosineDistances.csv");
        else
            newFilename = "TopicToTopicCosineDistances.csv";

        File file = new File(newFilename);
        CsvWriter csvWriter = new CsvWriter();
        csvWriter.setAlwaysDelimitText(true);

        try (CsvAppender csvAppender = csvWriter.append(file, StandardCharsets.UTF_8)) {
            String[] labels = new String[numTopics];
            csvAppender.appendField("");
            //Create the labels...
            for (int topic = 0; topic < numTopics; topic++) {
                labels[topic] = "";
                for (int word = 0; word < 3; word++) {
                    labels[topic] += wordsAndWeights.get(topic).get(word).label;
                    labels[topic] += "-";
                }
                labels[topic] = labels[topic].substring(0, labels[topic].length() - 1);
                csvAppender.appendField(labels[topic]);
            }

            csvAppender.endLine();

            for (int y = 0; y < simMatrix.length; y++) {
                csvAppender.appendField(labels[y]);

                for (int x = 0; x < simMatrix.length; x++)
                    csvAppender.appendField(String.valueOf(simMatrix[x][y]));

                csvAppender.endLine();
            }
        } catch (Exception e) {
            e.printStackTrace();
            ;
        }
    }

    /**
     * Gets a transposed version of the matrix, which we require for the topic to topic cosine distance calculations.
     * Importantly too, we also do some thresholding here, to remove the problems of the 'long tail' that you get in
     * topic models. I.e. if a value is below the threshold, we zero it to remove it's influence in the distance
     * calculations.
     *
     * @param topicModel - the untransposed distributions from the topic model itself
     * @param threshold  - the threshold we want to use
     * @return the transposed matrix as a 2D double array
     */
    private double[][] getTransposedMatrix(List<TopicData> topicModel, float threshold) {
        double[][] transposedMatrix = new double[topicModel.get(0).topicDistributions.length][topicModel.size()];

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

    /**
     * Here is where we actually calculate the cosine similarity/distance between two vectors. This code is a pretty
     * standard way of calculating it, which you can look up online!
     *
     * @param vectorA - the first vector
     * @param vectorB - the second vector
     * @return the similarity/distance between the two vectors
     */
    private double cosineSimilarity(double[] vectorA, double[] vectorB) {
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

    /**
     * Gets the topic distributions for each document from the topic model, and updates the JSON data structure with
     * them, so each document has its own, individual distribution across all topics. This is set directly in the
     * JSONRows structure, which will be saved to JSON later.
     */
    private void GetAndSetDocumentDistributions() {
        System.out.println("\n**********\nAdding Distributions to Document Rows!\n***********\n");

        //If you change the first value to false, you get the actual number of words distributed to each topic from each document!
        double[][] distributions = tModel.model.getDocumentTopics(true, false);

        for (Map.Entry<String, DocumentRow> entry : JSONRows.entrySet()) {
            if (tModel.stringIDtoNumID.containsKey(entry.getKey())) {
                int modelPosition = tModel.stringIDtoNumID.get(entry.getKey());
                entry.getValue().setTopicDistribution(distributions[modelPosition]);
            }
        }

        System.out.println("\n**********\nDistributions added to Document Rows!\n***********\n");
    }

    /**
     * Gets the top words for each topic, and also the topic documents for each topic, and sets them so they'll be saved
     * to the JSON file later. As always, we use the JSONIOWrapper to set this information so it is saved correctly
     * when the time comes.
     * <p>
     * N.B. This method ensures you have a MAXIMUM of the number of words and documents set and YOU MIGHT GET LESS. This
     * can happen if the topic doesn't have as many words or documents with a greater than zero contribution as you
     * requested.
     */
    private void GetAndSetTopicDetails() {
        System.out.println("\n**********\nAdding Topic Details to JSON Structure!\n***********\n");

        wordsAndWeights = new ArrayList<>();
        ArrayList<TreeSet<IDSorter>> sortedWords = tModel.model.getSortedWords();
        Alphabet alphabet = tModel.model.getAlphabet();

        ArrayList<TreeSet<IDSorter>> topicDocuments = tModel.model.getTopicDocuments(0);
        List<List<WordData>> documents = new ArrayList<>();

        for (int topic = 0; topic < sortedWords.size(); topic++) {
            List<WordData> topicWords = new ArrayList<>();

            int count = 0;
            for (IDSorter word : sortedWords.get(topic)) {
                if (count >= numWords)
                    break;

                if (word.getWeight() > 0)
                    topicWords.add(new WordData(word.getID(), topic, (String) alphabet.lookupObject(word.getID()), word.getWeight()));

                count++;
            }

            wordsAndWeights.add(topicWords);


            List<WordData> docsInTopic = new ArrayList<>();

            count = 0;
            for (IDSorter document : topicDocuments.get(topic)) {
                if (count >= numDocuments)
                    break;

                if (document.getWeight() > 0)
                    docsInTopic.add(new WordData(0, topic, tModel.numIDtoStringID.get(document.getID()), document.getWeight()));

                count++;
            }

            documents.add(docsInTopic);
        }


        jWrapper.SetTopicDetails(wordsAndWeights, documents);

        System.out.println("\n**********\nTopic Details added to JSON Structure!\n***********\n");
    }

    /**
     * Finally, we save all of the information back to the JSON file. In particular, we put lots of informatio into the
     * metadata about the topic model settings used, and update the JSONRow data so the distributions are correctly
     * saved. Top words and documents were directly set to the JSONIOWrapper object in the GetAndSetTopicDetails()
     * method, so they'll be saved too.
     *
     * @param JSONFile - location of the JSON file to save to
     */
    private void SaveJSON(String JSONFile) {
        ConcurrentHashMap<String, String> metadata = jWrapper.GetMetadata();
        metadata.put("P3_Used", "TopicModelFromJSON");
        metadata.put("numTopics", "" + numTopics);
        metadata.put("numIter", "" + numIterations);
        metadata.put("numTopWords", "" + numWords);
        metadata.put("numTopDocs", "" + numDocuments);
        jWrapper.SetMetadata(metadata);

        jWrapper.SetRowData(JSONRows);

        jWrapper.SaveJSON(JSONFile);
    }
}
