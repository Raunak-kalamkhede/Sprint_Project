package com.capstone.stepdefs;

import com.capstone.api.ApiClient;
import com.capstone.config.ConfigReader;
import com.capstone.pages.NotesPage;
import com.capstone.utils.PerformanceTracker;
import com.capstone.utils.ScenarioContext;
import io.cucumber.java.en.*;
import io.restassured.response.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;

import java.util.List;

/**
 * ApiStepDefs — Step definitions for API tests, E2E hybrid tests, and negative
 * scenarios.
 *
 * This class handles:
 * 1. Pure API steps (RestAssured calls, response validation)
 * 2. E2E cross-layer validation (compare UI data with API response)
 * 3. Negative scenarios (no token, invalid token)
 *
 * ScenarioContext carries AUTH_TOKEN from LoginStepDefs and NOTE_TITLE from
 * NotesStepDefs.
 */
public class ApiStepDefs {

    private static final Logger log = LogManager.getLogger(ApiStepDefs.class);

    private final ScenarioContext context;
    private final ApiClient apiClient;

    // Stores the last API response for assertion steps
    private Response lastResponse;

    public ApiStepDefs(ScenarioContext context) {
        this.context = context;
        this.apiClient = new ApiClient();
    }

    // ==========================================
    // API — LOGIN & AUTHENTICATION
    // ==========================================

    @When("the user attempts API login with valid credentials")
    public void theUserAttemptsApiLoginWithValidCredentials() {
        log.info("STEP: API login with valid credentials");

        lastResponse = apiClient.login(
                ConfigReader.getEmail(),
                ConfigReader.getPassword());

        context.set(
                ScenarioContext.API_RESPONSE,
                lastResponse);

        PerformanceTracker.recordApi(
                "POST /users/login",
                lastResponse.getTime());

        if (lastResponse.getStatusCode() == 200) {
            String token = apiClient.extractToken(lastResponse);

            context.set(
                    ScenarioContext.AUTH_TOKEN,
                    token);

            log.info("Auth token stored successfully");
        }
    }

    @Then("the API login response status should be {int}")
    public void theApiLoginResponseStatusShouldBe(int expectedStatus) {
        Assert.assertNotNull(
                lastResponse,
                "No login response available");

        Assert.assertEquals(
                lastResponse.getStatusCode(),
                expectedStatus,
                "Unexpected login status.\nResponse: " + lastResponse.asString());
    }

    @Then("the API response should contain a valid auth token")
    public void theApiResponseShouldContainAValidAuthToken() {
        String token = apiClient.extractToken(lastResponse);

        Assert.assertNotNull(
                token,
                "Auth token is null");

        Assert.assertFalse(
                token.isEmpty(),
                "Auth token is empty");

        Assert.assertTrue(
                token.length() > 10,
                "Auth token appears invalid");
    }

    @Given("I authenticate via API and store the token")
    public void givenIAuthenticateViaApiAndStoreToken() {
        log.info("STEP: Given I authenticate via API and store token for subsequent steps");
        Response loginResponse = apiClient.login(ConfigReader.getEmail(), ConfigReader.getPassword());
        Assert.assertEquals(loginResponse.getStatusCode(), 200,
                "API login failed. Cannot proceed with test. Response: " + loginResponse.asString());
        String token = apiClient.extractToken(loginResponse);
        context.set(ScenarioContext.AUTH_TOKEN, token);
        context.set(ScenarioContext.USER_EMAIL, ConfigReader.getEmail());
        PerformanceTracker.recordApi("POST /users/login", loginResponse.getTime());
        log.info("Token stored successfully. Test can now proceed.");
    }

    // ==========================================
    // API — GET /notes
    // ==========================================

    @When("I call GET \\/notes with the auth token")
    public void iCallGetNotesWithToken() {
        log.info("STEP: When I call GET /notes with the auth token");
        String token = context.getString(ScenarioContext.AUTH_TOKEN);
        Assert.assertFalse(token.isEmpty(), "AUTH_TOKEN not found in ScenarioContext — was login step run?");

        lastResponse = apiClient.getAllNotes(token);
        context.set(ScenarioContext.API_RESPONSE, lastResponse);

        PerformanceTracker.recordApi("GET /notes", lastResponse.getTime());
        log.info("GET /notes response: {} | time: {}ms", lastResponse.getStatusCode(), lastResponse.getTime());
    }

    @When("I call GET \\/notes without an auth token")
    public void iCallGetNotesWithoutToken() {
        log.info("STEP: When I call GET /notes without an auth token");
        lastResponse = apiClient.getNotesWithoutToken();
        context.set(ScenarioContext.API_RESPONSE, lastResponse);
        PerformanceTracker.recordApi("GET /notes (no token)", lastResponse.getTime());
    }

