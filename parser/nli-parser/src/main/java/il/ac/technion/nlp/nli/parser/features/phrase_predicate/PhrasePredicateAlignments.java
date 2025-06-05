package il.ac.technion.nlp.nli.parser.features.phrase_predicate;

import ofergivoli.olib.data_structures.map.Maps;
import ofergivoli.olib.data_structures.map.SafeMap;
import ofergivoli.olib.data_structures.map.SafeMapWrapper;
import edu.stanford.nlp.sempre.tables.features.PredicateInfo;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * A collection of {@link PhrasePredicateAlignment} objects, held as a map from phrase to a map from predicate to
 * alignment type.
 */
public class PhrasePredicateAlignments {

    /**
     * Iteration order of the map and its value maps is deterministic (determined by insertion order).
     */
    private final SafeMap<Phrase, SafeMap<PredicateInfo, List<UnlexicalizedAlignmentType>>>
            phraseToPredicateToAlignmentTypes = new SafeMapWrapper<>(new LinkedHashMap<>());


    public void add(PhrasePredicateAlignment alignment){

        if (!phraseToPredicateToAlignmentTypes.safeContainsKey(alignment.phrase))
            phraseToPredicateToAlignmentTypes.putNewKey(alignment.phrase, new SafeMapWrapper<>(new LinkedHashMap<>()));

        Maps.addToMapOfCollections(phraseToPredicateToAlignmentTypes.getExisting(alignment.phrase),
                alignment.predicate, alignment.alignmentType, LinkedList::new);
    }

    /**
     * Adds all the alignments in 'other'.
     */
    public void addAll(PhrasePredicateAlignments other){
        other.phraseToPredicateToAlignmentTypes.forEach((phrase, predicateToAlignmentTypes)->
            predicateToAlignmentTypes.forEach((predicate,alignmentTypes)->
                    alignmentTypes.forEach(alignmentType->
                            add(new PhrasePredicateAlignment(phrase, predicate, alignmentType)))));
    }

    /**
     * @return order is deterministic (defined by insertion order).
     */
    public List<PhrasePredicateAlignment> createAlignmentList() {

        List<PhrasePredicateAlignment> result = new LinkedList<>();

        phraseToPredicateToAlignmentTypes.forEach((phrase, predicateToAlignmentTypes) ->
                predicateToAlignmentTypes.forEach((predicate, alignmentTypes) ->
                        alignmentTypes.forEach(alignmentType ->
                                result.add(new PhrasePredicateAlignment(phrase, predicate, alignmentType)))));
        return result;
    }

    /**
     * @return a map containing an entry for each alignment with 'phrase'.
     */
    public @Nullable SafeMap<PredicateInfo, List<UnlexicalizedAlignmentType>> getAlignmentDataForPhrase(
            Phrase phrase) {
        return phraseToPredicateToAlignmentTypes.safeGet(phrase);
    }
}
