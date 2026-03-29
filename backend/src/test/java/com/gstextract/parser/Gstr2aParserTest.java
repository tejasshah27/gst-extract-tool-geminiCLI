package com.gstextract.parser;

import com.gstextract.config.AppConfig;
import com.gstextract.model.SourceType;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

public class Gstr2aParserTest {

    @Mock
    private AppConfig appConfig;

    private Gstr2aParser parser;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        
        // Mock AppConfig structure
        AppConfig.Tolerance tolerance = new AppConfig.Tolerance();
        tolerance.setTaxAmount(new BigDecimal("0.5"));
        when(appConfig.getTolerance()).thenReturn(tolerance);

        Map<String, Map<String, Map<String, List<String>>>> mappings = new HashMap<>();
        Map<String, Map<String, List<String>>> gstr2aMapping = new HashMap<>();
        Map<String, List<String>> b2bMapping = new HashMap<>();
        
        b2bMapping.put("gstin", Arrays.asList("GSTIN of supplier"));
        b2bMapping.put("invoiceNumber", Arrays.asList("Invoice number"));
        b2bMapping.put("invoiceDate", Arrays.asList("Invoice date"));
        b2bMapping.put("taxableValue", Arrays.asList("Taxable value"));
        b2bMapping.put("rate", Arrays.asList("Rate"));
        b2bMapping.put("igst", Arrays.asList("Integrated Tax"));
        b2bMapping.put("cgst", Arrays.asList("Central Tax"));
        b2bMapping.put("sgst", Arrays.asList("State/UT Tax"));
        b2bMapping.put("pos", Arrays.asList("Place of Supply"));
        
        gstr2aMapping.put("b2b", b2bMapping);
        mappings.put("gstr2a", gstr2aMapping);
        when(appConfig.getColumnMappings()).thenReturn(mappings);

        parser = new Gstr2aParser(appConfig);
    }

    @Test
    public void testParseB2BSheet() throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("B2B");
        
        // Header row
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("GSTIN of supplier");
        header.createCell(1).setCellValue("Invoice number");
        header.createCell(2).setCellValue("Invoice date");
        header.createCell(3).setCellValue("Taxable value");
        header.createCell(4).setCellValue("Rate");
        header.createCell(5).setCellValue("Integrated Tax");
        header.createCell(6).setCellValue("Central Tax");
        header.createCell(7).setCellValue("State/UT Tax");
        header.createCell(8).setCellValue("Place of Supply");

        // Data row
        Row data = sheet.createRow(1);
        data.createCell(0).setCellValue("27AAACR1234A1Z3"); // Valid GSTIN (checksum is 3)
        data.createCell(1).setCellValue("INV-001");
        data.createCell(2).setCellValue("01/04/2025");
        data.createCell(3).setCellValue(1000.0);
        data.createCell(4).setCellValue(18.0);
        data.createCell(5).setCellValue(0.0);
        data.createCell(6).setCellValue(90.0);
        data.createCell(7).setCellValue(90.0);
        data.createCell(8).setCellValue("27");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        
        InputStream in = new ByteArrayInputStream(out.toByteArray());
        Gstr2aParser.ParsingResult result = parser.parse(in, SourceType.GSTR2A);

        assertEquals(1, result.invoices().size());
        assertEquals(0, result.errors().size());
        assertEquals("27AAACR1234A1Z3", result.invoices().get(0).gstin());
        assertEquals("INV-001", result.invoices().get(0).invoiceNumber());
        assertEquals(new BigDecimal("1000.0"), result.invoices().get(0).taxableValue());
    }

    @Test
    public void testParseSheetWithErrors() throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("B2B");
        
        // Header row
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("GSTIN of supplier");
        header.createCell(1).setCellValue("Invoice number");
        header.createCell(2).setCellValue("Invoice date");
        header.createCell(3).setCellValue("Taxable value");
        header.createCell(4).setCellValue("Rate");

        // Data row with invalid GSTIN (15 chars but wrong format)
        Row data = sheet.createRow(1);
        data.createCell(0).setCellValue("27AAACR1234A1!1");
        data.createCell(1).setCellValue("INV-001");
        data.createCell(2).setCellValue("01/04/2025");
        data.createCell(3).setCellValue(100.0);
        data.createCell(4).setCellValue(18.0);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        
        InputStream in = new ByteArrayInputStream(out.toByteArray());
        Gstr2aParser.ParsingResult result = parser.parse(in, SourceType.GSTR2A);

        assertEquals(1, result.invoices().size());
        assertFalse(result.errors().isEmpty());
        assertTrue(result.errors().stream().anyMatch(e -> e.reason().contains("Invalid GSTIN format")));
    }
}
