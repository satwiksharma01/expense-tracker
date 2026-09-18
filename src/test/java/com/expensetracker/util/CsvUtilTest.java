package com.expensetracker.util;

import com.expensetracker.testing.Test;

import java.util.List;

import static com.expensetracker.testing.Assertions.assertEquals;
import static com.expensetracker.testing.Assertions.assertThrows;

public class CsvUtilTest {

    @Test
    public void roundTripsFieldsContainingCommasAndQuotes() {
        List<String> fields = List.of("1", "Dinner, drinks", "He said \"hi\"", "");
        String line = CsvUtil.toLine(fields);
        assertEquals("1,\"Dinner, drinks\",\"He said \"\"hi\"\"\",", line);
        assertEquals(fields, CsvUtil.parseLine(line));
    }

    @Test
    public void parsesEmptyFields() {
        assertEquals(List.of("", "", ""), CsvUtil.parseLine(",,"));
    }

    @Test
    public void rejectsUnterminatedQuote() {
        assertThrows(IllegalArgumentException.class, () -> CsvUtil.parseLine("1,\"never closed"));
    }

    @Test
    public void neutralizesSpreadsheetFormulas() {
        assertEquals("'=SUM(A1:A9)", CsvUtil.neutralizeFormula("=SUM(A1:A9)"));
        assertEquals("'@cmd", CsvUtil.neutralizeFormula("@cmd"));
        assertEquals("'+91 phone bill", CsvUtil.neutralizeFormula("+91 phone bill"));
        assertEquals("Lunch", CsvUtil.neutralizeFormula("Lunch"));
    }
}
