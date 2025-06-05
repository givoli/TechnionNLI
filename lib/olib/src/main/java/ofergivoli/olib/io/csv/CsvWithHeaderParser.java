package ofergivoli.olib.io.csv;


import com.google.common.base.Verify;
import ofergivoli.olib.io.IOUtils;
import ofergivoli.olib.io.TextIO;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * A simple parser for CSV files that their first row is a header row.
 */
public class CsvWithHeaderParser implements Closeable {

    private final CSVParser parser;
    private final Iterator<CSVRecord> iterator;
    private final ArrayList<String> header;
    private final boolean skipEmptyRows;

    /**
     * Opens the csv in UTF-8 encoding, and reads the first row (header).
     * @param useExcelFormat if false, uses the RFC4180 format (which is the most "standard" format).
     */
    public CsvWithHeaderParser(Path csv, boolean useExcelFormat, boolean skipEmptyRows) {
        this.skipEmptyRows = skipEmptyRows;
        CSVFormat format = useExcelFormat ? CSVFormat.EXCEL : CSVFormat.RFC4180;
        try {
            Reader reader = TextIO.getStandardStreamReader(csv.toFile());
            parser = new CSVParser(reader, format);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        iterator = parser.iterator();
        header = readHeader();
    }

    public ArrayList<String> getHeader() {
        return header;
    }

    /**
     * @return null in case there are no more rows to read. Otherwise, an array the size of {@link #header}.
     */
    public @Nullable ArrayList<String> readNextRow() {
        while (true) { // as long as we keep skipping empty lines:
            if (!iterator.hasNext())
                return null;
            CSVRecord csvRecord = iterator.next();
            ArrayList<String> result = new ArrayList<>(header.size());
            int elementsToRead = csvRecord.size();
            if (elementsToRead > header.size())
                throw new RuntimeException("Row contains more cells than the header");
            for (int i = 0; i<elementsToRead; i++) {
                result.add(csvRecord.get(i));
            }
            // in case there were empty cells at the end of the row:
            for (int i = elementsToRead; i < header.size(); i++)
                result.add("");
            if (skipEmptyRows && isRowEmpty(result))
                continue;
            return result;
        }
    }

    private boolean isRowEmpty(ArrayList<String> result) {
        return result.stream().allMatch(String::isEmpty);
    }

    private ArrayList<String> readHeader() {
        Verify.verify(header == null);
        Verify.verify(iterator.hasNext());
        CSVRecord csvRecord = iterator.next();
        ArrayList<String> result = new ArrayList<>(csvRecord.size());
        for (int i = 0; i<csvRecord.size(); i++) {
            result.add(csvRecord.get(i));
        }
        return result;
    }

    @Override
    public void close() throws IOException {
        parser.close();
    }

    public CsvContent readCsv(Path csv, boolean useExcelFormat) {
        return IOUtils.calcAndClose(new CsvWithHeaderParser(csv, useExcelFormat, skipEmptyRows), parser-> {
            CsvContent result = new CsvContent(parser.getHeader());
            ArrayList<String> row;
            while ((row = parser.readNextRow()) != null)
                result.addRow(row);
            return result;
        });
    }
}
