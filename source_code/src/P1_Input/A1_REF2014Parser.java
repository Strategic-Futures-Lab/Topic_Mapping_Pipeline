package P1_Input;

import PX_Helper.DocumentRow;
import PX_Helper.JSONIOWrapper;
import de.siegmar.fastcsv.reader.CsvParser;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRow;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Tom on 25/07/2018.
 */
public class A1_REF2014Parser
{

    private ConcurrentHashMap<String, DocumentRow> JSONRows = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, REF2014Data> REF2014Lookup = new ConcurrentHashMap<>();

    private List<File> fileList = new ArrayList<>();

    private String folderRoot, outputFile, CSVFile;

    /**
     * WARNING: Running this is parallel -might- alter the final visualisation due to reordering of processing!
     * For now, this is being set to serial! As of 25/06/2018 it is quick enough that a fix is not forthcoming.
     */
    private final static boolean RUN_IN_PARALLEL = false;

    /**
     * This is an input module specifically designed to load, process, and output the data from the REF2014 in a format
     * that the pipeline expects. In particular, it expects the Overview CSV file to be present, and for the UoA
     * information to be in several folders beneath this.
     *
     * This is a specialised input module! If you want to see how to write a more basic one, check out A1_PDFParser!
     *
     * The main requirement of any input module is to create the initial JSON file which will be used throughout the
     * pipeline. Make sure you fill in the following required fields:
     *
     * row.setID(~); row.setJSONRow(~); row.setIncludedInModel(~);
     *
     *
     * @param args - [PDF Root Location] [Output JSON Location] [CSV Location]
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

        A1_REF2014Parser startClass = new A1_REF2014Parser();
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
        ReadCSVDetails();
        FindAllFolders();
        ParseAllFolders();
        OutputJSON();
    }

    /**
     * All 3 arguments are required, so we simply check that the user has provided 3.
     *
     * @param args - The argument list directly from main()
     */
    private void checkArgs(String[] args)
    {
        if(args.length < 3)
        {
            System.out.println("\nArguments missing!\n\nPlease supply arguments in the following order: [PDF Root Location] [Output JSON Location] [CSV Location]");
            System.exit(1);
        }
        else
        {
            folderRoot = args[0];
            outputFile = args[1];
            CSVFile = args[2];
        }
    }

