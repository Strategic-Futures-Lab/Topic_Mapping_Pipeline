package model.ldacore;

import IO.Console;
import P3_TopicModelling.TopicModelCore.ModelledDocument;
import cc.mallet.topics.*;
import cc.mallet.types.*;
import data.SparseVector;
import cc.mallet.pipe.CharSequence2TokenSequence;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.TokenSequence2FeatureSequence;
import cc.mallet.pipe.iterator.CsvIterator;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class LDAModel implements Serializable {

    /**
     * Exception class for errors during the modelling process
     */
    public class LDAModelException extends Exception{
        public LDAModelException(String msg){ super(msg); }
    }

    private static final int PROC = 8;

    @Serial
    private static final long serialVersionUID = -8983749417082119056L;

    // MALLET options
    /** Number of topics to model */
    public int nTopics = 50;
    /** Random seed for the sampler */
    public int seed = 1;
    /** Number of sampling iterations */
    public int samplingIterations = 500;
    /** Number of maximisation iterations */
    public int maximisationIterations = 50;
    /** Sum of alpha dirichlet priors (topics over documents):
     * High alpha = document mix of more topics;
     * Low alpha = document mixture of few/one topics */
    public double alphaSum = 1.0;
    /** Beta dirichlet prior (words over topics):
     * High beta = topic mix of more words;
     * Low beta = topic mix of few words */
    public double beta = 0.01;
    /** Flag for making alpha values symmetric:
     * if asymmetric, some topics more likely to appear across documents */
    public boolean symmetricAlpha = false;
    /** Number of iterations between hyperparameters optimisations */
    public int optimisationInterval = 50;

    /** Flag for calculating the word distribution differences between documents and topics */
    public boolean getWordDistances = false;

    /** List of documents */
    public HashMap<String, LDADocument> documents;
    /** Map of document index (in the model) to document ID */
    private List<String> numIDtoStringID = new ArrayList<>();
//    /** Map of document ID to document index (in the model) */
//    public HashMap<String, Integer> stringIDtoNumID = new HashMap<>();
    /** Instances of documents for the model */
    private InstanceList instances;
    /** Model object */
    private ParallelTopicModel model;
    /** List of modelled topic, with ID and lemmas sorted by weight */
    public List<LDATopic> topics;

    /** Un-serialised record of log-likelihood throughout the modelling process */
    public transient LikelihoodLogs logLikelihoodLogs;
    /** Un-serialised record of the topics evolution throughout the modelling process */
    public transient TopicLogs topicLogs;

    /**
     * Empty constructor for inference, attributes loaded from serialised model
     */
    public LDAModel(){}

    /**
     * Constructor taking a map of documents to model
     * @param docs Map of documents to model topics from
     */
    public LDAModel(HashMap<String, LDADocument> docs){
        documents = docs;
    }

    /**
     * Constructor taking a list of documents to model
     * @param docs List of documents to model topics from
     */
    public LDAModel(List<LDADocument> docs){
        setDocuments(docs);
    }

    /**
     * Method loading a list of documents
     * @param docs List of documents to load
     */
    public void setDocuments(List<LDADocument> docs){
        documents = new HashMap<>();
        for(LDADocument doc: docs){
            documents.put(doc.getId(), doc);
        }
    }

    /**
     * Core method to run the LDA model
     * @param outputDirectory directory where temporary files are written (MALLET data and logs)
     * @throws LDAModelException if the model fails (writing files or MALLET error)
     */
    public void model(String outputDirectory) throws LDAModelException{
        // Filenames for MALLET's corpus and diagnostic files
        String corpusFile = outputDirectory + File.separator + "malletCorpus.txt";
        String diagnosticsFile = outputDirectory + File.separator + "malletDiagnostics.xml";

        // Step 1: write the corpus in MALLET's format (tsv: doc id, lang, lemmas
        try{
            File file = new File(corpusFile);
            file.getParentFile().mkdirs();
            FileWriter writer = new FileWriter(file);
            int count = 0;
            for(Map.Entry<String, LDADocument> entry: documents.entrySet()){
                LDADocument doc = entry.getValue();
                writer.write(doc.id() + "\ten\t" + doc.text() +"\r\n");
                numIDtoStringID.add(doc.id());
//                stringIDtoNumID.put(doc.id(), count);
                count++;
            }
        } catch (IOException e){
            throw new LDAModelException("Could not write MALLET corpus file ("+corpusFile+")");
        }

        // Step 2: import documents into MALLET's pipeline
        ArrayList<Pipe> pipeList = new ArrayList<>();
        // pipes: lowercase, tokenize, remove stopwords, map to features
        pipeList.add(new CharSequence2TokenSequence(Pattern.compile("\\p{L}[\\p{L}\\p{P}]+\\p{L}")));
        pipeList.add(new TokenSequence2FeatureSequence());
        instances = new InstanceList(new SerialPipes(pipeList));
        Reader fileReader = null;
        try{
            fileReader = new InputStreamReader(new FileInputStream(corpusFile), "UTF-8");
        } catch (Exception e){
            throw new LDAModelException("Could not open corpus file ("+corpusFile+")");
        }
        instances.addThruPipe(new CsvIterator(fileReader, Pattern.compile("^(\\S*)[\\s,]*(\\S*)[\\s,]*(.*)$"), 3, 2, 1));

        // Step 3: prepare the model
        model = new ParallelTopicModel(nTopics, alphaSum, beta); // create model with n topics, alpha sum and beta
        model.setRandomSeed(seed); // set seed
        model.setSymmetricAlpha(symmetricAlpha); // set symmetrical optimisation of alphas
        model.addInstances(instances); // add documents
        model.setNumIterations(samplingIterations); // set n iterations
        model.setNumThreads(PROC); // set n threads
        int topicPrintInterval = 50;
        int wordsPerTopicPrint = 10;
        model.setTopicDisplay(topicPrintInterval, wordsPerTopicPrint); // set topic logs, print interval and n words
        model.setOptimizeInterval(optimisationInterval); // set interval before optimising priors

        // Step 4: run the model
        // add custom log handler
        MalletLogHandler logHandler = new MalletLogHandler();
        ParallelTopicModel.logger.addHandler(logHandler);
        // model
        try {
            model.estimate();
            if(maximisationIterations > 0) model.maximize(maximisationIterations);
        } catch (Exception e){
            throw new LDAModelException("MALLET could not estimate the model");
        }
        // record logs
        ParallelTopicModel.logger.removeHandler(logHandler);
        logLikelihoodLogs = new LikelihoodLogs(logHandler.getLLRecords(), model.totalTokens, model.modelLogLikelihood(), samplingIterations);
        topicLogs = new TopicLogs(logHandler.getTopicRecords(), nTopics, samplingIterations, topicPrintInterval);
        // writing MALLET diagnostics
        TopicModelDiagnostics diagnostics = new TopicModelDiagnostics(model, wordsPerTopicPrint);
        try{
            File file = new File(diagnosticsFile);
            file.getParentFile().mkdirs();
            FileWriter writer = new FileWriter(file);
            writer.write(diagnostics.toXML());
            writer.close();
        } catch (IOException e){
            Console.warning("Could not write MALLET diagnostic file ("+diagnosticsFile+")");
        }

        // Step 5: save modelled data

        // Model vocabulary
        Alphabet vocabulary = model.alphabet;

        // For each doc
        for(int idx = 0; idx < model.data.size(); idx++){
            // get model instance of the document
            TopicAssignment modelDoc = model.data.get(idx);
            // retrieve original document
            LDADocument doc = documents.get(modelDoc.instance.getName().toString());
            // extract document features (word index in vocabulary), some words might be gone (MALLET stop-words)
            int[] docFeatures = ((FeatureSequence) modelDoc.instance.getData()).getFeatures();
            // find words for each features
            String[] docLabels = Arrays.stream(docFeatures).mapToObj(i -> vocabulary.lookupObject(i).toString()).toArray(String[]::new);
            // get topic assignment sequence
            int[] topicSequence = modelDoc.topicSequence.getFeatures();
            // get assignment count per topic
            int[] topicCount = new int[model.numTopics];
            for(int t: topicSequence) topicCount[t]++;
            // get topic distribution in document
            double[] topicDistrib = model.getTopicProbabilities(idx);
            // update original document
            doc.setIndex(idx);
            doc.setWords(docLabels, docFeatures);
            doc.setTopicAssignment(topicSequence, topicCount, topicDistrib);
        }

        // initialise topics
        topics = new ArrayList<>(model.numTopics);
        // get the list of sorted sets of word ID/count pairs, one set per topic
        ArrayList<TreeSet<IDSorter>> topicSortedWords = model.getSortedWords();
        // get the list of sorted sets of document ID/weight pairs, one set per topic
        ArrayList<TreeSet<IDSorter>> topicSortedDocs = model.getTopicDocuments(0);
        // For each topic
        for(int idx = 0; idx < model.numTopics; idx++){
            LDATopic topic = new LDATopic(idx);

            // instantiate lists of labels, ids and weights
            int labelNum = topicSortedWords.get(idx).size();
            String[] labels = new String[labelNum];
            int[] labelIds = new int[labelNum];
            double[] labelWeights = new double[labelNum];
            // setup iteration
            // change labelNum and use labelCount to limit the number of labels saved
            int labelCount = 0;
            Iterator<IDSorter> labelIterator = topicSortedWords.get(idx).iterator();
            while(labelIterator.hasNext()){
                IDSorter idCountPair = labelIterator.next();
                labels[labelCount] = vocabulary.lookupObject(idCountPair.getID()).toString();
                labelIds[labelCount] = idCountPair.getID();
                labelWeights[labelCount] = idCountPair.getWeight();
                labelCount++;
            }

            // instantiate lists of docs ids and weights
            int docNum = topicSortedDocs.get(idx).size();
            String[] docIds = new String[docNum];
            double[] docWeights = new double[docNum];
            // setup iteration
            int docCount = 0;
            Iterator<IDSorter> docIterator = topicSortedDocs.get(idx).iterator();
            while(docIterator.hasNext()){
                IDSorter doc = docIterator.next();
                if(doc.getWeight() <= 0.0) break;
                docIds[docCount] = numIDtoStringID.get(doc.getID());
                docWeights[docCount] = doc.getWeight();
                docCount++;
            }

            // update topic and add to list
            topic.setWordAssignments(labels, labelIds, labelWeights);
            topic.setDocumentAssignments(docIds, docWeights);
            topics.add(topic);
        }

        if(getWordDistances){
            // Getting the topics-label distributions as sparse vectors
            List<SparseVector> topicVectors = topics.stream()
                    .map(t->t.getWordDistribution(vocabulary.size()))
                    .collect(Collectors.toList());
            for(LDADocument doc: documents.values()){
                doc.setDistancesFromTopics(topicVectors);
            }
        }
    }

    /**
     * Getter for the list of topics
     * @return The list of modelled topics
     */
    public List<LDATopic> getTopics() { return topics; }

    /**
     * Method inferring the topic distribution for the given text
     * @param text lemmatised text to infer
     * @param iterations number of iterations
     * @return the topic probabilities for the text
     * @throws LDAModelException if the inference fails (MALLET error)
     */
    public double[] inferTopics(String text, int iterations) throws LDAModelException{
        try{
            //Create new instance from the pipeline we already have to ensure the same things happens to it, then load our extra document
            InstanceList newInstance = new InstanceList(instances.getPipe());
            newInstance.addThruPipe(new Instance(text, null, "infer", null));
            TopicInferencer inferencer = model.getInferencer();
            //Uses Gibbs sampling to infer a topic distribution from the new instance (document)
            //In the form of: (instance, numInterations, thinning, burnIn)
            inferencer.setRandomSeed(20);
            return inferencer.getSampledDistribution(newInstance.get(0), iterations, 1, 5);    // 100000, 1, 5
        } catch (Exception e){
            Console.error("Error while inferring document: "+e.getMessage());
            throw new LDAModelException("MALLET could not infer probabilities from new document");
        }
    }
}
