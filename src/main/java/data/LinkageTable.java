package data;

import IO.Console;
import IO.JSONHelper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Class representing a Linkage Table, used with Agglomerative clustering and agglomerative map layouts
 *
 * @author T. Methven, P. Le Bras
 * @version 3
 */
public class LinkageTable extends Clusters {

    /**
     * Class representing a node in the linkage table
     */
    public static class LinkageNode {
        /** index of first child node linked by this node */
        public int node1;
        /** index of second child node linked by this node */
        public int node2;
        /** distance of linkage */
        public double distance;
        /**
         * Initial constructor
         * @param n1 index of first child node
         * @param n2 index of second child node
         * @param d distance between the two nodes
         */
        public LinkageNode(int n1, int n2, double d) {
            node1 = n1;
            node2 = n2;
            distance = d;
        }

        /**
         * JSON Constructor
         * @param arr JSON array to read node from
         */
        public LinkageNode(JSONArray arr){
            node1 = (int) arr.get(0);
            node2 = (int) arr.get(1);
            distance = (double) arr.get(2);
        }
        /**
         * Method returning the node in JSON format
         * @return JSON formatted node
         */
        public JSONArray toJSON(){
            JSONArray res = new JSONArray();
            res.add(0, node1);
            res.add(1, node2);
            res.add(2, formatDouble(distance));
            return res;
        }

        private double formatDouble(double in){
            DecimalFormat df = new DecimalFormat("#.#####");
            df.setRoundingMode(RoundingMode.HALF_UP);
            return Double.parseDouble(df.format(in));
        }
    }

    // List of static fields used when reading/writing JSON files
    private static final String JSON_LINKAGE = "linkage";

    // List of nodes in the linkage table
    private List<LinkageNode> nodes;

    /**
     * Constructor, initialises an empty list of assignment and linkages
     */
    public LinkageTable(){
        super();
        nodes = new ArrayList<>();
    }

    /**
     * Constructor with initial size, initialises an empty list of assignment and linkages
     * @param size Expected number of items
     */
    public LinkageTable(int size){
        super(size);
        nodes = new ArrayList<>(size-1);
    }

    /**
     * Constructor loading clusters and linkages from file
     * @param filename File to load the clusters from
     * @throws IOException If there is an error with loading the file
     * @throws ParseException If there is an error with parsing the file
     */
    public LinkageTable(String filename) throws IOException, ParseException {
        JSONObject input = super.loadJSON(filename);
        nodes = new ArrayList<>(clusters.size()-1);
        JSONArray linkages = (JSONArray) input.get(JSON_LINKAGE);
        for(JSONArray n: (Iterable<JSONArray>) linkages){
            nodes.add(new LinkageNode(n));
        }
    }

    /**
     * Method adding a new node in the linkage table
     * @param child1 node index of first child
     * @param child2 node index of second child
     * @param distance join distance
     */
    public void addNode(int child1, int child2, double distance){
        nodes.add(new LinkageNode(child1, child2, distance));
    }

//    /**
//     * Method adding a new cluster assignment
//     * @param item ID of item assigned to cluster
//     * @param cluster ID of cluster to assign the item too
//     */
//    public void addAssignment(String item, String cluster){
//        clusters.add(new Pair<String,String>(item, cluster));
//    }

    /**
     * Method generating a JSON object with the cluster information
     */
    @Override
    public JSONObject toJSON() {
        JSONObject root = super.toJSON();
        JSONArray linkages = new JSONArray();
        for(LinkageNode n: nodes){
            linkages.add(n.toJSON());
        }
        root.put(JSON_LINKAGE, linkages);
        return root;
    }

    /**
     * Method writing the cluster information on a JSON file
     * @param filename File to write clusters on
     * @throws IOException If there is an error with writing the file
     */
    @Override
    public void writeClusters(String filename) throws IOException{
        try{
            JSONHelper.saveJSON(this.toJSON(), filename);
        } catch (IOException e){
            Console.error("Saving cluster file "+filename+" failed");
            throw e;
        }
    }

}
