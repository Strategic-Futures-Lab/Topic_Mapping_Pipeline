package corpus;

import java.util.List;

public class StopWords extends CleaningModule {

    List<String> stopWords;

    public void loadStopWords(String filename){
        stopWords = readTextFile(filename, "stop word(s)");
        stopWords = stopWords.stream().map(s -> s.trim().toLowerCase()).toList();
    }

    public void removeStopWords(List<String> lemmas){
        lemmas.removeAll(stopWords);
    }
}
