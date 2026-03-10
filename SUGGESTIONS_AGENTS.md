# Findings Report: "Requirements and Issue Governance" Section — AGENTS.md

**Reviewed by:** Claude Code (claude-sonnet-4-6)
**Date:** 2026-02-20
**Scope:** Lines 9–157 of AGENTS.md, cross-referenced against Sections 1, 7, 11, 12, 13

---

## SUMMARY BY CATEGORY

| Category | Finding Count | Severity Range |
|---|---|---|
| B. Cross-Section Inconsistencies | 1 | Medium |
| C. Agent Readability Issues | 3 | Low – Medium |
| D. Structural / Formatting | 3 | Low |

**Total findings: 7**

---

## CATEGORY B: CROSS-SECTION INCONSISTENCIES

---

### B-4 · MEDIUM · PR Traceability Rule Is Duplicated Across Two Sections With Different Framing

**Location:** Traceability Rules (lines ~140–144); Section 7 Branching and Release Workflow

**Description:**
The Traceability Rules state:

> Every PR must:
> - Reference the issue it implements
> - List the requirement IDs addressed

Section 7 (Branching and Release Workflow) independently states:

> Every PR must reference the issue it implements and list requirement IDs addressed.

These are semantically identical, which is good for consistency. However, they are framed independently in two different sections without cross-referencing each other. An agent asked to update one may not know to update the other, leading to the two falling out of sync in future edits.

Also, Section 7 does not state what happens when a PR covers multiple issues, while the Traceability Rules are silent on the same edge case.

**Suggested fix:** In Section 7, replace the duplicated PR traceability sentence with a cross-reference: "PRs must also satisfy the traceability rules defined in the 'Requirements and Issue Governance' section." This makes Section 7 a pointer to the authoritative rule rather than an independent restatement.

---

## CATEGORY C: AGENT READABILITY ISSUES

---

### C-6 · MEDIUM · Acceptance Criteria Format Constraint May Be Too Rigid for All Story Types

**Location:** Story Required Fields (lines ~106–108)

**Description:**
The Story Required Fields state that Acceptance Criteria must be "One or more testable conditions in Given / When / Then format." Given/When/Then is a BDD format most natural for user interaction stories. For stories that are more data-model-oriented, infrastructure-oriented, or governance-oriented (e.g., a story about FAIR compliance or data export), Given/When/Then may be awkward or inapplicable.

More critically for agent readability: there is no example of a well-formed Acceptance Criterion in GWT format, and no guidance on what constitutes "testable" in this context (automated test? manual validation? acceptance test?). An agent generating a Story issue has no template to follow.

**Suggested fix:** Add at least one concrete example of an Acceptance Criterion in GWT format, adjacent to the User Story example. Also add a note: "If Given/When/Then is not applicable (e.g., for FAIR or compliance-oriented stories), use a declarative condition format: 'The system must [observable outcome] when [precondition].'"

---

### C-7 · LOW · Story vs. Task Distinction Relies on "Derived From" Relationship That Is Not Precisely Defined

**Location:** Task definition (lines ~116–118)

**Description:**
The Task section states it is for "concrete technical implementation work derived from, and linked to, a story." The phrase "derived from" is interpreted differently by different agents:

- Does "derived from" mean the Task must be explicitly authorized by a Story's acceptance criteria?
- Does it mean the Task can be any implementation step that supports a Story's goal, even if not explicitly in the acceptance criteria?
- Can a single Task cover work that serves multiple Stories?

The "linked to" part is operationalized by the Parent Story required field, but "derived from" is not operationalized at all.

**Suggested fix:** Replace "derived from, and linked to, a story" with the more precise: "A Task must implement part or all of the acceptance criteria of its parent Story. A Task must not implement work outside the scope of its parent Story's acceptance criteria."

---

### C-8 · LOW · Section Has No Explicit Scope Statement for Agents (vs. Human Contributors)

**Location:** Section introduction (lines ~9–10)

**Description:**
The AGENTS.md file header states it guides "AI agents (and human contributors)." However, the Requirements and Issue Governance section is written entirely in human-contributor voice (imperative statements about "use the correct template," "include a changelog entry," etc.) with only one agent-specific line at the very end ("Agents must never modify requirements silently"). There is no upfront statement of which parts of this section apply to agents vs. humans, or what an agent's role in this governance process actually is.

