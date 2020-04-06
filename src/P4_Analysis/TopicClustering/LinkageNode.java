package P4_Analysis.TopicClustering;

public class LinkageNode {

    public boolean isLeaf;
    public String nodeId;
    public int nodeIndex;
    public LinkageNode childA;
    public LinkageNode childB;
    public double distance;

    public LinkageNode(String id, int index){
        nodeId = id;
        isLeaf = true;
        nodeIndex = index;
    }

    public LinkageNode(String id, int index, LinkageNode a, LinkageNode b, double d){
        nodeId = id;
        isLeaf = false;
        nodeIndex = index;
        childA = a;
        childB = b;
        distance = d;
    }

    public int nLeaves(){
        if(isLeaf){
            return 1;
        } else {
            return childA.nLeaves() + childB.nLeaves();
        }
    }
}
