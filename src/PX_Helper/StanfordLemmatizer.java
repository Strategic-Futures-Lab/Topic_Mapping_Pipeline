/*
 * Created by Stefano Padilla.
 * Last update 22 / 2 / 2015
 * Copyright Heriot-Watt University
 * Agreed for use inside EPSRC
 */

package PX_Helper;

/*
 * Using CoreNLP Library from Stanford
 * http://nlp.stanford.edu/software/corenlp.shtml
 *
 * Most of the code here comes from their examples.
 */

import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

public class

StanfordLemmatizer
{

    protected StanfordCoreNLP pipeline;

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
        this.pipeline = new StanfordCoreNLP( props);

    }

    public List<String> lemmatise(String documentText)
    {
        List<String> lemmas = new LinkedList<String>();
        // Create an empty Annotation just with the given text
        Annotation document = new Annotation(documentText);
        // run all Annotators on this text
        this.pipeline.annotate(document);
        // Iterate over all of the sentences found
        List<CoreMap> sentences = document.get(SentencesAnnotation.class);
        for(CoreMap sentence: sentences) {
            // Iterate over all tokens in a sentence
            for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
                // Retrieve and add the lemma for each word into the
                // list of lemmas
                lemmas.add(token.get(LemmaAnnotation.class));
            }
        }
        return lemmas;
    }

    public static List<String> removeStopWords(List<String> words){
        List<String> newWords = new LinkedList<String>();

        for(String word: words){
            boolean foundStopWord = false;

            // Remove tokenised words were the dash is still there.
            word = word.startsWith("-") ? word.substring(1) : word;
            word = word.endsWith("-") ? word.substring(word.length()-1) : word;

            // Loop through all words and remove stopwrods, characters, and special corpora dependant words.
            for (String sWord: StopWords.STOPWORDS){
                if(word.equals(sWord)){
                    foundStopWord = true;
                }
            }
            for (String sWord: StopWords.STOPCHARS){
                if(word.equals(sWord)){
                    foundStopWord = true;
                }
            }
            for (String sWord: StopWords.REMOVEWORDS){
                if(word.equals(sWord)){
                    foundStopWord = true;
                }
            }

            // Fix annoying lemma
            word = word.replace("datum", "data");

            //Remove numbers in the format [-]3343[.343]
            if(word.matches("-?\\d+(\\.\\d+)?")){
                foundStopWord = true;
            }

            if(!foundStopWord) newWords.add(word);
        }
        return newWords;
    }

}