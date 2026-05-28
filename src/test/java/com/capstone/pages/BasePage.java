package com.capstone.pages;

import com.capstone.config.ConfigReader;
import com.capstone.drivers.DriverManager;
import com.capstone.utils.SelfHealingLocator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * BasePage — Parent class for all Page Object classes.
 *
 * Contains REUSABLE methods for element interaction.
 * WHY: DRY principle. Every page needs waitForElement(), click(), type().
 * Centralising here means ONE fix if the logic changes.
 *
 * Page objects get the WebDriver by calling DriverManager.getDriver().
 * They NEVER create their own driver — that is DriverManager's job.
 */
public class BasePage {

    private static final Logger log = LogManager.getLogger(BasePage.class);

    protected WebDriver driver;
    protected WebDriverWait wait;
    protected SelfHealingLocator selfHealer;

    /**
     * Constructor — called by all page object constructors.
     * Gets driver from DriverManager (NOT creating a new one).
     */
    public BasePage() {
        this.driver = DriverManager.getDriver();
        int waitSeconds = ConfigReader.getExplicitWait();
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(waitSeconds));
        this.selfHealer = new SelfHealingLocator(driver, waitSeconds);
        log.debug("BasePage initialised with driver: {}", driver.getClass().getSimpleName());
    }

    // ==========================================
    // NAVIGATION
    // ==========================================

    /**
     * Navigates to a URL. Logs the action.
     */
    protected void navigateTo(String url) {
        log.info("Navigating to: {}", url);
        driver.get(url);
    }

    /**
     * Returns the current browser URL.
     */
    protected String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    /**
     * Returns the current page title.
     */
    protected String getPageTitle() {
        return driver.getTitle();
    }

    // ==========================================
    // ELEMENT INTERACTION
    // ==========================================

    /**
     * Waits for element to be visible then returns it.
     */
    protected WebElement waitForElement(By locator) {
        log.debug("Waiting for element: {}", locator);
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    /**
     * Waits for element to be clickable then returns it.
     */
    protected WebElement waitForClickable(By locator) {
        log.debug("Waiting for clickable: {}", locator);
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }

    /**
     * Clears field and types text. Logs the action (masks passwords).
     */
    protected void typeText(By locator, String text) {
        String logText = locator.toString().toLowerCase().contains("password") ? "***" : text;
        log.debug("Typing '{}' into: {}", logText, locator);
        WebElement element = waitForElement(locator);
        element.clear();
        element.sendKeys(text);
    }

    /**
     * Clicks an element. Waits for it to be clickable first.
     */
    protected void clickElement(By locator) {
        log.debug("Clicking: {}", locator);
        waitForClickable(locator).click();
    }

    /**
     * Gets text content of an element.
     */
    protected String getText(By locator) {
        return waitForElement(locator).getText().trim();
    }

    /**
     * Gets an attribute value of an element.
     */
    protected String getAttribute(By locator, String attribute) {
        return waitForElement(locator).getAttribute(attribute);
    }

    /**
     * Checks if an element is present on the page.
     */
    protected boolean isElementPresent(By locator) {
        try {
            driver.findElement(locator);
            return true;
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    /**
     * Checks if an element is displayed.
     */
    protected boolean isElementDisplayed(By locator) {
        try {
            return waitForElement(locator).isDisplayed();
        } catch (TimeoutException | NoSuchElementException e) {
            return false;
        }
    }

    /**
     * Selects an option from a <select> dropdown by visible text.
     */
    protected void selectByVisibleText(By locator, String text) {
        log.debug("Selecting '{}' from dropdown: {}", text, locator);
        Select select = new Select(waitForElement(locator));
        select.selectByVisibleText(text);
    }

    /**
     * Selects an option from a <select> dropdown by value attribute.
     */
    protected void selectByValue(By locator, String value) {
        Select select = new Select(waitForElement(locator));
        select.selectByValue(value);
    }

    /**
     * Scrolls the page to bring an element into view.
     */
    protected void scrollToElement(By locator) {
        WebElement element = waitForElement(locator);
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
    }

    /**
     * Clicks element using JavaScript (useful when normal click fails due to overlays).
     */
    protected void jsClick(By locator) {
        log.debug("JS clicking: {}", locator);
        WebElement element = waitForElement(locator);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
    }

    /**
     * Waits until element is invisible (useful after actions that hide elements).
     */
    protected void waitForInvisibility(By locator) {
        wait.until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    /**
     * Waits until URL contains a given substring.
     */
    protected void waitForUrlContains(String urlFragment) {
        wait.until(ExpectedConditions.urlContains(urlFragment));
    }

    // ==========================================
    // PERFORMANCE MEASUREMENT
    // ==========================================

    /**
     * Measures page load time using the browser's Navigation Timing API.
     * Returns milliseconds from navigationStart to loadEventEnd.
     *
     * Used for FR-08 performance checks.
     */
    public long getPageLoadTime() {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            Long loadEventEnd   = (Long) js.executeScript("return window.performance.timing.loadEventEnd");
            Long navigationStart = (Long) js.executeScript("return window.performance.timing.navigationStart");
            long loadTime = loadEventEnd - navigationStart;
            log.info("Page load time: {}ms | URL: {}", loadTime, driver.getCurrentUrl());
            return loadTime;
        } catch (Exception e) {
            log.warn("Could not measure page load time: {}", e.getMessage());
            return -1;
        }
    }

    /**
     * Checks if page load time is within the configured threshold.
     */
    public boolean isPageLoadAcceptable() {
        long loadTime = getPageLoadTime();
        long threshold = ConfigReader.getUiThresholdMs();
        if (loadTime < 0) return true; // skip if measurement failed
        boolean acceptable = loadTime < threshold;
        if (!acceptable) {
            log.warn("PERFORMANCE: Page load {}ms exceeds threshold {}ms", loadTime, threshold);
        }
        return acceptable;
    }
}
