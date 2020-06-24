package P0_Project;

import org.json.simple.JSONObject;

/**
 * Class for Label Index module project specification
 */
public class LabelIndexModuleSpecs {

    /** Filename to main topic data (from Topic Model module) */
    public String mainTopics;
    /** Filename to sub topic data (from Topic Model module), optional, defaults to "" */
    public String subTopics;
    /** Flag for indexing sub topics, defaults to false if subTopics = "" */
    public boolean indexSubTopics = false;
    /** Filename for the JSON index file generated */
    public String indexOutput;

    /**
     * Constructor: reads the specification from the "indexLabels" entry in the project file
     * @param specs JSON object attached to "indexLabels"
     */
    public LabelIndexModuleSpecs(JSONObject specs, MetaSpecs metaSpecs) {
        mainTopics = metaSpecs.getDataDir() + (String) specs.get("mainTopics");
        indexOutput = metaSpecs.getOutputDir() + (String) specs.get("output");
        subTopics = (String) specs.getOrDefault("subTopics", "");
        indexSubTopics = metaSpecs.useMetaModelType() ? metaSpecs.doHierarchical() : subTopics.length() > 0;
        if(indexSubTopics){
            subTopics = metaSpecs.getDataDir() + subTopics;
        }
    }
}
