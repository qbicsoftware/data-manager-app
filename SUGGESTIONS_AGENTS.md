# Findings Report: "Requirements and Issue Governance" Section — AGENTS.md

**Reviewed by:** Claude Code (claude-sonnet-4-6)
**Date:** 2026-02-20
**Scope:** Lines 9–157 of AGENTS.md, cross-referenced against Sections 1, 7, 11, 12, 13

---

## SUMMARY BY CATEGORY

| Category | Finding Count | Severity Range |
|---|---|---|
| A. Internal Inconsistencies | 5 | Medium – High |
| B. Cross-Section Inconsistencies | 4 | Medium – High |
| C. Agent Readability Issues | 8 | Low – High |
| D. Structural / Formatting | 4 | Low – Medium |

**Total findings: 21**

---

## CATEGORY A: INTERNAL INCONSISTENCIES

---

### A-1 · HIGH · Story Required Fields vs. Traceability Rules — `NFR-xx` and `C-xx` Allowed?

**Location:** Story required fields (lines ~100–106); Traceability Rules (lines ~138–139)

**Description:**
The Story "Required fields" block states:

> At least one `R-xx` reference
> May include related `NFR-xx`

This wording restricts the *mandatory* reference to `R-xx` only, with `NFR-xx` being optional and additive. However, the Traceability Rules state:

> Every Story must reference at least one requirement ID.

The phrase "requirement ID" in the Traceability Rules is left undefined in that context — it could reasonably be read as any valid ID (R, NFR, or C), which is broader than what the Story Required Fields block allows (R-xx mandatory, NFR-xx optional, C-xx not mentioned at all for stories).

An agent following the Story Required Fields block would require exactly one R-xx. An agent following the Traceability Rules alone might accept a story with only an NFR-xx reference and consider it compliant.

**Concrete conflict:** A story referencing only `FAIR-NFR-01` and no R-type ID satisfies "at least one requirement ID" per Traceability Rules, but violates "at least one R-xx reference" per Story Required Fields.

**Suggested fix:** Explicitly reconcile both places. Either:
- Change the Traceability Rules to say "at least one `R-xx` functional requirement ID", or
- Change Story Required Fields to say "at least one requirement ID (`R-xx`, `NFR-xx`, or `C-xx`)" if that is truly the intended policy. Whichever choice is made, both locations must mirror it exactly.

---

### A-2 · HIGH · `C-xx` Constraints: Allowed in Stories? Rule Is Self-Contradictory

**Location:** Requirement ID Scheme Rules (lines ~43–46); Story Required Fields (lines ~100–106); Task Required Fields (lines ~120–130)

**Description:**
The Requirement ID Scheme Rules state:

> Constraints (`C`) influence architecture and must not be converted into user stories unless user value is directly involved.

This implies that, in some cases, a constraint *can* be referenced in a story (when user value is directly involved). However:

- The Story Required Fields block does not mention `C-xx` at all — not as required, not as permitted, not as optional.
- The Task Required Fields block mentions `C-xx` only under "Technical Notes (optional)" — correctly framed as optional architectural guidance.
- There is no guidance on what "user value is directly involved" means, leaving the exception clause entirely undefined.

An agent cannot determine:
1. Whether a `C-xx` reference is ever valid in a Story.
2. How to decide if "user value is directly involved" — no test or example is provided.
3. Whether a story that wraps a constraint should cite the `C-xx` ID in addition to or instead of an `R-xx` ID.

**Suggested fix:**
Either remove the exception clause ("unless user value is directly involved") if it is not meant to apply to story *creation*, or add a concrete example and definition of when a constraint becomes user-facing. If `C-xx` IDs are ever valid in stories, add a line to Story Required Fields stating this (e.g., "Constraints (`C-xx`) may be referenced under Requirement IDs when the constraint directly affects user workflow").

---

### A-3 · MEDIUM · `NN` vs. `xx` Notation Used Inconsistently

**Location:** Requirement ID Scheme definition (lines ~17–23) vs. Story Required Fields (lines ~101–105) and Task Required Fields (lines ~121–122)

**Description:**
The ID scheme definition uses `NN` as the placeholder for the sequential number component:

> `<DOMAIN>-<TYPE>-<NN>`

But all downstream references in the Story and Task required fields use `xx` instead:

> "At least one `R-xx` reference"
> "May include related `NFR-xx`"
> "At least one `R-xx` or `NFR-xx` reference"
> "Constraints (`C-xx`) may be referenced here"

While a human reader likely understands these are the same placeholder, an agent parsing the schema definition and then encountering `R-xx` in a required fields block may treat them as different things — especially if performing any automated validation or pattern matching against the schema.

