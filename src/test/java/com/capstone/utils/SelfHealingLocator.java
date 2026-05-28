package com.capstone.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * SelfHealingLocator — Agentic locator with automatic fallback and retry.
 *
 * PROJECT SPEC: Section 3.3 "Agentic Self-Healing Locators"
 *
 * PROBLEM: When a frontend developer changes a button's ID from "submit-btn"
 * to "save-btn", all tests using the old ID fail immediately. The developer
 * must be paged, root cause investigated, tests fixed — hours of lost time.
 *
 * SOLUTION: Self-healing. The test tries the primary locator. If it fails,
 * it tries each fallback locator in order. If a fallback succeeds:
 *   1. The element is returned (test continues — NO failure)
 *   2. A WARNING is logged: "Primary locator broken, fallback used: [CSS: .save-btn]"
 *      → Developer sees the warning and fixes the primary at their convenience.
 *
 * FULL MCP HEALING: In production, instead of a manual fallback list,
 * the current page DOM would be sent to an AI model (Claude via MCP).
 * The AI generates a new locator. The test uses it. The healed locator is
 * saved for future runs. This is what Healenium implements commercially.
 * Our implementation is the manual-fallback equivalent of that concept.
 *
 * RETRY (withRetry): Wraps any action in exponential backoff retry.
 * Handles flaky elements: animations not finished, network blips, loading spinners.
 */
public class SelfHealingLocator {

    private static final Logger log = LogManager.getLogger(SelfHealingLocator.class);

    private final WebDriver driver;
    private final int timeoutSeconds;

    /**
     * @param driver         Active WebDriver for this thread
     * @param timeoutSeconds How long to wait for each locator attempt
     */
    public SelfHealingLocator(WebDriver driver, int timeoutSeconds) {
        this.driver = driver;
        this.timeoutSeconds = timeoutSeconds;
    }

    /**
     * Finds an element using the primary locator.
     * If primary fails, tries each fallback in order.
     *
     * @param primary   The preferred, intended locator
     * @param fallbacks Zero or more fallback locators (tried in order)
     * @return WebElement — the found element
     * @throws RuntimeException if ALL locators fail
     */
    public WebElement findElement(By primary, By... fallbacks) {
        // Step 1: Try primary locator
        WebElement element = tryLocator(primary);
        if (element != null) {
            return element;
        }

        // Step 2: Primary failed — try each fallback
        log.warn("PRIMARY LOCATOR FAILED: {} | Trying {} fallback(s)...", primary, fallbacks.length);

        for (By fallback : fallbacks) {
            element = tryLocator(fallback);
            if (element != null) {
                // HEALING SUCCESS: Log warning so developer knows to fix primary
                log.warn("SELF-HEALED using fallback: {} | Please update primary locator: {}",
                        fallback, primary);
                return element;
            }
        }

        // Step 3: All locators failed — gather diagnostic info
        String currentUrl = "";
        String pageSourceSnippet = "";
        try {
            currentUrl = driver.getCurrentUrl();
            String fullSource = driver.getPageSource();
            pageSourceSnippet = fullSource.substring(0, Math.min(500, fullSource.length()));
        } catch (Exception ignored) {}

        throw new RuntimeException(String.format(
                "SELF-HEALING FAILED: All locators exhausted.%n" +
                "Primary: %s%n" +
                "Fallbacks: %d tried%n" +
                "Current URL: %s%n" +
                "Page source (first 500 chars): %s",
                primary, fallbacks.length, currentUrl, pageSourceSnippet
        ));
    }

    /**
     * Attempts to find an element using a single locator.
     * Returns null if element is not found within timeout.
     *
     * @param locator Locator to try
     * @return WebElement if found, null if timeout
     */
    private WebElement tryLocator(By locator) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
            WebElement element = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
            log.debug("Locator found: {}", locator);
            return element;
        } catch (Exception e) {
            log.debug("Locator not found: {} | Reason: {}", locator, e.getMessage().split("\n")[0]);
            return null;
        }
    }

    /**
     * Wraps any Runnable action in a retry loop with exponential backoff.
     *
     * RETRY POLICY:
     *   Attempt 1: run action → if fails, wait 1 second
     *   Attempt 2: run action → if fails, wait 2 seconds
     *   Attempt 3 (final): run action → if fails, throw the exception
     *
     * USE CASE: Flaky animations (button appears for 100ms then hides),
     * brief network delays, lazy-loading elements.
     *
     * @param action      The action to retry (lambda or method reference)
     * @param maxAttempts Maximum retry attempts (recommended: 3)
     * @param actionName  Human-readable name for logging
     */
    public void withRetry(Runnable action, int maxAttempts, String actionName) {
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                log.debug("Attempt {}/{}: {}", attempt, maxAttempts, actionName);
                action.run();
                log.debug("Action succeeded on attempt {}: {}", attempt, actionName);
                return;  // SUCCESS — exit retry loop
            } catch (Exception e) {
                lastException = e;
                log.warn("Attempt {}/{} FAILED for '{}': {}", attempt, maxAttempts, actionName, e.getMessage());

                if (attempt < maxAttempts) {
                    long waitMs = attempt * 1000L;  // Exponential backoff: 1s, 2s, 3s...
                    log.debug("Waiting {}ms before retry...", waitMs);
                    try {
                        Thread.sleep(waitMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        // All attempts exhausted
        throw new RuntimeException(
            "Action '" + actionName + "' failed after " + maxAttempts + " attempts. " +
            "Last error: " + (lastException != null ? lastException.getMessage() : "unknown"),
            lastException
        );
    }

    /**
     * Convenience: withRetry with default 3 attempts.
     */
    public void withRetry(Runnable action, String actionName) {
        withRetry(action, 3, actionName);
    }
}
