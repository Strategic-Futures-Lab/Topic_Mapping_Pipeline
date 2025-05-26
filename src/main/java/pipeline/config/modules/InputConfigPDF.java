package pipeline.config.modules;

import IO.Console;
import pipeline.config.ConfigParser;
import pipeline.config.ModuleConfig;
import pipeline.ModuleType;
import pipeline.config.ProjectConfig;

import java.util.HashMap;

/**
 * Configuration class for PDFInput module
 *
 * @author P. Le Bras
 * @version 1
 */
public class InputConfigPDF extends ModuleConfig {

    private static final String[] MANDATORY_PARAMS = new String[]{"source","output"};

    /** Filename of the source PDF file/directory */
    public final String sourceFile;
    /** Filename of the output corpus JSON file */
    public final String outputFile;
    /** Consider split X pages as a separate document */
    public final int splitPages;

    /**
     * Constructor, parses and stores module parameters
     * @param name Module name as described in the YAML config file
     * @param moduleParams Map of unparsed YAML parameters
     * @param projectParams Global project parameters
     * @throws ConfigParser.ParseException If the configuration does not include all mandatory parameters or if a parameter is not found
     */
    public InputConfigPDF(String name, ModuleType type, HashMap<String, Object> moduleParams, ProjectConfig projectParams) throws ConfigParser.ParseException {
        super(name, type);
        for(String p: MANDATORY_PARAMS){
            if(!moduleParams.containsKey(p)) throw new ConfigParser.ParseException("Module of type \""+moduleType+"\" must have a \""+p+"\" parameter");
        }
        sourceFile = projectParams.sourceDirectory+getPathParam("source", moduleParams);
        outputFile = projectParams.dataDirectory+getPathParam("output", moduleParams);
        splitPages = getDefaultIntParam("splitPages", moduleParams, 0);
    }

    /**
     * Method logging the module parameters
     */
    public void logConfig(){
        Console.info("Reading PDF input from "+sourceFile+" and saving to "+outputFile);
    }
}
