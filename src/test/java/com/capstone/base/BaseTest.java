package com.capstone.base;

import com.capstone.config.ConfigReader;
import com.capstone.drivers.DriverManager;
import io.qameta.allure.Attachment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

/**
 * BaseTest — Parent class for all TestNG-based test classes.
 *
 * Handles:
 *   - Browser initialisation before each test (@BeforeMethod)
 *   - Screenshot capture on failure
 *   - Browser teardown after each test (@AfterMethod)
 *
 * WHY: Every test needs a browser. Putting setup/teardown in a base class
 * follows DRY principle — test classes focus only on test logic.
 *
 * NOTE: Cucumber tests use Hooks.java instead of this class.
 * This class is for direct TestNG tests if needed.
 */
public class BaseTest {

    private static final Logger log = LogManager.getLogger(BaseTest.class);

    /**
     * Opens browser before each test method.
     * Browser type comes from testng.xml parameter → ConfigReader fallback.
     *
     * @param browser Injected from testng.xml <parameter name="browser" value="chrome"/>
     */
    @BeforeMethod
    @Parameters("browser")
    public void setUp(@Optional("chrome") String browser) {
        String headlessValue = System.getProperty("headless", String.valueOf(ConfigReader.isHeadless()));
        boolean headless = Boolean.parseBoolean(headlessValue);

        String browserToUse = System.getProperty("browser", browser);
        log.info("=== Test Setup | Browser: {} | Headless: {} ===", browserToUse, headless);
        DriverManager.initDriver(browserToUse, headless);
    }

    /**
     * Tears down browser after each test method.
     * Takes screenshot if the test failed.
     *
     * @param result TestNG ITestResult — tells us if test passed/failed
     */
    @AfterMethod
    public void tearDown(ITestResult result) {
        if (result.getStatus() == ITestResult.FAILURE) {
            log.error("TEST FAILED: {} — capturing screenshot", result.getName());
            takeScreenshot();
        }
        DriverManager.quitDriver();
        log.info("=== Test Teardown complete for: {} ===", result.getName());
    }

    /**
     * Takes a screenshot and attaches it to the Allure report.
     * The @Attachment annotation tells Allure to embed the byte[] as an image.
     *
     * @return PNG screenshot bytes
     */
    @Attachment(value = "Screenshot on Failure", type = "image/png")
    public byte[] takeScreenshot() {
        try {
            WebDriver driver = DriverManager.getDriver();
            return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
        } catch (Exception e) {
            log.warn("Could not capture screenshot: {}", e.getMessage());
            return new byte[0];
        }
    }

    /**
     * Provides the WebDriver for subclasses that need direct access.
     */
    protected WebDriver getDriver() {
        return DriverManager.getDriver();
    }
}
