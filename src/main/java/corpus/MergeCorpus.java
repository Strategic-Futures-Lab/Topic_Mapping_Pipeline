package corpus;

import IO.Console;
import IO.Timer;
import pipeline.config.ModuleConfig;
import pipeline.config.ProjectConfig;
import pipeline.config.modules.MergeCorpusConfig;
import data.Corpus;
import data.Document;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.util.*;

/**
 * Module loading several corpus JSON files and merging them into a single corpus
 *
 * @author P. Le Bras
 * @version 1
 */
public class MergeCorpus extends CorpusModule {

    // module parameters
    private MergeCorpusConfig config;

    private MergeCorpus(MergeCorpusConfig c){
        config = c;
        config.logConfig();
    }

    /**
     * Main module method - processes parameters, loads corpora in one document list, save in one corpus output
     * @param moduleParameters module parameters
     * @param projectParameters project meta parameters
     * @throws Exception If the corpora cannot load properly
     */
    public static void run(ModuleConfig moduleParameters, ProjectConfig projectParameters) throws Exception {
        String MODULE_NAME = moduleParameters.moduleName+" ("+moduleParameters.moduleType+")";
        Console.moduleStart(MODULE_NAME);
        Timer.start(MODULE_NAME);
        MergeCorpus instance = new MergeCorpus((MergeCorpusConfig) moduleParameters);
        try{
            instance.loadCorpora();
            instance.writeCorpus(instance.config.outputFile);
        } catch (Exception e){
            Console.moduleFail(MODULE_NAME);
            throw e;
        }
        Console.moduleComplete(MODULE_NAME);
        Timer.stop(MODULE_NAME);
    }

    // load corpora one by one to build the list of documents
    private void loadCorpora() throws IOException, ParseException {
        int corpusIndex = 0;
        int docIndex = 0;
        corpus = new Corpus();
        for(String filename: config.corpusFiles){
            Corpus loadedCorpus = new Corpus(filename);
            for(Map.Entry<String, Document> entry: loadedCorpus.documents.entrySet()){
                Document doc = entry.getValue();
                doc.prefixId(Integer.toString(corpusIndex));
                doc.setIndex(docIndex);
                filterDocumentFields(doc, config.docFields);
                corpus.add(doc.getId(), doc);
                docIndex++;
            }
            corpusIndex++;
        }
        Console.info(corpusIndex+" corpora loaded, "+docIndex+" documents in total");
    }

}
