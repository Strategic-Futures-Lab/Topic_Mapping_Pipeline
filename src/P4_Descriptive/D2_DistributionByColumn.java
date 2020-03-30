package P4_Descriptive;

import PX_Helper.DocumentRow;
import PX_Helper.JSONIOWrapper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.swing.text.Document;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Tom on 23/05/2018.
 */
public class D2_DistributionByColumn
{
    private JSONIOWrapper jWrapper;
    private ConcurrentHashMap<String, DocumentRow> JSONRows;
    //Contains the ID of any row that failed, and the reason it failed!
    private ConcurrentHashMap<String, String> failedRetrievals;

    private String inputFilename, outputFilename;
    private String keyColumn;
    private HashSet<String> uniqueValues = new HashSet<>();
    private String[] distColumns;
    private int numTopics;
    private List<HashMap<String, Distribution>> distributions = new ArrayList<>();
    private HashMap<String, Distribution> skippedDistribution = new HashMap<>();

    /**
     * This is an optional part of the pipeline which allows you to create a summary distribution on a specific field
     * that was set in the P1_Input phase. For example, if you want to know how much money for each topic comes from
     * each university, you can do that here. The Key Column defines what field/column to search for unique values
     * (the university in our example) and the Distribute Column(s) should be fields with numerical values you can to
     * distribute (the money in our example). Please note: this will ALWAYS calculate the document distribution across
     * the unique values in the Key Column as well. The results should be saved in a new JSON file.
     *
     * @param args - [JSON Input Location] [JSON Distribution Location] [Key Column] [Distribute Column 1] [Distribute Column 2] ...
     */
    public static void main (String[] args)
    {
        System.out.println( "\n********************************************\n" +
                "*                                          *\n" +
                "* STARTING D2_DistributionByColumn PHASE!  *\n" +
                "*                                          *\n" +
                "* D2_DistributionByColumn:START            *\n" +
                "*                                          *\n" +
                "********************************************\n");

        D2_DistributionByColumn startClass = new D2_DistributionByColumn();
        startClass.CreateCSV(args);

        System.out.println( "\n********************************************\n" +
                              "*                                          *\n" +
                              "* D2_DistributionByColumn PHASE COMPLETE!  *\n" +
                              "*                                          *\n" +
                              "* D2_DistributionByColumn:END              *\n" +
                              "*                                          *\n" +
                              "********************************************\n");
    }

    /**
     * The main class that sets off the distribution creation process. We don't do anything special here, as everything
     * is outsourced to it's own method.
     *
     * @param args - The argument list directly from main()
     */
    private void CreateCSV(String[] args)
    {
        checkArgs(args);
        LoadJSON(inputFilename);
        ValidateColumns();
        FindUniqueValues();
        CalculateDistributions();
        SaveJSON(outputFilename);
    }

    /**
     * Here we ensure we have all the arguments we require to create the distribution file. In particular we need an
     * input and output file location, and the Key Column to use to calculate the distribution. You do not need to
     * supply addition distribution columns if you don't want to, as you'll get the document distribution by default.
     *
     * @param args - The argument list directly from main()
     */
    private void checkArgs(String[] args)
    {
        if(args.length < 3)
        {
            System.out.println("\nArguments missing!\n\nPlease supply arguments in the following order: [JSON Input Location] [JSON Distribution Location] [Key Column] [Distribute Column 1] [Distribute Column 2] ...");
            System.exit(1);
        }
        else if(args.length >= 3)
        {
            inputFilename = args[0];
            outputFilename = args[1];
            keyColumn = args[2];

            distColumns = new String[args.length - 3];
            System.arraycopy(args, 3, distColumns, 0, distColumns.length);
        }
    }

    /**
     * Loads the information from the JSON file, using the JSONIOWrapper interface
     *
     * @param JSONFile - location of the JSON file to load
     */
    private void LoadJSON(String JSONFile)
    {
        jWrapper = new JSONIOWrapper();
        jWrapper.LoadJSON(JSONFile);
        JSONRows = jWrapper.GetRowData();
        failedRetrievals = jWrapper.GetFailedRetrievals();
        numTopics = jWrapper.GetTopicSimilarities().length;
    }

