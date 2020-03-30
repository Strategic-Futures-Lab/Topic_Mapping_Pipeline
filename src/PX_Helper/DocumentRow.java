package PX_Helper;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.HashMap;
import java.util.List;

/**
 * Created by Tom on 18/04/2018.
 */
public class DocumentRow
{
    private String ID;
    private boolean IncludedInModel = true;
    private String RemovalReason = "";
    private int JSONRow;
    private double[] TopicDistribution;
    private String LemmaStringData;
    private int NumLemmas = -1;

    HashMap<String, String> values = new HashMap<>();

    public DocumentRow()
    {

    }

    public String getID()
    {
        return ID;
    }

    public void setID(String ID)
    {
        this.ID = ID;
    }

    public boolean isIncludedInModel()
    {
        return IncludedInModel;
    }

    public void setIncludedInModel(boolean IncludedInModel, String RemovalReason)
    {
        this.IncludedInModel = IncludedInModel;
        this.RemovalReason = RemovalReason;
    }

    public void setIncludedInModel(boolean IncludedInModel) { this.IncludedInModel = IncludedInModel; }

    public String getRemovalReason() { return RemovalReason; }

    public void setRemovalReason(String RemovalReason) { this.RemovalReason = RemovalReason; }

    public int getJSONRow()
    {
        return JSONRow;
    }

    public void setJSONRow(int JSONRow)
    {
        this.JSONRow = JSONRow;
    }

    public double[] getTopicDistribution() { return TopicDistribution; }
    public void setTopicDistribution(double[] distribution) { TopicDistribution = distribution; }

    public String getLemmaStringData()
    {
        return LemmaStringData;
    }

    public void setLemmaStringData(String lemmaStringData)
    {
        LemmaStringData = lemmaStringData;
    }

    public void setLemmas(List<String> lemmas)
    {
        setNumLemmas(lemmas.size());

        LemmaStringData = "";

        lemmas.forEach(text -> LemmaStringData += text + " ");

        LemmaStringData = LemmaStringData.trim();
    }

    public void setNumLemmas(int value)
    {
        NumLemmas = value;
    }

    public long getNumLemmas()
    {
        return NumLemmas;
    }

    public String getValue(String key)
    {
        return values.get(key);
    }

    public void setValue(String key, String value)
    {
        values.put(key, value);
    }

    public void removeValue(String key)
    {
        values.remove(key);
    }

    public HashMap<String, String> getValues()
    {
        return values;
    }

   // public void setValues(String key, JSONArray arr) { values.put(key , arr) ;}
}
