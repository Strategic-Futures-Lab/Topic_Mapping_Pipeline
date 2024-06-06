package P0_Project;

import PX_Data.JSONIOWrapper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Class parsing project wide meta-parameters specifications.
 *
 * @author P. Le Bras
 * @version 1
 */
@Deprecated
public class MetaSpecs {

    /** Top level project directory,
     * optional, defaults to "" */
    public String projectDirectory;

    /** Directory for input module source,
     * optional, defaults to "" */
    public String sourceDirectory;

    /** directory for outputs of: input, lemmatise, model, infer, distributeTopics/compareDistribution and clusterTopics,
     *  optional, defaults to "". */
    public String dataDirectory;

    /** directory for outputs of: exportTopicModel, labelIndex, distributeTopics (separate distribution files) and mapTopics/overwriteMap,
     *  optional, defaults to "". */
    public String outputDirectory;

    /** Flag for model type, "hierarchical" or "simple",
     * optional, defaults to null, i.e. use module level spec. */
    public String modelType;

    /** List of fields in documents' docData to overwrite lists of lemmatise and exportModel modules,
     *  optional, defaults to null, i.e. use module level spec. */
    public String[] docFields;

    /**
     * Constructor, parses the given JSON object to set parameters.
     * @param specs JSON specifications.
     */
    public MetaSpecs(JSONObject specs){
        projectDirectory = completeDirectoryName((String) specs.getOrDefault("projectDir", ""));
        sourceDirectory = completeDirectoryName((String) specs.getOrDefault("sourceDir", ""));
        dataDirectory = completeDirectoryName((String) specs.getOrDefault("dataDir", ""));
        outputDirectory = completeDirectoryName((String) specs.getOrDefault("outputDir", ""));
        modelType = (String) specs.getOrDefault("modelType", null);
        JSONArray fields = (JSONArray) specs.getOrDefault("docFields", null);
        docFields = fields == null ? null : JSONIOWrapper.getStringArray(fields);
    }

    /**
     * Method ensuring that a directory name parameter ends with "/".
     * @param dirName Name to check.
     * @return Directory name completed.
     */
    private String completeDirectoryName(String dirName){
        if(dirName.length() > 0 && !dirName.endsWith("/")){
            return dirName+"/";
        }
        return dirName;
    }

    /**
     * Getter method for the source data directory.
     * @return Path to the source data directory.
     */
    public String getSourceDir(){
        return projectDirectory + sourceDirectory;
    }

    /**
     * Getter method for the temporary data directory.
     * @return Path to the temporary data directory.
     */
    public String getDataDir(){
        return projectDirectory + dataDirectory;
    }

    /**
     * Getter method for the output data directory.
     * @return Path to the output data directory.
     */
    public String getOutputDir() {
        return projectDirectory + outputDirectory;
    }

    /**
     * Method indicating the type of model to run.
     * @return Flag for running a hierarchical model (true) or simple model (false).
     */
    public boolean doHierarchical(){
        return modelType.equals("hierarchical");
    }

    /**
     * Method indicating if the model type has been set project wide, and therefore should be used a module level.
     * @return Flag for using the model type project wide (true), or at module level (false).
     */
    public boolean useMetaModelType(){
        return (modelType.equals("simple") || modelType.equals("hierarchical"));
    }

    public boolean useMetaDocFields(){
        return docFields != null;
    }
}
