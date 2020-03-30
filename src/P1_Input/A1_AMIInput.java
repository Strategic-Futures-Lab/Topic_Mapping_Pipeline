package P1_Input;

import PX_Helper.DocumentRow;
import PX_Helper.JSONIOWrapper;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Tom on 12/06/2018.
 */
public class A1_AMIInput
{
    private HashSet<String> meetingNames = new HashSet<>();
    private ConcurrentHashMap<String, DocumentRow> JSONRows = new ConcurrentHashMap<>();

    private String folderRoot;

    /**
     * WARNING: Running this is parallel will alter the final visualisation due to reordering of processing!
     * For now, this is being set to serial! As of 25/06/2018 it is quick enough that a fix is not forthcoming.
     */
    private final static boolean RUN_IN_PARALLEL = false;

    private float timeSlice = 60;

    /**
     * This is an input module used specifically for parsing, processing, and splitting the AMI transcription data.
     * The code can perhaps be used to see how to read and split time-stamped XML data for other tasks as necessary!
     * N.B. This is quite similar to the A1_PDFParser module in structure, just it works with XML rather than PDFs.
     *
     * The main requirement of any input module is to create the initial JSON file which will be used throughout the
     * pipeline. Make sure you fill in the following required fields:
     *
     * row.setID(~); row.setJSONRow(~); row.setIncludedInModel(~);
     *
     *
     * @param args - [XML Root Location] [Output JSON Location] [Time Slice]
     */
    public static void main (String[] args)
    {
        System.out.println( "\n********************************************\n" +
                "*                                          *\n" +
                "* STARTING A1_AMIInput PHASE!              *\n" +
                "*                                          *\n" +
                "* A1_AMIInput:START                        *\n" +
                "*                                          * \n" +
                "********************************************\n");

        A1_AMIInput startClass = new A1_AMIInput();
        startClass.StartAMILoad(args);

        System.out.println( "\n********************************************\n" +
                "*                                          *\n" +
                "* A1_AMIInput PHASE COMPLETE!              *\n" +
                "*                                          *\n" +
                "* A1_AMIInput:END                          *\n" +
                "*                                          * \n" +
                "********************************************\n");
    }

    private void StartAMILoad(String[] args)
    {
        checkArgs(args);
        FindAllMeetings();
        LoadXMLMeetings();
        OutputJSON(args[1]);
    }

    /**
     * All arguments are required, so we check that the user has provided 3, then check we can parse the 3rd into an integer.
     *
     * @param args - The argument list directly from main()
     */
    private void checkArgs(String[] args)
    {
        if(args.length < 3)
        {
            System.out.println("\nArguments missing!\n\nPlease supply arguments in the following order: [XML Root Location] [Output JSON Location] [Time Slice]");
            System.exit(1);
        }
        else
        {
            folderRoot = args[0];

            try
            {
                timeSlice = Float.parseFloat(args[2]);
            }
            catch (NumberFormatException e)
            {
                System.out.println("\nThird argument [Time Slice] needs to be a valid float value!\n\nPlease supply arguments in the following order: [XML Root Location] [Output JSON Location] [Time Slice]");
                System.exit(1);
            }
        }
    }

    /**
     * Open the folder and iterates through all the files within the directory. Every file which is not a directory, we
     * add to a HashSet for later
     */
    private void FindAllMeetings()
    {
        File[] files = new File(folderRoot).listFiles();

        for(File file : files)
        {
            if(!file.isDirectory())
            {
                //Get the 'root' of the meeting name, i.e. the text up until the first full stop character (.)
                meetingNames.add(file.getName().substring(0, file.getName().indexOf('.')));
            }
        }

        System.out.println("File crawl complete...");
    }

    /**
     * Process the HashSet created in FindAllMeetings, passing each meeting root name to LoadSingleXMLMeeting in turn
     */
    private void LoadXMLMeetings()
    {
        if(RUN_IN_PARALLEL)
            meetingNames.parallelStream().forEach(this::LoadSingleXMLMeeting);
            //Non-parallel version:
        else
            meetingNames.forEach(this::LoadSingleXMLMeeting);
    }

