package PX_Helper;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.HashMap;

public class ProjectManager {

    public class Input{
        public String module;
        public String source;
        public HashMap<String, String> fields;
        public String output;

        public void getInputSpecs(JSONObject specs){
            module = (String) specs.get("module");
            source = (String) specs.get("source");
            fields = JSONIOWrapper.getStringMap((JSONObject) specs.get("fields"));
            output = (String) specs.get("output");
        }
    }

    public class Lemmatise{
        public String corpus;
        public String[] textFields;
        public String[] docFields;
        public String[] stopWords;
        public int minLemmas;
        public String output;

        public void getLemmatiseSpecs(JSONObject specs){
            corpus = (String) specs.get("corpus");
            textFields = JSONIOWrapper.getStringArray((JSONArray) specs.get("textFields"));
            docFields = JSONIOWrapper.getStringArray((JSONArray) specs.get("docFields"));
            stopWords = JSONIOWrapper.getStringArray((JSONArray) specs.get("stopWords"));
            minLemmas = Math.toIntExact((long) specs.get("minLemmas"));
            output = (String) specs.get("output");
        }
    }

    public static boolean runInput;
    public static Input input;
    public static boolean runLemmatise;
    public static Lemmatise lemmatise;

    public ProjectManager(String projectFile){
        JSONObject projectSpec = JSONIOWrapper.LoadJSON(projectFile);
        getRuns((JSONObject) projectSpec.get("run"));
        if(runInput){
            input = new Input();
            input.getInputSpecs((JSONObject) projectSpec.get("input"));
        }
        if(runLemmatise){
            lemmatise = new Lemmatise();
            lemmatise.getLemmatiseSpecs((JSONObject) projectSpec.get("lemmatise"));
        }
    }

    private void getRuns(JSONObject specs){
        runInput = (boolean) specs.get("input");
        runLemmatise = (boolean) specs.get("lemmatise");
    }

}