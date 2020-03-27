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
 * Created by Tom on 16/04/2018.
 */
public class A1_GTRCrawler
{
    private ConcurrentHashMap<String, Grant> Grants = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Grant> MissingRows = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, String> MissingReasons = new ConcurrentHashMap<>();

    private int numGrants, grantsProcessed = 0, retries = 0;
    private long startTime;

    private final static int UPDATE_FREQUENCY = 100;
    private final static int PROCESS_MAX_GRANTS = Integer.MAX_VALUE;
    private final static boolean RUN_IN_PARALLEL = true;

    /**
     * This is the first official input module for the SFLP pipeline. This module takes a CSV from GTR as input, and will
     * crawl the online XML API in order to get the additional data needed like Abstract, Impact etc.
     * By default this runs in parallel, but that can be turned off if you are having issues with it.
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
        System.out.println( "\n********************************************\n" +
                            "*                                          *\n" +
                            "* STARTING A1_GTRCrawler PHASE!            *\n" +
                            "*                                          *\n" +
                            "* A1_GTRCrawler:START                      *\n" +
                            "*                                          * \n" +
                            "********************************************\n");

        A1_GTRCrawler startClass = new A1_GTRCrawler();
        startClass.StartGTRCrawl(args);

        System.out.println( "\n********************************************\n" +
                            "*                                          *\n" +
                            "* A1_GTRCrawler PHASE COMPLETE!            *\n" +
                            "*                                          *\n" +
                            "* A1_GTRCrawler:END                        *\n" +
                            "*                                          * \n" +
                            "********************************************\n");
    }

    private void StartGTRCrawl(String[] args)
    {
        checkArgs(args);
        LoadCSVFile(args[0]);
        CrawlForAdditionalData();
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
                Grant grant = new Grant(row.getField("ProjectId"));
                grant.setJSONRow(rowNum++);

                grant.setFundingOrgName(row.getField("FundingOrgName"));
                grant.setProjectReference(row.getField("ProjectReference"));
                grant.setLeadROName(row.getField("LeadROName"));
                grant.setDepartment(row.getField("Department"));
                grant.setProjectCategory(row.getField("ProjectCategory"));
                grant.setPISurname(row.getField("PISurname"));
                grant.setPIFirstName(row.getField("PIFirstName"));
                grant.setPIOtherNames(row.getField("PIOtherNames"));
                grant.setStudentSurname(row.getField("StudentSurname"));
                grant.setStudentFirstName(row.getField("StudentFirstName"));
                grant.setStudentOtherNames(row.getField("StudentOtherNames"));
                grant.setTitle(row.getField("Title"));
                grant.setStartDate(row.getField("StartDate"));
                grant.setEndDate(row.getField("EndDate"));
                grant.setAwardPounds(row.getField("AwardPounds"));
                grant.setExpenditurePounds(row.getField("ExpenditurePounds"));
                grant.setRegion(row.getField("Region"));
                grant.setStatus(row.getField("Status"));
                grant.setGTRProjectURL(row.getField("GTRProjectUrl"));
                grant.setProjectID(row.getField("ProjectId"));
                grant.setFundingOrgID(row.getField("FundingOrgId"));
                grant.setLeadROID(row.getField("LeadROId"));
                grant.setPIID(row.getField("PIId"));

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
     * Runs through each element in the ConcurrentHashMap and triggers an XML crawl for each row, in order to populate
     * the information which is not contained within the CSV, such as fields like Abstract, Impact, and the like.
     *
     * This is done using the Java forEach behaviour, and can be run either in parallel (default) or sequentially.
     */
    private void CrawlForAdditionalData()
    {
        System.out.println("\n**********\nBeginning Online Crawl!\n***********\n" +
                            "Information on progress will appear below shortly:\n");
        startTime = System.currentTimeMillis();

        MissingRows = new ConcurrentHashMap<>();
        MissingReasons = new ConcurrentHashMap<>();

        //Parallel version of the lambada-style code. Please note:
        //1. You need to parallelise the entry set, and pass that to the method
        //2. You should use a ConcurrentHashMap rather than a usual HashMap. Normal HashMap will work (wrapped in a synchronizedMap) but will likely be slower.
        if(RUN_IN_PARALLEL)
            Grants.entrySet().parallelStream().forEach(this::GetAdditionalDataForGrant);
        //Non-parallel version:
        else
            Grants.entrySet().forEach(this::GetAdditionalDataForGrant);

        long timeTaken = (System.currentTimeMillis() - startTime) / (long)1000;
        System.out.println("\n**********\nOnline Crawl Complete!\n***********\n" +
                Math.floorDiv(timeTaken, 60) + " minutes, " + timeTaken % 60 + " seconds.");

        RetryFailedGrants();
    }

    /**
     * If any grants were recorded as having failed for whatever reason (e.g. XML wasn't there, or timeout) then we
     * retry crawling those grants up to three times. If they still fail, we give up on them and list them as failed
     * in the JSON.
     */
    private void RetryFailedGrants()
    {
        while(MissingRows.size() > 0 && retries < 3)
        {
            retries++;
            grantsProcessed = 0;
            numGrants = MissingRows.size();
            startTime = System.currentTimeMillis();

            System.out.println("\n**********\nRetrying Missing Grants!\n***********\n" +
                    MissingRows.size() + " grants failed. Will try to crawl them again. Retry: " + retries + "\n");

            ConcurrentHashMap<String, Grant> prevMissingRows = MissingRows;
            MissingRows = new ConcurrentHashMap<>();
            MissingReasons = new ConcurrentHashMap<>();

            if(RUN_IN_PARALLEL)
                prevMissingRows.entrySet().parallelStream().forEach(this::GetAdditionalDataForGrant);
                //Non-parallel version:
            else
                prevMissingRows.entrySet().forEach(this::GetAdditionalDataForGrant);

            System.out.println("\n**********\nGrant Retry Complete!\n***********\n");
        }

        if(MissingRows.size() > 0)
        {
            System.out.println("\n**********\nWARNING! - SOME GRANTS WERE UNABLE TO BE RETRIEVED AFTER 3 RETRIES!\n***********\nGrant IDs follow:\n");
            MissingRows.forEach((key, value) -> System.out.println(key + " FAILED!"));
        }
        else
        {
            System.out.println("\n**********\nSUCCESS! - All rows were crawled successfully. Please note: this does not necessarily mean they all had information!\n***********\n");
        }
    }

