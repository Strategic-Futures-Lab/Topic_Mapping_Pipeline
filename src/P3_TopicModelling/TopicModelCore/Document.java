package P3_TopicModelling.TopicModelCore;

import java.io.Serializable;

public class Document implements Serializable {
    public String ID, Lemma;

    public Document(String ID, String Lemma){
        this.ID = ID;
        this.Lemma = Lemma;
    }
}
