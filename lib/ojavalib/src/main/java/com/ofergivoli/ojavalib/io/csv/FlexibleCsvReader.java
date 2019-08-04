package com.ofergivoli.ojavalib.io.csv;

import com.google.common.base.Verify;
import com.ofergivoli.ojavalib.data_structures.map.SafeHashMap;
import com.ofergivoli.ojavalib.data_structures.map.SafeMap;
import com.ofergivoli.ojavalib.data_structures.set.SafeHashSet;
import com.ofergivoli.ojavalib.data_structures.set.SafeSet;
import com.ofergivoli.ojavalib.reflection.ReflectionUtils;
import com.ofergivoli.ojavalib.string.StringManager;
import org.apache.commons.lang3.ClassUtils;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Reads a CSV with a header in which a non-header row represents an object, but not all columns must represent fields.
 * The title of the columns (content of cell in first row) that represent fields must either:
 * - be identical to the field name.
 * - start with the field name followed by a separator.
 * Also, in case the user chooses so:
 *  - not all fields of the row-object must be represented by columns (and those that are not will remain with their
 *  default value).
 * - not all columns of the CSV must represent existing fields of the row-object.
 * A column representing a field of a primitive type may not contain empty cells (empty cells are considered those
 * containing only whitespaces).
 * A field of type {@link String} always gets set to a non-null value.
 * For non-primitive fields other then {@link String}, the column may contain empty cells representing the null value,
 * or equivalently, some specified string that represents the null value (e.g. "NA").
 * Boolean values can be represented in the csv as "true"/"yes"/"1" or "false"/"no"/"0".
 *
 *
 * Implementation note: remember that the {@link Nullable} annotation can't be seen during runtime, so we didn't define
 * a more restricted logic that uses this annotation.
 *
 *
 * @param <T> the type of the row-object represented by each CSV row.
 */
public class FlexibleCsvReader<T> implements Closeable {

    public static class InvalidContent extends RuntimeException{
        private static final long serialVersionUID = -2603134308715030711L;

        public InvalidContent(){
        }

        public InvalidContent(String s) {
            super(s);
        }
    }


    public static class InvalidRowObjectType extends RuntimeException {
        private static final long serialVersionUID = -7498333230832789640L;

        public InvalidRowObjectType() {
        }

        public InvalidRowObjectType(String s) {
            super(s);
        }
    }

    /**
     * See {@link #FlexibleCsvReader(Path, Class, boolean, boolean, boolean, String, boolean, String)}
     */
    private final @Nullable String stringRepresentingNull;

    private final String separator;
    private final Class<T> rowObjectClass;
    private final CsvWithHeaderParser csvParser;
    /**
     * Does not contain an entry for columns which are not representing any field.
     * Column index is zero-based.
     */
    private final SafeMap<Integer, Field> columnIndToField = new SafeHashMap<>();

    /**
     * Constructor for creating objects of type {@link #rowObjectClass}
     */
    private final Constructor<T> constructor;

    /**
     * Uses ';' as separator, and uses "NA" as a special string for representing the null value (for non-string
     * fields).
     * See {@link #FlexibleCsvReader(Path, Class, boolean, boolean, boolean, String, boolean, String)}
     */
    public FlexibleCsvReader(Path csv, boolean useExcelFormat, Class<T> rowObjectClass,
                             boolean ignoreUnmatchedColumnTitles, boolean ignoreUnmatchedFields,
                             boolean skipEmptyRows) {

        this(csv, rowObjectClass, useExcelFormat, ignoreUnmatchedColumnTitles, ignoreUnmatchedFields, ";",
                skipEmptyRows, "NA");
    }

