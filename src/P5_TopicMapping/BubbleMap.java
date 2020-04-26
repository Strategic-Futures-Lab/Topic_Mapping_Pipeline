package P5_TopicMapping;

import P0_Project.TopicMappingModuleSpecs;
import PY_Helper.OSValidator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class BubbleMap {

    private String mainTopicsFile;
    private String mainOutput;
    private String mapType;
    private String bubbleSize;
    private boolean mapSubTopics;
    private String subTopicsFile;
    private String subOutput;

    public static void MapTopics(TopicMappingModuleSpecs mapSpecs){
        System.out.println( "**********************************************************\n" +
                            "* STARTING Bubble Map !                                  *\n" +
                            "**********************************************************\n");

        BubbleMap startClass = new BubbleMap();
        startClass.ProcessArguments(mapSpecs);
        startClass.StartMapping();

        System.out.println( "**********************************************************\n" +
                            "* Bubble Map Complete !                                  *\n" +
                            "**********************************************************\n");
    }

    private void ProcessArguments(TopicMappingModuleSpecs mapSpecs){
        mainTopicsFile = mapSpecs.mainTopics;
        mainOutput = mapSpecs.mainOutput;
        mapType = mapSpecs.mapType;
        bubbleSize = mapSpecs.bubbleSize;
        mapSubTopics = mapSpecs.mapSubTopics;
        if(mapSubTopics){
            subTopicsFile = mapSpecs.subTopics;
            subOutput = mapSpecs.subOutput;
        }
    }

    private void StartMapping(){
        String[] args = new String[4];
        args[0] = mainTopicsFile;
        args[1] = mainOutput;
        args[2] = "true";
        args[3] = bubbleSize;
        callNodeJS(args);
        if(mapSubTopics){
            args[0] = subTopicsFile;
            args[1] = subOutput;
            args[2] = "false";
            args[3] = bubbleSize;
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
            processBuilder.command("bash", "-c", "node js_scripts/bubbleMap/index.js "+args);
        } else if(OSValidator.isWindows()){
            processBuilder.command("cmd.exe", "/c", "node js_scripts\\bubbleMap\\index.js "+args);
        }
        try {
            System.out.println("Calling Node JS ...");
            Process process = processBuilder.start();
            StringBuilder output = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line + "\n");
            }
            int exitVal = process.waitFor();
            if (exitVal == 0) {
                System.out.println(output);
                System.out.println("Node JS Finished!");
                // System.exit(0);
            } else {
                System.out.println("Node JS Failed!");
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

    }
}
