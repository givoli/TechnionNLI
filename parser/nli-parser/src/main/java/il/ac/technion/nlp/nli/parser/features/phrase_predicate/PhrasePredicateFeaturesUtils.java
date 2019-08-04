package il.ac.technion.nlp.nli.parser.features.phrase_predicate;

import edu.stanford.nlp.sempre.Derivation;
import edu.stanford.nlp.sempre.tables.features.PredicateInfo;
import il.ac.technion.nlp.nli.parser.features.InstructionFeatureComputer;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PhrasePredicateFeaturesUtils {


    /**
     * Add features about detecting a single phrase that has alignments with multiple predicates.
     */
    private static void addFeaturesAboutPhrasePredicateAlignmentPair(
            Derivation deriv, PredicateInfo predicate, UnlexicalizedAlignmentType alignmentType,
            PredicateInfo predicate2, UnlexicalizedAlignmentType alignmentType2) {

        UnlexicalizedAlignmentType weakerAlignmentType = alignmentType.weakerThan(alignmentType2) ?
                alignmentType : alignmentType2;

        String predicateTypesString = getPredicateTypesString(predicate, predicate2);

        for (String substring1 : Arrays.asList("", weakerAlignmentType.shortName + "|")) {
            for (String substring2 : Arrays.asList("", predicateTypesString)) {

                deriv.addFeature(InstructionFeatureComputer.INSTRUCTION_FEATURES_DOMAIN_IN_SEMPRE, "multiPhraseMatch|"
                 + substring1 + substring2);

            }
        }
    }

    private static String getPredicateTypesString(PredicateInfo predicate, PredicateInfo predicate2) {
        List<String> types = Stream.of(predicate.type, predicate2.type)
                .map(Enum::toString)
                .sorted()
                .collect(Collectors.toList());
        return types.get(0) + "," + types.get(1);
    }

    /**
     * @param result this method adds to this data-structure the data from the
     *               {@link Derivation#localPhrasePredicateAlignments} field of all the descendant derivations of 'deriv'
     *               (including itself).
     */
    private static void addUnlexicalizedMatchesFromEntireDerivation(Derivation deriv,
                                                                    PhrasePredicateAlignments result){
        if(deriv.children != null) {
            deriv.children.forEach(child ->
                    addUnlexicalizedMatchesFromEntireDerivation(child, result));
        }
        result.addAll(deriv.localPhrasePredicateAlignments);
    }



}
