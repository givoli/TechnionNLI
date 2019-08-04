package com.ofergivoli.ojavalib.data_structures.set;

import java.util.Collection;
import java.util.HashSet;


public class SafeHashSet<E> extends HashSet<E> implements SafeSet<E> {

    private static final long serialVersionUID = 1503380830724204956L;


    public SafeHashSet(Collection<E> values) {
        super(values);
    }

    public SafeHashSet() {
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
    public boolean containsAll(@SuppressWarnings("NullableProblems") Collection<?> c) {
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
