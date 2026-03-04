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

Each requirement must include three sections:

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

### Example 1: Functional Requirement

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
```

### Example 2: Non-Functional Requirement

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

