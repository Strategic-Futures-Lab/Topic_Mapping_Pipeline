package P1_Input;

import PX_Helper.JSONIOWrapper;
import org.json.simple.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class CorpusJSONDocument {
    private String id;
    private HashMap<String, String> fields;
    private int index;

    public CorpusJSONDocument(String docId, int docIndex){
        id = docId;
        index = docIndex;
        fields = new HashMap<String, String>();
    }

    public CorpusJSONDocument(JSONObject jsonDoc){
        id = (String) jsonDoc.get("id");
        fields = JSONIOWrapper.getStringMap((JSONObject) jsonDoc.get("fields"));
    }

    public void addField(String key, String value){
        fields.put(key, value);
    }

    public String getId(){
        return id;
    }

    public int getIndex(){ return index;}

    public HashMap<String, String> getFields(){
        return fields;
    }

    public String getFieldValue(String key){
        return fields.get(key);
    }

    public JSONObject toJSON(){
        JSONObject root = new JSONObject();
        root.put("id", id);
        root.put("index", index);
        JSONObject fieldsData = new JSONObject();
        for(Map.Entry<String, String> entry: fields.entrySet()){
            fieldsData.put(entry.getKey(), entry.getValue());
        }
        root.put("fields", fieldsData);
        return root;
    }
}
