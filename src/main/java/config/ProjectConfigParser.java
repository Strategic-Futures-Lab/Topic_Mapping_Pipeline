package config;

import IO.Console;
import IO.YAMLHelper;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Class for reading and parsing a Topic Map project configuration.
 * Reading (and parsing) is provided as a static method, and returns a singleton instance containing parameters.
 */
public class ProjectConfigParser {

    /**
     * Exception class for parsing errors on the ProjectConfig
     */
    public static class ParseException extends Exception{
        public ParseException(String msg){ super(msg); }
    }

    private static ProjectConfigParser PROJECT_CONFIG;
    private HashMap<String, Object> projectParameters;
    private ArrayList<String> workflow;
    private HashMap<String, Object> modulesParameters;

    private ProjectConfigParser(){
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

    private static ProjectConfigParser getInstance(){
        if(PROJECT_CONFIG == null) PROJECT_CONFIG = new ProjectConfigParser();
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
        try{
            return YAMLHelper.parseMap(o);
        } catch (YAMLHelper.YAMLParseException e){
            throw new ParseException("Config \""+cfg+"\": "+e.getMessage());
        }
    }

    /**
     * Parses an object into an array of Strings
     * @param o The object to parse
     * @param cfg The configuration key being parsed (for logging error)
     * @return The parsed array
     * @throws ParseException If the object is not an ArrayList, or one of the items is not a String
     */
    public static ArrayList<String> parseStringList(Object o , String cfg) throws ParseException{
        try{
            return YAMLHelper.parseStringList(o);
        } catch (YAMLHelper.YAMLParseException e){
            throw new ParseException("Config \""+cfg+"\": "+e.getMessage());
        }
    }

    /**
     * Parses an object into a String
     * @param o The object to parse
     * @param cfg The configuration key being parsed (for logging error)
     * @return The parsed String
     * @throws ParseException If the object is not a String
     */
    public static String parseString(Object o, String cfg) throws ParseException{
        try{
            return YAMLHelper.parseString(o);
        } catch (YAMLHelper.YAMLParseException e){
            throw new ParseException("Config \""+cfg+"\": "+e.getMessage());
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
        try{
            return YAMLHelper.parseInt(o);
        } catch (YAMLHelper.YAMLParseException e){
            throw new ParseException("Config \""+cfg+"\": "+e.getMessage());
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
        try{
            return YAMLHelper.parseDouble(o);
        } catch (YAMLHelper.YAMLParseException e){
            throw new ParseException("Config \""+cfg+"\": "+e.getMessage());
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
        try{
            return YAMLHelper.parseBoolean(o);
        } catch (YAMLHelper.YAMLParseException e){
            throw new ParseException("Config \""+cfg+"\": "+e.getMessage());
        }
    }

    private static void parseConfig(HashMap<String, Object> configMap) throws ParseException{
        Console.log("Parsing configuration file");
        ProjectConfigParser config = getInstance();
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
    public static ProjectConfigParser readConfigFromYAML(String filename) throws ParseException, FileNotFoundException, ClassCastException{
        ProjectConfigParser config = ProjectConfigParser.getInstance();
        try {
            Console.info("Reading project configurations from "+filename);
            HashMap<String,Object> yamlMap = YAMLHelper.loadYAMLFile(filename);
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
