package P0_Project;

import PX_Data.JSONIOWrapper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Class reading and validating parameters for the Infer Documents module ({@link P3_TopicModelling.InferDocuments}).
 *
 * @author P. Le Bras
 * @version 2
 */
public class DocumentInferModuleSpecs {

    // INPUT
    /** Filename to lemma data to infer (from Lemmatise module). */
    public String lemmas;
    /** Number of iterations for the inferencer to go through, optional, defaults to 100. */
    public int iterations = 100;
    /** Directory where serialised models are located, optional, defaults to "". */
    public String modelDir = "";
    /** Filename of serialised main model. */
    public String mainModel;
    /** Filename of serialised sub model, optional, defaults to "". */
    public String subModel = "";
    /** Flag for inferring from sub model, defaults to false if subModel = "". */
    public boolean inferFromSubModel = false;

    /** Filename of existing model document, only required if documentsOutput != "". */
    public String documents = "";
    /** Filename of existing main topic file, only required if mainTopicsOutput != "". */
    public String mainTopics = "";
    /** Filename of existing sub topic file, only required if subTopicsOutput != "". */
    public String subTopics = "";

    // OUTPUT
    /** Output directory, optional, defaults to "". */
    public String outputDir = "";

    /** Filename for the CSV inferred document file, optional, defaults to "". */
    public String csvOutput = "";
    /** Flag for exporting inferred documents as CSV, defaults to false if outputCSV = "". */
    public boolean exportCSV = false;
    /** List of fields in docData to keep, only required if exportCSV = true,
     * optional, defaults to empty, overwritten by meta parameter.  */
    public String[] docFields;
    /** Number of words to identify a topic in csv output, only required if exportCSV = true,
     * optional, defaults to 3. */
    public int numWordId = 3;

    /** Filename for the JSON document file generated, merging model and inferred data, optional, defaults to "". */
    public String documentsOutput = "";
    /** Flag for merging document data from model and inferred data, defaults to false if documentsOutput = "". */
    public boolean mergeDocuments = false;
    /** Filename for the main JSON topic file generated, merging model and inferred data, optional, defaults to "". */
    public String mainTopicsOutput = "";
    /** Flag for merging main topic data from model and inferred data, defaults to false if mainTopicsOutput = "". */
    public boolean mergeMainTopics = false;
    /** Filename for the sub JSON topic file generated, merging model and inferred data, optional, defaults to "". */
    public String subTopicsOutput = "";
    /** Flag for merging sub topic data from model and inferred data, defaults to false if subTopicsOutput = "". */
    public boolean mergeSubTopics = false;

    /**
     * Constructor: parses and validates the given JSON object to set parameters.
     * @param specs JSON object attached to "inferDocuments" in project file.
     * @param metaSpecs Meta-parameter specifications.
     */
    public DocumentInferModuleSpecs(JSONObject specs, MetaSpecs metaSpecs){
        lemmas = metaSpecs.getDataDir() + specs.get("lemmas");
        iterations = Math.toIntExact((long) specs.getOrDefault("iterations", (long) 100));

        modelDir = metaSpecs.getSourceDir() + specs.getOrDefault("modelDir", "");
        if(!modelDir.endsWith("/")&&modelDir.length()>0){
            modelDir = modelDir+"/";
        }

        mainModel = modelDir + specs.getOrDefault("mainModel", specs.get("model"));
        subModel = (String) specs.getOrDefault("subModel", "");
        inferFromSubModel = metaSpecs.useMetaModelType() ? metaSpecs.doHierarchical() : subModel.length() > 0;
        if(inferFromSubModel){
            subModel = modelDir + subModel;
        }

        outputDir = metaSpecs.getDataDir() + specs.getOrDefault("outputDir", "");

        csvOutput = (String) specs.getOrDefault("csvOutput", "");
        exportCSV = csvOutput.length() > 0;
        if(exportCSV){
            csvOutput = outputDir + csvOutput;
            numWordId = Math.toIntExact((long) specs.getOrDefault("numWordId", (long) 3));
            docFields = metaSpecs.useMetaDocFields() ?
                    metaSpecs.docFields :
                    JSONIOWrapper.getStringArray((JSONArray) specs.getOrDefault("docFields", new JSONArray()));
        }

        documentsOutput = (String) specs.getOrDefault("documentsOutput", "");
        mergeDocuments = documentsOutput.length() > 0;
        if(mergeDocuments){
            documentsOutput = outputDir + documentsOutput;
            documents = modelDir + specs.get("documents");
        }

        mainTopicsOutput = (String) specs.getOrDefault("mainTopicsOutput", (String) specs.getOrDefault("topicsOutput", ""));
        mergeMainTopics = mainTopicsOutput.length() > 0;
        if(mergeMainTopics){
            mainTopicsOutput = outputDir + mainTopicsOutput;
            mainTopics = modelDir + specs.getOrDefault("mainTopics", (String) specs.get("topics"));
        }

        subTopicsOutput = (String) specs.getOrDefault("subTopicsOutput", "");
        mergeSubTopics = subTopicsOutput.length() > 0;
        if(mergeSubTopics){
            subTopicsOutput = outputDir + subTopicsOutput;
            subTopics = modelDir + specs.get("subTopics");
        }
    }
}
