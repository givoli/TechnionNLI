package il.ac.technion.nlp.nli.parser.lexicon;

import com.ofergivoli.ojavalib.data_structures.map.SafeHashMap;
import com.ofergivoli.ojavalib.data_structures.map.SafeMap;
import com.ofergivoli.ojavalib.data_structures.set.SafeHashSet;
import com.ofergivoli.ojavalib.data_structures.set.SafeSet;
import com.ofergivoli.ojavalib.exceptions.UncheckedInvalidArgumentException;
import edu.stanford.nlp.sempre.*;
import edu.stanford.nlp.sempre.tables.FuzzyMatcher;
import il.ac.technion.nlp.nli.core.NliDescriptionsUtils;
import il.ac.technion.nlp.nli.core.dataset.Domain;
import il.ac.technion.nlp.nli.core.reflection.EntityGraphReflection;
import il.ac.technion.nlp.nli.core.state.NliEntity;
import il.ac.technion.nlp.nli.parser.InstructionKnowledgeGraph;
import il.ac.technion.nlp.nli.parser.features.PhraseAssociation;
import il.ac.technion.nlp.nli.parser.type_system.InstructionTypeLookup;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static il.ac.technion.nlp.nli.parser.NameValuesManager.NameValueType.NLI_ENTITY;
import static il.ac.technion.nlp.nli.parser.NameValuesManager.NameValueType.STRING;
import static il.ac.technion.nlp.nli.parser.lexicon.LexiconSemanticFn.Mode.*;


