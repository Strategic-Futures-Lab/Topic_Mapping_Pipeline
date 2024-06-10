package P3_TopicModelling.TopicModelCore;

import java.io.Serializable;

/**
 * Class representing a document input for the model.
 *
 * @author S. Padilla, T. Methven, P. Le Bras
 * @version 1
 */
@Deprecated
public class InputDocument implements Serializable {

    /** Serialisation ID. */
    private static final long serialVersionUID = 2244011647262167470L;

    /** Document's identifier. */
    public String ID;
    /** Document's content: list of lemmatised words. */
    public String inputLemmas;

    /**
     * Constructor
     * @param ID Document's identifier.
     * @param inputLemmas Document's lemmas.
     */
    public InputDocument(String ID, String inputLemmas){
        this.ID = ID;
        this.inputLemmas = inputLemmas;
    }
}
