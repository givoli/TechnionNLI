package ofergivoli.olib.io.csv;

import com.google.common.base.Verify;
import ofergivoli.olib.data_structures.map.SafeHashMap;
import ofergivoli.olib.data_structures.map.SafeMap;
import ofergivoli.olib.io.TextIO;
import ofergivoli.olib.io.WriterUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Represents the content of an entire CSV file that its first row is a header.
 */
public class CsvContent {

    private final ArrayList<String> header;

    /**
     * Each element is an array with the same size as {@link #header}.
     * Rows of the csv excluding the header.
     */
    private final List<ArrayList<String>> rows = new ArrayList<>();

    /**
     * @param header a shallow copy is kept.
     */
    public CsvContent(List<String> header) {
        Verify.verify(!header.isEmpty());
        this.header = new ArrayList<>(header);
    }

    public CsvContent(String... header) {
        this(Arrays.asList(header));
    }




    /**
     * @param row a shallow copy is kept.
     */
    public void addRow(List<String> row) {
        if (row.size() != header.size()) {
            throw new RuntimeException(String.format("row size (%d) must be equal to header size (%d)",
                    row.size(), header.size()));
        }
        rows.add(new ArrayList<>(row));
    }

    public void addRow (String... row) {
        addRow(Arrays.asList(row));
    }

    /**
     * The {@link Object#toString()} of each argument is used, except for null arguments that are converted
     * to an empty string.
     */
    public void convertObjectsToStringsAndAddRow (Object... row) {
        addRow(Arrays.stream(row)
                .map(obj-> obj == null ? "" : obj.toString())
                .collect(Collectors.toList()));
    }

    public ArrayList<String> getHeader() {
        return header;
    }

    public List<ArrayList<String>> getRows() {
        return rows;
    }

    public void writeEntireCsv(Writer writer) {
        writeCsvLine(writer,header);
        rows.forEach(row->writeCsvLine(writer,row));
    }

    public void writeEntireCsv(Path csvOutputFile) {
        try(Writer writer = TextIO.getStandardStreamWriter(csvOutputFile.toFile())){
            writeEntireCsv(writer);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void writeCsvLine(Writer writer, ArrayList<String> row){
        boolean first = true;
        for (String cell : row) {
            if (!first)
                WriterUtils.write(writer, ",");
            first = false;

            WriterUtils.write(writer, StringEscapeUtils.escapeCsv(cell));
        }
        WriterUtils.write(writer, "\n");
    }




    /**
     * @see #createFromValuePairs(List, List)
     */
    public static CsvContent createFromValuePairs(List<List<CsvDataCell>> rows)
    {
        return createFromValuePairs(null, rows);
    }

    /**
     * @param header if not null, all the header cells in 'header' will appear as the first header cells of the CSV, in
     *               the order they appear in this argument.
     * @param rows Elements don't necessarily include all header cells (resulting in empty cells).
     *             The order of header cells that are not in 'header' (all header cells in case 'header' is null) is
     *             determined "greedily" as we traverse 'rows'.
     */
    public static CsvContent createFromValuePairs(@Nullable List<String> header, List<List<CsvDataCell>> rows)
    {
        if (header == null)
            header = new LinkedList<>();
        else
            header = new LinkedList<>(header);

        List<String> headerExcludingProvidedHeaderCells = rows.stream()
                .flatMap(Collection::stream)
                .map(CsvDataCell::getHeaderCell)
                .distinct()
                .collect(Collectors.toCollection(LinkedList::new));
        headerExcludingProvidedHeaderCells.removeAll(header);
        header.addAll(headerExcludingProvidedHeaderCells);


        CsvContent result = new CsvContent(header);

        for (List<CsvDataCell> row : rows) {
            SafeMap<String,String> cellToContent = new SafeHashMap<>();
            row.forEach(pair->cellToContent.put(pair.getHeaderCell(),pair.getValue()));

            ArrayList<String> fullRow = new ArrayList<>();
            header.forEach(headerCell->{
                if (cellToContent.safeContainsKey(headerCell))
                    fullRow.add(cellToContent.safeGet(headerCell));
                else
                    fullRow.add("");
            });
            result.addRow(fullRow);
        }
        return result;
    }

    /**
     * @param columnIndex 0-based.
     * @param comparator the rows will be sorted in ascending order of the values this {@link Function} maps to.
     * @return a sorted shallow copy of this object.
     */
    public <T extends Comparable<? super T>> CsvContent getSortedShallowCopy(
            int columnIndex, Function<String, T> comparator) {

        List<ArrayList<String>>  sortedRows = rows.stream().
                sorted(Comparator.comparing(row->comparator.apply(row.get(columnIndex))))
                .collect(Collectors.toList());

        CsvContent result = new CsvContent(header);
        sortedRows.forEach(result::addRow);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CsvContent content = (CsvContent) o;
        return Objects.equals(header, content.header) &&
                Objects.equals(rows, content.rows);
    }

    @Override
    public int hashCode() {
        return Objects.hash(header, rows);
    }
}
