package ofergivoli.olib.io.serialization;

import ofergivoli.olib.exceptions.UncheckedClassNotFoundException;

import java.io.*;
import java.util.Optional;

/**
 * @param <T> the type of the objects read.
 */
public class ObjectReader<T extends Serializable> {

    private final ObjectInputStream objectInputStream;


    public ObjectReader(File inputFile) {
        try {
            this.objectInputStream =  new ObjectInputStream(new FileInputStream(inputFile));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }


    /**
     * @return An empty value in case EOF was reached.
     */
    public Optional<T> read() {
        try {
            //noinspection unchecked

            Optional<T> obj = Optional.of((T) objectInputStream.readObject());
            return obj;
        } catch (EOFException e)
        {
            return Optional.empty();
        } catch (ClassNotFoundException e) {
            throw new UncheckedClassNotFoundException(e);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void close() {
        try {
            objectInputStream.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
