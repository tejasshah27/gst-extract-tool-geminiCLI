package com.gstextract.parser;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class ParseUtils {

    public static BigDecimal parseBigDecimal(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) return BigDecimal.ZERO;
        if (cell.getCellType() == CellType.NUMERIC) {
            return BigDecimal.valueOf(cell.getNumericCellValue());
        }
        if (cell.getCellType() == CellType.STRING) {
            String val = cell.getStringCellValue().replace(",", "").trim();
            if (val.isEmpty()) return BigDecimal.ZERO;
            try {
                return new BigDecimal(val);
            } catch (NumberFormatException e) {
                return BigDecimal.ZERO;
            }
        }
        return BigDecimal.ZERO;
    }

    public static String parseString(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) return "";
        if (cell.getCellType() == CellType.STRING) return cell.getStringCellValue().trim();
        if (cell.getCellType() == CellType.NUMERIC) {
            // Check if it's a date cell that we mistakenly called parseString on
            if (DateUtil.isCellDateFormatted(cell)) {
                return parseLocalDate(cell).toString();
            }
            // For numbers like GSTIN if stored as numeric (unlikely but safe)
            BigDecimal val = BigDecimal.valueOf(cell.getNumericCellValue());
            if (val.scale() > 0 && val.signum() != 0) {
                try {
                    return val.stripTrailingZeros().toPlainString();
                } catch (Exception e) {
                    return val.toPlainString();
                }
            }
            return val.toPlainString();
        }
        return "";
    }

    public static LocalDate parseLocalDate(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) return null;
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        if (cell.getCellType() == CellType.STRING) {
            String val = cell.getStringCellValue().trim();
            // Try common formats
            String[] formats = {"dd/MM/yyyy", "dd-MM-yyyy", "yyyy-MM-dd", "MM/yyyy"};
            for (String format : formats) {
                try {
                    if (format.equals("MM/yyyy")) {
                        // For return period like 04/2025
                        return LocalDate.parse("01/" + val, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                    }
                    return LocalDate.parse(val, DateTimeFormatter.ofPattern(format));
                } catch (Exception ignored) {}
            }
        }
        return null;
    }
}
