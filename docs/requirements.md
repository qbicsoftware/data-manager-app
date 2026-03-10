# Requirements Registry

## Intent and Scope

This file is the **authoritative requirements registry** for the Data Manager Application. It contains all functional requirements (R), non-functional requirements (NFR), and constraints (C) that govern the system's capabilities and quality attributes.

### Relationship to Other Documents

- **PRD (`docs/prd.md`):** The Product Requirements Document contains the product vision, user personas, and business objectives. It is the upstream input to this registry.
- **This Registry:** Formalises PRD objectives into traceable, ID-tagged requirements. Each requirement is derived from and traces back to the PRD, stakeholder requests, regulatory drivers, or architectural decisions.
- **GitHub Stories and Tasks:** Requirements are one level above Stories. A Story references one or more requirement IDs and describes a user-facing workflow. A Task references a Story and implements concrete work towards that Story's acceptance criteria.

### Workflow

1. **PRD → Requirements → Stories → Tasks → Implementation**
2. Before making any change to the system, check this file to confirm the change is covered by an existing requirement. If the change introduces new system capability not covered by any existing requirement, update this file first (see `docs/requirements-guide.md` for full authoring conventions).
3. **Agents:** You must read this file and `AGENTS.md` Section 11 step 0 before making any code change.

---

## Requirement ID Schema

All requirements follow a domain-based ID structure:

```
<DOMAIN>-<TYPE>-<NN>
```

- **DOMAIN** — Functional area. Allowed values:
  - `AUTH` — Authentication, user identity, login, ORCID
  - `PROJECT` — Project management, experiments, experimental design
  - `SAMPLE` — Sample registration, batch management
  - `MEASUREMENT` — Measurement metadata, data tracking
  - `DATA` — Raw data handling, file management, downloads
  - `FAIR` — FAIR principles, RO-Crate export, data discoverability
  - `CARE` — CARE principles, governance, indigenous data rights
  - `QUALITY` — Data quality, validation, integrity
  - `LAB` — Laboratory operations, OpenBIS integration
  - `API` — Programmatic access, API design, tokens
  - `USER` — User management, roles, permissions, UI/UX
  - `COMM` — Communication, notifications, announcements, email

- **TYPE** — Requirement classification:
  - `R` — Functional requirement (system capability: "the system shall…")
  - `NFR` — Non-functional requirement (quality attribute: performance, scalability, security, usability, etc.)
  - `C` — Constraint (solution boundary: "must use…", "must not…", architectural decision)

- **NN** — Sequential number per (DOMAIN, TYPE) pair, zero-padded to two digits:
  - Valid: `01`, `02`, `10`, `99`
  - Invalid: `1`, `2`, `010` (no leading zeros beyond two digits)

### Examples

- `AUTH-R-01` — Functional requirement in the Authentication domain
- `PROJECT-NFR-02` — Non-functional requirement in the Project Management domain
- `SAMPLE-C-01` — Constraint in the Sample Management domain
- `FAIR-R-03` — Functional requirement in the FAIR domain

### Rules

- IDs are **stable and must never be renumbered or reused.**
- Constraints (`*-C-*`) influence architecture and must not be referenced in Stories. Reference them only in Task Technical Notes and ADRs.
- One requirement may be referenced by multiple Stories.
- A Story may reference multiple requirement IDs when a single user workflow spans multiple domains. If a Story spans more than two domains, consider splitting it.

---

## Requirement Format

Each requirement must contain:

```
### <ID>: <Short Title>

<Statement>

**Rationale:**
<Why this requirement exists — strategic, regulatory, stakeholder-driven, or architectural>

**Source (optional but recommended):**
<Link to PRD section, FAIR/CARE principle, regulatory document, stakeholder request, or ADR>
```

---

## Worked Example

### PROJECT-R-01: Project Creation by Authorised Users

The system shall allow authorised users to create new research projects with metadata including title, description, and principal investigator.

**Rationale:**
Projects are the primary organisational unit in the Data Manager. Users need the ability to establish new research initiatives and provide basic context. This is a foundational capability enabling all downstream sample and measurement registration workflows.

**Source:**
PRD §3.1 Core Data Management; User story: "As a researcher, I want to create a project so that I can organise my research data."

---

## AUTH — Authentication and User Identity

### Functional Requirements

_No requirements defined yet._

### Non-Functional Requirements

_No requirements defined yet._

### Constraints

_No requirements defined yet._

---

## PROJECT — Project Management and Experimental Design

### Functional Requirements

_No requirements defined yet._

### Non-Functional Requirements

_No requirements defined yet._

### Constraints

_No requirements defined yet._

---

## SAMPLE — Sample Registration and Batch Management

### Functional Requirements

_No requirements defined yet._

### Non-Functional Requirements

_No requirements defined yet._

### Constraints

_No requirements defined yet._

---

## MEASUREMENT — Measurement Metadata and Data Tracking

### Functional Requirements

_No requirements defined yet._

### Non-Functional Requirements

_No requirements defined yet._

### Constraints

_No requirements defined yet._

---

## DATA — Raw Data File Handling

### Functional Requirements

_No requirements defined yet._

### Non-Functional Requirements

_No requirements defined yet._

### Constraints

_No requirements defined yet._

---

## FAIR — FAIR Data Principles and Export

### Functional Requirements

_No requirements defined yet._

### Non-Functional Requirements

_No requirements defined yet._

### Constraints

_No requirements defined yet._

---

## CARE — CARE Principles and Data Governance

### Functional Requirements

_No requirements defined yet._

### Non-Functional Requirements

_No requirements defined yet._

### Constraints

_No requirements defined yet._

---

## QUALITY — Data Quality and Validation

### Functional Requirements

_No requirements defined yet._

### Non-Functional Requirements

_No requirements defined yet._

### Constraints

_No requirements defined yet._

---

## LAB — Laboratory Operations and External Integrations

### Functional Requirements

_No requirements defined yet._

### Non-Functional Requirements

_No requirements defined yet._

### Constraints

_No requirements defined yet._

---

## API — Programmatic Access and API Design

### Functional Requirements

_No requirements defined yet._

### Non-Functional Requirements

_No requirements defined yet._

### Constraints

_No requirements defined yet._

---

## USER — User Management, Roles, and Permissions

### Functional Requirements

_No requirements defined yet._

### Non-Functional Requirements

_No requirements defined yet._

### Constraints

_No requirements defined yet._

---

## COMM — Communication, Notifications, and Announcements

### Functional Requirements

_No requirements defined yet._

### Non-Functional Requirements

_No requirements defined yet._

### Constraints

_No requirements defined yet._