The schema examples use two-digit padded numbers (`01`, `02`), while `xx` is an untyped wildcard that gives no hint about padding or format.

**Suggested fix:** Standardize on one placeholder throughout the document. Recommend `<NN>` for schema definitions (with the explicit note that it is zero-padded, e.g. `01`) and `R-<NN>` in referenced usages, or alternatively use a concrete example like `R-01` in the required fields lists rather than a placeholder shorthand.

---

### A-4 · MEDIUM · Constraint Rule Partially Duplicated With Slight Difference in Framing

**Location:** Requirement ID Scheme Rules (line ~45–46); Task Required Fields — Technical Notes (line ~127–130); Task Rules (line ~132–133)

**Description:**
Constraints are mentioned in three places within the section:

1. ID Scheme Rules: "Constraints influence architecture and must not be converted into user stories unless user value is directly involved."
2. Task Technical Notes: "Constraints (`C-xx`) — Related ADRs" listed as optional technical notes content.
3. Task Rules: "Constraints (`C-xx`) may be referenced here when relevant."

The third occurrence (Task Rules) appears to restate what is already established by the Technical Notes optional field. It adds no new information and is mildly redundant, but more importantly it frames the permission slightly differently — "when relevant" vs. "optional" — without clarifying whether these mean the same thing. An agent reading both might try to infer a distinction that does not exist.

**Suggested fix:** Remove the redundant Task Rule bullet "Constraints (`C-xx`) may be referenced here when relevant" and consolidate the constraint guidance into one place (the Technical Notes optional field description), since that is already the mechanism by which constraints are referenced in tasks.

---

### A-5 · MEDIUM · "One requirement may be referenced by multiple stories" Has No Reciprocal Rule

**Location:** Requirement ID Scheme Rules (line ~41–42)

**Description:**
The rule states that one requirement can be referenced by multiple stories. This is clear and useful. However, there is no rule about the inverse: whether one story may reference multiple requirements. Given that the Story Required Fields require *at least one* R-xx reference, the implication is that multiple references are permitted, but this is never stated. In the Traceability Rules, there is also no mention of a maximum or guidance on when a story is referencing too many requirements (suggesting scope creep).

This is a low-stakes omission for human contributors but becomes an agent-readability issue because an agent building a story for a cross-domain feature (e.g., one that involves both `PROJECT-R-02` and `AUTH-R-01`) has no guidance on whether to create one story or two.

**Suggested fix:** Add a brief rule: "A story may reference multiple requirement IDs when a single user workflow spans multiple requirements. If a story spans more than two domains, consider splitting it."

---

## CATEGORY B: CROSS-SECTION INCONSISTENCIES

---

### B-1 · HIGH · `docs/requirements.md` Missing From Key Files Quick Reference (Section 13)

**Location:** Requirement Structure section (line ~53) states requirements must be in `docs/requirements.md`; Section 13 Key Files Quick Reference does not list it.

**Description:**
Section 13 lists both `.github/ISSUE_TEMPLATE/story.yml` and `.github/ISSUE_TEMPLATE/task.yml` as key files, which are directly related to the governance workflow described in this section. Yet `docs/requirements.md` — the single authoritative source for all requirements, explicitly named as the required destination for requirement documentation — is absent from Section 13.

This is a meaningful omission. An agent following Section 13 as a quick-reference entry point would not be directed to `docs/requirements.md` when it needs to check existing requirements before making domain changes (which is a prerequisite in Section 11).

**Suggested fix:** Add `docs/requirements.md` to the Section 13 table with the description: "Authoritative requirement registry — all R/NFR/C requirements documented here; must be updated before new capabilities are implemented."

---

### B-2 · HIGH · Section 11 (Agent Delegation Patterns) Does Not Reference Requirement Governance

**Location:** Section 11 "Before making changes" (steps 1–4); Requirements and Issue Governance section (lines 9–157)

**Description:**
Section 11 defines a four-step pre-change checklist for agents:

1. Identify the bounded context
2. Identify the layer
3. Check ExceptionHandling.md and service_api.md
4. Read existing tests

There is no step instructing the agent to:
- Check `docs/requirements.md` to verify the change is covered by an existing requirement before proceeding.
- Verify that a Story and/or Task issue exists for the work.
- Check whether the change would introduce new system capability (which would require a requirement update first, per the governance section).

The governance section states "Stories must not introduce new system capabilities not covered by an existing requirement" and "If new capability is needed, update `docs/requirements.md` first." But this constraint is completely decoupled from the agent's pre-change workflow in Section 11, making it easy to miss.

**Suggested fix:** Add a step 0 or step 5 to Section 11's "Before making changes" checklist: "Check `docs/requirements.md` to confirm the change is covered by an existing requirement. If the change introduces new system capability, update requirements first (see 'Requirements and Issue Governance')."

