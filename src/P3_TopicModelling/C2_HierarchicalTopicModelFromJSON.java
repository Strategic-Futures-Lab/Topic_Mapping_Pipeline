package P3_TopicModelling;

import P1_Input.A1_CSVInput;
import P2_Lemmatise.B2_LemmatiseJSONFile;
import P4_Descriptive.D1_CreateDownloadableCSV;
import PX_Helper.DocumentRow;
import PX_Helper.JSONIOWrapper;
import PY_TopicModelCore.TopicData;
import PY_TopicModelCore.TopicModel;
import PY_TopicModelCore.TopicRowContainer;
import PY_TopicModelCore.WordData;
import PZ_ScottishEnterprise.E3_Assignment;
import cc.mallet.types.Alphabet;
import cc.mallet.types.IDSorter;
import de.siegmar.fastcsv.writer.CsvAppender;
import de.siegmar.fastcsv.writer.CsvWriter;
import edu.stanford.nlp.util.StringUtils;
import edu.stanford.nlp.util.StringUtils.*;

import java.io.File;
import java.io.IOException;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Azimeh on 25/03/2020.
 */
public class C2_HierarchicalTopicModelFromJSON {
    private JSONIOWrapper jWrapper;
    private ConcurrentHashMap<String, DocumentRow> JSONRows;
    private List<TopicRowContainer> topicMap = new ArrayList<>();
    private List<List<WordData>> wordsAndWeights;


    private String[] superArgs = new String[6];
    private String[] subArgs = new String[6];
    private String[] superDownloadableCSVArgs, subDownloadableCSVArgs;
    private String[] inputArgs = new String[2];
    private String[] lemmaArgs = new String[5];
    private String[] assinmentArgs = new String[5];

    //Contains the ID of any row that failed, and the reason it failed!
    private ConcurrentHashMap<String, String> failedRetrievals;

    /**
     * This is the the step which is created for ScottishEnterprise project where we want to have 2 topic models,
     *      1. super model with lower number of topics
     *      2. sub model with higher number of topics
     * The aim is to run the assignment algorithm to find the most similar topics from the
     * sub model for each super topic. Basically we want to use it as a simple mechanism of hierarchical clustering
     *
     * This is the step which performs the topic modelling itself. Importantly, before this step you should have set
     * the required LemmaStringData field in the JSON file, as that is the field the topic modelling reads! This part
     * of the pipeline then puts lots of new information into the JSON file, such as the distributions and top words
     * and documents for each topic. It also outputs a CSV file with the distributions in a format which can be used
     * with Well Sorted in order to explore the best number of clusters.
     *
     *
     * @param args - [csv input Location] [Output Location][Number of Super Topics] [Number of Sub Topics] [Number of Iterations] [Number of Words] [Number of Documents]
     */
    public static void main(String[] args) throws InterruptedException, IOException {
        System.out.println("\n********************************************\n" +
                "*                                          *\n" +
                "* STARTING C2_HierarchicalTopicModelFromJSON!    *\n" +
                "*                                          *\n" +
                "* C2_HierarchicalTopicModelFromJSON :START    *\n" +
                "*                                          * \n" +
                "********************************************\n");

        C2_HierarchicalTopicModelFromJSON startClass = new C2_HierarchicalTopicModelFromJSON();
        startClass.StartTopicModelling(args);

        System.out.println("\n********************************************\n" +
                "*                                          *\n" +
                "* C2_HierarchicalTopicModelFromJSON PHASE COMPLETE!    *\n" +
                "*                                          *\n" +
                "* C2_HierarchicalTopicModelFromJSON:END                *\n" +
                "*                                          * \n" +
                "********************************************\n");
    }

    /**
     * This is the method which fires all the different processes off in order to do all the topic modelling and various
     * processes which are required for this.
     * <p>
     * Please note, the topic modelling used is MALLET and that is already parallelised.
     *
     * @param args - The argument list directly from main()
     */
    private void StartTopicModelling(String[] args) throws InterruptedException, IOException {
        checkArgs(args);

        //Read input
      //  A1_CSVInput.main(inputArgs);


        // Lemmatise JSON
     //   B2_LemmatiseJSONFile.main(lemmaArgs);


        //run topic models
     //   C1_TopicModelFromJSON.main(superArgs );
     //   C1_TopicModelFromJSON.main(subArgs );

        //run assignment
    //    D1_CreateDownloadableCSV.main(superDownloadableCSVArgs);
    //    D1_CreateDownloadableCSV.main(subDownloadableCSVArgs);

        //run the assignment algorithm
        E3_Assignment.main(assinmentArgs);




    }

