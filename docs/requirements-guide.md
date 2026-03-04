# Requirements Documentation Guide

A reference for creating, maintaining, and tracing requirements in the Data Manager project.

---

## Quick Reference: ID Schema

All requirements follow: `<DOMAIN>-<TYPE>-<NN>`

**DOMAIN** (functional area):
- `AUTH`: Authentication & identity
- `PROJECT`: Project management & lifecycle
- `SAMPLE`: Sample registration
- `MEASUREMENT`: Measurement data capture
- `DATA`: Raw data handling
- `FAIR`: FAIR data & export
- `CARE`: CARE data principles
- `QUALITY`: Data quality & validation
- `LAB`: Laboratory operations
- `API`: API & integration
- `USER`: User management & authorization
- `COMM`: Notifications & communication

**TYPE** (requirement class):
- `R`: Functional requirement (system capability)
- `NFR`: Non-functional requirement (quality attribute)
- `C`: Constraint (boundary condition, policy)

**NN** (sequential):
- Two digits, per domain and type: `01`, `02`, `03`, etc.
- **Never renumber** — IDs are stable and permanent

### Examples
```
AUTH-R-01        Functional requirement, authentication domain
PROJECT-NFR-02   Non-functional requirement, project management domain
FAIR-C-01        Constraint, FAIR data domain
```

---

## Requirement Structure

Each requirement must include three sections (Statement, Rationale, Source). Optional sections for dependencies and relationships are described below.

### 1. **Statement** (required)
Clear, capability-level description of what the system shall do.

**Format**: Use "shall" language (requirement keywords)
- "The system shall support..."
- "The system shall enable..."
- "The system shall provide..."
- "The system shall prevent..."

**Bad**: "Users can log in"  
**Good**: "The system shall support user authentication via email and password"

**Guidelines**:
- One primary capability per requirement (avoid "and" when listing unrelated features)
- Concrete and testable (avoid "ensure quality", "improve performance")
- Focused on behavior, not implementation

### 2. **Rationale** (required)
Explain **why** this requirement exists. Include strategic, regulatory, or stakeholder context.

**Comprehensive rationale includes**:
- **Strategic**: How it supports platform vision, PRD goals
- **Regulatory**: Compliance obligations (GDPR, research data governance)
- **Stakeholder**: User needs, team requests, operational needs
- **Technical**: Architecture implications, integration dependencies
- **Risk mitigation**: What problems it solves

**Example**:
> Enables secure programmatic access for automated workflows and CI/CD pipelines. Reduces risk from credential exposure compared to storing passwords in configuration files. Encryption ensures tokens cannot be compromised via database breach alone. Required for API-first automation and partner integrations.

### 3. **Source** (required)
Document origin of requirement. Include **all applicable**:

- **PRD section**: Reference to Product Requirements Document (e.g., "PRD §2.3 Partner Integration")
- **ADR reference**: Link to Architecture Decision Record (e.g., "ADR-005-Spring-Security-Framework")
- **Regulatory**: Standard or regulation (e.g., "GDPR Art. 32 (Encryption requirements)")
- **Stakeholder**: Who requested it and when (e.g., "Stakeholder request (Security team, Jan 2025)")
- **Technical**: Codebase patterns or frameworks (e.g., "Existing vault pattern; Spring Security best practice")

### 4. **Dependencies** (optional but recommended)

Capture relationships between requirements to enable impact analysis and identify prerequisites.

**Types of relationships:**

- **Prerequisites**: Requirements that must be implemented before this one (e.g., "AUTH-R-01 must exist before AUTH-R-02")
- **Related**: Requirements that share context or should be reviewed together (e.g., "AUTH-NFR-02 audit logging relates to AUTH-R-02 MFA")
- **Supersedes**: If this is a newer version, which older requirement(s) does it replace?

**Format**:
```markdown
**Dependencies**:
- Prerequisite: AUTH-R-01 (local authentication must exist before MFA)
- Related: AUTH-NFR-02 (security audit logging), AUTH-NFR-03 (session security)
```

**When to include**: 
- If requirement has a hard prerequisite (cannot be implemented without another requirement)
- If requirement is part of a feature cluster (e.g., multiple MFA-related requirements)
- If requirement significantly impacts other requirements (security, architecture)

**Why it matters**:
- Helps teams sequence implementation work correctly
- Enables impact analysis: "If AUTH-R-01 changes, what else is affected?"
- Prevents building features in the wrong order
- Makes scope dependencies explicit during sprint planning

---

## Relationship to Stories & Tasks

Requirements are **one level above Stories**:

```
PRD (Vision)
  ↓
Requirements (What must the system do?)
  ↓
Stories (What value does the user get?) [GitHub Issues]
  ↓
Tasks (How do we implement it?) [GitHub Issues]
  ↓
Pull Requests (Code changes)
```

