package PX_Helper;

import PX_Helper.JSONIOWrapper;
import org.json.simple.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class CorpusDocument {
    private String id;
    private HashMap<String, String> fields;

    public CorpusDocument(String docId){
        id = docId;
        fields = new HashMap<String, String>();
    }

    public CorpusDocument(JSONObject jsonDoc){
        id = (String) jsonDoc.get("id");
        fields = JSONIOWrapper.getStringMap((JSONObject) jsonDoc.get("fields"));
    }

    public void addField(String key, String value){
        fields.put(key, value);
    }

    public String getId(){
        return id;
    }

    public HashMap<String, String> getFields(){
        return fields;
    }

    public String getFieldValue(String key){
        return fields.get(key);
    }

    public JSONObject toJSON(){
        JSONObject root = new JSONObject();
        root.put("id", id);
        JSONObject fieldsData = new JSONObject();
        for(Map.Entry<String, String> entry: fields.entrySet()){
            fieldsData.put(entry.getKey(), entry.getValue());
        }
        root.put("fields", fieldsData);
        return root;
    }
}
