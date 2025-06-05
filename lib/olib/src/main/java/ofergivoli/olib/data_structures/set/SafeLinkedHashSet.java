package ofergivoli.olib.data_structures.set;

import java.util.Collection;
import java.util.LinkedHashSet;

public class SafeLinkedHashSet<E> extends LinkedHashSet<E> implements SafeSet<E>  {

    private static final long serialVersionUID = -956266983438274910L;

    public SafeLinkedHashSet(Collection<E> values) {
        super(values);
    }

    public SafeLinkedHashSet() {
    }



    // All the following methods are explicitly defined as deprecated, otherwise a user with a reference to this class'
    // objects will not see they are deprecated.

    @Deprecated
    @Override
    public boolean contains(Object o) {
        return super.contains(o);
    }

    @Deprecated
    @Override
    public boolean containsAll(@SuppressWarnings("NullableProblems")  Collection<?> c) {
        return super.containsAll(c);
    }


    @Deprecated
    @Override
    public boolean remove(Object o) {
        return super.remove(o);
    }

    @Deprecated
    @Override
    public boolean removeAll(Collection<?> c) {
        return super.removeAll(c);
    }


}
