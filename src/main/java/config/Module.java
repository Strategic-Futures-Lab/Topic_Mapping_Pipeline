package config;

import IO.ProjectConfig;
import config.modules.InputCSV;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

public class Module {

    private static final ArrayList<String> MODULE_TYPES = new ArrayList<>(Arrays.asList("inputCSV","inputTXT"));

    public final String name;
    public final String type;

    protected Module(String moduleName, String moduleType){
        name = moduleName;
        type = moduleType;
    }

    public static Module createModuleConfig(String moduleName, HashMap<String, Object> moduleParams) throws ProjectConfig.ParseException {
        if(moduleParams.containsKey("type")){
            String type = ProjectConfig.parseString(moduleParams.get("type"), moduleParams+"/type");
            switch (type){
                case "inputCSV":
                    return new InputCSV(moduleName, moduleParams);
                default:
                    throw new ProjectConfig.ParseException("Module type \""+type+"\" is not recognised");
            }
        } else {
            throw new ProjectConfig.ParseException("Module \""+moduleName+"\" does not have a \"type\" parameter");
        }
    }
}