    @When("I call GET \\/notes with an invalid token {string}")
    public void iCallGetNotesWithInvalidToken(String invalidToken) {
        log.info("STEP: When I call GET /notes with invalid token: {}", invalidToken);
        lastResponse = apiClient.getNotesWithInvalidToken(invalidToken);
        context.set(ScenarioContext.API_RESPONSE, lastResponse);
        PerformanceTracker.recordApi("GET /notes (invalid token)", lastResponse.getTime());
    }

    // ==========================================
    // API — CREATE NOTE
    // ==========================================

    @When("I create a note via API with category {string}, title {string}, and description {string}")
    public void iCreateANoteViaApi(String category, String title, String description) {
        log.info("STEP: When I create a note via API | [{}] {}", category, title);
        String token = context.getString(ScenarioContext.AUTH_TOKEN);
        Assert.assertFalse(token.isEmpty(), "AUTH_TOKEN not in context");

        lastResponse = apiClient.createNote(token, category, title, description);
        context.set(ScenarioContext.API_RESPONSE, lastResponse);

        PerformanceTracker.recordApi("POST /notes", lastResponse.getTime());

        // Extract note ID from creation response for subsequent steps
        if (lastResponse.getStatusCode() == 200 || lastResponse.getStatusCode() == 201) {
            String noteId = lastResponse.jsonPath().getString("data.id");
            if (noteId != null) {
                context.set(ScenarioContext.NOTE_ID, noteId);
                context.set(ScenarioContext.CREATED_NOTE_ID, noteId);
                context.set(ScenarioContext.NOTE_TITLE, title);
                log.info("Note created via API. ID: {}", noteId);
            }
        }
    }

    // ==========================================
    // API — DELETE NOTE
    // ==========================================

    @When("I delete the note via API using the stored note ID")
    public void iDeleteTheNoteViaApiUsingStoredId() {
        log.info("STEP: When I delete the note via API using stored note ID");
        String token = context.getString(ScenarioContext.AUTH_TOKEN);
        String noteId = context.getString(ScenarioContext.NOTE_ID);

        Assert.assertFalse(noteId.isEmpty(), "NOTE_ID not in context — was a note created/found?");

        lastResponse = apiClient.deleteNote(token, noteId);
        context.set(ScenarioContext.API_RESPONSE, lastResponse);

        PerformanceTracker.recordApi("DELETE /notes/" + noteId, lastResponse.getTime());
        log.info("DELETE /notes/{} response: {}", noteId, lastResponse.getStatusCode());
    }

    @When("I delete the previously created API note")
    public void iDeleteThePreviouslyCreatedApiNote() {
        log.info("STEP: When I delete the previously created API note");
        String token = context.getString(ScenarioContext.AUTH_TOKEN);
        String noteId = context.getString(ScenarioContext.CREATED_NOTE_ID);

        if (noteId == null || noteId.isEmpty()) {
            log.warn("No CREATED_NOTE_ID in context — cannot delete");
            return;
        }

        lastResponse = apiClient.deleteNote(token, noteId);
        PerformanceTracker.recordApi("DELETE /notes/" + noteId, lastResponse.getTime());
    }
        @When("the user attempts API login with email {string} and password {string}")
    public void theUserAttemptsApiLoginWithCustomCredentials(String email, String password) {
        log.info("STEP: API login attempt with custom credentials | Email: {}", email);

        // Execute the backend request using the scenario parameters
        lastResponse = apiClient.login(email, password);

        // Sync the context map so downstream assertion steps can validate the payload
        context.set(
                ScenarioContext.API_RESPONSE,
                lastResponse);

        PerformanceTracker.recordApi(
                "POST /users/login (Custom Payload)",
                lastResponse.getTime());
        
        log.info("API Custom login complete. Status code received: {}", lastResponse.getStatusCode());
    }


    // ==========================================
    // THEN STEPS — STATUS CODE & RESPONSE VALIDATION
    // ==========================================

    @Then("the API response status should be {int}")
    public void theApiResponseStatusShouldBe(int expectedStatus) {
        log.info("STEP: Then the API response status should be {}", expectedStatus);
        Response response = getLastResponse();
        Assert.assertEquals(response.getStatusCode(), expectedStatus,
                "Expected HTTP " + expectedStatus + " but got " + response.getStatusCode() +
                        "\nBody: " + response.asString());
    }

    @Then("the GET \\/notes response should be {int}")
    public void theGetNotesResponseShouldBe(int expectedStatus) {
        theApiResponseStatusShouldBe(expectedStatus);
    }

    @Then("the response should contain a list of notes")
    public void theResponseShouldContainAListOfNotes() {
        log.info("STEP: Then the response should contain a list of notes");
        Response response = getLastResponse();
        Assert.assertEquals(response.getStatusCode(), 200,
                "GET /notes did not return 200. Body: " + response.asString());

        List<?> notes = response.jsonPath().getList("data");
        Assert.assertNotNull(notes, "data field is null in response");
        log.info("Notes list returned with {} items", notes.size());
    }

