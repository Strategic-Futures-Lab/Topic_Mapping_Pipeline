package IO;

import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Class for reading and parsing a Topic Map project configuration.
 * Reading (and parsing) is provided as a static method, and returns a singleton instance containing parameters.
 */
public class ProjectConfig {

    /**
     * Exception class for parsing errors on the ProjectConfig
     */
    public static class ParseException extends Exception{
        public ParseException(String msg){ super(msg); }
    }

    private static ProjectConfig PROJECT_CONFIG;
    private static final String MODULE_NAME = "Config Parser";
    private HashMap<String, Object> projectParameters;
    private ArrayList<String> workflow;
    private HashMap<String, Object> modulesParameters;

    private ProjectConfig(){
        projectParameters = new HashMap<>();
        workflow = new ArrayList<>();
        modulesParameters = new HashMap<>();
    }

    /**
     * Returns the project's parameters, used to overwrite certain module parameters
     * @return The project's parameters
     */
    public HashMap<String,Object> getProjectParameters(){ return projectParameters; }

    /**
     * Returns the project's workflow, i.e., the list of modules to run
     * @return The list of modules to run
     */
    public ArrayList<String> getWorkflow(){ return workflow; }

    /**
     * Returns the list of parameters for each module to run
     * @return The list of module parameters
     */
    public HashMap<String, Object> getModulesParameters(){ return modulesParameters; }

    private static ProjectConfig getInstance(){
        if(PROJECT_CONFIG == null) PROJECT_CONFIG = new ProjectConfig();
        return PROJECT_CONFIG;
    }

    private static HashMap<String, Object> parseMap(Object o, String cfg) throws ParseException{
        HashMap<String, Object> res = new HashMap<>();
        if(o instanceof HashMap<?,?>){
            HashMap map = (HashMap) o;
            for(Object k: map.keySet()){
                if(k instanceof String){
                    res.put((String)k, map.get(k));
                } else {
                    throw new ParseException("key in \""+cfg+"\" needs to be a string");
                }
            }
        } else {
            throw new ParseException("Config \""+cfg+"\" needs to be a list of key/value pairs");
        }
        return res;
    }

    private static ArrayList<String> parseList(Object o , String cfg) throws ParseException{
        ArrayList<String> res = new ArrayList<>();
        if(o instanceof ArrayList<?>){
            ArrayList list = (ArrayList) o;
            for(Object s: list){
                if(s instanceof String){
                    res.add((String) s);
                } else {
                    throw new ParseException("item in \""+cfg+"\" needs to be a string");
                }
            }
        } else {
            throw new ParseException("Config \""+cfg+"\" needs to be a list of strings");
        }
        return res;
    }

    private static void parseConfig(HashMap<String, Object> configMap) throws ParseException{
        Console.log("Parsing configuration file");
        ProjectConfig config = getInstance();
        if(configMap.containsKey("run")){
            Console.log("Retrieving run workflow", 1);
            config.workflow = parseList(configMap.get("run"), "run");
            Console.tick();
            if(configMap.containsKey("project")){
                Console.log("Retrieving project parameters", 1);
                config.projectParameters = parseMap(configMap.get("project"), "project");
                Console.tick();
            }
            if(config.workflow.isEmpty()){
                Console.warning("Run workflow is empty");
            } else {
                Console.log("Retrieving module parameters", 1);
                for (String m : config.workflow) {
                    if (!configMap.containsKey(m))
                        throw new ParseException("Module " + m + " is part of the run sequence but doesn't have a configuration");
                    else {
                        config.modulesParameters.put(m, configMap.get(m));
                    }
                }
                Console.tick();
            }
        }
        Console.success("Configuration file parsed successfully");
    }

    /**
     * Reads and parses a YAML configuration file
     * @param filename The YAML filename
     * @return The singleton instance of ProjectConfig containing parameters
     * @throws ParseException If the configuration file doesn't conform to expectations
     * @throws FileNotFoundException If the filename provided doesn't refer to an existing file
     */
    public static ProjectConfig readConfigFromYAML(String filename) throws ParseException, FileNotFoundException{
        Console.submoduleStart(MODULE_NAME);
        Yaml yaml = new Yaml();
        ProjectConfig config = ProjectConfig.getInstance();
        try {
            InputStream input = new FileInputStream(filename);
            Console.info("Reading project configurations from "+filename);
            HashMap<String,Object> yamlMap = yaml.load(input);
            parseConfig(yamlMap);
        } catch (FileNotFoundException e){
            Console.error("Project configuration file "+filename+" not found!");
            throw e;
        } catch (ParseException e){
            Console.error(e.getMessage());
            throw e;
        }
        Console.submoduleComplete(MODULE_NAME);
        return config;
    }

}
