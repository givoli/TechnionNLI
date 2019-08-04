package il.ac.technion.nlp.nli.parser.kb;

import edu.stanford.nlp.sempre.NameValue;
import edu.stanford.nlp.sempre.Value;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.Objects;

/**
 * Immutable.
 *
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class KbTriple implements Comparable {

    public final NameValue firstArg;
    public final NameValue relation;
    public final Value secondArg;

    public KbTriple(NameValue firstArg, NameValue relation, Value secondArg) {
        this.firstArg = firstArg;
        this.relation = relation;
        this.secondArg = secondArg;
    }

    @Override
    public int compareTo(@NotNull Object o) {
        return Comparator.comparing((KbTriple t)->t.firstArg.id)
                .thenComparing(t->t.relation.id)
                .thenComparing(t->t.secondArg.toString())
                .compare(this, (KbTriple)o);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KbTriple kbTriple = (KbTriple) o;
        return Objects.equals(firstArg, kbTriple.firstArg) &&
                Objects.equals(relation, kbTriple.relation) &&
                Objects.equals(secondArg, kbTriple.secondArg);
    }

    @Override
    public int hashCode() {

        return Objects.hash(firstArg, relation, secondArg);
    }

    @Override
    public String toString() {
        return "[" + firstArg + "," + relation + "," + secondArg + "]";
    }
}
