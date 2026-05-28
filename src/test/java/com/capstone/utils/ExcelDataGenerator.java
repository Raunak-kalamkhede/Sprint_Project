package com.capstone.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * ExcelDataGenerator — Generates TestData.xlsx with pre-populated test data.
 *
 * WHY: TestData.xlsx is a binary file that cannot be committed as plain text.
 * This utility generates the Excel file programmatically at project setup time.
 *
 * HOW TO RUN:
 *   Right-click ExcelDataGenerator.java in IDE → Run main()
 *   OR: mvn exec:java -Dexec.mainClass="com.capstone.utils.ExcelDataGenerator"
 *
 * SHEETS GENERATED:
 *   - LoginData     → Valid and invalid login credentials
 *   - NotesData     → Test notes for each category
 *   - NegativeData  → Invalid inputs for negative test scenarios
 */
public class ExcelDataGenerator {

    private static final Logger log = LogManager.getLogger(ExcelDataGenerator.class);
    private static final String OUTPUT_PATH = "src/test/resources/testdata/TestData.xlsx";

    public static void main(String[] args) {
        generate();
    }

    /**
     * Generates the TestData.xlsx file with all test data sheets.
     */
    public static void generate() {
        log.info("Generating TestData.xlsx...");

        try {
            // Ensure testdata directory exists
            Files.createDirectories(Paths.get("src/test/resources/testdata"));

            XSSFWorkbook workbook = new XSSFWorkbook();

            // Create cell styles
            CellStyle headerStyle  = createHeaderStyle(workbook);
            CellStyle dataStyle    = createDataStyle(workbook);
            CellStyle passStyle    = createPassStyle(workbook);
            CellStyle failStyle    = createFailStyle(workbook);

            // Sheet 1: LoginData
            createLoginDataSheet(workbook, headerStyle, dataStyle, passStyle, failStyle);

            // Sheet 2: NotesData
            createNotesDataSheet(workbook, headerStyle, dataStyle);

            // Sheet 3: NegativeData
            createNegativeDataSheet(workbook, headerStyle, dataStyle, failStyle);

            // Write to file
            try (FileOutputStream fos = new FileOutputStream(OUTPUT_PATH)) {
                workbook.write(fos);
            }
            workbook.close();

            log.info("TestData.xlsx generated successfully at: {}", OUTPUT_PATH);
            System.out.println("✅ TestData.xlsx generated: " + OUTPUT_PATH);

        } catch (IOException e) {
            log.error("Failed to generate Excel: {}", e.getMessage());
            throw new RuntimeException("Excel generation failed: " + e.getMessage(), e);
        }
    }

    // ==========================================
    // SHEET CREATORS
    // ==========================================

    private static void createLoginDataSheet(XSSFWorkbook wb, CellStyle hStyle,
                                              CellStyle dStyle, CellStyle pStyle, CellStyle fStyle) {
        XSSFSheet sheet = wb.createSheet("LoginData");
        sheet.setColumnWidth(0, 8000);   // email
        sheet.setColumnWidth(1, 5000);   // password
        sheet.setColumnWidth(2, 5000);   // expected_result
        sheet.setColumnWidth(3, 8000);   // description

        // Header row
        String[] headers = {"email", "password", "expected_result", "description"};
        createHeaderRow(sheet, headers, hStyle);

        // Data rows
        Object[][] data = {
            {"raunak@gmail.com",      "Raunak@123",     "success", "Valid credentials — should login successfully"},
            {"invalid@test.com",      "wrongpassword",  "failure", "Invalid email — login should fail"},
            {"raunak@gmail.com",      "WrongPass123",   "failure", "Valid email, wrong password"},
            {"",                      "",               "failure", "Empty credentials — should show validation error"},
            {"notregistered@xyz.com", "Password123",    "failure", "Unregistered email"},
            {"bad-email-format",      "Raunak@123",     "failure", "Invalid email format"},
        };
        fillDataRows(sheet, data, dStyle);

        // Colour expected_result column
        for (int i = 1; i < data.length + 1; i++) {
            Row row = sheet.getRow(i);
            if (row != null) {
                Cell cell = row.getCell(2);
                if (cell != null) {
                    cell.setCellStyle("success".equals(cell.getStringCellValue()) ? pStyle : fStyle);
                }
            }
        }
    }

