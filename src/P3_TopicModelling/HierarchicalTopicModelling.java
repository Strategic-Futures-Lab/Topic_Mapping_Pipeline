package P3_TopicModelling;

import P0_Project.ProjectModel;
import P3_TopicModelling.Similarity.TopicsSimilarity;
import PX_Helper.Pair;
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

    private ProjectModel specs;

    public static void HierarchicalModel(ProjectModel specs){
        System.out.println( "**********************************************************\n" +
                            "* STARTING Hierarchical Topic Modelling !                *\n" +
                            "**********************************************************\n");

        HierarchicalTopicModelling startClass = new HierarchicalTopicModelling();
        startClass.specs = specs;
        startClass.RunModels();
        startClass.GetAndSetModelSimilarity();
        startClass.AssignTopicHierarchy();
        startClass.MergeDocuments();
        startClass.SaveTopics();

        System.out.println( "**********************************************************\n" +
                            "* Hierarchical Topic Modelling COMPLETE !                *\n" +
                            "**********************************************************\n");
    }

    private void RunModels(){
        Lemmas = new LemmaReader(specs.lemmas);
        MainTopicModel = TopicModelling.Model(specs, specs.mainModel, Lemmas);
        SubTopicModel = TopicModelling.Model(specs, specs.subModel, Lemmas);
    }

    private void GetAndSetModelSimilarity(){
        SimilarityMatrix = TopicsSimilarity.GetSimilarityMatrix(specs.subModel.topics, SubTopicModel.getTopicDistributions(),
                                                                specs.mainModel.topics, MainTopicModel.getTopicDistributions());
        if(specs.outputSimilarity){
            SaveSimilarityMatrix();
        }
    }

    private void AssignTopicHierarchy() {
        int maxAssign = specs.maxAssign;
        ConcurrentHashMap<String, ModelJSONTopic> mainTopics = MainTopicModel.getTopics();
        ConcurrentHashMap<String, ModelJSONTopic> subTopics = SubTopicModel.getTopics();

        List<Pair<Integer, List<Pair<Integer, Double>>>> assignment = new ArrayList<>(); // [(subTopic, [(mainTopic, sim)])]
        HashMap<String, ArrayList<String>> superSubGroups = new HashMap<>();

        System.out.println("Calculating Hierarchy Assignments ...");
        for (int sT = 0; sT < SimilarityMatrix.length; sT++) {
            double[] currentRow = SimilarityMatrix[sT];

            // for direct assignment
            ModelJSONTopic subTopic = subTopics.get(String.valueOf((sT)));

            List<Pair<Integer, Double>> assigns = new ArrayList<>();

            List<Integer> usedIdx = new ArrayList<>();
            for(int i = 0; i < maxAssign; i++){
                double currentMax = 0.00;
                int currentMaxIdx = 0;
                for(int mT = 0; mT < currentRow.length; mT++){
                    if(currentRow[mT] > currentMax && !usedIdx.contains(mT)){
                        currentMax = currentRow[mT];
                        currentMaxIdx = mT;
                    }
                }
                usedIdx.add(currentMaxIdx);

                assigns.add(new Pair<>(currentMaxIdx, currentMax));

                // if no difference check: assign directly
                ModelJSONTopic mainTopic = mainTopics.get(String.valueOf(currentMaxIdx));
                mainTopic.addSubTopicId(subTopic.getId());
                subTopic.addMainTopicId(mainTopic.getId());
            }
            assignment.add(new Pair<>(sT,assigns));
        }
        System.out.println("Hierarchy Assignments Completed!");

        if(specs.outputAssignment){
            SaveAssignment(assignment);
        }
    }

    private void MergeDocuments(){
        ConcurrentHashMap<String, ModelJSONDocument> mainDocs = MainTopicModel.getDocuments();
        ConcurrentHashMap<String, ModelJSONDocument> subDocs = SubTopicModel.getDocuments();
        for(Map.Entry<String, ModelJSONDocument> docEntry: mainDocs.entrySet()){
            String docKey = docEntry.getKey();
            docEntry.getValue().setSubTopicDistribution(subDocs.get(docKey).getTopicDistribution());
        }
        MainTopicModel.SaveDocuments();
    }

    private void SaveSimilarityMatrix(){
        System.out.println("Saving Model Similarity ...");

        File file = new File(specs.similarityOutput);
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
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Model Similarity Saved!");
    }

    private void SaveAssignment(List<Pair<Integer, List<Pair<Integer, Double>>>> assignment){
        System.out.println("Saving Hierarchy Assignments ...");

        File file = new File(specs.assignmentOutput);
        CsvWriter writer = new CsvWriter();
        writer.setAlwaysDelimitText(true);

        try (CsvAppender appender = writer.append(file, StandardCharsets.UTF_8)) {
            String[] mainLabels = MainTopicModel.getTopicsLabels();
            String[] subLabels = SubTopicModel.getTopicsLabels();
            for(Pair<Integer, List<Pair<Integer, Double>>> p1: assignment){
                int sT = p1.getLeft();
                List<Pair<Integer, Double>> assigns = p1.getRight();
                appender.appendField(subLabels[sT]);
                for(int j = 0; j < assigns.size(); j++){
                    Pair<Integer, Double> p2 = assigns.get(j);
                    int mT = p2.getLeft();
                    double sim = p2.getRight();
                    if(j > 0) appender.appendField("");
                    appender.appendField(mainLabels[mT]);
                    appender.appendField(String.valueOf(sim));
                    appender.endLine();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Hierarchy Assignemnts Saved!");
    }

    private void SaveTopics(){
        System.out.println("Saving Main Topics ...");
        MainTopicModel.SaveTopics();
        System.out.println("Main Topics Saved!");
        System.out.println("Saving Sub Topics ...");
        SubTopicModel.SaveTopics();
        System.out.println("Sub Topics Saved!");
    }
}
