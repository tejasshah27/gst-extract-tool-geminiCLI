package com.gstextract.parser;

import org.apache.poi.ss.usermodel.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SubTableParser {

    /**
     * Finds column indices based on aliases.
     * Searches the first few rows for the first row that matches at least one of the aliases.
     */
    public static Map<String, Integer> getColumnIndices(Sheet sheet, Map<String, List<String>> fieldAliases) {
        Map<String, Integer> indices = new HashMap<>();

        // Search first 5 rows for header
        for (int i = 0; i < Math.min(sheet.getLastRowNum() + 1, 10); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            boolean foundSomething = false;
            for (Cell cell : row) {
                if (cell.getCellType() == CellType.STRING) {
                    String header = cell.getStringCellValue().trim();
                    for (Map.Entry<String, List<String>> entry : fieldAliases.entrySet()) {
                        String fieldName = entry.getKey();
                        List<String> aliases = entry.getValue();
                        for (String alias : aliases) {
                            if (header.equalsIgnoreCase(alias)) {
                                indices.put(fieldName, cell.getColumnIndex());
                                foundSomething = true;
                                break;
                            }
                        }
                    }
                }
            }
            if (foundSomething) {
                // If we found some columns, check if we found most of them
                if (indices.size() >= fieldAliases.size() / 2) {
                    return indices;
                }
            }
        }
        return indices;
    }

    public static int findDataStartRow(Sheet sheet, Map<String, Integer> indices) {
        if (indices.isEmpty()) return -1;
        Integer firstColIndex = indices.values().iterator().next();

        // Start searching from the row after where we might have found headers
        for (int i = 0; i < Math.min(sheet.getLastRowNum() + 1, 15); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            Cell cell = row.getCell(firstColIndex);
            if (cell != null && cell.getCellType() != CellType.BLANK && cell.getCellType() != CellType.FORMULA) {
                // Heuristic: If it's not a header row (e.g., doesn't contain aliases), it's data
                // But wait, the header row IS what we found in getColumnIndices.
                // So data should start AFTER the header row.
                // Let's refine this: data start row is the first row after headers that looks like data.
            }
        }

        // Simpler approach for now: header row + 1
        // (This might need refinement for merged headers)
        return -1; // To be implemented properly in actual parser
    }
}