/**
 * Represents Lexicon related data for a specif inference.
 *
 * NOTICE: some code in this class was copied from ppasupat's {@link FuzzyMatchFn}, and then modified.
 *
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
@SuppressWarnings("unused")
public class Lexicon {


    public static class DerivedFormulas {
        /**
         * The order is arbitrary (but might be deterministic).
         */
        public final List<Pair<Formula, FeatureVector>> elements = new LinkedList<>();
    }

    private final InstructionKnowledgeGraph graph;
    private final PhraseAssociation phraseAssociation;
    private final boolean deterministic;

    /**
     * Contains data about all potential anchored derivation, regardless of whether or not a relevant phrase actually
     * appears in the utterance.
     */
    private final SafeMap<LexiconSemanticFn.Mode, SafeMap<String, DerivedFormulas>>
            modeToProcessedPhraseToPotentialAnchoredDerivedFormulas = new SafeHashMap<>();

    private final SafeMap<LexiconSemanticFn.Mode, DerivedFormulas> modeToNonAnchoredDerivedFormulas
            = new SafeHashMap<>();



    /**
     * @param phraseAssociation associations are added to this argument, according to the potential anchored derivations
     *                          created.
     */
    public Lexicon(InstructionKnowledgeGraph graph, PhraseAssociation phraseAssociation, boolean deterministic) {
        this.graph = graph;
        this.phraseAssociation = phraseAssociation;
        this.deterministic = deterministic;
        // Anchored:

        modeToProcessedPhraseToPotentialAnchoredDerivedFormulas.putNewKey(ANCHORED_STRING_ENTITY,
                createAnchoredDerivedFormulasDenotingStringEntities());

        modeToProcessedPhraseToPotentialAnchoredDerivedFormulas.putNewKey(ANCHORED_ENUM_VALUE,
                createAnchoredDerivedFormulasDenotingEnumValues());



        // Non-anchored:

        modeToNonAnchoredDerivedFormulas.putNewKey(NLI_METHOD_NAME,
                createDerivedFormulasDenotingFunctionNames());

        modeToNonAnchoredDerivedFormulas.putNewKey(PRIMITIVE_RELATION,
                createDerivedFormulasDenotingRelations(PRIMITIVE_RELATION));

        modeToNonAnchoredDerivedFormulas.putNewKey(NLI_ENTITY_RELATION,
                createDerivedFormulasDenotingRelations(NLI_ENTITY_RELATION));

        modeToNonAnchoredDerivedFormulas.putNewKey(ALL_NLI_ENTITIES_PER_TYPE,
                createDerivedFormulasDenotingAllNliEntitiesOfTheSameType());



    }


    /**
     * @return the key is a processed phrase, and the value is the {@link DerivedFormulas} anchored to it.
     */
    private SafeMap<String, DerivedFormulas> createAnchoredDerivedFormulasDenotingStringEntities() {

        SafeMap<String, DerivedFormulas> result = new SafeHashMap<>();


        Collection<String> secondArgStrings = graph.kb.getRelationToSecondArgToFirstArgs().values()
                .stream()
                .flatMap(secondArgToFirstArgs -> secondArgToFirstArgs.keySet().stream())
                .filter(value->value instanceof NameValue &&
                        graph.nameValuesManager.getNameValueType((NameValue)value) == STRING)
                .map(value->graph.nameValuesManager.getString((NameValue)value))
                .collect(Collectors.toCollection(SafeHashSet::new));

        if (deterministic)
            secondArgStrings = secondArgStrings.stream().sorted().collect(Collectors.toList());

        secondArgStrings.forEach(phrase->{
            FeatureVector features = createBasicFeatureVector(ANCHORED_STRING_ENTITY);
            NameValue value = graph.nameValuesManager.createNameValueRepresentingString(phrase);
            addAnchoredDerivedFormula(phrase, value, features, result);
        });

        return result;
    }


    /**
     * Creates formulas for enum values that appear in {@link #graph} as the second argument of some relation.
     * @return the key is processed phrase, and the value is the {@link DerivedFormulas} anchored to it.
     */
    private SafeMap<String, DerivedFormulas> createAnchoredDerivedFormulasDenotingEnumValues() {

        SafeMap<String, DerivedFormulas> result = new SafeHashMap<>();

        Collection<Enum<?>> enumValues = new HashSet<>();

        Collection<Enum<?>> finalEnumValues = enumValues;
        EntityGraphReflection.getPossiblyReachableNliEntityClasses(
                graph.getInitialState().getRootEntity().getClass(), true, deterministic).forEach(nliEntityClass->
                EntityGraphReflection.getRelationFieldsOfNliEntityClass(nliEntityClass, deterministic).forEach(
                        relation->{
                            if (relation.getType().isEnum()) {
                                for (Object enumConst : relation.getType().getEnumConstants())
                                    finalEnumValues.add((Enum<?>) enumConst);
                            }
                        }));


        if (deterministic)
            enumValues = enumValues.stream().sorted().collect(Collectors.toList());

        enumValues.forEach(enumValue ->
                NliDescriptionsUtils.generateDescriptionsForEnumValue(enumValue).forEach(description -> {
                    FeatureVector features = createBasicFeatureVector(ANCHORED_ENUM_VALUE);
                    NameValue value = graph.nameValuesManager.createNameValueRepresentingEnumValue(enumValue);
                    addAnchoredDerivedFormula(description, value, features, result);
                }));
        return result;
    }


    /**
     * Adds the derivation to 'outResult' and adds the an association to 'phraseAssociation'.
     * @param value the formula of the added derivation is a {@link ValueFormula} with this argument as its value.
     * @param outResult the key is a processed phrase.
     */
    private void addAnchoredDerivedFormula(String phrase, Value value, FeatureVector features,
                                           SafeMap<String, DerivedFormulas> outResult) {

        String processedPhrase = phraseAssociation.processPhrase.apply(phrase);
        DerivedFormulas derivedFormulas;
        if (outResult.safeContainsKey(processedPhrase)) {
            // multiple different phrases have the same processed form.
            derivedFormulas = outResult.getExisting(processedPhrase);
        } else {
            derivedFormulas = new DerivedFormulas();
            outResult.putNewKey(processedPhrase, derivedFormulas);
        }
        derivedFormulas.elements.add(new ImmutablePair<>(new ValueFormula<>(value), features));
        phraseAssociation.addAssociation(value, phrase, false);
    }



    private DerivedFormulas createDerivedFormulasDenotingFunctionNames() {

        DerivedFormulas result = new DerivedFormulas();


        Domain domain = graph.getInitialState().getDomain();

        List<Formula> formulas = new LinkedList<>();
        domain.getFriendlyIdToMethodId().keySet().forEach(friendlyId -> {
            ValueFormula<NameValue> formula = new ValueFormula<>(graph.nameValuesManager
                    .createNameValueRepresentingNliMethod(friendlyId));
            FeatureVector features = createBasicFeatureVector(NLI_METHOD_NAME);
            result.elements.add(new ImmutablePair<>(formula, features));
        });
        return result;
    }


    /**
     * @param mode either {@link LexiconSemanticFn.Mode#PRIMITIVE_RELATION} or
     * {@link LexiconSemanticFn.Mode#NLI_ENTITY_RELATION}.
     */
    private DerivedFormulas createDerivedFormulasDenotingRelations(LexiconSemanticFn.Mode mode) {

        DerivedFormulas result = new DerivedFormulas();

        graph.kb.getAllRelations(deterministic).stream()
                .filter(relation->{
                    Value secondArgRepresentative = graph.kb.getRelationToSecondArgToFirstArgs()
                            .getExisting(relation).keySet().iterator().next();
                    if (mode == PRIMITIVE_RELATION)
                        return !(secondArgRepresentative instanceof NameValue) ||
                                Objects.requireNonNull(graph.nameValuesManager.getNameValueType(
                                        ((NameValue) secondArgRepresentative))).representingPrimitiveEntity;
                    if (mode == NLI_ENTITY_RELATION)
                        return secondArgRepresentative instanceof NameValue &&
                                graph.nameValuesManager.getNameValueType((NameValue)secondArgRepresentative)
                                        == NLI_ENTITY;
                    throw new UncheckedInvalidArgumentException();
                })
                .forEach(relation->{
                    Formula formula = new ValueFormula<>(relation);
                    FeatureVector features = createBasicFeatureVector(mode);
                    result.elements.add(new ImmutablePair<>(formula, features));
                });

        return result;
    }


    /**
     * For each {@link NliEntity} type participating in the {@link InstructionKnowledgeGraph}, this method
     * generate a formula denoting all the entities in the KB of that type.
     */
    private DerivedFormulas createDerivedFormulasDenotingAllNliEntitiesOfTheSameType() {
        DerivedFormulas result = new DerivedFormulas();

        InstructionTypeLookup typeLookup = new InstructionTypeLookup();

        SafeMap<Value, SafeSet<NameValue>> nliEntityTypeToEntities = graph.kb.getRelationToSecondArgToFirstArgs()
                .safeGet(graph.NLI_ENTITY_TYPE_RELATION_NV);

        if (nliEntityTypeToEntities == null)
            return result;

        Stream<Value> nliEntityTypes = nliEntityTypeToEntities.keySet().stream();
        if (deterministic)
            nliEntityTypes = nliEntityTypes.sorted(Comparator.comparing(entity -> ((NameValue) entity).id));

        nliEntityTypes.forEach(nliEntityType -> {
            Formula formula = new JoinFormula(
                    new ReverseFormula(new ValueFormula<>(graph.NLI_ENTITY_TYPE_RELATION_NV)),
                    new ValueFormula<>(nliEntityType));

            FeatureVector features = createBasicFeatureVector(ALL_NLI_ENTITIES_PER_TYPE);
            result.elements.add(new ImmutablePair<>(formula, features));
        });

        return result;
    }


    public DerivedFormulas getAnchoredFormulas(LexiconSemanticFn.Mode mode, String phrase){

        if (!FuzzyMatcher.checkPunctuationBoundaries(phrase)) return new DerivedFormulas();

        String processedPhrase = phraseAssociation.processPhrase.apply(phrase);
        DerivedFormulas result = modeToProcessedPhraseToPotentialAnchoredDerivedFormulas.getExisting(mode)
                .safeGet(processedPhrase);
        return result == null ? new DerivedFormulas() : result;
    }


    public DerivedFormulas getNonAnchoredFormulas(LexiconSemanticFn.Mode mode){
        return modeToNonAnchoredDerivedFormulas.getExisting(mode);
    }


    /**
     * Currently, we don't extract features indicating lexicon items were derived with a specific
     * {@link LexiconSemanticFn.Mode}.
     */
    private static FeatureVector createBasicFeatureVector(LexiconSemanticFn.Mode mode) {
        return new FeatureVector();
    }

}
