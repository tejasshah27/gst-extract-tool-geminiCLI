package com.gstextract.parser;

import com.gstextract.config.AppConfig;
import com.gstextract.model.InwardInvoice;
import com.gstextract.model.Severity;
import com.gstextract.model.SourceType;
import com.gstextract.model.ValidationError;
import com.gstextract.validator.GstinValidator;
import com.gstextract.validator.InvoiceValidator;
import com.gstextract.validator.TaxAmountValidator;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class Gstr2aParser {

    private final AppConfig appConfig;

    @Autowired
    public Gstr2aParser(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    public ParsingResult parse(InputStream inputStream, SourceType sourceType) throws Exception {
        List<InwardInvoice> invoices = new ArrayList<>();
        List<ValidationError> errors = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                String sheetName = sheet.getSheetName();

                if (sheetName.equalsIgnoreCase("B2B") || sheetName.equalsIgnoreCase("CDNR")) {
                    parseSheet(sheet, sourceType, invoices, errors);
                }
            }
        }

        return new ParsingResult(invoices, errors);
    }

    private void parseSheet(Sheet sheet, SourceType sourceType, List<InwardInvoice> invoices, List<ValidationError> errors) {
        String sheetName = sheet.getSheetName();
        Map<String, List<String>> aliases = appConfig.getColumnMappings().get("gstr2a").get("b2b");
        Map<String, Integer> indices = SubTableParser.getColumnIndices(sheet, aliases);

        if (indices.isEmpty()) {
            errors.add(new ValidationError(sheetName, 0, "Sheet", "Could not find header columns for " + sheetName, Severity.ERROR));
            return;
        }

        // Find header row to start parsing from next row
        int headerRowIndex = findHeaderRow(sheet, indices);
        if (headerRowIndex == -1) {
            errors.add(new ValidationError(sheetName, 0, "Sheet", "Could not find header row for " + sheetName, Severity.ERROR));
            return;
        }

        for (int i = headerRowIndex + 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null || isRowEmpty(row)) continue;

            try {
                InwardInvoice invoice = parseRow(row, indices, sourceType, sheetName, errors);
                if (invoice != null) {
                    invoices.add(invoice);
                }
            } catch (Exception e) {
                errors.add(new ValidationError(sheetName, i + 1, "Row", "Fatal error parsing row: " + e.getMessage(), Severity.ERROR));
            }
        }
    }

    private int findHeaderRow(Sheet sheet, Map<String, Integer> indices) {
        for (int i = 0; i < 10; i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            for (Integer index : indices.values()) {
                Cell cell = row.getCell(index);
                if (cell != null && cell.getCellType() == CellType.STRING) {
                    // Check if any of our aliases match this cell (simple check)
                    return i;
                }
            }
        }
        return -1;
    }

    private boolean isRowEmpty(Row row) {
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK)
                return false;
        }
        return true;
    }

    private InwardInvoice parseRow(Row row, Map<String, Integer> indices, SourceType sourceType, String sheetName, List<ValidationError> errors) {
        int rowNum = row.getRowNum() + 1;

        String gstin = getStringValue(row, indices, "gstin");
        String invNum = getStringValue(row, indices, "invoiceNumber");
        LocalDate invDate = getLocalDateValue(row, indices, "invoiceDate");
        BigDecimal taxableValue = getBigDecimalValue(row, indices, "taxableValue");
        BigDecimal rate = getBigDecimalValue(row, indices, "rate");
        BigDecimal igst = getBigDecimalValue(row, indices, "igst");
        BigDecimal cgst = getBigDecimalValue(row, indices, "cgst");
        BigDecimal sgst = getBigDecimalValue(row, indices, "sgst");
        BigDecimal utgst = getBigDecimalValue(row, indices, "utgst");
        BigDecimal cess = getBigDecimalValue(row, indices, "cess");
        String pos = getStringValue(row, indices, "pos");
        boolean isAmendment = sheetName.endsWith("A");
        String hsnSac = getStringValue(row, indices, "hsnSac");
        
        String itcEligibleStr = getStringValue(row, indices, "itcEligible");
        boolean itcEligible = "Y".equalsIgnoreCase(itcEligibleStr) || "Yes".equalsIgnoreCase(itcEligibleStr);

        String reverseChargeStr = getStringValue(row, indices, "reverseCharge");
        boolean reverseCharge = "Y".equalsIgnoreCase(reverseChargeStr) || "Yes".equalsIgnoreCase(reverseChargeStr);

        // Validation
        GstinValidator.validate(gstin, sheetName, rowNum).ifPresent(errors::add);
        errors.addAll(InvoiceValidator.validate(invNum, invDate, sheetName, rowNum));
        errors.addAll(TaxAmountValidator.validate(taxableValue, rate, igst, cgst, sgst, utgst, appConfig.getTolerance().getTaxAmount(), pos, gstin, sheetName, rowNum));

        return new InwardInvoice(gstin, invNum, invDate, taxableValue, rate, igst, cgst, sgst, utgst, cess, pos, isAmendment, hsnSac, sourceType, itcEligible, reverseCharge);
    }

    private String getStringValue(Row row, Map<String, Integer> indices, String field) {
        Integer index = indices.get(field);
        if (index == null) return "";
        return ParseUtils.parseString(row.getCell(index));
    }

    private BigDecimal getBigDecimalValue(Row row, Map<String, Integer> indices, String field) {
        Integer index = indices.get(field);
        if (index == null) return BigDecimal.ZERO;
        return ParseUtils.parseBigDecimal(row.getCell(index));
    }

    private LocalDate getLocalDateValue(Row row, Map<String, Integer> indices, String field) {
        Integer index = indices.get(field);
        if (index == null) return null;
        return ParseUtils.parseLocalDate(row.getCell(index));
    }

    public static record ParsingResult(List<InwardInvoice> invoices, List<ValidationError> errors) {}
}
