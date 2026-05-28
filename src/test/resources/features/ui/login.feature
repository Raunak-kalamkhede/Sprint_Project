# ============================================================
# Feature: UI Login
# Author: Raunak | raunak@gmail.com
# Requirements: FR-01, FR-09
# Scenarios: TS-UI-01, TS-UI-02
# ============================================================

@ui @smoke
Feature: UI - Login and Authentication

  Background:
    Given the user is on the login page

  # ============================================================
  # TS-UI-01: Successful Login with Valid Credentials
  # Requirement: FR-01
  # ============================================================
  @ui @smoke @TS-UI-01
  Scenario: TS-UI-01 - Successful login with valid credentials
    When the user logs in with valid credentials
    Then the user should be redirected to the notes dashboard
    And the page load time should be acceptable

  
  # ============================================================
  # Data-Driven Login using Scenario Outline
  # Requirement: FR-01, FR-09
  # ============================================================
  @ui @negative @TS-UI-02
  Scenario Outline: TS-UI-02d - Login with various invalid credentials
    When the user logs in with email "<email>" and password "<password>"
    Then the login error message should be displayed

    Examples:
      | email                  | password       |
      | notregistered@test.com | Password123    |
