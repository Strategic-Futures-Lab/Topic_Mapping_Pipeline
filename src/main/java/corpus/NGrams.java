package corpus;

import IO.Console;
import IO.Timer;
import data.Document;
import data.Pair;
import pipeline.config.ModuleConfig;
import pipeline.config.ProjectConfig;
import pipeline.config.modules.NGramsConfig;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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

    // nGrams
    ConcurrentHashMap<String, Integer> unigrams;
    ConcurrentHashMap<String, Integer> bigrams;
    ConcurrentHashMap<String, Integer> trigrams;
    ConcurrentHashMap<String, Double> bigramsPMI;
    ConcurrentHashMap<String, Double> bigramsProbabilities;
    ConcurrentHashMap<String, Double> trigramsPMI;
    int nUnigrams;
    int nBigrams;
    int nTrigrams;


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
            //TODO
            instance.getNGrams();
            instance.calculateProbabilities();
            instance.writeCorpus();
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
        threshold = moduleParameters.threshold;
        Console.tick();
        String saveDiff = corpusFile.equals(outputFile) ? "" : " and saving to "+outputFile;
        Console.info("Analysing nGrams in corpus "+corpusFile+saveDiff, 1);
        Console.info("Building nGrams with frequency greater that "+threshold*100+" %", 2);
        if(nGramsFile != null) Console.info("Building nGrams in "+nGramsFile, 2);
    }

    private void getNGrams(){
        Console.log("Getting nGrams");
        noLemmas = 0;
        unigrams = new ConcurrentHashMap<>();
        bigrams = new ConcurrentHashMap<>();
        trigrams = new ConcurrentHashMap<>();
        if(RUN_IN_PARALLEL) corpus.documents.entrySet().parallelStream().forEach(this::getNGrams);
        else corpus.documents.entrySet().forEach(this::getNGrams);
        if(noLemmas>0) Console.warning(noLemmas+" documents had no lemmatised text to get nGrams from");
        else Console.tick();
//        nUnigrams = unigrams.values().stream().mapToInt(i->i).sum();
//        nBigrams = bigrams.values().stream().mapToInt(i->i).sum();
//        nTrigrams = trigrams.values().stream().mapToInt(i->i).sum();

//        nUnigrams = unigrams.size();
//        nBigrams = bigrams.size();
//        nTrigrams = trigrams.size();
//        bigrams.entrySet().removeIf(entry -> entry.getValue() <= 1);
//        trigrams.entrySet().removeIf(entry -> entry.getValue() <= 1);
    }

    private void getNGrams(Map.Entry<String, Document> docEntry){
        Document doc = docEntry.getValue();
        if(doc.hasLemmas()) {
            List<String> lemmas = doc.getLemmas();
            for(int i = 0; i < lemmas.size(); i++) {
                String unigram = lemmas.get(i);
                if(unigrams.containsKey(unigram)) unigrams.put(unigram, unigrams.get(unigram)+1);
                else unigrams.put(unigram, 1);
                if(i<lemmas.size()-1){
                    String bigram = lemmas.get(i)+"-"+lemmas.get(i+1);
                    if(bigrams.containsKey(bigram)) bigrams.put(bigram, bigrams.get(bigram)+1);
                    else bigrams.put(bigram, 1);
                }
                if(i<lemmas.size()-2){
                    String trigram = lemmas.get(i)+"-"+lemmas.get(i+1)+"-"+lemmas.get(i+2);
                    if(trigrams.containsKey(trigram)) trigrams.put(trigram, trigrams.get(trigram)+1);
                    else trigrams.put(trigram, 1);
                }
            }
        }
        else noLemmas++;
    }

    private void calculateProbabilities(){
        Console.log("Calculating PMI");
        bigramsPMI = new ConcurrentHashMap<>();
        bigramsProbabilities = new ConcurrentHashMap<>();
//        trigramsPMI = new ConcurrentHashMap<>();
        if(RUN_IN_PARALLEL) {
            bigrams.entrySet().parallelStream().forEach(this::calculateProbabilities);
//            trigrams.entrySet().parallelStream().forEach(this::calculateProbabilities);
        }
        else {
            bigrams.entrySet().forEach(this::calculateProbabilities);
//            trigrams.entrySet().forEach(this::calculateProbabilities);
        }
        Console.tick();
//        LinkedList<Pair<String, Double>> bigramsPMISorted = new LinkedList<>();
//        bigramsPMI.entrySet().stream().forEach(e->bigramsPMISorted.add(new Pair<>(e.getKey(),e.getValue())));
//        bigramsPMISorted.sort(new Comparator<Pair<String, Double>>() {
//            @Override
//            public int compare(Pair<String, Double> o1, Pair<String, Double> o2) {
//                return -Double.compare(o1.getRight(),o2.getRight());
//            }
//        });
//        bigramsPMI.entrySet().removeIf(entry -> entry.getValue() <= 10);
//        trigramsPMI.entrySet().removeIf(entry -> entry.getValue() <= 20);
        bigramsProbabilities.entrySet().removeIf(e -> e.getValue() <= 0.001);
        LinkedList<Pair<String, Double>> bigramsPSorted = new LinkedList<>();
        bigramsProbabilities.entrySet().stream().forEach(e->bigramsPSorted.add(new Pair<>(e.getKey(),e.getValue())));
        bigramsPSorted.sort(new Comparator<Pair<String, Double>>() {
            @Override
            public int compare(Pair<String, Double> o1, Pair<String, Double> o2) {
                return -Double.compare(o1.getRight(),o2.getRight());
            }
        });
        System.out.println("test");
    }

//    private void calculatePMI(Map.Entry<String,Integer> nGram){
//        String[] terms = nGram.getKey().split("-");
//        if(terms.length == 2){
//            double pBigram = (double) nGram.getValue() /nUnigrams;
//            double pUnigram1 = (double) unigrams.get(terms[0]) /nUnigrams;
//            double pUnigram2 = (double) unigrams.get(terms[1]) /nUnigrams;
//            double pmi = Math.log(pBigram/(pUnigram1*pUnigram2))/Math.log(2);
//            bigramsPMI.put(nGram.getKey(),pmi);
//        }
//        if(terms.length == 3){
//            double pTrigram = (double) nGram.getValue() /nUnigrams;
//            double pUnigram1 = (double) unigrams.get(terms[0]) /nUnigrams;
//            double pUnigram2 = (double) unigrams.get(terms[1]) /nUnigrams;
//            double pUnigram3 = (double) unigrams.get(terms[2]) /nUnigrams;
//            double pmi = Math.log(pTrigram/(pUnigram1*pUnigram2*pUnigram3)) / Math.log(2);
//            trigramsPMI.put(nGram.getKey(),pmi);
//        }
//    }

    private void calculateProbabilities(Map.Entry<String, Integer> nGram){
        String[] terms = nGram.getKey().split("-");
        if(terms.length == 2){
            // bigram
            bigramsProbabilities.put(nGram.getKey(), ((double)(nGram.getValue()+1)/(double)(unigrams.get(terms[0])+unigrams.size())));
        }
    }

}
