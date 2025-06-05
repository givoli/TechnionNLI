package ofergivoli.olib.data_structures.map;

import com.google.common.collect.BiMap;
import org.jetbrains.annotations.Nullable;

/**
 * @see SafeMap
 *
 * All the methods annotated as deprecated in this class should be explicitly defined as deprecated in all
 * extending classes (even when it's not a compilation error not to, due to extending another class that implements
 * those methods).
 *
 */
public interface SafeBiMap<K,V> extends BiMap<K,V>, SafeMap<K,V> {

    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated
    @Override
    BiMap<V, K> inverse();


    default @Nullable K safeInverseGet(V value) {
        //noinspection deprecation
        return inverse().get(value);
    }
}
