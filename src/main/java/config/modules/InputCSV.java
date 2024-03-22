package config.modules;

import IO.ProjectConfig;
import config.Module;

import java.util.HashMap;

public class InputCSV extends Module {

    private static final String[] MANDATORY_PARAMS = new String[]{"source","output","fields"};

    public final String source;
    public final String output;
    public final HashMap<String, String> fields;

    public InputCSV(String moduleName, HashMap<String, Object> moduleParams) throws ProjectConfig.ParseException{
        super(moduleName, "inputCSV");
        for(String p: MANDATORY_PARAMS){
            if(!moduleParams.containsKey(p)) throw new ProjectConfig.ParseException("Module of type \"inputCSV\" must have a \""+p+"\" parameter");
        }
        source = ProjectConfig.parseString(moduleParams.get("source"), moduleName+"/source");
        output = ProjectConfig.parseString(moduleParams.get("output"), moduleName+"/output");
        HashMap<String,Object> fieldsMap = ProjectConfig.parseMap(moduleParams.get("fields"), moduleName+"/fields");
        fields = new HashMap<>();
        for(String k: fieldsMap.keySet()){
            fields.put(k, ProjectConfig.parseString(fieldsMap.get(k), moduleName+"/fields/"+k));
        }
    }
}
