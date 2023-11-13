package P3_TopicModelling;

import P0_Project.ModelSpecs;
import P0_Project.TopicModelModuleSpecs;
import P3_TopicModelling.Similarity.TopicsSimilarity;
import PX_Data.DocIOWrapper;
import PX_Data.TopicIOWrapper;
import PY_Helper.LogPrint;
import de.siegmar.fastcsv.writer.CsvAppender;
import de.siegmar.fastcsv.writer.CsvWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class reading a lemma JSON file, generating a hierarchical topic model from these documents and saving several model files.
 * <br>
 * On top of the main and sub models files, the files saved include:<br>
 * - (optional) The sub topic to main topic similarity matrix CSV file;<br>
 * - (optional) The sub topic to main topic assignment data CSV file.
 *
 * @author P. Le Bras, A. Gharavi
 * @version 2
 */
public class HierarchicalTopicModelling {

    /** Main topic modelling instance. */
    private TopicModelling MainTopicModel;
    /** Sub topic modelling instance. */
    private TopicModelling SubTopicModel;
    /** Similarity matrix between sub and main topics. */
    private double[][] SimilarityMatrix;

    /** Filename of the inout lemma JSON file. */
    private String lemmasFile;
    /** Parameters of the main topic model. */
    private ModelSpecs mainModelSpecs;
    /** Number of topics in the main model. */
    private int nMainTopics;
    /** Parameters of the sub topic model. */
    private ModelSpecs subModelSpecs;
    /** Number of topics in the sub model. */
    private int nSubTopics;
    /** Assignment metric. */
    private String assignType;
    /** Maximum number of sub topics assigned to a main topic. */
    private int maxAssign;
    /** Boolean flag for writing similarities between main and sub topics on file. */
    private boolean outputSimilarity;
    /** Filename of CSV file containing the similarities between main and sub topics. */
    private String similarityOutput;
    /** Boolean flag for writing the assignment data on file. */
    private boolean outputAssignment;
    /** Filename of CSV file containing the assignment data. */
    private String assignmentOutput;

    /**
     * Main method, reads the specification and launches the sub-methods in order.
     * @param specs Specifications.
     * @return String indicating the time taken to read the lemmas JSON file, model topics and associated data,
     * assign sub topics to main topics and produce the output files.
     */
    public static String HierarchicalModel(TopicModelModuleSpecs specs){

        LogPrint.printModuleStart("Hierarchical topic modelling");

        long startTime = System.currentTimeMillis();

        HierarchicalTopicModelling startClass = new HierarchicalTopicModelling();
        startClass.ProcessArguments(specs);
        startClass.RunModels(specs);
        startClass.GetAndSetModelSimilarity();
        startClass.AssignTopicHierarchy();
        startClass.MergeDocuments();
        startClass.SaveTopics();

        long timeTaken = (System.currentTimeMillis() - startTime) / (long)1000;

        LogPrint.printModuleEnd("Hierarchical topic modelling");

        return "Hierarchical topic modelling: "+Math.floorDiv(timeTaken, 60) + " m, " + timeTaken % 60 + " s";
    }

    /**
     * Method processing the specification parameters.
     * @param specs Specifications.
     */
    private void ProcessArguments(TopicModelModuleSpecs specs){
        LogPrint.printNewStep("Processing arguments", 0);
        mainModelSpecs = specs.mainModel;
        nMainTopics = mainModelSpecs.topics;
        subModelSpecs = specs.subModel;
        nSubTopics = subModelSpecs.topics;
        lemmasFile = specs.lemmas;
        assignType = specs.assignmentType;
        maxAssign = specs.maxAssign;
        outputSimilarity = specs.outputSimilarity;
        if(outputSimilarity){
            similarityOutput = specs.similarityOutput;
        }
        outputAssignment = specs.outputAssignment;
        if(outputAssignment){
            assignmentOutput = specs.assignmentOutput;
        }
        LogPrint.printCompleteStep();
        LogPrint.printNote("Modelling "+nMainTopics+" main topics and "+nSubTopics+" sub topics");
        LogPrint.printNote("Assigning sub topics to main topic using "+assignType+" metric");
        LogPrint.printNote("Each sub topic will be assign a maximum of "+maxAssign+" main topics");
        if(outputSimilarity){
            LogPrint.printNote("Saving main topic to sub topic similarity");
        }
        if(outputAssignment){
            LogPrint.printNote("Saving sub topic to main topic assignment");
        }
    }

