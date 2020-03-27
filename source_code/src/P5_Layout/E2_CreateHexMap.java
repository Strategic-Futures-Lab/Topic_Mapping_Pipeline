package P5_Layout;

import PX_Helper.*;
import PY_TopicModelCore.WordData;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Tom on 08/05/2018.
 */
public class E2_CreateHexMap
{
    private String inputFilename, outputFilename;

    private JSONIOWrapper jWrapper;
    private List<List<WordData>> wordsAndWeights;
    private List<List<WordData>> topicDocuments;

    private double[][] topicSimilarity;

    private List<AgglomerativeClusteringHelper.ClusterRow> clusterRows;

    private int numLeafNodes;
    private int currentClusterNum = 0;

    private int numClusters = 10;
    private AgglomerativeClusteringHelper.CLUSTER_TYPE clusterMethod = AgglomerativeClusteringHelper.CLUSTER_TYPE.AVERAGE;
    private float surroundPercentage = 0.666666f;

    private Boolean outputLinkageTable = false;

    private int linkageSplitHeight;

    private List<Integer> genericTopics = new ArrayList<>();

    private double minHexX = Integer.MIN_VALUE, minHexY = Integer.MIN_VALUE;

    /**
     * This is the final part of the pipeline at time of writing, and this performs the final layout of the topics into
     * the hexagonal layout as seen in the D3 Hex Map layouts. In general this copies the functionality from Pierre's
     * original python code, with a couple of simplifications made where appropriate.
     *
     * This is one of the two required files for the D3 layout (this one gives the hexagon positions in particular),
     * with the second being made in the E1_CreateReducedJSONForD3 file. If you want to also show distributions, like
     * in the N8 map you need the third file from D2_DistributionByColumn.
     *
     * Most code in this file works in cubic coordinates, and is only converted to Euclidean ones later. See here:
     * https://www.redblobgames.com/grids/hexagons/#coordinates
     *
     * @param args - [JSON Input Location] [HexMap Output Location] [Num Clusters] [Cluster Method - AVERAGE|MAX|MIN] [Surround Percentage] [Generic Topic 1] [Generic Topic 2] ...
     */
    public static void main (String[] args)
    {
        System.out.println( "\n********************************************\n" +
                            "*                                          *\n" +
                            "* STARTING E2_CreateHexMap PHASE!          *\n" +
                            "*                                          *\n" +
                            "* E2_CreateHexMap:START                    *\n" +
                            "*                                          * \n" +
                            "********************************************\n");

        E2_CreateHexMap startClass = new E2_CreateHexMap();
        startClass.CreateHexMap(args);

        System.out.println( "\n********************************************\n" +
                            "*                                          *\n" +
                            "* E2_CreateHexMap PHASE COMPLETE!          *\n" +
                            "*                                          *\n" +
                            "* E2_CreateHexMap:END                      *\n" +
                            "*                                          * \n" +
                            "********************************************\n");
    }

    /**
     * The main class that sets off the JSON creation process. We don't do anything special here, as everything
     * is outsourced to it's own method.
     *
     * @param args - The argument list directly from main()
     */
    public void CreateHexMap(String[] args)
    {
        checkArgs(args);
        LoadJSON(inputFilename);
        CreateLinkageTable();
        PerformHexMapLayout();
    }