    /**
     * Before we do anything else, we ensure that both the Key Column and any Distribution Column(s) passed are actually
     * valid. For the key column we just check it exists as it can be any form, although text is best. For the
     * distribution column(s) we check that they exist, and that they contain a numerical value which we calculate means
     * with!
     *
     * Please note, that means if you have numerical values, you should avoid blanks (put 0 instead) as otherwise this
     * could fail!
     */
    private void ValidateColumns()
    {
        for(Map.Entry<String, DocumentRow> entry : JSONRows.entrySet())
        {
            DocumentRow row = entry.getValue();

            if(!row.getValues().containsKey(keyColumn))
            {
                System.out.println("\nKey Column invalid!\n\nThe Key Column (" + keyColumn + ") was not found within the JSON File provided! Please provide a valid Key Column!");
                System.exit(1);
            }

            try
            {
                for (String distColumn : distColumns)
                {
                    Double.parseDouble(row.getValue(distColumn));
                }
            }
            catch (NumberFormatException e)
            {
                System.out.println("\nDistribution Column invalid!\n\nPlease ensure all Distribution Columns you supply have numerical values contained within them!");
                System.exit(1);
            }

            System.out.println("\nAll Columns supplied appear to be valid!\n");

            return;
        }
    }

    /**
     * This method allows us to find the unique values in the Key Column, which will then be the values we distribute
     * the values to. As we use a hashmap, this will be case sensitive! We also create any data structures we'll need
     * to calculate the distributions here.
     */
    private void FindUniqueValues()
    {
        for(Map.Entry<String, DocumentRow> entry : JSONRows.entrySet())
        {
            DocumentRow row = entry.getValue();

            uniqueValues.add(row.getValue(keyColumn));
        }

        //Create the global structure we'll now use to calculate the distributions!
        for(int i = 0; i < numTopics; i++)
        {
            HashMap<String, Distribution> hashMap = new HashMap<>();
            for (String value : uniqueValues)
            {
                hashMap.put(value, new Distribution(value));

                if(i == 0)
                    skippedDistribution.put(value, new Distribution(value));
            }
            distributions.add(hashMap);
        }

        System.out.println("\nFound " + uniqueValues.size() + " unique values in Key Column!\n");
    }

    /**
     * This is where we do the calculation of distributions. By this point, the unique values in the Key Column should
     * have already been calculated and the global structures created (run FindUniqueValues() first).
     *
     * Important addendum: this includes the concept of a 'skipped' topic with the topicID of -1. Any document which
     * is skipped for whatever reason will be assigned to the 'skipped' topic at 100%, rather than to what would be
     * implied by its value in the Key Column.
     */
    private void CalculateDistributions()
    {
        int skipCount = 0;
        System.out.println("\n**********\nCalculating Distributions!\n***********\n");
        int count = 0;
        for(Map.Entry<String, DocumentRow> entry : JSONRows.entrySet())
        {
            DocumentRow row = entry.getValue();
            String key = row.getValue(keyColumn);
            double[] topicDist = row.getTopicDistribution();

            if(topicDist != null)
            {
                for(int i = 0; i < numTopics; i++)
                {
                    if(distributions.get(i).containsKey(key))
                    {

                        distributions.get(i).get(key).addToDocumentTotal(topicDist[i]);

                        for (String column : distColumns)
                        {
                            //Check to see if we have an empty string... If we do, skip it.
                            if(!row.getValue(column).isEmpty())
                                distributions.get(i).get(key).addToColumn(column, topicDist[i] * Double.parseDouble(row.getValue(column)));
                        }

                    }
                    else
                        System.out.println("\nWARNING! Failed to find the following key: " + key + "\n");
                }
            }
            else
            {
                if(skippedDistribution.containsKey(key))
                {
                    //Add the full document to the skipped distribution 'topic'.
                    skippedDistribution.get(key).addToDocumentTotal(1);

                    for (String column : distColumns)
                    {
                        //Check to see if we have an empty string... If we do, skip it.
                        if (!row.getValue(column).isEmpty())
                            skippedDistribution.get(key).addToColumn(column, Double.parseDouble(row.getValue(column)));
                    }
                }
                else
                    System.out.println("\nWARNING! Failed to find the following key: " + key + "\n");

                skipCount++;
            }

        }

        if(skipCount > 0)
            System.out.println("**********\nSKIPPED " + skipCount + " ROWS!\nThis could be for reasons of not having enough lemmas, stop phrases, or failed retrievals.\nTo discover exact reasons per document, run D1_CreateDownloadableCSV and examine the 'ReasonForRemoval' column\n**********\n");


        System.out.println("\n**********\nDistributions Calculated!\n***********\n");
    }

