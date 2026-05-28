package com.capstone.pages;

import com.capstone.config.ConfigReader;
import io.qameta.allure.Step;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;

/**
 * LoginPage — Page Object for the Notes App Login page.
 *
 * URL: https://practice.expandtesting.com/notes/app/login
 *
 * ALL locators for the login page are in THIS class.
 * If the login page UI changes, ONLY this file needs updating.
 * This is the core benefit of the Page Object Model (POM).
 */
public class LoginPage extends BasePage {

    private static final Logger log = LogManager.getLogger(LoginPage.class);

    // ==========================================
    // LOCATORS
    // ==========================================
    // Primary locators (current page structure)
    private static final By EMAIL_FIELD = By.cssSelector("input[data-testid='login-email']");
    private static final By PASSWORD_FIELD = By.cssSelector("input[data-testid='login-password']");
    private static final By LOGIN_BUTTON = By.cssSelector("button[data-testid='login-submit']");
    private static final By ERROR_MESSAGE = By.cssSelector("[data-testid='alert-message']");
    private static final By PAGE_HEADER = By.cssSelector("h1.title");
    private static final By REGISTER_LINK = By.cssSelector("a[href='/notes/app/register']");

    // Fallback locators for SelfHealingLocator (in case primary changes)
    private static final By EMAIL_FALLBACK_1 = By.id("email");
    private static final By EMAIL_FALLBACK_2 = By.name("email");
    private static final By PASS_FALLBACK_1 = By.id("password");
    private static final By PASS_FALLBACK_2 = By.name("password");
    private static final By BTN_FALLBACK_1 = By.xpath("//button[contains(text(),'Login')]");
    private static final By BTN_FALLBACK_2 = By.xpath("//button[@type='submit']");

    /**
     * Constructor — calls BasePage() which sets up WebDriver, waits, and
     * self-healer.
     */
    public LoginPage() {
        super();
    }

    // ==========================================
    // ACTION METHODS
    // ==========================================

    /**
     * Navigates browser to the login page URL.
     */
    @Step("Navigate to login page")
    public LoginPage navigateToLoginPage() {
        String loginUrl = ConfigReader.getBaseUrl() + "/login";
        log.info("Navigating to login page: {}", loginUrl);
        navigateTo(loginUrl);
        return this;
    }

    /**
     * Enters email into the email field.
     * Uses SelfHealingLocator — if primary CSS fails, tries ID and name fallbacks.
     */
    @Step("Enter email: {email}")
    public LoginPage enterEmail(String email) {
        log.info("Entering email: {}", email);
        selfHealer.findElement(EMAIL_FIELD, EMAIL_FALLBACK_1, EMAIL_FALLBACK_2).clear();
        selfHealer.findElement(EMAIL_FIELD, EMAIL_FALLBACK_1, EMAIL_FALLBACK_2).sendKeys(email);
        return this;
    }

    /**
     * Enters password into the password field.
     */
    @Step("Enter password")
    public LoginPage enterPassword(String password) {
        log.info("Entering password: ***");
        selfHealer.findElement(PASSWORD_FIELD, PASS_FALLBACK_1, PASS_FALLBACK_2).clear();
        selfHealer.findElement(PASSWORD_FIELD, PASS_FALLBACK_1, PASS_FALLBACK_2).sendKeys(password);
        return this;
    }

    /**
     * Clicks the Login button.
     * Incorporates a JavaScript click fallback logic if third-party ads or hidden
     * DOM elements intercept the standard physical click action sequence.
     */
    @Step("Click Login button")
    public void clickLoginButton() {
        log.info("Clicking login button");
        WebElement button = selfHealer.findElement(LOGIN_BUTTON, BTN_FALLBACK_1, BTN_FALLBACK_2);

        try {
            // Attempt standard click interaction first
            button.click();
        } catch (Exception e) {
            log.warn(
                    "Standard login click intercepted by an overlay/ad banner. Executing JavaScript click fallback. Error: {}",
                    e.getMessage());

            // Bypass physical rendering layers and execute directly via the engine DOM node
            // tree
            try {
                JavascriptExecutor js = (JavascriptExecutor) driver;
                js.executeScript("arguments[0].click();", button);
            } catch (Exception jsException) {
                log.error("JavaScript fallback click execution failed: {}", jsException.getMessage());
                throw jsException;
            }
        }
    }

    /**
     * Full login action — enters email, password, and clicks login.
     * Returns the page to allow method chaining.
     */
    @Step("Login with email: {email}")
    public void login(String email, String password) {
        enterEmail(email);
        enterPassword(password);
        clickLoginButton();
    }

    /**
     * Performs login with credentials from config.properties.
     * Used for scenarios that need a logged-in state as a precondition.
     */
    @Step("Login with default credentials from config")
    public void loginWithDefaultCredentials() {
        login(ConfigReader.getEmail(), ConfigReader.getPassword());
    }

    // ==========================================
    // ASSERTION / STATE METHODS
    // ==========================================

    /**
     * Returns the error message text shown after a failed login.
     * Returns empty string if no error message is displayed.
     */
    @Step("Get login error message")
    public String getErrorMessage() {
        try {
            String errorText = getText(ERROR_MESSAGE);
            log.info("Login error message: {}", errorText);
            return errorText;
        } catch (Exception e) {
            log.debug("No error message found on page");
            return "";
        }
    }

    /**
     * Checks if an error alert is currently visible on the page.
     */
    public boolean isErrorDisplayed() {
        return isElementDisplayed(ERROR_MESSAGE);
    }

    /**
     * Returns the current URL after login attempt.
     * Used to verify redirect to dashboard (URL contains '/notes/app') or
     * that user stayed on login page (URL contains '/login').
     */
    public String getCurrentPageUrl() {
        return getCurrentUrl();
    }

    /**
     * Checks if the login page header is visible.
     * Used to verify we are on the login page.
     */
    public boolean isOnLoginPage() {
        return isElementDisplayed(PAGE_HEADER);
    }

    /**
     * Waits for redirect after successful login.
     * After login, URL changes from /login to /notes/app.
     */
    @Step("Wait for redirect to dashboard after login")
    public void waitForLoginSuccess() {
        log.info("Waiting for redirect to notes dashboard...");
        waitForUrlContains("/notes/app");
        log.info("Login successful. Current URL: {}", getCurrentUrl());
    }
}