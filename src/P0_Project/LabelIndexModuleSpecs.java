package P0_Project;

import org.json.simple.JSONObject;

/**
 * Class reading and validating parameters for the Label Indexing module ({@link P4_Analysis.LabelIndex.LabelIndexing}).
 *
 * @author P. Le Bras
 * @version 1
 */
public class LabelIndexModuleSpecs {

    /** Filename to document data (from Topic Model or Document Infer modules), optional, defaults to "". */
    public String documents;
    /** Flag for indexing documents, defaults to false if documents = "". */
    public boolean indexDocuments = false;
    /** Flag for indexing documents not referenced in topics, optional, defaults to false. */
    public boolean useAllDocuments = false;
    /** Flag for indexing labels from documents not indexed from topics, optional, defaults to false. */
    public boolean useAllLabels = false;
    /** Filename to main topic data (from Topic Model or Document Infer modules). */
    public String mainTopics;
    /** Filename to sub topic data (from Topic Model or Document Infer modules), optional, defaults to "". */
    public String subTopics;
    /** Flag for indexing sub topics, defaults to false if subTopics = "" or modelType meta-parameter = "simple". */
    public boolean indexSubTopics = false;
    /** Filename for the JSON index file generated. */
    public String indexOutput;

    /**
     * Constructor: parses and validates the given JSON object to set parameters.
     * @param specs JSON object attached to "indexLabels" in project file.
     * @param metaSpecs Meta-parameter specifications.
     */
    public LabelIndexModuleSpecs(JSONObject specs, MetaSpecs metaSpecs) {
        documents = (String) specs.getOrDefault("documents", "");
        indexDocuments = documents.length() > 0;
        if(indexDocuments){
            documents = metaSpecs.getDataDir() + documents;
            useAllDocuments = (boolean) specs.getOrDefault("useAllDocuments", false);
            useAllLabels = (boolean) specs.getOrDefault("useAllLabels", false);
        }
        mainTopics = metaSpecs.getDataDir() + specs.get("mainTopics");
        indexOutput = metaSpecs.getOutputDir() + specs.get("output");
        subTopics = (String) specs.getOrDefault("subTopics", "");
        indexSubTopics = metaSpecs.useMetaModelType() ? metaSpecs.doHierarchical() : subTopics.length() > 0;
        if(indexSubTopics){
            subTopics = metaSpecs.getDataDir() + subTopics;
        }
    }
}
