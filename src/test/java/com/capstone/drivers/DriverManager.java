package com.capstone.drivers;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * DriverManager — Sole class responsible for WebDriver lifecycle.
 *
 * WHY SEPARATE CLASS: Instructor requirement. Single Responsibility Principle.
 * DriverManager ONLY manages driver creation, storage, and destruction.
 * Page classes and test classes NEVER create a WebDriver — they only call
 * getDriver().
 *
 * WHY ThreadLocal: In parallel execution (thread-count=3 in testng.xml), three
 * tests
 * run simultaneously on three threads. ThreadLocal gives each thread its own
 * isolated
 * WebDriver instance so they cannot interfere with each other.
 */
public class DriverManager {

    private static final Logger log = LogManager.getLogger(DriverManager.class);

    // ThreadLocal stores a separate WebDriver for EACH thread.
    // Thread 1 → its own ChromeDriver
    // Thread 2 → its own ChromeDriver
    // Thread 3 → its own ChromeDriver
    // They NEVER share.
    private static final ThreadLocal<WebDriver> driverThreadLocal = new ThreadLocal<>();

    // Private constructor — utility class, not meant to be instantiated
    private DriverManager() {
    }

    /**
     * Initialises a WebDriver based on the browser parameter.
     * Called from Hooks.java @Before each Cucumber scenario.
     *
     * @param browser  "chrome" | "firefox" | "edge" (case-insensitive)
     * @param headless true = no browser window (for CI/CD), false = visible browser
     *                 (for demo)
     */
    public static void initDriver(String browser, boolean headless) {
        if (driverThreadLocal.get() != null) {
            log.warn("Driver already initialised for this thread. Skipping re-init.");
            return;
        }

        WebDriver driver;
        log.info("Initialising {} driver | headless={} | thread={}", browser, headless,
                Thread.currentThread().getName());

        switch (browser.toLowerCase().trim()) {
            case "firefox":
                WebDriverManager.firefoxdriver().setup();
                FirefoxOptions firefoxOptions = new FirefoxOptions();

                // FIX: Hand over control as soon as HTML DOM interaction is available
                firefoxOptions.setPageLoadStrategy(PageLoadStrategy.EAGER);

                if (headless)
                    firefoxOptions.addArguments("--headless");
                driver = new FirefoxDriver(firefoxOptions);
                break;

            case "edge":
                WebDriverManager.edgedriver().setup();
                EdgeOptions edgeOptions = new EdgeOptions();

                // FIX: Hand over control as soon as HTML DOM interaction is available
                edgeOptions.setPageLoadStrategy(PageLoadStrategy.EAGER);

                if (headless)
                    edgeOptions.addArguments("--headless");
                edgeOptions.addArguments("--disable-notifications");
                edgeOptions.addArguments("--no-sandbox");
                edgeOptions.addArguments("--disable-dev-shm-usage");
                driver = new EdgeDriver(edgeOptions);
                break;

            case "chrome":
            default:
                WebDriverManager.chromedriver().setup();
                ChromeOptions chromeOptions = new ChromeOptions();

                // Hand over control as soon as HTML DOM interaction is available
                chromeOptions.setPageLoadStrategy(PageLoadStrategy.EAGER);

                // -----------------------------------------------------------------
                // FIX: Disable Chrome Password Manager & Data Breach Warnings
                // -----------------------------------------------------------------
                Map<String, Object> prefs = new HashMap<>();
                prefs.put("credentials_enable_service", false); // Stops offering to save passwords
                prefs.put("profile.password_manager_enabled", false); // Disables the password manager engine
                chromeOptions.setExperimentalOption("prefs", prefs);
                // -----------------------------------------------------------------

                if (headless) {
                    chromeOptions.addArguments("--headless=new");
                }
                chromeOptions.addArguments("--start-maximized");
                chromeOptions.addArguments("--disable-notifications");
                chromeOptions.addArguments("--disable-popup-blocking");
                chromeOptions.addArguments("--no-sandbox");
                chromeOptions.addArguments("--disable-dev-shm-usage");
                chromeOptions.addArguments("--disable-gpu");
                chromeOptions.addArguments("--remote-allow-origins=*");
                driver = new ChromeDriver(chromeOptions);
                break;
        }

        // Set explicit timeout definitions
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
        driver.manage().window().maximize();

        // Store this driver in the current thread's ThreadLocal slot
        driverThreadLocal.set(driver);
        log.info("Driver initialised successfully with EAGER execution strategy: {}",
                driver.getClass().getSimpleName());
    }

    /**
     * Returns the WebDriver for the CURRENT thread.
     * Page objects call this to get the browser they should control.
     * Never creates a new driver — only retrieves.
     *
     * @return WebDriver instance for current thread
     * @throws IllegalStateException if driver was not initialised for this thread
     */
    public static WebDriver getDriver() {
        WebDriver driver = driverThreadLocal.get();
        if (driver == null) {
            throw new IllegalStateException(
                    "WebDriver not initialised for thread: " + Thread.currentThread().getName() +
                            ". Ensure initDriver() is called in @Before hook.");
        }
        return driver;
    }

    /**
     * Quits the browser and removes the driver from ThreadLocal.
     * MUST be called in @After hook to prevent memory leaks.
     * After this call, getDriver() will throw until initDriver() is called again.
     */
    public static void quitDriver() {
        WebDriver driver = driverThreadLocal.get();
        if (driver != null) {
            try {
                driver.quit();
                log.info("Driver quit successfully for thread: {}", Thread.currentThread().getName());
            } catch (Exception e) {
                log.warn("Exception while quitting driver: {}", e.getMessage());
            } finally {
                // ALWAYS remove from ThreadLocal to avoid memory leaks in parallel execution
                driverThreadLocal.remove();
            }
        }
    }

    /**
     * Checks if driver is currently active for this thread.
     * Useful in hooks to avoid double-quit errors.
     */
    public static boolean isDriverActive() {
        return driverThreadLocal.get() != null;
    }
}