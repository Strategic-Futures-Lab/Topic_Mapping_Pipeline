package P3_TopicModelling.TopicModelCore;

import P2_Lemmatise.Lemmatizer.StanfordLemmatizer;
import PY_Helper.LogPrint;
import cc.mallet.pipe.CharSequence2TokenSequence;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.TokenSequence2FeatureSequence;
import cc.mallet.pipe.iterator.CsvIterator;
import cc.mallet.topics.ParallelTopicModel;
import cc.mallet.topics.TopicInferencer;
import cc.mallet.types.*;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

public class TopicModel implements Serializable {

    public static final int WTOPICS = 20;
    public static final int PROC = 8;

    private static final long serialVersionUID = -8983749417082119056L;

    public int numTopics = 300;
    public int SEED = 1;
    public int ITER = 500;

    public List<Document> documents;
    public List<String> numIDtoStringID = new ArrayList<>();
    public HashMap<String, Integer> stringIDtoNumID = new HashMap<>();
    public List<Topic> topics = new ArrayList<Topic>();        // list of topics
    public ParallelTopicModel model;                        // topic model
    public InstanceList instances;                          // topic instances
    public List<TopicData> topicDistributions = new ArrayList<TopicData>(); // List of distributions

    public static String name = "topicmodel";               // assign a name to differentiate when saving serialised models

    public TopicModel(){}           // Create empty constructor for quick inferance.
    public TopicModel(List<Document> docs) {
        this.documents = docs;
    }

    public void Model() {
        Model(true, true);
    }

    public void Model(boolean serialiseModel, boolean recreateCorpus) { Model(serialiseModel, recreateCorpus, "output"); }

