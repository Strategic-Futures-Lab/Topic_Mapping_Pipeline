package P0_Project;

import PX_Data.JSONIOWrapper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class DocumentInferModuleSpecs {

    /** Directory where model is located (from meta dataDir), optional, default to "" */
    public String modelDir;
    /** Name of serialised main model */
    public String mainModelName;
    /** Name of serialised sub model, optional, defaults to "" */
    public String subModelName;
    /** Flag for inferring from sub model, defaults to false if subModelName = "" */
    public boolean inferFromSubModel = false;
    /** Filename to lemma data to infer (from Lemmatise module) */
    public String lemmas;
    /** Number of iterations for the inferencer to go through, optional, defaults to 100 */
    public int iterations;
    /** List of fields in docData to keep, optional, defaults to empty  */
    public String[] docFields;
    /** Filename for the JSON inferred document file generated */
    public String documentOutput;
    /** Filename for the CSV inferred document file generated from main model, optional, defaults to "" */
    public String mainOutputCSV;
    /** Flag for exporting inferred document from main model as CSV, defaults to false if documentOutputCSV = "" */
    public boolean exportMainCSV;
    /** Filename for the CSV inferred document file generated from sub model, optional, defaults to "" */
    public String subOutputCSV;
    /** Flag for exporting inferred document from sub model as CSV, defaults to false if documentOutputCSV = "" */
    public boolean exportSubCSV;
    /** Filename for the CSV inferred document file, optional, defaults to "" */
    public String outputCSV;
    /** Flag for exporting inferred documents as CSV, defaults to false if outputCSV = "" */
    public boolean exportMergedTopicsCSV;
    /** Number of words to identify a topic in csv outputs,
     * optional, defaults to 3 */
    public int numWordId;

    public DocumentInferModuleSpecs(JSONObject specs, MetaSpecs metaSpecs){
        lemmas = metaSpecs.getDataDir() + (String) specs.get("lemmas");
        modelDir = metaSpecs.getDataDir() + (String) specs.getOrDefault("modelDir", "");
        if(!modelDir.endsWith("/")){
            modelDir = modelDir + "/";
        }
        mainModelName = (String) specs.get("mainModelName");
        subModelName = (String) specs.getOrDefault("subModelName", "");
        inferFromSubModel = !subModelName.equals("");
        iterations = Math.toIntExact((long) specs.getOrDefault("ietrations", (long) 100));
        docFields = metaSpecs.useMetaDocFields() ?
                metaSpecs.docFields :
                JSONIOWrapper.getStringArray((JSONArray) specs.getOrDefault("docFields", new JSONArray()));
        documentOutput = metaSpecs.getOutputDir() + specs.get("documentOutput");
        mainOutputCSV = (String) specs.getOrDefault("mainOutputCSV", "");
        exportMainCSV = mainOutputCSV.length() > 0;
        if(exportMainCSV){
            mainOutputCSV = metaSpecs.getOutputDir() + mainOutputCSV;
        }
        if(inferFromSubModel){
            subOutputCSV = (String) specs.getOrDefault("subOutputCSV", "");
            exportSubCSV = subOutputCSV.length() > 0;
            if(exportSubCSV){
                subOutputCSV = metaSpecs.getOutputDir() + subOutputCSV;
            }
        }
        outputCSV = (String) specs.getOrDefault("outputCSV", "");
        exportMergedTopicsCSV = outputCSV.length() > 0;
        if(exportMergedTopicsCSV){
            outputCSV = metaSpecs.getOutputDir() + outputCSV;
        }
        numWordId = Math.toIntExact((long) specs.getOrDefault("numWordId", (long) 3));
    }
}
