package il.ac.technion.nlp.nli.parser.general;

import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Just like {@link Function}, but calculates the function only the first time {@link #apply(Object)} is called per
 * arguments (in following calls - the previously calculated value is returned).
 *
 * Memory note: {@link CachedFunction} holds all objects it ever created (i.e. the result of the function application).

 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class CachedFunction<T, R> implements Function<T, R>{

    private Function<T, R> function;
    private Map<T, R> map = new HashMap<>();


    /**
     * See doc of {@link #getHitsRatio()}.
     */
    private long hitsNum = 0;
    private long missesNum = 0;


    /**
     * @param useIdentityComparisonForArg If true, then the argument of {@link #apply(Object)} is considered identical
     *                                    only to itself.
     *                                    If false, then the {@link #equals(Object)} and {@link #hashCode()} methods
     *                                    are used instead.
     */
    public CachedFunction(boolean useIdentityComparisonForArg, Function<T, R> function) {
        this.function = function;
        map = useIdentityComparisonForArg ? new IdentityHashMap<>() : new HashMap<>();
    }

    @Override
    public R apply(T t) {
        R val = map.get(t);
        if (val == null) {
            missesNum++;
            val = function.apply(t);
            map.put(t, val);
        } else
            hitsNum++;
        return val;
    }

    /**
     * @return The ratio between the number of hits (calls to {@link #apply(Object)} that didn't required computation)
     * and the number of total calls to {@link #apply(Object)}.
     */
    @SuppressWarnings("unused")
    public double getHitsRatio() {
        return (double)hitsNum / (hitsNum + missesNum);
    }

    /**
     * @return The map containing the cached data (modified as this object is being used).
     * read-only.
     */
    public Map<T, R> getCacheMap() {
        return Collections.unmodifiableMap(map);
    }
}
