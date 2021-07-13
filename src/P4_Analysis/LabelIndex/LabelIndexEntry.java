package P4_Analysis.LabelIndex;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.*;

/**
 * Class representing an entry in the label index.
 *
 * @author P. Le Bras
 * @version 1
 */
public class LabelIndexEntry {

    /** Value of the label indexed. */
    public String label;

    /** Set of main topic ids where the label is present in the top words. */
    public Set<String> mainTopics;
    /** Set of sub topic ids (and their assigned main topic) where the label is present in the top words. */
    public HashMap<String, Set<String>> subTopics;
    /** Set of document ids where the label is present. */
    public Set<String> documents;

    /**
     * Constructor, instantiates empty sets.
     * @param l Label value.
     */
    public LabelIndexEntry(String l){
        label = l;
        mainTopics = new HashSet<>();
        subTopics = new HashMap<>();
        documents = new HashSet<>();
    }

    /**
     * Method returning the label index entry as a JSON object to write on file.
     * @param indexSubTopics Boolean flag for writing the sub topic ids.
     * @param indexDocuments Boolean flag for writing the main topics ids.
     * @return The label index entry in JSON format.
     */
    public JSONObject toJSON(boolean indexSubTopics, boolean indexDocuments){
        JSONObject ids = new JSONObject();
        // adding all main topic ids
        JSONArray mainTopicIds = new JSONArray();
        mainTopicIds.addAll(mainTopics);
        ids.put("mainTopics", mainTopicIds);
        // adding all sub topic ids
        if(indexSubTopics){
            JSONArray subTopicIds = new JSONArray();
            for(Map.Entry<String, Set<String>> subIds : subTopics.entrySet()){
                JSONArray subTopicId = new JSONArray();
                JSONArray subTopicMainIds = new JSONArray();
                subTopicMainIds.addAll(subIds.getValue());
                subTopicId.add(subIds.getKey());
                subTopicId.add(subTopicMainIds);
                subTopicIds.add(subTopicId);
            }
            ids.put("subTopics", subTopicIds);
        }
        // adding all document ids
        if(indexDocuments){
            JSONArray docIds = new JSONArray();
            docIds.addAll(documents);
            ids.put("documents", docIds);
        }
        return ids;
    }
}