    public void Model(boolean serialiseModel, boolean recreateCorpus, String rotLoc){

        if(recreateCorpus) {
            //Save all data in mallet style
            try {
                File file = new File(rotLoc+File.separator+"modelCorpus.txt");
                file.getParentFile().mkdirs();
                FileWriter writer = new FileWriter(file);

                int count = 0;
                for (Document entry : documents) {
                    if(entry != null) {
                        writer.write(entry.ID + "\ten\t" + entry.Lemma + "\r\n");
                        numIDtoStringID.add(entry.ID);
                        stringIDtoNumID.put(entry.ID, count);
                        count++;
                    }
                }

                writer.close();
            } catch (IOException e) {
                System.out.println("Could not write file: " + e.getMessage());
                System.exit(1);
            }
        }


        // Begin by importing documents from text to feature sequences
        ArrayList<Pipe> pipeList = new ArrayList<Pipe>();

        // Pipes: lowercase, tokenize, remove stopwords, map to features
        pipeList.add( new CharSequence2TokenSequence(Pattern.compile("\\p{L}[\\p{L}\\p{P}]+\\p{L}")) );
        pipeList.add( new TokenSequence2FeatureSequence() );

        instances = new InstanceList (new SerialPipes(pipeList));

        Reader fileReader = null;
        try{
            fileReader = new InputStreamReader(new FileInputStream(new File(rotLoc + File.separator + "modelCorpus.txt")), "UTF-8");
        }
        catch(Exception e){
            System.out.println("Error: opening file for Mallet!");
            System.out.println(e.getMessage());
            System.exit(1);
        }

        instances.addThruPipe(new CsvIterator(fileReader, Pattern.compile("^(\\S*)[\\s,]*(\\S*)[\\s,]*(.*)$"),
                3, 2, 1)); // data, label, name fields


        // Create a model with n topics, alpha_t = 0.01, beta_w = 0.01
        //  Note that the first parameter is passed as the sum over topics, while
        //  the second is the parameter for a single dimension of the Dirichlet prior.

        model = new ParallelTopicModel(numTopics, 1.0, 0.01);
        model.randomSeed = SEED;
        model.addInstances(instances);

        // Use two parallel samplers, which each look at one half the corpus and combine
        //  statistics after every iteration.
        model.setNumThreads(PROC);

        // Run the model for 50 iterations and stop (this is for testing only,
        //  for real applications, use 1000 to 2000 iterations)
        model.setNumIterations(ITER);

        model.wordsPerTopic = WTOPICS;
        try
        {
            model.estimate();
        }
        catch (Exception e)
        {
            System.out.println("Error: unable to estimate model!");
            System.out.println(e.getMessage());
            return;
        }


        // Get all topic distributions for each document
        topicDistributions = new ArrayList<TopicData>(model.data.size());

        // Initialise the sorters with dummy values
        IDSorter[] sortedTopics = new IDSorter[ model.numTopics ];
        for (int topic = 0; topic < model.numTopics; topic++) {
            sortedTopics[topic] = new IDSorter(topic, topic);
        }

        int max = model.numTopics;

        for (int doc = 0; doc < model.data.size(); doc++) {
            LabelSequence topicSequence = (LabelSequence) model.data.get(doc).topicSequence;
            int[] currentDocTopics = topicSequence.getFeatures();

            TopicData docData = new TopicData(model.numTopics);

            docData.id = doc;	// Save id for document

            // Save name
            if (model.data.get(doc).instance.getName() != null) {
                docData.name = model.data.get(doc).instance.getName().toString();
            }
            else {
                docData.name = "no-name";
            }

            int docLen = currentDocTopics.length;
            docData.topics = docLen;

            int[] topicCounts = new int[ model.numTopics ];

            // Count up the tokens
            for (int token=0; token < docLen; token++) {
                topicCounts[ currentDocTopics[token] ]++;
            }

            // And normalise
            for (int topic = 0; topic < numTopics; topic++) {
                sortedTopics[topic].set(topic, (model.alpha[topic] + topicCounts[topic]) / (docLen + model.alphaSum) );
                //Previous Stefano Optimization - do not used in EPSRC code to allow better transparency of algorithm
                //sortedTopics[topic].set(topic, ((double)topicCounts[topic]) / ((double)docLen) );
            }

            // Note: Do not sort topic distributions
            //Arrays.sort(sortedTopics);

            double threshold = 0.0;
            int maxLabels = docData.topicLabels.length;
            if( maxLabels > 100) maxLabels = 100;       // Note: i'm only storing the top 100 labels and their weights
            for (int i = 0; i < maxLabels; i++) {
                if (sortedTopics[i].getWeight() < threshold) { break; }
                docData.topicLabels[i] = sortedTopics[i].getID();
                //docData.topicDistributions[i] = sortedTopics[i].getWeight();  // Old way with mistake - see below..
            }

            docData.topicDistributions = model.getTopicProbabilities(doc); // PS. This is a newer method to get the distributions

            topicDistributions.add(docData);


        }

        //write topics distribution to file using mallet output.
        /*File distributionsFile = new File("topicDistributions_"  + numTopics + ".csv");
        try {
            model.printDocumentTopics(distributionsFile);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }*/


        // Saving word weights
        List<WordData> topicWords = new ArrayList<WordData>();
        for (int topic = 0; topic < model.numTopics; topic++) {
            for (int type = 0; type < model.numTypes; type++) {

                int[] topicCounts = model.typeTopicCounts[type];

                double weight = model.beta;

                int index = 0;
                while (index < topicCounts.length && topicCounts[index] > 0) {

                    int currentTopic = topicCounts[index] & model.topicMask;

                    if (currentTopic == topic) {
                        weight += topicCounts[index] >> model.topicBits;
                        break;
                    }
                    index++;
                }

                weight = weight - model.alpha[topic];

                if(weight > 0.0){
                    WordData word = new WordData(type, topic, model.alphabet.lookupObject(type).toString(), weight);
                    topicWords.add(word);
                }
            }
        }

        //  Write words to file using mallet default output
        /*File wordsFile = new File("wordWeights_"  + numTopics + ".csv");
        try {
            model.printTopicWordWeights(wordsFile);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }*/


        //  Saving topics
        topics = new ArrayList<Topic>(model.numTopics);

        // Get an array of sorted sets of word ID/count pairs
        ArrayList<TreeSet<IDSorter>> topicSortedWords = model.getSortedWords();

        // The data alphabet maps word IDs to strings
        Alphabet dataAlphabet = instances.getDataAlphabet();

        for (int topic = 0; topic < model.numTopics; topic++) {
            Iterator<IDSorter> iterator = topicSortedWords.get(topic).iterator();
            int countLabels = 0;
            String[] labels = new String[topicSortedWords.get(topic).size()];
            int[] labelsID = new int[topicSortedWords.get(topic).size()];
            double[] weights = new double[topicSortedWords.get(topic).size()];
            while (iterator.hasNext() && (countLabels < model.wordsPerTopic)){
                IDSorter idCountPair = iterator.next();
                labels[countLabels] = dataAlphabet.lookupObject(idCountPair.getID()).toString();
                labelsID[countLabels] = idCountPair.getID();
                weights[countLabels] = idCountPair.getWeight();
                countLabels++;
            }
            Topic topicTemp = new Topic(topic, topicSortedWords.get(topic).size(), labels, labelsID, weights);
            topics.add(topicTemp);
        }

        if(serialiseModel)
        {
            // Serialising all information to allow inferring of new documents
            try
            {
                LogPrint.printNewStep("Serialising model "+name, 0);

                FileOutputStream fileOut = new FileOutputStream(rotLoc + name + "_pipeList_" + numTopics + ".ser");
                ObjectOutputStream out = new ObjectOutputStream(fileOut);
                out.writeObject(pipeList);
                out.close();
                fileOut.close();

                fileOut = new FileOutputStream(rotLoc + name + "_instances_" + numTopics + ".ser");
                out = new ObjectOutputStream(fileOut);
                out.writeObject(instances);
                out.close();
                fileOut.close();

                fileOut = new FileOutputStream(rotLoc + name + "_model_" + numTopics + ".ser");
                out = new ObjectOutputStream(fileOut);
                out.writeObject(model);
                out.close();
                fileOut.close();

                fileOut = new FileOutputStream(rotLoc + name + "_topicDistributions_" + numTopics + ".ser");
                out = new ObjectOutputStream(fileOut);
                out.writeObject(topicDistributions);
                out.close();
                fileOut.close();

                fileOut = new FileOutputStream(rotLoc + name + "_topicWords_" + numTopics + ".ser");
                out = new ObjectOutputStream(fileOut);
                out.writeObject(topicWords);
                out.close();
                fileOut.close();

                fileOut = new FileOutputStream(rotLoc + name + "_topics_" + numTopics + ".ser");
                out = new ObjectOutputStream(fileOut);
                out.writeObject(topics);
                out.close();
                fileOut.close();

                LogPrint.printCompleteStep();
            } catch (IOException e) {
                LogPrint.printNoteError("Error: Could not serialize data!");
                LogPrint.printNoteError(e.getMessage());
                System.exit(1);
            }
        }


        // Output information in CSV format
        /*FileWriter writer;
        FileWriter writer2;
        try {
            writer = new FileWriter(rotLoc + name + "_topicwords_" + numTopics + ".csv");
            writer2 = new FileWriter(rotLoc + name + "_topics_" + numTopics + ".csv");
            for (Topics topic: topics){
                writer2.append('"' + Integer.toString(topic.id) + '"' + ",");
                double sumTopic = 0.0;
                String topicText = "";
                for(int i = 0; i < topic.topicLabels.length; i++){
                    if(topic.topicWeights[i] > 0.0){
                        writer.append('"' + Integer.toString(topic.id) + '"' + ',');
                        writer.append('"' + topic.topicLabels[i].trim() + '"' + ',' );
                        writer.append('"' + " " + '"' + ',' );
                        writer.append('"' + Double.toString(topic.topicWeights[i]) + '"' + "\r\n" );
                        sumTopic += topic.topicWeights[i];
                        if( i > 0){
                            topicText += " " + topic.topicLabels[i];
                        }
                        else{
                            topicText += topic.topicLabels[i];
                        }
                    }
                }
                writer2.append('"' + Double.toString(sumTopic) + '"' + ',');
                writer2.append('"' + topicText + '"' + "\r\n");
            }
            writer.flush();
            writer.close();
            writer2.flush();
            writer2.close();
        } catch (IOException e) {
            System.out.println("Error: Could not save output data!");
            System.out.println(e.getMessage());
            System.exit(1);
        }*/

    }

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

