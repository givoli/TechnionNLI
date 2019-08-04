package com.ofergivoli.ojavalib.io.serialization;

import java.io.File;
import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class SerializationUtils {
	
	public static <T extends Serializable> void writeObjectToFile(File outputFile, T object) {

		try(ObjectWriter<T> writer = new ObjectWriter<>(outputFile)) {
            writer.write(object);
        }
	}

    /**
     * @param outputFile all the objects in 'objects' will be written to this file.
     */
    public static <T extends Serializable> void writeAllObjectsToFile(File outputFile, Collection<T> objects) {
        try (ObjectWriter<T> writer = new ObjectWriter<>(outputFile)) {
            objects.forEach(writer::write);
        }
    }


	public static <T extends Serializable> T readObjectFromFile(File inputFile) {
        T $;
        ObjectReader<T> reader = new ObjectReader<>(inputFile);
        //noinspection ConstantConditions
        $ = reader.read().get();
        reader.close();
        return $;
	}


    /**
     * @param inputFile contains a sequence of objects of type T.
     * @param maxCollectionSize set to null for no-limit.
     */
    public static <T extends Serializable> List<T> readAllObjectsFromFile(File inputFile, Integer maxCollectionSize) {
        List<T> $ = new LinkedList<>();
        ObjectReader<T> reader = new ObjectReader<>(inputFile);
        Optional<T> obj;
        while ((obj = reader.read()).isPresent()
                && (maxCollectionSize==null || $.size()<maxCollectionSize)) {
            //noinspection OptionalGetWithoutIsPresent
            $.add(obj.get());
        }
        reader.close();
        return $;
    }


    public static <T extends Serializable> List<T> readAllObjectsFromFile(File inputFile) {
        return readAllObjectsFromFile(inputFile,null);
    }

}
