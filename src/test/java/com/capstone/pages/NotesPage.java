package com.capstone.pages;

import org.testng.Assert;

import io.qameta.allure.Step;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.ArrayList;
import java.util.List;

/**
 * NotesPage — Page Object for the Notes Dashboard.
 *
 * URL: https://practice.expandtesting.com/notes/app
 */
public class NotesPage extends BasePage {

    private static final Logger log = LogManager.getLogger(NotesPage.class);

    // ==========================================
    // LOCATORS — Notes Dashboard
    // ==========================================

    private static final By ADD_NOTE_BUTTON = By.cssSelector("[data-testid='add-new-note']");
    private static final By ADD_NOTE_FALLBACK = By.xpath("//button[contains(text(),'Add Note')]");

    // Note creation form
    private static final By NOTE_CATEGORY_SELECT = By.cssSelector("select[data-testid='note-category']");
    private static final By NOTE_TITLE_FIELD = By.cssSelector("input[data-testid='note-title']");
    private static final By NOTE_DESC_FIELD = By.cssSelector("textarea[data-testid='note-description']");
    private static final By SUBMIT_NOTE_BUTTON = By.cssSelector("button[data-testid='note-submit']");

    // Fallbacks for note form
    private static final By CATEGORY_FALLBACK = By.id("category");
    private static final By TITLE_FALLBACK = By.id("title");
    private static final By DESC_FALLBACK = By.id("description");
    private static final By SUBMIT_FALLBACK = By.xpath("//button[@type='submit']");

    // Notes list
    private static final By NOTES_LIST = By.cssSelector("[data-testid='note-item']");
    private static final By NOTE_CARD_TITLE = By.cssSelector("[data-testid='note-card-title']");
    private static final By NOTE_CARD_CATEGORY = By.cssSelector("[data-testid='note-card-category']");

    // Delete / Edit buttons inside a note card
    private static final By DELETE_NOTE_BTN = By.cssSelector("button[data-testid='note-delete']");
    private static final By EDIT_NOTE_BTN = By.cssSelector("button[data-testid='note-edit']");
    private static final By CONFIRM_DELETE_BTN = By.cssSelector("button[data-testid='note-delete-confirm']");

    // Success / alert messages
    private static final By ALERT_MESSAGE = By.cssSelector("[data-testid='alert-message']");

    // User menu / logout
    private static final By USER_MENU = By.cssSelector("[data-testid='user-menu']");
    private static final By LOGOUT_LINK = By.cssSelector("[data-testid='logout']");

    // Page header to verify dashboard loaded
    private static final By DASHBOARD_HEADER = By.cssSelector("nav.navbar");

    public NotesPage() {
        super();
    }

    // ==========================================
    // CREATE NOTE ACTIONS
    // ==========================================

    @Step("Verify notes dashboard is loaded")
    public boolean isDashboardLoaded() {
        try {
            // Enhanced robustness: check for text presence or alternate structural layouts
            // if navbar lags
            wait.until(ExpectedConditions.or(
                    ExpectedConditions.visibilityOfElementLocated(DASHBOARD_HEADER),
                    ExpectedConditions.visibilityOfElementLocated(ADD_NOTE_BUTTON)));
            return true;
        } catch (Exception e) {
            log.warn("Dashboard structure not visible in timeframe: {}", e.getMessage());
            return false;
        }
    }

    @Step("Click Add Note button")
    public NotesPage clickAddNote() {
        log.info("Clicking Add Note button");
        WebElement addBtn = selfHealer.findElement(ADD_NOTE_BUTTON, ADD_NOTE_FALLBACK);

        // Ensure element framework layout properties are present
        wait.until(ExpectedConditions.elementToBeClickable(addBtn));

        try {
            // Attempt standard click interaction pattern
            addBtn.click();
        } catch (Exception e) {
            log.warn("Standard Add Note click intercepted by Google Ad iframe. Executing JS fallback click mapping.");
            try {
                // Bypass the browser layout layer completely to click at the core DOM node tree
                // level
                ((org.openqa.selenium.JavascriptExecutor) driver)
                        .executeScript("arguments[0].click();", addBtn);
            } catch (Exception jsEx) {
                log.error("JavaScript fallback execution engine failure on Add Note selector: {}", jsEx.getMessage());
                throw jsEx;
            }
        }
        return this;
    }

