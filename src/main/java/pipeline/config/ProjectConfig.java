package pipeline.config;

import IO.Console;

import java.util.ArrayList;
import java.util.HashMap;

public class ProjectConfig {

    /** Top level project directory, optional, defaults to "" */
    public final String projectDirectory;

    /** Directory for input sources, optional, defaults to "" */
    public final String sourceDirectory;

    /** directory for temporary output files, optional, defaults to "" */
    public final String dataDirectory;

    /** directory for main output files (model export, label index, topic distribution, topic map) optional, defaults to "" */
    public final String outputDirectory;

    /** List of fields in documents' docData to overwrite lists of lemmatise and exportModel modules,
     *  optional, defaults to null, i.e., use module level configuration */
    public final String[] docFields;


    /**
     * 
     * @param projectParams
     * @throws ConfigParser.ParseException
     */
    public ProjectConfig(HashMap<String, Object> projectParams) throws ConfigParser.ParseException {
        Console.log("Loading project parameters");

        // getting directories
        String sourceDir = "", dataDir = "", outputDir = "";
        if(projectParams.containsKey("directories")){
            HashMap<String, Object> directories = ConfigParser.parseMap(projectParams.get("directories"), "project/directories");
            projectDirectory = ConfigParser.checkDirectory(ConfigParser.parseString(directories.getOrDefault("project", ""), "project/directories/project"));
            sourceDir = ConfigParser.checkDirectory(ConfigParser.parseString(directories.getOrDefault("sources", ""), "project/directories/sources"));
            dataDir = ConfigParser.checkDirectory(ConfigParser.parseString(directories.getOrDefault("data", ""), "project/directories/data"));
            outputDir = ConfigParser.checkDirectory(ConfigParser.parseString(directories.getOrDefault("output", ""), "project/directories/output"));
        } else {
            projectDirectory = "";
        }
        sourceDirectory = projectDirectory + sourceDir;
        dataDirectory = projectDirectory + dataDir;
        outputDirectory = projectDirectory + outputDir;

        // getting document fields
        ArrayList<String> fields = ConfigParser.parseStringList(projectParams.getOrDefault("docFields", new ArrayList<String>()), "project/docFields");
        docFields = fields.isEmpty() ? null : fields.toArray(new String[0]);

        Console.tick();
    }

}
