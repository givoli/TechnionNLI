package il.ac.technion.nlp.nli.core.dataset.construction;

import ofergivoli.olib.data_structures.map.SafeHashMap;
import ofergivoli.olib.data_structures.map.SafeMap;
import ofergivoli.olib.data_structures.set.SafeSet;
import ofergivoli.olib.io.binary_files.PngFileInMemo;
import il.ac.technion.nlp.nli.core.dataset.Domain;
import il.ac.technion.nlp.nli.core.dataset.visualization.InitialAndDesiredStatesFigure;
import il.ac.technion.nlp.nli.core.method_call.MethodCall;
import il.ac.technion.nlp.nli.core.method_call.NonPrimitiveArgument;
import il.ac.technion.nlp.nli.core.state.NliEntity;
import il.ac.technion.nlp.nli.core.state.PrimitiveEntity;
import il.ac.technion.nlp.nli.core.state.State;
import il.ac.technion.nlp.nli.core.state.knowledgebase.KBTriple;
import il.ac.technion.nlp.nli.core.dataset.construction.hit_random_generation.HitConstructionInfo;
import il.ac.technion.nlp.nli.core.dataset.visualization.StateVisualizer;
import il.ac.technion.nlp.nli.core.dataset.visualization.html.HtmlString;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Represents a HIT - Human Intelligence Tasks for MTurk workers (not including the result from the worker).
 *
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class Hit implements Serializable {

    private static final long serialVersionUID = -6816099500088780750L;


    /**
     * See doc of constructor.
     */
    private final String id;
    private final Domain domain;
    /**
     * See doc of constructor.
     */
    private final State initialState;
    /**
     * See doc of constructor.
     */
    private final State destinationState;
    private final MethodCall methodCall;

    private final HitConstructionInfo hitConstructionInfo;


    /**
     * @param id               Used for equals() and hashCode().
     * @param initialState     The state on which {@link #methodCall} acts upon.
     * @param destinationState The state which is the result of executing methodCall on initialState.
     */
    public Hit(String id, Domain domain, State initialState, State destinationState, MethodCall methodCall,
               StateVisualizer initialStateVisualizer, StateVisualizer desiredStateVisualizer, ZonedDateTime creationTime) {

        this.id = id;
        this.domain = domain;
        this.initialState = initialState;
        this.destinationState = destinationState;
        this.methodCall = methodCall;
        this.initialStateVisualizer = initialStateVisualizer;
        this.desiredStateVisualizer = desiredStateVisualizer;
        hitConstructionInfo = new HitConstructionInfo(containsArgQueryableByComparisonOperation(),
                containsArgQueryableBySuperlativeOperation(), isQueryableByPrimitiveRelations(),
                methodCall.getMethodId(), creationTime);
    }

    /**
     * id (UUID) is randomly generated (but begins with 'idPrefix')
     */
    public Hit(Domain domain, State initialState, State destinationState, MethodCall methodCall,
               StateVisualizer initialStateVisualizer, StateVisualizer desiredStateVisualizer, String idPrefix,
               ZonedDateTime creationTime) {

        this(idPrefix + "__" + UUID.randomUUID().toString(),
                domain, initialState, destinationState, methodCall, initialStateVisualizer, desiredStateVisualizer,
                creationTime);
    }


    public State getDestinationState() {
        return destinationState;
    }

    /**
     * based on {@link #id}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        @SuppressWarnings("rawtypes")
        Hit other = (Hit) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }

    /**
     * based on {@link #id}
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    public String getId() {
        return id;
    }

    public State getInitialState() {
        return initialState;
    }

    public HitConstructionInfo getHitConstructionInfo() {
        return hitConstructionInfo;
    }

    public MethodCall getMethodCall() {
        return methodCall;
    }


    /**
     * A visualizer used to generate an {@link InitialAndDesiredStatesFigure}.
     */
    private final StateVisualizer initialStateVisualizer;


    /**
     * A visualizer used to generate an {@link InitialAndDesiredStatesFigure}.
     */
    private final StateVisualizer desiredStateVisualizer;


    public Domain getDomain() {
        return domain;
    }



    public PngFileInMemo createInitialAndDesiredStatesFigure(HitConstructionSettings hitConstructionSettings) {

        HtmlString initialStateVis = initialStateVisualizer.getVisualRepresentation(initialState);
        HtmlString desiredStateVis = desiredStateVisualizer.getVisualRepresentation(destinationState);

        return new InitialAndDesiredStatesFigure(initialStateVis, desiredStateVis).createPngImage(
                hitConstructionSettings);
    }


    /**
     * A HIT is "queryable by primitive relations" if there exists a matching LF (i.e. one with matching denotation)
     * that uses only primitive relations (may contain union and intersection operations) in order to represent each
     * denoted argument.
     *
     * Note: a HIT with function call that has only primitive arguments is trivially "queryable by primitive
     * relations".
     *
     * Note: in domains where all the relations are primitive (meaning, the second entity is primitive), HITs for
     * which this method returns false for are unsolvable (not even by using negation on non-arg entities, because if
     * you can describe these non-arg entities with primitive relations than you can also use these relations directly
     * on the arg entities).
     */
    public boolean isQueryableByPrimitiveRelations() {

        /**
         * Theorem: a HIT is "queryable by primitive relations" iff:
         *      For each non-primitive argument arg of the function call: if entity e1 is in arg and e2 is not, then there exists some
         *      relation R and entity x s.t. either (e1,R,x) or (e2,R,x) are in the KB, but not both. (otherwise, we say
         *      that e1 "can't be described" by primitive relations because of the "interfering" entity e2).
         * Proof (in short): If a HIT is "queryable by primitive relations" then there exists some LF as described which
         * proves the existence of such R and X for every such e1 and e2.
         * The other direction: If for each e1 in arg and e2 not in arg, some R and x exist as described above, then we can
         * construct a LF that is a union over multiple components, each denoting a single entity in arg. Each component
         * will be composed of intersections (and 'not' operators) that would "remove" any interfering e2 (which we know
         * is possible thanks to the R and x which exist for those e1 and e2).
         */

        Collection<KBTriple> primitiveTriples = initialState.getGraphKb().getTriples().stream()
                .filter(triple -> triple.secondEntity instanceof PrimitiveEntity)
                .collect(Collectors.toList());

        Set<String> idsOfNonPrimitiveEntitiesInPrimitiveRelations = primitiveTriples.stream()
                .map(triple -> initialState.getEntityId(triple.firstEntity))
                .collect(Collectors.toSet());

        return methodCall.getArguments().stream()
                .filter(arg->arg instanceof NonPrimitiveArgument)
                .noneMatch(arg ->
                aux_someEntityInterfereWithDescribingAnEntityInArg((NonPrimitiveArgument)arg, primitiveTriples,
                        idsOfNonPrimitiveEntitiesInPrimitiveRelations));
    }


    /**
     * Auxiliary method for {@link #isQueryableByPrimitiveRelations()}. Deals with only a single argument of the
     * function call.
     * @param primitiveTriplesInInitialState a primitive triple is one in which the second entity is primitive.
     * @return true iff there exists e1 in entityIdsInArg, and e2 not in entityIdsInArg, s.t. e1 "can't be described"
     * by primitive relations because of the "interfering" entity e2 (see: {@link #isQueryableByPrimitiveRelations()})*/
    private boolean aux_someEntityInterfereWithDescribingAnEntityInArg(
            NonPrimitiveArgument arg, Collection<KBTriple> primitiveTriplesInInitialState,
            Set<String> idsOfFirstEntitiesInPrimitiveTriplesInInitialState) {

        SafeSet<String> entityIdsInArg = arg.getNonPrimitiveEntityIds();

        return entityIdsInArg.stream().anyMatch(e1_id ->

                idsOfFirstEntitiesInPrimitiveTriplesInInitialState.stream()
                .filter(id -> !entityIdsInArg.safeContains(id))
                .anyMatch(e2_id -> {

                    NliEntity e2 = initialState.getEntityById(e2_id);
                    // We return true iff e2 is "interfering".
                    // So we need to check whether for every primitive relation R and primitive entity x:
                    //     R(e1,x) iff R(e2,x)

                    Set<ImmutablePair<Field, PrimitiveEntity>> fieldEntityPairsFor_e1 =
                            filterTriplesByFirstEntityAndMapToRelationSecondEntityPairs(
                                    primitiveTriplesInInitialState, e1_id);

                    Set<ImmutablePair<Field, PrimitiveEntity>> fieldEntityPairsFor_e2 =
                            filterTriplesByFirstEntityAndMapToRelationSecondEntityPairs(
                                    primitiveTriplesInInitialState, e2_id);

                    return fieldEntityPairsFor_e1.equals(fieldEntityPairsFor_e2);

                }));
    }

    /**
     * @param primitiveTriples the second entity must be a {@link PrimitiveEntity}.
     * @param firstEntityId to filter by.
     */
    private Set<ImmutablePair<Field, PrimitiveEntity>> filterTriplesByFirstEntityAndMapToRelationSecondEntityPairs(
            Collection<KBTriple> primitiveTriples, String firstEntityId) {
        return primitiveTriples.stream()
                .filter(triple -> initialState.getEntityId(triple.firstEntity).equals(firstEntityId))
                .map(triple -> new ImmutablePair<>(triple.relation, (PrimitiveEntity)triple.secondEntity))
                .collect(Collectors.toSet());
    }



    /**
     * A HIT's function call contains an argument which is "queryable by comparison operation" if its a non-primitive
     * argument and:
     * 1. There exists a matching LF that uses a comparison operator (and nothing else) to query for that
     * argument. The relation used by the LF must have at least one triple with a non-primitive entity not in the arg.
     * 2. The denotation for that argument is a set of more than one entity.
     */
    public boolean containsArgQueryableByComparisonOperation() {

        // we don't need the value of the output param.
        return containsArgQueryableByComparisonOperation(2, false);
    }

    /**
     * See {@link #containsArgQueryableByComparisonOperation()}
     * @param minEntitiesNumInArg the minimum number of entities an arg must contain (otherwise that arg won't be considered).
     * @param requireAllRelationScalarValuesInArgToBeEqual if true, the condition changes from "queryable by comparison"
     *                                                     to "queryable by superlative".
     */
    private boolean containsArgQueryableByComparisonOperation(int minEntitiesNumInArg,
                                                              boolean requireAllRelationScalarValuesInArgToBeEqual) {
        // contains all the triples in which the relation is scalar.
        SafeMap<Field,Collection<Pair<NliEntity,PrimitiveEntity>>> relationToNLIEntityScalarValuePairs = new SafeHashMap<>();
        // filling relationToNLIEntityScalarValuePairs:
        initialState.getGraphKb().getTriples().stream()
        .filter(triple -> triple.secondEntity instanceof PrimitiveEntity)
        .filter(triple -> ((PrimitiveEntity)triple.secondEntity).isScalarEntityType())
        .forEach(triple -> {
            ImmutablePair<NliEntity, PrimitiveEntity> pair = new ImmutablePair<>(triple.firstEntity,
                    (PrimitiveEntity) triple.secondEntity);
            if (!relationToNLIEntityScalarValuePairs.safeContainsKey(triple.relation))
                relationToNLIEntityScalarValuePairs.put(triple.relation, new LinkedList<>());
            relationToNLIEntityScalarValuePairs.safeGet(triple.relation).add(pair);
        });


        Collection<Field> relations = relationToNLIEntityScalarValuePairs.keySet();
        return methodCall.getArguments().stream()
                .filter(arg->arg instanceof NonPrimitiveArgument)
                .anyMatch(arg-> relations.stream().anyMatch(relation->
                        aux_isArgQueryableByComparisonOperationOnRelation((NonPrimitiveArgument) arg, minEntitiesNumInArg,
                                relation, relationToNLIEntityScalarValuePairs, requireAllRelationScalarValuesInArgToBeEqual)));
    }


    private class Range {
        /**
         * must be scalar type.
         */
        public PrimitiveEntity start;
        /**
         * must be salary type (identical to the type of {@link #start}).
         */
        public PrimitiveEntity end;
    }

    /**
     * Auxiliary method for {@link #containsArgQueryableByComparisonOperation()}.
     * @param minEntitiesNumInArg the minimum number of entities an arg must contain (otherwise that arg won't be considered).
     * @param requireAllRelationScalarValuesInArgToBeEqual if true, the condition changes from "queryable by comparison"
     *                                                     to "queryable by superlative".
     * @return true iff 'relation' allows to query for arg by a comparison operator.
     */
    private boolean aux_isArgQueryableByComparisonOperationOnRelation(
            NonPrimitiveArgument arg, int minEntitiesNumInArg, Field relation,
            SafeMap<Field, Collection<Pair<NliEntity, PrimitiveEntity>>> relationToNLIEntityScalarValuePairs,
            boolean requireAllRelationScalarValuesInArgToBeEqual) {

        assert(minEntitiesNumInArg>0);

        SafeSet<String> argEntityIds = arg.getNonPrimitiveEntityIds();
        if (argEntityIds.size() < minEntitiesNumInArg)
            return false;

        boolean allArgEntitiesHaveTripleOfThisRelation = relationToNLIEntityScalarValuePairs.safeGet(relation).stream()
                .map(pair -> initialState.getEntityId(pair.getLeft()))
                .filter(argEntityIds::safeContains)
                .collect(Collectors.toSet())
                .size() == argEntityIds.size();
        if (!allArgEntitiesHaveTripleOfThisRelation)
            return false;

        // the min and max scalars among all triples of 'relation' that their first-entity is in arg.
        PrimitiveEntity min = null, max = null;
        // finding min and max:
        for (String id : argEntityIds) {
            for (Pair<NliEntity, PrimitiveEntity> pair : relationToNLIEntityScalarValuePairs.safeGet(relation)) {
                String firstEntityId = initialState.getEntityId(pair.getLeft());
                if (id.equals(firstEntityId)) {
                    PrimitiveEntity secondEntity = pair.getRight();
                    if (min == null || min.isGreaterThan(secondEntity)) {
                        min = secondEntity;
                    }
                    if (max == null || max.isSmallerThan(secondEntity)) {
                        max = secondEntity;
                    }
                }
            }
        }

        //noinspection ConstantConditions
        if (requireAllRelationScalarValuesInArgToBeEqual && !min.equals(max))
            return false;

        Set<PrimitiveEntity> scalarsOfEntitiesNotInArg =
                relationToNLIEntityScalarValuePairs.safeGet(relation).stream()
                        .filter(pair -> !argEntityIds.safeContains(initialState.getEntityId(pair.getLeft())))
                        .map(Pair::getRight)
                        .collect(Collectors.toSet());

        if (scalarsOfEntitiesNotInArg.isEmpty())
            return false;

        PrimitiveEntity finalMin = min, finalMax = max; // required only because Java can't handle non-effectively-final variables in lamda expressions.
        return  scalarsOfEntitiesNotInArg.stream().allMatch(scalar -> scalar.isSmallerThan(finalMin)) ||
                scalarsOfEntitiesNotInArg.stream().allMatch(scalar -> scalar.isGreaterThan(finalMax));

    }


    /**
     * A HIT's function call contains an argument which is "queryable by superlative operation" if its a non-primitive
     * argument and:
     * 1. There exists a matching LF that uses a superlative operator (and nothing else) to query for that
     * argument. The relation used by the LF must have at least one triple with a non-primitive entity not in the arg.
     * 2. The denotation for that argument is not empty (i.e. contains at least one entity).
     *
     * Examples for an utterance resulting from a HIT that returns true for this method:
     * "... the rooms of the top floor ...", "... my  last meeting ...".
     */
    public boolean containsArgQueryableBySuperlativeOperation() {
        return  containsArgQueryableByComparisonOperation(1, true);
    }


    public String extractBatchIdFromHitIdAssumingExpectedFormat() {
        Pattern p = Pattern.compile("^[a-zA-Z_]*___batch([0-9.]*)_");
        Matcher m = p.matcher(id);
        if (!m.find())
            throw new RuntimeException("id format is not as expected.");
        return m.group(1);
    }

    @Override
    public String toString() {
        return id;
    }
}