    /**
     * Note: Sets the {@code accessible} flag of the fields & zero-arg constructor of {@link #rowObjectClass} to true.
     * @param stringRepresentingNull When not null, cells that their content is equal to this string will represent the
     *                               null value for non-string fields.
     * @param rowObjectClass The class that represents a row in the CSV file (i.e. the type of objects to create).
     *                       Must have a zero-parameters constructor.
     *                       The types of the fields to be set must be the ones handled by
     *                       {@link #parserCell(String, Class, String)}.
     *                       Fields & constructor may be private (and no setters are necessary).
     *                       May contain static fields (they are ignored by this class).
     * @param ignoreUnmatchedFields if true - unmatched row-object fields will get the default value.
     * @throws InvalidRowObjectType in case 'rowObjectClass' is invalid (contain an invalid field type or no
     *                                 zero-parameter constructor).
     */
    public FlexibleCsvReader(Path csv, Class<T> rowObjectClass, boolean useExcelFormat, boolean ignoreUnmatchedColumnTitles,
                             boolean ignoreUnmatchedFields, String separator, boolean skipEmptyRows,
                             @Nullable String stringRepresentingNull) {

        this.stringRepresentingNull = stringRepresentingNull;
        this.rowObjectClass = rowObjectClass;
        this.separator = separator;

        csvParser = new CsvWithHeaderParser(csv, useExcelFormat, skipEmptyRows);

        try {
            constructor = rowObjectClass.getConstructor();
        } catch (NoSuchMethodException e) {
           throw new InvalidRowObjectType("A zero-arg constructor must exist for class: " + rowObjectClass.getName());
        }
        constructor.setAccessible(true);


        SafeMap<String, Field> fieldNameToField = new SafeHashMap<>();
                ReflectionUtils.getAllFieldsOfClass(this.rowObjectClass, false, false).forEach(field->
                                Verify.verify(fieldNameToField.put(field.getName(), field) == null));

        SafeSet<String> fieldNamesNotYetMatchedToAColumn = new SafeHashSet<>(fieldNameToField.keySet());
        ArrayList<String> header = csvParser.getHeader();
        for (int i = 0; i < header.size(); i++) {
            String columnTitle = header.get(i);
            String fieldName = getPrefixUntilSeparator(columnTitle);
            if (fieldNamesNotYetMatchedToAColumn.safeContains(fieldName)) {
                columnIndToField.put(i, fieldNameToField.safeGet(fieldName));
                fieldNamesNotYetMatchedToAColumn.safeRemove(fieldName);
            } else {
                // no field with appropriate name was found.
                if (!ignoreUnmatchedColumnTitles)
                    throw new InvalidContent("Unmatched column title! In " + this.rowObjectClass.getName() + " there's no field named: " + fieldName);
            }
        }
        if (!ignoreUnmatchedFields && !fieldNamesNotYetMatchedToAColumn.isEmpty())
            throw new InvalidContent("The following fields of " + this.rowObjectClass.getName() + " were not matched by any column titles:\n" +
                    StringManager.collectionToStringWithNewlines(fieldNamesNotYetMatchedToAColumn));
    }


