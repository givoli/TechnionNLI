package ofergivoli.olib.io.serialization.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ofergivoli.olib.io.TextIO;

import java.io.*;
import java.lang.reflect.Type;


public class GsonSerialization {


    /**
     * WARNING: multiple references to the same object results in multiple serialized objects.
     * @param nonGenericObj must be a valid object for serialization according to Gson (so no circular references),
     *                      and also of non-generic type.
     */
    public static <T> void writeNonGenericObjectToFile(File outputFile, T nonGenericObj, boolean prettyOutput) {
        writeObjectToFile(outputFile, nonGenericObj, nonGenericObj.getClass(), prettyOutput);
    }



    /**
     * WARNING: multiple references to the same object results in multiple serialized objects.
     * @param obj must be a valid object for serialization according to Gson (so no circular references).
     *                      May be generic.
     */
    public static <T> void writeObjectToFile(File outputFile, T obj, Type objType, boolean prettyOutput) {

        try(Writer writer = TextIO.getStandardStreamWriter(outputFile)){
            GsonBuilder gsonBuilder = new GsonBuilder();
            if (prettyOutput)
                gsonBuilder = gsonBuilder.setPrettyPrinting();
            Gson gson = gsonBuilder.create();
            gson.toJson(obj, objType, writer);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }



    /**
     * @param objType may be generic.
     */
    public static <T> T readObjectFromFile(File inputFile, Type objType) {
        try(Reader reader = TextIO.getStandardStreamReader(inputFile)){
            Gson gson = new GsonBuilder().create();
            return gson.fromJson(reader, objType);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
