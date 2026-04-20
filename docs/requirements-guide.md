# Requirements Authoring Guide

This guide provides conventions for creating, documenting, and maintaining requirements in `docs/requirements.md`.

## Quick Start

1. **Check the current requirements** in `docs/requirements.md` to confirm you're not duplicating an existing requirement.
2. **Choose a domain** from the allowed list: `AUTH`, `PROJECT`, `SAMPLE`, `MEASUREMENT`, `DATA`, `FAIR`, `CARE`, `QUALITY`, `LAB`, `API`, `USER`, `COMM`.
3. **Choose a type**: `R` (functional requirement), `NFR` (non-functional requirement), or `C` (constraint).
4. **Assign a number**: Sequential, zero-padded to exactly two digits within your domain+type pair (e.g., `01`, `02`, `03`).
5. **Write the requirement** using the format specified below.

---

## Requirement ID Schema

All requirement IDs follow this pattern:

```
<DOMAIN>-<TYPE>-<NN>
```

- **DOMAIN** — Functional area (AUTH, PROJECT, SAMPLE, MEASUREMENT, DATA, FAIR, CARE, QUALITY, LAB, API, USER, COMM)
- **TYPE** — Requirement classification:
  - `R` = Functional requirement ("the system shall…")
  - `NFR` = Non-functional requirement (performance, security, usability, scalability, etc.)
  - `C` = Constraint (solution boundary, architectural decision, "must use…", "must not…")
- **NN** — Sequential number per (DOMAIN, TYPE) pair, zero-padded to exactly two digits: `01`, `02`, `10`, `99`

### Examples

- `AUTH-R-01` — Functional requirement (auth domain)
- `PROJECT-NFR-02` — Non-functional requirement (project domain)
- `SAMPLE-C-01` — Constraint (sample domain)
- `FAIR-R-03` — Functional requirement (FAIR domain)

### Rules

- IDs are **stable** and must never be renumbered or reused, even if a requirement is deprecated.
- IDs must not encode sprint numbers, versions, or document order.
- One requirement may be referenced by multiple Stories.
- Constraints (`*-C-*`) influence architecture and must not be referenced in Stories — reference them only in Task Technical Notes and ADRs.

---

## Requirement Format

Each requirement entry in `docs/requirements.md` must contain:

```markdown
### <ID>: <Short Title>

<Statement — clear, capability-level description>

**Rationale:**
<Why this requirement exists — strategic, regulatory, stakeholder-driven, or architectural>

**Source (optional but recommended):**
<Link to PRD section, FAIR/CARE principle, regulatory document, stakeholder request, or ADR>
```

### Field Definitions

- **ID** — Use the scheme above (e.g., `AUTH-R-01`)
- **Short Title** — One line, noun-focused, describing the capability or quality attribute
- **Statement** — 1–3 sentences describing what the system shall do (for R/NFR) or the solution boundary (for C)
- **Rationale** — Why this requirement exists: strategic importance, regulatory drivers, stakeholder requests, technical necessity
- **Source** — Link(s) to the upstream authority: PRD section, FAIR/CARE principle reference, standards document, ADR, or stakeholder issue

### Example

```markdown
### AUTH-R-01: User Authentication via Local Credentials

The system shall allow users to create a local account using an email address and password, 
and shall authenticate subsequent login attempts against the stored, hashed password.

**Rationale:**
Users require a low-friction on-ramp to the system without requiring external identity providers. 
Local credentials are a foundational capability enabling all downstream workflows. ORCID is an optional, 
secondary authentication pathway.

**Source:**
PRD §2.1 User Onboarding; User story: "As a researcher, I want to log in with my email."
```

---

## When to Create a New Requirement

Create a new requirement when:

1. **A user-facing capability is missing** from the system (a Story or Feature references it but no R/NFR exists)
2. **A quality attribute is not yet documented** (e.g., performance target, accessibility standard)
3. **An architectural constraint is not yet recorded** (e.g., "must use Java 21", "OpenBIS integration required")

**Do NOT create a requirement for:**

- A specific implementation detail (use Task Technical Notes instead)
- A feature that will be deprecated soon (mark existing requirement as superseded first)
- A one-off bug fix (use a bug report, not a requirement)

---

## Editing Requirements

### Adding a new requirement

1. Add the requirement entry to the appropriate domain section in `docs/requirements.md`
2. Assign a new sequential number (do not reuse retired IDs)
3. Create a PR with the requirement change
4. Include a changelog entry in the PR description: "Added [ID]: [Short Title]"

### Modifying an existing requirement

1. Update the statement, rationale, or source as needed
2. **Do not change the ID** — IDs are stable
3. Create a PR with the change
4. Include a changelog entry: "Modified [ID]: [reason for change]"

### Retiring a requirement

1. Mark it as **[RETIRED]** in the ID line
2. Add a note explaining why and when it was retired
3. Create a PR
4. Include a changelog entry: "Retired [ID]: [reason]"

---

## Common Pitfalls

❌ **Avoid:** Creating a requirement for a specific story (too granular)  
✅ **Do:** Create a requirement for a capability that multiple stories might address

❌ **Avoid:** Renumbering or reusing IDs (breaks traceability)  
✅ **Do:** Retire the old ID and assign a new number to a new requirement

❌ **Avoid:** Referencing Constraints in Stories  
✅ **Do:** Reference Constraints only in Task Technical Notes

❌ **Avoid:** Vague rationales ("because we need it")  
✅ **Do:** Explain the strategic, regulatory, or architectural driver

❌ **Avoid:** Mixing multiple capabilities in one requirement  
✅ **Do:** Split into separate requirements per capability/quality attribute

---

## Related Documents

- **AGENTS.md § Section 0** — Full requirement governance rules and issue templates
- **docs/requirements.md** — Authoritative requirements registry
- **AGENTS.md § Section 11** — Agent delegation patterns for requirement changes

---

## Questions?

Consult AGENTS.md Section 12 for what to ask a human reviewer before making requirement changes.
