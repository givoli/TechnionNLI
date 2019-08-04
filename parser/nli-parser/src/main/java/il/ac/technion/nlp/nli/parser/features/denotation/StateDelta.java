package il.ac.technion.nlp.nli.parser.features.denotation;

import com.ofergivoli.ojavalib.data_structures.set.SafeHashSet;
import com.ofergivoli.ojavalib.data_structures.set.SafeSet;
import edu.stanford.nlp.sempre.NameValue;
import il.ac.technion.nlp.nli.parser.InstructionKnowledgeGraph;
import il.ac.technion.nlp.nli.parser.kb.KbTriple;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Immutable.
 *
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class StateDelta {

    public final List<KbTriple> triplesAdded;
    public final List<KbTriple> triplesRemoved;

    private StateDelta(List<KbTriple> triplesAdded, List<KbTriple> triplesRemoved) {
        this.triplesAdded = triplesAdded;
        this.triplesRemoved = triplesRemoved;
    }

    public static StateDelta create(InstructionKnowledgeGraph initialGraph, InstructionKnowledgeGraph destinationGraph,
                                    boolean deterministic) {

        List<KbTriple> initialTriples = initialGraph.kb.createTripleList(deterministic);
        List<KbTriple> destinationTriples = destinationGraph.kb.createTripleList(deterministic);

        SafeSet<KbTriple> initialTripleSet = new SafeHashSet<>(initialTriples);
        SafeSet<KbTriple> destinationTripleSet = new SafeHashSet<>(destinationTriples);


        List<KbTriple> triplesAdded = getTriplesInA_ThatAreNotInB(destinationTriples, initialTripleSet);
        List<KbTriple> triplesRemoved = getTriplesInA_ThatAreNotInB(initialTriples, destinationTripleSet);

        return new StateDelta(triplesAdded, triplesRemoved);
    }

    /**
     * @return order follows order in 'a'.
     */
    private static List<KbTriple> getTriplesInA_ThatAreNotInB(List<KbTriple> a, SafeSet<KbTriple> b) {
        return a.stream()
                .filter(t->!b.safeContains(t))
                .collect(Collectors.toList());
    }

    /**
     * @return a list that includes a triple (a,r,b) if the triple is in {@link #triplesRemoved} and also there exists a
     * triple (a,r,x) in {@link #triplesAdded}, for some x.
     */
    public List<KbTriple> findTriplesInInitialStateThatChange() {
        SafeSet<Pair<NameValue, NameValue>> firstArgAndRelationPairsInTriplesAdded = triplesAdded.stream()
                .map(triple->new ImmutablePair<>(triple.firstArg, triple.relation))
                .collect(Collectors.toCollection(SafeHashSet::new));
        List<KbTriple> result = new LinkedList<>();
        triplesRemoved.forEach(triple->{
            if (firstArgAndRelationPairsInTriplesAdded.safeContains(
                    new ImmutablePair<>(triple.firstArg, triple.relation)))
                result.add(triple);
        });
        return result;
    }


}
