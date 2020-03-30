import P1_Input.CSVInput;
import P2_Lemmatise.Lemmatise;
import PX_Helper.ProjectManager;

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
    }

    private void RunInput(){
        switch (projectManager.input.module){
            case "CSV":
                CSVInput.CSVInput(projectManager.input.source,
                                  projectManager.input.fields,
                                  projectManager.input.output);
        }
    }

    private void RunLemmatise(){
        Lemmatise.Lemmatise(projectManager.lemmatise.corpus,
                            projectManager.lemmatise.textFields,
                            projectManager.lemmatise.docFields,
                            projectManager.lemmatise.stopWords,
                            projectManager.lemmatise.minLemmas,
                            projectManager.lemmatise.output);
    }
}
