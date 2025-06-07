package pipeline.config.modules;

import IO.Console;
import pipeline.ModuleType;
import pipeline.config.ConfigParser;
import pipeline.config.ModuleConfig;
import pipeline.config.ProjectConfig;

import java.util.HashMap;

/**
 * Configuration class for HTMLInput module
 *
 * @author P. Le Bras
 * @version 1
 */
public class InputConfigHTML extends ModuleConfig {

    private static final String[] MANDATORY_PARAMS = new String[]{"source","output","urlField"};

    /** Filename of the source CSV file */
    public final String sourceFile;
    /** Filename of the output corpus JSON file */
    public final String outputFile;
    /** CSV field where the URL of the HTML page can be found */
    public final String urlField;

    /** Name of the corpus */
    public final String corpusName;
    /** List of CSV fields to store in the  corpus JSON file; key is the name stored in the corpus JSON file,
     * value is the name found in the source CSV file */
    public final HashMap<String, String> documentFields;
    /** DOM selector from which to parse text on the HTML file */
    public final String domSelector;

    /**
     * Constructor, parses and stores module parameters
     * @param name Module name as described in the YAML config file
     * @param moduleParams Map of unparsed YAML parameters
     * @param projectParams Global project parameters
     * @throws ConfigParser.ParseException If the configuration does not include all mandatory parameters or if a parameter is not found
     */
    public InputConfigHTML(String name, ModuleType type, HashMap<String, Object> moduleParams, ProjectConfig projectParams) throws ConfigParser.ParseException{
        super(name, type, moduleParams.keySet(), MANDATORY_PARAMS);
        // mandatory parameters
        sourceFile = projectParams.sourceDirectory+getPathParam("source", moduleParams);
        outputFile = projectParams.outputDirectory+getPathParam("output", moduleParams);
        urlField = getStringParam("urlField", moduleParams);
        // optional parameters
        corpusName = getDefaultStringParam("name", moduleParams, name);
        domSelector = getDefaultStringParam("domSelector", moduleParams, "body");
        documentFields = new HashMap<>();
        if(moduleParams.containsKey("fields")){
            HashMap<String,Object> fieldsMap = getMapParam("fields", moduleParams);
            for(String k: fieldsMap.keySet()){
                documentFields.put(k, ConfigParser.parseString(fieldsMap.get(k), moduleName+"/fields/"+k));
            }
        }
    }

    /**
     * Method logging the module parameters
     */
    public void logConfig(){
        Console.info("Reading corpus "+corpusName);
        Console.info("Crawling HTML pages listed in "+sourceFile+" and saving to "+outputFile);
    }
}
