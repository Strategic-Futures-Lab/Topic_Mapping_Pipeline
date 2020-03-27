package P5_Layout;

import PX_Helper.DocumentRow;
import PX_Helper.JSONIOWrapper;
import PY_TopicModelCore.WordData;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Tom on 14/05/2018.
 * DOCUMENTATION TO COME!
 */
public class E1_CreateReducedJSONForD3
{
    private String inputFilename, outputFilename, skippedSortColumn;

    private JSONIOWrapper jWrapper;
    private ConcurrentHashMap<String, DocumentRow> JSONRows;
    private List<List<WordData>> wordsAndWeights;
    private List<List<WordData>> topicDocuments;

    private double[][] topicSimilarity;
    private int numTopics, numDocuments, numDocsPerTopic;
    // added by P. Le Bras on 06/12/18
    private String stopwords;

    private String[] columns, columnNames;

    /**
     * This part of the pipeline takes the huge JSON file we have by this stage, and simplifies it so we can use it in
     * a D3 Hex Map visualisation. In particular, it allows us to define which of the many fields/columns we added to
     * the JSON file in the P1_Input stage we want to appear in the JSON file. With the updated versions of the D3
     * visualisation, any columns you add here will appear in the data table on the right. We always add the Relevance
     * column and the 'warning triangle' at the moment, in the D3.
     *
     * This is one of the two required files for the D3 layout (this one populates the data table in particular), with
     * the second being made in the E2_CreateHexMap file. If you want to also show distributions, like in the N8 map,
     * you need the third file from D2_DistributionByColumn.
     *
     * The format of the Column arguments should be [Name=UserField] where name is the visible name you want, and column is
     * the name of the column in the JSON. E.g. University=LeadROName would mean 'put the LeadROName data in table
     * column titled University'.
     *
     * @param args - [JSON Input Location] [Python JSON Output Location] [Skipped Sort Column] [Column 1] [Column 2] ...
     */
    public static void main (String[] args)
    {
        System.out.println( "\n********************************************\n" +
                            "*                                          *\n" +
                            "* STARTING E1_CreateJSONForPython PHASE!   *\n" +
                            "*                                          *\n" +
                            "* E1_CreateJSONForPython:START             *\n" +
                            "*                                          * \n" +
                            "********************************************\n");

        E1_CreateReducedJSONForD3 startClass = new E1_CreateReducedJSONForD3();
        startClass.CreateJSONForPython(args);

        System.out.println( "\n********************************************\n" +
                            "*                                          *\n" +
                            "* STARTING E1_CreateJSONForPython PHASE!   *\n" +
                            "*                                          *\n" +
                            "* E1_CreateJSONForPython:START             *\n" +
                            "*                                          * \n" +
                            "********************************************\n");
    }

    /**
     * The main class that sets off the JSON creation process. We don't do anything special here, as everything
     * is outsourced to it's own method.
     *
     * @param args - The argument list directly from main()
     */
    private void CreateJSONForPython(String[] args)
    {
        checkArgs(args);
        LoadJSON(inputFilename);
        outputJSONforDocumentDetails(topicSimilarity, wordsAndWeights, topicDocuments, numTopics, numDocuments);
    }