### Mapping

| Artifact | Links To | Contains |
|----------|----------|----------|
| Requirement (e.g., `AUTH-R-02`) | PRD section + source | Statement, Rationale, Source |
| Story (`.github/ISSUE_TEMPLATE/story.yml`) | ≥1 Requirement ID | User Story, Acceptance Criteria, Story Points |
| Task (`.github/ISSUE_TEMPLATE/task.yml`) | Parent Story | Technical description, implementation notes |
| PR | Issue(s) | Code changes, test coverage |

### Rules

- **Every Story** must reference ≥1 Requirement ID
- **Every Task** must reference parent Story + ≥1 Requirement ID
- **Every PR** must reference issue(s) + list Requirement IDs addressed
- **Changes to behavior** require corresponding requirement update (or explicit justification)

---

## Creating a New Requirement

### Step 1: Assign ID
1. Identify the **domain** (AUTH, PROJECT, MEASUREMENT, etc.)
2. Identify the **type** (R, NFR, or C)
3. Find the next available **NN** in that domain+type combo
4. Example: If `PROJECT-R-03` exists, next is `PROJECT-R-04`

### Step 2: Write Statement
- One clear, testable capability
- Use "shall" language
- Avoid implementation details
- Avoid ambiguous terms ("efficient", "user-friendly")

### Step 3: Write Rationale
- Answer: **Why does the system need this capability?**
- Include strategic, regulatory, stakeholder, and technical context
- Link to broader goals (FAIR principles, security posture, operational needs)

### Step 4: Identify Source(s)
- PRD section? ADR? Regulatory? Stakeholder request?
- **Capture all applicable sources**

### Step 5: Create Story (separate issue)
- Reference the Requirement ID(s)
- Add acceptance criteria (Given/When/Then)
- Link to any technical design docs

---

## Writing Acceptance Criteria for Stories

Acceptance criteria (AC) define the concrete, testable conditions a Story must satisfy. They translate requirements into verifiable behaviors.

### Principles

Acceptance criteria should be:

- **Testable**: Verifiable without ambiguity by QA, developers, or automated tests
- **Independent**: Each criterion can be validated separately (ideally)
- **Complete**: Together they cover the full scope of the requirement and user story
- **Focused on behavior**: Describe what the system does, not how it does it
- **From the user's perspective**: Use language that resonates with the user role

### Format: Given/When/Then

Use the **Given/When/Then** format (Gherkin-style) for clarity:

- **Given**: Initial context or precondition
- **When**: The action or trigger
- **Then**: The expected outcome or behavior

### Examples

#### Example 1: AUTH-R-04 (Personal Access Tokens)

**Story**: As an automation engineer, I want to generate a personal access token so that I can authenticate programmatic requests without storing my password.

**Acceptance Criteria**:

```
✅ GOOD: Testable, independent, behavior-focused

- Given a user is logged in and on the Account Settings page
  When they click "Create Personal Access Token"
  Then a dialog appears with fields for "Token Description" (optional) and "Expiration Date" (optional)

- Given a user completes the token creation form without setting expiration
  When they click "Create"
  Then the token is displayed once and marked as "valid for 30 days" (system default)

- Given a token has been created
  When the user reloads the page
  Then the token is NOT displayed again (shown only once at creation)

- Given a user has generated a token
  When they click "Revoke" on the token in Account Settings
  Then the token is immediately invalidated and cannot be used for future API requests

- Given a revoked token
  When a user attempts to use it in an API request
  Then the request returns 401 Unauthorized with message "Token is invalid or revoked"
```

#### Example 2: AUTH-R-02 (MFA Setup)

**Story**: As a security-conscious researcher, I want to enable two-factor authentication so that my account is protected against credential compromise.

**Acceptance Criteria**:

```
✅ GOOD: Each criterion is independent and testable

- Given a user is logged in and on Account Settings
  When they navigate to "Security" and click "Enable Two-Factor Authentication"
  Then they are shown a QR code and a text backup code

- Given a user scans the QR code into an authenticator app (Google Authenticator, Authy, etc.)
  When they enter the 6-digit code from their app
  Then the system verifies the code and confirms MFA is enabled

- Given MFA is enabled for a user
  When they log out and attempt to log in with correct password
  Then they are prompted to enter a 6-digit code from their authenticator app

- Given a user has 3 failed TOTP attempts in one login session
  When they attempt a 4th attempt
  Then the login is locked for 5 minutes with message "Too many failed attempts. Try again later."

- Given MFA is enabled
  When a user is on Account Settings
  Then they see a "Disable Two-Factor Authentication" button and a backup code option
```

#### Example 3: PROJECT-R-01 (Project Creation)