    /**
     * Here we ensure we have all the arguments we require to create the JSON file. We require the following things:
     * Input and output file, the number of high level clusters to use (these have a thick border, and might be
     * separated too), the clustering method (usually MAX, never use MIN), and the surround percentage (which determines
     * how 'islandy' the layout will be, 1 meaning each cluster is its own island).
     *
     * In addition, you can define as many topics as you like as 'generic' (by passing their topic number) and these
     * will not be included in the layout, instead they'll appear with the 'skipped' topic in the bottom right.
     *
     * @param args - The argument list directly from main()
     */
    private void checkArgs(String[] args) {

        String inputValidationMessage = "Please supply arguments in the following order: [JSON Input Location] [HexMap Output Location] [Num Clusters] [Cluster Method - AVERAGE|MAX|MIN] [Surround Percentage] [Linkage Table Output - True|False] [Generic Topic 1] [Generic Topic 2] ...";

        if(args.length < 2){
            System.out.println("\nArguments missing!\n\n"+inputValidationMessage);
            System.exit(1);
        } else {

            inputFilename = args[0];
            outputFilename = args[1];

            if(args.length < 5){
                System.out.println("\nArguments missing!\n\n"+inputValidationMessage);
                System.out.println("Setting: [Num Clusters] to 0 | [Cluster Method] to AVERAGE | [Surround Percentage] to 0 | [Linkage Table Output] to false | No generic topics supplied!\n");

                numClusters = 0;
                clusterMethod = AgglomerativeClusteringHelper.CLUSTER_TYPE.AVERAGE;
                surroundPercentage = 0;
            } else {
                try {
                    numClusters = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    System.out.println("\n[Num Cluster] argument is NOT valid. It must be a valid integer!\n\n"+inputValidationMessage);
                    System.exit(1);
                }

                if(args[3].equalsIgnoreCase("MIN"))
                    clusterMethod = AgglomerativeClusteringHelper.CLUSTER_TYPE.MIN;
                else if(args[3].equalsIgnoreCase("AVERAGE"))
                    clusterMethod = AgglomerativeClusteringHelper.CLUSTER_TYPE.AVERAGE;
                else if(args[3].equalsIgnoreCase("MAX"))
                    clusterMethod = AgglomerativeClusteringHelper.CLUSTER_TYPE.MAX;
                else {
                    System.out.println("\n[Cluster Method] argument is NOT valid. It must be one of the following: AVERAGE|MAX|MIN!\n\n"+inputValidationMessage);
                    System.exit(1);
                }

                try {
                    surroundPercentage = Float.parseFloat(args[4]);
                    if(surroundPercentage < 0f || surroundPercentage > 1f)
                        throw new NumberFormatException("Float outside valid bounds of between 0 and 1");
                } catch (NumberFormatException e) {
                    System.out.println("\n[Surround Percentage] argument is NOT valid. It must be a valid float between 0 and 1!\n\n"+inputValidationMessage);
                    System.exit(1);
                }
                if(args.length > 5){
                    if(args[5].equalsIgnoreCase(("true")))
                        outputLinkageTable = true;

                    try {
                        for(int i = 6; i < args.length; i++) {
                            if(genericTopics.contains(Integer.parseInt(args[i])))
                                throw new NumberFormatException("Number already in list...");

                            genericTopics.add(Integer.parseInt(args[i]));
                        }

                        //Ensure these are sorted!
                        Collections.sort(genericTopics);
                    } catch (NumberFormatException e) {
                        System.out.println("\nA [Generic Topic] argument is NOT valid. They all must be UNIQUE valid integers!\n\n"+inputValidationMessage);
                        System.exit(1);
                    }
                }
            }
        }

    }

    /**
     * Load in the JSON file, using the JSONIOWrapper functionality.
     *
     * @param JSONFile - the filename of the main, full JSON file
     */
    private void LoadJSON(String JSONFile)
    {
        jWrapper = new JSONIOWrapper();
        jWrapper.LoadJSON(JSONFile);
        topicDocuments = jWrapper.GetTopicDocuments();
        wordsAndWeights = jWrapper.GetTopicWords();
        topicSimilarity = jWrapper.GetTopicSimilarities();

        ConcurrentHashMap<String, String> metadata = jWrapper.GetMetadata();
    }

    /**
     * Creates the linkage table in the Matlab-style. Each row represents a tree node (where two lower nodes join) and
     * the leaf nodes are implicit. It is an ordered list where each item consists of two nodes which are joined and the
     * height at which they join. For example, if you have 50 leaf nodes, any nodes numbered < 50 will be a leaf node.
     * Any numbered above 50 will be a tree node, represented in this list at position n - 50.
     * As each row defines a binary split, you can iterate from the end of the list upwards to get a specific number of
     * clusters.
     */
    private void CreateLinkageTable()
    {
        topicSimilarity = AgglomerativeClusteringHelper.RemoveTopicsFromMatrix(topicSimilarity, genericTopics);
        clusterRows = AgglomerativeClusteringHelper.PerformClustering(ConvertToDissimilarity(topicSimilarity), clusterMethod);
    }

