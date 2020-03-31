package P0_Project;

import PX_Helper.JSONIOWrapper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class ProjectLemmatise {
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
