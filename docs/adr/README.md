# Architecture Decision Records (ADRs)

This directory contains **Architecture Decision Records** for the Data Manager application.
Each ADR captures a significant architectural decision, the context in which it was made, and
the rationale behind the chosen approach.

## What is an ADR?

An Architecture Decision Record documents a meaningful architectural choice — including the
problem it solves, the alternatives considered, and the consequences of the decision. ADRs
provide a lightweight, append-only log of *why* the system is built the way it is.

## Template

This project uses the **Markdown Any Decision Records (MADR)** template, maintained by the
[MADR project](https://adr.github.io/madr/).

The blank template is available at:
[`docs/adr/templates/madr-template.md`](templates/madr-template.md)

To create a new ADR, copy the template into this directory using the naming convention below.

## Naming Convention

ADR files follow this pattern:

    NNNN-short-descriptive-name.md

Where `NNNN` is a zero-padded, four-digit sequential number assigned in creation order
(e.g., `0001`, `0002`).

**Example:**

    0001-use-spring-security-acl-for-resource-permissions.md
    0002-separate-data-management-and-finance-datasources.md

## Rules

- **One file per decision.** Each ADR is self-contained.
- **ADRs are append-only.** Never delete or edit a published ADR.
  - To reverse a decision, create a new ADR with status `superseded by [ADR-NNNN](NNNN-name.md)`.
  - To mark an ADR obsolete, create a new ADR that supersedes it, or update the status in
    the original ADR header only (e.g., `Status: deprecated`).
- **ADRs are created via Pull Request.** Review ensures alignment with the codebase and
  other decisions.
- **ADRs must be referenced from `AGENTS.md` and `requirements.md`** when they inform a
  requirement source or an architectural constraint (`C-<NN>` ID).
- If a decision affects database schema, security configuration, or external integrations,
  flag it in the PR description (see *Section 12 — What to Ask a Human* in `AGENTS.md`).

## Index of ADRs

| # | Title | Status | Date |
|---|---|---|---|
| [0001](0001-associated-datasets-domain-model.md) | Domain model and bounded context for associated datasets | approved | 2026-07-13 |
| [0002](0002-invenio-rdm-api-client-credentials.md) | InvenioRDM API client design and external credential security | approved | 2026-07-13 |
| [0003](0003-connection-lifecycle-stewardship.md) | Dataset connection lifecycle and data stewardship | approved | 2026-07-13 |
| [0004](0004-fair-signposting-deferred.md) | FAIR Signposting integration deferred | approved | 2026-07-13 |

*Update this table whenever a new ADR is added.*

## Attribution

The MADR template is used under the terms of the license included in
[`templates/LICENSE.md`](templates/LICENSE.md).

**Source:** https://github.com/adr/madr/  
**License:** MIT OR CC0-1.0
