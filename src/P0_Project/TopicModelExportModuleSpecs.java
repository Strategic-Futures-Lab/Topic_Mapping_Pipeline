package P0_Project;

import PX_Data.JSONIOWrapper;
import PY_Helper.LogPrint;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Class reading and validating parameters for the Model Export module ({@link P3_TopicModelling.ExportTopicModel}).
 *
 * @author P. Le Bras
 * @version 1
 */
public class TopicModelExportModuleSpecs {

    /** Filename of the main topic JSON file
     * (from {@link P3_TopicModelling.TopicModelling} or {@link P3_TopicModelling.InferDocuments}). */
    public String mainTopics;
    /** Filename for the JSON main topic model file generated, optional, defaults to "". */
    public String mainOutput;
    /** Flag for exporting main model as JSON, defaults to false if mainOutput = "". */
    public boolean exportMainTopicsJSON;
    /** Filename for the CSV main topic model file generated, optional, defaults to "". */
    public String mainOutputCSV;
    /** Flag for exporting main model as CSV, defaults to false if mainOutputCSV = "". */
    public boolean exportMainTopicsCSV;
    /** Filename of the sub topic JSON file
     * (from {@link P3_TopicModelling.TopicModelling} or {@link P3_TopicModelling.InferDocuments}),
     * optional, defaults to "". */
    public String subTopics;
    /** Flag for exporting sub model, defaults to false if subTopics = "". */
    public boolean exportSubTopics;
    /** Filename for the JSON sub topic model file generated, only needed if subTopics != "",
     * optional, defaults to "". */
    public String subOutput;
    /** Flag for exporting sub model as JSON, defaults to false if subOutput = "". */
    public boolean exportSubTopicsJSON;
    /** Filename for the CSV sub topic model file generated, only needed if subTopics != "",
     * optional, defaults to "". */
    public String subOutputCSV;
    /** Flag for exporting sub model as CSV, defaults to false if subTopics = "" and subOutputCSV = "". */
    public boolean exportSubTopicsCSV;
    /** Filename for the CSV merged topic model file generated, optional, defaults to "". */
    public String outputCSV;
    /** Flag for exporting merged model as CSV, defaults to false if outputCSV = "". */
    public boolean exportMergedTopicsCSV;
    /** Filename of the document JSON file
     * (from {@link P3_TopicModelling.TopicModelling} or {@link P3_TopicModelling.InferDocuments}). */
    public String documents;
    /** List of fields in docData to keep, optional, defaults to empty. */
    public String[] docFields;
    /** Number of words to identify a topic in csv outputs, optional, defaults to 3. */
    public int numWordId = 3;

    /**
     * Constructor: parses and validates the given JSON object to set parameters.
     * @param specs JSON object attached to "exportTopicModel" in project file.
     * @param metaSpecs Meta-parameter specifications.
     */
    public TopicModelExportModuleSpecs(JSONObject specs, MetaSpecs metaSpecs){
        mainTopics = metaSpecs.getDataDir() + specs.getOrDefault("mainTopics", specs.get("topics"));
        mainOutput = (String) specs.getOrDefault("mainOutput", specs.getOrDefault("output", ""));
        exportMainTopicsJSON = mainOutput.length() > 0;
        if(exportMainTopicsJSON){
            mainOutput = metaSpecs.getOutputDir() + mainOutput;
        }
        mainOutputCSV = (String) specs.getOrDefault("mainOutputCSV", "");
        exportMainTopicsCSV = mainOutputCSV.length() > 0;
        if(exportMainTopicsCSV){
            mainOutputCSV = metaSpecs.getOutputDir() + mainOutputCSV;
        }
        subTopics = (String) specs.getOrDefault("subTopics", "");
        exportSubTopics = metaSpecs.useMetaModelType() ? metaSpecs.doHierarchical() : subTopics.length() > 0;
        if(exportSubTopics){
            subTopics = metaSpecs.getDataDir() + subTopics;
            subOutput = (String) specs.getOrDefault("subOutput", "");
            exportSubTopicsJSON = subOutput.length() > 0;
            if(exportSubTopicsJSON){
                subOutput = metaSpecs.getOutputDir() + subOutput;
            }
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
        documents = metaSpecs.getDataDir() + specs.get("documents");
        docFields = metaSpecs.useMetaDocFields() ?
                metaSpecs.getDocFields() :
                JSONIOWrapper.getStringArray((JSONArray) specs.getOrDefault("docFields", new JSONArray()));
        numWordId = Math.toIntExact((long) specs.getOrDefault("numWordId", (long) 3));

        // validations
        if(numWordId < 1){
            LogPrint.printNote("Topic Model Export module: numWordId must be greater than 0, parameter was set to "+numWordId+", will be set to 1");
            numWordId = 1;
        }
    }
}
