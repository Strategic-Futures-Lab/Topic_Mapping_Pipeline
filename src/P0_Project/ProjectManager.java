package P0_Project;

import PX_Helper.JSONIOWrapper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.HashMap;

public class ProjectManager {

    public boolean runInput;
    public ProjectInput input;
    public boolean runLemmatise;
    public ProjectLemmatise lemmatise;
    public boolean runModel;
    public ProjectModel model;

    public ProjectManager(String projectFile){
        JSONObject projectSpec = JSONIOWrapper.LoadJSON(projectFile);
        getRuns((JSONObject) projectSpec.get("run"));
        if(runInput){
            input = new ProjectInput();
            input.getInputSpecs((JSONObject) projectSpec.get("input"));
        }
        if(runLemmatise){
            lemmatise = new ProjectLemmatise();
            lemmatise.getLemmatiseSpecs((JSONObject) projectSpec.get("lemmatise"));
        }
        if(runModel){
            model = new ProjectModel();
            model.getModelSpecs((JSONObject) projectSpec.get("model"));
        }
    }

    private void getRuns(JSONObject specs){
        runInput = (boolean) specs.get("input");
        runLemmatise = (boolean) specs.get("lemmatise");
        runModel = (boolean) specs.get("model");
    }

}