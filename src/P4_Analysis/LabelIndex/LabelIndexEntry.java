package P4_Analysis.LabelIndex;

import java.util.*;

public class LabelIndexEntry {

    public Set<String> mainTopics;
    public HashMap<String, Set<String>> subTopics;
    public Set<String> documents;

    public LabelIndexEntry(){
        mainTopics = new HashSet<>();
        subTopics = new HashMap<>();
        documents = new HashSet<>();
    }
}