    /**
     * Converts the similarity or distance information we get from the cosine similarity calculations to the dissimiliarity
     * matrix which is used within the clustering algorithms.
     *
     * @param similarityMatrix - a n x n similarity or distance matrix, like the one stored in the JSON, diagonal should be 1
     * @return an n x n dissimilarity matrix where the diagonal is 0
     */
    private double[][] ConvertToDissimilarity(double[][] similarityMatrix)
    {
        double[][] dissimilarityMatrix = new double[similarityMatrix.length][similarityMatrix.length];
        double maxVal = similarityMatrix[0][0];

        for(int y = 0; y < similarityMatrix.length; y++)
        {
            for(int x = 0; x < similarityMatrix.length; x++)
            {
                dissimilarityMatrix[x][y] = maxVal - similarityMatrix[x][y];
            }
        }

        return dissimilarityMatrix;
    }

    /**
     * Runs the hex map layout algorithm to get the positions of each topic in the hex map space, converts these positions
     * from the cubic coordinates used in the layout algorithm to standard 2D x/y Euclidean coordinates, and finally
     * saves these positions in a second JSON file as the D3 visualisation expects.
     */
    private void PerformHexMapLayout()
    {
        numLeafNodes = clusterRows.size() + 1;

        System.out.println("\n**********\nPerforming Hexagon Layout!\n***********\n");
        linkageSplitHeight = clusterRows.size() - numClusters;
        HashSet<Hexagon> finalGrouping = estimatePositions(clusterRows.size() - 1);
        System.out.println("\n**********\nHexagon Layout Complete!\n***********\n");

        System.out.println("\n**********\nConverting Hexagons to 2D Coordinates!\n***********\n");
        HashSet<EuclidHexagon> finalGrouping2D = switchToEuclideanCoordinates(finalGrouping);
        System.out.println("\n**********\nCoordinate Conversion Complete!\n***********\n");

        finalGrouping2D = PositionSkippedDocsHex(finalGrouping2D);
        finalGrouping2D = RenumberHexTopicsAndAddGenerics(finalGrouping2D, genericTopics);

        System.out.println("\n**********\nSaving Layout to JSON!\n***********\n");
        saveJSONPositions(outputFilename, finalGrouping2D);
        System.out.println("\n**********\nLayout Saved!\n***********\n");
    }

    private HashSet<EuclidHexagon> RenumberHexTopicsAndAddGenerics(HashSet<EuclidHexagon> finalGrouping2D, List<Integer> genericTopics)
    {
        for(EuclidHexagon hex : finalGrouping2D)
        {
            for(Integer genericTopic : genericTopics)
            {
                if(hex.topic >= genericTopic)
                    hex.topic++;
            }
        }

        double yPos = minHexY - 3;

        for(Integer genericTopic : genericTopics)
        {
            finalGrouping2D.add(new EuclidHexagon(minHexX + (Math.sqrt(3) * 2), yPos, genericTopic, -5));
            yPos -= 2;
        }

        return finalGrouping2D;
    }

    private HashSet<EuclidHexagon> PositionSkippedDocsHex(HashSet<EuclidHexagon> finalGrouping2D)
    {
        //Find the minimum x and maximum y
        for(EuclidHexagon hex : finalGrouping2D)
        {
            if(hex.x > minHexX)
                minHexX = hex.x;

            if(hex.y > minHexY)
                minHexY = hex.y;
        }

        //x += 0.5;

        finalGrouping2D.add(new EuclidHexagon(minHexX + (Math.sqrt(3) * 2), minHexY, -1, -1));

        return finalGrouping2D;
    }

