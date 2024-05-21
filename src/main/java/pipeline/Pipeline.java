package pipeline;

import IO.Console;
import config.ModuleConfig;
import config.ProjectConfig;
import config.ProjectConfigParser;
import config.modules.*;
import input.*;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Class representing a pipeline instance - loads and saves configuration, then runs each module required
 *
 * @author P. Le Bras
 * @version 1
 */
public class Pipeline {

    private ProjectConfig projectConfig;
    private ArrayList<ModuleConfig> moduleConfigs;

    /**
     * Constructor, sets up an empty list of module configurations to run
     */
    public Pipeline(){
        moduleConfigs = new ArrayList<>();
    }

    /**
     * Loads the configuration of modules to run
     * @param configFilename File name of the YAML configuration file
     * @throws Exception If the configuration file contains errors
     */
    public void loadProjectConfigurations(String configFilename) throws Exception {
        String MODULE_NAME = "Pipeline Configuration";
        Console.moduleStart(MODULE_NAME);
        try {
            ProjectConfigParser config = ProjectConfigParser.readConfigFromYAML(configFilename);
            projectConfig = new ProjectConfig(config.getProjectParameters());
            Console.log("Loading modules parameters");
            for(String module: config.getWorkflow()){
                HashMap<String, Object> moduleParams = config.getModuleParameters(module);
                if(moduleParams.containsKey("run") && !ProjectConfigParser.parseBoolean(moduleParams.get("run"), module+"/run")){
                    Console.warning("Skipping module "+module+" - run set to false", 1);
                } else {
                    Console.log("Configuring module "+module, 1);
                    moduleConfigs.add(ModuleConfig.createModuleConfig(module, moduleParams));
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

    /**
     * Launches the Pipeline execution
     * @throws Exception If one of the module fails
     */
    public void runPipeline() throws Exception{
        try{
            for(ModuleConfig moduleConfig : moduleConfigs){
                moduleConfig.moduleType.runModule(moduleConfig, projectConfig);
            }
        } catch (Exception e){
            throw e;
        }
    }
}