    /**
     * Here we ensure we have all the arguments we require to create the JSON file. In particular, we require an
     * input and output file, which should be different. Additionally, we need a numeric column which allows us to
     * determine how the 'skipped' documents are sorted, as they won't have relevance to the topic to use.
     * It's HIGHLY RECOMMENDED that if you're using this file that you supply at least one additional column, or the
     * data table won't make much sense for users!
     *
     * @param args - The argument list directly from main()
     */
    private void checkArgs(String[] args)
    {
        if(args.length < 4)
        {
            System.out.println("\nArguments missing!\n\nPlease supply arguments in the following order: [JSON Input Location] [Python JSON Output Location] [Skipped Sort Column] [Column 1] [Column 2] ...\n" +
                    "The format of the Column arguments should be [Name=UserField] where name is the visible name you want, and column is the name of the column in the JSON!");
            System.exit(1);
        }
        else
        {
            inputFilename = args[0];
            outputFilename = args[1];
            skippedSortColumn = args[2];

            columns = new String[args.length - 3];
            columnNames = new String[columns.length];
            for(int i = 3; i < columns.length + 3; i++)
            {
                String[] tokens = args[i].split("\\=");
                if(tokens.length == 2)
                {
                    columnNames[i - 3] = tokens[0];
                    columns[i - 3] = tokens[1];
                }
                else
                {
                    System.out.println("\nColumn input invalid!\n\nPlease supply arguments in the following order: [JSON Input Location] [Python JSON Output Location] [Skipped Sort Column] [Column 1] [Column 2] ...\n" +
                            "The format of the Column arguments should be [Name=UserField] where name is the visible name you want, and column is the name of the column in the JSON!");
                    System.exit(1);
                }
            }
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
        topicDocuments = jWrapper.GetTopicDocuments();
        wordsAndWeights = jWrapper.GetTopicWords();
        topicSimilarity = jWrapper.GetTopicSimilarities();

        ConcurrentHashMap<String, String> metadata = jWrapper.GetMetadata();

        numTopics = Integer.parseInt(metadata.get("numTopics"));
        numDocuments = Integer.parseInt(metadata.get("numDocs"));
        numDocsPerTopic = Integer.parseInt(metadata.get("numTopDocs"));
        stopwords = metadata.getOrDefault("StopWords", "");
    }

    /**
     * Here we take the top words which were originally saved in the JSON pipeline file, and convert them into the
     * format expected by the D3. The number of words appended will depend on the number specified in the
     * P3_TopicModelling stage.
     *
     * @param topicsObject - the JSONObject to append the topic words array to
     * @param labelList - the list of the words for this particular topic
     * @param topicNumber - the topic number to label the array with
     */
    @SuppressWarnings("unchecked")
    private void addWordsToTopicObject(JSONObject topicsObject, List<WordData> labelList, int topicNumber)
    {
        JSONArray tempArray = new JSONArray();

        for (WordData label : labelList)
        {
            JSONObject tempObject = new JSONObject();
            tempObject.put("label", label.label);
            tempObject.put("weight", label.weight);
            tempArray.add(tempObject);
        }

        topicsObject.put(Integer.toString(topicNumber), tempArray);
    }

    /**
     * Here we take the top documents which were originally saved in the JSON pipeline file, and convert them into the
     * format expected by the D3. The number of documents appended will depend on the number specified in the
     * P3_TopicModelling stage.
     *
     * @param topicDocsDistribObject - the JSONObject to append the topic documents array to
     * @param docsForTopic - the list of the documents for this particular topic
     * @param topicNumber - the topic number to label the array with
     */
    @SuppressWarnings("unchecked")
    private void addDocumentsToTopicDocsDistribObject(JSONObject topicDocsDistribObject, List<WordData> docsForTopic, int topicNumber)
    {
        JSONArray docDistribArray = new JSONArray();
        int count = 0;

        for(WordData doc : docsForTopic)
        {
            JSONObject docObject = new JSONObject();
            DocumentRow documentRow = JSONRows.get(doc.label);

            docObject.put("docID", doc.label);
            docObject.put("wordCount", documentRow.getNumLemmas());
            docObject.put("topicWeight", doc.weight);

            JSONObject docInfo = new JSONObject();

            for (String column : columns)
            {
                if(documentRow.getValues().containsKey(column))
                    docInfo.put(column, documentRow.getValue(column));
                else if(count == 0)
                    System.out.println("\n**********\nSKIPPING AS COLUMN NOT FOUND: " + column + "!\n***********\n");
            }

            docObject.put("docInfo", docInfo);
            docDistribArray.add(docObject);
            count++;
        }
        topicDocsDistribObject.put(Integer.toString(topicNumber), docDistribArray);
    }

    /**
     * Here we get a list of the documents which were 'skipped' for whatever reason earlier in the pipeline, and return
     * them in the List format expected by the addDocumentsToTopicDocsDistribObject() method so we can add the skipped
     * topics as a unique '-1' topic. This allows us to visualise them later as required.
     *
     * These are ordered by the column specified as [Skipped Sort Column] in the input args as we can't use the topic
     * relevance due to them not being modelled.
     *
     * @return a List of documents which were 'skipped' in a format acceptable for the addDocumentsToTopicDocsDistribObject() method
     */
    private List<WordData> getSkippedTopDocumentsByFunding()
    {
        List<WordData> documents = new ArrayList<>();
        boolean added;

        for(Map.Entry<String, DocumentRow> entry : JSONRows.entrySet())
        {
            added = false;
            DocumentRow row = entry.getValue();

            if(!row.isIncludedInModel())
            {
                if(!row.getValues().containsKey(skippedSortColumn))
                {
                    System.out.println("\nColumn input invalid!\n\nPlease supply arguments in the following order: [JSON Input Location] [Python JSON Output Location] [Skipped Sort Column] [Column 1] [Column 2] ...\n" +
                            "The format of the Column arguments should be [Name=Column] where name is the visible name you want, and column is the name of the column in the JSON!");
                    System.exit(1);
                }

                int value = Integer.parseInt(row.getValue(skippedSortColumn));

                for (int i = 0; i < Math.min(documents.size(), numDocsPerTopic); i++)
                {
                    if (value > documents.get(i).weight)
                    {
                        documents.add(i, new WordData(-1, -1, row.getID(), value));
                        added = true;
                        break;
                    }

                    //Don't add it to the end if we reached the end of the list length!
                    if (i == numDocsPerTopic - 1)
                        added = true;
                }

                if (!added)
                    documents.add(new WordData(-1, -1, row.getID(), value));
            }
        }

        return documents.subList(0, Math.min(numDocsPerTopic, documents.size()));
    }


    /**
     * This creates the JSON format which the legacy D3 Hex Map layout expect. As such, some of the tags won't
     * necessarily match the format expected from the rest of the pipeline. This is one of the two required files for
     * the D3 layout (this one populates the data table in particular), with the second being made in the
     * E2_CreateHexMap file. If you want to also show distributions, like in the N8 map, you need the third file from
     * D2_DistributionByColumn. As this is a unique JSON file, we don't use the JSONIOWrapper here.
     *
     * @param matrix - the topic similarities to put into the JSON
     * @param labels - the top words per topic to put into the JSON
     * @param topicDocs - the top docuiments per topic to put into the JSON
     * @param numTopics - the number of topics to put into the JSON metadata
     * @param numDocs - the number of documents to put into the JSON metadata
     */
    @SuppressWarnings("unchecked")
    private void outputJSONforDocumentDetails(double[][] matrix, List<List<WordData>> labels, List<List<WordData>> topicDocs, int numTopics, int numDocs)
    {
        System.out.println("\n**********\nCreating Document Details Output File!\n***********\n");
        JSONObject rootObject = new JSONObject();

        /*
         * CREATE THE JSON 'topics' OBJECT AND ADD IT TO THE ROOT OBJECT!
         */
        JSONObject topicsObject = new JSONObject();
        for(int i = 0; i < matrix[0].length; i++)
        {
            addWordsToTopicObject(topicsObject, labels.get(i), i);
        }

        //Add the '-1' unclassified topic into the topics object
        List<WordData> unclassifiedTopic = new ArrayList<>();
        unclassifiedTopic.add(new WordData(-1, -1, "Unclassified", 30));
        addWordsToTopicObject(topicsObject, unclassifiedTopic, -1);

        rootObject.put("topics", topicsObject);

        /*
         * CREATE THE JSON 'topicsSimilarites' OBJECT AND ADD IT TO THE ROOT OBJECT
         */
        JSONObject topicSimilaritiesObject = new JSONObject();

        for(int y = 0; y < matrix.length; y++)
        {
            JSONObject tempObject = new JSONObject();
            for(int x = 0; x < matrix[0].length;  x++)
            {
                tempObject.put(Integer.toString(x), matrix[x][y]);
            }
            topicSimilaritiesObject.put(Integer.toString(y), tempObject);
        }

        rootObject.put("topicsSimilarities", topicSimilaritiesObject);

        /*
         * CREATE THE 'metadata' OBJECT AND ADD IT TO THE ROOT OBJECT
         */

        JSONObject metadataObject = new JSONObject();

        metadataObject.put("nTopics", numTopics);
        metadataObject.put("nModels", 1);
        metadataObject.put("docClasses", new JSONArray());
        metadataObject.put("nDocs", numDocs);
        metadataObject.put("StopWords", stopwords);

        JSONArray nameArray = new JSONArray();
        JSONArray jsonArray = new JSONArray();
        for(int i = 0; i < columnNames.length; i++)
        {
            nameArray.add(columnNames[i]);
            jsonArray.add(columns[i]);
        }
        metadataObject.put("columnNames", nameArray);
        metadataObject.put("columns", jsonArray);

        rootObject.put("metadata", metadataObject);

        /*
         * ADD THE TIMESLICE DETAILS IF THEY EXIST!
         */
        JSONObject timeSlices = jWrapper.GetTimeSlicesInJSON();
        if(timeSlices != null)
            rootObject.put("timeSlices", timeSlices);

        /*
         * CREATE THE 'topicDocsDistrib' OBJECT, FILL IT, AND ADD IT TO ROOT OBJECT
         */
        JSONObject topicDocsDistribObject = new JSONObject();
        for(int i = 0; i < topicDocs.size(); i++)
        {
            addDocumentsToTopicDocsDistribObject(topicDocsDistribObject, topicDocs.get(i), i);
        }
        addDocumentsToTopicDocsDistribObject(topicDocsDistribObject, getSkippedTopDocumentsByFunding(), -1);

        rootObject.put("topicsDocsDistrib", topicDocsDistribObject);

        try (FileWriter file = new FileWriter(outputFilename))
        {
            file.write(rootObject.toJSONString());
            file.flush();

        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        System.out.println("\n**********\nDocument Details Output File Completed!\n***********\n");
    }

}
