package P2_Lemmatise;

import PX_Helper.JSONIOWrapper;
import org.json.simple.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LemmaJSONDocument {
    private String id;
    private int index;
    private HashMap<String, String> textFields;
    private HashMap<String, String> docFields;
    private String lemmaString;
    private int numLemma;
    private boolean removed = false;
    private String removeReason = "";

    public LemmaJSONDocument(JSONObject jsonDoc, List<String> textKeys, List<String> docKeys){
        id = (String) jsonDoc.get("docId");
        index = Math.toIntExact((long) jsonDoc.get("index"));
        HashMap<String, String> fields = JSONIOWrapper.getStringMap((JSONObject) jsonDoc.get("fields"));
        textFields = new HashMap<>();
        docFields = new HashMap<>();
        for(Map.Entry<String, String> field: fields.entrySet()){
            String k = field.getKey();
            if(textKeys.contains(k)) textFields.put(k, field.getValue());
            if(docKeys.contains(k)) docFields.put(k, field.getValue());
        }
    }

    public String getId(){
        return id;
    }

    public int getIndex(){ return index;}

    public HashMap<String, String> getTextFields(){
        return textFields;
    }

    public String getTextFieldValue(String key){
        return textFields.get(key);
    }

    public HashMap<String, String> getDocFields(){
        return docFields;
    }

    public String getDocFieldValue(String key){
        return docFields.get(key);
    }

    public void setLemmas(List<String> lemmas){
        numLemma = lemmas.size();
        lemmaString = "";
        lemmas.forEach(text -> lemmaString += text + " ");
        lemmaString = lemmaString.trim();
    }

    public void remove(String reason){
        removed = true;
        removeReason = reason;
    }

    public JSONObject toJSON(){
        JSONObject root = new JSONObject();
        root.put("docId", id);
        root.put("index", index);
        JSONObject docData = new JSONObject();
        for(Map.Entry<String, String> entry: docFields.entrySet()){
            docData.put(entry.getKey(), entry.getValue());
        }
        root.put("docData", docData);
        root.put("lemmas", lemmaString);
        if(removed){
            root.put("removed", true);
            root.put("removeReason", removeReason);
        }
        return root;
    }
}
