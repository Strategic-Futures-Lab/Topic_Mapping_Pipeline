package P2_Lemmatise.Lemmatizer;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Class providing a static map of common words often missed or wrongly lemmatised and their correct lemma.
 *
 * @author P. Le Bras
 * @version 1
 */
@Deprecated
public class ReplaceWords {

    public static final Map<String, String> REPLACEWORDS = Stream.of(new String[][] {
            { "datum", "data"},
    }).collect(Collectors.toMap(d -> d[0], d -> d[1]));
}
