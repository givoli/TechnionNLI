package com.ofergivoli.ojavalib.io.csv.supercsv;

import org.supercsv.cellprocessor.ift.CellProcessor;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Represent settings that define how a given JavaBean type can be read/written from/to
 * a CSV file.
 * Each column name (header, i.e., first row cell) must be identical to a field name of the JavaBean.
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType") //TODO: take care of
public class CsvSettings {

	private String[] columnNames = new String[0];


    /**
     * @param columnNames The order of elements defines the order of columns in the CSV file.
     * This is used only when writing to a CSV file (not when reading), so you can pass a null if you're sure
     * you won't be writing. TODO: we use it only for writing (call it CsvWriterSettings), and move login into CsvWriter itself. (and delete this class)
     * The name of a column must be identical to a name of a field.
     */
	public CsvSettings(Collection<String> columnNames) {
		this.columnNames = columnNames.toArray(new String[0]);
	}

	
	/**
	 * The "processor" at index i in the returned array is defines the way strings in column i+1 are 
	 * parsed.
	 * If this is an empty value (default) then no special processing is done when reading.
	 */
	private  Optional<CellProcessor[]> superCsvProcessorsForReading = Optional.empty();

	
	/**
	 * If this is an empty value (default)  then no special processing is done when writing.
	 */
	private Optional<CellProcessor[]> superCsvProcessorsForWriting = Optional.empty();


	String[] getColumnNames() {
		return columnNames;
	}


	Optional<CellProcessor[]> getSuperCsvProcessorsForReading() {
		return superCsvProcessorsForReading;
	}


	public CsvSettings setSuperCsvProcessorsForReading(
			Optional<CellProcessor[]> superCsvProcessorsForReading) {
		this.superCsvProcessorsForReading = superCsvProcessorsForReading;
		return this;
	}


	Optional<CellProcessor[]> getSuperCsvProcessorsForWriting() {
		return superCsvProcessorsForWriting;
	}


	public CsvSettings setSuperCsvProcessorsForWriting(
			Optional<CellProcessor[]> superCsvProcessorsForWriting) {
		this.superCsvProcessorsForWriting = superCsvProcessorsForWriting;
		
		return this;
	}


	static <T> CsvSettings createFromDeclaredFieldsOfClass(Class<T> clazz) {

		List<String> fields = Arrays.stream(clazz.getDeclaredFields())
				.map(Field::getName)
				.collect(Collectors.toList());

		return new CsvSettings(fields);
	}
}
