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

public class TopicMapping {

    private boolean TESTING = false;

    private String projectFile;
    private ProjectManager projectManager;

    public static void main(String[] args) {
        TopicMapping startClass = new TopicMapping();
        startClass.CheckArgs(args);
        startClass.LoadProject();
        startClass.Run();
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
        if(TESTING){
            this.RunTESTING();
        }
        if(projectManager.runInput){
            this.RunInput();
        }
        if(projectManager.runLemmatise){
            this.RunLemmatise();
        }
        if(projectManager.runModel){
            this.RunModel();
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
                CSVInput.CSVInput(projectManager.input);
            case "PDF":
                PDFInput.PDFInput(projectManager.input);
        }
    }

    private void RunLemmatise(){
        Lemmatise.Lemmatise(projectManager.lemmatise);
    }

    private void RunModel(){
        if(projectManager.model.module.equals("simple")){
            TopicModelling.SingleModel(projectManager.model);
        } else if(projectManager.model.module.equals("hierarchical")){
            HierarchicalTopicModelling.HierarchicalModel(projectManager.model);
        }

    }

    private void RunLabelIndex(){
        LabelIndexing.Index(projectManager.labelIndex);
    }

    private void RunTopicDistrib(){
        TopicDistribution.Distribute(projectManager.topicDistrib);
    }

    private void RunTopicCluster(){
        TopicClustering.Cluster(projectManager.topicCluster);
    }

    private void RunTopicMap() {
        if(projectManager.topicMap.mapType.equals("bubble")){
            BubbleMap.MapTopics(projectManager.topicMap);
        }
    }

    private void RunTESTING(){

    }
}
