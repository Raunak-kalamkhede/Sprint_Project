package com.capstone.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

/**
 * RetryAnalyzer — Implements automated retry logic for flaky UI/API steps.
 * Aligned with Section 3.3 (Agentic Automation) of the Capstone Requirements.
 */
public class RetryAnalyzer implements IRetryAnalyzer {
    private static final Logger log = LogManager.getLogger(RetryAnalyzer.class);
    
    private int retryCount = 0;
    // Set the maximum number of retries (e.g., 2 retries means a test can run up to 3 times total)
    private static final int MAX_RETRY_COUNT = 2; 

    /**
     * TestNG calls this method automatically every time a test fails.
     * @return true if the test should run again, false if it should stay failed.
     */
    @Override
    public boolean retry(ITestResult result) {
        if (!result.isSuccess() && retryCount < MAX_RETRY_COUNT) {
            retryCount++;
            log.warn("[AGENTIC RETRY] Test '{}' failed on thread '{}'. Attempting automated re-run ({} out of {})...",
                    result.getName(), Thread.currentThread().getName(), retryCount, MAX_RETRY_COUNT);
            return true; // Command TestNG to execute the test again
        }
        
        if (retryCount >= MAX_RETRY_COUNT) {
            log.error("[AGENTIC RETRY] Test '{}' has exhausted all {} retry attempts. Marking scenario as FAILED.", 
                    result.getName(), MAX_RETRY_COUNT);
        }
        return false; // Do not retry further, accept failure
    }
}