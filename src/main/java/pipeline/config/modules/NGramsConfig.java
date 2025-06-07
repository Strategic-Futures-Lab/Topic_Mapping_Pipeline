package pipeline.config.modules;

import IO.Console;
import pipeline.ModuleType;
import pipeline.config.ConfigParser;
import pipeline.config.ModuleConfig;
import pipeline.config.ProjectConfig;

import java.util.HashMap;

/**
 * Configuration class for NGrams module
 *
 * @author P. Le Bras
 * @version 1
 */
public class NGramsConfig extends ModuleConfig {

    private static final String[] MANDATORY_PARAMS = new String[]{"corpus", "nGrams", "analysis"};

    /** Filenames of the source corpus files */
    public final String corpusFile;
    /** Filename of the output corpus file */
    public final String outputFile;
    /** Frequency threshold for considering a nGram */
    public final boolean analysis;
    /** Maximum size of nGrams */
    public final int maxNGramSize;
    /** Filename of nGrams file (build input or analysis output) */
    public final String nGramsFile;

    /**
     * Constructor, parses and stores module parameters
     * @param name Module name as described in the YAML config file
     * @param moduleParams Map of unparsed YAML parameters
     * @param projectParams Global project parameters
     * @throws ConfigParser.ParseException If the configuration does not include all mandatory parameters or if a parameter is not found
     */
    public NGramsConfig(String name, ModuleType type, HashMap<String, Object> moduleParams, ProjectConfig projectParams) throws ConfigParser.ParseException {
        super(name, type, moduleParams.keySet(), MANDATORY_PARAMS);
        // mandatory parameters
        String c = getPathParam("corpus", moduleParams);
        corpusFile = projectParams.dataDirectory+c;
        analysis = getBooleanParam("analysis", moduleParams);
        String n = getPathParam("nGrams", moduleParams);
        nGramsFile = (analysis ? projectParams.outputDirectory : projectParams.sourceDirectory)+n;
        // optional parameters
        outputFile = projectParams.dataDirectory+getDefaultPathParam("output", moduleParams, c);
        maxNGramSize = getDefaultIntParam("size", moduleParams, 3);
        if(maxNGramSize < 2) throw new ConfigParser.ParseException("Module of type \""+moduleType+"\" must have ngram size of 2 or more");
    }

    /**
     * Method logging the module parameters
     */
    public void logConfig(){
        String task = analysis ? "Analysing" : "Building";
        Console.info(task+" nGrams in corpus "+corpusFile);
        if(analysis){
            Console.step("Analysing n-grams up to "+maxNGramSize+" terms", 1);
            Console.step("Saving analysis in "+nGramsFile, 1);
        } else {
            Console.step("Reading n-grams from "+nGramsFile, 1);
            if(!corpusFile.equals(outputFile)) Console.step("Saving in "+outputFile, 1);
        }
    }
}
