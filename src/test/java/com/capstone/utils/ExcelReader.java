package com.capstone.utils;

import com.capstone.config.ConfigReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ExcelReader — Reads test data from TestData.xlsx using Apache POI.
 *
 * WHY APACHE POI: Instructor requirement — NO HARDCODING of test data.
 * All test inputs (email, password, note title, category, description)
 * live in an Excel file. Non-technical QA team members can update test
 * data without touching Java code.
 *
 * FORMAT: The Excel file has multiple sheets. Each sheet has a header row
 * (row 0) and data rows below it.
 *
 * RETURN FORMAT: List<Map<String,String>>
 *   Each Map represents one row: { "email" → "raunak@gmail.com", "password" → "Raunak@123" }
 *   Test steps access data like: row.get("email"), row.get("title")
 *
 * SHEETS IN TestData.xlsx:
 *   - LoginData     → email, password, expected_result
 *   - NotesData     → category, title, description, expected_result
 *   - NegativeData  → scenario, input, expected_error
 */
public class ExcelReader {

    private static final Logger log = LogManager.getLogger(ExcelReader.class);

    /**
     * Reads all data rows from a given sheet in TestData.xlsx.
     *
     * @param sheetName Name of the Excel sheet (e.g., "LoginData")
     * @return List of row maps: each map is { header → cell_value }
     */
    public static List<Map<String, String>> readSheet(String sheetName) {
        String filePath = ConfigReader.getTestDataFile();
        log.info("Reading Excel sheet '{}' from: {}", sheetName, filePath);

        List<Map<String, String>> data = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                log.error("Sheet '{}' not found in {}", sheetName, filePath);
                throw new RuntimeException("Sheet '" + sheetName + "' not found in Excel file: " + filePath);
            }

            // Row 0 is the header row
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new RuntimeException("Header row (row 0) is empty in sheet: " + sheetName);
            }

            int columnCount = headerRow.getLastCellNum();

            // Read each data row (starting from row 1)
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row dataRow = sheet.getRow(rowIndex);
                if (dataRow == null || isRowEmpty(dataRow)) {
                    continue; // Skip empty rows
                }

                Map<String, String> rowData = new HashMap<>();
                for (int colIndex = 0; colIndex < columnCount; colIndex++) {
                    String header = getCellValue(headerRow.getCell(colIndex));
                    String value  = getCellValue(dataRow.getCell(colIndex));
                    rowData.put(header, value);
                }
                data.add(rowData);
            }

            log.info("Read {} data rows from sheet '{}'", data.size(), sheetName);

        } catch (IOException e) {
            log.error("Failed to read Excel file '{}': {}", filePath, e.getMessage());
            throw new RuntimeException("Excel read failure: " + e.getMessage(), e);
        }

        return data;
    }

    /**
     * Reads a specific row from a sheet by row index (0-based, excluding header).
     *
     * @param sheetName Sheet name
     * @param rowIndex  0 = first data row (after header)
     * @return Map of { header → cell_value }
     */
    public static Map<String, String> readRow(String sheetName, int rowIndex) {
        List<Map<String, String>> allRows = readSheet(sheetName);
        if (rowIndex >= allRows.size()) {
            throw new RuntimeException("Row index " + rowIndex + " out of bounds for sheet '" + sheetName +
                    "' (has " + allRows.size() + " data rows)");
        }
        return allRows.get(rowIndex);
    }

    /**
     * Reads the first data row from a sheet.
     * Convenience for scenarios that use single-row test data.
     */
    public static Map<String, String> readFirstRow(String sheetName) {
        return readRow(sheetName, 0);
    }

    /**
     * Converts cell value to String regardless of cell type.
     * Handles: STRING, NUMERIC, BOOLEAN, BLANK, FORMULA.
     */
    private static String getCellValue(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:  return cell.getStringCellValue().trim();
            case NUMERIC: return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try { return cell.getStringCellValue().trim(); }
                catch (Exception e) { return String.valueOf(cell.getNumericCellValue()); }
            case BLANK:
            default: return "";
        }
    }

    /**
     * Checks if all cells in a row are blank (to skip empty rows).
     */
    private static boolean isRowEmpty(Row row) {
        for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String val = getCellValue(cell);
                if (!val.isEmpty()) return false;
            }
        }
        return true;
    }
}
