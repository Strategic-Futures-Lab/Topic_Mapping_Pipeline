package P4_Descriptive;

import PX_Helper.DocumentRow;
import PX_Helper.JSONIOWrapper;
import PX_Helper.Pair;
import PY_TopicModelCore.WordData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Tom on 03/07/2018.
 *
 * Changed by P. Le Bras on 05/12/18:
 *  - added possibility to specify user field to carry to the reduced JSON
 *    It assumes that the value of that user field is identical for all subdocuments
 */
public class D3_SubDocumentReduce
{
    private String inputFilename, outputFilename, skippedSortColumn;

    private JSONIOWrapper jWrapper;
    private ConcurrentHashMap<String, DocumentRow> JSONRows, newJSONRows;
    private ConcurrentHashMap<String, List<List<Double>>> DocumentTimeSlices;
    private ConcurrentHashMap<String, Integer> NumTimeSlices;
    private List<List<WordData>> wordsAndWeights;
    private List<List<WordData>> topicDocuments;

    private ConcurrentHashMap<String, List<String>> LemmaRows;
    private ConcurrentHashMap<String, List<Double>> TopicDistributionRows;

    private double[][] topicSimilarity;
    private int numTopics, numDocuments, numDocsPerTopic, maxTimeSlice = Integer.MIN_VALUE, roundUpPadding = 10;

    private boolean padTimeSlices = false;

    // Changed by P. Le Bras on 05/12/18
    private String[] columnsToKeep;
    private ConcurrentHashMap<String, ArrayList<Pair<String, String>>> columnValues;

    /**
     * This is another optional part of the pipeline. Specifically it should ONLY be used if you've used an input module
     * which splits the input documents into 'sub documents' in some way. For example A1_AMIInput (and with some
     * modification A1_PDFParser) both split their respective documents by word or time.
     *
     * In short, what this module does is to take those sub documents and use the results to construct a topic timeline
     * about the original documents. So, each original document will appear as a single row in the hex map table, BUT
     * you have access to how the topic trends throughout the document. The resolution of this depends on how finely
     * you originally sliced the input documents up back in the P1 module used.
     *
     * @param args - [JSON Input Location] [JSON Output Location] [Pad Time Slices]
     */
    public static void main (String[] args)
    {
        System.out.println( "\n********************************************\n" +
                "*                                          *\n" +
                "* STARTING D3_SubDocumentReduce PHASE!     *\n" +
                "*                                          *\n" +
                "* D3_SubDocumentReduce:START               *\n" +
                "*                                          * \n" +
                "********************************************\n");

        D3_SubDocumentReduce startClass = new D3_SubDocumentReduce();
        startClass.ReduceSubDocuments(args);

        System.out.println( "\n********************************************\n" +
                "*                                          *\n" +
                "* STARTING D3_SubDocumentReduce PHASE!     *\n" +
                "*                                          *\n" +
                "* D3_SubDocumentReduce:START               *\n" +
                "*                                          * \n" +
                "********************************************\n");
    }

    /**
     * The main class that sets off the reduction process. We don't do anything special here, as everything
     * is outsourced to it's own method.
     *
     * @param args - The argument list directly from main()
     */
    private void ReduceSubDocuments(String[] args)
    {
        checkArgs(args);
        LoadJSON(inputFilename);
        CreateDocumentTrends();
        CreateReducedRows();
        CalculateNewTopDocuments();
        //outputJSONforDocumentDetails(topicSimilarity, wordsAndWeights, topicDocuments, numTopics, numDocuments);
        SaveJSONFile(outputFilename);
    }

