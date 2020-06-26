package P0_Project;

import org.json.simple.JSONObject;

/**
 * Class for Label Index module project specification
 */
public class LabelIndexModuleSpecs {

    /** Filename to document data (from Topic Model module), optional, defaults to "" */
    public String documents;
    /** Flag for indexing documents, defaults to false if documents = "" */
    public boolean indexDocuments = false;
    /** Flag for indexing documents not referenced in topics, optional, defaults to false */
    public boolean useAllDocuments = false;
    /** Flag for indexing labels from documents not indexed from topics, optional, defaults to false */
    public boolean useAllLabels = false;
    /** Filename to main topic data (from Topic Model module) */
    public String mainTopics;
    /** Filename to sub topic data (from Topic Model module), optional, defaults to "" */
    public String subTopics;
    /** Flag for indexing sub topics, defaults to false if subTopics = "" or modelType meta-parameter = "simple" */
    public boolean indexSubTopics = false;
    /** Filename for the JSON index file generated */
    public String indexOutput;

    /**
     * Constructor: reads the specification from the "indexLabels" entry in the project file
     * @param specs JSON object attached to "indexLabels"
     */
    public LabelIndexModuleSpecs(JSONObject specs, MetaSpecs metaSpecs) {

        documents = (String) specs.getOrDefault("documents", "");
        indexDocuments = documents.length() > 0;
        if(indexDocuments){
            documents = metaSpecs.getDataDir() + documents;
            useAllDocuments = (boolean) specs.getOrDefault("useAllDocuments", false);
            useAllLabels = (boolean) specs.getOrDefault("useAllLabels", false);
        }
        mainTopics = metaSpecs.getDataDir() + (String) specs.get("mainTopics");
        indexOutput = metaSpecs.getOutputDir() + (String) specs.get("output");
        subTopics = (String) specs.getOrDefault("subTopics", "");
        indexSubTopics = metaSpecs.useMetaModelType() ? metaSpecs.doHierarchical() : subTopics.length() > 0;
        if(indexSubTopics){
            subTopics = metaSpecs.getDataDir() + subTopics;
        }
    }
}
