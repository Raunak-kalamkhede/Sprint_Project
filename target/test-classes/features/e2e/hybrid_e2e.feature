# ============================================================
# Feature: Hybrid E2E Tests — UI + API Cross-Layer Validation
# Author: Raunak | raunak@gmail.com
# Requirements: FR-05, FR-07
# Scenarios: TS-E2E-01, TS-E2E-02, TS-E2E-04
#
# WHAT IS A HYBRID E2E TEST?
# These tests use BOTH Selenium (browser) AND RestAssured (API)
# in the same scenario. This validates that the UI and backend
# are in sync — data created in one layer appears correctly in the other.
# ============================================================

@e2e
Feature: E2E - Hybrid UI and API Cross-Layer Validation

  # ============================================================
  # TS-E2E-01: Note Created via UI Appears in API Response
  # Requirements: FR-05
  #
  # Flow:
  #   1. Login via API → get token
  #   2. Login via UI → create note in browser
  #   3. Call GET /notes API → verify note exists in API response
  #   4. Compare UI and API data field-by-field
  # ============================================================
  @e2e @smoke @TS-E2E-01
  Scenario: TS-E2E-01 - Note created via UI appears in the API response
    Given I authenticate via API and store the token
    And the user is logged in to the notes dashboard
    When the user creates a note with category "Home", title "E2E Sync Test Note", and description "Created in browser - must appear in API"
    Then the created note should appear in the notes list
    And the note created in UI should appear in the API response
    # FIX: Check performance timings immediately while the main response object is active
    And the API response time should be within 2 seconds
    And the note category and title should match between UI and API

  # ============================================================
  # TS-E2E-02: Note Deleted via API Disappears from UI
  # Requirements: FR-07
  #
  # Flow (reverse sync test):
  #   1. Login via API → get token
  #   2. Create note via API
  #   3. Login via UI → verify note is visible in browser
  #   4. Delete note via API (no browser!)
  #   5. Refresh UI → verify note is GONE from browser
  # ============================================================
  @e2e @TS-E2E-02
  Scenario: TS-E2E-02 - Note deleted via API disappears from the UI
    Given I authenticate via API and store the token
    When I create a note via API with category "Work", title "E2E Reverse Sync Note", and description "Created via API - will be deleted via API"
    Then the API response status should be 200
    And the user is logged in to the notes dashboard
    And the note with title "E2E Reverse Sync Note" should appear in the notes list
    When I delete the note via API using the stored note ID
    Then the API response status should be 200
    And the note deleted via API should not appear in the UI

  # ============================================================
  # TS-E2E-04: Complete Note Lifecycle
  # Requirements: FR-01, FR-02, FR-05, FR-06, FR-07
  #
  # Flow (full CRUD lifecycle):
  #   1. Authenticate (API)
  #   2. Login (UI)
  #   3. Create note (UI)
  #   4. Verify note in UI list
  #   5. Verify note in API response (sync check)
  #   6. Find note ID in API
  #   7. Delete note via API
  #   8. Verify note gone from UI
  # ============================================================
  @e2e @TS-E2E-04
  Scenario: TS-E2E-04 - Complete note lifecycle: create in UI, verify in API, delete via API, verify removal in UI
    Given I authenticate via API and store the token
    And the user is logged in to the notes dashboard
    When the user creates a note with category "Personal", title "Full Lifecycle E2E Note", and description "Complete lifecycle: UI create, API verify, API delete, UI confirm"
    Then the created note should appear in the notes list
    And the note created in UI should appear in the API response
    And the note category and title should match between UI and API
    When I find the note ID in the API and store it
    And I delete the note via API using the stored note ID
    Then the API response status should be 200
    And the note deleted via API should not appear in the UI
    And the API response time should be within 2 seconds

 