    /**
     * Check that we have the arguments required for the topic modelling. The only required ones are the input and
     * output file. If you only specify these, however, all the topic modelling settings will be set to defaults. If you
     * instead specify all the arguments, you can set the number of topics, iterations, and the number of words and
     * documents which will be listed for each topic. These are listed from most related, to least, and you want to
     * not set this too high as it will make the JSON file massive! Finally, we also check that numeric inputs are
     * valid, and if not we crash out.
     *
     * @param args - The argument list directly from main()
     */
    private void checkArgs(String[] args) {
        if (args.length < 3) {
            System.out.println("\nArguments missing! Please supply arguments in the following order: [JSON Location] [Output Location Super] [Output Location Sub] [Number of Super Topics] [Number of Sub Topics] [Number of Iterations] [Number of Words] [Number of Documents]");
            System.exit(1);
        } else if (args.length < 7) {
            System.out.println("\nTopic Model arguments missing. Continuing with defaults -> 30 topics, 2000 iterations, 50 words, 100 Documents.\n\nPlease supply arguments in the following order: " +
                    "[csv input Location] [Output Location] [Number of Super Topics] [Number of Sub Topics] [Number of Iterations] [Number of Words] [Number of Documents]");

        } else {

            String projectName = args[0].substring(args[0].lastIndexOf("/"));
            System.out.println(projectName);


            //input parser args
            inputArgs[0] = args[0];
            inputArgs[1] = args[1]+ projectName.replace(".csv" , ".json");

            //Lemmatise Args
            lemmaArgs[0] = inputArgs[1];
            lemmaArgs[1] = args[1] + projectName.replace(".csv" , "Lemma.json");
            lemmaArgs[2] = String.valueOf(false);
            lemmaArgs[3] = String.valueOf(20);      // min number of lemmas
            lemmaArgs[4] = args[2];



            //args fot topic modelling
            superArgs[0] = lemmaArgs[1];
            superArgs[1] = args[1] + projectName.replace(".csv" , "SuperTP.json");
            superArgs[2] = args[3];                 //number of super topics
            superArgs[3] = args[5];
            superArgs[4] = args[6];
            superArgs[5] = args[7];

            subArgs[0] = lemmaArgs[1];
            subArgs[1] = args[1] + projectName.replace(".csv" , "SubTP.json");
            subArgs[2] = args[4];                  // number of sub topics
            subArgs[3] = args[5];
            subArgs[4] = args[6];
            subArgs[5] = args[7];

           //args for downloadable csvs
            superDownloadableCSVArgs = new String[7];
            superDownloadableCSVArgs[0] = superArgs[1];
            superDownloadableCSVArgs[1] = superArgs[1].replace(".json" , ".csv");
            superDownloadableCSVArgs[2] = String.valueOf(5);    // It will only use top 5 words to create the csv headers
            superDownloadableCSVArgs[3] = String.valueOf(false);                //sort the top words alphabetically?
           // superDownloadableCSVArgs[4] = "Title";
           // superDownloadableCSVArgs[5] = "AuthorsList";
           // superDownloadableCSVArgs[6] = "LeadORName";


            subDownloadableCSVArgs = new String[7];
            subDownloadableCSVArgs[0] = subArgs[1];
            subDownloadableCSVArgs[1] = subArgs[1].replace(".json" , ".csv");
            subDownloadableCSVArgs[2] = String.valueOf(5);    // It will only use top 5 words to create the csv headers
            subDownloadableCSVArgs[3] = String.valueOf(false);                //sort the top words alphabetically?
           // subDownloadableCSVArgs[4] = "Title";
           // subDownloadableCSVArgs[5] = "AuthorsList";
           // subDownloadableCSVArgs[6] = "LeadORName";


            assinmentArgs[0] = superDownloadableCSVArgs[1];
            assinmentArgs[1] = subDownloadableCSVArgs[1];
            assinmentArgs[2] = args[1] + projectName.replace(".csv" , "Assignment.json");
            assinmentArgs[3] = args[3];
            assinmentArgs[4] = args[4];

        }

    }





}
