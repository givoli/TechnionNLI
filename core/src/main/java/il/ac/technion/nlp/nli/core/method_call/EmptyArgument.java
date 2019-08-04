package il.ac.technion.nlp.nli.core.method_call;

/**
 * Note: {@link PrimitiveArgument} and {@link NonPrimitiveArgument} can represent an empty argument as well. We use
 * an object of this class to avoid specifying whether the expected type is {@link PrimitiveArgument} or
 * {@link NonPrimitiveArgument}.
 *
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class EmptyArgument extends Argument {

    private static final long serialVersionUID = 4299405272183671775L;

    @Override
    public int size() {
        return 0;
    }


    @Override
    public boolean equals(Object o) {
        return this == o || o != null && getClass() == o.getClass();
    }

    @Override
    public int hashCode() {
        return 0;
    }
}
