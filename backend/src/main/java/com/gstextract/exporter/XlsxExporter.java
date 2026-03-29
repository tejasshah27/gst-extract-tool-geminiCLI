package com.gstextract.exporter;

import com.gstextract.model.InwardInvoice;
import com.gstextract.model.ValidationError;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Component
public class XlsxExporter {

    public byte[] exportInvoicesAndErrors(List<InwardInvoice> invoices, List<ValidationError> errors) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            createInvoicesSheet(workbook, invoices);
            createErrorsSheet(workbook, errors);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private void createInvoicesSheet(Workbook workbook, List<InwardInvoice> invoices) {
        Sheet sheet = workbook.createSheet("Invoices");
        Row header = sheet.createRow(0);

        String[] columns = {
            "GSTIN", "Invoice Number", "Invoice Date", "Taxable Value", "Rate",
            "IGST", "CGST", "SGST", "UTGST", "Cess", "POS", "Amendment", "HSN/SAC",
            "Source", "ITC Eligible", "Reverse Charge"
        };

        for (int i = 0; i < columns.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(columns[i]);
            CellStyle style = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            style.setFont(font);
            cell.setCellStyle(style);
        }

        int rowNum = 1;
        for (InwardInvoice inv : invoices) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(inv.gstin());
            row.createCell(1).setCellValue(inv.invoiceNumber());
            if (inv.invoiceDate() != null) {
                row.createCell(2).setCellValue(inv.invoiceDate().toString());
            }
            row.createCell(3).setCellValue(inv.taxableValue() != null ? inv.taxableValue().doubleValue() : 0);
            row.createCell(4).setCellValue(inv.rate() != null ? inv.rate().doubleValue() : 0);
            row.createCell(5).setCellValue(inv.igst() != null ? inv.igst().doubleValue() : 0);
            row.createCell(6).setCellValue(inv.cgst() != null ? inv.cgst().doubleValue() : 0);
            row.createCell(7).setCellValue(inv.sgst() != null ? inv.sgst().doubleValue() : 0);
            row.createCell(8).setCellValue(inv.utgst() != null ? inv.utgst().doubleValue() : 0);
            row.createCell(9).setCellValue(inv.cess() != null ? inv.cess().doubleValue() : 0);
            row.createCell(10).setCellValue(inv.pos());
            row.createCell(11).setCellValue(inv.isAmendment() ? "Y" : "N");
            row.createCell(12).setCellValue(inv.hsnSac());
            row.createCell(13).setCellValue(inv.sourceType().toString());
            row.createCell(14).setCellValue(inv.itcEligible() ? "Y" : "N");
            row.createCell(15).setCellValue(inv.reverseCharge() ? "Y" : "N");
        }

        for (int i = 0; i < columns.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createErrorsSheet(Workbook workbook, List<ValidationError> errors) {
        Sheet sheet = workbook.createSheet("Errors");
        Row header = sheet.createRow(0);

        String[] columns = {"Sheet", "Row", "Field", "Reason", "Severity"};

        for (int i = 0; i < columns.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(columns[i]);
            CellStyle style = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            style.setFont(font);
            cell.setCellStyle(style);
        }

        int rowNum = 1;
        for (ValidationError err : errors) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(err.sheet());
            row.createCell(1).setCellValue(err.row());
            row.createCell(2).setCellValue(err.field());
            row.createCell(3).setCellValue(err.reason());
            row.createCell(4).setCellValue(err.severity().toString());
        }

        for (int i = 0; i < columns.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }
}
