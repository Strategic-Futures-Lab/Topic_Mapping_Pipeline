import P1_Input.CSVInput;
import P2_Lemmatise.Lemmatise;
import P0_Project.ProjectManager;
import P3_TopicModelling.HierarchicalTopicModelling;
import P3_TopicModelling.LemmaReader;
import P3_TopicModelling.Similarity.TopicsSimilarity;
import P3_TopicModelling.TopicModelling;

public class TopicMapping {

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
        if(projectManager.runInput){
            this.RunInput();
        }
        if(projectManager.runLemmatise){
            this.RunLemmatise();
        }
        if(projectManager.runModel){
            this.RunModel();
        }
    }

    private void RunInput(){
        switch (projectManager.input.module){
            case "CSV":
                CSVInput.CSVInput(projectManager.input);
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
}
