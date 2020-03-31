import P1_Input.CSVInput;
import P2_Lemmatise.Lemmatise;
import P0_Project.ProjectManager;
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

    private void RunModel(){
        LemmaReader.ReadLemma(projectManager.model.lemmas);
        System.out.println("Running main topic model ...");
        TopicModelling T1 = TopicModelling.Model(LemmaReader.Documents,
                                                 LemmaReader.metadata,
                                                 projectManager.model.mainModel.topics,
                                                 projectManager.model.mainModel.words,
                                                 projectManager.model.mainModel.docs,
                                                 projectManager.model.mainModel.iterations,
                                                 projectManager.model.outputDir);
        System.out.println("Main topic model completed!");
        double[][] M1 = TopicsSimilarity.GetSimilarityMatrix(projectManager.model.mainModel.topics, T1.getTopicDistributions());
        T1.setTopicsSimilarity(M1);
        if(projectManager.model.module.equals("simple")){
            System.out.println("Saving topic model data ...");
            T1.SaveTopics(projectManager.model.mainModel.topicOutput);
            T1.SaveDocuments(projectManager.model.documentOutput);
            System.out.println("Topic model data saved!");
        } else if(projectManager.model.module.equals("hierarchical")){
            System.out.println("Running sub topic model ...");
            TopicModelling T2 = TopicModelling.Model(LemmaReader.Documents,
                                                     LemmaReader.metadata,
                                                     projectManager.model.subModel.topics,
                                                     projectManager.model.subModel.words,
                                                     projectManager.model.subModel.docs,
                                                     projectManager.model.subModel.iterations,
                                                     projectManager.model.outputDir);
            System.out.println("Sub topic model completed!");
            double[][] M2 = TopicsSimilarity.GetSimilarityMatrix(projectManager.model.subModel.topics, T2.getTopicDistributions());
            T2.setTopicsSimilarity(M2);

            double[][] M12 = TopicsSimilarity.GetSimilarityMatrix(projectManager.model.mainModel.topics, T1.getTopicDistributions(),
                                                                  projectManager.model.subModel.topics, T2.getTopicDistributions());
            System.out.println("Saving topic model data ...");
            T2.SaveTopics(projectManager.model.subModel.topicOutput);
            T1.SaveTopics(projectManager.model.mainModel.topicOutput);
            System.out.println("Topic model data saved!");

            // TODO merge document data (Hierarchy package)
            // TODO get super-sub topics assignment from M12 and update topic data
        }

    }
}
