package il.ac.technion.nlp.nli.core.reflection;

import com.google.common.base.Verify;
import com.ofergivoli.ojavalib.reflection.ReflectionUtils;
import il.ac.technion.nlp.nli.core.state.Entity;
import il.ac.technion.nlp.nli.core.state.NliEntity;
import il.ac.technion.nlp.nli.core.state.NliRootEntity;
import il.ac.technion.nlp.nli.core.state.PrimitiveEntity;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class EntityGraphReflection {


    /**
     * @return each (field,entity) pair returned matches an entity such that the relation (fromEntity,entity) is
     * represented by the relation 'field' which is a field of 'fromEntity'.
     * This methods performs setAccessible(true) on all returned fields.
     * Note that the returned list may contain multiple entries for the same field.
     * @param deterministicOrder When true, the order of the returned list is deterministic (might cost in computation time -
     *                      sorting primitive entities if necessary).
     *                      When true, all non-primitive collection relation fields must be a subtype of {@link List}
     *                      (an exception is thrown otherwise).
     * If a relation field is a {@link List}, the order of the related elements in the returned list match the order of
     * the list field.
     */
    public static List<Pair<Field, Entity>> getEntitiesInOutgoingRelationsFromGivenEntity(NliEntity fromEntity,
                                                                                          boolean deterministicOrder) {

        List<Pair<Field, Entity>> fieldEntityPairs = new LinkedList<>();

        getRelationFieldsOfNliEntityClass(fromEntity.getClass(),true).forEach(field->
                getEntitiesInOutgoingRelationFromGivenEntity(fromEntity, deterministicOrder, field).forEach(entity->
                        fieldEntityPairs.add(new ImmutablePair<>(field, entity))));

        return fieldEntityPairs;
    }

    /**
     * @return the returned list contains entity x iff (fromEntity,x) is represented by the 'relation' which is a field
     * of 'fromEntity'.
     * This methods performs setAccessible(true) on 'relation'.
     * @param deterministicOrder When true, the order of the returned list is deterministic (might cost additional
     *                           computation time - sorting primitive entities if necessary).
     *                           When true, if 'relation' is a non-primitive collection relation field, it must be a
     *                           subtype of {@link List} (an exception is thrown otherwise).
     * If a relation field is a {@link List}, the order of the related elements in the returned list match the order of
     * the list field.
     */
    public static List<Entity> getEntitiesInOutgoingRelationFromGivenEntity(
            NliEntity fromEntity, boolean deterministicOrder, Field relation) {

        List<Entity> result = new LinkedList<>();
        Object fieldValue;
        try {
            fieldValue = relation.get(fromEntity);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        if (fieldValue == null)
            return new LinkedList<>();

        Collection<?> entities;
        if (GeneralReflection.isUserEntityTypeCollection(relation.getGenericType())) {

            if (deterministicOrder) {
                if (List.class.isAssignableFrom(relation.getType())) {
                    entities = (Collection<?>) fieldValue;
                } else {
                    // we need to sort the entities in some deterministic way.
                    Class<?> entityType = GeneralReflection.getUserTypeOfUserTypeCollection(
                            relation.getType());
                    if (!PrimitiveEntity.isPrimitiveEntityType(entityType)) {
                        throw new RuntimeException("Detected an non-primitive entity collection field which is not a list. Can't order the non-primitive entities in a deterministic way. Field: " + relation);
                    } else {
                        // we sort the primitive entities in a deterministic way:
                        entities = ((Collection<?>) fieldValue).stream()
                                .map(PrimitiveEntity::new)
                                .sorted(Comparator.comparing(PrimitiveEntity::getUniqueDeterministicStringDefiningEntity))
                                .map(PrimitiveEntity::getValue)
                                .collect(Collectors.toList());
                    }
                }
            } else {
                // deterministic order is not required.
                entities = (Collection<?>) fieldValue;
            }
        } else { // the field is not a collection
            assert GeneralReflection.isUserEntityType(fieldValue.getClass());
            entities = Collections.singletonList(fieldValue);
        }

        entities.forEach(value -> {
            Verify.verify(value != null);
            result.add(createEntityFromUserEntityValue(value));
        });

        return result;
    }

    private static Entity createEntityFromUserEntityValue(Object value) {
        return value instanceof NliEntity ? (NliEntity) value : new PrimitiveEntity(value);
    }

    /**
     * @param relationField Can be any field of a {@link NliEntity} (even one which is not a relation field).
     * @return null if 'relationField' is not a relation field. Otherwise the type of the second entity of the relation.
     */
    private static
    @Nullable Class<?> getClassOfSecondEntityOfRelation(Field relationField) {
        Verify.verify(NliEntity.class.isAssignableFrom(relationField.getDeclaringClass()));
        return GeneralReflection.getUserType(relationField.getGenericType());
    }

    public static boolean isRelationField(Field field) {
        return GeneralReflection.isUserEntityTypeOrCollectionThereof(field.getGenericType());
    }


    /**
     * @param deterministic when true the returned list is sorted according to some deterministic order.
     */
    public static List<Field> getRelationFieldsOfNliEntityClass(Class<? extends NliEntity> clazz, boolean deterministic)
    {
        Stream<Field> stream = ReflectionUtils.getAllFieldsOfClass(clazz, false, true).stream()
                .filter(EntityGraphReflection::isRelationField);
        if (deterministic)
            stream = stream.sorted(Comparator.comparing(GeneralReflection::getUniqueDeterministicStringForField));
        return stream.collect(Collectors.toList());
    }


    /**
     * This methods performs setAccessible(true) on all returned fields.
     * @param deterministicOrder see {@link #getEntitiesInOutgoingRelationsFromGivenEntity(NliEntity, boolean)}
     * @return each pair (field,primitive) returned matches a primitive such that the relation (fromEntity, primitive)
     * is represented by 'field' which is a field of 'fromEntity'.
     * Note that the returned list may contain multiple entries for the same field.
     * If a relevant relation field is a {@link List}, the order of the related elements in the returned list match
     * the order of the elements of the field list.
     */
    public static List<Pair<Field, PrimitiveEntity>> getPrimitiveEntitiesInOutgoingRelationFromGivenEntity(
            NliEntity fromEntity, boolean deterministicOrder) {

        List <Pair<Field, PrimitiveEntity>> result = new LinkedList<>();

        getEntitiesInOutgoingRelationsFromGivenEntity(fromEntity, deterministicOrder).stream()
                .filter(fieldEntityPair-> fieldEntityPair.getRight() instanceof PrimitiveEntity)
                .forEach(fieldEntityPair->result.add(new ImmutablePair<>(
                        fieldEntityPair.getLeft(),(PrimitiveEntity)fieldEntityPair.getRight())));

        return result;
    }

    /**
     * @param deterministicOrder see {@link #getEntitiesInOutgoingRelationsFromGivenEntity(NliEntity, boolean)}
     * @return each pair (field,toEntity) returned matches an entity such that the relation (fromEntity, toEntry) is
     * represented by 'field' which is a field of 'fromEntity'.
     *  This methods performs setAccessible(true) on all returned fields.
     * Note that the returned list may contain multiple entries for the same field.
     * If a relevant relation field is a {@link List}, the order of the related elements in the returned list match
     * the order of the elements of the field list.
     */
    public static List <Pair<Field, NliEntity>> getNonPrimitiveEntitiesInOutgoingRelationFromGivenEntity(
            NliEntity fromEntity, boolean deterministicOrder) {

        return getEntitiesInOutgoingRelationsFromGivenEntity(fromEntity, deterministicOrder).stream()
            .filter(fieldEntityPair->fieldEntityPair.getRight() instanceof NliEntity)
            .map(fieldEntityPair->new ImmutablePair<>(
                    fieldEntityPair.getLeft(), (NliEntity)fieldEntityPair.getRight()))
            .collect(Collectors.toList());
    }

    /**
     * @return all reachable entities (including the root) in depth-first search pre-order.
     * @param includeRootEntity
     * @param deterministicOrder see {@link #getEntitiesInOutgoingRelationsFromGivenEntity(NliEntity, boolean)}
     */
    public static List<NliEntity> getNonPrimitiveEntitiesReachableFromRoot(
            NliRootEntity root, boolean includeRootEntity, boolean deterministicOrder) {

        List<NliEntity> result = new LinkedList<>();
        Queue<NliEntity> pending = new LinkedList<>();
        // only the keys of this map are used (i.e. used as a map):
        IdentityHashMap<NliEntity,Object> visitedEntities = new IdentityHashMap<>();
        pending.add(root);
        while (!pending.isEmpty()) {
            NliEntity current = pending.remove();
            if (visitedEntities.containsKey(current))
                continue;
            result.add(current);
            visitedEntities.put(current, null);
            EntityGraphReflection.getNonPrimitiveEntitiesInOutgoingRelationFromGivenEntity(current, deterministicOrder)
                    .forEach(pair->pending.add(pair.getRight()));
        }
        if (!includeRootEntity)
            result.remove(root);
        return result;
    }

    /**
     * @param includeRootInResult note that we add the root to the result list (regardless of 'includeRootInResult') if
     *                            it is reachable by a non-empty path from root (due to the entity graph being cyclic).
     * @param deterministic when true the returned list is sorted according to some deterministic order.
     * @return all the {@link NliEntity} classes that may be reachable from an entity of type 'rootEntityClass'.
     * TODO: test
     */
    public static Collection<Class<? extends NliEntity>> getPossiblyReachableNliEntityClasses(
            Class<? extends NliRootEntity> rootEntityClass, boolean includeRootInResult, boolean deterministic) {

        Collection<Class<? extends NliEntity>> result = new LinkedList<>();

        Set<Class<? extends NliEntity>> alreadySeen = Collections.newSetFromMap(new IdentityHashMap<>());
        Stack<Class<? extends NliEntity>> pending = new Stack<>();

        // classic DFS.
        pending.push(rootEntityClass);
        while (!pending.empty()) {
            Class<? extends NliEntity> current = pending.pop();
            if (alreadySeen.contains(current))
                continue;
            alreadySeen.add(current);

            if (! (result.isEmpty() && !includeRootInResult && rootEntityClass.equals(current)))
                result.add(current);

            // Adding to 'pending' all relevant Classes considering the fields of 'current'.
            //noinspection unchecked
            getRelationFieldsOfNliEntityClass(current, deterministic).stream()
                    .map(EntityGraphReflection::getClassOfSecondEntityOfRelation)
                    .filter(NliEntity.class::isAssignableFrom)
                    .forEach(clazz -> pending.push((Class<? extends NliEntity>)clazz));

        }

        return result;
    }


    public static String entityGraphToHumanFriendlyString(NliRootEntity root, boolean deterministic){
        StringBuilder sb = new StringBuilder();
        getNonPrimitiveEntitiesReachableFromRoot(root, true, deterministic).forEach(e->{
            sb.append(e.getClass().getSimpleName()).append("\t").append(System.identityHashCode(e)).append(":\n");
            getPrimitiveEntitiesInOutgoingRelationFromGivenEntity(e,deterministic).forEach(fieldEntityPair->
                    sb.append("\t").append(fieldEntityPair.getLeft().getName()).append(":\t")
                            .append(fieldEntityPair.getRight()).append("\n"));
        });
        return sb.toString();
    }

    /**
     * The order of entities in collection fields matter (think of the edges of the graph as being tagged with the
     * index).
     * If there's a collection field of non-primitive entities, and the collection is not a list - an exception is
     * thrown (because we have no tractable way of checking equality).
     */
    public static boolean entityGraphEquals(NliRootEntity root1, NliRootEntity root2) {

        /**
         * Maps a non-primitive entity in the entity tree of 'root1' to the non-primitive entity it is equal to in the
         * tree on 'root2'.
         */
        IdentityHashMap<NliEntity, NliEntity> entityInTree1ToEntityInTree2 = new IdentityHashMap<>();
        ArrayList<NliEntity> nonPrimitiveEntitiesInTree1 = new ArrayList<>(
                getNonPrimitiveEntitiesReachableFromRoot(root1, true, true));
        ArrayList<NliEntity> nonPrimitiveEntitiesInTree2 = new ArrayList<>(
                getNonPrimitiveEntitiesReachableFromRoot(root2, true, true));
        if (nonPrimitiveEntitiesInTree1.size() != nonPrimitiveEntitiesInTree2.size())
            return false;
        for (int i=0; i<nonPrimitiveEntitiesInTree1.size(); i++) {
            entityInTree1ToEntityInTree2.put(nonPrimitiveEntitiesInTree1.get(i),
                    nonPrimitiveEntitiesInTree2.get(i));
        }


        for (NliEntity entityOnTree1 : nonPrimitiveEntitiesInTree1){

            // checking primitive entities in relation fields:
            NliEntity entityOnTree2 = entityInTree1ToEntityInTree2.get(entityOnTree1);
            if (!getPrimitiveEntitiesInOutgoingRelationFromGivenEntity(entityOnTree1,true).equals(
                    getPrimitiveEntitiesInOutgoingRelationFromGivenEntity(entityOnTree2,true))) {
                // the two non-primitive entities don't have the same primitive relation fields' type/name/value.
                return false;
            }

            // checking non-primitive entities in relation fields:
            ArrayList<Pair<Field, NliEntity>> actualPairsInTree2 =
                    new ArrayList<>(getNonPrimitiveEntitiesInOutgoingRelationFromGivenEntity(entityOnTree2,true));
            ArrayList<Pair<Field, NliEntity>> expectedPairsInTree2 =
                getNonPrimitiveEntitiesInOutgoingRelationFromGivenEntity(entityOnTree1,true).stream()
                .<Pair<Field, NliEntity>>map(fieldEntity->new ImmutablePair<>(fieldEntity.getLeft(),
                        entityInTree1ToEntityInTree2.get(fieldEntity.getRight())))
                .collect(Collectors.toCollection(ArrayList::new));
            if (actualPairsInTree2.size() != expectedPairsInTree2.size())
                return false;
            for (int i=0; i<actualPairsInTree2.size(); i++){
                // checking that the relation fields are the same field:
                if (!actualPairsInTree2.get(i).getLeft().equals(expectedPairsInTree2.get(i).getLeft()))
                    return false;
                // checking that the relation field values are the same object:
                if (actualPairsInTree2.get(i).getRight() != expectedPairsInTree2.get(i).getRight())
                    return false;
            }
        }
        return true;
    }

}
