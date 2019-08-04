package il.ac.technion.nlp.nli.core.dataset;

import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

/**
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class ExampleCorrectness implements Serializable{

    private static final long serialVersionUID = -4008998881298747655L;

    /**
     * null if not marked either way.
     */
    @Nullable public Boolean markedCorrect;

    /**
     * null if not marked either way.
     * TODO: define precisely.
     */
    @Nullable public Boolean markedProblematic;


    public ExampleCorrectness(Boolean markedCorrect, Boolean markedProblematic) {
        this.markedCorrect = markedCorrect;
        this.markedProblematic = markedProblematic;
    }
}
