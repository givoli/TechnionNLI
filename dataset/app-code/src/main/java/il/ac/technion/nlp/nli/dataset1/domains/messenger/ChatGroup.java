package il.ac.technion.nlp.nli.dataset1.domains.messenger;

import il.ac.technion.nlp.nli.core.state.NliEntity;

import java.util.List;

/**
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class ChatGroup implements NliEntity {

    private static final long serialVersionUID = 6353018790086535346L;

    public List<User> contacts;
    public boolean muted;

    /**
     * This also counts users which are not contacts.
     */
    public int participantsNumber;

    public ChatGroup(List<User> contacts, boolean muted, int participantsNumber) {
        this.contacts = contacts;
        this.muted = muted;
        this.participantsNumber = participantsNumber;
    }
}
