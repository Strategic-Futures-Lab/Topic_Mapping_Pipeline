package pipeline.config.modules;

import IO.Console;
import pipeline.ModuleType;
import pipeline.config.ConfigParser;
import pipeline.config.ModuleConfig;
import pipeline.config.ProjectConfig;

import java.util.HashMap;
import java.util.List;

/**
 * Configuration class for MergeCorpus module
 *
 * @author P. Le Bras
 * @version 1
 */
public class MergeCorpusConfig extends ModuleConfig {

    private static final String[] MANDATORY_PARAMS = new String[]{"corpora", "output"};

    /** Filenames of the source corpus files */
    public final List<String> corpusFiles;
    /** Filename of the output corpus file */
    public final String outputFile;

    /** List of document fields to keep */
    public final String[] docFields;
    /** Name of the corpus */
    public final String corpusName;

    /**
     * Constructor, parses and stores module parameters
     * @param name Module name as described in the YAML config file
     * @param moduleParams Map of unparsed YAML parameters
     * @param projectParams Global project parameters
     * @throws ConfigParser.ParseException If the configuration does not include all mandatory parameters or if a parameter is not found
     */
    public MergeCorpusConfig(String name, ModuleType type, HashMap<String, Object> moduleParams, ProjectConfig projectParams) throws ConfigParser.ParseException {
        super(name, type, moduleParams.keySet(), MANDATORY_PARAMS);
        // mandatory parameters
        List<String> f = getPathListParam("corpora", moduleParams);
        if(f.size() < 2) throw new ConfigParser.ParseException("Module of type \""+moduleType+"\" must have a more than 1 corpus to merge");
        corpusFiles = f.stream().map(s->projectParams.dataDirectory+s).toList();
        outputFile = projectParams.dataDirectory+getPathParam("output", moduleParams);
        // optional parameters
        corpusName = getDefaultStringParam("name", moduleParams, name);
        if(moduleParams.containsKey("docFields")){
            docFields = getStringListParam("docFields", moduleParams).toArray(new String[0]);
        } else {
            docFields = projectParams.docFields;
        }
    }

    /**
     * Method logging the module parameters
     */
    public void logConfig(){
        Console.info("Merging the following corpora into "+outputFile+":");
        for(String corpus: corpusFiles) Console.info("- "+corpus, 1);
        Console.info("Renaming corpus to "+corpusName);
    }
}
