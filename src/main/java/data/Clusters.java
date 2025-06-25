package data;

import IO.Console;
import IO.JSONHelper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Class representing the result of a clustering algorithm
 *
 * @author P. Le Bras
 * @version 1
 */
public class Clusters {

    // List of static fields used when reading/writing JSON files
    protected static final String JSON_CLUSTERS = "clusters";
    protected static final String JSON_ITEMID = "i";
    protected static final String JSON_CLUSTERID = "c";

    // list of cluster assignment
    protected List<Pair<String, String>> clusters;

    /**
     * Constructor, initialises an empty list of assignment
     */
    public Clusters(){
        clusters = new ArrayList<>();
    }

    /**
     * Constructor with initial size, initialises an empty list of assignment
     * @param size Expected number of items
     */
    public Clusters(int size){
        clusters = new ArrayList<>(size);
    }

    /**
     * Constructor loading clusters from file
     * @param filename File to load the clusters from
     * @throws IOException If there is an error with loading the file
     * @throws ParseException If there is an error with parsing the file
     */
    public Clusters(String filename) throws IOException, ParseException {
        loadJSON(filename);
    }

    /**
     * Method adding a new cluster assignment
     * @param item ID of item assigned to cluster
     * @param cluster ID of cluster to assign the item too
     */
    public void addAssignment(String item, String cluster){
        clusters.add(new Pair<String,String>(item, cluster));
    }

    /**
     * Method retrieving all items assigned to a given cluster
     * @param cluster ID of cluster to retrieve items from
     * @return List of items assigned th the cluster
     */
    public List<String> getItemsInCluster(String cluster){
        return clusters.parallelStream()
                .filter(p->p.getRight().equals(cluster))
                .map(Pair::getLeft)
                .toList();
    }

    /**
     * Method writing the cluster assignments on a JSON file
     * @param filename File to write clusters on
     * @throws IOException If there is an error with writing the file
     */
    public void writeClusters(String filename) throws IOException{
        try{
            JSONObject root = this.toJSON();
            JSONHelper.saveJSON(root, filename);
        } catch (IOException e){
            Console.error("Saving cluster file "+filename+" failed");
            throw e;
        }
    }

    // Separate to/from JSON that can be used by subclasses

    protected JSONObject toJSON(){
        JSONObject root = new JSONObject();
        JSONArray arr = new JSONArray();
        for(Pair<String, String> c: clusters){
            JSONObject assign = new JSONObject();
            assign.put(JSON_ITEMID, c.getLeft());
            assign.put(JSON_CLUSTERID, c.getRight());
            arr.add(assign);
        }
        root.put(JSON_CLUSTERS, arr);
        return root;
    }

    protected JSONObject loadJSON(String filename) throws IOException, ParseException {
        try{
            JSONObject input = JSONHelper.loadJSON(filename);
            JSONArray arr = (JSONArray) input.get(JSON_CLUSTERS);
            clusters = new ArrayList<>(arr.size());
            for(JSONObject assign: (Iterable<JSONObject>) arr){
                clusters.add(new Pair<>((String) assign.get(JSON_ITEMID), (String) assign.get(JSON_CLUSTERID)));
            }
            return input;
        } catch (IOException e) {
            Console.error("Loading cluster file "+filename+" failed");
            throw e;
        } catch (ParseException e) {
            Console.error("Parsing cluster file "+filename+" failed");
            throw e;
        }
    }
}
