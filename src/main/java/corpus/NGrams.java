package corpus;

import IO.CSVHelper;
import IO.Console;
import IO.Timer;
import data.Document;
import data.Pair;
import pipeline.config.ModuleConfig;
import pipeline.config.ProjectConfig;
import pipeline.config.modules.BuildTextConfig;
import pipeline.config.modules.NGramsConfig;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Module loading a lemmatised corpus JSON file and running a nGram analysis on lemmas
 *
 * @author P. Le Bras
 * @version 1
 */
public class NGrams extends CleaningModule {

    private static final char JOINCHAR = '+';
    private static final char SAVECHAR = '_';

    // module parameters
    private NGramsConfig config;

    // nGrams
    ConcurrentHashMap<String, Integer> unigramsFound;
    ConcurrentHashMap<String, Integer> ngramsFound;
    // using concurrent hashmap for parallel computation
    ConcurrentHashMap<String, Double> ngramsProbabilities;
    // using linked list to sort by probabilities
    LinkedList<Pair<String,Double>> sortedProbabilities;
    List<String> ngramsRead;

    // for logging purposes
    int noLemmas;
    // Flag for processing documents in parallel
    private final static boolean RUN_IN_PARALLEL = false;

    private NGrams(NGramsConfig c){
        config = c;
        config.logConfig();
    }

    /**
     * Main module method - processes parameters, loads corpus, build nGrams, calculate frequency, joins n grams and save corpus again
     * @param moduleParameters module parameters
     * @throws Exception If the corpus cannot load properly
     */
    public static void run(ModuleConfig moduleParameters) throws Exception {
        String MODULE_NAME = moduleParameters.moduleName+" ("+moduleParameters.moduleType+")";
        Console.moduleStart(MODULE_NAME);
        Timer.start(MODULE_NAME);
        NGrams instance = new NGrams((NGramsConfig) moduleParameters);
        try{
            instance.loadCorpus(instance.config.corpusFile);
            if(instance.config.analysis) {
                instance.getNGrams();
                instance.calculateNGramsProbabilities();
                instance.saveNGramsProbabilities();
            } else {
                instance.loadNGrams();
                instance.combineNGrams();
                instance.writeCorpus(instance.config.outputFile);
            }
        } catch (Exception e){
            Console.moduleFail(MODULE_NAME);
            throw e;
        }
        Console.moduleComplete(MODULE_NAME);
        Timer.stop(MODULE_NAME);
    }

    private void getNGrams(){
        Console.log("Getting n-grams");
        noLemmas = 0;
        unigramsFound = new ConcurrentHashMap<>();
        ngramsFound = new ConcurrentHashMap<>();
        if(RUN_IN_PARALLEL) corpus.documents.entrySet().parallelStream().forEach(this::getNGrams);
        else corpus.documents.entrySet().forEach(this::getNGrams);
        if(noLemmas>0) Console.warning(noLemmas+" documents had no lemmatised text to get n-grams from");
        else Console.tick();

        // Below removes unique nGrams: TODO check if should be kept or not
//        int ngramsSizeTotal = ngrams.size();
//        ngrams.entrySet().removeIf(e -> e.getValue() <= 1);
//        int ngramsSizeFiltered = ngrams.size();
//        Console.info("Found "+ngramsSizeTotal+" nGrams in total, removed "+(ngramsSizeTotal-ngramsSizeFiltered)+" unique nGrams ("+ngramsSizeFiltered+" left)",1);
    }

    private void getNGrams(Map.Entry<String, Document> docEntry){
        Document doc = docEntry.getValue();
        StringBuilder term = new StringBuilder();
        if(doc.hasLemmas()) {
            List<List<String>> lemmas = doc.getLemmas();
            for(List<String> sentence: lemmas) {
                for (int i = 0; i < sentence.size(); i++) {
                    term.append(sentence.get(i));
                    if (unigramsFound.containsKey(term.toString()))
                        unigramsFound.put(term.toString(), unigramsFound.get(term.toString()) + 1);
                    else unigramsFound.put(term.toString(), 1);
                    for (int n = 1; n < config.maxNGramSize; n++) {
                        if (i + n < sentence.size()) {
                            term.append(JOINCHAR).append(sentence.get(i + n));
                            if (ngramsFound.containsKey(term.toString()))
                                ngramsFound.put(term.toString(), ngramsFound.get(term.toString()) + 1);
                            else ngramsFound.put(term.toString(), 1);
                        }
                    }
                    term.setLength(0);
                }
            }
        }
        else noLemmas++;
    }

