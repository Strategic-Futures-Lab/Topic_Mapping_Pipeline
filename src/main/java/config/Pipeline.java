package config;

import IO.Console;
import IO.ProjectConfig;

import java.util.ArrayList;

public class Pipeline {

    private static final String MODULE_NAME = "Pipeline Configuration";

    private Project project;
    private ArrayList<Module> modules;

    public Pipeline(){
        modules = new ArrayList<>();
    }

    public void loadProjectConfigurations(String configFilename) throws Exception {
        Console.moduleStart(MODULE_NAME);
        try {
            ProjectConfig config = ProjectConfig.readConfigFromYAML(configFilename);
            project = new Project(config.getProjectParameters());
            for(String module: config.getWorkflow()){
                modules.add(Module.createModuleConfig(module, config.getModuleParameters(module)));
            }
        } catch (Exception e){
            Console.error(e.getMessage());
            Console.moduleFail(MODULE_NAME);
            throw e;
        }
        Console.moduleComplete(MODULE_NAME);
    }
}
