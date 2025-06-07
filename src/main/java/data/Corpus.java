package data;

import IO.Console;
import IO.JSONHelper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.util.IntSummaryStatistics;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Class representing a corpus (collection of documents).
 * Provides method for loading a typical corpus file.
 *
 * @author P. Le Bras
 * @version 1
 */
public class Corpus {

    // List of static fields used when reading/writing JSON files
    private static final String JSON_NAME = "name";
    private static final String JSON_STATS = "stats";
    private static final String JSON_DOCS = "documents";

    /** Corpus name */
    public String name;
    /** List of documents in the corpus, identified with a String */
    public ConcurrentHashMap<String, Document> documents;
    /** Stats attached to the corpus */
    public JSONObject stats;

    /** Basic constructor, creates empty corpus */
    public Corpus(){
        name = "corp"+this.hashCode();
        stats = new JSONObject();
        documents = new ConcurrentHashMap<>();
    }

    /**
     * Constructor loading corpus from file
     * @param filename File to load corpus from
     * @throws IOException If there is an error with loading the file
     * @throws ParseException If there is an error with parsing JSON
     */
    public Corpus(String filename) throws IOException, ParseException {
        documents = new ConcurrentHashMap<>();
        loadCorpus(filename);
    }

    /**
     * Returns the corpus size (number of documents)
     * @return Number of documents in the corpus
     */
    public int size(){
        return documents.size();
    }

    /**
     * Adds a document to the corpus
     * @param id Document ID
     * @param doc Document
     */
    public void add(String id, Document doc){
        documents.put(id, doc);
    }

    /**
     * Loads a corpus from a JSON file
     * @param filename File to load corpus from
     * @throws IOException If there is an error with loading the file
     * @throws ParseException If there is an error with parsing JSON
     */
    public void loadCorpus(String filename) throws IOException, ParseException {
        try {
            JSONObject input = JSONHelper.loadJSON(filename);
            name = (String) input.get(JSON_NAME);
            stats = (JSONObject) input.getOrDefault(JSON_STATS, new JSONObject());
            JSONArray corpus = (JSONArray) input.get(JSON_DOCS);
            for(JSONObject jsonDoc: (Iterable<JSONObject>) corpus){
                Document doc = new Document(jsonDoc);
                documents.put(doc.getId(), doc);
            }
            Console.note("Loaded "+documents.size()+" documents", 1);
        } catch (IOException e) {
            Console.error("Loading corpus file "+filename+" failed");
            throw e;
        } catch (ParseException e) {
            Console.error("Parsing corpus file "+filename+" failed");
            throw e;
        }
    }

    /**
     * Writes the corpus on a JSON file
     * @param filename File to write corpus on
     * @throws IOException If there is an error with writing the file
     */
    public void writeCorpus(String filename) throws IOException{
        try {
            JSONObject root = new JSONObject();
            JSONArray corpus = new JSONArray();
            root.put(JSON_NAME, name);
            buildStats();
            root.put(JSON_STATS, stats);
            for (Map.Entry<String, Document> doc : documents.entrySet()) {
                corpus.add(doc.getValue().toJSON());
            }
            root.put(JSON_DOCS, corpus);
            JSONHelper.saveJSON(root, filename);
        } catch (IOException e){
            Console.error("Saving corpus file "+filename+" failed");
            throw e;
        }
    }

    private void buildStats(){
        // TODO: improve stats method
        int nDocs = documents.size();
        long nModelled = documents.entrySet().parallelStream().filter(d->d.getValue().isModelled()).count();
        long nLemmatised = documents.entrySet().parallelStream().filter(d->d.getValue().hasLemmas()).count();
        long nEmpty = documents.entrySet().parallelStream().filter(d->d.getValue().emptyText()).count();
        stats.put("n",nDocs);
        stats.put("empty",nEmpty);
        stats.put("lemmatised",nLemmatised);
        stats.put("modelled",nModelled);
        if(nLemmatised > 0) {
            JSONObject lemmaStats = new JSONObject();
            IntSummaryStatistics summaryStats = documents.entrySet().parallelStream().collect(Collectors.summarizingInt(d->d.getValue().getNumLemmas()));
            lemmaStats.put("min",summaryStats.getMin());
            lemmaStats.put("max",summaryStats.getMax());
            lemmaStats.put("avg",summaryStats.getAverage());
            int[] thresholds = new int[]{10,20,30,40,50};
            for(int t: thresholds){
                long lemmaBelowT = documents.entrySet().parallelStream().filter(d->d.getValue().getNumLemmas()<t).count();
                lemmaStats.put("<"+t,lemmaBelowT);
            }
            stats.put("lemmas",lemmaStats);
        }
    }

}
