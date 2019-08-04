package il.ac.technion.nlp.nli.core.state;

import com.ofergivoli.ojavalib.data_structures.map.SafeHashMap;
import com.ofergivoli.ojavalib.data_structures.map.SafeIdentityHashMap;
import com.ofergivoli.ojavalib.data_structures.map.SafeMap;
import il.ac.technion.nlp.nli.core.dataset.Domain;
import il.ac.technion.nlp.nli.core.reflection.EntityGraphReflection;
import il.ac.technion.nlp.nli.core.state.knowledgebase.GraphKb;
import il.ac.technion.nlp.nli.core.state.knowledgebase.KBTriple;
import org.apache.commons.lang3.SerializationUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

/**
 * Represents an entity graph and {@link NliEntity}-id mapping.
 */
public class State implements Serializable {
	private static final long serialVersionUID = -4246020041717636274L;
	private Domain domain;
	private NliRootEntity rootEntity;
    /**
     * Used only by {@link #updateIdsOfNonPrimitiveEntities(boolean)})}.
     */
    private int nextIdToGive = 1;
    /**
     * For all non-primitive entities in the entity graph.
     */
	private SafeMap<String, NliEntity> entityIdToEntity;
    /**
     * For all non-primitive entities in the entity graph.
     */
    private SafeIdentityHashMap<NliEntity, String> entityToEntityId;
    /**
     * Generated first time on demand (and updated from that moment when entity graph changes).
     */
    transient @Nullable private GraphKb graphKb;

	/**
	 * This constructor generates ids for the non-primitive entities in the graph which its root is constantRootEntity.
     * @param constantRootEntity Must not change (otherwise the generated id-object mapping would be invalidated).
     * @param deterministic when true the functionality of this constructor is deterministic.
	 */
	public State(Domain domain, NliRootEntity constantRootEntity, boolean deterministic) {
		this.domain = domain;
		this.rootEntity = constantRootEntity;
		updateIdsOfNonPrimitiveEntities(deterministic);
	}

	/**
	 * @return A map mapping entity id to entity (contains all the entities in the state).
     */
	public NliRootEntity getRootEntity() {
		return rootEntity;
	}

    /**
     * Updates {@link #entityToEntityId} and {@link #entityIdToEntity}.
     * Can be called from constructor (when some two fields are still null).
     * If these two fields were already set, then non-primitive entities which still exist in the entity graph
     * remain with the same id. Non-primitive entities no longer on the entity graph
     * are removed from theses data structures, and new ones get a new id (never before given in this {@link State}).
     *
     * @param deterministic When true, the functionality of this method is deterministic.
     */
    private void updateIdsOfNonPrimitiveEntities(boolean deterministic) {

        // doing a classic BFS for from rootEntity.

        SafeIdentityHashMap<NliEntity, String> newEntityToId = new SafeIdentityHashMap<>();

        Queue<NliEntity> pending = new LinkedList<>(); // not yet traversed.
        pending.add(rootEntity);

        while (!pending.isEmpty()) {
            NliEntity current = pending.remove();

            if (newEntityToId.safeContainsKey(current)) continue;

            EntityGraphReflection.getNonPrimitiveEntitiesInOutgoingRelationFromGivenEntity(current, deterministic)
                    .forEach(pair-> pending.add(pair.getRight()));

            String newId;
            if (entityToEntityId!=null && entityToEntityId.safeContainsKey(current)) {
                // 'current' already has an id.
                newId = entityToEntityId.safeGet(current);
            } else {
                newId = Integer.toString(nextIdToGive);
                nextIdToGive++;
            }
            newEntityToId.put(current, newId);
        }

        entityToEntityId = newEntityToId;
        // updating entityIdToEntity:
        entityIdToEntity = new SafeHashMap<>();
        newEntityToId.forEach((entity,id)->entityIdToEntity.put(id,entity));

    }

    /**
     * This method must be called after doing anything that changes the entity graph (reachable from the root entity) of
     * this state.
     */
    public void updateStateFollowingEntityGraphModifications() {
        updateIdsOfNonPrimitiveEntities(true);
        if (graphKb != null)
            graphKb = generateGraphKB();
    }


    /**
     * @param otherState if null, false is returned.
	 * @return Returns true iff otherState is logically equivalent to this state. Note that the id assignment is not
     * relevant for this.
	 */
	public boolean entityGraphsEqual(@Nullable State otherState) {
        return otherState != null &&
                EntityGraphReflection.entityGraphEquals(rootEntity, otherState.rootEntity);
    }


	/**
	 * Implementation uses serialization.
	 * Make sure there are no needed non-transient fields in the state and entity graph that
	 * reference an external object.
     * The {@link #domain} field of the duplicated object is set to the one of the this object (not the duplicated
     * domain).
	 */
	public State deepCopy() {
        State clone = SerializationUtils.clone(this);
        clone.domain = domain; // there's a bit of time waist here (no reason the domain field was serialized)...
        return clone;
	}



	public Domain getDomain() {
		return domain;
	}

    public @NotNull String getEntityId(NliEntity e) {
        return entityToEntityId.getExisting(e);
    }

    public List<String> getEntityIds(List<? extends NliEntity> l) {
	    return l.stream()
                .map(this::getEntityId)
                .collect(Collectors.toList());
    }

    public NliEntity getEntityById(String id) {
        return entityIdToEntity.safeGet(id);
    }


    /**
     * The returned value is invalid once this state is modified in a way that removes non-primitive entities.
     */
    private GraphKb generateGraphKB() {

        Collection<KBTriple> triples = new LinkedList<>();
        EntityGraphReflection.getNonPrimitiveEntitiesReachableFromRoot(rootEntity, false, false).forEach(current->
                EntityGraphReflection.getEntitiesInOutgoingRelationsFromGivenEntity(current, false).forEach(pair-> {
            Field relationField = pair.getLeft();
            Entity secondEntity = pair.getRight();
                    triples.add(new KBTriple(current, relationField,secondEntity, this));
        }));
        return new GraphKb(triples);
    }

    public GraphKb getGraphKb() {
        if (graphKb == null)
            graphKb = generateGraphKB();
        return graphKb;
    }

    @Override
    public String toString() {
        return getGraphKb().toString();
    }
}