---

### B-3 · HIGH · Section 12 (Human Escalation) Does Not Cover Requirement-Related Escalations

**Location:** Section 12 "What to Ask a Human Before Proceeding"; Requirement Edits sub-section (lines ~149–157)

**Description:**
Section 12 lists thirteen specific categories of changes that require human approval before an agent proceeds. None of them relate to:

- Adding new requirements to `docs/requirements.md`
- Modifying or removing existing requirements
- Creating new Stories that imply new capabilities
- Determining whether a change "introduces new system capability" (the judgment call that triggers a requirement update)

The governance section does state "Agents must never modify requirements silently" and mandates a PR with a changelog entry. But "never do X silently" is a weaker instruction than "pause and ask a human first." An agent might interpret "never silently" as meaning it should proceed with the change as long as it creates a PR — without first confirming the scope of the requirement change with a human.

**Suggested fix:** Add at least two entries to Section 12's escalation list:
- "Adding, modifying, or removing any entry in `docs/requirements.md`."
- "Implementing a change that introduces new system capability not covered by an existing requirement."

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

### C-1 · HIGH · No Decision Tree for Story vs. Task vs. Both — When Does Work Require One, the Other, or Both?

**Location:** Issues section — Story and Task sub-sections (lines ~68–135)

**Description:**
The section describes when to use a Story template and when to use a Task template, but it does not specify:

1. Whether every Task must have a pre-existing parent Story issue, or whether a Task can be created without a Story if the work is purely technical with no user-facing component.
2. Whether an agent should create a Story *and* a Task for a given unit of work, or whether only one is needed.
3. What the agent should do if it is performing implementation work and there is no existing Story issue to reference (e.g., when working from a direct code change request rather than from an existing issue).

The Task required fields list "Parent Story — Link to the parent story issue (e.g. #123)" as a required field. This implies every Task must have a parent Story. But there is no explicit statement of this rule, and no instruction for what an agent should do if no Story exists.

**Suggested fix:** Add an explicit ordering rule such as: "A Task must always be preceded by a Story. If no parent Story exists for a piece of work, create the Story first. Never create a Task without a parent Story." Also add guidance for agents acting on direct implementation requests without an existing issue.

---

### C-2 · HIGH · "Agents Must Never Modify Requirements Silently" — Not Operationally Actionable

**Location:** Requirement Edits sub-section, final line (line ~157)

**Description:**
The instruction "Agents must never modify requirements silently" is the only agent-specific directive in the entire section (the rest is phrased for human contributors). However, it does not define what "silently" means in agent context:

- Does creating a PR satisfy "not silently"?
- Does the agent need to explicitly flag the requirement change to a human reviewer before creating the PR?
- What if the agent is working in an automated pipeline where there is no human in the loop to notify?
- Does "modify" include *reading and deciding not to change* a requirement, or only actual edits?

The instruction also appears at the very end of the section, after all the structural rules, which reduces its visibility.

**Suggested fix:** Replace or augment the instruction with operational specifics: "Before modifying `docs/requirements.md`, an agent must (1) pause and confirm the change with a human reviewer, (2) create a dedicated PR for the requirement change alone (not bundled with implementation), and (3) include the mandatory changelog entry in the PR description per the Requirement Edits rules above." Move this guidance to a more prominent position — ideally immediately after the section introduction or at the top of the Requirement Edits sub-section.

---

### C-3 · MEDIUM · "R-xx" Shorthand Gives No Indication of Padding Format

**Location:** Story Required Fields (line ~101), Task Required Fields (line ~121)

**Description:**
Across the required fields for both Story and Task, requirement IDs are referenced using the shorthand `R-xx`, `NFR-xx`, and `C-xx`. The `xx` placeholder gives no indication of:

- Whether the number is zero-padded to two digits (e.g., `01` vs `1`)
- Whether padding is required or optional
- What the maximum length of `NN` is

The ID Scheme section does show zero-padded examples (`01`, `02`, `03`) but never explicitly states that zero-padding is mandatory. An agent creating or validating requirement references might accept `API-R-1` as valid when the intended canonical form is `API-R-01`.

**Suggested fix:** Add an explicit rule to the ID Scheme Rules: "The sequential number `NN` must be zero-padded to at least two digits (e.g., `01`, `02`, `10`). IDs without zero-padding are invalid." Also update the `R-xx` shorthand references to `R-<NN>` for consistency.

---

### C-4 · MEDIUM · No Instruction for What to Do If `docs/requirements.md` Does Not Exist

**Location:** Requirement Structure section (line ~53)

