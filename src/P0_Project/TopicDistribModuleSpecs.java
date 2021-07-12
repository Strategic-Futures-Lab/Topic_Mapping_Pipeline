package P0_Project;

import PX_Data.JSONIOWrapper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Class reading and validating parameters for the Topic Distribution module
 * ({@link P4_Analysis.TopicDistribution.TopicDistribution}).
 *
 * @author P. Le Bras
 * @version 1
 */
public class TopicDistribModuleSpecs {

    /** Filename of document JSON file (from Topic Model or Document Infer modules). */
    public String documents;
    /** Filename of main topic JSON file (from Topic Model or Document Infer modules). */
    public String mainTopics;
    /** Filename for the JSON main topic file generated. */
    public String mainOutput;
    /** Filename to sub topic data (from Topic Model or Document Infer modules), optional, defaults to "". */
    public String subTopics;
    /** Flag for distributing over sub topics, defaults to false if subTopics = "". */
    public boolean distributeSubTopics = false;
    /** Filename for the JSON sub topic file generated, only required if subTopics != "". */
    public String subOutput;
    /** List of specifications for each distribution to calculate. */
    public List<DistribSpecs> fields;

    /**
     * Constructor: parses and validates the given JSON object to set parameters.
     * @param specs JSON object attached to "distributeTopics" in project file.
     * @param metaSpecs Meta-parameter specifications.
     */
    public TopicDistribModuleSpecs(JSONObject specs, MetaSpecs metaSpecs){
        documents = metaSpecs.getDataDir() + specs.get("documents");
        mainTopics = metaSpecs.getDataDir() + specs.getOrDefault("mainTopics", specs.get("topics"));
        mainOutput = metaSpecs.getDataDir() + specs.getOrDefault("mainOutput", specs.get("output"));
        subTopics = (String) specs.getOrDefault("subTopics", "");
        distributeSubTopics = metaSpecs.useMetaModelType() ? metaSpecs.doHierarchical() : subTopics.length() > 0;
        if(distributeSubTopics){
            subTopics = metaSpecs.getDataDir() + subTopics;
            subOutput = metaSpecs.getDataDir() + specs.get("subOutput");
        }
        fields = new ArrayList<>();
        for(JSONObject field: JSONIOWrapper.getJSONObjectArray((JSONArray) specs.get("distributions"))){
            fields.add(new DistribSpecs(field, metaSpecs));
        };
    }

}
