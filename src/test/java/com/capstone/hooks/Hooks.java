package com.capstone.hooks;

import com.capstone.config.ConfigReader;
import com.capstone.drivers.DriverManager;
import com.capstone.pages.NotesPage;
import com.capstone.utils.PerformanceTracker;
import io.cucumber.java.*;
import io.qameta.allure.Allure;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Hooks — Cucumber lifecycle hooks for all scenarios.
 *
 * Handles targeted conditional browser instantiation to support parallelized,
 * multi-layered API, UI, and hybrid end-to-end framework layers safely.
 */
public class Hooks {

    private static final Logger log = LogManager.getLogger(Hooks.class);

    public Hooks() {
    }

    /**
     * @Before — Runs before EACH Cucumber scenario.
     *         Evaluates tag expressions dynamically to optimize execution overhead.
     *
     * @param scenario The current scenario runtime state
     */
    @Before(order = 1)
    public void setUp(Scenario scenario) {
        log.info("====== SCENARIO START: {} ======", scenario.getName());
        log.info("Tags: {}", scenario.getSourceTagNames());

        // Reset performance tracker for clean slate per scenario
        PerformanceTracker.clear();

        // Refined logical gate to assess if UI capability is required
        boolean isApiOnly = scenario.getSourceTagNames().contains("@api") &&
                !scenario.getSourceTagNames().contains("@e2e") &&
                !scenario.getSourceTagNames().contains("@ui");

        // Fallback: If scenario name indicates cross-layer validation, force
        // infrastructure initialization
        if (scenario.getName().toLowerCase().contains("ui") || scenario.getName().toLowerCase().contains("hybrid")) {
            isApiOnly = false;
        }

        if (!isApiOnly) {
            String browser = System.getProperty("browser", ConfigReader.getBrowser());
            boolean headless = Boolean.parseBoolean(
                    System.getProperty("headless", String.valueOf(ConfigReader.isHeadless())));

            log.info("Initialising browser: {} | headless: {}", browser, headless);
            DriverManager.initDriver(browser, headless);
        } else {
            log.info("API-only execution matrix detected — skipping browser initialisation");
        }
    }

    /**
     * @AfterStep — Runs AFTER every single Cucumber step.
     *            Captures a screenshot and attaches it to both Allure and Cucumber
     *            HTML reports.
     *
     * @param scenario The current scenario runtime state
     */
    @AfterStep
    public void afterStep(Scenario scenario) {
        // Defensive check: validation framework guarantees driver allocation state
        if (!DriverManager.isDriverActive()) {
            return;
        }

        try {
            byte[] screenshot = captureScreenshot();
            if (screenshot != null && screenshot.length > 0) {
                // Attach to Allure report
                Allure.addAttachment(
                        "Step Visual State: " + scenario.getName(),
                        "image/png",
                        new ByteArrayInputStream(screenshot),
                        ".png");
                // Attach to Cucumber HTML report
                scenario.attach(screenshot, "image/png", "step-screenshot");
            }
        } catch (Exception e) {
            log.debug("Step-level asset generation bypassed: {}", e.getMessage());
        }
    }

    /**
     * @After — Runs after EACH Cucumber scenario.
     *        Safely tears down contextual thread environments and builds analytical
     *        metrics.
     *
     * @param scenario The current scenario runtime state
     */
    @After(order = 1)
    public void tearDown(Scenario scenario) {
        try {
            if (scenario.isFailed()) {
                log.error("SCENARIO FAILED: {}", scenario.getName());

                if (DriverManager.isDriverActive()) {
                    byte[] failureScreenshot = captureScreenshot();
                    if (failureScreenshot != null && failureScreenshot.length > 0) {
                        // Allure attachment
                        Allure.addAttachment(
                                "FAILURE Screenshot: " + scenario.getName(),
                                "image/png",
                                new ByteArrayInputStream(failureScreenshot),
                                ".png");
                        // Cucumber HTML attachment
                        scenario.attach(failureScreenshot, "image/png", "failure-screenshot");

                        // Save to disk architecture for CI/CD archiving matrices
                        saveScreenshotToFile(failureScreenshot, scenario.getName());
                    }
                }
            }

            // Attach performance summary metrics to report output
            PerformanceTracker.attachSummaryToAllure();

        } catch (Exception e) {
            log.warn("Exception processed during resource teardown lifecycle: {}", e.getMessage());
        } finally {
            // Guarantee browser instance disposal across execution threads to mitigate
            // resource exhaustion leaks
            if (DriverManager.isDriverActive()) {
                try {
                    DriverManager.quitDriver();
                    log.info("Browser driver detached successfully from execution context thread.");
                } catch (Exception e) {
                    log.error("Failed to safely terminate running WebDriver instance: {}", e.getMessage());
                }
            }
            log.info("====== SCENARIO END: {} | Status: {} ======",
                    scenario.getName(), scenario.getStatus());
        }
    }

    // ==========================================
    // PRIVATE HELPERS
    // ==========================================

    /**
     * Captures a PNG screenshot from the current browser state safely.
     */
    private byte[] captureScreenshot() {
        try {
            if (DriverManager.isDriverActive()) {
                WebDriver driver = DriverManager.getDriver();
                if (driver != null) {
                    return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
                }
            }
        } catch (Exception e) {
            log.debug("Runtime screenshot capture interrupted: {}", e.getMessage());
        }
        return new byte[0];
    }

    /**
     * Saves screenshot bytes to directory.
     */
    private void saveScreenshotToFile(byte[] screenshot, String scenarioName) {
        try {
            String screenshotDir = ConfigReader.getScreenshotDir();
            Files.createDirectories(Paths.get(screenshotDir));

            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
            String sanitisedName = scenarioName
                    .replaceAll("[^a-zA-Z0-9_\\-]", "_")
                    .substring(0, Math.min(50, scenarioName.length()));
            String fileName = sanitisedName + "_" + timestamp + ".png";

            Path filePath = Paths.get(screenshotDir, fileName);
            Files.write(filePath, screenshot);
            log.info("Screenshot saved successfully: {}", filePath.toAbsolutePath());

        } catch (IOException e) {
            log.warn("Failed to write screenshot data payload to file path system: {}", e.getMessage());
        }
    }
    @After(value = "@ui", order = 10)
    public void cleanupUserDashboardData(Scenario scenario) {
        log.info("Executing post-scenario UI data cleanup hook for: {}", scenario.getName());
        
        try {
            NotesPage notesPage = new NotesPage();
            // Verify we are actually logged in and looking at the application layout before trying to delete
            if (notesPage.isDashboardLoaded()) {
                notesPage.deleteAllNotes();
            }
        } catch (Exception e) {
            log.error("Global UI teardown data purge failed gracefully: {}", e.getMessage());
        }
    }
}