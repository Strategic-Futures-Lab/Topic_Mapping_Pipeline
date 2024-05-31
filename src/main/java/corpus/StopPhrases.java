package corpus;

import java.util.List;

public class StopPhrases extends CleaningModule {

    List<String> stopPhrases;

    public void loadStopPhrases(String filename){
        stopPhrases = readTextFile(filename, "stop phrase(s)");
        stopPhrases = stopPhrases.stream().map(s -> s.trim().toLowerCase()).toList();
    }

    public String removeStopPhrases(String rawText){
        String text = rawText;
        for(String phrase: stopPhrases){
            text = text.replaceAll(phrase, " ");
        }
        return text;
    }
}
