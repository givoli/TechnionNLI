package ofergivoli.olib.data_structures.map;

import java.util.HashMap;
import java.util.Map;

/**
 * A {@link SafeMap} implemented by {@link HashMap}.
 */
public class SafeHashMap<K,V>  extends HashMap<K,V> implements SafeMap<K,V> {

    private static final long serialVersionUID = 3757776990099263067L;


    public SafeHashMap() {
    }

    public SafeHashMap(Map<K, V> m) {
        super(m);
    }


    /* All the following methods are explicitly defined as deprecated, otherwise a user with a reference to this class'
    objects will not see they are deprecated. */
    @Deprecated
    @Override
    public V get(Object key){
        return super.get(key);
    }

    @Deprecated
    @Override
    public boolean containsKey(Object key){
        return super.containsKey(key);
    }

    @Deprecated
    @Override
    public V remove(Object key){
        return super.remove(key);
    }

    @Deprecated
    @Override
    public boolean containsValue(Object value){
        return super.containsValue(value);
    }

    @Deprecated
    @Override
    public V getOrDefault(Object key, V defaultValue){
        return super.getOrDefault(key, defaultValue);
    }

    @Deprecated
    @Override
    public boolean remove(Object key, Object value){
        return super.remove(key, value);
    }

}
