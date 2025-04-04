package corpus;

import IO.CSVHelper;
import IO.Console;
import IO.Timer;
import data.Document;
import data.Pair;
import pipeline.config.ModuleConfig;
import pipeline.config.ProjectConfig;
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

    // module parameters
    private String nGramsFile;
    private double threshold;
    private int maxSize;
    private String nGramsOutputFile;
    private boolean analysing;

    // nGrams
    ConcurrentHashMap<String, Integer> unigrams;
    ConcurrentHashMap<String, Integer> ngrams;
    // using concurrent hashmap for parallel computation
    ConcurrentHashMap<String, Double> ngramsProbabilities;
    // using linked list to sort by probabilities
    LinkedList<Pair<String,Double>> sortedProbabilities;

    // for logging purposes
    int noLemmas;
    // Flag for processing documents in parallel
    private final static boolean RUN_IN_PARALLEL = true;

    /**
     * Main module method - processes parameters, loads corpus, build nGrams, calculate frequency, joins n grams and save corpus again
     * @param moduleParameters module parameters
     * @param projectParameters project meta parameters
     * @throws Exception If the corpus cannot load properly
     */
    public static void run(ModuleConfig moduleParameters, ProjectConfig projectParameters) throws Exception {
        String MODULE_NAME = moduleParameters.moduleName+" ("+moduleParameters.moduleType+")";
        Console.moduleStart(MODULE_NAME);
        Timer.start(MODULE_NAME);
        NGrams instance = new NGrams();
        instance.processParameters((NGramsConfig) moduleParameters, projectParameters);
        try{
            instance.loadCorpus();
            if(instance.analysing) {
                instance.getNGrams();
                instance.calculateNGramsProbabilities();
                instance.saveNGramsProbabilities();
            } else {
                //TODO: add other pathway - save given ngrams in corpus
                instance.writeCorpus();
            }
        } catch (Exception e){
            Console.moduleFail(MODULE_NAME);
            throw e;
        }
        Console.moduleComplete(MODULE_NAME);
        Timer.stop(MODULE_NAME);
    }

    // processes project and module parameters
    private void processParameters(NGramsConfig moduleParameters, ProjectConfig projectParameters){
        Console.log("Processing parameters");
        corpusFile = projectParameters.dataDirectory+moduleParameters.corpus;
        outputFile = projectParameters.dataDirectory+moduleParameters.output;
        nGramsFile = moduleParameters.nGrams == null ? null : projectParameters.sourceDirectory+moduleParameters.nGrams;
        nGramsOutputFile = moduleParameters.nGramsOutput == null ? null : projectParameters.dataDirectory+moduleParameters.nGramsOutput;
        threshold = moduleParameters.threshold;
        maxSize = moduleParameters.maxSize;
        analysing = nGramsOutputFile != null;
        Console.tick();
        String task = analysing ? "Analysing" : "Building";
        String saveDiff = corpusFile.equals(outputFile)||analysing ? "" : " and saving to "+outputFile;
        Console.info(task+" nGrams in corpus "+corpusFile+saveDiff, 1);
        if(analysing){
            Console.info("Analysing nGrams up to "+maxSize+" terms", 2);
            Console.info("Saving analysis in "+nGramsOutputFile, 2);
        }
        // TODO
//        Console.info("Building nGrams with frequency greater that "+threshold*100+" %", 2);
//        if(nGramsFile != null) Console.info("Building nGrams in "+nGramsFile, 2);
    }

    private void getNGrams(){
        Console.log("Getting nGrams");
        noLemmas = 0;
        unigrams = new ConcurrentHashMap<>();
        ngrams = new ConcurrentHashMap<>();
        if(RUN_IN_PARALLEL) corpus.documents.entrySet().parallelStream().forEach(this::getNGrams);
        else corpus.documents.entrySet().forEach(this::getNGrams);
        if(noLemmas>0) Console.warning(noLemmas+" documents had no lemmatised text to get nGrams from");
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
            List<String> lemmas = doc.getLemmas();
            for(int i = 0; i < lemmas.size(); i++) {
                term.append(lemmas.get(i));
                if (unigrams.containsKey(term.toString())) unigrams.put(term.toString(), unigrams.get(term.toString()) + 1);
                else unigrams.put(term.toString(), 1);
                for(int n = 1; n < maxSize; n++){
                    if(i+n<lemmas.size()) {
                        term.append("-").append(lemmas.get(i + n));
                        if (ngrams.containsKey(term.toString())) ngrams.put(term.toString(), ngrams.get(term.toString()) + 1);
                        else ngrams.put(term.toString(), 1);
                    }
                }
                term.setLength(0);
            }
        }
        else noLemmas++;
    }

    private void calculateNGramsProbabilities() throws Exception {
        Console.log("Calculating Probabilities");
        ngramsProbabilities = new ConcurrentHashMap<>();
        if (RUN_IN_PARALLEL) {
            ngrams.entrySet().parallelStream().forEach(this::calculateNGramProbabilities);
        } else {
            ngrams.entrySet().forEach(this::calculateNGramProbabilities);
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
        int i = ngram.getKey().lastIndexOf("-");
        String full = ngram.getKey();
        String start =  full.substring(0, i);
        double numerator = (double)(ngram.getValue()+k);
        double denominator = start.contains("-") ? (double)(ngrams.get(start)+k*unigrams.size()) : (double)(unigrams.get(start)+k*unigrams.size());
        ngramsProbabilities.put(full, (numerator/denominator));
    }

    private void saveNGramsProbabilities() throws Exception {
        String[] headers = new String[]{"ngram","probability","count"};
        LinkedList<String[]> rows = new LinkedList<>();
        for(Pair<String, Double> p: sortedProbabilities){
            rows.add(new String[]{p.getLeft(),String.valueOf(p.getRight()),String.valueOf(ngrams.get(p.getLeft()))});
        }
        CSVHelper.saveCSVFile(nGramsOutputFile, headers, rows, 0);
    }
}
