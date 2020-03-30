package P1_Input;

import PX_Helper.DocumentRow;
import PX_Helper.Grant;
import PX_Helper.JSONIOWrapper;
import de.siegmar.fastcsv.reader.CsvParser;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRow;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Azimeh 25/03/2020
 *
 */
public class A1_CSVInput
{
    private ConcurrentHashMap<String, Grant> Grants = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Grant> MissingRows = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, String> MissingReasons = new ConcurrentHashMap<>();

    private int numGrants, grantsProcessed = 0, retries = 0;
    private long startTime;

    private final static int UPDATE_FREQUENCY = 100;
    private final static int PROCESS_MAX_GRANTS = Integer.MAX_VALUE;


    /**
     * This is the first official input module for the SFLP pipeline. This module takes a CSV as input, and will parse all the columns.
     *
     * This first version was developed for Scottish enterprise project and you might need to change parameter names to reflect your needs.
     *
     * The main requirement of any input module is to create the initial JSON file which will be used throughout the
     * pipeline. Make sure you fill in the following required fields:
     *
     * row.setID(~); row.setJSONRow(~); row.setIncludedInModel(~);
     *
     *
     * @param args - [CSV Location] [Output JSON Location]
     */
    public static void main (String[] args)
    {
        System.out.println( "\n**********************************************************\n" +
                            "*                                                          *\n" +
                            "* STARTING A1_CSV Input PHASE!                             *\n" +
                            "*                                                          *\n" +
                            "* A1_CSVInput: START                                       *\n" +
                            "*                                                          *\n" +
                            "************************************************************\n");

        A1_CSVInput startClass = new A1_CSVInput();
        startClass.StartGTRCrawl(args);

        System.out.println( "\n******************************************************\n" +
                            "*                                                      *\n" +
                            "* A1_CSVInput PHASE COMPLETE!                          *\n" +
                            "*                                                      *\n" +
                            "* A1_CSVInput: END                                     *\n" +
                            "*                                                      *\n" +
                            "********************************************************\n");
    }



    private void StartGTRCrawl(String[] args)
    {
        checkArgs(args);
        LoadCSVFile(args[0]);
        OutputJSON(args[1]);
    }

    /**
     * Both arguments are required, so we simply check that the user has provided 2.
     *
     * @param args - The argument list directly from main()
     */
    private void checkArgs(String[] args)
    {
        if(args.length < 2)
        {
            System.out.println("\nArguments missing!\n\nPlease supply arguments in the following order: [CSV Location] [Output JSON Location]");
            System.exit(1);
        }
    }

