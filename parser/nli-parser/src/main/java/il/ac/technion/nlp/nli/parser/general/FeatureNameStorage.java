package il.ac.technion.nlp.nli.parser.general;

import com.ofergivoli.ojavalib.data_structures.map.SafeHashMap;
import com.ofergivoli.ojavalib.data_structures.map.SafeMap;

/**
 * An object of this class is used to store the names of the feature names extracted so far in the current inference,
 * in order to avoid keeping references to multiple identical {@link String} objects.
 *
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class FeatureNameStorage {

    private final SafeMap<String,String> featureNameToItself = new SafeHashMap<>();


    /**
     * @return a value equal to 'feature'.
     * The caller of this method should keep a reference to the returned value rather than to the argument sent to
     * this method. This way, you won't waist memory on holding references to multiple identical feature name string
     * objects.
     */
    public String getReferenceToStoredFeatureName(String feature) {
        String savedCopy = featureNameToItself.safeGet(feature);
        if (savedCopy != null)
            return savedCopy;
        featureNameToItself.putNewKey(feature,feature);
        return feature;
    }


    public long getEstimatedMemoryUsedForStoringFeatureDataInBytes(){

        long estimatedMemoryUsedToStoreFeatureNames =
                featureNameToItself.keySet().stream().mapToLong(this::getEstimatedMemoryUsedToStoreStringInBytes).sum();

        long memoryUsedToStoreFeatureValues = featureNameToItself.size() * 8;

        return estimatedMemoryUsedToStoreFeatureNames + memoryUsedToStoreFeatureValues;
    }

    public int getEstimatedMemoryUsedToStoreStringInBytes(String s){
        // based on: [https://www.javamex.com/tutorials/memory/string_memory_usage.shtml]
        return 8 * ((s.length() * 2) + 45) / 8;
    }

    public void clear(){
        featureNameToItself.clear();
    }

}
