package P0_Project;

import PX_Data.JSONIOWrapper;
import PY_Helper.LogPrint;
import org.json.simple.JSONObject;

/**
 * Class implementing a Project Manager to read project specifications and distribute them across modules.
 *
 * @author P. Le Bras
 * @version 1
 */
@Deprecated
public class ProjectManager {

    /** Project meta-parameters. */
    public MetaSpecs metaSpecs;

    /** Flag for running the Input module. */
    public boolean runInput;
    /** Flag for running Lemmatise module. */
    public boolean runLemmatise;
    /** Flag for running the Topic Model module. */
    public boolean runModel;
    /** Flag for running the Topic Model Export module. */
    public boolean runTopicModelExport;
    /** Flag for running the Document Infer module. */
    public boolean runDocumentInfer;
    /** Flag for running the Index Label module. */
    public boolean runLabelIndex;
    /** Flag for running the Topic Distribution module. */
    public boolean runTopicDistrib;
    /** Flag for running the Compare Topic Distribution module. */
    public boolean runCompareDistrib;
    /** Flag for running the Topic Cluster module. */
    public boolean runTopicCluster;
    /** Flag for running the Topic Mapping module. */
    public boolean runTopicMap;
    /** Flag for running the Overwrite Map module. */
    public boolean runOverwriteMap;

    /** Input module specifications. */
    public InputModuleSpecs input;
    /** Lemmatise module specifications. */
    public LemmatiseModuleSpecs lemmatise;
    /** Topic Model module specifications. */
    public TopicModelModuleSpecs model;
    /** Topic Model Export module specifications. */
    public TopicModelExportModuleSpecs topicModelExport;
    /** Document Infer module specifications. */
    public DocumentInferModuleSpecs documentInfer;
    /** Label Index module specifications. */
    public LabelIndexModuleSpecs labelIndex;
    /** Topic Distribution module specifications. */
    public TopicDistribModuleSpecs topicDistrib;
    /** Compare Topic Distribution module specifications. */
    public CompareDistributionsModuleSpecs compareDistrib;
    /** Topic Cluster module specifications. */
    public TopicClusterModuleSpecs topicCluster;
    /** Topic Mapping module specifications. */
    public TopicMappingModuleSpecs topicMap;
    /** Overwrite Map module specifications. */
    public OverwriteMapModuleSpecs overwriteMap;

    /**
     * Constructor, also triggers reading the project file and setting up the specs.
     * @param projectFile Filename for the project file.
     */
    public ProjectManager(String projectFile){
        JSONObject projectSpec = JSONIOWrapper.LoadJSON(projectFile, 0);
        getRuns((JSONObject) projectSpec.get("run"));
        getMetaSpecs((JSONObject) projectSpec.getOrDefault("metaParameters", new JSONObject()));
        getSpecs(projectSpec);
    }

    /**
     * Method reading the "run" entry in project file to know which module should be executed.
     * @param specs JSON object attached to "run" in project file.
     */
    private void getRuns(JSONObject specs){
        LogPrint.printNewStep("Getting modules to run", 0);
        runInput = (boolean) specs.getOrDefault("input", false);
        runLemmatise = (boolean) specs.getOrDefault("lemmatise", false);
        runModel = (boolean) specs.getOrDefault("model", false);
        runDocumentInfer = (boolean) specs.getOrDefault("inferDocuments", false);
        runTopicModelExport = (boolean) specs.getOrDefault("exportTopicModel", false);
        runLabelIndex = (boolean) specs.getOrDefault("indexLabels", false);
        runTopicDistrib = (boolean) specs.getOrDefault("distributeTopics", false);
        runCompareDistrib = (boolean) specs.getOrDefault("compareDistributions", false);
        runTopicCluster = (boolean) specs.getOrDefault("clusterTopics", false);
        runTopicMap = (boolean) specs.getOrDefault("mapTopics", false);
        runOverwriteMap = (boolean) specs.getOrDefault("overwriteMap", false);
        LogPrint.printCompleteStep();
    }

    /**
     * Method getting the meta-parameters specifications.
     * @param specs JSON object attached to "metaParameters" in project file.
     */
    private void getMetaSpecs(JSONObject specs){
        LogPrint.printNewStep("Getting project's meta-parameters", 0);
        metaSpecs = new MetaSpecs(specs);
        LogPrint.printCompleteStep();
    }

    /**
     * Method setting up the different module specs instances needed (only for modules to be run).
     * @param projectSpec JSON object from the project file, specific module entries are then distributed.
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
        if(runDocumentInfer){
            documentInfer = new DocumentInferModuleSpecs((JSONObject) projectSpec.get("inferDocuments"), metaSpecs);
        }
        if(runTopicModelExport){
            topicModelExport = new TopicModelExportModuleSpecs((JSONObject) projectSpec.get("exportTopicModel"), metaSpecs);
        }
        if(runLabelIndex){
            labelIndex = new LabelIndexModuleSpecs((JSONObject) projectSpec.get("indexLabels"), metaSpecs);
        }
        if(runTopicDistrib){
            topicDistrib = new TopicDistribModuleSpecs((JSONObject) projectSpec.get("distributeTopics"), metaSpecs);
        }
        if(runCompareDistrib){
            compareDistrib = new CompareDistributionsModuleSpecs((JSONObject) projectSpec.get("compareDistributions"), metaSpecs);
        }
        if(runTopicCluster){
            topicCluster = new TopicClusterModuleSpecs((JSONObject) projectSpec.get("clusterTopics"), metaSpecs);
        }
        if(runTopicMap){
            topicMap = new TopicMappingModuleSpecs((JSONObject) projectSpec.get("mapTopics"), metaSpecs);
        }
        if(runOverwriteMap){
            overwriteMap = new OverwriteMapModuleSpecs((JSONObject) projectSpec.get("overwriteMap"), metaSpecs);
        }
        LogPrint.printCompleteStep();
    }

}