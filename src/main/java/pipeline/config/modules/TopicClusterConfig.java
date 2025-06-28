package pipeline.config.modules;

import IO.Console;
import pipeline.ModuleType;
import pipeline.config.ConfigParser;
import pipeline.config.ModuleConfig;
import pipeline.config.ProjectConfig;

import java.util.HashMap;

/**
 * Configuration class for Topic Cluster module
 *
 * @author P. Le Bras
 * @version 1
 */
public class TopicClusterConfig extends ModuleConfig {

    private static final String[] MANDATORY_PARAMS = new String[]{"similarities", "output"};

    /** Filename of the source similarity file */
    public final String similaritiesFile;
    /** Filename of the output cluster file */
    public final String outputFile;
    /** Number of clusters to produce, must be between 1 and the number of topics */
    public final int nClusters;
    /** Linkage type to use: min, max or upgma */
    public final String linkage;
    /** Filename of the optional hierarchical topic grouping file, only cluster topics in the same group */
    public final String groupsFile;

    /**
     * Constructor, parses and stores module parameters
     * @param name Module name as described in the YAML config file
     * @param moduleParams Map of unparsed YAML parameters
     * @param projectParams Global project parameters
     * @throws ConfigParser.ParseException If the configuration does not include all mandatory parameters or if a parameter is not found
     */
    public TopicClusterConfig(String name, ModuleType type, HashMap<String, Object> moduleParams, ProjectConfig projectParams) throws ConfigParser.ParseException {
        super(name, type, moduleParams.keySet(), MANDATORY_PARAMS);
        // mandatory parameters
        similaritiesFile = projectParams.dataDirectory + getPathParam("similarities", moduleParams);
        outputFile = projectParams.dataDirectory+getPathParam("output", moduleParams);
        // optional parameters
        nClusters = getDefaultIntParam("clusters", moduleParams, 1);
        linkage = getDefaultStringParam("linkage", moduleParams, "upgma");
        String hierarchy = getDefaultPathParam("hierarchy", moduleParams, null);
        groupsFile = hierarchy == null ? null : projectParams.dataDirectory+hierarchy;
    }

    /**
     * Method logging the module parameters
     */
    public void logConfig(){
        Console.info("Loading topic similarities from: "+similaritiesFile, 1);
        Console.info("Saving to: "+outputFile, 1);
        Console.info("Using "+linkage+" linkage criteria", 1);
        if(groupsFile == null){
            Console.info("Building "+nClusters+" clusters", 1);
        } else {
            Console.info("Clustering topics based on groupings in "+groupsFile, 1);
        }
    }


}
