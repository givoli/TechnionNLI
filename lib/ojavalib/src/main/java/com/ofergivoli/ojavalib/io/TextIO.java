package com.ofergivoli.ojavalib.io;

import com.ofergivoli.ojavalib.exceptions.UncheckedFileNotFoundException;
import com.ofergivoli.ojavalib.string.StringManager;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BOMInputStream;
import org.mozilla.universalchardet.UniversalDetector;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;


public class TextIO {


	public static class UncheckedUnsupportedEncodingException extends RuntimeException {	private static final long serialVersionUID = 1L;}


	// good number of bytes to read iteratively from file.
	private static final int GOOD_SIZE_OF_BYTES_BUFFER_FOR_SCANNING_FILE = 4096;



	/**
	 * The encoding used is UTF-8 with BOM.
	 * The BOM will be written during this method call.
	 * Note: this encoding is one of the encodings which I consider as "standard" in the conventions of this
	 * library.
	 */
	public static OutputStreamWriter getStandardStreamWriter(OutputStream os) {
		return getStreamWriterForUtf8(os, true);
	}


	public static OutputStreamWriter getStandardStreamWriter(File outputFile) {
		try {
			return getStandardStreamWriter(new FileOutputStream(outputFile));
		} catch (FileNotFoundException e) {
			throw new UncheckedFileNotFoundException(e);
		}
	}