    /**
     * Method reading the lemmas and launching the main and sub models.
     * @param specs Module specifications passed on to the model instances.
     */
    private void RunModels(TopicModelModuleSpecs specs){
        LogPrint.printSubModuleStart("Lemma reader");
        LemmaReader lemmas = new LemmaReader(lemmasFile);
        LogPrint.printSubModuleStart("Main model");
        MainTopicModel = TopicModelling.Model(specs, mainModelSpecs, lemmas);
        LogPrint.printSubModuleStart("Sub model");
        SubTopicModel = TopicModelling.Model(specs, subModelSpecs, lemmas);
        LogPrint.printSubModuleEnd();
    }

    /**
     * Method calculating the similarities between sub and main topics.
     */
    private void GetAndSetModelSimilarity(){
        if(assignType.equals("document")){
            SimilarityMatrix = TopicsSimilarity.DocumentCosineSimilarity(SubTopicModel.getModelledDocuments(),
                    MainTopicModel.getModelledDocuments());
        } else {
            // Default similarity measure "perceptual"
            SimilarityMatrix = TopicsSimilarity.PerceptualSimilarity(SubTopicModel.getModelledTopics(),
                    MainTopicModel.getModelledTopics());
        }
        if(outputSimilarity) SaveSimilarityMatrix();
    }

    /**
     * Method using topic similarities to assign sub topics to main topics.
     */
    private void AssignTopicHierarchy() {
        ConcurrentHashMap<String, TopicIOWrapper> mainTopics = MainTopicModel.getTopics();
        ConcurrentHashMap<String, TopicIOWrapper> subTopics = SubTopicModel.getTopics();

        HashMap<Integer, HashMap<Integer, Double>> assignment = new HashMap<>();

        LogPrint.printNewStep("Calculating hierarchy assignments", 0);
        for (int sT = 0; sT < SimilarityMatrix.length; sT++) {
            double[] currentRow = SimilarityMatrix[sT];

            TopicIOWrapper subTopic = subTopics.get(String.valueOf((sT)));
            HashMap<Integer, Double> assigns = new HashMap<>();

            List<Integer> usedIdx = new ArrayList<>();
            // while under the maxAssign threshold
            for(int i = 0; i < maxAssign; i++){
                // find the next highest similarity between this sub topic and the main topics
                int currentMaxIdx = 0;
                double currentMax = 0.0;
                for(int mT = 0; mT < currentRow.length; mT++){
                    if(currentRow[mT] > currentMax && !usedIdx.contains(mT)){
                        currentMax = currentRow[mT];
                        currentMaxIdx = mT;
                    }
                }
                // remove the main topic found from list of potential assignment for this sub topic
                usedIdx.add(currentMaxIdx);
                // if no difference check: assign directly
                // save the assignment
                assigns.put(currentMaxIdx, currentMax);
                // find the main topic
                TopicIOWrapper mainTopic = mainTopics.get(String.valueOf(currentMaxIdx));
                // assign the main topic to the sub topic
                subTopic.addMainTopicId(mainTopic.getId(), currentMax);
                // assign the sub topic to the main topic
                mainTopic.addSubTopicId(subTopic.getId(), currentMax);
            }
            // save this sub topic's assignments
            assignment.put(sT,assigns);
        }
        LogPrint.printCompleteStep();

        if(outputAssignment){
            SaveAssignment(assignment);
        }
    }

