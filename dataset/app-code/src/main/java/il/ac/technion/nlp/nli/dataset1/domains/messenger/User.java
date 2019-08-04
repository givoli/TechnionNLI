package il.ac.technion.nlp.nli.dataset1.domains.messenger;

import il.ac.technion.nlp.nli.core.state.NliEntity;
import org.jetbrains.annotations.NotNull;

/**
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class User implements NliEntity, Comparable{

    private static final long serialVersionUID = -1559124541189358852L;

    public String firstName;

    // more fields not relevant to our dataset can follow...

    public User(String firstName) {
        this.firstName = firstName;
    }

    @Override
    public boolean equals(Object o) {
       return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    /**
     * Note: this method exists to support sorting in interface functions, in order to get deterministic functionality.
     */
    @Override
    public int compareTo(@NotNull Object user) {
        return firstName.compareTo(((User) user).firstName);
    }
}