	public static OutputStreamWriter getStreamWriterForUtf8(OutputStream os, boolean writeBOM) {
		OutputStreamWriter $ = new OutputStreamWriter(os,StandardCharsets.UTF_8);
		if (writeBOM) {
			try {
				$.write('\ufeff');
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
		return $;
	}



	public static OutputStreamWriter getStreamWriterForUtf8(File outputFile, boolean writeBOM) {
		try {
			return getStreamWriterForUtf8(new FileOutputStream(outputFile), writeBOM);
		} catch (FileNotFoundException e) {
			throw new UncheckedFileNotFoundException(e);
		}
	}



	/**
	 * If a BOM exist - it will be automatically consumed and used to detect the encoding. In that case, the 
	 * BOM will be skipped when the user reads from the stream (i.e. no need for manually skipping the BOM).
	 * If a BOM does not exist - UTF-8 encoding will be used.
	 * IMPORTANT: After calling this method you may no longer keep reading from 'is' (not by the returned 
	 * reader) because only the returned reader will return few characters that where read during the execution
	 * of this method.
	 * @throws UncheckedUnsupportedEncodingException self explanatory
	 */
	public static InputStreamReader getStandardStreamReader(InputStream is) {

		BOMInputStream stream = new BOMInputStream(is);

		try {
			String encoding = stream.hasBOM() ? stream.getBOMCharsetName() : StandardCharsets.UTF_8.name();
			return new InputStreamReader(stream, encoding);
			/**
			 * Note: it was critical to use in the last line 'stream' and not 'is' becuase BOMInputStream reads 
			 * few characters from the stream, and we loose those characters (they are not returned to the user)
			 * if we'll read from is.
			 */
		} catch (UnsupportedEncodingException e) {
			throw new UncheckedUnsupportedEncodingException();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
	
	
	public static InputStreamReader getStandardStreamReader(File inputFile) {
		try {
			return (getStandardStreamReader(new FileInputStream(inputFile)));
		} catch (FileNotFoundException e) {
			throw new UncheckedFileNotFoundException(e);
		}
		
	}


	/**
	 * The BOM is skipped (i.e. the returned reader will not return it as read characters).
	 * @throws UncheckedFileNotFoundException  self explanatory
	 */
	public static String readAllTextFromFile(File inputFile, String charsetName)
	{
		try (FileInputStream is = new FileInputStream(inputFile)) {
			return IOUtils.toString(new BOMInputStream(is), charsetName);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}


	/**
	 * BOM is not written.
	 * @param outputFile - file to be created/overwritten.
	 * @param okToOverwrite If this is false, and 'outputFile' already exists, an UncheckedIOException is 
	 * thrown (and the existing file is not modified). Otherwise, content of existing file (if such exists)
	 * is overwritten. 
	 */
	public static void writeTextToFile(File outputFile, String charsetName, String text, boolean okToOverwrite)
	{
		
		if (!okToOverwrite && outputFile.exists())
		{
			throw new UncheckedIOException(new IOException("File already exists!"));
		}
		
		
		try (OutputStreamWriter os = new OutputStreamWriter(new FileOutputStream(outputFile),charsetName)) {
			os.write(text);
		} catch (UnsupportedEncodingException e) {
			throw new UncheckedUnsupportedEncodingException();
		} catch (FileNotFoundException e) {
			throw new UncheckedFileNotFoundException(e);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}


	/**
	 * IMPORTANT: It is the user's responsibility to make sure reader does not read the BOM (otherwise this 
	 * method will read the BOM as part of the text)
	 */
	public static String readAllFromReader(Reader r) {
		StringBuilder sb = new StringBuilder();
		char buffer[] = new char[GOOD_SIZE_OF_BYTES_BUFFER_FOR_SCANNING_FILE];
		while (true) {
			int readCount;
			try {
				readCount = r.read(buffer);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			if (readCount<0)
				return sb.toString();
			sb.append(buffer,0,readCount);
		}
	}


	/**
	 * The encoding treatment is as defined in the documentation of {@link #getStandardStreamReader(InputStream)}.
	 * In particular, the BOM (if existing) is skipped (i.e. the returned reader will not return it as read characters).
	 */
	public static String readAllTextFromFileInStandardEncoding(File inputFile) {

		
		try (InputStreamReader sr = getStandardStreamReader(new FileInputStream(inputFile))){
			return readAllFromReader(sr);
		} catch (FileNotFoundException e) {
			throw new UncheckedFileNotFoundException(e);
		} catch (IOException e1) {
			throw new UncheckedIOException(e1);
		}
	}

	public static String readAllTextFromFileInStandardEncoding(Path inputFile) {
		return readAllTextFromFileInStandardEncoding(inputFile.toFile());
	}



		/**
         * The encoding treatment is as defined in the documentation of {@link #getStandardStreamWriter(OutputStream)}.
         * @param okToOverwrite If this is false, and 'outputFile' already exists, an UncheckedIOException is
         * thrown (and the existing file is not modified). Otherwise, content of existing file (if such exists)
         * is overwritten.
         */
	public static void writeTextToFileInStandardEncoding(File outputFile, String text, boolean okToOverwrite) {

		if (!okToOverwrite && outputFile.exists())
		{
			throw new UncheckedIOException(new IOException("File already exists!"));
		}
		
		
		try (OutputStreamWriter sw = getStandardStreamWriter(new FileOutputStream(outputFile))) {
			sw.write(text);
		} catch (FileNotFoundException e) {
			throw new UncheckedFileNotFoundException(e);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}


	public static void writeTextToFileInStandardEncoding(Path outputFile, String text, boolean okToOverwrite) {
		writeTextToFileInStandardEncoding(outputFile.toFile(), text, okToOverwrite);
	}


	/**
	 * If an encoding is not detected, the encoding treatment is as defined in the documentation of 
	 * {@link #getStandardStreamReader(InputStream)}. 
	 * In any case - the BOM (if existing) is skipped (i.e. the returned reader will not return it as read characters).
	 * @throws UncheckedUnsupportedEncodingException  self explanatory
	 */
	public static InputStreamReader getStreamReaderDetectingEncodingAutomatically(File inputFile) {

		Optional<String> encoding = detectFileEncoding(inputFile);

		try {
			if (!encoding.isPresent()) 
				return getStandardStreamReader(new FileInputStream(inputFile));

			return new InputStreamReader(new BOMInputStream(new FileInputStream(inputFile)),encoding.get());
		} catch (UnsupportedEncodingException e) {
			throw new UncheckedUnsupportedEncodingException();
		} catch (FileNotFoundException e) {
			throw new UncheckedFileNotFoundException(e);
		}
	}


	/**
	 * See documentation of {@link #getStreamReaderDetectingEncodingAutomatically(File)}}
	 * In particular, the BOM (if existing) is skipped (i.e. the returned reader will not return it as read characters).
	 */
	public static String readAllTextFromFileDetectingEncodingAutomatically(File inputFile) {
		InputStreamReader reader = getStreamReaderDetectingEncodingAutomatically(inputFile);
		return readAllFromReader(reader);
	}

	public static String readAllTextFromFileDetectingEncodingAutomatically(Path inputFile) {
		return readAllTextFromFileDetectingEncodingAutomatically(inputFile.toFile());
	}


		/**
         * See documentation of {@link #getStreamReaderDetectingEncodingAutomatically(File)}}
         * In particular, the BOM (if existing) is skipped (i.e. the returned reader will not return it as read characters).
         */
	public static List<String> readAllLinesFromFileDetectingEncodingAutomatically(File inputFile) {
		try (InputStreamReader reader = getStreamReaderDetectingEncodingAutomatically(inputFile)) {
			return StringManager.splitStringIntoNonEmptyLines(readAllFromReader(reader));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static List<String> readAllLinesFromFileDetectingEncodingAutomatically(Path inputFile) {
		return readAllLinesFromFileDetectingEncodingAutomatically(inputFile.toFile());
	}


	/**
	 *  Returns the detected encoding of the file, or an empty value if no encoding was detected.
	 *  I think that detection may fail (or perhaps even be false), even for files with consistent encoding.    
	 */ 
	public static Optional<String> detectFileEncoding(File inputFile) {

		try (java.io.FileInputStream is = new FileInputStream(inputFile)) {
			UniversalDetector detector = new UniversalDetector(null);

			byte buffer[] = new byte[GOOD_SIZE_OF_BYTES_BUFFER_FOR_SCANNING_FILE];
			int nread;

			while ((nread = is.read(buffer)) > 0 && !detector.isDone()) {
				detector.handleData(buffer, 0, nread);
			}

			detector.dataEnd();

			String encoding = detector.getDetectedCharset();
			if (encoding == null)
				return Optional.empty();

			return Optional.of(encoding);
		} catch (FileNotFoundException e) {
			throw new UncheckedFileNotFoundException(e);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

	}




}
