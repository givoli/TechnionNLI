package il.ac.technion.nlp.nli.core.method_call;

import java.io.Serializable;
import java.util.Collection;

/**
 * Represents a single argument for a {@link MethodCall}.
* Can represent either a single {@link il.ac.technion.nlp.nli.core.state.Entity} or a {@link Collection} thereof.
* Must be valid for deep copies of the state as well (meaning: must refer to non-primitive entities only by id).
 * TODO: maybe should be generic with generic argument being either PrimitiveEntitiy or String in implementing classes. And then add here a method returning Collection<T>, and make everything use it instead of getters in implementing classes.
 */
public abstract class Argument implements Serializable {

    private static final long serialVersionUID = -1473233072508712882L;

    /**
     * @return the number of values represented by this argument.
     */
    public abstract int size();


    /**
     * In case of argument values are non-primitive entities, this method compares ids.
     */
    @Override
    public abstract boolean equals(Object other);

    /**
     * In case of argument values are non-primitive entities, the hashCode is a function of the ids.
     */
    @Override
    public abstract int hashCode();

}
