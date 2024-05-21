package config;

import IO.Console;
import config.modules.*;
import pipeline.ModuleType;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Superclass for parsing and storing module parameters, contains a factory static method for creating specialised subclasses
 *
 * @author P. Le Bras
 * @version 1
 */
public class ModuleConfig {

    /** Name of the module, as described in the config file */
    public final String moduleName;
    /** Type of module (internal pipeline name) */
    public final ModuleType moduleType;

    protected ModuleConfig(String moduleName, ModuleType moduleType){
        this.moduleName = moduleName;
        this.moduleType = moduleType;
    }

    /**
     * Factory method for instantiating a specialised module parameter subclass;
     * Uses the enum {@link ModuleType} to retrieve the correct constructor
     * @param moduleName Name of the module as described in the configuration file
     * @param moduleParams Map containing unparsed module parameters
     * @return Subclass instance with specialised parameters
     * @throws ProjectConfigParser.ParseException If the module type is absent or not recognised or if the module subclass threw an exception
     * @throws RuntimeException If the module configuration class instantiation fails
     */
    public static ModuleConfig createModuleConfig(String moduleName, HashMap<String, Object> moduleParams) throws ProjectConfigParser.ParseException, RuntimeException {
        if(moduleParams.containsKey("type")){
            String type = ProjectConfigParser.parseString(moduleParams.get("type"), moduleName+"/type");
            try {
                ModuleType moduleType = ModuleType.getType(type);
                Class configClass = moduleType.config;
                Constructor configCtor = configClass.getConstructor(String.class, ModuleType.class, HashMap.class);
                return (ModuleConfig) configCtor.newInstance(moduleName, moduleType, moduleParams);
            } catch (NoSuchMethodException e) {
                Console.error("Invalid constructor for Module Configuration " + type);
                throw new RuntimeException(e);
            } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
                Console.error("Error while instantiating Module Configuration " + type);
                throw new RuntimeException(e);
            }
        } else {
            throw new ProjectConfigParser.ParseException("Module \""+moduleName+"\" does not have a \"type\" parameter");
        }
    }

    // Methods querying values from the module parameters using the ProjectConfig static methods
    // for simple values (String, boolean, int, double) an additional method allows to get default values in case
    // the parameter is not present in the configuration file
    // for complex values (map, list), you should instantiate an empty structure and query containsKey when needed

    protected String getStringParam(String paramName, HashMap<String, Object> moduleParams) throws ProjectConfigParser.ParseException {
        return ProjectConfigParser.parseString(moduleParams.get(paramName), moduleName +"/"+paramName);
    }

    protected String getDefaultStringParam(String paramName, HashMap<String, Object> moduleParams, String defValue) throws ProjectConfigParser.ParseException {
        if(moduleParams.containsKey(paramName)) return getStringParam(paramName, moduleParams);
        else return defValue;
    }

    protected boolean getBooleanParam(String paramName, HashMap<String, Object> moduleParams) throws ProjectConfigParser.ParseException {
        return ProjectConfigParser.parseBoolean(moduleParams.get(paramName), moduleName +"/"+paramName);
    }

    protected boolean getDefaultBooleanParam(String paramName, HashMap<String, Object> moduleParams, boolean defValue) throws ProjectConfigParser.ParseException {
        if(moduleParams.containsKey(paramName)) return getBooleanParam(paramName, moduleParams);
        else return defValue;
    }

    protected int getIntParam(String paramName, HashMap<String, Object> moduleParams) throws ProjectConfigParser.ParseException {
        return ProjectConfigParser.parseInt(moduleParams.get(paramName), moduleName +"/"+paramName);
    }

    protected int getDefaultIntParam(String paramName, HashMap<String, Object> moduleParams, int defValue) throws ProjectConfigParser.ParseException {
        if(moduleParams.containsKey(paramName)) return getIntParam(paramName, moduleParams);
        else return defValue;
    }

    protected double getDoubleParam(String paramName, HashMap<String, Object> moduleParams) throws ProjectConfigParser.ParseException {
        return ProjectConfigParser.parseDouble(moduleParams.get(paramName), moduleName +"/"+paramName);
    }

    protected double getDefaultDoubleParam(String paramName, HashMap<String, Object> moduleParams, double defValue) throws ProjectConfigParser.ParseException {
        if(moduleParams.containsKey(paramName)) return getDoubleParam(paramName, moduleParams);
        else return defValue;
    }

    protected HashMap<String,Object> getMapParam(String paramName, HashMap<String, Object> moduleParams) throws ProjectConfigParser.ParseException {
        return ProjectConfigParser.parseMap(moduleParams.get(paramName), moduleName +"/"+paramName);
    }

    protected ArrayList<String> getStringListParam(String paramName, HashMap<String, Object> moduleParams) throws ProjectConfigParser.ParseException {
        return ProjectConfigParser.parseStringList(moduleParams.get(paramName), moduleName +"/"+paramName);
    }
}
