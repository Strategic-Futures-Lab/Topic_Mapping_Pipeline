import IO.Console;
import IO.ProjectConfig;
import config.Pipeline;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class Main {

    private static final String MODULE_NAME = "Topic Mapping Pipeline";

    public static void main(String[] args) {

        if(args.length < 1){
            Console.error("The Topic Mapping Pipeline requires a YAML project configuration file as parameter.");
            System.exit(1);
        }

        Console.moduleStart(MODULE_NAME);
        long startTime = System.currentTimeMillis();

        try {
            Pipeline pipeline = new Pipeline();
            pipeline.loadProjectConfigurations(args[0]);
        } catch (Exception e) {
            Console.moduleFail(MODULE_NAME);
            System.exit(1);
        }







//        TopicMapping startClass = new TopicMapping();
//        startClass.CheckArgs(args);
//        startClass.LoadProject();
//        startClass.Run();
//        for(String t: times){
//            LogPrint.printNote(t);
//        }

        long timeTaken = (System.currentTimeMillis() - startTime) / (long)1000;
        Console.info("Time taken: "+Math.floorDiv(timeTaken, 60) + " m, " + timeTaken % 60 + " s.");
        Console.moduleComplete(MODULE_NAME);
    }
}