**Story**: As a project manager, I want to create a new research project so that I can organize my team and data.

**Acceptance Criteria**:

```
✅ GOOD: Covers the full workflow

- Given a user is logged in with Project Manager role
  When they navigate to Projects and click "Create New Project"
  Then a form appears with fields: Project Code, Project Title, Description, Principal Investigator

- Given the project creation form is open
  When they enter a unique project code and required fields
  And click "Create"
  Then the project is created and they are taken to the project overview page

- Given a project has been created
  When the creator views the project settings
  Then they are listed as an Administrator with full permissions

- Given a project exists
  When a user who is not assigned to the project attempts to access it
  Then they receive a "403 Forbidden" error

- Given a project exists
  When the project code conflicts with an existing project
  Then an error message appears: "Project code already exists. Please choose a different code."
```

### Common Mistakes to Avoid

| ❌ Anti-Pattern | ✅ Correct |
|---|---|
| "The system should handle tokens" | "Given a user creates a token without expiration, when they check settings, then it shows 30-day default" |
| "Users can download data" | "Given a user has read access to a project, when they click Download, then a ZIP file is generated and sent to their browser" |
| "MFA should be secure" | "Given 3 failed TOTP attempts, when a 4th is tried, then login is locked for 5 minutes" |
| "The system shall provide good UX" | "Given a user submits an invalid email, when they click Register, then an error message appears below the email field" |
| Acceptance criteria that are implementation details | Focus on behavior: "shall use Spring Security" → "when users log in, they receive a secure HTTP-only session cookie" |
| Criteria that can't be tested independently | Each Given/When/Then should be verifiable on its own |

### Tips for Writing Good AC

1. **Write from user perspective**: Use roles ("As a researcher", "As an admin") and user-facing language
2. **Be specific with values**: "30 days" not "a reasonable time period"
3. **Cover the happy path and key error cases**: Include what happens when things go wrong
4. **Test against the requirement**: Each AC should trace back to a specific part of the requirement statement
5. **Use concrete nouns**: "Display a dialog" not "Show something to the user"
6. **Avoid ambiguous words**: Instead of "reasonable", "quickly", "properly", use measurable terms

### Acceptance Criteria vs. Requirement Statement

| Aspect | Requirement | Acceptance Criteria (in Story) |
|---|---|---|
| **Level** | Capability level | Implementation/behavior level |
| **Audience** | Stakeholders, product, architecture | Developers, QA, testers |
| **Detail** | "The system shall support MFA" | "When user enters 3 wrong codes, login is locked 5 min" |
| **Location** | `docs/requirements.md` | GitHub Story issue |
| **Purpose** | Define WHAT must be built | Define HOW to verify it's correct |

---

## Editing Requirements

### When Requirements Change

**Scenario A: Clarification (no scope change)**
- Edit the requirement in-place
- No version bump needed
- Commit message: `docs: clarify requirement <ID>`

**Scenario B: Scope expansion (new capability)**
- Create a **new requirement** with next NN
- Don't modify the original
- Commit message: `docs: add requirement <DOMAIN>-<TYPE>-<NN>`

**Scenario C: Scope reduction or removal (rare)**
- Mark as superseded: `[SUPERSEDED by <DOMAIN>-<TYPE>-<NN>]`
- Keep original for traceability
- Commit message: `docs: supersede requirement <ID>`

### Pre-Submission Checklist for Requirement Authors

Before submitting a requirement change as a PR, verify:

- [ ] ID follows `<DOMAIN>-<TYPE>-<NN>` format and doesn't conflict with existing IDs
- [ ] Statement uses "shall" language and describes a testable capability
- [ ] Rationale explains **why** this requirement exists (strategic, regulatory, operational, technical, or risk mitigation)
- [ ] All sources are documented (PRD section, ADR, regulatory, stakeholder, or technical reference)
- [ ] No duplicate of an existing requirement — checked `docs/requirements.md` for similar capabilities
- [ ] If expanding scope of existing requirement, creating a NEW requirement (not modifying original)
- [ ] If clarifying or fixing typo, it's truly a clarification (no scope change)
- [ ] Related or dependent requirements are noted (if applicable)
- [ ] Acceptance criteria belong in linked Story, not in the requirement statement
- [ ] Examples or rationale patterns make the requirement clear

**Tip**: Use the Examples section (below) as a reference for your requirement's structure and tone.

### PR Review Checklist

When reviewing requirement changes:
- [ ] ID follows `<DOMAIN>-<TYPE>-<NN>` format
- [ ] Statement is clear and testable
- [ ] Rationale explains "why", not "what"
- [ ] All sources are documented
- [ ] No duplicate functionality with existing requirements
- [ ] Stories/tasks are updated if applicable
- [ ] Commit message explains reason for change
- [ ] Pre-submission checklist was used by author

