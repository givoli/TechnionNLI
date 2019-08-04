package il.ac.technion.nlp.nli.parser.features.denotation;

import com.ofergivoli.ojavalib.data_structures.set.SafeHashSet;
import com.ofergivoli.ojavalib.data_structures.set.SafeSet;
import edu.stanford.nlp.sempre.Derivation;
import edu.stanford.nlp.sempre.Example;
import edu.stanford.nlp.sempre.NameValue;
import il.ac.technion.nlp.nli.core.state.State;
import il.ac.technion.nlp.nli.parser.InstructionKnowledgeGraph;
import il.ac.technion.nlp.nli.parser.NameValuesManager;
import il.ac.technion.nlp.nli.parser.kb.KbTriple;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Extracts features with regard to a given {@link StateDelta} object (using also other data from the example).
 *
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class StateDeltaFeatureGenerator {

    private final Example sempreExample;
    private final Derivation derivation;
    private final StateDelta delta;
    private final NameValuesManager nameValuesManager;


    public StateDeltaFeatureGenerator(Example sempreExample, Derivation derivation, InstructionKnowledgeGraph graph,
                                      State endState, boolean deterministic) {

        this.sempreExample = sempreExample;
        this.derivation = derivation;
        delta = StateDelta.create(graph, new InstructionKnowledgeGraph(endState, deterministic), deterministic);
        nameValuesManager = graph.nameValuesManager;
    }


    /**
     * Does not extract features returned by {@link #generateFeaturesToBeConcatenatedWithPhraseFeatures()}.
     */
    public boolean isEmpty(){
        return (delta.triplesAdded.isEmpty() && delta.triplesRemoved.isEmpty());
    }


    // TODO: delete if not used (those features didn't help).
    public List<String> generateFeaturesToBeConcatenatedWithPhraseFeatures() {

        List<String> result = new LinkedList<>();


        List<KbTriple> changing = delta.findTriplesInInitialStateThatChange();
        if (!changing.isEmpty()){
            // checking if it's a relation field change (and not for example a change of order in a list field):
            if (changing.stream().anyMatch(t-> nameValuesManager.getNameValueType(t.relation) ==
                    NameValuesManager.NameValueType.FIELD_RELATION))
                result.add("FieldValuesChanged");
        }


        // We ignore triples that their relation and first-arg pair is in the following set, when looking for
        // triples that where added/removed (because we assume such triples where simply modified - i.e. they were
        // replaces with a similar triple that differ only in its second arg).
        SafeSet<Pair<NameValue,NameValue>> relationAndFirstArgOfChangingTriples = changing.stream()
                .map(triple->new ImmutablePair<>(triple.relation, triple.firstArg))
                .collect(Collectors.toCollection(SafeHashSet::new));


        if (delta.triplesAdded.stream().anyMatch(triple->!relationAndFirstArgOfChangingTriples.safeContains(
                new ImmutablePair<>(triple.relation, triple.firstArg))))
            result.add("AddedTriples");


        if (delta.triplesRemoved.stream().anyMatch(triple->!relationAndFirstArgOfChangingTriples.safeContains(
                new ImmutablePair<>(triple.relation, triple.firstArg))))
            result.add("RemovedTriples");

        return result;
    }
}
