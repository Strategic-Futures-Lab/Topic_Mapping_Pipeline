package config;

import IO.Console;
import config.modules.CorpusCSV;
import input.CSVInput;

import java.util.ArrayList;
import java.util.HashMap;

public class Pipeline {

    private Project project;
    private ArrayList<Module> modules;

    public Pipeline(){
        modules = new ArrayList<>();
    }

    public void loadProjectConfigurations(String configFilename) throws Exception {
        String MODULE_NAME = "Pipeline Configuration";
        Console.moduleStart(MODULE_NAME);
        try {
            ProjectConfigParser config = ProjectConfigParser.readConfigFromYAML(configFilename);
            project = new Project(config.getProjectParameters());
            Console.log("Loading modules parameters");
            for(String module: config.getWorkflow()){
                HashMap<String, Object> moduleParams = config.getModuleParameters(module);
                if(moduleParams.containsKey("run") && !ProjectConfigParser.parseBoolean(moduleParams.get("run"), module+"/run")){
                    Console.warning("Skipping module "+module+" - run set to false", 1);
                } else {
                    Console.log("Configuring module "+module, 1);
                    modules.add(Module.createModuleConfig(module, moduleParams));
                    Console.tick();
                }
            }
        } catch (Exception e){
            Console.error(e.getMessage());
            Console.moduleFail(MODULE_NAME);
            throw e;
        }
        Console.moduleComplete(MODULE_NAME);
    }

    public void runPipeline() throws Exception{
        try{
            for(Module module: modules){
                switch(module.moduleType){
                    case "corpusCSV":
                        CSVInput.run((CorpusCSV) module, project);
                }
            }
        } catch (Exception e){
            throw e;
        }
    }
}