    /**
     * This is the main core of this input module. It does the following:
     * 1. Gets all files in the folder which contain the meeting root.
     * 2. Goes through these files, adding the words into a TreeSet which sorts by utterance time.
     * 3. Goes through that TreeSet, recombining the words into strings, splitting them at a certain time.
     *    i.e. it buckets the words into time segments. 0-60 seconds, 60-120 seconds etc.
     * 4. Creates DocumentRows out of these strings, ready to be output in the format the pipeline expects!
     *
     * @param meetingRoot - the root name of the file. In the AMI database each 'meeting' is made up of multiple files,
     *                    one for each participant. So the meeting root would be something like EN2001b, whereas the
     *                    files might be EN2001b.A.words.xml, EN2001b.B.words.xml, EN2001b.C.words.xml etc.
     */
    private void LoadSingleXMLMeeting(String meetingRoot)
    {
        System.out.println("Processing Meeting: " + meetingRoot);

        try
        {
            //Get any file in the folder root that contains that starts with the meeting root. In other words, get all
            //instances of the meeting, as each person is listed separately.
            File[] files = new File(folderRoot).listFiles((d, name) -> name.startsWith(meetingRoot));

            TreeSet<WordTime> words = new TreeSet<>();

            if (files != null)
            {
                for (File file : files)
                {
                    //System.out.println("Processing: " + file.getName());
                    //Parse the XML file using the DocumentBuilder functionality
                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder builder = factory.newDocumentBuilder();
                    Document doc = builder.parse(file);
                    doc.getDocumentElement().normalize();

                    //Get a list of all the words in the XML, tagged with 'w'
                    NodeList nodeList = doc.getElementsByTagName("w");

                    //Get through each node and put them into the TreeSet which should sort them all by time across
                    //the multiple files!
                    for(int i = 0; i < nodeList.getLength(); i++)
                    {
                        //Iterate through the node list and grab any which aren't tagged with 'punc'
                        Node node = nodeList.item(i);
                        if(node.getAttributes().getNamedItem("punc") == null && node.getAttributes().getNamedItem("starttime") != null)
                        {
                            WordTime temp = new WordTime(Float.parseFloat(node.getAttributes().getNamedItem("starttime").getTextContent()),
                                                         node.getTextContent());

                            words.add(temp);
                        }
                    }
                }
            }

            String curString = "";
            float nextSliceTime = 0f;
            int idCount = 0;

            DocumentRow curDoc = new DocumentRow();
            //At this stage we should have a list of words which are time stamped, so now we iterate through the words
            //and split them into different documents!
            for(WordTime word : words)
            {
                //If we have come to a word with a time stamp greater than our next slice time...
                if(word.time >= nextSliceTime)
                {
                    //If we have some words for the document (i.e. this is skipped for the first word) add the document to the JSONRows structure
                    if(curString.length() > 0)
                    {
                        curDoc.setValue("DistColumn", "0");
                        curDoc.setValue("RawText", curString.trim());
                        curDoc.setValue("OriginalDocument", meetingRoot);
                        curDoc.setValue("SplitNumber", String.valueOf(idCount));
                        curDoc.setValue("StartTime", String.valueOf(nextSliceTime));
                        curDoc.setValue("EndTime", String.valueOf(nextSliceTime + timeSlice));
                        JSONRows.put(curDoc.getID(), curDoc);
                        idCount++;
                    }

                    //Then, create a new document, give it an unique ID, and reset the curString to blank!
                    curDoc = new DocumentRow();
                    curDoc.setID(meetingRoot + "_" + idCount);
                    curString = "";
                    nextSliceTime += timeSlice;
                }

                //Finally, we add the new word to the string we're building!
                curString += word.word + " ";
            }

            //System.out.println("Break point!");
        }
        catch (Exception e)
        {
            System.out.println("Something broke!");
            e.printStackTrace();
        }
    }

    /**
     * Finally we use the JSONIOWrapper functionality to save the JSON file in a format which the rest of the pipeline
     * expects!
     *
     * @param JSONFile - the location to save the JSON file to
     */
    private void OutputJSON(String JSONFile)
    {
        int count = 0;
        for(Map.Entry<String, DocumentRow> entry : JSONRows.entrySet())
            entry.getValue().setJSONRow(count++);

        System.out.println("\nNumber of documents created: " + JSONRows.size() + "\n");

        JSONIOWrapper jWrapper = new JSONIOWrapper();
        jWrapper.CreateBlankJSONStructure();
        jWrapper.SetRowData(JSONRows);

        /*
         * THIS IS WHERE THE METADATA IS SET! MAKE SURE TO DO THIS IN YOUR OWN INPUT MODULE
         */
        ConcurrentHashMap<String, String> metadata = new ConcurrentHashMap<>();
        metadata.put("numDocs", "" + JSONRows.size());
        metadata.put("P1_Used", "AMIInput");
        metadata.put("TimeSlice", String.valueOf(timeSlice));
        jWrapper.SetMetadata(metadata);
        /*
         * META DATA ENDS
         */

        jWrapper.SetFailedRetrievals(new ConcurrentHashMap<>());

        jWrapper.SaveJSON(JSONFile);
    }


    /**
     * A simple utility class which allows us to put the time-stamped word data into a TreeSet and have Java's
     * Comparable functionality do all the sorting for us!
     */
    private class WordTime implements Comparable<WordTime>
    {
        public float time;
        public String word;

        public WordTime(float time, String word)
        {
            this.time = time;
            this.word = word;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            WordTime wordTime = (WordTime) o;

            return Float.compare(wordTime.time, time) == 0;
        }

        @Override
        public int hashCode()
        {
            return (time != +0.0f ? Float.floatToIntBits(time) : 0);
        }

        @Override
        public int compareTo(WordTime other)
        {
            return Float.compare(this.time, other.time);
        }
    }
}
