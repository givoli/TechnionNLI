package il.ac.technion.nlp.nli.core.dataset.simple_test_domain;

import il.ac.technion.nlp.nli.core.state.NliEntity;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class Group  implements NliEntity {
    private static final long serialVersionUID = -3407208427004716949L;

    public List<User> users = new LinkedList<>();
    public String name = "NA";

    /**
     * Just to have a field with a name colliding with {@link User}'s field.
     */
    public int age = -1;

}
