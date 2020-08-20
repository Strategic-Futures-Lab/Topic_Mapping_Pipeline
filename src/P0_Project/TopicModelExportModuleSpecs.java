package P0_Project;

import PX_Data.JSONIOWrapper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class TopicModelExportModuleSpecs {

    /** Filename to main topic data (from Topic Model module) */
    public String mainTopics;
    /** Filename for the JSON main topic model file generated */
    public String mainOutput;
    /** Filename for the CSV main topic model file generated, optional, defaults to "" */
    public String mainOutputCSV;
    /** Flag for exporting main model as CSV, defaults to false if mainOutputCSV = "" */
    public boolean exportMainTopicsCSV;
    /** Filename to sub topic data (from Topic Model module), optional, defaults to "" */
    public String subTopics;
    /** Flag for exporting sub model, defaults to false if subTopics = "" */
    public boolean exportSubTopics;
    /** Filename for the JSON sub topic model file generated, only required if subTopics not empty */
    public String subOutput;
    /** Filename for the CSV sub topic model file generated, only needed if subTopics not empty,
     * optional, defaults to "" */
    public String subOutputCSV;
    /** Flag for exporting sub model as CSV, defaults to false if subTopics = "" and subOutputCSV = "" */
    public boolean exportSubTopicsCSV;
    /** Filename for the CSV merged topic model file generated, optional, defaults to "" */
    public String outputCSV;
    /** Flag for exporting merged model as CSV, defaults to false if outputCSV = "" */
    public boolean exportMergedTopicsCSV;
    /** Filename to the document data (from Topic Model Module) */
    public String documents;
    /** List of fields in docData to keep, optional, defaults to empty  */
    public String[] docFields;
    /** Number of words to identify a topic in csv outputs,
     * optional, defaults to 3 */
    public int numWordId = 3;

    /**
     * Constructor: reads the specification from the "mapTopics" entry in the project file
     * @param specs JSON object attached to "mapTopics"
     */
    public TopicModelExportModuleSpecs(JSONObject specs, MetaSpecs metaSpecs){
        mainTopics = metaSpecs.getDataDir() + (String) specs.get("mainTopics");
        mainOutput = metaSpecs.getOutputDir() + (String) specs.get("mainOutput");
        mainOutputCSV = (String) specs.getOrDefault("mainOutputCSV", "");
        exportMainTopicsCSV = mainOutputCSV.length() > 0;
        if(exportMainTopicsCSV){
            mainOutputCSV = metaSpecs.getOutputDir() + mainOutputCSV;
        }
        subTopics = (String) specs.getOrDefault("subTopics", "");
        exportSubTopics = metaSpecs.useMetaModelType() ? metaSpecs.doHierarchical() : subTopics.length() > 0;
        if(exportSubTopics){
            subTopics = metaSpecs.getDataDir() + subTopics;
            subOutput = metaSpecs.getOutputDir() + (String) specs.get("subOutput");
            subOutputCSV = (String) specs.getOrDefault("subOutputCSV", "");
            exportSubTopicsCSV = subOutputCSV.length() > 0;
            if(exportSubTopicsCSV){
                subOutputCSV = metaSpecs.getOutputDir() + subOutputCSV;
            }
        }
        outputCSV = (String) specs.getOrDefault("outputCSV", "");
        exportMergedTopicsCSV = outputCSV.length() > 0;
        if(exportMergedTopicsCSV){
            outputCSV = metaSpecs.getOutputDir() + outputCSV;
        }
        documents = metaSpecs.getDataDir() + (String) specs.get("documents");
        docFields = metaSpecs.useMetaDocFields() ?
                metaSpecs.docFields :
                JSONIOWrapper.getStringArray((JSONArray) specs.getOrDefault("docFields", new JSONArray()));
        numWordId = Math.toIntExact((long) specs.getOrDefault("numWordId", (long) 3));
    }
}