    /**
     * Method merging document data from the sub model into the main model, and writing the document data on file.
     */
    private void MergeDocuments(){
        ConcurrentHashMap<String, DocIOWrapper> mainDocs = MainTopicModel.getDocuments();
        ConcurrentHashMap<String, DocIOWrapper> subDocs = SubTopicModel.getDocuments();
        for(Map.Entry<String, DocIOWrapper> docEntry: mainDocs.entrySet()){
            String docKey = docEntry.getKey();
            docEntry.getValue().setSubTopicDistribution(subDocs.get(docKey).getMainTopicDistribution());
            if(subDocs.get(docKey).hasMainTopicFullWordDistances()){
                docEntry.getValue().setSubTopicFullWordDistances(subDocs.get(docKey).getMainTopicFullWordDistances());
            }
            if(subDocs.get(docKey).hasMainTopicCompWordDistances()){
                docEntry.getValue().setSubTopicCompWordDistances(subDocs.get(docKey).getMainTopicCompWordDistances());
            }
        }
        MainTopicModel.SaveDocuments(nMainTopics, nSubTopics);
    }

    /**
     * Method writing the models' similarities on file.
     */
    private void SaveSimilarityMatrix(){
        LogPrint.printNewStep("Saving model similarities", 0);

        File file = new File(similarityOutput);
        file.getParentFile().mkdirs();
        try {
            // this will erase the content of the file before appending data to it.
            new FileWriter(file.getPath(), false).close();
        } catch (IOException e) {
            LogPrint.printNoteError("Error while saving similarity matrix\n");
            e.printStackTrace();
        }
        CsvWriter writer = new CsvWriter();
        writer.setAlwaysDelimitText(true);

        try (CsvAppender appender = writer.append(file, StandardCharsets.UTF_8)) {
            String[] mainLabels = MainTopicModel.getTopicsLabels();
            String[] subLabels = SubTopicModel.getTopicsLabels();
            appender.appendField("");
            for (int mT = 0; mT < nMainTopics; mT++) {
                appender.appendField(mainLabels[mT]);
            }
            appender.endLine();

            for (int sT = 0; sT < SimilarityMatrix.length; sT++) {
                appender.appendField(subLabels[sT]);
                for (int mT = 0; mT < SimilarityMatrix[0].length; mT++)
                    appender.appendField(String.valueOf(SimilarityMatrix[sT][mT]));
                appender.endLine();
            }
            LogPrint.printCompleteStep();
        } catch (Exception e) {
            LogPrint.printNoteError("Error while saving similarity matrix\n");
            e.printStackTrace();
        }
    }

    /**
     * Method writing the assignment data on file.
     * @param assignment Assignment date to save.
     */
    private void SaveAssignment(HashMap<Integer, HashMap<Integer, Double>> assignment){
        LogPrint.printNewStep("Saving hierarchy assignments", 0);

        File file = new File(assignmentOutput);
        file.getParentFile().mkdirs();
        try {
            // this will erase the content of the file before appending data to it.
            new FileWriter(file.getPath(), false).close();
        } catch (IOException e) {
            LogPrint.printNoteError("Error while saving similarity matrix\n");
            e.printStackTrace();
        }
        CsvWriter writer = new CsvWriter();
        writer.setAlwaysDelimitText(true);

        try (CsvAppender appender = writer.append(file, StandardCharsets.UTF_8)) {
            String[] mainLabels = MainTopicModel.getTopicsLabels();
            String[] subLabels = SubTopicModel.getTopicsLabels();
            for(Map.Entry<Integer, HashMap<Integer, Double>> a : assignment.entrySet()){
                int sT = a.getKey();
                HashMap<Integer, Double> b = a.getValue();
                appender.appendField(subLabels[sT]);
                boolean skipFirstCol = false;
                for(Map.Entry<Integer, Double> c : b.entrySet()){
                    int mT = c.getKey();
                    double sim = c.getValue();
                    if(skipFirstCol) appender.appendField("");
                    appender.appendField(mainLabels[mT]);
                    appender.appendField(String.valueOf(sim));
                    appender.endLine();
                    skipFirstCol = true;
                }
            }
            LogPrint.printCompleteStep();
        } catch (Exception e) {
            LogPrint.printNoteError("Error while saving hierarchy assignments\n");
            e.printStackTrace();
        }
    }

    /**
     * Method writing the topics data on file.
     */
    private void SaveTopics(){
        MainTopicModel.SaveTopics();
        SubTopicModel.SaveTopics();
    }
}