    /**
     * Runs through the CSV, row by row, to create and populate the initial ConcurrentHashMap with all the data which
     * is available in the CSV file.
     *
     * @param CSVFile - The location of the CSV file to read
     */
    private void LoadCSVFile(String CSVFile)
    {
        String csvFile = CSVFile;
        File file = new File(csvFile);
        CsvReader csvReader = new CsvReader();
        csvReader.setContainsHeader(true);

        int rowNum = 0;

        try (CsvParser csvParser = csvReader.parse(file, StandardCharsets.UTF_8))
        {
            CsvRow row;
            while ((row = csvParser.nextRow()) != null && rowNum < PROCESS_MAX_GRANTS)
            {

                System.out.println("Project ID: " + row.getField("web-scraper-order") );

                //Grant grant = new Grant(row.getField("web-scraper-order"));
                Grant grant = new Grant(row.getField("web-scraper-order"));
                grant.setJSONRow(rowNum++);

                //ScottishEnterprise
                grant.setFundingOrgName(row.getField("FundingOrgName"));
                grant.setProjectReference(row.getField("ProjectReference"));
                grant.setProjectID(row.getField("web-scraper-order"));
                grant.setTitle(row.getField("Title"));
                grant.setProjectURL(row.getField("Title-href"));
                grant.setLeadROName(row.getField("Organisation"));
                grant.setAbstractText(row.getField("Abstract"));
                grant.setAuthorsList(row.getField("Authors"));
                grant.setDate(row.getField("Date"));
                grant.setPublicationType(row.getField("Publication Type"));
                grant.setPublicationStatus(row.getField("Publication Status"));
                grant.setDepartment(row.getField("Department"));
                grant.setNumberofPages(row.getField("Number of Pages"));
                grant.setPublisher(row.getField("Publisher"));
                grant.setJournal(row.getField("Journal"));
                grant.setDownloadLink(row.getField("Download Link-href"));
                grant.setDoiLink(row.getField("doi link-href"));

                Grants.put(grant.getID(), grant);
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            numGrants = Grants.size();
            System.out.println("Number of grants recovered from file: " + numGrants);
        }
    }


    /**
     * The final step of any input module should be to save the information which has been gathered into a JSON file
     * for the rest of the pipeline to use. This can be done, as it is here, by creating a new JSONIOWrapper object,
     * calling CreateBlankJSONStructure() and then proceeding to populate it. Make sure you sent the ID, the JSONRow, and
     * whether each row was successfully retrieved.
     *
     * Any other data can be saved by using the setValue() method, and referenced later in the pipeline by the name
     * given to it. For example, in the next step, you will tell the pipeline what fields to use for the lemma text by
     * passing it one of the names you set.
     *
     * P.S. For completeness, make sure to set what P1 stage you've used and the number of docs in the metadata!
     *
     * @param JSONFile - the location to save the JSON file to
     */
    private void OutputJSON(String JSONFile)
    {
        System.out.println("\n**********\nOutputting Grants to JSON!\n***********\n");

        JSONIOWrapper jWrapper = new JSONIOWrapper();
        jWrapper.CreateBlankJSONStructure();

        ConcurrentHashMap<String, DocumentRow> RowData = new ConcurrentHashMap<>();

        for (Map.Entry<String, Grant> entry : Grants.entrySet())
        {
            DocumentRow row = new DocumentRow();
            Grant tempGrant = entry.getValue();
            
            row.setID(tempGrant.getProjectID());
            row.setJSONRow(tempGrant.getJSONRow());
            row.setIncludedInModel(tempGrant.isSuccessfullyRetrieved());

            row.setValue("ProjectId", tempGrant.getProjectID());
            row.setValue("Title", tempGrant.getTitle());
            row.setValue("ProjectURL" , tempGrant.getProjectURL());
            row.setValue("LeadORName", tempGrant.getLeadROName());
            row.setValue("AbstractText", tempGrant.getAbstractText());
            row.setValue("AuthorsList" , tempGrant.getAuthorsList());
            row.setValue("Date" , tempGrant.getDate());
            row.setValue("PublicationType", tempGrant.getPublicationType());
            row.setValue("PublicationStatus", tempGrant.getPublicationStatus());
            row.setValue("Department", tempGrant.getDepartment());
            row.setValue("NumberOfPages", tempGrant.getNumberofPages());
            row.setValue("Publisher", tempGrant.getPublisher());
            row.setValue("Journal", tempGrant.getJournal());
            row.setValue("DownloadLink", tempGrant.getDownloadLink());
            row.setValue("DoiLink", tempGrant.getDoiLink());

            /*
            //Temp Json Object to store author Urls
            JSONArray tempAuthors = new JSONArray();

            tempGrant.getAuthorsLinks().entrySet().forEach(ent->{
                JSONObject auth = new JSONObject();
                if (!ent.getKey().equalsIgnoreCase("null")){
                    auth.put("name", ent.getKey());
                    auth.put("url", ent.getValue());

                    tempAuthors.add(auth);
                }
            });

            row.setValue("AuthorsURLs", tempAuthors.toString());
            */
            RowData.put(row.getID(), row);
        }

        jWrapper.SetRowData(RowData);

        /*
         * THIS IS WHERE THE METADATA IS SET! MAKE SURE TO DO THIS IN YOUR OWN INPUT MODULE
         */
        ConcurrentHashMap<String, String> metadata = new ConcurrentHashMap<>();
        metadata.put("numDocs", "" + numGrants);
        metadata.put("P1_Used", "CSVInput");
        jWrapper.SetMetadata(metadata);
        /*
         * META DATA ENDS
         */

        ConcurrentHashMap<String, String> failedRetrievals = new ConcurrentHashMap<>();
        MissingRows.forEach((key, value) -> failedRetrievals.put(key, MissingReasons.get(key)));

        jWrapper.SetFailedRetrievals(failedRetrievals);

        jWrapper.SaveJSON(JSONFile);
    }
}

