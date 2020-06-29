package P0_Project;

import PX_Data.JSONIOWrapper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class MetaSpecs {

    /** top level project directory, optional, defaults to "" */
    public String projectDirectory;

    /** directory for input module source, optional, defaults to "" */
    public String sourceDirectory;

    /** directory for outputs of:
     *  - input,
     *  - lemmatise
     *  - model
     *  - distributeTopics
     *  - clusterTopics
     *  optional, defaults to "" */
    public String dataDirectory;

    /** directory for outputs of:
     *  - exportTopicModel,
     *  - labelIndex
     *  - distributeTopics (separate distribution files)
     *  - mapTopics
     *  optional, defaults to "" */
    public String outputDirectory;

    /** flag for model type, hiearchical or simple, optional, defaults to null, i.e. use module level spec */
    public String modelType;

    /** List of fields in docData to overwrite lists of lemmatise and exportModel modules
     *  optional, defaults to null  */
    public String[] docFields;

    public MetaSpecs(JSONObject specs){
        projectDirectory = completeDirectoryName((String) specs.getOrDefault("projectDir", ""));
        sourceDirectory = completeDirectoryName((String) specs.getOrDefault("sourceDir", ""));
        dataDirectory = completeDirectoryName((String) specs.getOrDefault("dataDir", ""));
        outputDirectory = completeDirectoryName((String) specs.getOrDefault("outputDir", ""));
        modelType = (String) specs.getOrDefault("modelType", null);
        JSONArray fields = (JSONArray) specs.getOrDefault("docFields", null);
        docFields = fields == null ? null : JSONIOWrapper.getStringArray(fields);
    }

    private String completeDirectoryName(String dirName){
        if(dirName.length() > 0 && !dirName.endsWith("/")){
            return dirName+"/";
        }
        return dirName;
    }

    public String getSourceDir(){
        return projectDirectory+sourceDirectory;
    }

    public String getDataDir(){
        return projectDirectory+dataDirectory;
    }

    public String getOutputDir() {
        return projectDirectory + outputDirectory;
    }

    public boolean doHierarchical(){
        return modelType.equals("hierarchical");
    }

    public boolean useMetaModelType(){
        return (modelType.equals("simple") || modelType.equals("hierarchical"));
    }

    public boolean useMetaDocFields(){
        return docFields != null;
    }
}
