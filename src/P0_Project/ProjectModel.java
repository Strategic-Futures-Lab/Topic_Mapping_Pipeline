package P0_Project;

import org.json.simple.JSONObject;

public class ProjectModel implements ModuleSpecs {
    public String lemmas;
    public String module;
    public String outputDir;
    public ModelSpecs mainModel;
    public ModelSpecs subModel;
    public String documentOutput;
    public String similarityOutput;
    public boolean outputSimilarity = false;
    public String assignmentOutput;
    public boolean outputAssignment = false;
    public int maxAssign;

    public void getSpecs(JSONObject specs){
        lemmas = (String) specs.get("lemmas");
        module = (String) specs.get("module");
        outputDir = (String) specs.get("outputDir");
        documentOutput = outputDir + specs.get("documentOutput");
        mainModel = new ModelSpecs((JSONObject) specs.get("mainModel"), outputDir);
        if(module.equals("hierarchical")){
            subModel = new ModelSpecs((JSONObject) specs.get("subModel"), outputDir);
            JSONObject hierarchySpecs = (JSONObject) specs.get("hierarchy");
            maxAssign = Math.toIntExact((long) hierarchySpecs.get("maxAssign"));
            similarityOutput = (String) hierarchySpecs.getOrDefault("modelSimOutput", "");
            if(!similarityOutput.equals("")){
                outputSimilarity = true;
                similarityOutput = outputDir+similarityOutput;
            }
            assignmentOutput = (String) hierarchySpecs.getOrDefault("assignmentOutput", "");
            if(!assignmentOutput.equals("")){
                outputAssignment = true;
                assignmentOutput = outputDir+assignmentOutput;
            }
        }
    }
}