    /**
     * Here we add the topic to the JSON structure which we will then save out to a file. We do this manually, as we
     * can't use the JSONIOWrapper for this.
     *
     * @param conceptID - the topicID, known for legacy reasons as 'conceptId' in the JSON file we output
     * @param rootArray - the root array we want to append the topic objects to
     * @param uniDistributions - the distributions across the unique values from the Key Column
     */
    private void AddTopicToJSON(int conceptID, JSONArray rootArray, HashMap<String, Distribution> uniDistributions)
    {
        JSONObject topicObj = new JSONObject();
        topicObj.put("conceptId", conceptID);

        JSONArray topicData = new JSONArray();
        for(String label : uniqueValues)
        {
            Distribution tempDist = uniDistributions.get(label);
            JSONObject valueObj = new JSONObject();
            valueObj.put("label", label);
            valueObj.put("numberOfDocuments", tempDist.getDocumentTotal());

            for(Map.Entry<String, Double> entry : tempDist.getColumnValues().entrySet())
            {
                valueObj.put(entry.getKey(), entry.getValue());
            }
            topicData.add(valueObj);
        }

        topicObj.put("topicData", topicData);
        rootArray.add(topicObj);
    }

    /**
     * Saves the distributions to a separate JSON file which can be accessed from D3. This was designed to work (mostly)
     * with legacy D3/Hex Map versions so the labels don't necessarily match up with the rest of the pipeline.
     *
     * @param JSONFile - location of the JSON file to save to
     */
    private void SaveJSON(String JSONFile)
    {
        System.out.println("\n**********\nSaving Distribution Completed!\n***********\n");

        JSONArray rootArray = new JSONArray();

        AddTopicToJSON(-1, rootArray, skippedDistribution);

        for(int i = 0; i < numTopics; i++)
        {
            AddTopicToJSON(i, rootArray, distributions.get(i));
        }

        try (FileWriter file = new FileWriter(JSONFile))
        {
            file.write(rootArray.toJSONString());
            file.flush();

        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        System.out.println("\n**********\nDistribution File Saved!\n***********\n");
    }

    /**
     * This class is to allow us to easily store the distributions in a Hash Map.
     */
    public class Distribution
    {
        private String ID;
        private double documentTotal;
        private HashMap<String, Double> columnTotals = new HashMap<>();

        public Distribution(String ID)
        {
            this.ID = ID;
            documentTotal = 0;
            columnTotals = new HashMap<>();
        }

        public void addToDocumentTotal(double value)
        {
            documentTotal += value;
        }

        public void addToColumn(String key, double value)
        {
            if(columnTotals.containsKey(key))
                columnTotals.put(key, columnTotals.get(key) + value);
            else
                columnTotals.put(key, value);
        }

        public String getID()
        {
            return ID;
        }

        public double getDocumentTotal()
        {
            return documentTotal;
        }

        public HashMap<String, Double> getColumnValues()
        {
            return columnTotals;
        }
    }
}
