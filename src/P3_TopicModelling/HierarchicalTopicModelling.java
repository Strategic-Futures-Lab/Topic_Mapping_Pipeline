package P3_TopicModelling;

import P0_Project.TopicModelModuleSpecs;
import P3_TopicModelling.Similarity.TopicsSimilarity;
import PX_Data.DocIOWrapper;
import PX_Data.TopicIOWrapper;
import PY_Helper.LogPrint;
import de.siegmar.fastcsv.writer.CsvAppender;
import de.siegmar.fastcsv.writer.CsvWriter;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HierarchicalTopicModelling {

    private LemmaReader Lemmas;
    private TopicModelling MainTopicModel;
    private TopicModelling SubTopicModel;
    private double[][] SimilarityMatrix;

    private TopicModelModuleSpecs specs;

    public static String HierarchicalModel(TopicModelModuleSpecs specs){

        LogPrint.printModuleStart("Hierarchical topic modelling");

        long startTime = System.currentTimeMillis();

        HierarchicalTopicModelling startClass = new HierarchicalTopicModelling();
        startClass.specs = specs;
        startClass.RunModels();
        startClass.GetAndSetModelSimilarity();
        startClass.AssignTopicHierarchy();
        startClass.MergeDocuments();
        startClass.SaveTopics();

        long timeTaken = (System.currentTimeMillis() - startTime) / (long)1000;

        LogPrint.printModuleEnd("Hierarchical topic modelling");

        return "Hierarchical topic modelling: "+Math.floorDiv(timeTaken, 60) + " m, " + timeTaken % 60 + " s";
    }

    private void RunModels(){
        LogPrint.printSubModuleStart("Lemma reader");
        Lemmas = new LemmaReader(specs.lemmas);
        LogPrint.printSubModuleStart("Main model");
        MainTopicModel = TopicModelling.Model(specs, specs.mainModel, Lemmas);
        LogPrint.printSubModuleStart("Sub model");
        SubTopicModel = TopicModelling.Model(specs, specs.subModel, Lemmas);
        LogPrint.printSubModuleEnd();
    }

    private void GetAndSetModelSimilarity(){
        SimilarityMatrix = TopicsSimilarity.GetSimilarityMatrix(specs.subModel.topics, SubTopicModel.getTopicDistributions(),
                                                                specs.mainModel.topics, MainTopicModel.getTopicDistributions());
        if(specs.outputSimilarity){
            SaveSimilarityMatrix();
        }
    }

    private void AssignTopicHierarchy() {
        int maxAssignMain = specs.maxAssignMain;
        int maxAssignSub = specs.maxAssignSub;
        ConcurrentHashMap<String, TopicIOWrapper> mainTopics = MainTopicModel.getTopics();
        ConcurrentHashMap<String, TopicIOWrapper> subTopics = SubTopicModel.getTopics();

        HashMap<Integer, HashMap<Integer, Double>> assignment = new HashMap<>();

        LogPrint.printNewStep("Calculating hierarchy assignments", 0);
        for (int sT = 0; sT < SimilarityMatrix.length; sT++) {
            double[] currentRow = SimilarityMatrix[sT];

            // for direct assignment
            TopicIOWrapper subTopic = subTopics.get(String.valueOf((sT)));

            HashMap<Integer, Double> assigns = new HashMap<>();

            List<Integer> usedIdx = new ArrayList<>();
            // while under the maxAssignSub threshold
            for(int i = 0; i < maxAssignSub; i++){
                // find the next highest similarity between this sub topic and the main topics
                double currentMax = 0.00;
                int currentMaxIdx = 0;
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
                // if still under the maxAssignMain threshold
                if(i<maxAssignMain){
                    // assign the sub topic to the main topic
                    mainTopic.addSubTopicId(subTopic.getId(), currentMax);
                }
            }
            // save this sub topic's assignments
            assignment.put(sT,assigns);
        }
        LogPrint.printCompleteStep();

        if(specs.outputAssignment){
            SaveAssignment(assignment);
        }
    }

    private void MergeDocuments(){
        ConcurrentHashMap<String, DocIOWrapper> mainDocs = MainTopicModel.getDocuments();
        ConcurrentHashMap<String, DocIOWrapper> subDocs = SubTopicModel.getDocuments();
        for(Map.Entry<String, DocIOWrapper> docEntry: mainDocs.entrySet()){
            String docKey = docEntry.getKey();
            docEntry.getValue().setSubTopicDistribution(subDocs.get(docKey).getMainTopicDistribution());
        }
        MainTopicModel.SaveDocuments(specs.mainModel.topics, specs.subModel.topics);
    }

    private void SaveSimilarityMatrix(){
        LogPrint.printNewStep("Saving model similarities", 0);

        File file = new File(specs.similarityOutput);
        file.getParentFile().mkdirs();
        CsvWriter writer = new CsvWriter();
        writer.setAlwaysDelimitText(true);

        try (CsvAppender appender = writer.append(file, StandardCharsets.UTF_8)) {
            String[] mainLabels = MainTopicModel.getTopicsLabels();
            String[] subLabels = SubTopicModel.getTopicsLabels();
            appender.appendField("");
            for (int mT = 0; mT < specs.mainModel.topics; mT++) {
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

        // System.out.println("Model Similarity Saved!");
    }

    private void SaveAssignment(HashMap<Integer, HashMap<Integer, Double>> assignment){
        LogPrint.printNewStep("Saving hierarchy assignments", 0);

        File file = new File(specs.assignmentOutput);
        file.getParentFile().mkdirs();
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

        // System.out.println("Hierarchy Assignemnts Saved!");
    }

    private void SaveTopics(){
        // System.out.println("Saving Main Topics ...");
        MainTopicModel.SaveTopics();
        // System.out.println("Main Topics Saved!");
        // System.out.println("Saving Sub Topics ...");
        SubTopicModel.SaveTopics();
        // System.out.println("Sub Topics Saved!");
    }
}
