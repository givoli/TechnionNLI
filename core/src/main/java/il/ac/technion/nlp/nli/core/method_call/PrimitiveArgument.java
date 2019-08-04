package il.ac.technion.nlp.nli.core.method_call;

import il.ac.technion.nlp.nli.core.state.PrimitiveEntity;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class PrimitiveArgument extends Argument {
    private static final long serialVersionUID = -8035678798396228265L;

    final Collection<PrimitiveEntity> primitiveEntities;


    public PrimitiveArgument(Object... primitiveValues) {
        this.primitiveEntities = Arrays.stream(primitiveValues)
                .map(PrimitiveEntity::new)
                .collect(Collectors.toList());
    }

    private PrimitiveArgument() {
        primitiveEntities = new LinkedList<>();
    }


    public static Argument createFromEntities(Collection<PrimitiveEntity> primitiveEntities) {
        PrimitiveArgument result = new PrimitiveArgument();
        result.primitiveEntities.addAll(primitiveEntities);
        return result;
    }


    public Collection<PrimitiveEntity> getPrimitiveEntities() {
        return primitiveEntities;
    }

    @Override
    public int size() {
        return primitiveEntities.size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PrimitiveArgument that = (PrimitiveArgument) o;
        return Objects.equals(primitiveEntities, that.primitiveEntities);
    }

    @Override
    public int hashCode() {
        return Objects.hash(primitiveEntities);
    }
}
