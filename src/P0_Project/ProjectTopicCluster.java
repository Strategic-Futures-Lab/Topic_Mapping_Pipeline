package P0_Project;

import org.json.simple.JSONObject;

public class ProjectTopicCluster implements ModuleSpecs {

    public String mainTopics;
    public String mainOutput;
    public String linkageMethod;
    public boolean clusterSubTopics = false;
    public String subTopics;
    public String subOutput;

    public void getSpecs(JSONObject specs){
        mainTopics = (String) specs.get("mainTopics");
        mainOutput = (String) specs.get("mainOutput");
        linkageMethod = (String) specs.get("linkageMethod"); // max | min | avg
        subTopics = (String) specs.getOrDefault("subTopics", "");
        clusterSubTopics = subTopics.length() > 0;
        if(clusterSubTopics){
            subOutput = (String) specs.get("subOutput");
        }
    }
}
