package com.capstone.utils;

import io.qameta.allure.Allure;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * PerformanceTracker — Records and reports UI and API performance metrics.
 *
 * WHY: FR-08 requires API response time < 2 seconds.
 * The evaluator wants to see timing data. PerformanceTracker collects all
 * timings during the test run and generates a summary table attached to
 * the Allure report.
 *
 * USAGE in step definitions:
 *   PerformanceTracker.record("GET /notes", responseTime);
 *   // At end of scenario:
 *   PerformanceTracker.attachSummaryToAllure();
 */
public class PerformanceTracker {

    private static final Logger log = LogManager.getLogger(PerformanceTracker.class);

    // Stores performance records for the current test run
    // ThreadLocal ensures parallel threads don't share timing data
    private static final ThreadLocal<List<PerformanceRecord>> records =
            ThreadLocal.withInitial(ArrayList::new);

    // ==========================================
    // RECORD A TIMING
    // ==========================================

    /**
     * Records a performance measurement.
     *
     * @param operation   Human-readable name (e.g., "POST /users/login", "Page Load - Dashboard")
     * @param type        "API" or "UI"
     * @param durationMs  Duration in milliseconds
     * @param threshold   Acceptable threshold in milliseconds
     */
    public static void record(String operation, String type, long durationMs, long threshold) {
        PerformanceRecord rec = new PerformanceRecord(operation, type, durationMs, threshold);
        records.get().add(rec);

        String status = rec.isPassed() ? "✅ PASS" : "❌ FAIL";
        log.info("PERF {} | {} | {}ms (threshold: {}ms) | {}", type, operation, durationMs, threshold, status);
    }

    /**
     * Records an API timing (uses default API threshold from config).
     */
    public static void recordApi(String endpoint, long durationMs) {
        long threshold = 2000L; // FR-08: API < 2 seconds
        record(endpoint, "API", durationMs, threshold);
    }

    /**
     * Records a UI page load timing.
     */
    public static void recordUi(String pageName, long durationMs) {
        long threshold = 5000L; // UI pages < 5 seconds
        record(pageName, "UI", durationMs, threshold);
    }

    // ==========================================
    // GENERATE REPORT
    // ==========================================

    /**
     * Generates an HTML performance summary table and attaches it to Allure.
     * Call this at the end of each scenario (in @After hook or final step).
     */
    public static void attachSummaryToAllure() {
        List<PerformanceRecord> allRecords = records.get();
        if (allRecords.isEmpty()) return;

        String htmlTable = buildHtmlTable(allRecords);

        Allure.addAttachment(
                "Performance Summary",
                "text/html",
                new ByteArrayInputStream(htmlTable.getBytes(StandardCharsets.UTF_8)),
                ".html"
        );

        // Also log as plain text
        log.info("=== PERFORMANCE SUMMARY ===");
        for (PerformanceRecord rec : allRecords) {
            log.info("{} | {} | {}ms | {}", rec.type, rec.operation, rec.durationMs,
                    rec.isPassed() ? "PASS" : "FAIL");
        }
        log.info("===========================");
    }

    /**
     * Generates a plain text summary (for Cucumber HTML report attachment).
     */
    public static String getPlainTextSummary() {
        List<PerformanceRecord> allRecords = records.get();
        if (allRecords.isEmpty()) return "No performance data recorded.";

        StringBuilder sb = new StringBuilder("PERFORMANCE SUMMARY\n");
        sb.append("=".repeat(70)).append("\n");
        sb.append(String.format("%-10s %-35s %10s %12s %8s%n",
                "TYPE", "OPERATION", "DURATION", "THRESHOLD", "STATUS"));
        sb.append("-".repeat(70)).append("\n");

        for (PerformanceRecord rec : allRecords) {
            sb.append(String.format("%-10s %-35s %9dms %11dms %8s%n",
                    rec.type, rec.operation, rec.durationMs, rec.thresholdMs,
                    rec.isPassed() ? "PASS" : "FAIL"));
        }
        sb.append("=".repeat(70)).append("\n");
        sb.append("Total checks: ").append(allRecords.size())
          .append(" | Passed: ").append(allRecords.stream().filter(PerformanceRecord::isPassed).count())
          .append(" | Failed: ").append(allRecords.stream().filter(r -> !r.isPassed()).count());
        return sb.toString();
    }

    /**
     * Clears all records for the current thread.
     * Call this at the start of each scenario to reset.
     */
    public static void clear() {
        records.get().clear();
    }

    // ==========================================
    // PRIVATE HELPERS
    // ==========================================

    private static String buildHtmlTable(List<PerformanceRecord> allRecords) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><style>")
            .append("body{font-family:Arial,sans-serif;margin:20px;}")
            .append("h2{color:#333;}")
            .append("table{border-collapse:collapse;width:100%;}")
            .append("th{background:#4a90d9;color:white;padding:10px;text-align:left;}")
            .append("td{padding:8px;border-bottom:1px solid #ddd;}")
            .append("tr:nth-child(even){background:#f9f9f9;}")
            .append(".pass{color:green;font-weight:bold;}")
            .append(".fail{color:red;font-weight:bold;}")
            .append("</style></head><body>")
            .append("<h2>Performance Test Summary</h2>")
            .append("<table><tr>")
            .append("<th>Type</th><th>Operation</th><th>Duration (ms)</th>")
            .append("<th>Threshold (ms)</th><th>Status</th></tr>");

        for (PerformanceRecord rec : allRecords) {
            String statusClass = rec.isPassed() ? "pass" : "fail";
            String statusText  = rec.isPassed() ? "✅ PASS" : "❌ FAIL";
            html.append("<tr>")
                .append("<td>").append(rec.type).append("</td>")
                .append("<td>").append(rec.operation).append("</td>")
                .append("<td>").append(rec.durationMs).append("</td>")
                .append("<td>").append(rec.thresholdMs).append("</td>")
                .append("<td class='").append(statusClass).append("'>").append(statusText).append("</td>")
                .append("</tr>");
        }

        long passCount = allRecords.stream().filter(PerformanceRecord::isPassed).count();
        html.append("</table>")
            .append("<p><b>Total: ").append(allRecords.size())
            .append(" | ✅ Pass: ").append(passCount)
            .append(" | ❌ Fail: ").append(allRecords.size() - passCount).append("</b></p>")
            .append("</body></html>");

        return html.toString();
    }

    // ==========================================
    // INNER RECORD CLASS
    // ==========================================

    private static class PerformanceRecord {
        String operation;
        String type;
        long durationMs;
        long thresholdMs;

        PerformanceRecord(String operation, String type, long durationMs, long thresholdMs) {
            this.operation   = operation;
            this.type        = type;
            this.durationMs  = durationMs;
            this.thresholdMs = thresholdMs;
        }

        boolean isPassed() {
            return durationMs < thresholdMs;
        }
    }
}
