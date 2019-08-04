package com.ofergivoli.ojavalib.data_structures.map;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;


public class Maps {

    /**
     * Takes a map that map Key type to a collection of CollectionType type (s.t. each element of the collection is of
     * type Element), and adds given elements to the collection of a given key, creating a new collection if that key
     * doesn't already have associated collection.
     * @param collectionBuilder The supplier to be used to create a new empty collection if relevant.
     */
    public static <Key, Element, CollectionType extends Collection<Element>> void addToMapOfCollections (
            Map<Key,CollectionType> map, Key key, Element elementToAdd,
            Supplier<CollectionType> collectionBuilder) {

        CollectionType collection;
        if (!map.containsKey(key)) {
            collection = collectionBuilder.get();
            map.put(key, collection);
        } else
            collection = map.get(key);

        collection.add(elementToAdd);
    }



    /**
     * TODO: delete (not generally useful enough)
     * Takes a map that maps Key type to a collection of CollectionType type (s.t. each element of the collection is of
     * type Element), and returns the collection of a given key, creating a new collection if that key doesn't already
     * have associated collection.
     * @param collectionBuilder The supplier to be used to create a new empty collection if relevant.
     */
    @Deprecated
    public static <Key, Element, CollectionType extends Collection<Element>> CollectionType
        getCollectionFromMapOfCollections ( Map<Key,CollectionType> map, Key key,
                                            Supplier<CollectionType> collectionBuilder) {

        if (!map.containsKey(key)) {
            map.put(key, collectionBuilder.get());
        }

        return map.get(key);
    }

    /**
     * Modifies the value in map associated with 'key' by applying 'function' on it, assuming 'initialValue' if no
     * value was previously associated with 'key'.
     */
    public static <Key, Value> void modifyValue(Map<Key, Value> map, Key key, Value initialValue,
                                                Function<Value,Value> function) {
        Value previousValue = map.get(key);
        if (previousValue == null)
            map.put(key, function.apply(initialValue));
        else
            map.put(key, function.apply(previousValue));
    }


    /**
     * Modifies all the values in the map by applying 'function' on each.
     */
    public static <Key, Value> void modifyAllValues(Map<Key, Value> map, Function<Value,Value> function) {
        map.forEach((key,value)->map.put(key, function.apply(value)));
    }



}
