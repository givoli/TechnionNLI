package il.ac.technion.nlp.nli.parser.features;

import ofergivoli.olib.data_structures.map.Maps;
import ofergivoli.olib.data_structures.map.SafeHashMap;
import ofergivoli.olib.data_structures.map.SafeMap;
import ofergivoli.olib.data_structures.set.SafeHashSet;
import ofergivoli.olib.data_structures.set.SafeLinkedHashSet;
import ofergivoli.olib.data_structures.set.SafeSet;
import edu.stanford.nlp.sempre.NameValue;
import edu.stanford.nlp.sempre.Value;
import edu.stanford.nlp.sempre.tables.StringNormalizationUtils;
import edu.stanford.nlp.sempre.tables.features.PredicateInfo;
import il.ac.technion.nlp.nli.core.NliDescriptionsUtils;
import il.ac.technion.nlp.nli.core.state.NliEntity;
import il.ac.technion.nlp.nli.parser.general.CachedFunction;
import il.ac.technion.nlp.nli.parser.BooleanEnum;
import il.ac.technion.nlp.nli.parser.InstructionKnowledgeGraph;
import il.ac.technion.nlp.nli.parser.NameValuesManager;
import il.ac.technion.nlp.nli.parser.kb.GraphKbWithSempreTypes;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.stream.Collectors;