    @Step("Select category: {category}")
    public NotesPage selectCategory(String category) {
        log.info("Selecting category: {}", category);
        wait.until(ExpectedConditions.visibilityOfElementLocated(NOTE_CATEGORY_SELECT));
        try {
            selectByVisibleText(NOTE_CATEGORY_SELECT, category);
        } catch (Exception e) {
            selectByValue(NOTE_CATEGORY_SELECT, category.toLowerCase());
        }
        return this;
    }

    @Step("Enter note title: {title}")
    public NotesPage enterTitle(String title) {
        log.info("Entering note title: {}", title);
        WebElement field = selfHealer.findElement(NOTE_TITLE_FIELD, TITLE_FALLBACK);
        wait.until(ExpectedConditions.visibilityOf(field));
        field.clear();
        field.sendKeys(title);
        return this;
    }

    @Step("Enter note description")
    public void enterDescription(String description) {
        log.info("Entering note description");
        WebElement field = selfHealer.findElement(NOTE_DESC_FIELD, DESC_FALLBACK);
        wait.until(ExpectedConditions.visibilityOf(field));
        field.clear();
        field.sendKeys(description);
    }

    @Step("Submit the note form")
    public void submitNote() {
        log.info("Submitting note form");
        WebElement submitBtn = selfHealer.findElement(SUBMIT_NOTE_BUTTON, SUBMIT_FALLBACK);
        wait.until(ExpectedConditions.elementToBeClickable(submitBtn));
        submitBtn.click();
        wait.until(ExpectedConditions.invisibilityOf(submitBtn));
    }

    public void createNote(String category, String title, String description) {
        clickAddNote();
        selectCategory(category);
        enterTitle(title);
        enterDescription(description);
        submitNote();
    }

    // ==========================================
    // VIEW / VERIFY NOTES
    // ==========================================

    @Step("Get all note titles from dashboard")
    public List<String> getAllNoteTitles() {
        List<String> titles = new ArrayList<>();
        try {
            // Read instantaneously without blocking if cards have been wiped clean
            List<WebElement> titleElements = driver.findElements(NOTE_CARD_TITLE);
            for (WebElement el : titleElements) {
                titles.add(el.getText().trim());
            }
        } catch (Exception e) {
            log.warn("Could not retrieve text contents from note titles: {}", e.getMessage());
        }
        return titles;
    }

    public boolean isNoteVisible(String title) {
        List<String> allTitles = getAllNoteTitles();
        return allTitles.stream().anyMatch(t -> t.equalsIgnoreCase(title));
    }

    public int getNoteCount() {
        try {
            return driver.findElements(NOTES_LIST).size();
        } catch (Exception e) {
            return 0;
        }
    }

