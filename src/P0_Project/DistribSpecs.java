package P0_Project;

import PX_Data.JSONIOWrapper;
import org.json.simple.JSONObject;

import java.util.HashMap;

/**
 * Class reading an validating parameters for a topic distribution.
 *
 * @author P. Le Bras
 * @version 2
 */
public class DistribSpecs {

    /** Name of field in docData to get distribution domain from,
     * optional, defaults to "": no field -> only calculate total weight of topic against valueField. */
    public String fieldName = "";
    /** Separator to use on domain fields to get unique field values (if more than one value stored in a document),
     * optional, defaults to "": no separation. */
    public String fieldSeparator = "";
    /** Filename of JSON distribution file to generate, optional, defaults to "": save in topic data. */
    public String output = "";
    /** Maximum number of unique value weights to save, optional, defaults to -1: all values,
     * if sets to 0: nothing saved but the total topic weight. */
    public int topPerTopic = -1;
    /** Name of field in docData to weight distribution, has to be numerical values,
     * optional, defaults to "": use 1.0 (document count). */
    public String valueField = "";

    /** Filename of the CSV file containing data about the distribution domain,
     * optional, defaults to "": no additional data about the distribution domain. */
    public String domainDataFile = "";
    /** Boolean flag for incorporating data about the distribution domain,
     * only applicable if the distribution is saved in a separate JSON file,
     * defaults to false if domainDataFile = "". */
    public boolean includeDomainData = false;
    /** In the domain data file, which field identifies a domain entry,
     * only needed if domainDataFile != "", defaults to "id". */
    public String domainDataId;
    /** In the domain data file, which field to read and save as data, optional. */
    public HashMap<String, String> domainDataFields;

    /**
     * Constructor: reads the specification from a JSON object passed from TopicDistribModuleSpecs.
     * @param specs JSON object where the distribution specifications are written.
     */
    public DistribSpecs(JSONObject specs, MetaSpecs metaSpecs){
        fieldName = (String) specs.getOrDefault("fieldName", "");
        fieldSeparator = (String) specs.getOrDefault("fieldSeparator", "");
        output = (String) specs.getOrDefault("output", "");
        if(output.length() > 0){
            output = metaSpecs.getOutputDir() + output;
        }
        topPerTopic = Math.toIntExact((long) specs.getOrDefault("topPerTopic", (long) -1));
        valueField = (String) specs.getOrDefault("valueField", "");
        domainDataFile = (String) specs.getOrDefault("domainData", "");
        if(domainDataFile.length() > 0){
            includeDomainData = true;
            domainDataFile = metaSpecs.getSourceDir() + domainDataFile;
            domainDataId = (String) specs.getOrDefault("domainDataId", "id");
            domainDataFields = JSONIOWrapper.getStringMap((JSONObject) specs.get("domainDataFields"));

        }
    }

}
