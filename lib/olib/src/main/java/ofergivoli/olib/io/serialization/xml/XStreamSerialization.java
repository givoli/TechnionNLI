package ofergivoli.olib.io.serialization.xml;

import ofergivoli.olib.exceptions.UncheckedFileNotFoundException;
import ofergivoli.olib.io.TextIO;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.security.AnyTypePermission;

import java.io.*;
import java.nio.file.Path;


public class XStreamSerialization {

	/**
	 * Writes 'obj' as XML to 'writer' using XStream. Supports multiple references per object and circular references.
	 */
	public static <T> void writeObjectInXmlFormat(T obj, Writer writer) {
		XStream xstream = new XStream();
		xstream.setMode(XStream.XPATH_ABSOLUTE_REFERENCES);
		xstream.toXML(obj, writer);
	}


	/**
	 * See: {@link #writeObjectInXmlFormat(Object, Writer)}
	 *
	 * @param outputFile Overwritten if already exists.
	 */
	@SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated
	public static <T> void writeObjectToXmlFile(T obj, File outputFile) {
		//TODO: test
		try (Writer writer = TextIO.getStandardStreamWriter(new FileOutputStream(outputFile))) {
			writeObjectInXmlFormat(obj, writer);
		} catch (FileNotFoundException e) {
			throw new UncheckedFileNotFoundException(e);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static <T> void writeObjectToXmlFile(T obj, Path outputFile) {
		//noinspection deprecation
		writeObjectToXmlFile(obj, outputFile.toFile());
	}

	/**
	 * Using XStream. The source that 'reader' reads from should be trusted, because XStream is given permission to read
	 * all types (I have no idea if writing a malicious XML is trivial or not).
	 *
	 * @param ignoreUnknownElementsInXml when true we ignore any elements (e.g. fields) in the XML that are does not exist
	 *                                   in the relevant class that we instantiate (otherwise an exception is thrown).
	 */
	public static <T> T readObjectInXmlFormatFromTrustedSource(Reader reader, boolean ignoreUnknownElementsInXml) {
		XStream xstream = new XStream();
		XStream.setupDefaultSecurity(xstream); // TODO: remove this line once updating XStream to version 1.5 or later
		xstream.addPermission(new AnyTypePermission());

		if (ignoreUnknownElementsInXml)
			xstream.ignoreUnknownElements();
		@SuppressWarnings("unchecked")
		T $ = (T) xstream.fromXML(reader);
		return $;
	}

	/**
	 * using XStream.
	 *
	 * @param ignoreUnknownElementsInXml when true we ignore any elements (e.g. fields) in the XML that are does not exist
	 *                                   in the relevant class that we instantiate (otherwise an exception is thrown).
	 */
	public static <T> T readObjectFromTrustedXmlFile(boolean ignoreUnknownElementsInXml, File inputXml) {
		//TODO: test
		try (Reader reader = TextIO.getStandardStreamReader(new FileInputStream(inputXml))) {
			return readObjectInXmlFormatFromTrustedSource(reader, ignoreUnknownElementsInXml);
		} catch (FileNotFoundException e) {
			throw new UncheckedFileNotFoundException(e);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static <T> T readObjectFromTrustedXmlFile(boolean ignoreUnknownElementsInXml, Path inputXml) {
		return readObjectFromTrustedXmlFile(ignoreUnknownElementsInXml, inputXml.toFile());
	}
}
