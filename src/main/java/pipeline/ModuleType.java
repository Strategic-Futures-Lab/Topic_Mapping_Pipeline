package pipeline;

import IO.Console;
import config.ModuleConfig;
import config.ProjectConfig;
import config.ProjectConfigParser;
import config.modules.*;

import java.lang.reflect.InvocationTargetException;

/**
 * Enumeration of all modules in the pipeline, with associated classes (inc. configuration)
 */
public enum ModuleType {

    // Input modules
    CSVInput (input.CSVInput.class, InputConfigCSV.class),
    TXTInput (input.TXTInput.class, InputConfigTXT.class),
    PDFInput (input.PDFInput.class, InputConfigPDF.class),
    HTMLInput (input.HTMLInput.class, InputConfigHTML.class),
    GTRInput (input.GTRInput.class, InputConfigGTR.class),
    // Corpus management modules
    BuildText (corpus.BuildText.class, BuildTextConfig.class),
    MergeCorpus (corpus.MergeCorpus.class, MergeCorpusConfig.class),
    StopPhrases (corpus.StopPhrases.class, StopPhrasesConfig.class),
    Lemmatise (corpus.Lemmatise.class, LemmatiseConfig.class),
    StopWords (corpus.StopPhrases.class, StopWordsConfig.class);

    public final Class module;
    public final Class config;

    ModuleType(Class mod, Class conf){
        module = mod;
        config = conf;
    }

    public void runModule(ModuleConfig moduleParams, ProjectConfig projectParams) throws RuntimeException {
        try {
            module.getMethod("run", ModuleConfig.class, ProjectConfig.class ).invoke(null, moduleParams, projectParams );
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            Console.error("Error while trying to execute the module " + this);
            throw new RuntimeException(e);
        }
    }

    public static ModuleType getType(String typeName) throws ProjectConfigParser.ParseException {
        return switch (typeName) {
            case "inputCSV" -> CSVInput;
            case "inputTXT" -> TXTInput;
            case "inputPDF" -> PDFInput;
            case "inputHTML" -> HTMLInput;
            case "inputGTR" -> GTRInput;
            case "buildText" -> BuildText;
            case "mergeCorpus" -> MergeCorpus;
            case "stopPhrases" -> StopPhrases;
            case "lemmatise" -> Lemmatise;
            case "stopWords" -> StopWords;
            default ->
                    throw new ProjectConfigParser.ParseException("Module type \"" + typeName + "\" is not recognised");
        };
    }
}
