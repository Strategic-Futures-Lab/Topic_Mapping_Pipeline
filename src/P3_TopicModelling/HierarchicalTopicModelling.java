package P3_TopicModelling;

import P0_Project.TopicModelModuleSpecs;
import P3_TopicModelling.Similarity.TopicsSimilarity;
import PX_Data.DocIOWrapper;
import PX_Data.TopicIOWrapper;
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

    public static void HierarchicalModel(TopicModelModuleSpecs specs){
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
        ConcurrentHashMap<String, TopicIOWrapper> mainTopics = MainTopicModel.getTopics();
        ConcurrentHashMap<String, TopicIOWrapper> subTopics = SubTopicModel.getTopics();

//        List<Pair<Integer, List<Pair<Integer, Double>>>> assignment = new ArrayList<>(); // [(subTopic, [(mainTopic, sim)])]
        HashMap<Integer, HashMap<Integer, Double>> assignment = new HashMap<>();
//        HashMap<String, ArrayList<String>> superSubGroups = new HashMap<>();

        System.out.println("Calculating Hierarchy Assignments ...");
        for (int sT = 0; sT < SimilarityMatrix.length; sT++) {
            double[] currentRow = SimilarityMatrix[sT];

            // for direct assignment
            TopicIOWrapper subTopic = subTopics.get(String.valueOf((sT)));

            HashMap<Integer, Double> assigns = new HashMap<>();

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

                assigns.put(currentMaxIdx, currentMax);

                // if no difference check: assign directly
                TopicIOWrapper mainTopic = mainTopics.get(String.valueOf(currentMaxIdx));
                mainTopic.addSubTopicId(subTopic.getId());
                subTopic.addMainTopicId(mainTopic.getId());
            }
            assignment.put(sT,assigns);
        }
        System.out.println("Hierarchy Assignments Completed!");

        if(specs.outputAssignment){
            SaveAssignment(assignment);
        }
    }

    private void MergeDocuments(){
        ConcurrentHashMap<String, DocIOWrapper> mainDocs = MainTopicModel.getDocuments();
        ConcurrentHashMap<String, DocIOWrapper> subDocs = SubTopicModel.getDocuments();
        for(Map.Entry<String, DocIOWrapper> docEntry: mainDocs.entrySet()){
            String docKey = docEntry.getKey();
            docEntry.getValue().setSubTopicDistribution(subDocs.get(docKey).getTopicDistribution());
        }
        MainTopicModel.SaveDocuments(specs.mainModel.topics, specs.subModel.topics);
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

    private void SaveAssignment(HashMap<Integer, HashMap<Integer, Double>> assignment){
        System.out.println("Saving Hierarchy Assignments ...");

        File file = new File(specs.assignmentOutput);
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
