package P0_Project;

import PX_Data.JSONIOWrapper;
import PY_Helper.LogPrint;
import org.json.simple.JSONObject;

/**
 * Project Manager Class:
 *  - reads project specs from file
 *  - distribute specs across modules
 */
public class ProjectManager {

    /** Project meta-parameters */
    public MetaSpecs metaSpecs;
    /** If Input module should run */
    public boolean runInput;
    /** Input module specifications */
    public InputModuleSpecs input;
    /** If Lemmatise module should run */
    public boolean runLemmatise;
    /** Lemmatise module specifications */
    public LemmatiseModuleSpecs lemmatise;
    /** If Topic Model module should run */
    public boolean runModel;
    /** Topic Model module specifications */
    public TopicModelModuleSpecs model;
    /** If Topic Model Export should run */
    public boolean runTopicModelExport;
    /** Topic Model Export module specifications */
    public TopicModelExportModuleSpecs topicModelExport;
    /** If Document Infer module should run */
    public boolean runDocumentInfer;
    /** Document Infer module specifications */
    public DocumentInferModuleSpecs documentInfer;
    /** If Index Label module should run */
    public boolean runLabelIndex;
    /** Label Index module specifications */
    public LabelIndexModuleSpecs labelIndex;
    /** If Topic Distribution module should run */
    public boolean runTopicDistrib;
    /** Topic Distribution module specifications */
    public TopicDistribModuleSpecs topicDistrib;
    /** If Topic Cluster module should run */
    public boolean runTopicCluster;
    /** Topic Cluster module specifications */
    public TopicClusterModuleSpecs topicCluster;
    /** If Topic Mapping module should run */
    public boolean runTopicMap;
    /** Topic Mapping module specifications */
    public TopicMappingModuleSpecs topicMap;

    /**
     * Constructor, also triggers reading the project file and setting up the specs
     * @param projectFile filename for the project file
     */
    public ProjectManager(String projectFile){
        JSONObject projectSpec = JSONIOWrapper.LoadJSON(projectFile, 0);
        getRuns((JSONObject) projectSpec.get("run"));
        getMetaSpecs((JSONObject) projectSpec.getOrDefault("metaParameters", new JSONObject()));
        getSpecs(projectSpec);
    }

    /**
     * Reads the "run" entry in project file to know which module should be executed
     * @param specs JSON object attached to "run" in project file
     */
    private void getRuns(JSONObject specs){
        LogPrint.printNewStep("Getting modules to run", 0);
        runInput = (boolean) specs.get("input");
        runLemmatise = (boolean) specs.get("lemmatise");
        runModel = (boolean) specs.get("model");
        runTopicModelExport = (boolean) specs.get("exportTopicModel");
        runDocumentInfer = (boolean) specs.get("inferDocuments");
        runLabelIndex = (boolean) specs.get("indexLabels");
        runTopicDistrib = (boolean) specs.get("distributeTopics");
        runTopicCluster = (boolean) specs.get("clusterTopics");
        runTopicMap = (boolean) specs.get("mapTopics");
        LogPrint.printCompleteStep();
    }

    private void getMetaSpecs(JSONObject specs){
        LogPrint.printNewStep("Getting project's meta-parameters", 0);
        metaSpecs = new MetaSpecs(specs);
        LogPrint.printCompleteStep();
    }

    /**
     * Sets up the different module specs instances needed (only for modules to be run)
     * @param projectSpec JSON object from the project file, specific module entries are then distributed
     */
    private void getSpecs(JSONObject projectSpec){
        LogPrint.printNewStep("Getting modules parameters", 0);
        if(runInput){
            input = new InputModuleSpecs((JSONObject) projectSpec.get("input"), metaSpecs);
        }
        if(runLemmatise){
            lemmatise = new LemmatiseModuleSpecs((JSONObject) projectSpec.get("lemmatise"), metaSpecs);
        }
        if(runModel){
            model = new TopicModelModuleSpecs((JSONObject) projectSpec.get("model"), metaSpecs);
        }
        if(runTopicModelExport){
            topicModelExport = new TopicModelExportModuleSpecs((JSONObject) projectSpec.get("exportTopicModel"), metaSpecs);
        }
        if(runDocumentInfer){
            documentInfer = new DocumentInferModuleSpecs((JSONObject) projectSpec.get("inferDocuments"), metaSpecs);
        }
        if(runLabelIndex){
            labelIndex = new LabelIndexModuleSpecs((JSONObject) projectSpec.get("indexLabels"), metaSpecs);
        }
        if(runTopicDistrib){
            topicDistrib = new TopicDistribModuleSpecs((JSONObject) projectSpec.get("distributeTopics"), metaSpecs);
        }
        if(runTopicCluster){
            topicCluster = new TopicClusterModuleSpecs((JSONObject) projectSpec.get("clusterTopics"), metaSpecs);
        }
        if(runTopicMap){
            topicMap = new TopicMappingModuleSpecs((JSONObject) projectSpec.get("mapTopics"), metaSpecs);
        }
        LogPrint.printCompleteStep();
    }

}