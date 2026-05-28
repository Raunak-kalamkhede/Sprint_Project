package com.capstone.stepdefs;

import com.capstone.config.ConfigReader;
import com.capstone.pages.LoginPage;
import com.capstone.utils.PerformanceTracker;
import com.capstone.utils.ScenarioContext;
import io.cucumber.java.en.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;

/**
 * LoginStepDefs
 * Handles ONLY UI Login scenarios.
 */
public class LoginStepDefs {

    private static final Logger log = LogManager.getLogger(LoginStepDefs.class);

    private final ScenarioContext context;
    private LoginPage loginPage;

    public LoginStepDefs(ScenarioContext context) {
        this.context = context;
    }

    // =====================================================
    // GIVEN
    // =====================================================

    @Given("the user is on the login page")
    public void theUserIsOnTheLoginPage() {
        log.info("STEP: User navigates to login page");

        loginPage = new LoginPage();
        loginPage.navigateToLoginPage();

        long loadTime = loginPage.getPageLoadTime();

        PerformanceTracker.recordUi(
                "Login Page Load",
                loadTime);
    }

    // =====================================================
    // WHEN
    // =====================================================

    @When("the user logs in with valid credentials")
    public void theUserLogsInWithValidCredentials() {
        log.info("STEP: Login using configured credentials");

        loginPage.login(
                ConfigReader.getEmail(),
                ConfigReader.getPassword());
    }

    @When("the user logs in with email {string} and password {string}")
    public void theUserLogsInWithEmailAndPassword(String email, String password) {
        log.info("STEP: Login using supplied credentials");

        loginPage.login(email, password);
    }

    // =====================================================
    // THEN
    // =====================================================

    @Then("the user should be redirected to the notes dashboard")
    public void theUserShouldBeRedirectedToNotesDashboard() {
        log.info("STEP: Verify dashboard redirection");

        loginPage.waitForLoginSuccess();

        // Stabilize EAGER loading context to guarantee state synchronization matches
        // perfectly
        try {
            Thread.sleep(1200);
        } catch (InterruptedException ignored) {
        }

        String currentUrl = loginPage.getCurrentPageUrl();

        Assert.assertTrue(
                currentUrl.contains("/notes/app"),
                "Expected dashboard URL but got: " + currentUrl);
    }

    @Then("the login error message should be displayed")
    public void theLoginErrorMessageShouldBeDisplayed() {
        Assert.assertTrue(
                loginPage.isErrorDisplayed(),
                "Expected login error message to be displayed");

        String errorText = loginPage.getErrorMessage();

        Assert.assertFalse(
                errorText.isEmpty(),
                "Error message is empty");
    }

    @Then("the login error should contain {string}")
    public void theLoginErrorShouldContain(String expectedText) {
        String actualError = loginPage.getErrorMessage();

        Assert.assertTrue(
                actualError.toLowerCase().contains(expectedText.toLowerCase()),
                "Expected: " + expectedText + " but found: " + actualError);
    }

    @Then("the page load time should be acceptable")
    public void thePageLoadTimeShouldBeAcceptable() {
        boolean acceptable = loginPage.isPageLoadAcceptable();

        Assert.assertTrue(
                acceptable,
                "Page load time exceeded threshold");
    }
}