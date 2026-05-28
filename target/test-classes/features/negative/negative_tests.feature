# ============================================================
# Feature: Negative Test Scenarios
# Author: Raunak | raunak@gmail.com
# Requirements: FR-09
# ============================================================

@negative
Feature: Negative - Security and Error Handling Validation

  # ============================================================
  # TS-NEG-01: Unauthorized Access — No Token Provided
  # ============================================================
  @negative @smoke @TS-NEG-01
  Scenario: TS-NEG-01 - Access rejected when no auth token is provided
    When I call GET /notes without an auth token
    Then the response should not be authorised
    And the response should contain an error message
    And the API response time should be within 5 seconds

  # ============================================================
  # TS-NEG-02: Unauthorized Access — Invalid Token Formats
  # ============================================================
  @negative @TS-NEG-02
  Scenario: TS-NEG-02 - Access rejected with a gibberish auth token
    When I call GET /notes with an invalid token "this-is-not-a-valid-token-xyz123"
    Then the response should not be authorised
    And the response should contain an error message

  @negative @TS-NEG-02b
  Scenario: TS-NEG-02b - Access rejected with an expired-format JWT token
    When I call GET /notes with an invalid token "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.INVALID.SIGNATURE"
    Then the response should not be authorised

  # ============================================================
  # TS-NEG-03: UI Login Boundary Edge Cases
  # ============================================================
  @negative @ui @TS-NEG-03
  Scenario Outline: TS-NEG-03 - UI login rejected for invalid credentials
    Given the user is on the login page
    When the user logs in with email "<email>" and password "<password>"
    Then the login error message should be displayed

    Examples:
      | email                      | password        |
      | nonexistent@capstone.com   | ValidPass123    |
      | raunak@gmail.com           | incorrectpass   |
      | hacker@evil.com            | password        |

  # ============================================================
  # TS-NEG-04: Pure API Login Failure Validation
  # ============================================================
  @negative @api @TS-NEG-04
  Scenario Outline: TS-NEG-04 - API login endpoints must reject bad payloads
    When the user attempts API login with email "<email>" and password "<password>"
    Then the API login response status should be 401
    And the response should contain an error message

    Examples:
      | email                 | password       |
      | wrong@example.com     | WrongPass123   |
      | raunak@gmail.com      | BadPassword    |