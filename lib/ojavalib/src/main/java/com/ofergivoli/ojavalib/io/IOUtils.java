package com.ofergivoli.ojavalib.io;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.function.Consumer;
import java.util.function.Function;


public class IOUtils {

    /**
     * Executes 'consumer' with 'closeable' and then close 'closeable', throwing only unchecked exceptions.
     */
    public static <T extends Closeable> void doAndClose(T closeable, Consumer<T> consumer) {
        consumer.accept(closeable);
        try {
            closeable.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }


    /**
     * Applies 'func' on 'closeable' and then close 'closeable', throwing only unchecked exceptions.
     * @return the value returned by 'func'.
     */
    public static <T extends Closeable, L> L calcAndClose(T closeable, Function<T, L> func) {
        L result = func.apply(closeable);
        try {
            closeable.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return result;
    }


    /**
     * A simple wrapper to {@link Closeable#close()} that throws only unchecked exceptions.
     */
    public static void close(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void write(Writer writer, String str){
        try {
            writer.write(str);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