    /**
     * Saves the hexagon positions (and the words for the word clouds) into a separate JSON file, as the D3 visualisation
     * expects.
     *
     * @param filename - the filename to save the layout information into
     * @param finalGrouping the result of the hex layout algorithm, already converted to Euclidean coordinates
     */
    private void saveJSONPositions(String filename, HashSet<EuclidHexagon> finalGrouping)
    {
        try (FileWriter file = new FileWriter(filename))
        {
            JSONObject root = new JSONObject();

            JSONArray conceptsData = new JSONArray();

            for(EuclidHexagon hex : finalGrouping)
            {
                JSONObject topicObject = new JSONObject();

                topicObject.put("clusterId", hex.cluster);
                topicObject.put("conceptId", hex.topic);

                JSONObject coord = new JSONObject();
                coord.put("x", hex.x);
                coord.put("y", hex.y);

                topicObject.put("hexCoordinates", coord);

                JSONArray labels = new JSONArray();

                if(hex.topic > -1)
                {
                    for (WordData wordData : wordsAndWeights.get(hex.topic))
                    {
                        JSONObject wordObj = new JSONObject();
                        wordObj.put("label", wordData.label);
                        wordObj.put("weight", wordData.weight);
                        labels.add(wordObj);
                    }
                }
                else
                {
                    JSONObject wordObj = new JSONObject();
                    wordObj.put("label", "Unclassified");
                    wordObj.put("weight", 2500);
                    labels.add(wordObj);
                    for(int i = 0; i < 10; i++)
                    {
                        wordObj = new JSONObject();
                        wordObj.put("label", "");
                        wordObj.put("weight", 0);
                        labels.add(wordObj);
                    }
                }
                topicObject.put("labels", labels);

                conceptsData.add(topicObject);
            }

            root.put("conceptsData", conceptsData);

            if(outputLinkageTable){
                JSONArray linkageTable = new JSONArray();

                for(AgglomerativeClusteringHelper.ClusterRow row : clusterRows){
                    JSONObject linkageTableRow = new JSONObject();

                    linkageTableRow.put("node1", row.Node1);
                    linkageTableRow.put("node2", row.Node2);
                    linkageTableRow.put("distance", row.Distance);

                    linkageTable.add(linkageTableRow);
                }

                root.put("linkageTable", linkageTable);
            }

            String string = root.toJSONString();
            file.write(root.toJSONString());
            file.flush();

        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    /**
     *
     * @param rowNumber
     * @return
     */
    private HashSet<Hexagon> estimatePositions(int rowNumber)
    {
        HashSet<Hexagon> group1 = new HashSet<>();
        AgglomerativeClusteringHelper.ClusterRow clusterRow = clusterRows.get(rowNumber);

        System.out.println("Processing row: " + rowNumber + " (Node " + (rowNumber + numLeafNodes) + ") - Node1: " + clusterRow.Node1 + " | Node2: " + clusterRow.Node2);

        //if(clusterRow.Node1 == 29 && clusterRow.Node2 == 52)
        //    System.out.println("Debug point!");

        //If we've found a leaf node...
        if(clusterRow.Node1 < numLeafNodes)
        {
            //If we're join TWO leaf nodes, just return...
            if(clusterRow.Node2 < numLeafNodes)
            {
                group1.add(new Hexagon(0, 0, 0, clusterRow.Node1, currentClusterNum));
                group1.add(new Hexagon(1, -1, 0, clusterRow.Node2, currentClusterNum));
                return group1;
            }
            else
            {
                group1.add(new Hexagon(0, 0, 0, clusterRow.Node1, currentClusterNum));

                //If we are at a tree node 'above' the split line, make sure the depth first search on the other side
                //starts at a new cluster number.
                if(rowNumber > linkageSplitHeight)
                    currentClusterNum++;

                HashSet<Hexagon> group2 = estimatePositions(clusterRow.Node2 - numLeafNodes);
                return positionGroupsWithSwapping(group1, group2, rowNumber);
            }
        }
        else if(clusterRow.Node2 < numLeafNodes)
        {
            group1.add(new Hexagon(0, 0, 0, clusterRow.Node2, currentClusterNum));

            //If we are at a tree node 'above' the split line, make sure the depth first search on the other side
            //starts at a new cluster number.
            if(rowNumber > linkageSplitHeight)
                currentClusterNum++;

            HashSet<Hexagon> group2 = estimatePositions(clusterRow.Node1 - numLeafNodes);
            return positionGroupsWithSwapping(group1, group2, rowNumber);
        }
        else
        {
            group1 = estimatePositions(clusterRow.Node1 - numLeafNodes);

            //If we are at a tree node 'above' the split line, make sure the depth first search on the other side
            //starts at a new cluster number.
            if(rowNumber > linkageSplitHeight)
                currentClusterNum++;

            HashSet<Hexagon> group2 = estimatePositions(clusterRow.Node2 - numLeafNodes);
            return positionGroupsWithSwapping(group1, group2, rowNumber);
        }
    }

    private HashSet<Hexagon> positionGroupsWithSwapping(HashSet<Hexagon> group1, HashSet<Hexagon> group2, int rowNumber)
    {
        if(group1.size() > group2.size())
            return positionGroups(group1, group2, rowNumber);
        else
            return positionGroups(group2, group1, rowNumber);
    }

    private HashSet<Hexagon> positionGroups(HashSet<Hexagon> group1, HashSet<Hexagon> group2, int rowNumber)
    {
        HashSet<Hexagon> finalPosition = new HashSet<>();
        double minAverageDistance = Float.MAX_VALUE, tempAverageDistance;
        HashSet<Hexagon> potentialLocations = new HashSet<>();

        int level = 1;

        HashSet<Hexagon> group1Spaced;
        if(rowNumber > linkageSplitHeight)
            group1Spaced = surroundGroup(group1, surroundPercentage);
        else
            group1Spaced = cloneGroup(group1);

        /*
         * We iterate through this finding process, each time widening the search range around the hexagons, i.e. how
         * far away from the larger group we look. We start with the immediately surrounding hexs (1 level), then 2, etc
         * Once we've found a valid position, drop out from the while loop!
         */
        while(finalPosition.size() == 0)
        {
            //Get all the position locations we can try... For each hexagon in the largest group...
            for(Hexagon group1Hex : group1Spaced)
            {
                //Get all of its empty neighbours...
                for(Hexagon surroundHex : findFreeNeighbours(group1Spaced, group1Hex, level))
                {
                    //And if we don't have them already, add them to a potential location list
                    if(!potentialLocations.contains(surroundHex))
                        potentialLocations.add(surroundHex);
                }
            }

            //Now we iterate through each candidate position to test the placement of the second, smaller group...
            for(Hexagon candidateHex : potentialLocations)
            {
                //We will try the smaller group in all six possible rotations on the hex grid...
                for(int i = 0; i < 6; i++)
                {
                    HashSet<Hexagon> newGroup2 = cloneGroup(group2);

                    //Rotate the number of times required as rotateGroup rotates 60 degrees...
                    newGroup2 = rotateGroupNumTimes(newGroup2, i);

                    //Translate the group to the candidate hexagon...
                    newGroup2 = translateGroup(newGroup2, candidateHex);

                    //If this new position does NOT intersect with the larger group...
                    if(!groupIntersects(group1Spaced, newGroup2))
                    {
                        //Get the distance between this new group and the larger group, then check if it is a new minimum
                        tempAverageDistance = getAverageDistanceBetweenGroups(group1, newGroup2);

                        if(tempAverageDistance < minAverageDistance)
                        {
                            //If it is, record the new minimum distance, before setting it as the new final position!
                            minAverageDistance = tempAverageDistance;
                            finalPosition = new HashSet<>();
                            finalPosition.addAll(group1);
                            finalPosition.addAll(newGroup2);

                            if(finalPosition.size() < group1.size() + newGroup2.size())
                                System.out.println("HEXAGON LOST! WHY!?");
                        }
                    }
                }
            }

            level++;
        }

        return finalPosition;
    }

    private HashSet<Hexagon> surroundGroup(HashSet<Hexagon> group, float surroundPercentage)
    {
        HashSet<Hexagon> neighbours = new HashSet<>();

        for(Hexagon hex : group)
        {
            //Get all of its empty neighbours...
            for (Hexagon freeHex : findFreeNeighbours(group, hex, 1))
            {
                if(!coordinateExists(neighbours, freeHex))
                    neighbours.add(freeHex);
            }
        }

        //Now remove the surrounding items as necessary, with a roughly even spread.
        neighbours = takeEvenSpread(neighbours, surroundPercentage);

        //Finally, add the original group back in for layout purposes
        neighbours.addAll(group);

        return neighbours;
    }

    private HashSet<Hexagon> takeEvenSpread(HashSet<Hexagon> group, float surroundPercentage)
    {
        HashSet<Hexagon> neighbours = new HashSet<>();
        float curValue = 0;

        for(Hexagon hex : group)
        {
            curValue += surroundPercentage;

            if(curValue >= 1)
            {
                neighbours.add(hex);
                curValue -= 1;
            }
        }

        return neighbours;
    }

    private HashSet<Hexagon> findFreeNeighbours(HashSet<Hexagon> group, Hexagon coordinate, int level)
    {
        HashSet<Hexagon> neighbours = findNeighbours(coordinate, level);
        HashSet<Hexagon> freeNeighbours = new HashSet<>();

        for (Hexagon neighbour : neighbours)
        {
            if(!coordinateExists(group, neighbour))
                freeNeighbours.add(neighbour);
        }

        return freeNeighbours;
    }

    private HashSet<Hexagon> findNeighbours(Hexagon coordinate, int level)
    {
        HashSet<Hexagon> directNeighbours = new HashSet<>();

        directNeighbours.add(new Hexagon(coordinate.x + 1, coordinate.y - 1, coordinate.z));
        directNeighbours.add(new Hexagon(coordinate.x, coordinate.y - 1, coordinate.z + 1));
        directNeighbours.add(new Hexagon(coordinate.x - 1, coordinate.y, coordinate.z + 1));
        directNeighbours.add(new Hexagon(coordinate.x - 1, coordinate.y + 1, coordinate.z));
        directNeighbours.add(new Hexagon(coordinate.x, coordinate.y + 1, coordinate.z - 1));
        directNeighbours.add(new Hexagon(coordinate.x + 1, coordinate.y, coordinate.z - 1));

        if(level == 1)
        {
            return directNeighbours;
        }
        else
        {
            HashSet<Hexagon> neighbours = new HashSet<>();
            for(Hexagon hex : directNeighbours)
            {
                for(Hexagon hex2 : findNeighbours(hex, level - 1))
                {
                    if(!neighbours.contains(hex2))
                        neighbours.add(hex2);
                }
            }
            return neighbours;
        }
    }

    /**
     * Get the average distance between the two groups in the following way:
     * - Interate through each pair of hexagons and get their distance in the hex grid
     * - Also get their distance in the original similarity matrix
     * - Sum: hexDist * simDist for each pair
     * - Divide this result by the sum of simDist
     *
     * @param group1 - the first group to compare
     * @param group2 - the second group to compare
     * @return the weighted distance between the two groups: sum(hexDist * simDist) / sum(simDist)
     */
    private double getAverageDistanceBetweenGroups(HashSet<Hexagon> group1, HashSet<Hexagon> group2)
    {
        double numerator = 0;
        double denominator = 0;
        int tempHexDist = 0;
        double tempSimDist = 0;

        for(Hexagon hex1 : group1)
        {
            for(Hexagon hex2 : group2)
            {
                tempHexDist = getHexDistanceBetweenHexagons(hex1, hex2);
                tempSimDist = getSimDistanceBetweenHexagons(hex1, hex2);

                numerator += (double)tempHexDist * tempSimDist;
                denominator += tempSimDist;
            }
        }

        if(denominator != 0)
            return numerator / denominator;
        else
            return numerator;
    }

    /**
     * Gets the distance between two hexagons on the hex grid itself. i.e. hexagons next to each other will have a
     * distance of 1.
     * @param hex1 - the first hex to compare
     * @param hex2 - the second hex to compare
     * @return the distance between the two items, as defined on the hex grid. Always an int value.
     */
    private int getHexDistanceBetweenHexagons(Hexagon hex1, Hexagon hex2)
    {
        return Math.max(Math.max(Math.abs(hex1.x - hex2.x), Math.abs(hex1.y - hex2.y)),
                        Math.abs(hex1.z - hex2.z));
    }

    /**
     * Gets the distances between two hexagons from the original similarity matrix. As such, this will fail if passed
     * a hexagon without a valid topic number.
     * @param hex1 - the first hex to compare
     * @param hex2 - the second hex to compare
     * @return the distance between the two hexagons in the higher dimensional space represented by the sim matrix. Double value.
     */
    private double getSimDistanceBetweenHexagons(Hexagon hex1, Hexagon hex2)
    {
        if(hex1.topic > -1 && hex2.topic > -1)
        {
            return topicSimilarity[hex1.topic][hex2.topic];
        }
        else
        {
            System.out.println("\n**********\nTRYING TO GET DISTANCE BETWEEN TWO INVALID HEXS... EXITING...\n**********\n");
            System.exit(1);
            return -1;
        }
    }

    /**
     * As we by default only rotate clockwise by one step (60 degrees) this function allows us to rotate an arbitary
     * number of times as appropriate. Each rotation will still only be in 60 degree steps, however.
     * @param group - the group to rotate
     * @param numberOfTimes - number of 60 degree clockwise steps to perform
     * @return the rotated group
     */
    private HashSet<Hexagon> rotateGroupNumTimes(HashSet<Hexagon> group, int numberOfTimes)
    {
        for(int i = 0; i < numberOfTimes; i++)
            group = rotateGroup(group);

        return group;
    }

    /**
     * Rotates a group by 60 degrees clockwise, and returns the rotated group!
     * @param group - the group to rotate
     * @return the rotated group
     */
    private HashSet<Hexagon> rotateGroup(HashSet<Hexagon> group)
    {
        for(Hexagon hex : group)
        {
            int x = hex.x;
            int y = hex.y;
            int z = hex.z;
            hex.x = -z;
            hex.y = -x;
            hex.z = -y;
        }

        return group;
    }

    /**
     * Translates a group by the vector provided
     * @param group - group to translate
     * @param vector - vector (defined as a Hexagon object) to translate the group by!
     * @return the group which has now been translated!
     */
    private HashSet<Hexagon> translateGroup(HashSet<Hexagon> group, Hexagon vector)
    {
        for(Hexagon hex : group)
        {
            hex.x += vector.x;
            hex.y += vector.y;
            hex.z += vector.z;
        }

        return group;
    }

    /**
     * Clones a group into an entirely new object with the exact same properties...
     * @param group - the group you want cloned
     * @return the new HashSet which represents the group!
     */
    private HashSet<Hexagon> cloneGroup(HashSet<Hexagon> group)
    {
        HashSet<Hexagon> clone = new HashSet<>();
        for(Hexagon hex : group)
            clone.add(new Hexagon(hex.x, hex.y, hex.z, hex.topic, hex.cluster));

        return clone;
    }

    /**
     * Check whether the hexagon coordinates appear within the group supplied!
     * @param group - the group to check against
     * @param coordinate - the coordinates to check
     * @return whether the coordinates are represented in the group
     */
    private boolean coordinateExists(HashSet<Hexagon> group, Hexagon coordinate)
    {
        for(Hexagon hex : group)
        {
            if(hex.x == coordinate.x && hex.y == coordinate.y && hex.z == coordinate.z)
                return true;
        }

        return false;
    }

    /**
     * Tests whether two groups intersect. If a single intersection is found, it returns immediately!
     * @param group1 - first group to test
     * @param group2 - second group to test
     * @return true if the two group intersect, false if not
     */
    private boolean groupIntersects(HashSet<Hexagon> group1, HashSet<Hexagon> group2)
    {
        for(Hexagon hex : group1)
        {
            if(coordinateExists(group2, hex))
                return true;
        }

        return false;
    }

    /**
     * Converts all of the hexagon positions which are in cubic coordinates into euclidean coordinates and returns them!
     * @param group - the group of hexagons to convert
     * @return a new HashSet of the hexagons' Euclidean positions
     */
    private HashSet<EuclidHexagon> switchToEuclideanCoordinates(HashSet<Hexagon> group)
    {
        HashSet<EuclidHexagon> newHexagons = new HashSet<>();

        for(Hexagon hex : group)
        {
            newHexagons.add(cubicPositionToEuclidean(hex));
        }

        return newHexagons;
    }

    /**
     * Tranform the cubic coordinates used throughout the hex layout process to the standard x/y euclidean coordinate
     * system. Assumes:
     * - Radius of hexagon is 1
     * - Height = size * 2 = 2
     * - Width = sqrt(3) / 2 * height = sqrt(3)
     * - Vertical distance = height * 3/4 = 3/2
     * - Vertical coordinate is proportional to z coordinate in cubic
     * - Horizontal distance = width = sqrt(3)
     * - Horizontal coordinate is proportional to (x - y) / 2 coordinate in cubic
     * @param hex - the hex to convert
     * @return a EuclidHexagon object which contains the new values
     */
    private EuclidHexagon cubicPositionToEuclidean(Hexagon hex)
    {
        double x = ((double)hex.x - (double)hex.y) / 2 * Math.sqrt(3);
        double y = (double)hex.z * 3.0 / 2.0;

        return new EuclidHexagon(x, y, hex.topic, hex.cluster);
    }

    private void printHexLayout(HashSet<Hexagon> group)
    {
        /*for(int r = 0; r < group.size(); r++)
        {

        }*/
    }

}
