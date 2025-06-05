package ofergivoli.olib.data_structures.map;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;


public class SafeHashBiMap<K,V>  implements SafeBiMap<K,V> {

    private static final int DEFAULT_EXPECTED_SIZE = 16;

    /**
     * We need to have a {@link HashBiMap} field rather than extending it because its constructor is private.
     */
    private HashBiMap<K,V> biMap;


    public SafeHashBiMap (int expectedSize) {
        biMap = HashBiMap.create(expectedSize);
    }
    public SafeHashBiMap () {
        this(DEFAULT_EXPECTED_SIZE);
    }



    @Deprecated
    @Override
    public V get(Object key){
        return biMap.get(key);
    }

    @Deprecated
    @Override
    public boolean containsKey(Object key){
        return biMap.containsKey(key);
    }

    @Deprecated
    @Override
    public V remove(Object key){
        return biMap.remove(key);
    }

    @Deprecated
    @Override
    public boolean containsValue(Object value){
        return biMap.containsValue(value);
    }

    @Deprecated
    @Override
    public V getOrDefault(Object key, V defaultValue){
        return biMap.getOrDefault(key, defaultValue);
    }

    @Deprecated
    @Override
    public boolean remove(Object key, Object value){
        return biMap.remove(key, value);
    }


    @Override
    public int size() {
        return biMap.size();
    }

    @Override
    public boolean isEmpty() {
        return biMap.isEmpty();
    }

    @Override
    public void forEach(BiConsumer<? super K, ? super V> action) {
        biMap.forEach(action);
    }

    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        biMap.replaceAll(function);
    }

    @Override
    public V putIfAbsent(K key, V value) {
        return biMap.putIfAbsent(key, value);
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        return biMap.replace(key, oldValue, newValue);
    }

    @Override
    public V replace(K key, V value) {
        return biMap.replace(key, value);
    }

    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        return biMap.computeIfAbsent(key, mappingFunction);
    }

    @Override
    public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return biMap.computeIfPresent(key, remappingFunction);
    }

    @Override
    public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return biMap.compute(key, remappingFunction);
    }

    @Override
    public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        return biMap.merge(key, value, remappingFunction);
    }

    @Nullable
    @Override
    public V put(@Nullable K k, @Nullable V v) {
        return biMap.put(k,v);
    }

    @Nullable
    @Override
    public V forcePut(@Nullable K k, @Nullable V v) {
        return biMap.forcePut(k, v);
    }

    @Override
    public void putAll(@NotNull Map<? extends K, ? extends V> map) {
        biMap.putAll(map);
    }

    @Override
    public void clear() {
        biMap.clear();
    }

    @NotNull
    @Override
    public Set<K> keySet() {
        return biMap.keySet();
    }

    @NotNull
    @Override
    public Set<V> values() {
        return biMap.values();
    }

    @NotNull
    @Override
    public Set<Entry<K, V>> entrySet() {
        return biMap.entrySet();
    }

    @Deprecated
    @Override
    public BiMap<V, K> inverse() {
        return biMap.inverse();
    }
}