    public String getAlertMessage() {
        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(ALERT_MESSAGE));
            return getText(ALERT_MESSAGE);
        } catch (Exception e) {
            return "";
        }
    }

    // ==========================================
    // DELETE NOTE
    // ==========================================

    /**
     * Deletes a note by matching its title safely.
     * Prevents empty dashboard execution lockouts.
     */
    /**
     * Deletes a note by matching its text title map directly.
     * Integrates explicit visibility gates to ensure React nodes are bound cleanly.
     */
    @Step("Delete note with title: {title}")
    public void deleteNoteByTitle(String title) {
        log.info("Attempting to delete note titled: {}", title);

        try {
            // 1. Dynamic XPath locating the specific card parent containing your exact
            // title string node
            String cardXPath = String.format(
                    "//div[contains(@class,'card') or @data-testid='note-item'][.//v or .//*[contains(text(),'%s')]]",
                    title);

            // 2. Explicitly wait up to 10 seconds for that specific target card to appear
            // in the view context layout
            WebElement targetCard = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(cardXPath)));
            log.info("Target note card element verified on screen layout grid.");

            // 3. Extract the inner delete icon target linked exclusively inside that parent
            // context card
            WebElement deleteBtn = targetCard.findElement(By.cssSelector("button[data-testid='note-delete']"));

            // 4. Handle scrolling and executing interaction sequence
            try {
                JavascriptExecutor js = (JavascriptExecutor) driver;
                js.executeScript("arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", deleteBtn);
                Thread.sleep(400);
            } catch (Exception ignored) {
            }

            wait.until(ExpectedConditions.elementToBeClickable(deleteBtn));
            try {
                deleteBtn.click();
            } catch (Exception clickEx) {
                log.warn("Native delete selector blocked by ad frames. Running JavaScript engine execution.");
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", deleteBtn);
            }
            log.info("Fired deletion tracking trigger.");

            // 5. Explicitly handle the system confirmation modal step
            WebElement confirmBtn = wait.until(ExpectedConditions.elementToBeClickable(CONFIRM_DELETE_BTN));
            try {
                confirmBtn.click();
            } catch (Exception confirmEx) {
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", confirmBtn);
            }
            log.info("Fired system delete confirmation submission.");

            // 6. Friend's Logic Sync: Block execution thread until the target card asset is
            // completely detached from DOM
            wait.until(ExpectedConditions.stalenessOf(targetCard));
            log.info("FR-07 Integration Success: Note card completely detached from browser viewport layout engine.");

        } catch (Exception e) {
            log.error("Failed to execute card removal sequence for target note '{}': {}", title, e.getMessage());
            Assert.fail("Core deletion automation lifecycle interrupted: " + e.getMessage());
        }
    }

    /**
     * Loops through the dashboard layout and purges every visible note card.
     * Ideal for after-test teardown sequences to guarantee a clean slate.
     */
    @Step("Teardown: Purge all notes from the dashboard")
    public void deleteAllNotes() {
        log.info("Starting automated dashboard purge sequence...");

        // Loop continuously as long as there is at least one card element visible
        while (getNoteCount() > 0) {
            try {
                // Fetch fresh lists on each iteration to avoid StaleElementReferenceExceptions
                List<WebElement> noteCards = driver.findElements(NOTES_LIST);
                if (noteCards.isEmpty())
                    break;

                WebElement firstCard = noteCards.get(0);
                WebElement titleElement = firstCard.findElement(By.cssSelector("[data-testid='note-card-title']"));
                String targetTitle = titleElement.getText().trim();

                log.info("Purging note: '{}'", targetTitle);

                // Locate and click the delete button on this card
                WebElement deleteBtn = firstCard.findElement(By.cssSelector("button[data-testid='note-delete']"));
                wait.until(ExpectedConditions.elementToBeClickable(deleteBtn));

                try {
                    deleteBtn.click();
                } catch (Exception e) {
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", deleteBtn);
                }

                // Confirm the deletion in the modal popup overlay
                WebElement confirmBtn = wait.until(ExpectedConditions.elementToBeClickable(CONFIRM_DELETE_BTN));
                try {
                    confirmBtn.click();
                } catch (Exception e) {
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", confirmBtn);
                }

                // Wait for the UI layout engine to finish detaching this node
                wait.until(ExpectedConditions.stalenessOf(titleElement));

                // Optional micro-sleep to allow React state transitions to stabilize smoothly
                Thread.sleep(300);

            } catch (Exception e) {
                log.warn("Purge loop encountered a temporary synchronization variance: {}. Retrying...",
                        e.getMessage());
                // Refresh the page to clear any stuck UI modals and try again
                refreshPage();
            }
        }
        log.info("Purge sequence complete. Dashboard is completely clean.");
    }

    public void refreshPage() {
        driver.navigate().refresh();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ignored) {
        }
        isDashboardLoaded();
    }

    public void logout() {
        try {
            wait.until(ExpectedConditions.elementToBeClickable(USER_MENU)).click();
            wait.until(ExpectedConditions.elementToBeClickable(LOGOUT_LINK)).click();
        } catch (Exception e) {
            navigateTo(driver.getCurrentUrl().replace("/notes/app", "/notes/app/login"));
        }
    }
}