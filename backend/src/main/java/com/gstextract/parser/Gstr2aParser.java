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

    private static class ParserContext {
        String lastGstin = "";
        String lastInvNum = "";
        LocalDate lastInvDate = null;
        String lastPos = "";
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
        Map<String, Map<String, List<String>>> gstr2aMappings = appConfig.getColumnMappings().get("gstr2a");
        if (gstr2aMappings == null) return;
        
        Map<String, List<String>> aliases = gstr2aMappings.get("b2b");
        Map<String, Integer> indices = SubTableParser.getColumnIndices(sheet, aliases);

        if (indices.isEmpty()) {
            errors.add(new ValidationError(sheetName, 0, "Sheet", "Could not find header columns for " + sheetName, Severity.ERROR));
            return;
        }

        int headerRowIndex = findHeaderRow(sheet, indices);
        if (headerRowIndex == -1) {
            errors.add(new ValidationError(sheetName, 0, "Sheet", "Could not find header row for " + sheetName, Severity.ERROR));
            return;
        }

        ParserContext context = new ParserContext();

        for (int i = headerRowIndex + 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null || isRowEmpty(row)) continue;

            try {
                InwardInvoice invoice = parseRow(row, indices, sourceType, sheetName, errors, context);
                if (invoice != null) {
                    invoices.add(invoice);
                }
            } catch (Exception e) {
                errors.add(new ValidationError(sheetName, i + 1, "Row", "Fatal error parsing row: " + e.getMessage(), Severity.ERROR));
            }
        }
    }

    private int findHeaderRow(Sheet sheet, Map<String, Integer> indices) {
        for (int i = 0; i < 20; i++) { // Check up to 20 rows for multi-line headers
            Row row = sheet.getRow(i);
            if (row == null) continue;
            for (Integer index : indices.values()) {
                Cell cell = row.getCell(index);
                if (cell != null && cell.getCellType() == CellType.STRING) {
                    return i;
                }
            }
        }
        return -1;
    }

    private boolean isRowEmpty(Row row) {
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                // Check if it's a numeric cell with a non-zero value or a string cell with text
                if (cell.getCellType() == CellType.NUMERIC && cell.getNumericCellValue() != 0) return false;
                if (cell.getCellType() == CellType.STRING && !cell.getStringCellValue().trim().isEmpty()) return false;
                if (cell.getCellType() == CellType.FORMULA) return false;
            }
        }
        return true;
    }

    private InwardInvoice parseRow(Row row, Map<String, Integer> indices, SourceType sourceType, String sheetName, List<ValidationError> errors, ParserContext context) {
        int rowNum = row.getRowNum() + 1;

        String gstin = getStringValue(row, indices, "gstin");
        String invNum = getStringValue(row, indices, "invoiceNumber");
        LocalDate invDate = getLocalDateValue(row, indices, "invoiceDate");
        String pos = getStringValue(row, indices, "pos");

        // Carry forward mechanism for merged/blank header cells
        if (gstin == null || gstin.isEmpty()) {
            gstin = context.lastGstin;
        } else {
            context.lastGstin = gstin;
        }

        if (invNum == null || invNum.isEmpty()) {
            invNum = context.lastInvNum;
        } else {
            context.lastInvNum = invNum;
        }

        if (invDate == null) {
            invDate = context.lastInvDate;
        } else {
            context.lastInvDate = invDate;
        }

        if (pos == null || pos.isEmpty()) {
            pos = context.lastPos;
        } else {
            context.lastPos = pos;
        }

        BigDecimal taxableValue = getBigDecimalValue(row, indices, "taxableValue");
        BigDecimal rate = getBigDecimalValue(row, indices, "rate");
        BigDecimal igst = getBigDecimalValue(row, indices, "igst");
        BigDecimal cgst = getBigDecimalValue(row, indices, "cgst");
        BigDecimal sgst = getBigDecimalValue(row, indices, "sgst");
        BigDecimal utgst = getBigDecimalValue(row, indices, "utgst");
        BigDecimal cess = getBigDecimalValue(row, indices, "cess");
        boolean isAmendment = sheetName.endsWith("A");
        String hsnSac = getStringValue(row, indices, "hsnSac");
        
        String itcEligibleStr = getStringValue(row, indices, "itcEligible");
        boolean itcEligible = "Y".equalsIgnoreCase(itcEligibleStr) || "Yes".equalsIgnoreCase(itcEligibleStr);

        String reverseChargeStr = getStringValue(row, indices, "reverseCharge");
        boolean reverseCharge = "Y".equalsIgnoreCase(reverseChargeStr) || "Yes".equalsIgnoreCase(reverseChargeStr);

        // Validation - only validate header info if it's new/provided, or if it's a standalone row
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
