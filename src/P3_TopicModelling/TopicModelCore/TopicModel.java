package P3_TopicModelling.TopicModelCore;

import PX_Data.TopicIOWrapper;
import PY_Helper.LogPrint;
import cc.mallet.pipe.CharSequence2TokenSequence;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.TokenSequence2FeatureSequence;
import cc.mallet.pipe.iterator.CsvIterator;
import cc.mallet.topics.ParallelTopicModel;
import cc.mallet.topics.TopicAssignment;
import cc.mallet.topics.TopicModelDiagnostics;
import cc.mallet.topics.TopicInferencer;
import cc.mallet.types.*;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Class running a Topic Model using MALLET.
 *
 * @author S. Padilla, T. Methven, P. Le Bras, A. Vidal
 * @version 4
 */
public class TopicModel implements Serializable {

    /** Number of threads to use for parallel topic modelling. */
    public static final int PROC = 8;

    /** Serialisation ID. */
    private static final long serialVersionUID = -8983749417082119056L;

    /** Number of topics to model. */
    public int TOPICS = 300;
    /** Random seed for the sampler. */
    public int SEED = 1;
    /** Number of sampling iterations. */
    public int ITERSAMPLING = 500;
    /** Number of maximisation iterations. */
    public int ITERMAXIMISE = 0;
    /** Sum of alpha dirichlet priors (topics over documents).
     * High alpha = document mix of most topics.
     * Low alpha = document mixture of few/one topics. */
    public double ALPHASUM = 1.0;
    /** Beta dirichlet prior (words over topics).
     * High beta = topic mix of most words.
     * Low beta = topic mix of few words. */
    public double BETA = 0.01;
    /** Boolean flag for making alpha values symmetric.
     * If asymmetric, some topics more likely to appear across documents. */
    public boolean SYMMETRICALPHA = false;
    /** Number of iterations between hyperparameters optimisations. */
    public int OPTIMINTERVAL = 50;

    /** List of input documents, with ID and lemmas. */
    public List<InputDocument> inputDocuments;
    /** Map of document index (in the model) to document ID. */
    public List<String> numIDtoStringID = new ArrayList<>();
    /** Map of document ID to document index (in the model). */
    public HashMap<String, Integer> stringIDtoNumID = new HashMap<>();
    /** Instances of documents for the model. */
    public InstanceList instances;
    /** Model object. */
    public ParallelTopicModel model;
    /** List of modelled documents, with ID, used lemmas and topic assignment data. */
    public List<ModelledDocument> modelledDocuments = new ArrayList<>();
    /** List of modelled topic, with ID and lemmas sorted by weight. */
    public List<ModelledTopic> modelledTopics = new ArrayList<>();
    /** Total number of unique words in the vocabulary. */
    public int vocabularySize;

    /** Un-serialised record of log-likelihood throughout the modelling process. */
    public transient LogLikelihoodRecord logLikelihoodRecord;
    /** Un-serialised record of the topics evolution throughout the modelling process. */
    public transient TopicRecord topicRecord;

    /**
     * Empty constructor for inference, attributes loaded from serialised model.
     */
    public TopicModel(){}

    /**
     * Constructor taking a list of documents to model.
     * @param docs List of documents to model.
     */
    public TopicModel(List<InputDocument> docs) {
        this.inputDocuments = docs;
    }

