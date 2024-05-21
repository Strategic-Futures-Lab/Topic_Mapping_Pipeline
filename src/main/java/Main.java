import IO.Console;
import IO.Timer;
import pipeline.Pipeline;

public class Main {

    private static final String MODULE_NAME = "Topic Mapping Pipeline";

    public static void main(String[] args) {

        if(args.length < 1){
            Console.error("The Topic Mapping Pipeline requires a YAML project configuration file as parameter.");
            System.exit(1);
        }

        Console.moduleStart(MODULE_NAME);
        Timer.start(MODULE_NAME);
        Pipeline pipeline = new Pipeline();
        try {
            pipeline.loadProjectConfigurations(args[0]);
            pipeline.runPipeline();
        } catch (Exception e) {
            Timer.print();
            Console.moduleFail(MODULE_NAME);
            System.exit(1);
        }

        Timer.stop(MODULE_NAME);
        Timer.print();
        Console.moduleComplete(MODULE_NAME);
    }
}