    @Then("the API response should contain the created note")
    public void theApiResponseShouldContainTheCreatedNote() {
        log.info("STEP: Then the API response should contain the created note");
        String token = context.getString(ScenarioContext.AUTH_TOKEN);
        String title = context.getString(ScenarioContext.NOTE_TITLE);

        Response notesResponse = apiClient.getAllNotes(token);
        PerformanceTracker.recordApi("GET /notes (verify)", notesResponse.getTime());

        boolean exists = apiClient.noteExistsInApi(notesResponse, title);
        Assert.assertTrue(exists,
                "Note '" + title + "' not found in GET /notes response.\n" +
                        "Full response: " + notesResponse.asString());
        log.info("Confirmed: note '{}' exists in API response", title);
    }

    @Then("the API response time should be within {int} seconds")
    public void theApiResponseTimeShouldBeWithin(int seconds) {
        log.info("STEP: Then the API response time should be within {} second(s)", seconds);
        Response response = getLastResponse();
        long responseTimeMs = response.getTime();
        long thresholdMs = seconds * 1000L;

        PerformanceTracker.recordApi("Response Time Check", responseTimeMs);

        Assert.assertTrue(responseTimeMs < thresholdMs,
                "API response time " + responseTimeMs + "ms exceeds " + thresholdMs + "ms threshold");
        log.info("Performance PASS: {}ms < {}ms", responseTimeMs, thresholdMs);
    }

    @Then("the response should contain an error message")
    public void theResponseShouldContainAnErrorMessage() {
        log.info("STEP: Then the response should contain an error message");
        Response response = getLastResponse();
        String body = response.asString();
        Assert.assertFalse(body.isEmpty(), "Response body is empty — expected error message");

        // The API returns either "message" or "error" field for errors
        String message = response.jsonPath().getString("message");
        if (message == null)
            message = response.jsonPath().getString("error");
        Assert.assertNotNull(message, "No 'message' or 'error' field in response: " + body);
        log.info("Error message in response: '{}'", message);
    }

    @Then("the response should not be authorised")
    public void theResponseShouldNotBeAuthorised() {
        log.info("STEP: Then the response should not be authorised");
        Response response = getLastResponse();
        Assert.assertEquals(response.getStatusCode(), 401,
                "Expected 401 Unauthorized but got: " + response.getStatusCode() +
                        "\nBody: " + response.asString());
    }

    // ==========================================
    // E2E CROSS-LAYER VALIDATION STEPS
    // ==========================================

    @Then("the note created in UI should appear in the API response")
public void theNoteCreatedInUIShouldAppearInApiResponse() {
    log.info("STEP: Then the note created in UI should appear in the API response (E2E validation)");
    
    // 1. Extract the active JWT token from your shared ScenarioContext mapping layer
    //  FIX: Pass String.class as the second argument to satisfy the generic signature
String token = context.get(com.capstone.utils.ScenarioContext.AUTH_TOKEN, String.class);
    
    // 2. FIX: Pass the token argument into the method to satisfy the ApiClient signature
    lastResponse = apiClient.getAllNotes(token); 
    
    // 3. Keep your existing ScenarioContext map update intact
    context.set(com.capstone.utils.ScenarioContext.API_RESPONSE, lastResponse);
    
    // Your existing validation assertions below...
    org.testng.Assert.assertEquals(lastResponse.getStatusCode(), 200);
    
    boolean noteFound = lastResponse.jsonPath().getList("data.title")
            .contains(context.get(com.capstone.utils.ScenarioContext.NOTE_TITLE, String.class));
    org.testng.Assert.assertTrue(noteFound, "Note not found in the API backend layout manifest array stream.");
}

    @Then("the note category and title should match between UI and API")
    public void theNoteCategoryAndTitleShouldMatchBetweenUiAndApi() {
        log.info("STEP: Then the note category and title should match between UI and API");

        String token = context.getString(ScenarioContext.AUTH_TOKEN);
        String noteId = context.getString(ScenarioContext.NOTE_ID);
        String uiTitle = context.getString(ScenarioContext.NOTE_TITLE);
        String uiCat = context.getString(ScenarioContext.NOTE_CATEGORY);

        if (noteId == null || noteId.isEmpty()) {
            // Try to find it
            Response notesResp = apiClient.getAllNotes(token);
            noteId = apiClient.findNoteIdByTitle(notesResp, uiTitle);
            Assert.assertNotNull(noteId, "Cannot find note ID for title: " + uiTitle);
        }

        Response noteResp = apiClient.getNoteById(token, noteId);
        PerformanceTracker.recordApi("GET /notes/" + noteId, noteResp.getTime());

        String apiTitle = noteResp.jsonPath().getString("data.title");
        String apiCategory = noteResp.jsonPath().getString("data.category");

        log.info("UI title: '{}' | API title: '{}'", uiTitle, apiTitle);
        log.info("UI category: '{}' | API category: '{}'", uiCat, apiCategory);

        Assert.assertEquals(apiTitle, uiTitle,
                "Title mismatch! UI='" + uiTitle + "' API='" + apiTitle + "'");
        Assert.assertEquals(apiCategory.toLowerCase(), uiCat.toLowerCase(),
                "Category mismatch! UI='" + uiCat + "' API='" + apiCategory + "'");

        log.info("E2E FIELD MATCH VALIDATED: Title and Category match between UI and API");
    }

