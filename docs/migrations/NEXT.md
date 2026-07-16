# Migration: Next release

**Current pom version:** 1.12.10  
**Target release version:** TBD *(renamed to `released/v<version>.md` at release cut)*  
**Status:** under development

> **To operators:** This document describes schema changes for the **upcoming,
> unreleased** Data Manager version. It is actively being edited as migrations
> land in the codebase. Do **not** apply any migrations listed here to
> production until the release is published and this file moves to
> `released/v<version>.md`.

For the migration documentation structure, see [`README.md`](README.md).

---

## Summary of schema changes in this release

> *Append a row here whenever a new incremental script is added. Numbering is
> sequential within the release and determines apply order.*

| # | Script | Description | Risk |
|---|---|---|---|
| 1 | [`sql/migrations/create-associated-dataset.sql`](../sql/migrations/create-associated-dataset.sql) | Create `associated_dataset` table for connecting InvenioRDM datasets to projects (FEAT-DATSET-01) | low — new empty table, no data loss on rollback |

Each row links to its incremental script. The sections below expand each entry
with apply / verify / rollback detail.

---

## Migration #1: `associated_dataset` table

| Field | Value |
|---|---|
| **Story** | [#1467 — FEAT-DATSET-01: Connecting open, published datasets](https://github.com/qbicsoftware/data-manager-app/issues/1467) |
| **Feature** | [#1466 — FEAT-DATASET-CONNECTION](https://github.com/qbicsoftware/data-manager-app/issues/1466) |
| **ADRs** | [0001](../adr/0001-associated-datasets-domain-model.md), [0002](../adr/0002-invenio-rdm-api-client-credentials.md), [0003](../adr/0003-connection-lifecycle-stewardship.md) |
| **Scope** | Database schema only (new table) |
| **Script** | [`sql/migrations/create-associated-dataset.sql`](../sql/migrations/create-associated-dataset.sql) |
| **Target datasource** | `data_management` |

### What it does

Creates an empty `associated_dataset` table. Per [ADR-0001](../adr/0001-associated-datasets-domain-model.md):

- Lives in the `project-management` bounded context (table name starts with `associated_dataset`, not under the existing `dataset_*` namespace, to keep the InvenioRDM connection concept distinct from the legacy OpenBIS raw-data tables).
- Has four **universal columns** (`title`, `pid`, `version`, `publication_date`) duplicated from the JSON blob for SQL sort/filter.
- Stores **source-specific metadata** (InvenioRDM creators, community, access details) in a MariaDB `JSON` column (`resource_metadata`). At the expected scale of <100 rows/project, this is efficient; the escape hatch for future scale is a generated virtual column + index — no entity change required.
- **Soft-delete for removal** (ADR-0001, ADR-0003): deleting a connection sets `connection_state = 'REMOVED'`, leaving the row as an audit tombstone. Active query paths filter `REMOVED` out. There is no `DELETE` statement in v1.
- **No SQL FK constraint on `experiment_id`** — the application enforces referential integrity in memory so that project-ACL changes and experiment deletions don't cascade-delete dataset connections.

### What is NOT in this migration

A second table, `user_invenio_rdm_credential` (for per-user Personal Access
Tokens to access restricted datasets), is planned for Stories 14/15
(FEAT-DATSET-14, FEAT-DATSET-15). It is intentionally **not** created by this
migration because FEAT-DATSET-01 covers only public, open datasets. A follow-up
entry in this file — or in the next release's `NEXT.md` — will cover the
credentials table when those stories land.

### Pre-flight

Run these checks against the target database **before** applying:

```sql
-- 1. Confirm the table does NOT already exist
SELECT COUNT(*) AS table_exists
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name = 'associated_dataset';
-- Expected: 0

-- 2. Confirm target datasource charset/collation matches the rest of the schema
SELECT @@character_set_database AS charset,
       @@collation_database    AS collation;
-- Expected: utf8mb4, utf8mb4_unicode_ci

-- 3. Confirm you are connected to the data-management datasource
SELECT DATABASE();
-- Expected: data_management (or whatever your DM schema is named)
```

If the table already exists (return value = 1), this migration was already
applied — no further action required (the DDL is idempotent).

### Apply

```bash
mysql -u <user> -h <host> -P <port> data_management \
    < sql/migrations/create-associated-dataset.sql
```

Or inline:

```sql
-- The full DDL is inlined in the script for review; see
-- sql/migrations/create-associated-dataset.sql
```

### Verify

```sql
-- 1. Confirm 15 columns
SELECT COUNT(*) AS column_count
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'associated_dataset';
-- Expected: 15

-- 2. Confirm indexes are in place (4 keys + PK = 5 rows)
SELECT index_name, GROUP_CONCAT(column_name ORDER BY seq_in_index) AS columns
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name = 'associated_dataset'
GROUP BY index_name
ORDER BY index_name;
-- Expected rows:
--   PRIMARY                       id
--   idx_assoc_ds_project          project_id
--   idx_assoc_ds_project_state    project_id,connection_state
--   idx_assoc_ds_source_type      source_type
--   idx_assoc_ds_state            connection_state

-- 3. Confirm the application can query the table
SELECT COUNT(*) AS active_connected_count
FROM associated_dataset
WHERE connection_state <> 'REMOVED';
-- Expected: 0 (empty table immediately after migration)

-- 4. Confirm no unintended table was created on the finance datasource
SELECT table_schema, table_name
FROM information_schema.tables
WHERE table_name = 'associated_dataset';
-- Expected: exactly one row, for the data-management schema
```

### Rollback

Rollback is destructive — any rows inserted by the application after the
migration will be lost. Because FEAT-DATSET-01 is the first release introducing
this table, rollback is only meaningful before any datasets have been connected
by users.

```sql
-- DANGER: drops all connected-dataset connections.
-- Safe only when the table is empty (see rollback plan below).
DROP TABLE IF EXISTS `associated_dataset`;
```

If the table contains connected-dataset rows, rollback requires one of:

1. **Reverse the feature deployment** to a version that does not reference the
   table, then drop the table.
2. **Truncate the table**, then drop it. Both cases lose all connection data.

In either case, notify users that previously connected datasets will no longer
appear in their projects.

### Operator notes

- **No data is moved.** This migration only creates an empty table.
- **No downtime required.** `CREATE TABLE IF NOT EXISTS` on MariaDB takes
  milliseconds and does not lock other tables.
- **No foreign key constraint is created** between
  `associated_dataset.experiment_id` and `experiments_datamanager.id`. The
  application enforces referential integrity in memory.
- **Future migration (same feature):** Stories 14/15 will add the
  `user_invenio_rdm_credential` table. Expect a follow-up entry in this file
  (or in a subsequent release's `NEXT.md`).

---

## Migration #<next>: <title>

*Template — copy this heading and fill it in when a new schema change lands.*

| Field | Value |
|---|---|
| **Story** | <GitHub link> |
| **Feature** | <GitHub link> |
| **ADRs** | <comma-separated ADR links> |
| **Scope** | <what kind of change: new table / alter / index / view / …> |
| **Script** | `sql/migrations/<script-name>.sql` |
| **Target datasource** | <`data_management` or `finance` or both> |

### What it does

<Explanation of the schema change and which ADRs/decisions drive it.>

### Pre-flight

```sql
-- <SQL checks to run before applying.>
```

### Apply

```bash
mysql -u <user> -h <host> -P <port> <datasource> \
    < sql/migrations/<script-name>.sql
```

### Verify

```sql
-- <SQL checks to confirm the migration worked.>
```

### Rollback

```sql
-- <Rollback SQL, if safe. Explicitly call out destructive operations.>
```

### Operator notes

- <Relevant caveats, downtime needed, data movement, etc.>

---

## Release cut procedure

When this release is ready to ship:

1. **Freeze this file.** No further entries after the version is cut.
2. **Confirm the target version.** Update `<version>` in root `pom.xml` if it
   wasn't already; record it here.
3. **Rename:** `NEXT.md` → `released/v<version>.md`
4. **Add a release-wide pre-flight section** to the new file if applicable.
5. **Reset `NEXT.md`:** create a fresh empty copy from the template above
   for the *subsequent* release.

See [`README.md`](README.md) for the full migration documentation structure.
