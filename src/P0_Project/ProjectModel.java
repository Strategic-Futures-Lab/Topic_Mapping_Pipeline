package P0_Project;

import org.json.simple.JSONObject;

public class ProjectModel {
    public String lemmas;
    public String module;
    public String outputDir;
    public ModelSpecs mainModel;
    public ModelSpecs subModel;
    public String documentOutput;

    public void getModelSpecs(JSONObject specs){
        lemmas = (String) specs.get("lemmas");
        module = (String) specs.get("module");
        outputDir = (String) specs.get("outputDir");
        documentOutput = outputDir + specs.get("documentOutput");
        mainModel = new ModelSpecs((JSONObject) specs.get("mainModel"), outputDir);
        if(module.equals("hierarchical")){
            subModel = new ModelSpecs((JSONObject) specs.get("subModel"), outputDir);
        }
    }
}
