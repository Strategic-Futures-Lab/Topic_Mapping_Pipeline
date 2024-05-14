package config;

import IO.Console;
import IO.ProjectConfig;

import java.util.ArrayList;
import java.util.HashMap;

public class Project {

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
    public final String[] documentFields;

    /** Flag to instruct modules to use project-defined document fields, i.e., if the fields are non-empty */
    public final boolean useProjectDocFields;


    /**
     * 
     * @param projectParams
     * @throws ProjectConfig.ParseException
     */
    public Project(HashMap<String, Object> projectParams) throws ProjectConfig.ParseException {
        Console.log("Loading project parameters");

        // getting directories
        String sourceDir = "", dataDir = "", outputDir = "";
        if(projectParams.containsKey("directories")){
            HashMap<String, Object> directories = ProjectConfig.parseMap(projectParams.get("directories"), "project/directories");
            projectDirectory = checkDirectory(ProjectConfig.parseString(directories.getOrDefault("project", ""), "project/directories/project"));
            sourceDir = checkDirectory(ProjectConfig.parseString(directories.getOrDefault("sources", ""), "project/directories/sources"));
            dataDir = checkDirectory(ProjectConfig.parseString(directories.getOrDefault("data", ""), "project/directories/data"));
            outputDir = checkDirectory(ProjectConfig.parseString(directories.getOrDefault("output", ""), "project/directories/output"));
        } else {
            projectDirectory = "";
        }
        sourceDirectory = projectDirectory + sourceDir;
        dataDirectory = projectDirectory + dataDir;
        outputDirectory = projectDirectory + outputDir;

        // getting document fields
        ArrayList<String> fields = ProjectConfig.parseStringList(projectParams.getOrDefault("docFields", new ArrayList<String>()), "project/docFields");
        documentFields = fields.toArray(new String[0]);
        useProjectDocFields = documentFields.length != 0;

        Console.tick();
    }

    // Ensures a directory names ends with "/"
    private String checkDirectory(String dirName){
        if(!dirName.isEmpty() && !dirName.endsWith("/")){
            return dirName+"/";
        }
        return dirName;
    }

}
