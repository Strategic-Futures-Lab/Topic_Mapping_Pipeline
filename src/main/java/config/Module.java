package config;

import config.modules.*;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Superclass for parsing and storing module parameters, contains a factory static method for creating specialised subclasses
 */
public class Module {

    /** Name of the module, as described in the config file */
    public final String moduleName;
    /** Type of module (internal pipeline name) */
    public final String moduleType;

    protected Module(String moduleName, String moduleType){
        this.moduleName = moduleName;
        this.moduleType = moduleType;
    }

    /**
     * Factory method for instantiating a specialised module parameter subclass
     * @param moduleName Name of the module as described in the configuration file
     * @param moduleParams Map containing unparsed module parameters
     * @return Subclass instance with specialised parameters
     * @throws ProjectConfigParser.ParseException If the module type is absent or not recognised or if the module subclass threw an exception
     */
    public static Module createModuleConfig(String moduleName, HashMap<String, Object> moduleParams) throws ProjectConfigParser.ParseException {
        if(moduleParams.containsKey("type")){
            String type = ProjectConfigParser.parseString(moduleParams.get("type"), moduleName+"/type");
            switch (type){
                case "corpusCSV":
                    return new CorpusCSV(moduleName, moduleParams);
                case "corpusTXT":
                    return new CorpusTXT(moduleName, moduleParams);
                case "corpusPDF":
                    return new CorpusPDF(moduleName, moduleParams);
                case "corpusHTML":
                    return new CorpusHTML(moduleName, moduleParams);
                case "corpusGTR":
                    return new CorpusGTR(moduleName, moduleParams);
                default:
                    throw new ProjectConfigParser.ParseException("Module type \""+type+"\" is not recognised");
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
