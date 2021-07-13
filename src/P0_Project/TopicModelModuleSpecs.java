package P0_Project;

import PY_Helper.LogPrint;
import org.json.simple.JSONObject;

/**
 * Class reading and validating parameters for the Topic Model modules ({@link P3_TopicModelling.TopicModelling} and
 * {@link P3_TopicModelling.HierarchicalTopicModelling}).
 *
 * @author P. Le Bras
 * @version 2
 */
public class TopicModelModuleSpecs {

    /** Filename to lemma data (from {@link P2_Lemmatise.Lemmatise}). */
    public String lemmas;
    /** Which module to run: "simple" or "hierarchical". */
    public String modelType;
    /** Directory name in which to write the topic model data
     * (can be same as corpus or lemma, or a level lower to contain all files in one place). */
    public String dataDir;
    /** Model specifications for the topic model (main topic model if "hierarchical" module used). */
    public ModelSpecs mainModel;
    /** Model specifications for the sub topic model (if "hierarchical" module used). */
    public ModelSpecs subModel;
    /** Filename for the JSON document file generated, not including directory. */
    public String documentOutput;
    /** Filename for the CSV similarity file, not including directory:
     * similarity between main and sub topic model if "hierarchical" module used,
     * optional, defaults to "". */
    public String similarityOutput;
    /** Flag for writing similarity between main and sub topic model if "hierarchical" module used,
     * defaults to false if similarityOuput = "". */
    public boolean outputSimilarity = false;
    /** Filename for the CSV assignment file, not including directory:
     * assignment between main and sub topics if "hierarchical" module used, optional, defaults to "". */
    public String assignmentOutput;
    /** Flag for writing assignment between main and sub topics if "hierarchical" module used,
     * defaults to false if assignmentOutput = "". */
    public boolean outputAssignment = false;
    /** Maximum number of times a sub topic gets assigned to main topics, if "hierarchical" module used,
     * optional, defaults to 1. */
    public int maxAssign;
    /** Type of similarity to use for assignment, if "hierarchical" module used, optional, defaults to "perceptual".
     * "perceptual" will use the topics' labels overlap. "document" will use the topics' distribution in document space. */
    public String assignmentType;

    /**
     * Constructor: parses and validates the given JSON object to set parameters.
     * @param specs JSON object attached to "model" in project file.
     * @param metaSpecs Meta-parameter specifications.
     */
    public TopicModelModuleSpecs(JSONObject specs, MetaSpecs metaSpecs){
        lemmas = metaSpecs.getDataDir() + specs.get("lemmas");
        modelType = metaSpecs.useMetaModelType() ?
                metaSpecs.modelType :
                (String) specs.getOrDefault("modelType", specs.get("module"));
        dataDir = metaSpecs.getDataDir() + specs.getOrDefault("dataDir", specs.getOrDefault("outputDir", ""));
        if(!dataDir.endsWith("/")){
            dataDir = dataDir + "/";
        }
        documentOutput = dataDir + specs.get("documentOutput");
        mainModel = new ModelSpecs((JSONObject) specs.getOrDefault("model", specs.get("mainModel")), dataDir);
        if(modelType.equals("hierarchical")){
            subModel = new ModelSpecs((JSONObject) specs.get("subModel"), dataDir);
            JSONObject hierarchySpecs = (JSONObject) specs.get("hierarchy");
            maxAssign = Math.toIntExact((long) hierarchySpecs.getOrDefault("maxAssign", (long) 1));
            assignmentType = (String) hierarchySpecs.getOrDefault("assignmentType", "perceptual");
            similarityOutput = (String) hierarchySpecs.getOrDefault("modelSimOutput", "");
            if(!similarityOutput.equals("")){
                outputSimilarity = true;
                similarityOutput = dataDir + similarityOutput;
            }
            assignmentOutput = (String) hierarchySpecs.getOrDefault("assignmentOutput", "");
            if(!assignmentOutput.equals("")){
                outputAssignment = true;
                assignmentOutput = dataDir + assignmentOutput;
            }

            if(maxAssign < 1){
                LogPrint.printNote("Topic Model module: maxAssign must be greater than 0, parameter was set to "+maxAssign+", will be set to default: 1");
                maxAssign = 1;
            }
            if(!assignmentType.equals("perceptual") && !assignmentType.equals("document")){
                LogPrint.printNote("Topic Model module: assignmentType must be either \"perceptual\" or \"document\", parameter was set to \""+assignmentType+"\", will be set to default: \"perceptual\"");
                assignmentType = "perceptual";
            }
        }

        if(!modelType.equals("simple") && !modelType.equals("hierarchical")){
            LogPrint.printNoteError("Parameter Error: Topic Model module: modelType must be either \"simple\" of \"hierarchical\", parameter was set to "+modelType);
            System.exit(1);
        }
    }
}
