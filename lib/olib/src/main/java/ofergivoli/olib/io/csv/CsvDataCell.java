package ofergivoli.olib.io.csv;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 * A cell in a csv row (not the header row).
 */
public class CsvDataCell {

    private Pair<String,String> headerCellValuePair;

    public CsvDataCell(String headerCell, String value) {
        this.headerCellValuePair = new ImmutablePair<>(headerCell, value);
    }

    public String getHeaderCell(){
        return headerCellValuePair.getLeft();
    }

    public String getValue(){
        return headerCellValuePair.getRight();
    }
}
