package com.ofergivoli.ojavalib.io.csv.supercsv;


import com.ofergivoli.ojavalib.exceptions.UncheckedFileNotFoundException;
import com.ofergivoli.ojavalib.io.TextIO;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvBeanWriter;
import org.supercsv.io.ICsvBeanWriter;
import org.supercsv.prefs.CsvPreference;

import java.io.*;
import java.util.Optional;

/**
 * Writes a list of JavaBeans (all of the same type) to CSV (each JavaBean is represented by a single row).
 * Each column name (a.k.a "header", i.e. first row cell) must be identical to a field name of the JavaBeans. 
 * @param <T> The type of the JavaBean each row represents.
 *     IMPORTANT: the class type of T must be accessible (according to the modifiers) to org.supercsv.io.CsvBeanWriter.
 *     Unimportant note: If I'll ever ever want to modify the supercsv lib to fix this, I should add a call to
 *     		{@link java.lang.reflect.Method#setAccessible(boolean)} with value 'true',
 */
public class  CsvWriter<T> implements Closeable {
	
	
	private CsvSettings settings;
	
	private ICsvBeanWriter csvBeanWriter;
	

	// The first row of the CSV.
	private String[] header;
	 
	 
	/**
	 * CSV header (first row) is written by this constructor. 
	 * @param outputFile In "standard" encoding (see documentation in {@link TextIO}).
	 */
	public CsvWriter(CsvSettings settings, File outputFile) {		
		try {
			OutputStreamWriter sw = TextIO.getStandardStreamWriter(new FileOutputStream(outputFile));
			init(settings,sw);
		} catch (FileNotFoundException e) {
			throw new UncheckedFileNotFoundException(e);
		}
	}

	/**
	 * @param objType The type of the object written.
     */
	public CsvWriter(Class<T> objType, File outputFile) {
		this(CsvSettings.createFromDeclaredFieldsOfClass(objType),outputFile);
	}
	

			
 
	/**
	 * See {@link #CsvWriter(CsvSettings, File)} documentation.
	 * @param writer - Will be closed when this object is closed.
	 */
	public CsvWriter(CsvSettings settings, Writer writer) {
		init(settings,writer);
	}
	
	/**
	 * @param writer - Will be closed when this object is closed.
	 */
	private void init(CsvSettings settings, Writer writer) {
		this.settings = settings;
		
		csvBeanWriter = new CsvBeanWriter(writer, CsvPreference.STANDARD_PREFERENCE);
		header = settings.getColumnNames();
		
		try {
			csvBeanWriter.writeHeader( header );
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * @param obj Its type must match the given settings.
	 *
	 */
	public void writeObject(T obj, boolean flush) {
		try {
			Optional<CellProcessor[]> cellProcessors = settings.getSuperCsvProcessorsForWriting();
			if (cellProcessors.isPresent())
				csvBeanWriter.write(obj, header, cellProcessors.get());
			else
				csvBeanWriter.write(obj, header, getCellProcessorsFromHeader());
			if (flush)
				csvBeanWriter.flush();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

	}

	
	public void writeAllObjects(Iterable<T> objects) {
		for (T o : objects)
			writeObject(o, false);
		try {
			csvBeanWriter.flush();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	
	public static <L> void writeAllObjectsToFile(CsvSettings settings, File outputFile, Iterable<L> objects) { 
		try (CsvWriter<L> writer = new CsvWriter<>(settings,outputFile)) {
			writer.writeAllObjects(objects);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
	

	@Override
	public void close() throws IOException {
		csvBeanWriter.close();
	}

	private CellProcessor[] getCellProcessorsFromHeader() {
		CellProcessor[] result = new CellProcessor[header.length];
		for (int i=0; i<header.length; i++)
			result[i] = new org.supercsv.cellprocessor.Optional();
		return result;
	}
}
