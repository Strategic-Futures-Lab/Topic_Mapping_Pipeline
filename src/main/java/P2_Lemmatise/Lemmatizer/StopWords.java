package P2_Lemmatise.Lemmatizer;

import java.util.List;

/**
 * Class providing static lists of common stop words to remove during the lemmatisation process.
 *
 * @author S. Padilla, T. Methven, P. Le Bras
 * @version 2
 */
@Deprecated
public class StopWords {

    /**
     * List of common stop words in the English language.
     */
    public static final List<String> STOPWORDS = List.of(
            // Digits
            "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
            // A
            "a","ah","able","about","above","according","accordingly","across","actually","after","afterwards",
            "again","against","all","allow","allows","almost","alone","along","already","also","although","always",
            "am","among","amongst","an","and","another","any","anybody","anyhow","anyone","anything","anyway",
            "anyways","anywhere","apart","appear","appreciate","appropriate","are","around","as","aside","ask",
            "asking","associated","at","available","away","awfully",
            // B
            "b","be","became","because","become","becomes","becoming","been","before","beforehand","behind","being",
            "believe","below","beside","besides","best","better","between","beyond","both","brief","but","by",
            // C
            "c","came","can","cannot","cant","cause","causes","certain","certainly","changes","clearly","co","com",
            "come","comes","concerning","consequently","consider","considering","contain","containing","contains",
            "corresponding","could","course","currently",
            // D
            "d","definitely","described","despite","did","different","do","does","doing","done","down","downwards","during",
            // E
            "e","each","edu","eg","eight","either","else","elsewhere","enough","entirely","especially","et","etc",
            "even","ever","every","everybody","everyone","everything","everywhere","ex","exactly","example","except",
            // F
            "f","far","few","fifth","first","five","followed","following","follows","for","former","formerly","forth",
            "four","from","further","furthermore",
            // G
            "g","get","gets","getting","given","gives","go","goes","going","gone","got","gotten","greetings",
            // H
            "h","had","happens","hardly","has","have","having","he","hello","help","hence","her","here","hereafter",
            "hereby","herein","hereupon","hers","herself","hi","him","himself","his","hither","hmm","hopefully",
            "how","howbeit","however",
            // I
            "i","ie","if","ignored","immediate","in","inasmuch","inc","indeed","indicate","indicated","indicates",
            "inner","insofar","instead","into","inward","is","it","its","itself",
            // J
            "j","just",
            // K
            "k","keep","keeps","kept","know","knows","known",
            // L, "ll" added to avoid words like you'll, I'll etc.
            "l","last","lately","later","latter","latterly","least","less","lest","let","like","liked","likely",
            "little","ll","look","looking","looks","ltd",
            "m","mainly","many","may","maybe","me","mean","meanwhile","merely","might","more","moreover","most",
            "mostly","much","must","my","myself",
            // N
            "n","name","namely","nd","near","nearly","necessary","need","needs","neither","never","nevertheless",
            "new","next","nine","no","nobody","non","none","noone","nor","normally","not","nothing","novel","now","nowhere",
            // O
            "o","obviously","of","off","often","oh","ok","okay","old","on","once","one","ones","only","onto","or",
            "other","others","otherwise","ought","our","ours","ourselves","out","outside","over","overall","own",
            // P
            "p","page","particular","particularly","per","perhaps","placed","please","plus","possible","presumably","probably","provides",
            // Q
            "q","que","quite","qv",
            // R
            "r","rather","rd","re","ref","ref2014","really","reasonably","regarding","regardless","regards",
            "relatively","respectively","right",
            // S
            "s","said","same","saw","say","saying","says","second","secondly","see","seeing","seem","seemed","seeming",
            "seems","seen","self","selves","sensible","sent","serious","seriously","seven","several","shall","she",
            "should","since","six","so","some","somebody","somehow","someone","something","sometime","sometimes",
            "somewhat","somewhere","soon","sorry","specified","specify","specifying","still","sub","such","sup","sure",
            // T
            "t","take","taken","tell","tends","th","than","thank","thanks","thanx","that","thats","the","their",
            "theirs","them","themselves","then","thence","there","thereafter","thereby","therefore","therein","theres",
            "thereupon","these","they","think","third","this","thorough","thoroughly","those","though","three",
            "through","throughout","thru","thus","to","together","too","took","toward","towards","tried","tries",
            "truly","try","trying","twice","two",
            // U
            "u","uh","um","un","under","unfortunately","unless","unlikely","until","unto","uoa","up","upon","us","use",
            "used","useful","uses","using","usually","uucp",
            // V, "ve" added to avoid words like I've,you've etc.
            "v","value","various","ve","very","via","viz","vs",
            // W
            "w","want","wants","was","way","we","welcome","well","went","were","what","whatever","when","whence",
            "whenever","where","whereafter","whereas","whereby","wherein","whereupon","wherever","whether","which",
            "while","whither","who","whoever","whole","whom","whose","why","will","willing","wish","with","within",
            "without","wonder","would","would",
            // X
            "x",
            // Y
            "y","yes","yet","yeah","you","your","yours","yourself","yourselves",
            // Z
            "z","zero");

    /**
     * List of non-word characters, eg punctuation.
     */
    public static final List<String> STOPCHARS = List.of("`", "\"", ",", "'", "'s", "#", "£", "$", "&", "!", "-", "/",
            "\\", "|", "?", "%", "^", "(", ")", "=", "+" , "_", "@", "~", ";", ":", "*", " ", "", "\"\"", "``", "''",
            ",", "/", "\\", "\t", "\n", "\r");

    /**
     * List of other common words to exclude, eg HTML tags accidentally scraped, encoded URLs, etc.
     */
    public static final List<String> REMOVEWORDS = List.of("<p>", "</p>", "&quot;", "&amp;", "http", "https", "www",
        "div", "org");

    /*public static final String[] REMOVEWORDS = new String[]{"uk", "ahrc", "bbsrc", "epsrc", "esrc", "mrc", "nerc", "stfc", "rcuk",
            "proposal", "grant", "develop", "technique", "project", "method", "provide", "study", "research", "experimental", "result",
            "understanding", "programme", "university", "area", "work", "include", "activity", "problem", "approach", "make", "time",
            "cost", "group", "field", "set", "system", "question", "application", "account", "case", "<p>", "</p>", "&quot;", "&amp;"};*/

    // public static final String[] STOPPHRASES = new String[]
    //         {
    //                 "Environment template (REF5)",
    //                 "Environment template",
    //                 "(REF5)",
    //                 "REF5",
    //                 "Unit of Assessment"
    //         };

}