    private void calculateNGramsProbabilities() throws Exception {
        Console.log("Calculating Probabilities");
        ngramsProbabilities = new ConcurrentHashMap<>();
        if (RUN_IN_PARALLEL) {
            ngramsFound.entrySet().parallelStream().forEach(this::calculateNGramProbabilities);
        } else {
            ngramsFound.entrySet().forEach(this::calculateNGramProbabilities);
        }
        Console.tick();
        Console.log("Sorting");
//        ngramsProbabilities.entrySet().removeIf(e -> e.getValue() <= 0.001);
        sortedProbabilities = new LinkedList<>();
        ngramsProbabilities.entrySet().stream().forEach(e->sortedProbabilities.add(new Pair<>(e.getKey(),e.getValue())));
        sortedProbabilities.sort(new Comparator<Pair<String, Double>>() {
            @Override
            public int compare(Pair<String, Double> o1, Pair<String, Double> o2) {
                return -Double.compare(o1.getRight(),o2.getRight());
            }
        });
        Console.tick();
    }

    /** Calculates the probabilities of a ngram using Laplace Smoothing */
    private void calculateNGramProbabilities(Map.Entry<String, Integer> ngram){
        byte k = 1;
        int i = ngram.getKey().lastIndexOf(JOINCHAR);
        String full = ngram.getKey();
        String start = full.substring(0, i);
        double numerator = (double) (ngram.getValue() + k);
        double denominator = start.contains(Character.toString(JOINCHAR)) ? (double) (ngramsFound.get(start) + k * unigramsFound.size()) : (double) (unigramsFound.get(start) + k * unigramsFound.size());
        ngramsProbabilities.put(full, (numerator / denominator));

    }

    private void saveNGramsProbabilities() throws Exception {
        String[] headers = new String[]{"ngram","probability","count"};
        LinkedList<String[]> rows = new LinkedList<>();
        for(Pair<String, Double> p: sortedProbabilities){
            rows.add(new String[]{p.getLeft().replace(JOINCHAR, SAVECHAR),String.valueOf(p.getRight()),String.valueOf(ngramsFound.get(p.getLeft()))});
        }
        CSVHelper.saveCSVFile(config.nGramsFile, headers, rows, 0);
    }

    private void loadNGrams(){
        ngramsRead = readTextFile(config.nGramsFile, "n-grams");
        ngramsRead = ngramsRead.stream().map(s -> s.trim().toLowerCase()).toList();
    }

    private void combineNGrams(){
        Console.log("Combining n-grams");
        noLemmas = 0;
        if(RUN_IN_PARALLEL) corpus.documents.entrySet().parallelStream().forEach(this::combineNGrams);
        else corpus.documents.entrySet().forEach(this::combineNGrams);
        if(noLemmas>0) Console.warning(noLemmas+" documents had no lemmatised text to combine n-grams from");
        else Console.tick();
    }

    private void combineNGrams(Map.Entry<String, Document> docEntry){
        Document doc = docEntry.getValue();
        if(doc.hasLemmas()) {
            List<String> sentences = doc.getLemmaSentences();
            List<String> newSentences = new ArrayList<>();
            for(String sentence: sentences) {
                String newSentence = sentence;
                for(String ngram: ngramsRead){
                    newSentence = newSentence.replaceAll(ngram.replace(SAVECHAR, ' '), ngram);
                }
                newSentences.add(newSentence);
            }
            doc.setLemmaSentences(newSentences);
        }
        else noLemmas++;
    }
}
