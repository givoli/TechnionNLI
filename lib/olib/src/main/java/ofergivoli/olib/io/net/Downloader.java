package ofergivoli.olib.io.net;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;


public class Downloader {

    public static String downloadTextContent(URL url) {
        StringBuilder sb = new StringBuilder();

        //Based on: https://commons.apache.org/proper/commons-io/description.html
        try (InputStream in = url.openStream()) {
            InputStreamReader reader = new InputStreamReader(in);
            BufferedReader buf = new BufferedReader(reader);
            String line;
            while ((line = buf.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return sb.toString();
    }

    public static String downloadTextContent(String url) {
        try {
            return downloadTextContent(new URL(url));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

}