    /**
     * Method modelling topics from the input documents.
     * @param outputDir Name of directory for writing MALLET files (corpus and diagnostics).
     */
    public void Model(String outputDir){

        // Filenames for MALLET's corpus and diagnostic files
        String corpusFile = outputDir + File.separator + "malletCorpus.txt";
        String diagnosticFile = outputDir + File.separator + "malletDiagnostics.xml";

        // ==================================================
        // Writing the corpus in MALLET's format (tsv: document ID, language, lemmas)
        // ==================================================
        try {
            File file = new File(corpusFile);
            file.getParentFile().mkdirs();
            FileWriter writer = new FileWriter(file);
            int count = 0;
            for (InputDocument entry : inputDocuments) {
                if(entry != null) {
                    writer.write(entry.ID + "\ten\t" + entry.inputLemmas + "\r\n");
                    numIDtoStringID.add(entry.ID);
                    stringIDtoNumID.put(entry.ID, count);
                    count++;
                }
            }
            writer.close();
        } catch (IOException e) {
            LogPrint.printNoteError("Could not write file corpus file");
            LogPrint.printNoteError(e.getMessage());
            System.exit(1);
        }

        // ==================================================
        // Importing the corpus from file
        // ==================================================
        ArrayList<Pipe> pipeList = new ArrayList<>();

        // Pipes: lowercase, tokenize, remove stopwords, map to features
        pipeList.add(new CharSequence2TokenSequence(Pattern.compile("\\p{L}[\\p{L}\\p{P}]+\\p{L}")));
        pipeList.add(new TokenSequence2FeatureSequence());

        instances = new InstanceList(new SerialPipes(pipeList));

        Reader fileReader = null;
        try{
            fileReader = new InputStreamReader(new FileInputStream(corpusFile), "UTF-8");
        } catch(Exception e){
            LogPrint.printNoteError("Error: Could not open "+corpusFile);
            LogPrint.printNoteError(e.getMessage());
            System.exit(1);
        }

        instances.addThruPipe(new CsvIterator(fileReader, Pattern.compile("^(\\S*)[\\s,]*(\\S*)[\\s,]*(.*)$"),
                3, 2, 1)); // data, label, name fields

        // ==================================================
        // Preparing up the model
        // ==================================================

        // Create a model with n topics, alpha sum and beta
        //   Note that alpha is passed as the sum over topics, while
        //   beta is for a single dimension of the Dirichlet prior.
        model = new ParallelTopicModel(TOPICS, ALPHASUM, BETA);
        // set the random seed
        model.setRandomSeed(SEED);
        // set the alpha values symmetrical optimisation
        model.setSymmetricAlpha(SYMMETRICALPHA);
        // add the documents
        model.addInstances(instances);
        // set the number of iterations
        model.setNumIterations(ITERSAMPLING);
        // set the number of threads
        model.setNumThreads(PROC);

        // setting the interval for displaying words when logging,
        //   first the interval of print, eg. every 100 iterations
        //   second the number of words to print per topics
        int topicPrintInterval = 50;
        int wordPerTopicPrint = 10;
        model.setTopicDisplay(topicPrintInterval, wordPerTopicPrint);
        // setting the interval for optimising alpha and beta parameters
        model.setOptimizeInterval(OPTIMINTERVAL);

        // adding custom log handler to capture model data
        MalletLogHandler logHandler = new MalletLogHandler();
        ParallelTopicModel.logger.addHandler(logHandler);

        // ==================================================
        // Running the model
        // ==================================================

        try {
            // Sample the model, i.e. find more likely solution from sample space
            model.estimate();
            if(ITERMAXIMISE > 0){
                // Run a few extra maximisation steps, i.e. find the local maximum of the solution sampled above
                model.maximize(ITERMAXIMISE);
            }
        } catch (Exception e) {
            LogPrint.printNoteError("Error: Mallet could not estimate the model!");
            LogPrint.printNoteError(e.getMessage());
            System.exit(1);
        }

        // ==================================================
        // Recording Mallet logs
        // ==================================================

        // removing log handler
        ParallelTopicModel.logger.removeHandler(logHandler);
        // recording history log-likelihood
        logLikelihoodRecord = new LogLikelihoodRecord(logHandler.getLLRecords(), model.totalTokens, model.modelLogLikelihood(), ITERSAMPLING);
        // recording history of topics
        topicRecord = new TopicRecord(logHandler.getTopicRecords(), TOPICS, ITERSAMPLING, topicPrintInterval);

        // ==================================================
        // Writing Mallet's diagnostics on file
        // ==================================================

        // Get model diagnostics from mallet
        TopicModelDiagnostics diagnostics = new TopicModelDiagnostics(model, 10);
        //Write mallet diagnostics to file
        try {
            File file = new File(diagnosticFile);
            file.getParentFile().mkdirs();
            FileWriter writer = new FileWriter(file);
            writer.write(diagnostics.toXML());
            writer.close();
        } catch (IOException e) {
            LogPrint.printNoteError("Error: Could not write file "+diagnosticFile);
            LogPrint.printNoteError(e.getMessage());
            System.exit(1);
        }

        // ==================================================
        // Saving the modelled data:
        //     - topic assignment in documents, with topic count and distribution
        //     - word assignment in topics, with word weights
        // ==================================================

        // The model's vocabulary
        Alphabet vocabulary = model.alphabet;
        vocabularySize = vocabulary.size();

        // Initialise the modelled documents data
        modelledDocuments = new ArrayList<>(model.data.size());
        // For each documents
        for (int doc = 0; doc < model.data.size(); doc++) {
            // Get the model instance of the document
            TopicAssignment modelDoc = model.data.get(doc);
            // Instantiate the modelled document data
            ModelledDocument modelledDoc = new ModelledDocument(modelDoc.instance.getName().toString(), doc);
            // Get the document's feature (i.e. index of words in vocabulary)
            // Note that some words from the input lemmas will be ignored by MALLET (e.g. considered stop-words)
            // Hence the document's input lemmas and features might differ
            int[] modelDocFeatures = ((FeatureSequence) modelDoc.instance.getData()).getFeatures();
            String[] modelDocLemmas = new String[modelDocFeatures.length];
            for(int i = 0; i < modelDocFeatures.length; i++){
                modelDocLemmas[i] = vocabulary.lookupObject(modelDocFeatures[i]).toString();
            }
            // Get the document's topic modelledDocument, for each word
            int[] modelDocTopicSequence = modelDoc.topicSequence.getFeatures();
            // Get the document's topic count (number of words labelled with each topic)
            int[] modelDocTopicCount = new int[model.numTopics];
            for (int i : modelDocTopicSequence) {
                modelDocTopicCount[i]++;
            }
            // Get the document's topic distribution
            double[] modelDocTopicDistribution = model.getTopicProbabilities(doc);
            // Set the lemmas in the modelled document
            modelledDoc.setLemmas(modelDocLemmas, modelDocFeatures);
            // Set this topic assignment data in the modelled document
            modelledDoc.setTopicAssignment(modelDocTopicSequence, modelDocTopicCount, modelDocTopicDistribution);
            // Add to the list of modelled documents
            modelledDocuments.add(modelledDoc);
        }

        // Initialise the modelled topics data
        modelledTopics = new ArrayList<>(model.numTopics);
        // Get the list of sorted sets of word ID/count pairs, one set per topic
        ArrayList<TreeSet<IDSorter>> topicSortedWords = model.getSortedWords();
        // Get the list of sorted sets of document ID/weight pairs, one set per topic
        ArrayList<TreeSet<IDSorter>> topicSortedDocs = model.getTopicDocuments(0);
        // For each topic
        for (int topic = 0; topic < model.numTopics; topic++) {
            // Instantiate the modelled document data
            ModelledTopic modelledTopic = new ModelledTopic(Integer.toString(topic), topic);

            // Instantiate a counter and lists of labels, words ids and weights
            int countLabels = 0;
            String[] labels = new String[topicSortedWords.get(topic).size()];
            int[] labelsID = new int[topicSortedWords.get(topic).size()];
            double[] labelWeights = new double[topicSortedWords.get(topic).size()];
            // Get the iterator to go through the sorted words
            Iterator<IDSorter> labelIterator = topicSortedWords.get(topic).iterator();
            // Iterate through the sorted word list and populate the lists
            // use the commented line below  if you want to limit the number of words saved
            // while (iterator.hasNext() && (countLabels < model.wordsPerTopic)){
            while (labelIterator.hasNext()){
                IDSorter idCountPair = labelIterator.next();
                labels[countLabels] = vocabulary.lookupObject(idCountPair.getID()).toString();
                labelsID[countLabels] = idCountPair.getID();
                labelWeights[countLabels] = idCountPair.getWeight();
                countLabels++;
            }
            // Set the word assignment data in the modelled topic
            modelledTopic.setLabelsAssignment(labels, labelsID, labelWeights);

            // Instantiate a counter and lists of docs ids and weights
            int countDocs = 0;
            String[] docs = new String[topicSortedDocs.get(topic).size()];
            double[] docWeights = new double[topicSortedDocs.get(topic).size()];
            // Get the iterator to go through the sorted docs
            Iterator<IDSorter> docIterator = topicSortedDocs.get(topic).iterator();
            while (docIterator.hasNext()){
                IDSorter doc = docIterator.next();
                if(doc.getWeight() <= 0.0) break;
                docs[countDocs] = numIDtoStringID.get(doc.getID());
                docWeights[countDocs] = doc.getWeight();
                countDocs++;
            }
            // Set the document data in the modelled topic
            modelledTopic.setDocumentsAssignment(Arrays.copyOfRange(docs, 0, countDocs), Arrays.copyOfRange(docWeights, 0, countDocs));

            // Add to the list of modelled topics
            modelledTopics.add(modelledTopic);
        }
    }

