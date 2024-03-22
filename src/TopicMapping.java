import P1_Input.*;
import P2_Lemmatise.Lemmatise;
import P0_Project.ProjectManager;
import P3_TopicModelling.HierarchicalTopicModelling;
import P3_TopicModelling.InferDocuments;
import P3_TopicModelling.TopicModelling;
import P4_Analysis.LabelIndex.LabelIndexing;
import P4_Analysis.TopicClustering.TopicClustering;
import P4_Analysis.TopicDistribution.CompareDistributions;
import P4_Analysis.TopicDistribution.TopicDistribution;
import P5_TopicMapping.BubbleMapping.BubbleMapJS;
import P5_TopicMapping.BubbleMapping.BubbleMap;
import P3_TopicModelling.ExportTopicModel;
import P5_TopicMapping.OverwriteMap;
import PY_Helper.LogPrint;

import java.util.ArrayList;
import java.util.List;

public class TopicMapping {

    private String projectFile;
    private ProjectManager projectManager;
    private static List<String> times = new ArrayList<>();

    public static void main(String[] args) {
        LogPrint.printModuleStart("Pipeline");

        long startTime = System.currentTimeMillis();

        TopicMapping startClass = new TopicMapping();
        startClass.CheckArgs(args);
        startClass.LoadProject();
        startClass.Run();
        for(String t: times){
            LogPrint.printNote(t);
        }

        long timeTaken = (System.currentTimeMillis() - startTime) / (long)1000;

        LogPrint.printNote("Total: "+Math.floorDiv(timeTaken, 60) + " m, " + timeTaken % 60 + " s.");

        LogPrint.printModuleEnd("Pipeline");
    }

    private void CheckArgs(String[] args){
        if(args.length == 1){
            projectFile = args[0];
        }
    }

    private void LoadProject(){
        projectManager = new ProjectManager(projectFile);
    }

    private void Run(){
        if(projectManager.runInput){
            this.RunInput();
        }
        if(projectManager.runLemmatise){
            this.RunLemmatise();
        }
        if(projectManager.runModel){
            this.RunModel();
        }
        if(projectManager.runDocumentInfer){
            this.RunDocumentInference();
        }
        if(projectManager.runTopicModelExport){
            this.RunExport();
        }
        if(projectManager.runLabelIndex){
            this.RunLabelIndex();
        }
        if(projectManager.runTopicDistrib){
            this.RunTopicDistrib();
        }
        if(projectManager.runCompareDistrib){
            this.RunCompareDistrib();
        }
        if(projectManager.runTopicCluster){
            this.RunTopicCluster();
        }
        if(projectManager.runTopicMap){
            this.RunTopicMap();
        }
        if(projectManager.runOverwriteMap){
            this.RunOverwriteMap();
        }
    }

    private void RunInput(){
        switch (projectManager.input.module){
            case "CSV":
                times.add(CSVInput.CSVInput(projectManager.input));
                break;
            case "GTR":
                times.add(GTRInput.GTRInput(projectManager.input));
                break;
            case "HTML":
                times.add(HTMLInput.HTMLInput(projectManager.input));
                break;
            case "PDF":
                times.add(PDFInput.PDFInput(projectManager.input));
                break;
            case "TXT":
                times.add(TXTInput.TXTInput(projectManager.input));
                break;
        }
    }

    private void RunLemmatise(){
        times.add(Lemmatise.Lemmatise(projectManager.lemmatise));
    }

    private void RunModel(){
        if(projectManager.model.modelType.equals("simple")){
            times.add(TopicModelling.SingleModel(projectManager.model));
        } else if(projectManager.model.modelType.equals("hierarchical")){
            times.add(HierarchicalTopicModelling.HierarchicalModel(projectManager.model));
        }

    }

    private void RunDocumentInference(){
        times.add(InferDocuments.InferDocuments(projectManager.documentInfer));
    }

    private void RunExport(){
        times.add(ExportTopicModel.ExportTopicModel(projectManager.topicModelExport));
    }

    private void RunLabelIndex(){
        times.add(LabelIndexing.Index(projectManager.labelIndex));
    }

    private void RunTopicDistrib(){
        times.add(TopicDistribution.Distribute(projectManager.topicDistrib));
    }

    private void RunCompareDistrib(){
        times.add(CompareDistributions.Compare(projectManager.compareDistrib));
    }

    private void RunTopicCluster(){
        times.add(TopicClustering.Cluster(projectManager.topicCluster));
    }

    private void RunTopicMap() {
        if(projectManager.topicMap.mapType.equals("bubbleJS")){
            times.add(BubbleMapJS.MapTopics(projectManager.topicMap));
        } else if(projectManager.topicMap.mapType.equals("bubble")){
            times.add(BubbleMap.MapTopics(projectManager.topicMap));
        }
    }

    private void RunOverwriteMap(){
        times.add(OverwriteMap.Overwrite(projectManager.overwriteMap));
    }
}
