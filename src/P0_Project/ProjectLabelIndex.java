package P0_Project;

import org.json.simple.JSONObject;

public class ProjectLabelIndex implements ModuleSpecs {

    public String mainTopics;
    public boolean indexSubTopics = false;
    public String subTopics;
    public String indexOutput;

    public void getSpecs(JSONObject specs) {

        mainTopics = (String) specs.get("mainTopics");
        indexOutput = (String) specs.get("output");
        subTopics = (String) specs.getOrDefault("subTopics", "");
        indexSubTopics = subTopics.length() > 0;

    }
}
