package com.gstextract.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Random;

public class ExcelGenerator {

    private static final int ROW_COUNT = 100000;
    private static final Random RANDOM = new Random();

    public static void main(String[] args) throws IOException {
        String baseDir = "excels_large/";
        Files.createDirectories(Paths.get(baseDir));

        System.out.println("Starting generation of 100K entry files...");

        generateLargeGstr2a(baseDir + "GSTR2A_Apr2025.xlsx", "GSTR2A");
        generateLargeGstr2a(baseDir + "GSTR2B_Apr2025.xlsx", "GSTR2B");
        generateLargeGstr1(baseDir + "GSTR1_Apr2025.xlsx");
        generateLargePurchaseRegister(baseDir + "PurchaseReg_Apr2025.xlsx");
        generateGstr3b(baseDir + "GSTR3B_Apr2025.xlsx"); // Summary stays relatively small as per nature

        System.out.println("Excel files generated in " + baseDir);
    }

    private static void generateLargeGstr2a(String path, String type) throws IOException {
        System.out.println("Generating " + path + "...");
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
            Sheet b2bSheet = workbook.createSheet("B2B");
            String[] headers = {
                "GSTIN of Supplier", "Trade/Legal name of the Supplier", "Invoice number", "Invoice Date", 
                "Invoice Value", "Place of supply", "Supply Attracts Reverse Charge", "Rate", "Taxable Value", 
                "Integrated Tax", "Central Tax", "State/UT Tax"
            };

            Row headerRow = b2bSheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            for (int i = 1; i <= ROW_COUNT; i++) {
                Row row = b2bSheet.createRow(i);
                row.createCell(0).setCellValue("27AAAAA" + String.format("%04d", i % 10000) + "A1Z5");
                row.createCell(1).setCellValue("Supplier " + (i % 100));
                row.createCell(2).setCellValue("INV-" + type + "-" + String.format("%06d", i));
                row.createCell(3).setCellValue("01-04-2025");
                row.createCell(4).setCellValue(1180.00);
                row.createCell(5).setCellValue("27-Maharashtra");
                row.createCell(6).setCellValue("N");
                row.createCell(7).setCellValue(18.0);
                row.createCell(8).setCellValue(1000.00);
                row.createCell(9).setCellValue(0.00);
                row.createCell(10).setCellValue(90.00);
                row.createCell(11).setCellValue(90.00);
                
                if (i % 10000 == 0) System.out.println(type + " progress: " + i + " rows");
            }

            workbook.createSheet("CDNR");

            try (FileOutputStream fileOut = new FileOutputStream(path)) {
                workbook.write(fileOut);
            }
            workbook.dispose();
        }
    }

    private static void generateLargeGstr1(String path) throws IOException {
        System.out.println("Generating " + path + "...");
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
            Sheet b2bSheet = workbook.createSheet("B2B");
            String[] headers = {
                "GSTIN/UIN of Recipient", "Receiver Name", "Invoice Number", "Invoice Date", 
                "Invoice Value", "Place Of Supply", "Reverse Charge", "Invoice Type", "Rate", "Taxable Value", 
                "Integrated Tax", "Central Tax", "State/UT Tax", "Cess Amount"
            };

            Row headerRow = b2bSheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            for (int i = 1; i <= ROW_COUNT; i++) {
                Row row = b2bSheet.createRow(i);
                row.createCell(0).setCellValue("29BBBBB" + String.format("%04d", i % 10000) + "B1Z5");
                row.createCell(1).setCellValue("Customer " + (i % 100));
                row.createCell(2).setCellValue("S-INV-" + String.format("%06d", i));
                row.createCell(3).setCellValue("10-04-2025");
                row.createCell(4).setCellValue(1180.00);
                row.createCell(5).setCellValue("29-Karnataka");
                row.createCell(6).setCellValue("N");
                row.createCell(7).setCellValue("Regular");
                row.createCell(8).setCellValue(18.0);
                row.createCell(9).setCellValue(1000.00);
                row.createCell(10).setCellValue(0.00);
                row.createCell(11).setCellValue(90.00);
                row.createCell(12).setCellValue(90.00);
                
                if (i % 10000 == 0) System.out.println("GSTR1 progress: " + i + " rows");
            }

            workbook.createSheet("CDNR");

            try (FileOutputStream fileOut = new FileOutputStream(path)) {
                workbook.write(fileOut);
            }
            workbook.dispose();
        }
    }

    private static void generateLargePurchaseRegister(String path) throws IOException {
        System.out.println("Generating " + path + "...");
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
            Sheet sheet = workbook.createSheet("PurchaseRegister");
            String[] headers = {
                "Supplier GSTIN", "Supplier Name", "Invoice No", "Date", "Taxable Value", "IGST", "CGST", "SGST"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            for (int i = 1; i <= ROW_COUNT; i++) {
                Row row = sheet.createRow(i);
                row.createCell(0).setCellValue("27AAAAA" + String.format("%04d", i % 10000) + "A1Z5");
                row.createCell(1).setCellValue("Supplier " + (i % 100));
                row.createCell(2).setCellValue("INV-GSTR2A-" + String.format("%06d", i));
                row.createCell(3).setCellValue("01-04-2025");
                row.createCell(4).setCellValue(1000.00);
                row.createCell(5).setCellValue(0.00);
                row.createCell(6).setCellValue(90.00);
                row.createCell(7).setCellValue(90.00);
                
                if (i % 10000 == 0) System.out.println("PurchaseRegister progress: " + i + " rows");
            }

            try (FileOutputStream fileOut = new FileOutputStream(path)) {
                workbook.write(fileOut);
            }
            workbook.dispose();
        }
    }

    private static void generateGstr3b(String path) throws IOException {
        System.out.println("Generating " + path + " (Summary version)...");
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
            Sheet summarySheet = workbook.createSheet("Summary");
            Row row0 = summarySheet.createRow(0);
            row0.createCell(0).setCellValue("3.1 Details of Outward Supplies");
            Row header31 = summarySheet.createRow(1);
            header31.createCell(0).setCellValue("Nature of Supplies");
            header31.createCell(1).setCellValue("Total Taxable Value");
            header31.createCell(2).setCellValue("Integrated Tax");

            Row data31 = summarySheet.createRow(2);
            data31.createCell(0).setCellValue("(a) Outward taxable supplies");
            data31.createCell(1).setCellValue(1000000.00);
            data31.createCell(2).setCellValue(180000.00);

            try (FileOutputStream fileOut = new FileOutputStream(path)) {
                workbook.write(fileOut);
            }
            workbook.dispose();
        }
    }
}