    /**
     * Method inferencing a new document's topic distributions from the model.
     * @param lemmatisedText Lemmatised document to infer.
     * @param iterations Number of iterations.
     * @return The new document's topic probabilities.
     */
    public double[] InferTopics(String lemmatisedText, int iterations){

        double[] newProbabilities;

        try {
            //Create new instance from the pipeline we already have to ensure the same things happens to it, then load our extra document
            InstanceList newInstance = new InstanceList(instances.getPipe());
            newInstance.addThruPipe(new Instance(lemmatisedText, null, "infer", null));

            TopicInferencer inferencer = model.getInferencer();

            //Uses Gibbs sampling to infer a topic distribution from the new instance (document)
            //In the form of: (instance, numInterations, thinning, burnIn)
            inferencer.setRandomSeed(20);
            newProbabilities = inferencer.getSampledDistribution(newInstance.get(0), iterations, 1, 5);    // 100000, 1, 5

        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.err.println(e.getCause());
            return new double[0];
        }

        return newProbabilities;
    }

    /** OLD CODE */
    //
    // /** Launch modelling with defaults */
    // public void Model() {
    //     Model(true, true);
    // }
    //
    // /** Launch modelling with default "output" folder */
    // public void Model(boolean serialiseModel, boolean recreateCorpus) {
    //     Model(serialiseModel, recreateCorpus, "output");
    // }
    //
    // /** Launch modelling process */
    // public void Model(boolean serialiseModel, boolean recreateCorpus, String rotLoc){
    //
    //     if(recreateCorpus) {
    //         //Save all data in mallet style
    //         try {
    //             File file = new File(rotLoc+File.separator+"modelCorpus.txt");
    //             file.getParentFile().mkdirs();
    //             FileWriter writer = new FileWriter(file);
    //
    //             int count = 0;
    //             for (Document entry : documents) {
    //                 if(entry != null) {
    //                     writer.write(entry.ID + "\ten\t" + entry.Lemma + "\r\n");
    //                     numIDtoStringID.add(entry.ID);
    //                     stringIDtoNumID.put(entry.ID, count);
    //                     count++;
    //                 }
    //             }
    //
    //             writer.close();
    //         } catch (IOException e) {
    //             System.out.println("Could not write file: " + e.getMessage());
    //             System.exit(1);
    //         }
    //     }
    //
    //
    //     // Begin by importing documents from text to feature sequences
    //     ArrayList<Pipe> pipeList = new ArrayList<Pipe>();
    //
    //     // Pipes: lowercase, tokenize, remove stopwords, map to features
    //     pipeList.add( new CharSequence2TokenSequence(Pattern.compile("\\p{L}[\\p{L}\\p{P}]+\\p{L}")) );
    //     pipeList.add( new TokenSequence2FeatureSequence() );
    //
    //     instances = new InstanceList (new SerialPipes(pipeList));
    //
    //     Reader fileReader = null;
    //     try{
    //         fileReader = new InputStreamReader(new FileInputStream(new File(rotLoc + File.separator + "modelCorpus.txt")), "UTF-8");
    //     }
    //     catch(Exception e){
    //         System.out.println("Error: opening file for Mallet!");
    //         System.out.println(e.getMessage());
    //         System.exit(1);
    //     }
    //
    //     instances.addThruPipe(new CsvIterator(fileReader, Pattern.compile("^(\\S*)[\\s,]*(\\S*)[\\s,]*(.*)$"),
    //             3, 2, 1)); // data, label, name fields
    //
    //
    //     // Create a model with n topics, alpha_t = 0.01, beta_w = 0.01
    //     //  Note that the first parameter is passed as the sum over topics, while
    //     //  the second is the parameter for a single dimension of the Dirichlet prior.
    //
    //     model = new ParallelTopicModel(TOPICS, 1.0, 0.01);
    //     model.randomSeed = SEED;
    //     model.addInstances(instances);
    //
    //     // Use two parallel samplers, which each look at one half the corpus and combine
    //     //  statistics after every iteration.
    //     model.setNumThreads(PROC);
    //
    //     // Run the model for 50 iterations and stop (this is for testing only,
    //     //  for real applications, use 1000 to 2000 iterations)
    //     model.setNumIterations(ITER);
    //
    //     model.wordsPerTopic = WTOPICS;
    //     // setting the interval for displaying words when logging
    //     model.setTopicDisplay(100, 10);
    //     // adding custom log handler to capture model data
    //     MalletLogHandler logHandler = new MalletLogHandler();
    //     model.logger.addHandler(logHandler);
    //     try {
    //         model.estimate();
    //     } catch (Exception e) {
    //         System.out.println("Error: unable to estimate model!");
    //         System.out.println(e.getMessage());
    //         return;
    //     }
    //     // removing log handler
    //     model.logger.removeHandler(logHandler);
    //     // storing log-likelihood
    //     logLikelihood = new LogLikelihood(logHandler.getLLRecords(), model.totalTokens, model.modelLogLikelihood(), ITER);
    //
    //     // Get all topic distributions for each document
    //     topicDistributions = new ArrayList<TopicData>(model.data.size());
    //
    //     // Initialise the sorters with dummy values
    //     IDSorter[] sortedTopics = new IDSorter[ model.numTopics ];
    //     for (int topic = 0; topic < model.numTopics; topic++) {
    //         sortedTopics[topic] = new IDSorter(topic, topic);
    //     }
    //
    //     int max = model.numTopics;
    //
    //     for (int doc = 0; doc < model.data.size(); doc++) {
    //         LabelSequence topicSequence = (LabelSequence) model.data.get(doc).topicSequence;
    //         int[] currentDocTopics = topicSequence.getFeatures();
    //
    //         TopicData docData = new TopicData(model.numTopics);
    //
    //         docData.id = doc;	// Save id for document
    //
    //         // Save name
    //         if (model.data.get(doc).instance.getName() != null) {
    //             docData.name = model.data.get(doc).instance.getName().toString();
    //         }
    //         else {
    //             docData.name = "no-name";
    //         }
    //
    //         int docLen = currentDocTopics.length;
    //         docData.topics = docLen;
    //
    //         int[] topicCounts = new int[ model.numTopics ];
    //
    //         // Count up the tokens
    //         for (int token=0; token < docLen; token++) {
    //             topicCounts[ currentDocTopics[token] ]++;
    //         }
    //
    //         // And normalise
    //         for (int topic = 0; topic < TOPICS; topic++) {
    //             sortedTopics[topic].set(topic, (model.alpha[topic] + topicCounts[topic]) / (docLen + model.alphaSum) );
    //             //Previous Stefano Optimization - do not used in EPSRC code to allow better transparency of algorithm
    //             //sortedTopics[topic].set(topic, ((double)topicCounts[topic]) / ((double)docLen) );
    //         }
    //
    //         // Note: Do not sort topic distributions
    //         //Arrays.sort(sortedTopics);
    //
    //         double threshold = 0.0;
    //         int maxLabels = docData.topicLabels.length;
    //         if( maxLabels > 100) maxLabels = 100;       // Note: i'm only storing the top 100 labels and their weights
    //         for (int i = 0; i < maxLabels; i++) {
    //             if (sortedTopics[i].getWeight() < threshold) { break; }
    //             docData.topicLabels[i] = sortedTopics[i].getID();
    //             //docData.topicDistributions[i] = sortedTopics[i].getWeight();  // Old way with mistake - see below..
    //         }
    //
    //         docData.topicDistributions = model.getTopicProbabilities(doc); // PS. This is a newer method to get the distributions
    //
    //         topicDistributions.add(docData);
    //
    //
    //     }
    //
    //     //write topics distribution to file using mallet output.
    //     /*File distributionsFile = new File("topicDistributions_"  + numTopics + ".csv");
    //     try {
    //         model.printDocumentTopics(distributionsFile);
    //     } catch (IOException e) {
    //         e.printStackTrace();
    //         System.exit(1);
    //     }*/
    //
    //
    //     // Saving word weights
    //     List<WordData> topicWords = new ArrayList<WordData>();
    //     for (int topic = 0; topic < model.numTopics; topic++) {
    //         for (int type = 0; type < model.numTypes; type++) {
    //
    //             int[] topicCounts = model.typeTopicCounts[type];
    //
    //             double weight = model.beta;
    //
    //             int index = 0;
    //             while (index < topicCounts.length && topicCounts[index] > 0) {
    //
    //                 int currentTopic = topicCounts[index] & model.topicMask;
    //
    //                 if (currentTopic == topic) {
    //                     weight += topicCounts[index] >> model.topicBits;
    //                     break;
    //                 }
    //                 index++;
    //             }
    //
    //             weight = weight - model.alpha[topic];
    //
    //             if(weight > 0.0){
    //                 WordData word = new WordData(type, topic, model.alphabet.lookupObject(type).toString(), weight);
    //                 topicWords.add(word);
    //             }
    //         }
    //     }
    //
    //     //  Write words to file using mallet default output
    //     /*File wordsFile = new File("wordWeights_"  + numTopics + ".csv");
    //     try {
    //         model.printTopicWordWeights(wordsFile);
    //     } catch (IOException e) {
    //         e.printStackTrace();
    //         System.exit(1);
    //     }*/
    //
    //
    //     //  Saving topics
    //     topics = new ArrayList<Topic>(model.numTopics);
    //
    //     // Get an array of sorted sets of word ID/count pairs
    //     ArrayList<TreeSet<IDSorter>> topicSortedWords = model.getSortedWords();
    //
    //     // The data alphabet maps word IDs to strings
    //     Alphabet dataAlphabet = instances.getDataAlphabet();
    //
    //     for (int topic = 0; topic < model.numTopics; topic++) {
    //         Iterator<IDSorter> iterator = topicSortedWords.get(topic).iterator();
    //         int countLabels = 0;
    //         String[] labels = new String[topicSortedWords.get(topic).size()];
    //         int[] labelsID = new int[topicSortedWords.get(topic).size()];
    //         double[] weights = new double[topicSortedWords.get(topic).size()];
    //         while (iterator.hasNext() && (countLabels < model.wordsPerTopic)){
    //             IDSorter idCountPair = iterator.next();
    //             labels[countLabels] = dataAlphabet.lookupObject(idCountPair.getID()).toString();
    //             labelsID[countLabels] = idCountPair.getID();
    //             weights[countLabels] = idCountPair.getWeight();
    //             countLabels++;
    //         }
    //         Topic topicTemp = new Topic(topic, topicSortedWords.get(topic).size(), labels, labelsID, weights);
    //         topics.add(topicTemp);
    //     }
    //
    //     // Note PLB: the following serialises the model by splitting its attributes, but is actually not used (yet)
    //     // the current approach used in the topic model module is to serialise the whole topic model instance
    //     if(serialiseModel) {
    //         // Serialising all information to allow inferring of new documents
    //         try {
    //             LogPrint.printNewStep("Serialising model "+name, 0);
    //
    //             FileOutputStream fileOut = new FileOutputStream(rotLoc + name + "_pipeList_" + TOPICS + ".ser");
    //             ObjectOutputStream out = new ObjectOutputStream(fileOut);
    //             out.writeObject(pipeList);
    //             out.close();
    //             fileOut.close();
    //
    //             fileOut = new FileOutputStream(rotLoc + name + "_instances_" + TOPICS + ".ser");
    //             out = new ObjectOutputStream(fileOut);
    //             out.writeObject(instances);
    //             out.close();
    //             fileOut.close();
    //
    //             fileOut = new FileOutputStream(rotLoc + name + "_model_" + TOPICS + ".ser");
    //             out = new ObjectOutputStream(fileOut);
    //             out.writeObject(model);
    //             out.close();
    //             fileOut.close();
    //
    //             fileOut = new FileOutputStream(rotLoc + name + "_topicDistributions_" + TOPICS + ".ser");
    //             out = new ObjectOutputStream(fileOut);
    //             out.writeObject(topicDistributions);
    //             out.close();
    //             fileOut.close();
    //
    //             fileOut = new FileOutputStream(rotLoc + name + "_topicWords_" + TOPICS + ".ser");
    //             out = new ObjectOutputStream(fileOut);
    //             out.writeObject(topicWords);
    //             out.close();
    //             fileOut.close();
    //
    //             fileOut = new FileOutputStream(rotLoc + name + "_topics_" + TOPICS + ".ser");
    //             out = new ObjectOutputStream(fileOut);
    //             out.writeObject(topics);
    //             out.close();
    //             fileOut.close();
    //
    //             LogPrint.printCompleteStep();
    //         } catch (IOException e) {
    //             LogPrint.printNoteError("Error: Could not serialize data!");
    //             LogPrint.printNoteError(e.getMessage());
    //             System.exit(1);
    //         }
    //     }
    //
    //
    //     // Output information in CSV format
    //     /*FileWriter writer;
    //     FileWriter writer2;
    //     try {
    //         writer = new FileWriter(rotLoc + name + "_topicwords_" + numTopics + ".csv");
    //         writer2 = new FileWriter(rotLoc + name + "_topics_" + numTopics + ".csv");
    //         for (Topics topic: topics){
    //             writer2.append('"' + Integer.toString(topic.id) + '"' + ",");
    //             double sumTopic = 0.0;
    //             String topicText = "";
    //             for(int i = 0; i < topic.topicLabels.length; i++){
    //                 if(topic.topicWeights[i] > 0.0){
    //                     writer.append('"' + Integer.toString(topic.id) + '"' + ',');
    //                     writer.append('"' + topic.topicLabels[i].trim() + '"' + ',' );
    //                     writer.append('"' + " " + '"' + ',' );
    //                     writer.append('"' + Double.toString(topic.topicWeights[i]) + '"' + "\r\n" );
    //                     sumTopic += topic.topicWeights[i];
    //                     if( i > 0){
    //                         topicText += " " + topic.topicLabels[i];
    //                     }
    //                     else{
    //                         topicText += topic.topicLabels[i];
    //                     }
    //                 }
    //             }
    //             writer2.append('"' + Double.toString(sumTopic) + '"' + ',');
    //             writer2.append('"' + topicText + '"' + "\r\n");
    //         }
    //         writer.flush();
    //         writer.close();
    //         writer2.flush();
    //         writer2.close();
    //     } catch (IOException e) {
    //         System.out.println("Error: Could not save output data!");
    //         System.out.println(e.getMessage());
    //         System.exit(1);
    //     }*/
    //
    // }
    //
    // public double[] InferTopics(String UID, String text, StanfordLemmatizer slem, boolean serverProcess){
    //
    //     // Lemmatise text
    //     HashMap<String, String> dataIn = new HashMap<String, String>();  // Store the grant ID and descriptions
    //
    //     dataIn.put("infer", text);
    //
    //     String rawString = dataIn.get("infer");
    //     rawString = rawString.toLowerCase() ;		// Everything to lower case
    //     rawString = rawString.replace('.', ' ') ; // Remove full stops
    //     rawString = rawString.replace('/', ' ') ; // Remove slashes stops
    //     rawString = rawString.replace('\\', ' ') ; // Remove slashes stops
    //     rawString = rawString.replace(',', ' ') ; // Remove full stops
    //     rawString = rawString.trim().replaceAll(" +", " ");	// Remove extra spaces
    //     List<String> inputLem = StanfordLemmatizer.removeStopWords( slem.lemmatise(rawString) ) ;
    //
    //     text = "";
    //     for (String words: inputLem){
    //         text += words + " ";
    //     }
    //
    //     String lemmatizedFileName = "infer_raw/" + UID + "_infered.txt";
    //
    //     //Save lemmatised text for topic model inferer.
    //     try{
    //         //System.out.println("Serialising new lemmatised text");
    //         Writer writerL = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(lemmatizedFileName), "UTF8"));
    //         writerL.append("infer\ten\t" + text + "\r\n");
    //         writerL.close();
    //
    //     }
    //     catch(IOException e){
    //         if(serverProcess){
    //             String errorFile = "infer_raw/" + UID + "_status.txt";
    //             try {                           // Save file if error
    //                 Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(errorFile), "UTF8"));
    //                 writer.append("Error in server - could not save files to server");
    //                 writer.close();
    //             } catch (IOException e1) {
    //                 System.exit(1);
    //             }
    //         }
    //         else {
    //             System.out.println("Error: save text for inferer!");
    //             System.out.println(e.getMessage());
    //             return new double[0];
    //         }
    //     }
    //
    //     double[] newProbabilities;
    //
    //     try
    //     {
    //         //Create new instance from the pipeline we already have to ensure the same things happens to it, then load our extra document
    //         InstanceList newInstance = new InstanceList(instances.getPipe());
    //         newInstance.addThruPipe(new Instance(text, null, "infer", null));
    //
    //         TopicInferencer inferencer = model.getInferencer();
    //
    //         //Uses Gibbs sampling to infer a topic distribution from the new instance (document)
    //         //In the form of: (instance, numInterations, thinning, burnIn)
    //         inferencer.setRandomSeed(20);
    //         newProbabilities = inferencer.getSampledDistribution(newInstance.get(0), 100000, 1, 5);    // 100000, 1, 5
    //
    //     }
    //     catch (Exception e)
    //     {
    //         System.err.println(e.getMessage());
    //         System.err.println(e.getCause());
    //         return new double[0];
    //     }
    //
    //
    //     return newProbabilities;
    // }
}