    private static void createNotesDataSheet(XSSFWorkbook wb, CellStyle hStyle, CellStyle dStyle) {
        XSSFSheet sheet = wb.createSheet("NotesData");
        sheet.setColumnWidth(0, 4000);   // category
        sheet.setColumnWidth(1, 8000);   // title
        sheet.setColumnWidth(2, 12000);  // description
        sheet.setColumnWidth(3, 5000);   // expected_result
        sheet.setColumnWidth(4, 5000);   // tag

        String[] headers = {"category", "title", "description", "expected_result", "tag"};
        createHeaderRow(sheet, headers, hStyle);

        Object[][] data = {
            {"Home",     "Home Renovation Plans",    "Fix kitchen tiles and repaint the living room walls",        "success", "@ui"},
            {"Work",     "Sprint Planning Notes",    "Plan all tasks for Sprint 4 delivery and review backlog",    "success", "@ui"},
            {"Personal", "Personal Goals 2025",      "Learn Selenium automation and achieve ISTQB certification",  "success", "@ui"},
            {"Home",     "Weekly Shopping List",     "Buy groceries, vegetables, and household cleaning supplies", "success", "@ui"},
            {"Work",     "Bug Fix Priority List",    "Critical bugs: Login timeout, API 500, Report generation",   "success", "@ui"},
            {"Personal", "Exercise Routine",         "30 min cardio, 20 min weights, weekend yoga",               "success", "@ui"},
            {"Work",     "API Created Note",         "Created directly via RestAssured API test",                  "success", "@api"},
            {"Personal", "Note For Deletion Test",   "This note will be deleted as part of delete test",          "success", "@api"},
            {"Home",     "E2E Sync Test Note",       "Created in browser - must appear in API response",          "success", "@e2e"},
            {"Work",     "E2E Reverse Sync Note",    "Created via API - will be deleted via API, verified in UI", "success", "@e2e"},
        };
        fillDataRows(sheet, data, dStyle);
    }

    private static void createNegativeDataSheet(XSSFWorkbook wb, CellStyle hStyle,
                                                 CellStyle dStyle, CellStyle fStyle) {
        XSSFSheet sheet = wb.createSheet("NegativeData");
        sheet.setColumnWidth(0, 6000);   // scenario
        sheet.setColumnWidth(1, 8000);   // input
        sheet.setColumnWidth(2, 6000);   // expected_error
        sheet.setColumnWidth(3, 8000);   // description

        String[] headers = {"scenario", "input", "expected_error", "description"};
        createHeaderRow(sheet, headers, hStyle);

        Object[][] data = {
            {"TS-NEG-01", "No auth token",                          "401 Unauthorized",  "GET /notes without token"},
            {"TS-NEG-03", "invalid-token-xyz123",                   "401 Unauthorized",  "GET /notes with random string as token"},
            {"TS-NEG-03", "eyJhbGciOiJIUzI1NiJ9.INVALID.SIGNATURE","401 Unauthorized",  "GET /notes with malformed JWT"},
            {"TS-NEG-01", "invalid@test.com / wrongpassword",       "401 Unauthorized",  "API login with wrong credentials"},
            {"TS-UI-02",  "empty email / empty password",           "Validation Error",  "UI login with empty fields"},
            {"TS-UI-02",  "bad-email-format / Raunak@123",          "Invalid format",    "UI login with bad email format"},
        };
        fillDataRows(sheet, data, dStyle);
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    private static void createHeaderRow(XSSFSheet sheet, String[] headers, CellStyle style) {
        Row headerRow = sheet.createRow(0);
        headerRow.setHeight((short) 500);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(style);
        }
    }

    private static void fillDataRows(XSSFSheet sheet, Object[][] data, CellStyle style) {
        for (int r = 0; r < data.length; r++) {
            Row row = sheet.createRow(r + 1);
            for (int c = 0; c < data[r].length; c++) {
                Cell cell = row.createCell(c);
                cell.setCellValue(String.valueOf(data[r][c]));
                cell.setCellStyle(style);
            }
        }
    }

    private static CellStyle createHeaderStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(new XSSFColor(new byte[]{(byte)33, (byte)90, (byte)160}, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private static CellStyle createDataStyle(XSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private static CellStyle createPassStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = (XSSFCellStyle) createDataStyle(wb);
        style.setFillForegroundColor(new XSSFColor(new byte[]{(byte)198, (byte)239, (byte)206}, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private static CellStyle createFailStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = (XSSFCellStyle) createDataStyle(wb);
        style.setFillForegroundColor(new XSSFColor(new byte[]{(byte)255, (byte)199, (byte)206}, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }
}
