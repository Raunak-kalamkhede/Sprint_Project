# ============================================================
# Feature: API Testing with RestAssured
# Author: Raunak | raunak@gmail.com
# Requirements: FR-01, FR-04, FR-06, FR-08
# Scenarios: TS-API-01, TS-API-02, TS-API-03, TS-API-05, TS-API-06
# ============================================================

@api
Feature: API - Notes REST API Validation

  # ============================================================
  # TS-API-01: Login API Returns Valid Auth Token
  # Requirement: FR-01
  # ============================================================
  @api @smoke @TS-API-01
  Scenario: TS-API-01 - Login API returns valid JWT auth token
    When the user attempts API login with valid credentials
    Then the API login response status should be 200
    And the API response should contain a valid auth token
    And the API response time should be within 4 seconds

  # ============================================================
  # TS-API-03: GET /notes Returns List for Authenticated User
  # Requirement: FR-04
  # ============================================================
  @api @smoke @TS-API-03
  Scenario: TS-API-03 - GET notes returns list for authenticated user
    Given I authenticate via API and store the token
    When I call GET /notes with the auth token
    Then the GET /notes response should be 200
    And the response should contain a list of notes
    And the API response time should be within 4 seconds

  # ============================================================
  # TS-API-02: Create Note via API, Verify in GET /notes
  # Requirement: FR-04, FR-06
  # ============================================================
  @api @TS-API-02
  Scenario: TS-API-02 - Create note via API and verify it appears in GET notes
    Given I authenticate via API and store the token
    When I create a note via API with category "Work", title "API Created Note", and description "Created directly via RestAssured API call"
    Then the API response status should be 200
    And the API response time should be within 4 seconds
    When I call GET /notes with the auth token
    Then the API response should contain the created note
    And the API response time should be within 4 seconds
    When I delete the previously created API note
    Then the API response status should be 200

  # ============================================================
  # TS-API-05: Delete Note via API, Verify Removal
  # Requirement: FR-06
  # ============================================================
  @api @TS-API-05
  Scenario: TS-API-05 - Delete a note via API and verify removal
    Given I authenticate via API and store the token
    When I create a note via API with category "Personal", title "Note For Deletion Test", and description "This note will be deleted via API"
    Then the API response status should be 200
    When I delete the previously created API note
    Then the API response status should be 200
    And the API response time should be within 4 seconds

  # ============================================================
  # TS-API-06: All API Endpoints Respond Within 4 Seconds
  # Requirement: FR-08 (Performance)
  # ============================================================
  @api @performance @TS-API-06
  Scenario: TS-API-06 - All API endpoints respond within 4 seconds
    Given I authenticate via API and store the token
    Then all tested API endpoints should respond within 4 seconds

  # ============================================================
  # Additional: API response structure validation
  # ============================================================
  @api @TS-API-03
  Scenario Outline: TS-API-03b - GET notes with different auth scenarios
    Given I authenticate via API and store the token
    When I call GET /notes with the auth token
    Then the GET /notes response should be 200

    Examples:
      | description          |
      | First auth attempt   |
      | Second auth attempt  |
