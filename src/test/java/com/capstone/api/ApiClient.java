package com.capstone.api;

import com.capstone.config.ConfigReader;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;

/**
 * ApiClient — Central class for ALL RestAssured API calls.
 *
 * WHY: DRY principle. Base request specification (URL, headers, logging, Allure filter)
 * is defined ONCE. All endpoint methods reuse it. If the base URL changes,
 * only ConfigReader needs updating.
 *
 * The AllureRestAssured filter automatically attaches every API request + response
 * to the Allure report — no manual logging needed.
 */
public class ApiClient {

    private static final Logger log = LogManager.getLogger(ApiClient.class);

    private final String baseUri;

    public ApiClient() {
        this.baseUri = ConfigReader.getApiBaseUrl();
        log.info("ApiClient initialised. Base URI: {}", baseUri);
    }

    // ==========================================
    // REQUEST SPECIFICATIONS
    // ==========================================

    /**
     * Creates the base RequestSpecification.
     * Pre-fills: Base URI, Content-Type, Accept, Allure filter, request/response logging.
     *
     * All API methods use this as their foundation.
     */
    private RequestSpecification baseSpec() {
        return new RequestSpecBuilder()
                .setBaseUri(baseUri)
                .setContentType("application/json")
                .addHeader("Accept", "application/json")
                .addFilter(new AllureRestAssured())       // Auto-attaches to Allure report
                .addFilter(new RequestLoggingFilter(LogDetail.ALL))   // Log all request details
                .addFilter(new ResponseLoggingFilter(LogDetail.ALL))  // Log all response details
                .build();
    }

    /**
     * Creates an authenticated RequestSpecification.
     * Adds the x-auth-token header required by protected endpoints.
     *
     * @param token JWT token received from login response
     */
    private RequestSpecification authSpec(String token) {
        return new RequestSpecBuilder()
                .addRequestSpecification(baseSpec())
                .addHeader("x-auth-token", token)
                .build();
    }

    // ==========================================
    // AUTH ENDPOINTS
    // ==========================================

    /**
     * POST /users/login — Authenticates user and returns JWT token.
     *
     * Requirement: FR-01
     * @param email    User email
     * @param password User password
     * @return Full Response object (caller extracts token, status code, etc.)
     */
    public Response login(String email, String password) {
        log.info("API Login | email: {}", email);

        Map<String, String> body = new HashMap<>();
        body.put("email", email);
        body.put("password", password);

        Response response = given()
                .spec(baseSpec())
                .body(body)
                .when()
                .post("/users/login")
                .then()
                .extract()
                .response();

        log.info("Login response status: {} | time: {}ms", response.getStatusCode(), response.getTime());
        return response;
    }

    /**
     * Extracts the auth token from a login response.
     *
     * @param loginResponse Response from the login() method
     * @return JWT auth token string
     */
    public String extractToken(Response loginResponse) {
        String token = loginResponse.jsonPath().getString("data.token");
        if (token == null || token.isEmpty()) {
            throw new RuntimeException("Auth token not found in login response. Response: " + loginResponse.asString());
        }
        log.info("Auth token extracted successfully (length: {})", token.length());
        return token;
    }

    // ==========================================
    // NOTES ENDPOINTS
    // ==========================================

    /**
     * GET /notes — Returns all notes for the authenticated user.
     *
     * Requirement: FR-04
     * @param token Auth token
     * @return Response containing list of notes
     */
    public Response getAllNotes(String token) {
        log.info("API GET /notes");

        return given()
                .spec(authSpec(token))
                .when()
                .get("/notes")
                .then()
                .extract()
                .response();
    }

    /**
     * POST /notes — Creates a new note via API.
     *
     * Requirement: FR-06
     * @param token       Auth token
     * @param category    "Home" | "Work" | "Personal"
     * @param title       Note title
     * @param description Note description
     * @return Response containing created note data
     */
    public Response createNote(String token, String category, String title, String description) {
        log.info("API POST /notes | category: {} | title: {}", category, title);

        Map<String, String> body = new HashMap<>();
        body.put("category", category);
        body.put("title", title);
        body.put("description", description);

        return given()
                .spec(authSpec(token))
                .body(body)
                .when()
                .post("/notes")
                .then()
                .extract()
                .response();
    }

