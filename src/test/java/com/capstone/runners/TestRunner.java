package com.capstone.runners;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import org.testng.annotations.DataProvider;

/**
 * TestRunner — Connects Cucumber to TestNG.
 *
 * @CucumberOptions specifies:
 *                  features — where to find .feature files
 *                  glue — packages containing Step Definitions and Hooks
 *                  plugin — report output formats:
 *                  - allure-results: Allure JSON for HTML report
 *                  - html: Cucumber HTML report
 *                  - json: For Cucumber Reports Jenkins plugin
 *                  - pretty: Console output (readable)
 *                  tags — can be overridden from command line with
 *                  -Dcucumber.filter.tags
 *                  monochrome — cleaner console output
 *
 * @DataProvider(parallel = true):
 *                        Makes each Cucumber scenario a separate TestNG data
 *                        row.
 *                        Combined with testng.xml thread-count="3", up to 3
 *                        scenarios run simultaneously.
 *                        ThreadLocal in DriverManager ensures each gets its own
 *                        browser.
 */
@CucumberOptions(features = "src/test/resources/features", glue = {
        "com.capstone.stepdefs",
        "com.capstone.hooks"
}, plugin = {
        "io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm",
        "pretty",
        "html:target/cucumber-reports/cucumber-report.html",
        "json:target/cucumber-reports/cucumber-report.json",
        "junit:target/cucumber-reports/cucumber-report.xml"
}, monochrome = true, publish = false)
public class TestRunner extends AbstractTestNGCucumberTests {

    /**
     * Override scenarios() with @DataProvider(parallel = true).
     *
     * WHY: AbstractTestNGCucumberTests.scenarios() returns all scenarios as a 2D
     * array.
     * Setting parallel=true tells TestNG to distribute these rows across threads
     * defined in testng.xml (thread-count=3).
     *
     * Without this override: all scenarios run sequentially on one thread.
     * With this override: scenarios run in parallel → much faster test execution.
     */
    @Override
    @DataProvider(parallel = false)
    public Object[][] scenarios() {
        return super.scenarios();
    }
}
