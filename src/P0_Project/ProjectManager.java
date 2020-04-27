package P0_Project;

import PX_Data.JSONIOWrapper;
import org.json.simple.JSONObject;

/**
 * Project Manager Class:
 *  - reads project specs from file
 *  - distribute specs across modules
 */
public class ProjectManager {

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
    /** Topic Mapping module spedifications */
    public TopicMappingModuleSpecs topicMap;

    /**
     * Constructor, also triggers reading the project file and setting up the specs
     * @param projectFile filename for the project file
     */
    public ProjectManager(String projectFile){
        JSONObject projectSpec = JSONIOWrapper.LoadJSON(projectFile);
        getRuns((JSONObject) projectSpec.get("run"));
        getSpecs(projectSpec);
    }

    /**
     * Reads the "run" entry in project file to know which module should be executed
     * @param specs JSON object attached to "run" in project file
     */
    private void getRuns(JSONObject specs){
        runInput = (boolean) specs.get("input");
        runLemmatise = (boolean) specs.get("lemmatise");
        runModel = (boolean) specs.get("model");
        runLabelIndex = (boolean) specs.get("indexLabels");
        runTopicDistrib = (boolean) specs.get("distributeTopics");
        runTopicCluster = (boolean) specs.get("clusterTopics");
        runTopicMap = (boolean) specs.get("mapTopics");
    }

    /**
     * Sets up the different module specs instances needed (only for modules to be run)
     * @param projectSpec JSON object from the project file, specific module entries are then distributed
     */
    private void getSpecs(JSONObject projectSpec){
        if(runInput){
            input = new InputModuleSpecs((JSONObject) projectSpec.get("input"));
        }
        if(runLemmatise){
            lemmatise = new LemmatiseModuleSpecs((JSONObject) projectSpec.get("lemmatise"));
        }
        if(runModel){
            model = new TopicModelModuleSpecs((JSONObject) projectSpec.get("model"));
        }
        if(runLabelIndex){
            labelIndex = new LabelIndexModuleSpecs((JSONObject) projectSpec.get("indexLabels"));
        }
        if(runTopicDistrib){
            topicDistrib = new TopicDistribModuleSpecs((JSONObject) projectSpec.get("distributeTopics"));
        }
        if(runTopicCluster){
            topicCluster = new TopicClusterModuleSpecs((JSONObject) projectSpec.get("clusterTopics"));
        }
        if(runTopicMap){
            topicMap = new TopicMappingModuleSpecs((JSONObject) projectSpec.get("mapTopics"));
        }
    }

}