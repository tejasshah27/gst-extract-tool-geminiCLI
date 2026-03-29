package com.gstextract.parser;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

public class ParseUtilsTest {

    @Test
    public void testParseBigDecimalNumeric() {
        Cell cell = Mockito.mock(Cell.class);
        when(cell.getCellType()).thenReturn(CellType.NUMERIC);
        when(cell.getNumericCellValue()).thenReturn(123.45);
        
        BigDecimal result = ParseUtils.parseBigDecimal(cell);
        assertEquals(new BigDecimal("123.45"), result);
    }

    @Test
    public void testParseBigDecimalString() {
        Cell cell = Mockito.mock(Cell.class);
        when(cell.getCellType()).thenReturn(CellType.STRING);
        when(cell.getStringCellValue()).thenReturn("1,234.56");
        
        BigDecimal result = ParseUtils.parseBigDecimal(cell);
        assertEquals(new BigDecimal("1234.56"), result);
    }

    @Test
    public void testParseStringNumeric() {
        Cell cell = Mockito.mock(Cell.class);
        when(cell.getCellType()).thenReturn(CellType.NUMERIC);
        when(cell.getNumericCellValue()).thenReturn(123.0);
        
        String result = ParseUtils.parseString(cell);
        assertEquals("123", result);
    }

    @Test
    public void testParseLocalDateString() {
        Cell cell = Mockito.mock(Cell.class);
        when(cell.getCellType()).thenReturn(CellType.STRING);
        when(cell.getStringCellValue()).thenReturn("25/12/2023");
        
        LocalDate result = ParseUtils.parseLocalDate(cell);
        assertEquals(LocalDate.of(2023, 12, 25), result);
    }

    @Test
    public void testParseLocalDateReturnPeriod() {
        Cell cell = Mockito.mock(Cell.class);
        when(cell.getCellType()).thenReturn(CellType.STRING);
        when(cell.getStringCellValue()).thenReturn("04/2025");
        
        LocalDate result = ParseUtils.parseLocalDate(cell);
        assertEquals(LocalDate.of(2025, 4, 1), result);
    }
}
