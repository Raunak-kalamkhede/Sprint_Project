# ============================================================
# Feature: UI Notes Management
# Author: Raunak | raunak@gmail.com
# Requirements: FR-02, FR-03, FR-07
# Scenarios: TS-UI-03, TS-UI-04, TS-UI-07
# ============================================================

@ui
Feature: UI - Notes CRUD Operations

  Background:
    Given the user is logged in to the notes dashboard

  # ============================================================
  # TS-UI-03: Create a Note and Verify it Appears in List
  # Requirements: FR-02, FR-03
  # ============================================================
  @ui @smoke @TS-UI-03
  Scenario: TS-UI-03 - Create a note and verify it appears in the list
    When the user creates a note with category "Home", title "My First Test Note", and description "This note was created by Selenium automation"
    Then the note with title "My First Test Note" should appear in the notes list
    And the dashboard page load time should be within limits

  
  # Requirements: FR-07
  # ============================================================
  @ui @TS-UI-07
  Scenario: TS-UI-07 - Delete a note and verify it is removed from the list
    When the user creates a note with category "Work", title "Note To Be Deleted", and description "This note will be deleted"
    And the note with title "Note To Be Deleted" should appear in the notes list
    When the user deletes the note titled "Note To Be Deleted"
    Then the note with title "Note To Be Deleted" should NOT appear in the notes list

