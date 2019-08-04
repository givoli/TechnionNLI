package il.ac.technion.nlp.nli.core.state.knowledgebase;

import il.ac.technion.nlp.nli.core.state.Entity;
import il.ac.technion.nlp.nli.core.state.NliEntity;
import il.ac.technion.nlp.nli.core.state.PrimitiveEntity;
import il.ac.technion.nlp.nli.core.state.State;

import java.lang.reflect.Field;
import java.util.Objects;

/**
 * A {@link KBTriple} is not meant to be saved persistently.
 *
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class KBTriple {

    public final NliEntity firstEntity;
    public final Field relation;
    public final Entity secondEntity;

    /**
     * see constructor.
     */
    private final State state;


    /**
     * @param state Will be used to get the id of entities. Thus caller must not remove from
     *              it non-primitive entities which are in this {@link KBTriple#}.
     */
    public KBTriple(NliEntity firstEntity, Field relation, Entity secondEntity, State state) {
        this.firstEntity = firstEntity;
        this.relation = relation;
        this.secondEntity = secondEntity;
        this.state = state;
    }

    /**
     * The comparison of non-primitive entities is done by their ids (according to the state of each). Other than that,
     * the {@link #state} fields are not used.
     * This method can be used on triples across different states (it provides expected results because it compares
     * entity ids instead of non-primitive entity objects).
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KBTriple kbTriple = (KBTriple) o;
        return
                Objects.equals(relation, kbTriple.relation) &&
                        getPrimitiveEntityOrId(firstEntity).equals(kbTriple.getPrimitiveEntityOrId(kbTriple.firstEntity)) &&
                        getPrimitiveEntityOrId(secondEntity).equals(kbTriple.getPrimitiveEntityOrId(kbTriple.secondEntity));

        // TODO: test
    }

    /**
     * Considers the id of {@link NliEntity} instead of their actual value. Uses the {@link #state} field only to get
     * those ids.
     * This method can be used on triples across different states (it provides expected results because it relies on
     * entity ids instead of non-primitive entity objects).
     */
    @Override
    public int hashCode() {
        return Objects.hash(getPrimitiveEntityOrId(firstEntity), relation, getPrimitiveEntityOrId(secondEntity));
    }

    /**
     * @return either a {@link PrimitiveEntity} or a String (id of {@link NliEntity}).
     */
    private Object getPrimitiveEntityOrId(Entity entity) {
        if (entity instanceof PrimitiveEntity)
            return entity;
        return state.getEntityId((NliEntity) entity);
    }

    @Override
    public String toString() {
        return "(" + getPrimitiveEntityOrId(firstEntity) + "," +  relation.getName() + "," +
                getPrimitiveEntityOrId(secondEntity) + ")";
    }
}