/**
 * Represents a mapping from {@link Value}s that either appear in an {@link InstructionKnowledgeGraph} or represent an
 * NLI method name; to phrases that their appearance in the utterance is expected to be associated with those
 * {@link Value}s appearing in the correct logical form.
 * The inverse mapping is also held.
 *
 * In this class, an inverse {@link NameValue} is equivalent to its non-inverse form.
 *
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class PhraseAssociation {


    /**
     * The keys in {@link #predicateInfoToAssociatedProcessedPhrases} that are probably not domain specific.
     * Doesn't contain any {@link PredicateInfo} with an inverse {@link NameValue}.
     */
    private final SafeSet<PredicateInfo> domainIndependentPredicateInfos = new SafeHashSet<>();

    /**
     * Takes a phrase and returns a processed phrase.
     */
    public final CachedFunction<String, String> processPhrase = new CachedFunction<>(false, phrase->
            StringNormalizationUtils.collapseNormalize(PredicateInfo.getLemma(phrase).toLowerCase()));

    /**
     * @see #getPredicateInfosAssociatedWithPhrase(String)
     */
    private final SafeMap<String, SafeLinkedHashSet<PredicateInfo>> processedPhraseToAssociatedPredicateInfos
            = new SafeHashMap<>();

    /**
     * The iteration order of each value element is from most to least important.
     * The keys don't include any {@link PredicateInfo} with an inverse {@link NameValue}.
     */
    private final SafeMap<PredicateInfo, SafeLinkedHashSet<String>> predicateInfoToAssociatedProcessedPhrases
            = new SafeHashMap<>();

    private final NameValuesManager nameValueManager;



    /**
     * Initializes an instance with associations for the relevant {@link NameValue}s that either appear in the KB of
     * 'graph', or represent nli method names in the domain of 'graph'.
     */
    public PhraseAssociation(InstructionKnowledgeGraph graph, boolean useDescriptions) {
        this.nameValueManager = graph.nameValuesManager;
        getAllNameValuesInKb(graph.kb).forEach(nameValue->
                addAssociationsForNameValue(graph, nameValue, useDescriptions));

        graph.initialState.getDomain().getFriendlyIdToMethodId().keySet().stream()
                .sorted() // for determinism.
                .forEach(friendlyId->{
                    NameValue nameValue = graph.nameValuesManager.createNameValueRepresentingNliMethod(friendlyId);
                    addAssociationsForNameValue(graph, nameValue, useDescriptions);
                });
    }


    /**
     * @return The iteration order is arbitrary (but deterministic w.r.t order of method calls with this instance).
     *         Doesn't contain any {@link PredicateInfo} with an inverse {@link NameValue}.
     */
    public @NotNull SafeLinkedHashSet<PredicateInfo> getPredicateInfosAssociatedWithPhrase(String phrase) {
        SafeLinkedHashSet<PredicateInfo> result = processedPhraseToAssociatedPredicateInfos.safeGet(
                processPhrase.apply(phrase));
        return result == null ? new SafeLinkedHashSet<>() : result;
    }


    /**
     * @param predicateInfo If representing a {@link NameValue}, an inverse form of the {@link NameValue} is equivalent
     *                      to the non-inverse form.
     * @return iteration order is from most to least important.
     */
    public @NotNull SafeLinkedHashSet<String> getProcessedPhrasesAssociatedWithPredicateInfo(
            PredicateInfo predicateInfo ){

        SafeLinkedHashSet<String> result = predicateInfoToAssociatedProcessedPhrases.safeGet(
                predicateInfo.getCanonicalForm());
        return result == null ? new SafeLinkedHashSet<>() : result;
    }



    private static SafeSet<NameValue> getAllNameValuesInKb(GraphKbWithSempreTypes kb) {
        SafeSet<NameValue> result = new SafeHashSet<>();

        // relations:
        result.addAll(kb.getRelationToFirstArgToSecondArgs().keySet());

        // first args:
        result.addAll(kb.getRelationToFirstArgToSecondArgs().values().stream()
                .flatMap(firstArgToSecondArgs->firstArgToSecondArgs.keySet().stream())
                .collect(Collectors.toList()));

        // second args:
        result.addAll(kb.getRelationToSecondArgToFirstArgs().values().stream()
                .flatMap(secondArgToFirstArgs->secondArgToFirstArgs.keySet().stream())
                .filter(secondArg->secondArg instanceof NameValue)
                .map(secondArg->(NameValue)secondArg)
                .collect(Collectors.toList()));

        return result;
    }


    /**
     * The order of calls to this method doesn't matter.
     * @param nameValue the inverse form is equivalent to the non-inverse form.
     */
    private void addAssociationsForNameValue(InstructionKnowledgeGraph graph, NameValue nameValue,
                                             boolean useDescriptionPhrases) {

        nameValue = nameValue.getNonReversedNameValue();

        NameValuesManager.NameValueType nameValueType = graph.nameValuesManager.getNameValueType(nameValue);
        if (nameValueType == null)
            return;

        switch (nameValueType) {

            case NLI_METHOD_NAME:
                String friendlyId = graph.nameValuesManager.getNliMethodFriendlyId(nameValue);
                Method method = graph.initialState.getDomain().getFriendlyIdToMethodId().getExisting(friendlyId)
                        .getMethod();
                for (String description : NliDescriptionsUtils.generateDescriptionsForMethod(method, useDescriptionPhrases)) {
                    addAssociation(nameValue, description, false);
                }
                return;
                

            case ENUM:
                Enum<?> enumValue = graph.nameValuesManager.getEnumValue(nameValue);
                if (!enumValue.getClass().equals(BooleanEnum.class)) {
                    for (String description : NliDescriptionsUtils.generateDescriptionsForEnumValue(enumValue)) {
                        addAssociation(nameValue, description, false);
                    }
                }
                return;


            case FIELD_RELATION:
                Field field = graph.nameValuesManager.getRelationField(nameValue);
                for (String description : NliDescriptionsUtils.generateDescriptionsForRelationField(field, useDescriptionPhrases)) {
                    addAssociation(nameValue, description, false);
                }
                return;
                

            case NLI_ENTITY:
                return;
                

            case NLI_ENTITY_TYPE:
                Class<? extends NliEntity> type = graph.nameValuesManager.getNliEntityType(nameValue);
                for (String description : NliDescriptionsUtils.generateDescriptionsForNliEntityType(type, useDescriptionPhrases)) {
                    addAssociation(nameValue, description, false);
                }
                return;
                

            case NON_FIELD_RELATION:
                return;


            case STRING:
                String str = graph.nameValuesManager.getString(nameValue);
                addAssociation(nameValue, str, false);
                return;
        }

        throw new Error();
    }



    /**
     * @param predicateInfo The {@link PredicateInfo#getCanonicalForm()} of this argument is used here.
     * In case it's not a key in {@link #predicateInfoToAssociatedProcessedPhrases}, false is returned.
     */
    public boolean isPredicateProbablyDomainSpecific(PredicateInfo predicateInfo) {
        predicateInfo = predicateInfo.getCanonicalForm();
        return predicateInfoToAssociatedProcessedPhrases.safeContainsKey(predicateInfo) &&
                !domainIndependentPredicateInfos.safeContains(predicateInfo);
    }


    /**
     * The order of calls to this method with the same 'value' is important.
     * The order of calls to this method with different 'value' values isn't important (even not for determinism).
     * @param isValueDomainIndependent should be true if 'value' is probably domain independent. If this argument
     *                                 is different in different calls for the same 'value' (and instance), then
     *                                 we assume the value is domain independent.
     * @param value If it's a {@link NameValue}, an inverse form is equivalent to non-inverse form.
     */
    public void addAssociation(Value value, String phrase, boolean isValueDomainIndependent) {

        PredicateInfo predicateInfo = new PredicateInfo(nameValueManager, value).getCanonicalForm();
        String processedPhrase = processPhrase.apply(phrase);

        Maps.addToMapOfCollections(predicateInfoToAssociatedProcessedPhrases, predicateInfo, processedPhrase,
                SafeLinkedHashSet::new);

        Maps.addToMapOfCollections(processedPhraseToAssociatedPredicateInfos, processedPhrase, predicateInfo,
                SafeLinkedHashSet::new);

        if (isValueDomainIndependent)
            domainIndependentPredicateInfos.add(predicateInfo);

    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("processedPhraseToAssociatedPredicateInfos:\n");
        processedPhraseToAssociatedPredicateInfos.keySet().forEach(processedPhrase->{
            sb.append(processedPhrase).append("\n");
            processedPhraseToAssociatedPredicateInfos.getExisting(processedPhrase).forEach(predicateInfo ->
                sb.append("\t" + predicateInfo.toString() + "\n"));
        });

        sb.append("\n\npredicateInfoToAssociatedProcessedPhrases:\n");
        predicateInfoToAssociatedProcessedPhrases.keySet().forEach(predicateInfo->{
            sb.append(predicateInfo).append("\n");
            predicateInfoToAssociatedProcessedPhrases.getExisting(predicateInfo).forEach(processedPhrase ->
                    sb.append("\t" + processedPhrase + "\n"));
        });

        return sb.toString();
    }


}
