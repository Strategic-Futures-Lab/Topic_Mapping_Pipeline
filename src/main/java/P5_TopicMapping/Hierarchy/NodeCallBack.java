package P5_TopicMapping.Hierarchy;

/**
 * Interface for to implement callbacks on hierarchy nodes
 *
 * @author P. Le Bras
 * @version 1
 */
public interface NodeCallBack<Node extends HierarchyNode> {
    /**
     * Callback method to execute on a hierarchy node
     * @param n node to execute the callback on
     */
    void method(Node n);
}
