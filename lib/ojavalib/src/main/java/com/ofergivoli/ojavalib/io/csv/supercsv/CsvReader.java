package com.ofergivoli.ojavalib.io.csv.supercsv;

import com.ofergivoli.ojavalib.io.TextIO;
import org.supercsv.io.CsvBeanReader;
import org.supercsv.io.ICsvBeanReader;
import org.supercsv.prefs.CsvPreference;

import java.io.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * Reads a list of JavaBeans (all of the same type) from CSV (each JavaBean is represented by a single row).
 * Each column name (a.k.a "header", i.e. first row cell) must be identical to a field name of the JavaBeans.
 * Note: unfortunately there's no single CSV format - multiple format have different escaping rules. I think that this
 * implementation corresponds to the "Excel" way, while the "unix way" is slightly different (just based on a post I
 * read).
 * @param <T> The type of the JavaBean each row represents.
 */
public class CsvReader<T> implements Closeable {

	private ICsvBeanReader csvBeanReader;
	
	
	// first row in CSV file.
	private String[] header;
	
	// The type of the objects represented by the lines of the CSV file.
	private Class<T> objType;
	
	private CsvSettings settings;
	
	/**
	 * The header (first row) will be read by this constructor.
	 * @param inputFile Encoding will be detected (hopefully) automatically (see documentation in 
	 * {@link TextIO}). This is useful, because Excel may save CSV files in
	 * Windows-1255 encoding, etc. 
	 * @param objType The type of the object read. Must match the given settings.
     *                May be null if you're never going to read objects via this reader (e.g. you only use it to get
     *                the header).
	 */
	public CsvReader(CsvSettings settings, File inputFile, Class<T> objType) {		
		InputStreamReader sr = TextIO.getStreamReaderDetectingEncodingAutomatically(inputFile);
		init(settings,sr, objType);
	}

	public CsvReader(File inputFile, Class<T> objType) {
		this(new CsvSettings(null),inputFile,objType);
	}
	
	
	/**
	 * @param reader - Will be closed when this object is closed.
	 */
	public CsvReader(CsvSettings settings, Reader reader, Class<T> objType) {
		init(settings, reader, objType);
	}

	
	/**
	 * @param reader - Will be closed when this object is closed.
	 */
	private void init(CsvSettings settings, Reader reader, Class<T> objType) {
		this.settings = settings;
		csvBeanReader = new CsvBeanReader(reader, CsvPreference.STANDARD_PREFERENCE);
		this.objType = objType;
		try {
			header = csvBeanReader.getHeader(true);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	
	/**
	 * @return An empty value if no more rows exist.
	 */
	public Optional<T> readObject() {
        T obj;
		try {
			if (settings.getSuperCsvProcessorsForReading().isPresent())
				obj = csvBeanReader.read(objType, header, settings.getSuperCsvProcessorsForReading().get());
			else
				obj = csvBeanReader.read(objType, header);
			
			if (obj == null) 
				return Optional.empty();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
        
        return Optional.of(obj);
	}

	
	public List<T> readAllObjects() {
		
		List<T> $ = new LinkedList<>();
		while (true) {
			Optional<T> obj = readObject();
			if (!obj.isPresent())
				return $;
			$.add(obj.get());
		}
	}


	@Override
	public void close() throws IOException {
		csvBeanReader.close();
	}


	/**
	 * @return The column names (i.e. the content of the first row).
     */
	public String[] getHeader() {
		return header;
	}
}