    /**
     * Here we ensure we have all the arguments we require to create the JSON file. This function updates the main JSON
     * file, so you can choose to overwrite the main one if desired. You will lose the raw sub-document details if you
     * do, however. The only other input you need is whether or not to pad the number of time slices for each document
     * distribution. This defaults to true if left off!
     *
     * true - each distribution will have the same number of time slices as the longest. In effect, each distribution
     *        will be right zero-padded to ensure everything is the same length. This is nicer or animated graphs during
     *        visualisation.
     *
     * false - The distributions will be in effect a jagged 2D array, where each distribution is only the length of the
     *         number of sub documents each document was split in. This was kept just in case someone in future wanted
     *         this behaviour!
     *
     * It should be noted even if you put true for padding, the original number of time slices is still stored in the
     * "TimeSegments" tag in the JSON file, so you're not losing that information.
     *
     * FINALLY: If you want the interface to have this trend/timeline data, you MUST use the output from this module as
     * the input into the P5 modules! In general you should run the D1_CreateDownloadableCSV module before this one,
     * however, as that way the sub document information goes into the CSV for people to use how they like.
     *
     * @param args - The argument list directly from main()
     */
    private void checkArgs(String[] args)
    {
        if(args.length < 2)
        {
            System.out.println("\nArguments missing!\n\nPlease supply arguments in the following order: [JSON Input Location] [JSON Output Location] [Pad Time Slices]");
            System.exit(1);
        }
        else
        {
            inputFilename = args[0];
            outputFilename = args[1];

            if(args.length > 2)
            {
                if(args[2].equalsIgnoreCase(("true")))
                    padTimeSlices = true;
            }

            if(args.length > 3)
            {
                try
                {
                    roundUpPadding = Integer.parseInt(args[3]);
                }
                catch (NumberFormatException e)
                {
                    System.out.println("Fourth argument must be an integer if provided! Please supply arguments in the following order: [JSON Input Location] [JSON Output Location] [Pad Time Slices] [Round Up Padding]");
                    System.exit(1);
                }
            }

            // Added by P. Le Bras on 05/12/18
            if(args.length > 4){
                columnsToKeep = new String[args.length - 4];
                for(int i = 4; i < columnsToKeep.length + 4; i++){
                    columnsToKeep[i - 4] = args[i];
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

        ConcurrentHashMap<String, String> metadata = jWrapper.GetMetadata();

        numTopics = Integer.parseInt(metadata.get("numTopics"));
        numDocuments = Integer.parseInt(metadata.get("numDocs"));
        numDocsPerTopic = Integer.parseInt(metadata.get("numTopDocs"));
    }

    /**
     * This method does the grunt work of actually calculating the topic trends for each document. This method, in
     * particular, creates and populates the data structures to store this information. The JSON/JSONIOWrapper creation
     * is performed in CreateReducedRows. It should be noted in particular that due to the nature of the JSON storage,
     * sub documents don't get loaded in a sensible order, so much of the work done here is just to reorder things
     * correctly!
     */
    private void CreateDocumentTrends()
    {
        DocumentTimeSlices = new ConcurrentHashMap<>();
        NumTimeSlices = new ConcurrentHashMap<>();

        for(Map.Entry<String, DocumentRow> entry : JSONRows.entrySet())
        {
            String ID = entry.getKey();
            DocumentRow row = entry.getValue();
            String origDoc = row.getValue("OriginalDocument");
            int splitNumber = Integer.parseInt(row.getValue("SplitNumber"));

            if(!DocumentTimeSlices.containsKey(origDoc))
            {
                List<List<Double>> tempTimeSlice = new ArrayList<>();
                for(int i = 0; i < numTopics; i++)
                {
                    tempTimeSlice.add(new ArrayList<>());
                }

                DocumentTimeSlices.put(origDoc, tempTimeSlice);
            }

            if(row.isIncludedInModel())
            {
                for (int i = 0; i < numTopics; i++)
                {
                    AddDoubleToListAtAnyPosition(DocumentTimeSlices.get(origDoc).get(i),
                            row.getTopicDistribution()[i],
                            splitNumber);
                }

                if(splitNumber > maxTimeSlice)
                    maxTimeSlice = splitNumber;

            }
        }

        //Finally, pad out all the meeting time slices to make them look better in the interface! Of course, this only
        // happens if the requisite input argument is set to true!s
        if(padTimeSlices)
        {
            //Round up the time slices to the next round number as specified (for visualisation reasons)
            maxTimeSlice = (int)Math.ceil((double)maxTimeSlice / roundUpPadding) * roundUpPadding;

            for(Map.Entry<String, DocumentRow> entry : JSONRows.entrySet())
            {
                String ID = entry.getKey();
                DocumentRow row = entry.getValue();
                String origDoc = row.getValue("OriginalDocument");
                for (int i = 0; i < numTopics; i++)
                {
                    AddDoubleToListAtAnyPosition(DocumentTimeSlices.get(origDoc).get(i),
                            0.0,
                            maxTimeSlice);
                }
            }
        }
    }

    /**
     * A utility method which allows arbitrary list addition, in particular it allows you to add doubles to a list
     * position which is bigger than the list size. It works be creating 'empty' list positions until it gets to the
     * correct place to add the value provided. All empty list positions are set to 0.0
     *
     * Yes, I know this isn't very efficient, but it's quick enough that who cares?
     *
     * @param list - the list you are wanting to add to
     * @param item - the value to add to the list
     * @param position - the position to add the double to the list
     */
    private void AddDoubleToListAtAnyPosition(List<Double> list, double item, int position)
    {
        if(position < 0)
        {
            System.err.println("CANNOT PUT ELEMENT AT POSITION < 0");
            System.exit(1);
        }
        else if(position < list.size())
        {
            list.set(position, item);
        }
        else
        {
            for (int i = list.size(); i < position; i++)
            {
                list.add(0.0);
            }

            list.add(item);
        }
    }

    /**
     * A utility method which allows arbitrary list addition, in particular it allows you to add strings to a list
     * position which is bigger than the list size. It works be creating 'empty' list positions until it gets to the
     * correct place to add the string provided. All empty list positions are set to "" or an zero-length string.
     *
     * Yes, I know this isn't very efficient, but it's quick enough that who cares?
     *
     * @param list - the list you are wanting to add to
     * @param item - the value to add to the list
     * @param position - the position to add the string to the list
     */
    private void AddStringToListAtAnyPosition(List<String> list, String item, int position)
    {
        if(position < 0)
        {
            System.err.println("CANNOT PUT ELEMENT AT POSITION < 0");
            System.exit(1);
        }
        else if(position < list.size())
        {
            list.set(position, item);
        }
        else
        {
            for (int i = list.size(); i < position; i++)
            {
                list.add("");
            }

            list.add(item);
        }
    }

    /**
     * This method, as the title suggests, does the main 'recombining' work. In particular it calculates the document-
     * wide topic distributions by summing up the sub-document distributions across the topics, and taking the average!
     * It also saves the total distributions for each topic across sub-documents, as it was decided to use this for
     * ordering purposes, rather than the average. Finally, it populates the DocumentRows in the JSONIOWrapper structure
     * ready to be saved later.
     *
     * N.B. There was some discussion if we should order by average or total topic distribution values. Average would
     * give undue prominence to short documents (i.e. ones with fewer sub-documents) where as total might give
     * prominence to longer ones. Tom & Stefano decided that total would likely map to users' expectations more as a
     * meeting which talks MORE about a topic (rather than on average more) would be expected to appear first.
     */
    private void CreateReducedRows()
    {
        LemmaRows = new ConcurrentHashMap<>();
        TopicDistributionRows = new ConcurrentHashMap<>();
        newJSONRows = new ConcurrentHashMap<>();
        // Added by P. Le Bras on 05/12/18
        columnValues = new ConcurrentHashMap<>();

        for(Map.Entry<String, DocumentRow> entry : JSONRows.entrySet())
        {
            String ID = entry.getKey();
            DocumentRow row = entry.getValue();
            String origDoc = row.getValue("OriginalDocument");
            int splitNumber = Integer.parseInt(row.getValue("SplitNumber"));

            //If this is the first from a certain document we've found, create the required data structures
            if(!LemmaRows.containsKey(origDoc))
            {
                LemmaRows.put(origDoc, new ArrayList<>());

                List<Double> zeroList = new ArrayList<>();
                for(int i = 0; i < numTopics; i++)
                {
                    zeroList.add(0.0);
                }

                TopicDistributionRows.put(origDoc, zeroList);

                // Added by P. Le Bras on 05/12/18
                ArrayList<Pair<String,String>> values = new ArrayList<>();
                for(String columnName: columnsToKeep){
                    values.add(new Pair<>(columnName,row.getValue(columnName)));
                }
                columnValues.put(origDoc,values);
            }

            //If the row is included, add it to the row!
            if(row.isIncludedInModel())
            {
                /*if(origDoc.equals("EN2001a"))
                    System.out.println("Debug Point");*/

                //Add the lemmas back in the correct order...
                AddStringToListAtAnyPosition(LemmaRows.get(origDoc), row.getLemmaStringData(), splitNumber);

                //And sum up all the topic distributions!
                for(int i = 0; i < numTopics; i++)
                {
                    TopicDistributionRows.get(origDoc).set(i, TopicDistributionRows.get(origDoc).get(i) + row.getTopicDistribution()[i]);
                }
            }
        }

        int JSONRow = 0;
        //Once we've combined everything, go back through the new distributions, and take the averages...
        for(Map.Entry<String, List<Double>> entry : TopicDistributionRows.entrySet())
        {
            String ID = entry.getKey();

            /*if(ID.equals("EN2001a"))
                System.out.println("Debug Point");*/

            int numParts = 0;
            double[] topicDist = new double[numTopics];
            String lemmaString = "";
            //Only grab parts which have some text in, as the others weren't included in the topic model
            for(int i = 0; i < LemmaRows.get(ID).size(); i++)
            {
                if(LemmaRows.get(ID).get(i).length() > 0)
                {
                    numParts++;
                    lemmaString = lemmaString.concat(" " + LemmaRows.get(ID).get(i));
                }
            }

            lemmaString = lemmaString.trim();

            for(int i = 0; i < numTopics; i++)
            {
                topicDist[i] = TopicDistributionRows.get(ID).get(i) / numParts;
            }

            DocumentRow newRow = new DocumentRow();
            newRow.setID(ID);
            newRow.setJSONRow(JSONRow++);
            newRow.setTopicDistribution(topicDist);
            newRow.setLemmaStringData(lemmaString);
            newRow.setNumLemmas(lemmaString.split(" ").length);
            newRow.setValue("TimeSegments", String.valueOf(LemmaRows.get(ID).size()));
            newRow.setValue("ValidTimeSegments", String.valueOf(numParts));
            newRow.setValue("OriginalDocument", ID);

            //Added by P. Le Bras on 05/12/18
            for(Pair<String,String> valuesToKeep:columnValues.get(ID)){
                newRow.setValue(valuesToKeep.getLeft(),valuesToKeep.getRight());
            }

            newJSONRows.put(ID, newRow);

            NumTimeSlices.put(ID, LemmaRows.get(ID).size());
        }

        jWrapper.SetRowData(newJSONRows);
    }

    /**
     * Now we've recombined all the documents, we need to calculate the new top documents for each topic. We use the
     * weight values calculated in CreateReducedRows, so please see the note there about the ordering decision made.
     */
    private void CalculateNewTopDocuments()
    {
        //Get the topic words, which we won't be changing...
        List<List<WordData>> topicWords = jWrapper.GetTopicWords();
        //Create a structure to hold the documents which we WILL be changing.
        List<List<WordData>> documents = new ArrayList<>();

        /**
         * Create the correct number of lists for the number of topics!
         */
        for(int topic = 0; topic < numTopics; topic++)
        {
            documents.add(new ArrayList<>());
        }

        for(Map.Entry<String, List<Double>> entry : TopicDistributionRows.entrySet())
        {
            String ID = entry.getKey();
            List<Double> topicDist = entry.getValue();

            for(int topic = 0; topic < numTopics; topic++)
            {
                int pos;
                for(pos = 0; pos < documents.get(topic).size(); pos++)
                {
                    if(topicDist.get(topic) >= documents.get(topic).get(pos).weight)
                        break;
                }

                if(topicDist.get(topic) > 0)
                    documents.get(topic).add(pos, new WordData(0, topic, ID, topicDist.get(topic)));
            }
        }

        jWrapper.SetTopicDetails(topicWords, documents);
        //System.out.println("Debug Point!");
    }

    /**
     * Finally, save all the information back into the JSON file. Unlike most of the rest of the modules from P4 and P5
     * this one is designed to put information back into the 'core' JSON file which earlier parts of the pipeline use.
     * So you CAN give the same input and output file. You will lose the raw sub-document details if you do, however.
     *
     * @param JSONFile - location of the JSON file to save to
     */
    private void SaveJSONFile(String JSONFile)
    {
        ConcurrentHashMap<String, String> metadata = jWrapper.GetMetadata();
        metadata.put("P4_Used(D3)", "SubDocumentReduce");
        metadata.put("NumReducedDocuments", String.valueOf(LemmaRows.size()));

        jWrapper.SetMetadata(metadata);
        jWrapper.SetTimeSlices(DocumentTimeSlices, NumTimeSlices);
        jWrapper.SaveJSON(JSONFile);
    }
}
