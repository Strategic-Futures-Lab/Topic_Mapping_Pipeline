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
    public HashMap<String, Object> getProjectParameters(){ return projectParameters; }

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

    /**
     * Returns the parameters for a specific module to run
     * @param moduleName The module to query
     * @return The module's parameters
     * @throws ParseException If the module's parameters don't parse has a map
     */
    public HashMap<String, Object> getModuleParameters(String moduleName) throws ParseException{
        return parseMap(modulesParameters.get(moduleName), moduleName);
    }

    private static ProjectConfig getInstance(){
        if(PROJECT_CONFIG == null) PROJECT_CONFIG = new ProjectConfig();
        return PROJECT_CONFIG;
    }

    /**
     * Parses an object into a map String -> Object
     * @param o The object to parse
     * @param cfg The config key being parsed (for logging error)
     * @return The parsed map
     * @throws ParseException If the object is not a map, or one of the key is not a String
     */
    public static HashMap<String, Object> parseMap(Object o, String cfg) throws ParseException{
        HashMap<String, Object> res = new HashMap<>();
        if(o instanceof HashMap<?,?>){
            HashMap map = (HashMap) o;
            for(Object k: map.keySet()){
                if(k instanceof String){
                    res.put((String)k, map.get(k));
                } else {
                    throw new ParseException("Key in \""+cfg+"\" needs to be a string");
                }
            }
        } else {
            throw new ParseException("Config \""+cfg+"\" needs to be a list of key/value pairs");
        }
        return res;
    }

    /**
     * Parses an object into an array of Strings
     * @param o The object to parse
     * @param cfg The configuration key being parsed (for logging error)
     * @return The parsed array
     * @throws ParseException If the object is not an ArrayList, or one of the items is not a String
     */
    public static ArrayList<String> parseStringList(Object o , String cfg) throws ParseException{
        ArrayList<String> res = new ArrayList<>();
        if(o instanceof ArrayList<?>){
            ArrayList list = (ArrayList) o;
            for(Object s: list){
                if(s instanceof String){
                    res.add((String) s);
                } else {
                    throw new ParseException("Item in \""+cfg+"\" needs to be a string");
                }
            }
        } else {
            throw new ParseException("Config \""+cfg+"\" needs to be a list of strings");
        }
        return res;
    }

    /**
     * Parses an object into a String
     * @param o The object to parse
     * @param cfg The configuration key being parsed (for logging error)
     * @return The parsed String
     * @throws ParseException If the object is not a String
     */
    public static String parseString(Object o, String cfg) throws ParseException{
        if(o instanceof String){
            return (String) o;
        } else {
            throw new ParseException("Value for \""+cfg+"\" needs to be a string");
        }
    }

    /**
     * Parses an object into an integer
     * @param o The object to parse
     * @param cfg The configuration key being parsed (for logging error)
     * @return The parsed integer
     * @throws ParseException If the object is not an integer
     */
    public static int parseInt(Object o, String cfg) throws ParseException{
        if(o instanceof Integer){
            return (Integer) o;
        } else {
            throw new ParseException("Value for \""+cfg+"\" needs to be an integer");
        }
    }

    /**
     * Parses an object into a double
     * @param o The object to parse
     * @param cfg The configuration key being parsed (for logging error)
     * @return The parsed double
     * @throws ParseException If the object is not a double
     */
    public static double parseDouble(Object o, String cfg) throws ParseException{
        if(o instanceof Double){
            return (Double) o;
        } else {
            throw new ParseException("Value for \""+cfg+"\" needs to be a double");
        }
    }

    /**
     * Parses an object into a boolean
     * @param o The object to parse
     * @param cfg The configuration key being parsed (for logging error)
     * @return The parsed boolean
     * @throws ParseException If the object is not a boolean
     */
    public static boolean parseBoolean(Object o, String cfg) throws ParseException{
        if(o instanceof Boolean){
            return (Boolean) o;
        } else {
            throw new ParseException("Value for \""+cfg+"\" needs to be a boolean");
        }
    }

    private static void parseConfig(HashMap<String, Object> configMap) throws ParseException{
        Console.log("Parsing configuration file");
        ProjectConfig config = getInstance();
        if(configMap.containsKey("run")){
            Console.log("Retrieving run workflow", 1);
            config.workflow = parseStringList(configMap.get("run"), "run");
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
    }

    /**
     * Reads and parses a YAML configuration file
     * @param filename The YAML filename
     * @return The singleton instance of ProjectConfig containing parameters
     * @throws ParseException If the configuration file doesn't conform to expectations
     * @throws FileNotFoundException If the filename provided doesn't refer to an existing file
     */
    public static ProjectConfig readConfigFromYAML(String filename) throws ParseException, FileNotFoundException, ClassCastException{
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
        } catch (ClassCastException e){
            Console.error("Expected a YAML compatible file, with key/value pairs");
            throw e;
        }
        return config;
    }

}
