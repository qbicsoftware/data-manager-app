# Incremental migration scripts (Tier 1)

This directory holds runnable, idempotent DDL scripts for schema changes to
the Data Manager application. Each script is the atomic unit of a migration —
it does one thing, does it safely, and references the story/feature/ADRs that
drive it.

For the three-tier migration documentation structure, see
[`docs/migrations/README.md`](../../docs/migrations/README.md).

---

## Current scripts

| # | Script | Description | Story | Target datasource |
|---|---|---|---|---|
| 1 | [`create-associated-dataset.sql`](create-associated-dataset.sql) | Create `associated_dataset` table for connecting InvenioRDM datasets to projects | [#1467](https://github.com/qbicsoftware/data-manager-app/issues/1467) | `data_management` |
| 2 | [`extend-dataset-visibility-in-project-overview.sql`](extend-dataset-visibility-in-project-overview.sql) | Add connected-dataset aggregates (count, open/restricted breakdown, last-connected) to `project_overview` view | [#1475](https://github.com/qbicsoftware/data-manager-app/issues/1475) | `data_management` |

*Add a row here and drop in the script when a new migration lands.*

---

## Naming convention

Scripts are named by the schema change, **not** by date or by version. Dates
and version associations live in the operator-facing migration guides
(`docs/migrations/released/v*.md` or `docs/migrations/NEXT.md`), not here.

Format: `<kebab-case-name>.sql`

Examples:
- `create-associated-dataset.sql` ← adds a new table
- `add-user-preferences-column.sql` ← adds a column to an existing table
- `replace-ip-summary-view.sql` ← replaces a view definition
- `drop-unused-indexes-on-sample.sql` ← removes indexes

---

## Required header format

Every script must open with a comment header containing these fields:

```sql
-- =============================================================================
-- Migration: <short human-readable name>
-- Story:    <GitHub issue link>
-- Feature:  <GitHub issue link or feature ID>
-- ADRs:     <comma-separated list, e.g. 0001, 0002, 0003>
-- Datasource: <data_management | finance | both>
--
-- <Free-form notes: design decisions, scale assumptions, rollback caveats,
--  anything an operator would want to know before running this script.>
-- =============================================================================
```

Example: see [`create-associated-dataset.sql`](create-associated-dataset.sql).

---

## Idempotency requirement

All scripts **must** be idempotent — safe to run multiple times against the
same target datasource without changing the outcome or producing errors:

| Statement | Idempotent form |
|---|---|
| `CREATE TABLE` | `CREATE TABLE IF NOT EXISTS ...` |
| `ADD COLUMN`   | Use MariaDB `ALTER TABLE ... ADD COLUMN IF NOT EXISTS ...` |
| `DROP TABLE`   | `DROP TABLE IF EXISTS ...` |
| `CREATE INDEX` | Wrap in a conditional `IF NOT EXISTS` check, or a stored-procedure guard |
| `CREATE VIEW`  | `CREATE OR REPLACE VIEW ...` |

If a change cannot be made idempotent (rare), document that explicitly in the
header and in the matching operator-facing guide.

---

## Where scripts are referenced

Each script is referenced by an entry in the corresponding
operator-facing migration guide:

- **Next release (unreleased):** [`docs/migrations/NEXT.md`](../../docs/migrations/NEXT.md)
- **Released versions:** [`docs/migrations/released/v*.md`](../../docs/migrations/released/)

The operator guide duplicates the pre-flight / verify / rollback SQL so
operators don't have to read this directory directly. This directory stores the
canonical DDL; the guides explain how to run it in context.
