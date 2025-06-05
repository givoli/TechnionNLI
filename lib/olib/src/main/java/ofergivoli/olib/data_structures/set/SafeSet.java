package ofergivoli.olib.data_structures.set;

import java.util.Collection;
import java.util.Set;

/**
 * Adding methods with more restrictive parameter types, deprecating methods with dangerously unrestricted parameter
 * types (at the cost of losing the ability to check/remove an object from the set by an object of a different type
 * than 'E').
 */
public interface SafeSet<E> extends Set<E> {


    default boolean safeContains(E value){
        //noinspection deprecation
        return contains(value);
    }

    default boolean safeRemove(E value) {
        //noinspection deprecation
        return remove(value);
    }

    default boolean safeContainsAll(Collection<?> c) {
        //noinspection deprecation
        return containsAll(c);
    }

    @SuppressWarnings("UnusedReturnValue")
    default boolean safeRemoveAll(Collection<?> c){
        //noinspection deprecation
        return removeAll(c);
    }


    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated
    @Override
    boolean contains(Object o);

    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated
    @Override
    boolean containsAll(@SuppressWarnings("NullableProblems") Collection<?> c);

    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated
    @Override
    boolean remove(Object o);

    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated
    @Override
    boolean removeAll(@SuppressWarnings("NullableProblems") Collection<?> c);

}
