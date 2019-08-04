package com.ofergivoli.ojavalib.io.csv;

import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;


public class CsvContentTest {

    @SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
    @Test
    public void createFromValuePairsTest(){

        List<List<CsvDataCell>> rows = new LinkedList<>();

        rows.add(Arrays.asList(new CsvDataCell("1", "a")));
        rows.add(Arrays.asList(new CsvDataCell("2", "b")));
        rows.add(Arrays.asList(new CsvDataCell("2", "bb"),new CsvDataCell("3", "c")));
        rows.add(Arrays.asList(new CsvDataCell("3", "cc")));


        // not using an explicit header:
        CsvContent expected = new CsvContent(Arrays.asList("1", "2", "3"));
        expected.addRow(Arrays.asList("a", "", ""));
        expected.addRow(Arrays.asList("", "b", ""));
        expected.addRow(Arrays.asList("", "bb", "c"));
        expected.addRow(Arrays.asList("", "", "cc"));
        assertEquals(expected, CsvContent.createFromValuePairs(null, rows));


        // using an explicit header:
        List<String> header = Arrays.asList("11", "22", "2");
        CsvContent expected2 = new CsvContent(Arrays.asList("11", "22", "2", "1", "3"));
        expected2.addRow(Arrays.asList("", "", "", "a", ""));
        expected2.addRow(Arrays.asList("", "", "b", "", ""));
        expected2.addRow(Arrays.asList("", "", "bb", "", "c"));
        expected2.addRow(Arrays.asList("", "", "", "", "cc"));
        assertEquals(expected2, CsvContent.createFromValuePairs(header, rows));

    }

}