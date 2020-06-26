package P5_TopicMapping;

import P0_Project.TopicMappingModuleSpecs;
import PY_Helper.LogPrint;
import PY_Helper.OSValidator;
import org.apache.commons.logging.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class BubbleMap {

    private String mainTopicsFile;
    private String mainOutput;
    private String mapType;
    private String bubbleSize;
    private String bubbleScale;
    private boolean mapSubTopics;
    private String subTopicsFile;
    private String subOutput;

    private String nodeCommand;

    public static String MapTopics(TopicMappingModuleSpecs mapSpecs){

        LogPrint.printModuleStart("Bubble map");

        long startTime = System.currentTimeMillis();

        BubbleMap startClass = new BubbleMap();
        startClass.ProcessArguments(mapSpecs);
        startClass.StartMapping();

        long timeTaken = (System.currentTimeMillis() - startTime) / (long)1000;

        LogPrint.printModuleEnd("Bubble map");

        return "Topic mapping (bubbles): "+Math.floorDiv(timeTaken, 60) + " m, " + timeTaken % 60 + " s";

    }

    private void ProcessArguments(TopicMappingModuleSpecs mapSpecs){
        LogPrint.printNewStep("Processing arguments", 0);
        mainTopicsFile = mapSpecs.mainTopics;
        mainOutput = mapSpecs.mainOutput;
        mapType = mapSpecs.mapType;
        bubbleSize = mapSpecs.bubbleSize;
        bubbleScale = "["+mapSpecs.bubbleScale[0]+","+mapSpecs.bubbleScale[1]+"]";
        mapSubTopics = mapSpecs.mapSubTopics;
        if(mapSubTopics){
            subTopicsFile = mapSpecs.subTopics;
            subOutput = mapSpecs.subOutput;
        }
        nodeCommand = mapSpecs.nodeCommand;
        LogPrint.printCompleteStep();
        if(mapSubTopics) LogPrint.printNote("Mapping sub topics");
        LogPrint.printNote("Using "+bubbleSize+" distribution for bubble sizes");
        LogPrint.printNote("Using "+bubbleScale+" scale for bubble sizes");
        LogPrint.printNote("Using node command: "+nodeCommand);
    }

    private void StartMapping(){
        String[] args = new String[5];
        args[0] = mainTopicsFile;
        args[1] = mainOutput;
        args[2] = "true";
        args[3] = bubbleSize;
        args[4] = bubbleScale;
        callNodeJS(args);
        if(mapSubTopics){
            args[0] = subTopicsFile;
            args[1] = subOutput;
            args[2] = "false";
            callNodeJS(args);
        }
    }

    // -- Linux --
    // - Run a shell command
    // processBuilder.command("bash", "-c", "ls /home/");
    // - Run a shell script
    // processBuilder.command("path/to/hello.sh");

    // -- Windows --
    // - Run a command
    // processBuilder.command("cmd.exe", "/c", "dir C:\\Users");
    // - Run a bat file
    // processBuilder.command("C:\\path\\to\\hello.bat");

    private void callNodeJS(String[] arguments){
        String args = String.join(" ", arguments);
        ProcessBuilder processBuilder = new ProcessBuilder();
        if(OSValidator.isMac() || OSValidator.isUnix()){
            processBuilder.command("bash", "-c", nodeCommand+" js_scripts/bubbleMap/index.js "+args);
        } else if(OSValidator.isWindows()){
            processBuilder.command("cmd.exe", "/c", nodeCommand+" js_scripts\\bubbleMap\\index.js "+args);
        }
        try {
            LogPrint.printNewStep("Calling Node JS", 0);
            Process process = processBuilder.start();
            // StringBuilder output = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                // output.append(line + "\n");
                LogPrint.printExternalStep(line, 1);
            }
            int exitVal = process.waitFor();
            if (exitVal == 0) {
                // System.out.println(output);
                // System.out.println("Node JS Finished!");
                // System.exit(0);
            } else {
                LogPrint.printNoteError("Node JS failed");
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

    }
}
