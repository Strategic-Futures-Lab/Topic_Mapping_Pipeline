package PX_Helper;
import java.util.HashMap;

/**
 * Created by Tom on 17/03/2017.
 */
public class Grant implements java.io.Serializable
{
    private String ID;
    private boolean SuccessfullyRetrieved = true;
    private int JSONRow;

    private String fundingOrgName;
    private String projectReference;
    private String leadROName;
    private String department;
    private String projectCategory;
    private String PISurname;
    private String PIFirstName;
    private String PIOtherNames;
    private String studentSurname;
    private String studentFirstName;
    private String studentOtherNames;
    private String title;
    private String startDate;
    private String endDate;
    private String awardPounds;
    private String expenditurePounds;
    private String region;
    private String status;
    private String GTRProjectURL;
    private String projectID;
    private String fundingOrgID;
    private String leadROID;
    private String PIID;

    //4 Hubs project
    private String Investigator;
    private String Hubs;

    private String projectURL;
    private String authorsList;
    private String date;
    private String publicationType;
    private String publicationStatus;
    private String numberofPages;
    private String publisher;
    private String journal;
    private String downloadLink;
    private String doiLink;

    private HashMap<String, String> authorsLinks;


    private String abstractText;
    private String techAbstractText;
    private String impactText;

    /**
     * Force a Grant to be initialised with at least an ID!
     * @param ID - ID should be the same as the one used for the HashMap key!
     */
    public Grant(String ID)
    {
        this.setID(ID);
    }

    public String getID()
    {
        return ID;
    }
    public void setID(String ID)
    {
        this.ID = ID;
    }

    public int getJSONRow() { return JSONRow; }
    public void setJSONRow(int JSONRow) { this.JSONRow = JSONRow; }

    public boolean isSuccessfullyRetrieved() { return SuccessfullyRetrieved; }
    public void setSuccessfullyRetrieved(boolean successfullyRetrieved) { SuccessfullyRetrieved = successfullyRetrieved; }


    public String getFundingOrgName()
    {
        return fundingOrgName;
    }
    public void setFundingOrgName(String fundingOrgName) { this.fundingOrgName = fundingOrgName; }

    public String getProjectReference() { return projectReference; }
    public void setProjectReference(String projectReference) { this.projectReference = projectReference; }

    public String getLeadROName()
    {
        return leadROName;
    }
    public void setLeadROName(String leadROName)
    {
        this.leadROName = leadROName;
    }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getProjectCategory() { return projectCategory; }
    public void setProjectCategory(String projectCategory) { this.projectCategory = projectCategory; }

    public String getPISurname() { return PISurname; }
    public void setPISurname(String PISurname) { this.PISurname = PISurname; }

    public String getPIFirstName() { return PIFirstName; }
    public void setPIFirstName(String PIFirstName) { this.PIFirstName = PIFirstName; }

    public String getPIOtherNames() { return PIOtherNames; }
    public void setPIOtherNames(String PIOtherNames) { this.PIOtherNames = PIOtherNames; }

    public String getStudentSurname() { return studentSurname; }
    public void setStudentSurname(String studentSurname) { this.studentSurname = studentSurname; }

    public String getStudentFirstName() { return studentFirstName; }
    public void setStudentFirstName(String studentFirstName) { this.studentFirstName = studentFirstName; }

    public String getStudentOtherNames() { return studentOtherNames; }
    public void setStudentOtherNames(String studentOtherNames) { this.studentOtherNames = studentOtherNames; }

    public String getTitle()
    {
        return title;
    }
    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getStartDate()
    {
        return startDate;
    }
    public void setStartDate(String startDate)
    {
        this.startDate = startDate;
    }

    public String getEndDate()
    {
        return endDate;
    }
    public void setEndDate(String endDate)
    {
        this.endDate = endDate;
    }

    public String getAwardPounds()
    {
        return awardPounds;
    }
    public void setAwardPounds(String awardPounds)
    {
        this.awardPounds = awardPounds;
    }

    public String getExpenditurePounds()
    {
        return expenditurePounds;
    }
    public void setExpenditurePounds(String expenditurePounds)
    {
        this.expenditurePounds = expenditurePounds;
    }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getGTRProjectURL()
    {
        return GTRProjectURL;
    }
    public void setGTRProjectURL(String GTRProjectURL)
    {
        this.GTRProjectURL = GTRProjectURL;
    }

    public String getProjectID() {
        return projectID; }
    public void setProjectID(String projectID) { this.projectID = projectID; }

    public String getFundingOrgID() { return fundingOrgID; }
    public void setFundingOrgID(String fundingOrgID) { this.fundingOrgID = fundingOrgID; }

    public String getLeadROID() { return leadROID; }
    public void setLeadROID(String leadROID) { this.leadROID = leadROID; }

    public String getPIID() { return PIID; }
    public void setPIID(String PIID) { this.PIID = PIID; }

    public String getAbstractText()
    {
        return abstractText;
    }
    public void setAbstractText(String abstractText)
    {
        this.abstractText = abstractText;
    }

    public String getTechAbstractText() { return techAbstractText; }
    public void setTechAbstractText(String techAbstractText)
    {
        this.techAbstractText = techAbstractText;
    }

    public String getImpactText()
    {
        return impactText;
    }
    public void setImpactText(String impactText)
    {
        this.impactText = impactText;
    }



    // 4Hubs project////////////////////////
    public String getInvestigator() { return Investigator; }
    public void setInvestigator(String Investigator){ this.Investigator = Investigator; }

    public String getHubs() { return Hubs; }
    public void setHubs(String Hubs) { this.Hubs = Hubs; }

    //////////////////////////// Scottish enterprise ////////////////////////////
    public String getProjectURL()
    {
        return projectURL;
    }
    public void setProjectURL(String projectURL)
    {
        this.projectURL = projectURL;
    }

    public String getAuthorsList() { return authorsList; }
    public void setAuthorsList(String authorsList)
    {
        this.authorsList = authorsList;
    }

    public String getDate()
    {
        return date;
    }
    public void setDate(String date)
    {
        this.date = date;
    }

    public String getPublicationType()
    {
        return publicationType;
    }
    public void setPublicationType(String publicationType)
    {
        this.publicationType = publicationType;
    }

    public String getPublicationStatus()
    {
        return publicationStatus;
    }
    public void setPublicationStatus(String publicationStatus)
    {
        this.publicationStatus = publicationStatus;
    }

    public String getNumberofPages()
    {
        return numberofPages;
    }
    public void setNumberofPages(String numberofPages)
    {
        this.numberofPages = numberofPages;
    }

    public String getPublisher()
    {
        return publisher;
    }
    public void setPublisher(String publisher)
    {
        this.publisher = publisher;
    }

    public String getJournal()
    {
        return journal;
    }
    public void setJournal(String journal)
    {
        this.journal = journal;
    }

    public String getDownloadLink()
    {
        return downloadLink;
    }
    public void setDownloadLink(String downloadLink)
    {
        this.downloadLink = downloadLink;
    }

    public String getDoiLink()
    {
        return doiLink;
    }
    public void setDoiLink(String doiLink)
    {
        this.doiLink = doiLink;
    }


    public HashMap<String, String> getAuthorsLinks() { return authorsLinks; }
    public void setAuthorsLinks(HashMap authorsLinks) { this.authorsLinks = authorsLinks; }
}
