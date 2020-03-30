package P1_Input;

import PX_Helper.DocumentRow;
import PX_Helper.JSONIOWrapper;
import PX_Helper.Pair;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Tom on 13/06/2018.
 * Modified by P. Le Bras on 03/12/18:
 *      - changed json output format to allow reduce json step
 *      - allowed input folder to comprise of subfolder, each being a different dataset
 *      the dataset value is passed on to the json output
 */
public class A1_PDFParser
{
    private ConcurrentHashMap<String, DocumentRow> JSONRows = new ConcurrentHashMap<>();

    private List<Pair<File,String>> fileList = new ArrayList<>();
    private List<String> datasetList = new ArrayList<>();

    private String folderRoot;

    private int wordsPerDoc = 0;

    /**
     * WARNING: Running this is parallel will alter the final visualisation due to reordering of processing!
     * For now, this is being set to serial! As of 25/06/2018 it is quick enough that a fix is not forthcoming.
     */
    private final static boolean RUN_IN_PARALLEL = false;

    /**
     * This is an input module which can take PDF files, extract the raw text from them, split them into sub documents
     * of specific word lengths, and then create the JSON file required for the rest of the pipeline. This should work
     * with the D3_SubDocumentReduce module in the same way A1_AMIInput does, if you want timeline data, but this has
     * not yet been tested.
     * N.B. This is quite similar to the A1_AMIInput module in structure, just it works with PDFs rather than XML.
     *
     * The main requirement of any input module is to create the initial JSON file which will be used throughout the
     * pipeline. Make sure you fill in the following required fields:
     *
     * row.setID(~); row.setJSONRow(~); row.setIncludedInModel(~);
     *
     *
     * @param args - [PDF Root Location] [Output JSON Location] [Words Per Document]
     */
    public static void main (String[] args)
    {
        System.out.println( "\n********************************************\n" +
                "*                                          *\n" +
                "* STARTING A1_PDFParser PHASE!             *\n" +
                "*                                          *\n" +
                "* A1_PDFParser:START                       *\n" +
                "*                                          * \n" +
                "********************************************\n");

        A1_PDFParser startClass = new A1_PDFParser();
        startClass.StartPDFParse(args);

        System.out.println( "\n********************************************\n" +
                "*                                          *\n" +
                "* A1_PDFParser PHASE COMPLETE!             *\n" +
                "*                                          *\n" +
                "* A1_PDFParser:END                         *\n" +
                "*                                          * \n" +
                "********************************************\n");
    }

