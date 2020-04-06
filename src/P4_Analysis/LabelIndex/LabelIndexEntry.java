package P4_Analysis.LabelIndex;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LabelIndexEntry {

    public Set<String> mainTopics;
    public Set<String> subTopics;

    public LabelIndexEntry(){
        mainTopics = new HashSet<>();
        subTopics = new HashSet<>();
    }
}