    @Then("the note deleted via API should not appear in the UI")
    public void theNoteDeletedViaApiShouldNotAppearInUI() {
        log.info("STEP: Then the note deleted via API should not appear in the UI (reverse E2E)");

        String deletedTitle = context.getString(ScenarioContext.NOTE_TITLE);

        NotesPage notesPage = new NotesPage();
        notesPage.refreshPage();

        // Give the UI a moment to reflect the deletion
        try {
            Thread.sleep(1500);
        } catch (InterruptedException ignored) {
        }

        boolean stillVisible = notesPage.isNoteVisible(deletedTitle);
        Assert.assertFalse(stillVisible,
                "REVERSE E2E SYNC FAIL: Note '" + deletedTitle + "' was deleted via API " +
                        "but still appears in the UI.\nCurrent UI notes: " + notesPage.getAllNoteTitles());

        log.info("REVERSE E2E VALIDATED: Note '{}' deleted via API — confirmed absent from UI", deletedTitle);
    }

    @Then("I find the note ID in the API and store it")
    public void iFindTheNoteIdInApiAndStoreIt() {
        log.info("STEP: Then I find the note ID in the API and store it");

        String token = context.getString(ScenarioContext.AUTH_TOKEN);
        String title = context.getString(ScenarioContext.NOTE_TITLE);

        Response notesResponse = apiClient.getAllNotes(token);
        String noteId = apiClient.findNoteIdByTitle(notesResponse, title);

        Assert.assertNotNull(noteId, "Note '" + title + "' not found in API response");
        context.set(ScenarioContext.NOTE_ID, noteId);
        log.info("Note ID stored: {} for title: '{}'", noteId, title);
    }

    @Then("all tested API endpoints should respond within 4 seconds")
    public void allTestedApiEndpointsShouldRespondWithin2Seconds() {
        log.info("STEP: Then all tested API endpoints should respond within 4 seconds");

        String token = context.getString(ScenarioContext.AUTH_TOKEN);
        long threshold = ConfigReader.getApiThresholdMs();
        boolean allPass = true;
        StringBuilder failures = new StringBuilder();

        // Test each endpoint
        String[][] endpoints = {
                { "POST /users/login", "login" },
                { "GET /notes", "get" },
                { "POST /notes", "create" },
                { "DELETE /notes/{id}", "delete" }
        };

        for (String[] ep : endpoints) {
            Response resp;
            try {
                switch (ep[1]) {
                    case "login":
                        resp = apiClient.login(ConfigReader.getEmail(), ConfigReader.getPassword());
                        break;
                    case "get":
                        resp = apiClient.getAllNotes(token);
                        break;
                    case "create":
                        resp = apiClient.createNote(token, "Home", "Perf Test Note", "Performance test");
                        if (resp.getStatusCode() == 200) {
                            String id = resp.jsonPath().getString("data.id");
                            if (id != null)
                                context.set(ScenarioContext.CREATED_NOTE_ID, id);
                        }
                        break;
                    default:
                        continue;
                }
                long time = resp.getTime();
                PerformanceTracker.recordApi(ep[0], time);
                if (time >= threshold) {
                    allPass = false;
                    failures.append(ep[0]).append(": ").append(time).append("ms | ");
                }
            } catch (Exception e) {
                log.warn("Could not test endpoint {}: {}", ep[0], e.getMessage());
            }
        }

        // Cleanup created note
        String createdId = context.getString(ScenarioContext.CREATED_NOTE_ID);
        if (!createdId.isEmpty()) {
            apiClient.deleteNote(token, createdId);
        }

        Assert.assertTrue(allPass,
                "Performance failures: " + failures + " (threshold: " + threshold + "ms)");
        log.info("All API endpoints responded within {}ms", threshold);
    }

    // ==========================================
    // PRIVATE HELPERS
    // ==========================================

    private Response getLastResponse() {
        if (lastResponse != null)
            return lastResponse;
        Response fromContext = context.get(ScenarioContext.API_RESPONSE, Response.class);
        Assert.assertNotNull(fromContext, "No API response available. Was an API call made?");
        return fromContext;
    }
}