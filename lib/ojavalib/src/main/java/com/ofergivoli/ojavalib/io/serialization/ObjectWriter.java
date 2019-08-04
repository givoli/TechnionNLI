package com.ofergivoli.ojavalib.io.serialization;

import java.io.*;

/**
 * @param <T> the type of the objects written.
 */
public class ObjectWriter<T extends Serializable> implements Closeable {

    private final ObjectOutputStream objectOutputStream;

    /**
     * Note: all objects written by the created writer will stay alive at least until the GC collects the writer itself.
     */
    public ObjectWriter(File outputFile) {
        try {
            this.objectOutputStream = new ObjectOutputStream(new FileOutputStream(outputFile));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }




    public void write(T object) {
        try {
            objectOutputStream.writeObject(object);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void close() {
        try {
            objectOutputStream.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }


    public void flush() {
        try {
            objectOutputStream.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