    public double[] InferTopics(String UID, String text, StanfordLemmatizer slem, boolean serverProcess){

        // Lemmatise text
        HashMap<String, String> dataIn = new HashMap<String, String>();  // Store the grant ID and descriptions

        dataIn.put("infer", text);

        String rawString = dataIn.get("infer");
        rawString = rawString.toLowerCase() ;		// Everything to lower case
        rawString = rawString.replace('.', ' ') ; // Remove full stops
        rawString = rawString.replace('/', ' ') ; // Remove slashes stops
        rawString = rawString.replace('\\', ' ') ; // Remove slashes stops
        rawString = rawString.replace(',', ' ') ; // Remove full stops
        rawString = rawString.trim().replaceAll(" +", " ");	// Remove extra spaces
        List<String> inputLem = StanfordLemmatizer.removeStopWords( slem.lemmatise(rawString) ) ;

        text = "";
        for (String words: inputLem){
            text += words + " ";
        }

        String lemmatizedFileName = "infer_raw/" + UID + "_infered.txt";

        //Save lemmatised text for topic model inferer.
        try{
            //System.out.println("Serialising new lemmatised text");
            Writer writerL = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(lemmatizedFileName), "UTF8"));
            writerL.append("infer\ten\t" + text + "\r\n");
            writerL.close();

        }
        catch(IOException e){
            if(serverProcess){
                String errorFile = "infer_raw/" + UID + "_status.txt";
                try {                           // Save file if error
                    Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(errorFile), "UTF8"));
                    writer.append("Error in server - could not save files to server");
                    writer.close();
                } catch (IOException e1) {
                    System.exit(1);
                }
            }
            else {
                System.out.println("Error: save text for inferer!");
                System.out.println(e.getMessage());
                return new double[0];
            }
        }

        double[] newProbabilities;

        try
        {
            //Create new instance from the pipeline we already have to ensure the same things happens to it, then load our extra document
            InstanceList newInstance = new InstanceList(instances.getPipe());
            newInstance.addThruPipe(new Instance(text, null, "infer", null));

            TopicInferencer inferencer = model.getInferencer();

            //Uses Gibbs sampling to infer a topic distribution from the new instance (document)
            //In the form of: (instance, numInterations, thinning, burnIn)
            inferencer.setRandomSeed(20);
            newProbabilities = inferencer.getSampledDistribution(newInstance.get(0), 100000, 1, 5);    // 100000, 1, 5

        }
        catch (Exception e)
        {
            System.err.println(e.getMessage());
            System.err.println(e.getCause());
            return new double[0];
        }


        return newProbabilities;
    }
}