For example: is an agent expected to *create* GitHub issues, or only to ensure its code changes are traceable to existing issues? Is an agent expected to *edit* `docs/requirements.md`, or only read it?

**Suggested fix:** Add a brief "Agent scope" paragraph at the top of the section, e.g.: "Agents are expected to: (1) read `docs/requirements.md` before beginning any change; (2) verify that a Story and Task issue exist for the work being done; (3) never modify `docs/requirements.md` without human approval; (4) include requirement IDs in all PRs they create. Agents are not expected to create GitHub issues autonomously unless explicitly instructed."

---

## CATEGORY D: STRUCTURAL AND FORMATTING OBSERVATIONS

---

### D-2 · LOW · Mixed Horizontal Rule Styles

**Location:** Throughout the Requirements and Issue Governance section (lines ~47, ~65, ~66, ~111, ~112, ~134, ~135, ~147)

**Description:**
The section uses two distinct horizontal rule styles inconsistently:

- `------------------------------------------------------------------------` (long dashes, used as sub-section dividers within the section)
- `---` (short form, used in the cross-reference context block and elsewhere in AGENTS.md)

Both render as `<hr>` in standard Markdown, so there is no functional difference. However, the inconsistency is visually jarring in raw form and suggests the section was written separately and merged, rather than following the document's style.

**Suggested fix:** Standardize all horizontal rules to `---` (three dashes) throughout the document for consistency with the rest of AGENTS.md.

---

### D-3 · LOW · Mixed List Marker Styles

**Location:** Requirement ID Scheme (lines ~14–23), Requirement Structure (lines ~55–62), Traceability Rules (lines ~136–145)

**Description:**
The section uses `-` list markers in some places (Traceability Rules) and `*` or no marker in others, but most noticeably there is a mix of:

- Indented `-` bullet lists with two-space indent
- Indented `-` bullet lists with four-space indent
- Block-indented code/verbatim formatting for the ID schema (four-space indent, renders as code block in Markdown)

The Traceability Rules use `- ` (hyphen-space) at the top level and `    -   ` (four-space, hyphen, three-space) at the second level, which is inconsistent with the Story/Task sections that use `-   ` (hyphen, three-space) at all levels.

**Suggested fix:** Adopt a single list style throughout: `-` markers with consistent 4-space indentation for nested items. Run the document through Prettier or a Markdown linter to enforce consistency.

---

### D-4 · LOW · No Examples Provided for Task Issues (Only Story Has an Example)

**Location:** Story section has no inline example of a completed Story; Task section has no inline example either; but the Requirement Structure sub-section provides a complete example

**Description:**
The Requirement Structure sub-section provides a complete worked example of what a properly documented requirement looks like. Neither the Story nor the Task sub-section provides a comparable example of a correctly filled-out issue. Given that the Story template has three required fields (Requirement IDs, User Story, Acceptance Criteria) and the Task has four (Parent Story, Requirement IDs, Description, Technical Notes), examples would significantly improve an agent's ability to produce well-formed issues.

The absence is particularly notable for the Acceptance Criteria field, where the GWT format is specified but no example is given — leaving an agent to infer the correct format.

**Suggested fix:** Add a minimal example block to both the Story and Task sub-sections, similar to the Requirement Structure example. These do not need to be complete or reflect real project data — a schematic example is sufficient. For instance, under Story: show a User Story sentence and one GWT Acceptance Criterion. Under Task: show the parent story reference, one requirement ID, and a one-line description.

---

## REMAINING OPEN FINDINGS

All findings listed here remain unresolved and have not been applied to AGENTS.md:

| ID | Severity | Description |
|---|---|---|
| B-4 | MEDIUM | PR traceability rule is duplicated across two sections without cross-referencing; risk of future drift |
| C-6 | MEDIUM | Acceptance Criteria GWT format may be too rigid for non-interaction stories; no example provided |
| C-7 | LOW | "Derived from" relationship between Task and Story is not operationally defined |
| C-8 | LOW | Section has no explicit scope statement distinguishing agent vs. human contributor responsibilities |
| D-2 | LOW | Mixed horizontal rule styles (`---` vs. long-dash) across the section |
| D-3 | LOW | Mixed list marker styles and inconsistent indentation levels |
| D-4 | LOW | No worked example provided for Story or Task issue format |

---

*End of Findings Report*
