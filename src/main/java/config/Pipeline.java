package config;

import IO.Console;
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

    private Project project;
    private ArrayList<Module> modules;

    /**
     * Constructor, sets up an empty list of module configurations to run
     */
    public Pipeline(){
        modules = new ArrayList<>();
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

    /**
     * Launches the Pipeline execution
     * @throws Exception If one of the module fails
     */
    public void runPipeline() throws Exception{
        try{
            for(Module module: modules){
                switch(module.moduleType){
                    case "corpusCSV":
                        CSVInput.run((CorpusCSV) module, project);
                    case "corpusTXT":
                        TXTInput.run((CorpusTXT) module, project);
                    case "corpusPDF":
                        PDFInput.run((CorpusPDF) module, project);
                    case "corpusCSV":
                        CSVInput.run((CorpusCSV) module, project);
                    case "corpusCSV":
                        CSVInput.run((CorpusCSV) module, project);
                }
            }
        } catch (Exception e){
            throw e;
        }
    }
}
