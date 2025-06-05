package ofergivoli.olib.data_structures.map;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Adding methods with more restrictive parameter types, deprecating methods with dangerously unrestricted parameter
 * types (at the cost of loosing the ability to query for a key using an object of a different type than 'K').
 *
 * All the methods annotated as deprecated in this class should be explicitly defined as deprecated in all
 * extending classes (even when it's not a compilation error not to, due to extending another class that implements
 * those methods).
 */
public interface SafeMap<K,V> extends Map<K,V> {

    default V safeGet(K key) {
        //noinspection deprecation
        return get(key);
    }

    default boolean safeContainsKey(K key){
        //noinspection deprecation
        return containsKey(key);
    }

    default V safeRemove(K key){
        //noinspection deprecation
        return remove(key);
    }

    default boolean safeContainsValue(V value){
        //noinspection deprecation
        return containsValue(value);
    }

    default V safeGetOrDefault(K key, V defaultValue) {
        //noinspection deprecation
        return getOrDefault(key, defaultValue);
    }

    default boolean safeRemove(K key, V value){
        //noinspection deprecation
        return remove(key, value);
    }

    /**
     * Puts the new [key,value] pair in the map and then verifies the key did not already exist in the map.
     * @throws RuntimeException in case 'key' already exists in the map. This exception is throws AFTER the 'value'
     * replaces the old value of 'key'.
     */
    default void putNewKey(K key, V value){
        V previous = put(key, value);
        if (previous != null)
            throw new RuntimeException("The map already contained the key: " + key);
    }

    /**
     * Like {@link #safeGet(Object)} but verifies that the returned value is not null.
     * @throws RuntimeException in case {@link #get} returned null. Note that this can happen even if the key exists in
     * the map, in case it is mapped to 'null'.
     */
    default @NotNull V getExisting(K key){
        @SuppressWarnings("deprecation")
        V result = get(key);
        if (result == null)
            throw new RuntimeException("The map does not contain the key: " + key);
        return result;
    }


    @Deprecated
    @Override
    V get(Object key);

    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated
    @Override
    boolean containsKey(Object key);

    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated
    @Override
    V remove(Object key);

    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated
    @Override
    boolean containsValue(Object value);

    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated
    @Override
    V getOrDefault(Object key, V defaultValue);

    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated
    @Override
    boolean remove(Object key, Object value);


}