    /**
     * This is the method where the actual crawling happens. It will be passed individual entries from the ConcurrentHashMap
     * and will try to crawl the XML API from GTR. Once it's found the required information, it will update the entry, or
     * list it as failed so it can be retried
     *
     * @param entry - a single entry from the main ConcurrentHashMap which is storing the grants.
     */
    private void GetAdditionalDataForGrant(Map.Entry<String, Grant> entry)
    {
        String ID = entry.getKey();
        Grant grant = entry.getValue();

        String URLString = "http://gtr.rcuk.ac.uk/gtr/api/projects/" + grant.getProjectID();

        try
        {
            if(grantsProcessed % UPDATE_FREQUENCY == 0 && grantsProcessed != 0)
            {
                System.out.println();
                long timeTaken = (System.currentTimeMillis() - startTime) / (long)1000;
                String timeTakenStr = "Time Taken: " + Math.floorDiv(timeTaken, 60) + " minutes, " + timeTaken % 60 + " seconds.";

                float timeLeft = ((float) timeTaken / (float) grantsProcessed) * (numGrants - grantsProcessed);
                String timeToGoStr = "Estimated Remaining Time: " + Math.floor(timeLeft / 60) + " minutes, " + Math.floor(timeLeft % 60) + " seconds.";

                float percentage = (((float) grantsProcessed / (float) numGrants) * 100);

                System.out.println("Processing grant ID: " + ID + " | Number: " + grantsProcessed +
                        " | Percent Complete: " + (Math.round(percentage * 100f) / 100f) + "%");

                System.out.println(timeTakenStr + " | " + timeToGoStr);
            }

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document overviewXML = db.parse(new URL(URLString).openStream());
            overviewXML.getDocumentElement().normalize();

            //Abstract:
            NodeList nList = overviewXML.getElementsByTagName("ns2:abstractText");
            if(nList.getLength() > 0)
                grant.setAbstractText(nList.item(0).getTextContent());
            else
                grant.setAbstractText("");

            //Tech Abstract (this field appears to be rare):
            nList = overviewXML.getElementsByTagName("ns2:techAbstractText");
            if(nList.getLength() > 0)
                grant.setTechAbstractText(nList.item(0).getTextContent());
            else
                grant.setTechAbstractText("");

            //Impact (this field is not in all grants, but more common than tech abstract):
            nList = overviewXML.getElementsByTagName("ns2:potentialImpact");
            if(nList.getLength() > 0)
                grant.setImpactText(nList.item(0).getTextContent());
            else
                grant.setImpactText("");

        }
        catch (Exception e)
        {
            //e.printStackTrace();
            grant.setSuccessfullyRetrieved(false);
            MissingRows.put(ID, grant);
            MissingReasons.put(ID, e.toString());
            System.out.println(e.toString());
        }
        finally
        {
            grantsProcessed++;
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

            row.setValue("FundingOrgName", tempGrant.getFundingOrgName());
            row.setValue("ProjectReference", tempGrant.getProjectReference());
            row.setValue("LeadROName", tempGrant.getLeadROName());
            row.setValue("Department", tempGrant.getDepartment());
            row.setValue("ProjectCategory", tempGrant.getProjectCategory());
            row.setValue("PISurname", tempGrant.getPISurname());
            row.setValue("PIFirstName", tempGrant.getPIFirstName());
            row.setValue("PIOtherNames", tempGrant.getPIOtherNames());
            row.setValue("StudentSurname", tempGrant.getStudentSurname());
            row.setValue("StudentFirstName", tempGrant.getStudentFirstName());
            row.setValue("StudentOtherNames", tempGrant.getStudentOtherNames());
            row.setValue("Title", tempGrant.getTitle());
            row.setValue("StartDate", tempGrant.getStartDate());
            row.setValue("EndDate", tempGrant.getEndDate());
            row.setValue("AwardPounds", tempGrant.getAwardPounds());
            row.setValue("ExpenditurePounds", tempGrant.getExpenditurePounds());
            row.setValue("Region", tempGrant.getRegion());
            row.setValue("Status", tempGrant.getStatus());
            row.setValue("GTRProjectUrl", tempGrant.getGTRProjectURL());
            row.setValue("ProjectId", tempGrant.getProjectID());
            row.setValue("FundingOrgId", tempGrant.getFundingOrgID());
            row.setValue("LeadROId", tempGrant.getLeadROID());
            row.setValue("PIId", tempGrant.getPIID());
            row.setValue("AbstractText", tempGrant.getAbstractText());
            row.setValue("TechAbstractText", tempGrant.getTechAbstractText());
            row.setValue("ImpactText", tempGrant.getImpactText());

            RowData.put(row.getID(), row);
        }

        jWrapper.SetRowData(RowData);

        /*
         * THIS IS WHERE THE METADATA IS SET! MAKE SURE TO DO THIS IN YOUR OWN INPUT MODULE
         */
        ConcurrentHashMap<String, String> metadata = new ConcurrentHashMap<>();
        metadata.put("numDocs", "" + numGrants);
        metadata.put("P1_Used", "GTRCrawler");
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