    /**
     * DELETE /notes/{id} — Deletes a note by ID.
     *
     * Requirement: FR-06
     * @param token  Auth token
     * @param noteId Note ID from GET /notes response
     * @return Response (200 on success)
     */
    public Response deleteNote(String token, String noteId) {
        log.info("API DELETE /notes/{}", noteId);

        return given()
                .spec(authSpec(token))
                .when()
                .delete("/notes/" + noteId)
                .then()
                .extract()
                .response();
    }

    /**
     * GET /notes/{id} — Gets a specific note by ID.
     *
     * Requirement: FR-04, FR-05
     * @param token  Auth token
     * @param noteId Note ID
     * @return Response containing note data
     */
    public Response getNoteById(String token, String noteId) {
        log.info("API GET /notes/{}", noteId);

        return given()
                .spec(authSpec(token))
                .when()
                .get("/notes/" + noteId)
                .then()
                .extract()
                .response();
    }

    /**
     * GET /notes (no token) — Tests unauthorized access.
     *
     * Requirement: FR-09 (negative scenarios)
     * @return Response (should be 401)
     */
    public Response getNotesWithoutToken() {
        log.info("API GET /notes WITHOUT token (negative test)");

        return given()
                .spec(baseSpec())
                .when()
                .get("/notes")
                .then()
                .extract()
                .response();
    }

    /**
     * GET /notes with an invalid/expired token.
     *
     * Requirement: FR-09 (negative scenarios)
     * @param invalidToken Deliberately invalid token string
     * @return Response (should be 401)
     */
    public Response getNotesWithInvalidToken(String invalidToken) {
        log.info("API GET /notes with INVALID token (negative test)");

        return given()
                .spec(authSpec(invalidToken))
                .when()
                .get("/notes")
                .then()
                .extract()
                .response();
    }

    // ==========================================
    // PERFORMANCE CHECK
    // ==========================================

    /**
     * Checks if an API response time is within the configured threshold.
     *
     * Requirement: FR-08
     * @param response Response from any API call
     * @return true if response time < api.response.threshold.ms
     */
    public boolean isResponseTimeAcceptable(Response response) {
        long responseTime = response.getTime();
        long threshold = ConfigReader.getApiThresholdMs();
        boolean acceptable = responseTime < threshold;

        if (acceptable) {
            log.info("PERFORMANCE OK: Response time {}ms < threshold {}ms", responseTime, threshold);
        } else {
            log.warn("PERFORMANCE FAIL: Response time {}ms >= threshold {}ms", responseTime, threshold);
        }
        return acceptable;
    }

    /**
     * Gets the response time in milliseconds from a Response.
     */
    public long getResponseTimeMs(Response response) {
        return response.getTime();
    }

    // ==========================================
    // JSON PATH HELPERS
    // ==========================================

    /**
     * Finds a note by title in a GET /notes response.
     * Returns the note's ID if found, null if not found.
     *
     * @param notesResponse Response from getAllNotes()
     * @param title         Title to search for
     * @return Note ID string or null
     */
    public String findNoteIdByTitle(Response notesResponse, String title) {
        try {
            // API returns: { "data": [ { "id": "...", "title": "...", ... } ] }
            int count = notesResponse.jsonPath().getList("data").size();
            for (int i = 0; i < count; i++) {
                String noteTitle = notesResponse.jsonPath().getString("data[" + i + "].title");
                if (title.equalsIgnoreCase(noteTitle)) {
                    return notesResponse.jsonPath().getString("data[" + i + "].id");
                }
            }
        } catch (Exception e) {
            log.warn("Could not search for note by title: {}", e.getMessage());
        }
        log.warn("Note with title '{}' not found in API response", title);
        return null;
    }

    /**
     * Checks if a note with the given title exists in the GET /notes response.
     */
    public boolean noteExistsInApi(Response notesResponse, String title) {
        return findNoteIdByTitle(notesResponse, title) != null;
    }
}
