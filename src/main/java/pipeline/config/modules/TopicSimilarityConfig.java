package pipeline.config.modules;

import IO.Console;
import pipeline.ModuleType;
import pipeline.config.ConfigParser;
import pipeline.config.ModuleConfig;
import pipeline.config.ProjectConfig;

import java.util.HashMap;
import java.util.List;

/**
 * Configuration class for Topic Similarity module
 */
public class TopicSimilarityConfig extends ModuleConfig {

    private static final String[] MANDATORY_PARAMS = new String[]{"topics", "output"};

    /** Filenames of the source topic files */
    public final List<String> topicFiles;
    /** Filename of the output similarity file */
    public final String outputFile;
    /** Method for estimating similarity */
    public final String similarity;
    /** Basis of similarity, documents (true) or words (false) */
    public final boolean useDocuments;

    /**
     * Constructor, parses and stores module parameters
     * @param name Module name as described in the YAML config file
     * @param moduleParams Map of unparsed YAML parameters
     * @throws ConfigParser.ParseException If the configuration does not include all mandatory parameters or if a parameter is not found
     */
    public TopicSimilarityConfig(String name, ModuleType type, HashMap<String, Object> moduleParams, ProjectConfig projectParams) throws ConfigParser.ParseException {
        super(name, type, moduleParams.keySet(), MANDATORY_PARAMS);
        // mandatory parameters
        List<String> files = getPathListParam("topics", moduleParams).stream().map(s->projectParams.dataDirectory+s).toList();
        topicFiles = files.size() > 2 ? files.subList(0,2) : files;
        outputFile = projectParams.dataDirectory+getPathParam("output", moduleParams);
        // optional parameters
        similarity = getDefaultStringParam("similarity", moduleParams, "cosine");
        useDocuments = getDefaultBooleanParam("useDocuments", moduleParams, true);
    }

    public void logConfig(){
        Console.info("Loading topics from:", 1);
        for(String f: topicFiles) Console.info(" - "+f, 2);
        Console.info("Saving to: "+outputFile, 1);
        Console.info("Using "+similarity+" similarity", 1);
        Console.info("Using "+(useDocuments?"documents":"words")+" features", 1);
    }
}