---

## Common Mistakes to Avoid

| ❌ Mistake | ✅ Correct |
|-----------|-----------|
| ID reused or renumbered | Use next available NN, IDs are immutable |
| Acceptance criteria in requirement | Put AC in Story, not requirement |
| Statement uses "nice to have" language | Use "shall" (requirement) vs "may" (optional) |
| Vague rationale ("improves UX") | Concrete reason (reduces support tickets, enables workflow) |
| No source documented | Always include PRD/ADR/regulatory/stakeholder reference |
| Implementation details in statement | "System shall use Spring Security" → "System shall support OAuth2" |
| Too many features per requirement | Split into separate requirements |

---

## Examples

### Example 1: Functional Requirement (with Dependencies)

```
AUTH-R-04: Personal Access Tokens (PAT)

Statement:
The system shall support generation and management of encrypted personal 
access tokens that enable programmatic API access without exposing user 
passwords. Tokens shall be:
  - Generated by users in account settings with optional description and expiration
  - Encrypted at rest in the database
  - Revocable by users at any time
  - Valid for at least 30 days (configurable per deployment)
  - Displayed only once at creation time

Rationale:
Enables secure programmatic access for automated workflows, CI/CD pipelines, 
and third-party integrations. Reduces risk from credential exposure compared 
to storing passwords in configuration files. Encryption ensures tokens cannot 
be compromised via database breach alone (requires vault key). Revocation 
enables immediate access termination without password reset. Required for 
API-first automation and partner integrations.

Source:
- PRD §2.3 (Partner Integration)
- API security best practice
- Stakeholder request (automation team)

Dependencies:
- Prerequisite: AUTH-R-01 (user authentication must exist before tokens)
- Related: AUTH-NFR-02 (audit logging of token creation/usage), AUTH-C-03 (token encryption)
```

### Example 2: Non-Functional Requirement (with Dependencies)

```
AUTH-NFR-02: Security Audit Logging

Statement:
The system shall create audit log entries for all authentication-related events, 
including: user login attempts (success/failure), MFA setup/enable/disable, 
MFA challenge attempts, password reset requests, and personal access token 
creation/usage/revocation. Each entry shall capture: user ID, event type, 
timestamp, IP address, user agent, outcome, and context. Logs shall be 
retained for minimum 90 days.

Rationale:
Enables security investigation and incident response. Provides accountability 
trail for sensitive operations. Supports compliance audits for GDPR and research 
data governance. Helps detect brute-force attacks and unauthorized access 
attempts.

Source:
- GDPR Art. 32 (Security measures)
- Research data governance requirements
- Regulatory compliance requirement

Dependencies:
- Related: AUTH-R-02 (MFA), AUTH-R-04 (PAT), AUTH-R-05 (password reset)
  — all events from these requirements should be logged
```

### Example 3: Constraint

```
AUTH-C-01: MFA Voluntary Adoption (Phase 1)

Statement:
In Phase 1 (initial release), TOTP-based MFA shall be optional for all user 
roles. MFA shall not be mandatory for project access or operational use of 
the platform.

Rationale:
Reduces adoption friction and operational complexity for Phase 1 release. 
Allows users to adopt MFA incrementally. Mandatory enforcement can be phased 
in after successful optional rollout. Prevents lockout scenarios where users 
lose authenticator access before recovery procedures are established.

Source:
- Product decision (Jan 2025)
- Risk mitigation (Phase 1 simplification)
```

---

## Traceability Tips

### Finding all work for a requirement
```
# In GitHub, search for requirement ID
is:issue AUTH-R-02
is:pr AUTH-R-02

# In codebase, grep for ID in stories/tasks
grep -r "AUTH-R-02" .github/
grep -r "AUTH-R-02" AGENTS.md docs/
```

### Impact analysis
If `AUTH-R-02` changes, affected issues are those referencing it:
1. Find all Stories with `AUTH-R-02` in requirement_ids
2. Find all Tasks with parent Story
3. Find all PRs linking to those Tasks

---

## Tools & References

- **Requirement ID Scheme**: See AGENTS.md §9 (Requirement ID Scheme)
- **Story Template**: `.github/ISSUE_TEMPLATE/story.yml`
- **Task Template**: `.github/ISSUE_TEMPLATE/task.yml`
- **Current Requirements**: `docs/requirements.md`
- **PRD**: `docs/prd.md`

---

## Questions?

- How do I link a requirement to a story? → Add requirement ID to `requirement_ids` field in story
- Can I edit a requirement ID? → No, IDs are immutable for traceability
- Where do acceptance criteria go? → In Stories (GitHub issues), not in requirements
- What if a requirement doesn't fit the schema? → Discuss with team; schema is designed to cover all cases

