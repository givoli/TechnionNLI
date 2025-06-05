package ofergivoli.olib.io;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;


public class WriterUtils {

    /**
     * A simple wrapper to writer.write(), throwing only unchecked exceptions.
     */
    public static void write(Writer writer, String str) {
        try {
            writer.write(str);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
