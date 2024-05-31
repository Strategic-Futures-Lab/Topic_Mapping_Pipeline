package corpus.lemmatizer;

import P2_Lemmatise.Lemmatizer.ReplaceWords;
import P2_Lemmatise.Lemmatizer.StopWords;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

/**
 * Class instantiating and launching the lemmatizer provided by the CoreNLP library from Stanford
 * (<a href="https://stanfordnlp.github.io/CoreNLP/"> https://stanfordnlp.github.io/CoreNLP/ </a>).
 *
 * @author S. Padilla, T. Methven, P. Le Bras
 * @version 3
 */
public class StanfordLemmatizer {

    // lemmatizer pipeline, e.g., annotate, tokenize, PoS tag,, lemmatise
    private StanfordCoreNLP pipeline;

    /**
     * Constructor, instantiates the lemmatisation pipeline
     */
    public StanfordLemmatizer() {
        // Create StanfordCoreNLP object properties, with POS tagging
        // (required for lemmatization), and lemmatization
        Properties props;
        props = new Properties();
        props.put("annotators", "tokenize, cleanxml, ssplit, pos, lemma");

        /*
         * This is a pipeline that takes in a string and returns various analyzed linguistic forms.
         * The String is tokenized via a tokenizer (such as PTBTokenizerAnnotator),
         * and then other sequence model style annotation can be used to add things like lemmas,
         * POS tags, and named entities. These are returned as a list of CoreLabels.
         * Other analysis components build and store parse trees, dependency graphs, etc.
         *
         * This class is designed to apply multiple Annotators to an Annotation.
         * The idea is that you first build up the pipeline by adding Annotators,
         * and then you take the objects you wish to annotate and pass them in and
         * get in return a fully annotated object.
         *
         *  StanfordCoreNLP loads a lot of models, so you probably
         *  only want to do this once per execution
         */
        this.pipeline = new StanfordCoreNLP(props);

    }

    /**
     * Method processing a String text and returning its list of lemmas
     * @param documentText Text to process
     * @return The list containing the text's lemmas
     */
    public List<String> lemmatise(String documentText) {
        List<String> lemmas = new LinkedList<String>();
        // Create an empty Annotation just with the given text
        Annotation document = new Annotation(documentText);
        // run all Annotators on this text
        this.pipeline.annotate(document);
        // Iterate over all of the sentences found
        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
        for(CoreMap sentence: sentences) {
            // Iterate over all tokens in a sentence
            for (CoreLabel token: sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                // Retrieve and add the lemma for each word into the
                // list of lemmas
                lemmas.add(token.get(CoreAnnotations.LemmaAnnotation.class));
            }
        }
        return lemmas;
    }

    /**
     * Method cleaning a given list of lemmas, removing common stop words and fixing known lemma issues
     * Uses {@link corpus.lemmatizer.StopWords} and {@link corpus.lemmatizer.ReplaceWords} to do so, check these classes to customise lists
     * @param words List of lemmas to clean
     * @return The cleaned list of lemmas
     */
    public static List<String> removeCommonStopWords(List<String> words){
        List<String> newWords = new LinkedList<String>();

        for(String word: words){
            // Remove tokenised words where the dash is still there.
            word = word.startsWith("-") ? word.substring(1) : word;
            word = word.endsWith("-") ? word.substring(word.length()-1) : word;

            // Check the stop words and characters list for the word instance.
            boolean foundStopWord = StopWords.STOPWORDS.contains(word);
            // Check if word is a number with format [-]3343[.343]
            if(!foundStopWord){
                foundStopWord = word.matches("-?\\d+(\\.\\d+)?");
            }
            // Replace missed lemmas, eg latin singular words
            if(!foundStopWord && ReplaceWords.REPLACEWORDS.containsKey(word)){
                word = ReplaceWords.REPLACEWORDS.get(word);
            }

            // If the word passed all these check, we can add it to the list of lemmas
            if(!foundStopWord) newWords.add(word);
        }
        return newWords;
    }
}