    private void StartPDFParse(String[] args)
    {
        checkArgs(args);
        FindAllPDFs();
        ParseAllPDFs();
        //LoadXMLMeetings();
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
            System.out.println("\nArguments missing!\n\nPlease supply arguments in the following order: [PDF Root Location] [Output JSON Location] [Words Per Document]");
            System.exit(1);
        }
        else
        {
            folderRoot = args[0];

            try
            {
                wordsPerDoc = Integer.parseInt(args[2]);
                if(wordsPerDoc < 0)
                    // test updated by P. Le Bras to allow non-subdivision of doc if wordsPerDoc = 0. 16/05/19
                    throw new NumberFormatException("Numerical argument invalid!");
            }
            catch (Exception e)
            {
                System.out.println("\nArguments in valid. The third argument needs to be a valid integer!\n\nPlease supply arguments in the following order: [PDF Root Location] [Output JSON Location] [Words Per Document]");
                System.exit(1);
            }

        }
    }

    /**
     * Open the folder and iterates through all the files within the directory. Every file which is not a directory, we
     * add to a HashSet for later
     * Modified by P. Le Bras on 03/12/18
     * If file is a directory, it assumes it is a dataset and calls FindAllPDFsInSubDirectory.
     */
    private void FindAllPDFs(){
        File directory = new File(folderRoot);
        File[] files = directory.listFiles();

        for(File file : files){
            if(!file.isDirectory()){
                fileList.add(new Pair<>(file,directory.getName()));
            } else {
                FindAllPDFsInSubDirectory(file, file.getName());
            }
        }

        System.out.println("File crawl complete...");
    }

    /**
     * Created by P. Le Bras on 03/12/18
     * Open the folder and iterates through all the files within the directory. Every file which is not a directory, we
     * add to a HashSet for later.
     *
     * @param directory directory to parse to find pdf inputs
     * @param dataset name of dataset to pass to the parser when creating the json info
     */
    private void FindAllPDFsInSubDirectory(File directory, String dataset){
        datasetList.add(dataset);

        File[] files = directory.listFiles();

        for(File file : files){
            if(!file.isDirectory()){
                fileList.add(new Pair<>(file,dataset));
            }
        }
    }

    /**
     * Process the HashSet created in FindAllPDFs, passing each meeting root name to ParseSinglePDF in turn
     */
    private void ParseAllPDFs()
    {
        if(RUN_IN_PARALLEL)
            fileList.parallelStream().forEach(this::ParseSinglePDF);
            //Non-parallel version:
        else
            fileList.forEach(this::ParseSinglePDF);
    }

    /**
     * This is the main core of this input module. It does the following:
     * 1. Opens the file passed to this function and strips the text into a single string, using PDFBox.
     * 2. Splits the string into a list of words, recombining the words into strings of a certain word length.
     *    i.e. it buckets the words into word segments. 0-200 words, 200-400 words etc.
     * 3. Creates DocumentRows out of these strings, ready to be output in the format the pipeline expects!
     *
     * @param pdf - pair comprising of the file name of the PDF to be processed and the dataset value to pass the
     *            json output
     */
    private void ParseSinglePDF(Pair<File,String> pdf)
    {
        File file = pdf.getLeft();
        String dataset = pdf.getRight();
        try
        {
            String rootName = file.getName();
            rootName = rootName.substring(0, rootName.indexOf('.'));

            PDDocument document = PDDocument.load(file);
            System.out.println("Processing document: " + rootName);
            PDFTextStripper pdfTextStripper = new PDFTextStripper();
            String text = pdfTextStripper.getText(document);

            DocumentRow newDoc = new DocumentRow();
            String newDocString = "";
            int numWordsInString = 0;
            int subDocCount = 0;
            for(String word : text.split("\\s+"))
            {
                // test updated by P. Le Bras to allow non-subdivision of doc if wordsPerDoc = 0 - 16/05/19
                if(numWordsInString == wordsPerDoc && wordsPerDoc > 0)
                {
                    newDoc.setValue("RawText", newDocString.trim());
                    newDoc.setValue("OriginalDocument", rootName);
                    newDoc.setValue("DistributionColumn", String.valueOf(0));
                    int startPos = (subDocCount * wordsPerDoc);
                    newDoc.setValue("WordRange", startPos + " - " + (startPos + numWordsInString));
                    /*------------------------------------------------------------*/
                    // Added by P. Le Bras - 03/12/18
                    newDoc.setValue("StartTime", ""+startPos);
                    newDoc.setValue("EndTime", ""+(startPos + numWordsInString));
                    newDoc.setValue("SplitNumber", String.format("%03d", subDocCount));
                    newDoc.setValue("Dataset",dataset);
                    /*------------------------------------------------------------*/
                    newDoc.setIncludedInModel(true);
                    newDoc.setID(rootName + "_" + String.format("%03d", subDocCount));

                    JSONRows.put(newDoc.getID(), newDoc);

                    newDoc = new DocumentRow();
                    numWordsInString = 0;
                    newDocString = "";
                    subDocCount++;
                }

                if(word.length() > 1)
                {
                    newDocString += word + " ";
                    numWordsInString++;
                }
            }

            //Make sure to add the final part too, whatever words are left over!
            newDoc.setValue("RawText", newDocString.trim());
            newDoc.setValue("OriginalDocument", rootName);
            newDoc.setValue("DistributionColumn", String.valueOf(0));
            int startPos = (subDocCount * wordsPerDoc);
            newDoc.setValue("WordRange", startPos + " - " + (startPos + numWordsInString));
            /*------------------------------------------------------------*/
            // Added by P. Le Bras - 03/12/18
            newDoc.setValue("StartTime", ""+startPos);
            newDoc.setValue("EndTime", ""+(startPos + numWordsInString));
            newDoc.setValue("SplitNumber", String.format("%03d", subDocCount));
            newDoc.setValue("Dataset",dataset);
            /*------------------------------------------------------------*/
            newDoc.setIncludedInModel(true);
            newDoc.setID(rootName + "_" + String.format("%03d", subDocCount));

            JSONRows.put(newDoc.getID(), newDoc);

            document.close();
        }
        catch (Exception e)
        {
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
        {
            entry.getValue().setJSONRow(count);
            entry.getValue().setValue("SortValue", String.valueOf(count));
            count++;
        }

        System.out.println("\nNumber of documents created: " + JSONRows.size() + "\n");

        JSONIOWrapper jWrapper = new JSONIOWrapper();
        jWrapper.CreateBlankJSONStructure();
        jWrapper.SetRowData(JSONRows);

        /*
         * THIS IS WHERE THE METADATA IS SET! MAKE SURE TO DO THIS IN YOUR OWN INPUT MODULE
         */
        ConcurrentHashMap<String, String> metadata = new ConcurrentHashMap<>();
        metadata.put("numDocs", "" + JSONRows.size());
        metadata.put("P1_Used", "PDFParser");
        jWrapper.SetMetadata(metadata);
        /*
         * META DATA ENDS
         */

        jWrapper.SetFailedRetrievals(new ConcurrentHashMap<>());

        jWrapper.SaveJSON(JSONFile);
    }
}
