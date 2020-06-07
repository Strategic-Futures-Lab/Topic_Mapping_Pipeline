import P1_Input.CSVInput;
import P1_Input.PDFInput;
import P2_Lemmatise.Lemmatise;
import P0_Project.ProjectManager;
import P3_TopicModelling.HierarchicalTopicModelling;
import P3_TopicModelling.TopicModelling;
import P4_Analysis.LabelIndex.LabelIndexing;
import P4_Analysis.TopicClustering.TopicClustering;
import P4_Analysis.TopicDistribution.TopicDistribution;
import P5_TopicMapping.BubbleMap;
import P3_TopicModelling.ExportTopicModel;
import PY_Helper.LogPrint;

import java.util.ArrayList;
import java.util.List;

public class TopicMapping {

    private String projectFile;
    private ProjectManager projectManager;
    private static List<String> times = new ArrayList<>();

    public static void main(String[] args) {
        LogPrint.printModuleStart("Pipeline");
        TopicMapping startClass = new TopicMapping();
        startClass.CheckArgs(args);
        startClass.LoadProject();
        startClass.Run();
        for(String t: times){
            LogPrint.printNote(t);
        }
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
        if(projectManager.runTopicModelExport){
            this.RunExport();
        }
        if(projectManager.runLabelIndex){
            this.RunLabelIndex();
        }
        if(projectManager.runTopicDistrib){
            this.RunTopicDistrib();
        }
        if(projectManager.runTopicCluster){
            this.RunTopicCluster();
        }
        if(projectManager.runTopicMap){
            this.RunTopicMap();
        }
    }

    private void RunInput(){
        switch (projectManager.input.module){
            case "CSV":
                times.add(CSVInput.CSVInput(projectManager.input));
                break;
            case "PDF":
                PDFInput.PDFInput(projectManager.input);
                break;
        }
    }

    private void RunLemmatise(){
        times.add(Lemmatise.Lemmatise(projectManager.lemmatise));
    }

    private void RunModel(){
        if(projectManager.model.module.equals("simple")){
            times.add(TopicModelling.SingleModel(projectManager.model));
        } else if(projectManager.model.module.equals("hierarchical")){
            times.add(HierarchicalTopicModelling.HierarchicalModel(projectManager.model));
        }

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

    private void RunTopicCluster(){
        times.add(TopicClustering.Cluster(projectManager.topicCluster));
    }

    private void RunTopicMap() {
        if(projectManager.topicMap.mapType.equals("bubble")){
            times.add(BubbleMap.MapTopics(projectManager.topicMap));
        }
    }
}
