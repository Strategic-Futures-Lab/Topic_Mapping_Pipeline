package pipeline.config.modules;

import IO.Console;
import pipeline.ModuleType;
import pipeline.config.ConfigParser;
import pipeline.config.ModuleConfig;
import pipeline.config.ProjectConfig;

import java.util.HashMap;

/**
 * Configuration class for TXTInput module
 *
 * @author P. Le Bras
 * @version 1
 */
public class InputConfigTXT extends ModuleConfig {

    private static final String[] MANDATORY_PARAMS = new String[]{"source","output"};

    /** Filename of the source TXT file/directory */
    public final String sourceFile;
    /** Filename of the output corpus JSON file */
    public final String outputFile;

    /** Name of the corpus */
    public final String corpusName;
    /** Flag for considering empty lines as document separators */
    public final boolean splitEmptyLines;

    /**
     * Constructor, parses and stores module parameters
     * @param name Module name as described in the YAML config file
     * @param moduleParams Map of unparsed YAML parameters
     * @param projectParams Global project parameters
     * @throws ConfigParser.ParseException If the configuration does not include all mandatory parameters or if a parameter is not found
     */
    public InputConfigTXT(String name, ModuleType type, HashMap<String, Object> moduleParams, ProjectConfig projectParams) throws ConfigParser.ParseException{
        super(name, type, moduleParams.keySet(), MANDATORY_PARAMS);
        // mandatory parameters
        sourceFile = projectParams.sourceDirectory+getPathParam("source", moduleParams);
        outputFile = projectParams.dataDirectory+getPathParam("output", moduleParams);
        // optional parameters
        corpusName = getDefaultStringParam("name", moduleParams, name);
        splitEmptyLines = getDefaultBooleanParam("emptyLineSplit", moduleParams, false);
    }

    /**
     * Method logging the module parameters
     */
    public void logConfig(){
        Console.info("Reading corpus "+corpusName);
        Console.info("Parsing TXT input from "+sourceFile+" and saving to "+outputFile);
    }
}