    /**
     * Load in the Overview CSV which gives us the university and score details, which we then later map to the file
     * names. Here we go through each line of the file, adding it to a HashMap, filling the custom object with the
     * details for each university in each UoA.
     */
    private void ReadCSVDetails()
    {
        String csvFile = CSVFile;
        File file = new File(csvFile);
        CsvReader csvReader = new CsvReader();
        csvReader.setContainsHeader(true);

        int rowNum = 0;

        try (CsvParser csvParser = csvReader.parse(file, StandardCharsets.UTF_8))
        {
            CsvRow row;
            while ((row = csvParser.nextRow()) != null)
            {
                String profile = row.getField("Profile");

                if(profile != null && profile.equals("Environment"))
                {
                    REF2014Data tempRef = new REF2014Data();
                    tempRef.PRN = row.getField("Institution code (UKPRN)");
                    tempRef.MainPanel = row.getField("Main panel");
                    tempRef.InstitutionName = row.getField("Institution name");
                    tempRef._4Star = row.getField("4*");
                    tempRef._3Star = row.getField("3*");
                    tempRef._2Star = row.getField("2*");
                    tempRef._1Star = row.getField("1*");
                    tempRef._Unclassified = row.getField("unclassified");

                    String key = row.getField("Institution code (UKPRN)");
                    String UoA = row.getField("Unit of assessment number");

                    if (key == null)
                    {
                        System.out.println("\n**********\nERROR! Got a null key from the CSV file. STOPPING.\n**********\n");
                        System.exit(1);
                    }
                    else if(UoA == null)
                    {
                        System.out.println("\n**********\nERROR! Got a null UoA field from the CSV file. STOPPING.\n**********\n");
                        System.exit(1);
                    }

                    REF2014Lookup.put(key + "-" + UoA + "-", tempRef);
                }

                rowNum++;
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            System.out.println("Number of rows recovered from file: " + rowNum);
            System.out.println("Total number of Institutions recovered from file: " + REF2014Lookup.size());
        }
    }

    /**
     * Here we find all the folders, each of which represents a UoA. It works similar to the A1_PDFParser, in that we
     * iterate through each file in a directory, but in this case we create a HashSet of all directories, rather than
     * files.
     */
    private void FindAllFolders()
    {
        File[] files = new File(folderRoot).listFiles();

        for(File file : files)
        {
            if(file.isDirectory())
            {
                fileList.add(file);
            }
        }

        System.out.println("File crawl complete...");
    }

    /**
     * Process the HashSet created in FindAllFolders, passing each root folder name to ParseFolder in turn
     */
    private void ParseAllFolders()
    {
        if(RUN_IN_PARALLEL)
            fileList.parallelStream().forEach(this::ParseFolder);
            //Non-parallel version:
        else
            fileList.forEach(this::ParseFolder);
    }

    /**
     * For each folder we then get a list of all of the files in the correct sub-folder, before passing these to another
     * function to process each actual PDF in turn!
     *
     * @param folder - the folder which represents the UoA
     */
    private void ParseFolder(File folder)
    {
        String UoA = folder.getName();
        System.out.println("\nPROCESSING FOLDER: " + UoA);
        String newFolder = Paths.get(folder.getPath() + "/EnvironmentTemplate").toString();

        File[] files = new File(newFolder).listFiles();

        if(files != null)
        {
            for (File pdfFile : files)
                ParseSinglePDF(pdfFile, UoA);

        }
        else
        {
            System.out.println("**********\nERROR! FILE DEREFERENCING FAILED FOR: " + UoA + "\n**********");
            System.exit(1);
        }
    }

    /**
     * This is the main core of this module! It does the following:
     * 1. Loads and strips the text out of the PDF file, getting a single string of text.
     * 2. Creates a DocumentRow object and populates it with the information expected and required for the rest of the
     *    pipeline
     * 3. Queries the HashMap we created in ReadCSVDetails to get additional information about the document (e.g. Uni
     *    name and REF scores)
     * 4. Puts this Document into a HashMap for saving later.
     *
     * @param file
     * @param UoA
     */
    private void ParseSinglePDF(File file, String UoA)
    {
        try
        {
            String rootName = file.getName();
            rootName = rootName.substring(0, rootName.indexOf('.'));

            PDDocument document = PDDocument.load(file);
            System.out.println("Processing document: " + UoA + " - " + rootName);
            PDFTextStripper pdfTextStripper = new PDFTextStripper();
            String text = pdfTextStripper.getText(document);

            DocumentRow newDoc = new DocumentRow();

            newDoc.setID(rootName);
            newDoc.setValue("RawText", text);
            newDoc.setValue("OriginalDocument", rootName);
            newDoc.setValue("UoAString", UoA.substring(4));
            newDoc.setValue("UoAValue", UoA.substring(0, 2));
            newDoc.setValue("DistributionColumn", String.valueOf(0));
            newDoc.setIncludedInModel(true);

            /**
             * NOTE: Some file names (annoyingly) add an A or B suffix to the end, which DO NOT exist in the CSV file.
             * In addition, the UoA numbers are not zero padded, so we can't use simple substring. Instead, we look for
             * the location of the second '-' character.
             */
            int secondPos = rootName.indexOf('-', rootName.indexOf('-') + 1);
            String cleanRoot = rootName.substring(0, secondPos + 1);
            //System.out.println(cleanRoot);
            REF2014Data temp = REF2014Lookup.get(cleanRoot);

            if(temp == null)
            {
                System.out.println("\n**********\nERROR! Failed to find " + cleanRoot + " in the REF2014 Lookup! STOPPING\n**********\n");
                System.exit(1);
            }

            newDoc.setValue("Institution code (UKPRN)", temp.PRN);
            newDoc.setValue("CleanRoot", cleanRoot);
            newDoc.setValue("Institution name", temp.InstitutionName);
            newDoc.setValue("Main panel", temp.MainPanel);
            newDoc.setValue("4*", temp._4Star);
            newDoc.setValue("3*", temp._3Star);
            newDoc.setValue("2*", temp._2Star);
            newDoc.setValue("1*", temp._1Star);
            newDoc.setValue("unclassified", temp._Unclassified);
            newDoc.setValue("URL", "http://strategicfutures.org/demos/REF2014/resources/pdfs/" + rootName + ".pdf");

            if(JSONRows.containsKey(newDoc.getID()))
            {
                System.out.println("**********\nERROR! REPEATED ID NAME IN JSONRows: " + newDoc.getID() + "\n**********");
                System.exit(1);
            }

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
     */
    private void OutputJSON()
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

        jWrapper.SaveJSON(outputFile);
    }

    private class REF2014Data
    {
        public String PRN, InstitutionName, MainPanel, _4Star, _3Star, _2Star, _1Star, _Unclassified;

        public REF2014Data()
        {
        }

        public REF2014Data(String PRN, String institutionName, String mainPanel, String _4Star, String _3Star, String _2Star, String _1Star, String _Unclassified)
        {
            this.PRN = PRN;
            this.InstitutionName = institutionName;
            this.MainPanel = mainPanel;
            this._4Star = _4Star;
            this._3Star = _3Star;
            this._2Star = _2Star;
            this._1Star = _1Star;
            this._Unclassified = _Unclassified;
        }
    }
}


