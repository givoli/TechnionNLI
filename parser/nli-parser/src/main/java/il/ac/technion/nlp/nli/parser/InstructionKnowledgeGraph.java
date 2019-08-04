package il.ac.technion.nlp.nli.parser;

import com.google.common.base.Verify;
import com.ofergivoli.ojavalib.reflection.ReflectionUtils;
import edu.stanford.nlp.sempre.*;
import edu.stanford.nlp.sempre.tables.TableKnowledgeGraph;
import edu.stanford.nlp.sempre.tables.TableTypeSystem;
import edu.stanford.nlp.sempre.tables.lambdadcs.LambdaDCSException;
import fig.basic.LispTree;
import fig.basic.Pair;
import il.ac.technion.nlp.nli.core.reflection.EntityGraphReflection;
import il.ac.technion.nlp.nli.core.reflection.GeneralReflection;
import il.ac.technion.nlp.nli.core.state.*;
import il.ac.technion.nlp.nli.core.state.knowledgebase.GraphKb;
import il.ac.technion.nlp.nli.core.state.knowledgebase.KBTriple;
import il.ac.technion.nlp.nli.parser.kb.GraphKbWithSempreTypes;
import org.apache.commons.lang3.ClassUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Stream;

/**
 * A {@link GraphKb} wrapper with required logic for integration into Sempre.
 *
 * Unless specified otherwise, when referring to a {@link NameValue} id we mean one without a preceding "!"
 * (that indicates a reversed relation). When we want to refer to ids that may contain a preceding "!" we use
 * the term "reversible id".
 *
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class InstructionKnowledgeGraph extends KnowledgeGraph {

    public final State initialState;
    public final NameValuesManager nameValuesManager = new NameValuesManager();
    public final GraphKbWithSempreTypes kb = new GraphKbWithSempreTypes();

    private final boolean deterministic;



    /**
     * First arg represents an {@link NliEntity}, second arg represents its type.
     */
    public final NameValue NLI_ENTITY_TYPE_RELATION_NV;
    /**
     * First arg represents an {@link NliEntity}, second arg represents the next {@link NliEntity} in a list of the
     * entities of that type in the {@link NliRootEntity}.
     */
    public final NameValue NEXT_RELATION_NV;
    /**
     * First arg represents an {@link NliEntity}, second arg represents the index of that entity in a list of the
     * entities of that type in the {@link NliRootEntity}.
     */
    public final NameValue INDEX_RELATION_NV;


    /**
     * @param deterministic when true the functionality of this object is deterministic.
     */
    public InstructionKnowledgeGraph(State initialState, boolean deterministic) {
        this.deterministic = deterministic;
        this.initialState = initialState;


        // Creating NameValue objects for non-field relations:
        NLI_ENTITY_TYPE_RELATION_NV = nameValuesManager.createNameValueRepresentingNonFieldRelation(
                CanonicalNames.TYPE, true); // the chosen id here is important for Sempre's type inference logic.
        NEXT_RELATION_NV = nameValuesManager.createNameValueRepresentingNonFieldRelation(
                TableTypeSystem.ROW_NEXT_VALUE.id, true); // the chosen id here is important for Sempre's pruning logic.
        INDEX_RELATION_NV = nameValuesManager.createNameValueRepresentingNonFieldRelation(
                TableTypeSystem.ROW_INDEX_VALUE.id, true);


        createFactsFromState();
    }


    private void createFactsFromState(){
        initialState.getGraphKb().getTriples().forEach(this::addFactFromTriple);
        addFactsWithNliEntityTypeRelation();
        addFactsAboutOrderOfListFields();
        kb.verifyValidity(nameValuesManager);
    }

    private void addFactsWithNliEntityTypeRelation() {
        EntityGraphReflection.getNonPrimitiveEntitiesReachableFromRoot(initialState.getRootEntity(), false,
                deterministic).forEach(entity ->
                        kb.addFact(NLI_ENTITY_TYPE_RELATION_NV,
                                nameValuesManager.createNameValueRepresentingNliEntity(initialState, entity),
                                nameValuesManager.createNameValueRepresentingNliEntityType(entity.getClass())));
    }

    private void addFactFromTriple(KBTriple triple) {
        NameValue relation = nameValuesManager.createNameValueRepresentingRelationField(triple.relation);
        NameValue firstArg = nameValuesManager.createNameValueRepresentingNliEntity(initialState, triple.firstEntity);
        createSempreValuesFromEntity(triple.secondEntity).forEach(secondArg->
            kb.addFact(relation, firstArg, secondArg));
    }

    /**
     *  @return a list with at least one element (in most cases, only one element - see
     *  {@link #createSempreValuesFromPrimitiveEntity}).
     */
    public List<Value> createSempreValuesFromEntity(Entity entity) {
        return entity instanceof PrimitiveEntity ?
                createSempreValuesFromPrimitiveEntity((PrimitiveEntity) entity) :
                Collections.singletonList(nameValuesManager.createNameValueRepresentingNliEntity(initialState,
                        (NliEntity) entity));
    }

    /**
     * @param entity If this is a {@link ZonedDateTime}, two values are returned - a {@link DateValue} and a
     *               {@link TimeValue}, because the parser does not support a single type representing a full date-time
     *               value.
     *               For any other argument type, a single element is returned.
     */
    private @NotNull List<Value> createSempreValuesFromPrimitiveEntity(PrimitiveEntity entity) {

        if (entity.getValue() == null)
            throw new RuntimeException("primitive value null not supported");

        List<Value>  result = new LinkedList<>();
        
        if (ClassUtils.isAssignable(entity.getValue().getClass(), Integer.class)){
            result.add(new NumberValue((int)entity.getValue()));
        } else if (ClassUtils.isAssignable(entity.getValue().getClass(), Double.class)){
            result.add(new NumberValue((double)entity.getValue()));
        } else if (ClassUtils.isAssignable(entity.getValue().getClass(), Boolean.class)){
            result.add(nameValuesManager.createNameValueRepresentingBoolean((Boolean)entity.getValue()));
        } else if (ClassUtils.isAssignable(entity.getValue().getClass(), ZonedDateTime.class)){
            ZonedDateTime time = (ZonedDateTime)entity.getValue();
            result.add(new TimeValue(time.getHour(), time.getMinute()));
            result.add(new DateValue(time.getYear(), time.getMonthValue() ,time.getDayOfMonth()));
        } else if (ClassUtils.isAssignable(entity.getValue().getClass(), String.class)){
            result.add(nameValuesManager.createNameValueRepresentingString((String)entity.getValue()));
        } else if (ClassUtils.isAssignable(entity.getValue().getClass(), Enum.class)){
            result.add(nameValuesManager.createNameValueRepresentingEnumValue((Enum<?>)entity.getValue()));
        } else {
            throw new Error("Unexpected primitive entity type.");
        }
        return result;
    }

    /**
     * @return null in case 'sempreValue' does not represent a {@link PrimitiveEntity} of type
     * 'primitiveEntityUserValueType'.
     */
    public @Nullable PrimitiveEntity createPrimitiveEntityFromSempreValue(Value sempreValue,
                                                                           Class<?> primitiveEntityUserValueType) {

        Verify.verify(PrimitiveEntity.isPrimitiveEntityType(primitiveEntityUserValueType));

        if (ClassUtils.isAssignable(primitiveEntityUserValueType, Integer.class)){
            if (!(sempreValue instanceof NumberValue))
                return null;
            return new PrimitiveEntity((int)((NumberValue) sempreValue).value);

        } else if (ClassUtils.isAssignable(primitiveEntityUserValueType, Double.class)){
            if (!(sempreValue instanceof NumberValue))
                return null;
            return new PrimitiveEntity(((NumberValue) sempreValue).value);

        } else if (ClassUtils.isAssignable(primitiveEntityUserValueType, Boolean.class)){
            if (!(sempreValue instanceof NameValue) ||
                    nameValuesManager.getNameValueType(((NameValue)sempreValue))
                            != NameValuesManager.NameValueType.ENUM)
                return null;
            Enum<?> enumValue = nameValuesManager.getEnumValue((NameValue) sempreValue);
            if (!enumValue.getClass().equals(BooleanEnum.class))
                return null;
            return new PrimitiveEntity(((BooleanEnum)enumValue).value);

        } else if (ClassUtils.isAssignable(primitiveEntityUserValueType, ZonedDateTime.class)){
            throw new RuntimeException("ZonedDateTime value creation is not supported");

        } else if (ClassUtils.isAssignable(primitiveEntityUserValueType, String.class)){

            if (sempreValue instanceof StringValue)
                return new PrimitiveEntity(((StringValue)sempreValue).value);

            if (sempreValue instanceof NameValue &&
                    nameValuesManager.getNameValueType(((NameValue)sempreValue)) ==
                            NameValuesManager.NameValueType.STRING)
                return new PrimitiveEntity(nameValuesManager.getString((NameValue) sempreValue));


            return null;

        } else if (ClassUtils.isAssignable(primitiveEntityUserValueType, Enum.class)){
            if (!(sempreValue instanceof NameValue) ||
                    nameValuesManager.getNameValueType(((NameValue)sempreValue))
                            != NameValuesManager.NameValueType.ENUM)
                return null;
            return new PrimitiveEntity(nameValuesManager.getEnumValue((NameValue)sempreValue));

        } else {
            throw new Error("Unexpected primitive entity type.");
        }
    }



    /**
     * Adds to the KB the facts (i.e. relation instances) with {@link #NEXT_RELATION_NV} and {@link #INDEX_RELATION_NV}.
     */
    private void addFactsAboutOrderOfListFields() {


        EntityGraphReflection.getRelationFieldsOfNliEntityClass(initialState.getRootEntity().getClass(),deterministic)
                                .stream()
                                .filter(relation-> {
                                    Class<?> userEntityType =
                                            GeneralReflection.getUserType(relation.getGenericType());
                                    assert userEntityType != null;
                                    return NliEntity.class.isAssignableFrom(userEntityType) &&
                                            GeneralReflection.isUserEntityTypeCollection(relation.getGenericType()) &&
                                            List.class.isAssignableFrom(relation.getType());
                                })
                                .forEach(relation->{
                                    @SuppressWarnings("unchecked")
                                    List<? extends NliEntity> list = (List<? extends NliEntity>)
                                            ReflectionUtils.getValueOfFieldSettingAccessible(relation,
                                                    initialState.getRootEntity());
                                    addFactsAboutOrderOfListFields(list);
                                });
    }

    /**
     * Adds to the KB the facts (i.e. relation instances) with {@link #NEXT_RELATION_NV} and {@link #INDEX_RELATION_NV}.
     * @param entityList should be a list of entities that appeared as a relation list field.
     */
    private void addFactsAboutOrderOfListFields(List<? extends NliEntity> entityList) {
        ArrayList<? extends NliEntity> array = new ArrayList<>(entityList);
        for (int i=0; i<array.size(); i++){
            NameValue entityNV = nameValuesManager.createNameValueRepresentingNliEntity(initialState, array.get(i));
            List<Value> sempreValues = createSempreValuesFromPrimitiveEntity(new PrimitiveEntity(i + 1));
            Verify.verify(sempreValues.size() == 1);
            kb.addFact(INDEX_RELATION_NV, entityNV, sempreValues.get(0));
            if (i<array.size()-1) {
                NameValue nextEntityNV = nameValuesManager.createNameValueRepresentingNliEntity(initialState, array.get(i+1));
                kb.addFact(NEXT_RELATION_NV, entityNV, nextEntityNV);
            }
        }
    }


    public State getInitialState(){
        return initialState;
    }


    @Override
    public LispTree toLispTree() {
        LispTree result = LispTree.proto.newList();
        result.addChild(this.getClass().getSimpleName());
        return result;
    }

    /**
     * Note: This method implementation was copied from ppasupat's {@link TableKnowledgeGraph}.
     */
    @Override
    public List<Value> joinFirst(Value r, Collection<Value> firsts) {
        return joinSecond(getReversedPredicate(r), firsts);
    }

    /**
     * Note: This method implementation was copied from ppasupat's {@link TableKnowledgeGraph}.
     */
    @Override
    public List<Value> joinSecond(Value r, Collection<Value> seconds) {
        List<Value> answer = new ArrayList<>();
        for (Pair<Value, Value> pair : filterSecond(r, seconds))
            answer.add(pair.getFirst());
        return answer;
    }

    /**
     * Note: This method implementation was copied from ppasupat's {@link TableKnowledgeGraph}.
     */
    @Override
    public List<fig.basic.Pair<Value, Value>> filterFirst(Value r, Collection<Value> firsts) {
        return getReversedPairs(filterSecond(getReversedPredicate(r), firsts));
    }


    @Override
    public List<Pair<Value, Value>> filterSecond(Value relation, Collection<Value> secondElementsOfPairs) {

        // Don't get confused between the terms first/second pair elements (which refers to the 'secondElementsOfPairs'
        // argument of this method, and also refers to the values that can serve as the second element in the returned
        // pairs); and the terms "first arg" and "second arg" which refer to the first and second arguments of a
        // relation.


        List<Pair<Value, Value>> answer = new ArrayList<>();

        if (!(relation instanceof NameValue))
            return answer;

        boolean isInverse = false;
        // we now set isInverse, and in case 'relation' has an inverse prefix ("!") we shall remove that prefix (so the
        // code that follows can assume 'relationNV' does not contain it)
        Value reversed = isReversedRelation(relation);
        if (reversed != null) {
            isInverse = true;
            relation = reversed;
        }

        NameValue relationNV = (NameValue) relation;

        Boolean isBinary = nameValuesManager.isNameValueRepresentBinaryRelation(relationNV);
        if (isBinary== null || !isBinary || !kb.getRelationToFirstArgToSecondArgs().safeContainsKey(relationNV))
            return answer;

        if (relation.equals(NEXT_RELATION_NV) && secondElementsOfPairs.size() != 1)
            throw new LambdaDCSException(LambdaDCSException.Type.nonSingletonList,
                    "Refusing to return an answer for the 'next' relation with multiple NLI entities");

        Stream<Value> secondElementsOfPairsToConsider;
        if (secondElementsOfPairs.size() == Integer.MAX_VALUE) {
            // 'secondElementsOfPairs' is an infinite collection.
            secondElementsOfPairsToConsider = kb.getRelationToSecondArgToFirstArgs().getExisting(relationNV).keySet()
                    .stream()
                    .filter(secondElementsOfPairs::contains);
        } else {
            // 'secondElementsOfPairs' is a finite collection.
            secondElementsOfPairsToConsider = secondElementsOfPairs.stream();
        }

        boolean finalIsInverse = isInverse;
        secondElementsOfPairsToConsider.forEach(secondElementsOfPair->
                kb.getRelationAsFunction(relationNV, finalIsInverse).apply(secondElementsOfPair)
                        .forEach(firstElement ->
                                answer.add(new Pair<>(firstElement, secondElementsOfPair))));

        return answer;
    }

    @Override
    public List<Formula> getFuzzyMatchedFormulas(String term, FuzzyMatchFn.FuzzyMatchFnMode mode) {
        throw new RuntimeException("method shouldn't be used");
    }
    @Override
    public List<Formula> getAllFormulas(FuzzyMatchFn.FuzzyMatchFnMode mode) {
        throw new RuntimeException("method shouldn't be used");
    }



}
