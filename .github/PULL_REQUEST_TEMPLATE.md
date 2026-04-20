## Description

<!-- Briefly describe what this PR implements and why. Keep it to 2-3 sentences. -->

## Issue and Traceability

### Linked Task Issue
<!-- Every PR must implement a Task issue. Reference it here using the format: #123 -->
- **Closes:** #<!-- Task issue number -->

### Requirement IDs Addressed
<!-- List all functional (R) and non-functional (NFR) requirement IDs this PR addresses.
     Use the format: DOMAIN-TYPE-NN (e.g., SAMPLE-R-01, AUTH-NFR-02, API-R-03)
     Only R and NFR types are valid here. Constraint IDs (C) must NOT be listed.
     If no requirements are addressed, that may indicate missing requirement documentation.
-->
- [ ] Requirement IDs: <!-- e.g., PROJECT-R-01, PROJECT-R-02, SAMPLE-NFR-01 -->

## Requirement Update Status

<!-- Check one of the boxes below: either requirements were updated, or justify why not. -->

- [ ] **Requirements Updated:** The corresponding functional/non-functional requirements in `docs/requirements.md` have been updated to reflect this implementation.
- [ ] **Justification Provided:** This PR does not change externally observable behavior, so no requirement update is needed. (Provide brief justification below)

<!-- If you selected "Justification Provided", explain why no requirement update is necessary:
     Examples: "This is a refactoring with no behavioral change", "This is an internal optimization",
     "This fixes a bug but does not change the specification of the feature" -->
### Justification (if applicable)
<!-- Provide justification here if no requirement update is needed. -->

## Changes Summary

### Modified Areas
<!-- Indicate which parts of the codebase this PR touches. Check all that apply. -->
- [ ] Domain layer (`domain/model/`, domain events, aggregates)
- [ ] Application layer (`application/` services, use cases)
- [ ] Infrastructure layer (JPA repositories, external integrations, config)
- [ ] UI / Views (`views/` components, Vaadin routes)
- [ ] Database schema (`sql/`, DDL changes)
- [ ] Tests (unit, integration, or Spock specs)
- [ ] Documentation, CI/CD, or build configuration
- [ ] Other (describe): <!-- Provide details here -->

### Behavioral Changes
<!-- Does this PR change externally observable behavior? If yes, describe what changed and confirm it's covered by a requirement update above. -->
- [ ] No behavioral changes (internal refactoring, optimization, test addition, etc.)
- [ ] Yes, behavior changes: <!-- Describe the changes and confirm requirement update or justification above -->

## Sensitive Changes Requiring Review
<!-- If your PR touches any of these areas, flag it explicitly below so reviewers pay attention. -->
- [ ] **Database schema changes** (`sql/complete-schema.sql`, table/column DDL)
- [ ] **Spring Security configuration** (`security/`, ACL setup, authentication/authorization)
- [ ] **FAIR / RO-Crate export format** (`docs/fair/`, RO-Crate builder logic)
- [ ] **Artemis messaging topics** (JMS topic names, consumer configuration)
- [ ] **Requirement file edits** (`docs/requirements.md`)
- [ ] None of the above

## Pre-Submission Checklist

<!-- Complete all items before submitting. -->

- [ ] **Issue Linked:** PR references a Task issue (see "Linked Task Issue" section above)
- [ ] **Requirements Listed:** All functional (R) and non-functional (NFR) requirement IDs are listed above
- [ ] **No Constraint IDs:** No `C` (constraint) IDs are listed in the Requirement IDs section
- [ ] **Behavior vs. Requirements:** Requirement update confirmed OR explicit justification provided
- [ ] **Code Style:** Code follows Google Java Style Guide (run formatter before commit)
- [ ] **Tests:** New tests added or existing tests updated to cover changes
- [ ] **No Secrets:** No hardcoded credentials, API keys, passwords, or secrets committed
- [ ] **Documentation:** Updated relevant docs if the change affects user-facing behavior or public APIs

---

### Notes for Reviewers
<!-- Highlight any non-obvious decisions, trade-offs, or areas needing careful review. -->
