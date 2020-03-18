Feature: Feature file examples

  The following scenarios display the different ways to use the modification features.
  - Required columns: ("source" & "s.Type") or ("source" & "s.match")
  - Column order is optional
  As long as the columns are included, they can be arranged in any order (spelling independent).

  Scenario: Replace output content with other content
    Given In the mock "Mock_Test", apply the following sequences:
      | Source   | S.Type | S.Match      | Action (match) |
      | response | body   | (test:)ready | @{1}done       |
    Then Response body contains the string "test:done"

  Scenario: Sequence testing; Different seq # equals different bounds
    Given In the mock "Mock_Test", apply the following sequences:
      | Seq | Condition | Source   | S.Type | S.Index | S.Match | Action (match) |
      | 0   |           | var      |        | grab    |         | blank          |
      | 0   | ?         | response | body   |         | test    |                |
      | 0   |           | var      |        | grab    |         | complete       |
      | 1   |           | var      |        | also    |         | something      |
    Then Assert the variable "grab" equals "blank"
    And Assert the variable "also" equals "something"

  Scenario: Replace output content with other content
    Given In the mock "Mock_Test", apply the following sequences:
      | Source   | S.Type | S.Index    | Action (var) | Action (match) |
      | request  | head   | item       | hold_1       |                |
      | response | body   | (test:)\d+ |              | @{1}@{hold_1}  |
    Then Assert the response body contains "test:something"

