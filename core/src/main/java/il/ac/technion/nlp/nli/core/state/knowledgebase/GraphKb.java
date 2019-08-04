package il.ac.technion.nlp.nli.core.state.knowledgebase;

import com.google.common.base.Verify;
import com.ofergivoli.ojavalib.data_structures.set.SafeHashSet;
import com.ofergivoli.ojavalib.data_structures.set.SafeSet;
import com.ofergivoli.ojavalib.string.StringManager;

import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Not meant to be saved persistently.
 *
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class GraphKb {

    private final SafeSet<KBTriple> triples = new SafeHashSet<>();

    public GraphKb()
    {
    }

    public GraphKb(Collection<KBTriple> triples)
    {
        this.triples.addAll(triples);
    }


    public void add(KBTriple triple) {
        // actually add the triple:
        Verify.verify(triples.add(triple));
    }


    public SafeSet<KBTriple> getTriples() {
        return triples;
    }


    @Override
    public String toString() {
        return StringManager.collectionToStringWithNewlines(
                triples.stream()
                        .sorted(Comparator.comparing(KBTriple::toString))
                        .collect(Collectors.toList()));
    }

    /**
     * Based on: {@link #triples}. So true is returned iff the two graphs are identical.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GraphKb graphKb = (GraphKb) o;
        return Objects.equals(getTriples(), graphKb.getTriples());
    }

    /**
     * Based on: {@link #triples}
     */
    @Override
    public int hashCode() {
        return Objects.hash(getTriples());
    }
}