    /**
     * Sets the {@code accessible} flag of the accessed fields to true.
     * @return null if there are no more rows.
     * @throws InvalidContent if invalid content encountered in the csv.
     * @throws InvalidRowObjectType see {@link #parserCell(String, Class, String)}
     */
    public @Nullable
    T parseNextRow() {

        ArrayList<String> row = csvParser.readNextRow();
        if (row == null)
            return null;
        try {
            T result = constructor.newInstance();
            columnIndToField.forEach((columnInd,field)-> {
                String cell = row.get(columnInd);
                field.setAccessible(true);
                try {
                    if (!field.getType().equals(String.class) && isCellContentRepresentsNull(cell)) {
                        // we just let the field keep its null value.
                        if (field.getType().isPrimitive())
                            throw new InvalidContent("Can't assign a null value for primitive field: " + field.getName());
                    } else {
                        setFieldValue(result, field, cell);
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            });
            return result;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isCellContentRepresentsNull(String content) {
        return content.trim().isEmpty() || content.equals(stringRepresentingNull);
    }

    /**
     * @throws InvalidContent see {@link #parserCell(String, Class, String)}
     * @throws InvalidRowObjectType see {@link #parserCell(String, Class, String)}
     */
    private void setFieldValue(T object, Field field, String value) throws IllegalAccessException {
        field.set(object, parserCell(value, field.getType(), field.getName()));
    }

    /**
     * @return a value of type 'type' represented by 'cellContent'.
     * @throws InvalidContent if the cell content could not be parsed according to 'type'.
     * @throws InvalidRowObjectType if 'type' is invalid.
     */
    protected Object parserCell(String cellContent, Class<?> type, String fieldNameForErrorMessage) {
        try {
            if (ClassUtils.isAssignable(type, String.class, true)) {
                return cellContent;
            }
            if (ClassUtils.isAssignable(type, Integer.class, true)) {
                return Integer.parseInt(cellContent);
            }
            if (ClassUtils.isAssignable(type, Double.class, true)) {
                return Double.parseDouble(cellContent);
            }
            if (ClassUtils.isAssignable(type, Long.class, true)) {
                return Long.parseLong(cellContent);
            }
            if (ClassUtils.isAssignable(type, Boolean.class, true)) {
                switch (cellContent.toLowerCase()) {
                    case "true":
                    case "yes":
                    case "1":
                        return true;

                    case "false":
                    case "no":
                    case "0":
                        return false;
                }
                throw new InvalidContent("While parsing field \"" + fieldNameForErrorMessage +
                        "\": the following string could not be parsed as boolean: " + cellContent);
            }
        } catch (NumberFormatException e){
            throw new InvalidContent("While parsing field \"" + fieldNameForErrorMessage +
                    "\": NumberFormatException for cell content: \"" + cellContent + "\""
                    + "\nRequired type: " + type.getSimpleName());
        }
        throw new InvalidRowObjectType("Unsupported field type: " + type);
    }

    /**
     * @throws InvalidContent if invalid content encountered in the csv.
     * @throws InvalidRowObjectType see {@link #parserCell(String, Class, String)}
     */
    public List<T> readAll()
    {
        List<T> result = new LinkedList<>();
        T obj;
        while ((obj = parseNextRow()) != null)
            result.add(obj);
        return result;
    }

    /**
     * @return the entire 'title' if no separator is found.
     */
    private String getPrefixUntilSeparator(String title) {
        int ind = title.indexOf(separator);
        if (ind<0)
            return title;
        return title.substring(0, ind);
    }

    @Override
    public void close() throws IOException {
        csvParser.close();
    }


    /**
     * @see #readAll(Path, Class, boolean, boolean, boolean, boolean, String, String)
     * Uses ';' as seperator, and "NA" as 'stringRepresentingNull'.
     */
    public static <T> List<T> readAll(Path csv, Class<T> rowObjectClass,
                                      boolean useExcelFormat, boolean ignoreUnmatchedColumnTitles,
                                      boolean ignoreUnmatchedFields, boolean skipEmptyRows) {
        return readAll(csv, rowObjectClass, useExcelFormat, ignoreUnmatchedColumnTitles, ignoreUnmatchedFields,
                skipEmptyRows, ";", "NA");
    }

    /**
     * @see #FlexibleCsvReader(Path, Class, boolean, boolean, boolean, String, boolean, String)
     * @throws InvalidContent if invalid content encountered in the csv.
     * @throws InvalidRowObjectType in case 'rowObjectClass' is invalid (contain an invalid field type or no zero-parameter
     *                              constructor).
     */
    public static <T> List<T> readAll(Path csv, Class<T> rowObjectClass, boolean useExcelFormat, boolean ignoreUnmatchedColumnTitles,
                                      boolean ignoreUnmatchedFields, boolean skipEmptyRows, String separator,
                                      @Nullable String stringRepresentingNull) {

        return com.ofergivoli.ojavalib.io.IOUtils.calcAndClose(new FlexibleCsvReader<>(csv, rowObjectClass, useExcelFormat,
                        ignoreUnmatchedColumnTitles, ignoreUnmatchedFields, separator, skipEmptyRows,
                        stringRepresentingNull), FlexibleCsvReader::readAll);
    }

}
