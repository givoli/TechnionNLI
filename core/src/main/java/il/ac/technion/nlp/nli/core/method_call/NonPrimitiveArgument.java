package il.ac.technion.nlp.nli.core.method_call;

import com.ofergivoli.ojavalib.data_structures.set.SafeHashSet;
import com.ofergivoli.ojavalib.data_structures.set.SafeSet;
import il.ac.technion.nlp.nli.core.state.NliEntity;
import il.ac.technion.nlp.nli.core.state.State;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class NonPrimitiveArgument extends Argument {

    private static final long serialVersionUID = -4134672575101383385L;

    final SafeSet<String> nonPrimitiveEntityIds;


    /**
     * @param nonPrimitiveEntityIds If it contains a single element then it may represent either a parameter of type
     *                              {@link NliEntity} or a {@link Collection} thereof.
     */
    public NonPrimitiveArgument(SafeSet<String> nonPrimitiveEntityIds) {
        this.nonPrimitiveEntityIds = nonPrimitiveEntityIds;
    }

    public NonPrimitiveArgument(Collection<String> nonPrimitiveEntityIds) {
        this(new SafeHashSet<>(nonPrimitiveEntityIds));
    }

    public NonPrimitiveArgument(String... nonPrimitiveEntityIds) {
        this(new SafeHashSet<>(Arrays.asList(nonPrimitiveEntityIds)));
    }

    /**
     * TODO: make old code use this.
     * @param initialState not saved, just used for extracting ids.
     */
    public NonPrimitiveArgument(State initialState, List<? extends NliEntity> entities) {
        this(entities.stream()
                .map(initialState::getEntityId)
                .collect(Collectors.toList()));
    }

    public NonPrimitiveArgument(State initialState, NliEntity entity) {
        this(initialState.getEntityId(entity));
    }

    public SafeSet<String> getNonPrimitiveEntityIds() {
        return nonPrimitiveEntityIds;
    }

    @Override
    public int size() {
        return nonPrimitiveEntityIds.size();
    }


    /**
     * Based on the ids.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NonPrimitiveArgument that = (NonPrimitiveArgument) o;
        return Objects.equals(nonPrimitiveEntityIds, that.nonPrimitiveEntityIds);
    }

    /**
     * Based on the ids.
     */
    @Override
    public int hashCode() {
        return Objects.hash(nonPrimitiveEntityIds);
    }
}