**Description:**
The section states "All requirements must be documented in `docs/requirements.md`" but gives no instruction for the case where this file does not yet exist (e.g., in a newly initialised clone, or if the file was accidentally deleted). An agent hitting this scenario has no guidance on whether to:

- Create the file and initialize it with a template/structure
- Halt and ask a human
- Proceed without requirements documentation

This is particularly relevant because Section 11 tells agents to read existing tests before writing code, but there is no analogous "check if requirements file exists before modifying requirements" instruction.

**Suggested fix:** Add a conditional instruction: "If `docs/requirements.md` does not exist, do not create it unilaterally — pause and notify a human reviewer, as this likely indicates a setup or migration issue."

---

### C-5 · MEDIUM · "If new capability is needed, update `docs/requirements.md` first" — No Definition of "New Capability"

**Location:** Story Rules (lines ~113–115)

**Description:**
The rule states:

> If new capability is needed, update `docs/requirements.md` first.

But "new capability" is not defined. An agent could reasonably interpret the following as either "new capability" or not:

- Adding a new API endpoint to an existing feature (new endpoint, same domain)
- Supporting a new file format for an existing upload workflow
- Adding a new measurement type to an existing data model
- Changing a validation rule that affects existing behavior
- Adding a UI field that exposes already-modeled data

The ambiguity means an agent cannot reliably determine whether a planned change requires a requirements update before proceeding.

**Suggested fix:** Add a concrete test for "new capability": "A change introduces new capability if it enables a user or system to perform an action that was not previously possible, or if it changes the externally observable behavior of an existing capability in a way not described by any existing requirement."

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

### D-1 · MEDIUM · Section Has No Number — All Other Sections Are Numbered

**Location:** Section heading "## Requirements and Issue Governance" (line 9)

**Description:**
Every other major section in AGENTS.md is numbered (1 through 13). This section has no number. The absence makes it harder to reference in other sections ("see Section X") and inconsistent with the document's own structure. It also means there is no cross-reference from Section 11 or Section 12 to this section by number.

**Suggested fix:** Renumber this section as Section 0 (since it is placed before Section 1 and covers governance meta-information), or insert it as a numbered section within the 1–13 sequence (e.g., renumber the existing sections and insert this as Section 1, making "Project Overview" Section 2). The most disruptive-free approach is to label it "Section 0" or add a heading note "See also from Section 11 and 12."

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

## PRIORITIZED TOP 5 MOST IMPACTFUL IMPROVEMENTS

The following five improvements, if made, would have the greatest positive effect on agent correctness, traceability, and governance compliance:

---

**Priority 1 — B-2: Add requirement governance check to Section 11's pre-change checklist (HIGH impact)**

*Why it is the top priority:* Section 11 is the primary entry point for agent behavior on this codebase. Without a step to check `docs/requirements.md` before making changes, the entire requirement governance framework is effectively invisible to an agent following the delegation workflow. This single omission means the governance rules in lines 9–157 may never be consulted in practice.

---

**Priority 2 — A-1: Reconcile Story Required Fields with Traceability Rules on what counts as a valid "requirement ID" reference (HIGH impact)**

*Why:* This ambiguity creates a direct compliance gap. An agent that follows the Traceability Rules literally may produce Stories that violate the Story Required Fields rule (or vice versa). Since both rules are about the same artifact (a Story issue), they must be unambiguous and identical. A story backed only by an NFR-xx being considered compliant or non-compliant has significant downstream consequences for traceability audits.

---

**Priority 3 — B-3: Add requirement-related escalations to Section 12's human-approval list (HIGH impact)**

*Why:* "Agents must never modify requirements silently" (C-2, line 157) is an important constraint but is buried and vague. Moving the substance of this rule into Section 12 as a formal human-escalation trigger makes it authoritative and enforceable in the same way the 13 other escalation categories are. Without this, an agent has no formal mechanism to pause before modifying the requirement registry.

---

**Priority 4 — C-1: Add an explicit ordering rule and decision tree for Story → Task sequencing (MEDIUM-HIGH impact)**

*Why:* The requirement that every Task have a parent Story is implied but never stated as a rule. An agent receiving a direct "implement this feature" instruction with no existing GitHub issue has no guidance on whether to create a Story, a Task, or both — or to halt. This is a common scenario in agent-driven development and the gap will be encountered frequently.

---

**Priority 5 — B-1: Add `docs/requirements.md` to Section 13 Key Files Quick Reference (MEDIUM impact)**

*Why:* Section 13 is explicitly designed as a quick-reference entry point. The presence of `story.yml` and `task.yml` in the table but the absence of `docs/requirements.md` creates a structural gap that will cause agents using Section 13 as a navigation guide to overlook the requirements registry. This is a low-effort fix with high discoverability payoff.

---

*End of Findings Report*
