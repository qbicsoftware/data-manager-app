# Migration: Next release

**Current pom version:** 1.15.2  
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
| 1 | [`add-sample-batch-property-and-project-association.sql`](../../sql/migrations/add-sample-batch-property-and-project-association.sql) | Add `batch`, `project_id`, `registrationTime`, `lastModified` to `sample` and backfill from legacy `sample_batches` (additive) | Low (non-destructive) |
| 2 | [`finalize-sample-batch-removal.sql`](../../sql/migrations/finalize-sample-batch-removal.sql) | Add `project_id` FK, drop `sample.assigned_batch_id`, drop legacy `sample_batches`/`sample_batches_sampleid` (stop-the-world) | High (destructive) |
| 3 | [`create-pinned-projects.sql`](../../sql/migrations/create-pinned-projects.sql) | Create `pinned_projects` table holding per-user pinned-project associations | low (additive, new empty table) |
| 4 | [`create-user-groups.sql`](../../sql/migrations/create-user-groups.sql) | Create `user_group` + `group_membership` tables for the user-groups bounded context (ad-hoc group creation) | Low (additive, new empty tables) |

Each row links to its incremental script. The sections below expand each entry
with apply / verify / rollback detail.

---

## Application properties changes

*Document any new, removed, or renamed `application.properties` entries here.
If there are none this release, delete this section.*

### New routing property for the datasets view

This release adds a new routing property used by the dataset notification
emails (dataset connected / updated / removed):

```properties
# Route to the project's connected-datasets view (used by dataset notification emails)
routing.projects.datasets.endpoint=/projects/%s/datasets
```

The emails previously linked to the project info page via
`routing.projects.info.endpoint` (`/projects/%s/info`); they now link straight
into the project's datasets view (`/projects/%s/datasets`, matching the
`projects/:projectId?/datasets` Vaadin route).

**This is a purely additive, non-breaking configuration change** — the
`/projects/%s/info` property continues to be used for the project-access-grant
email and is unchanged. The new property ships with the default value above
and does not require an environment variable or secret.

**Operator action:**

1. Add the property above to your `application.properties` (or copy the
   updated `application.properties.template`).
2. Restart the application.
3. Verify a dataset connect/sync/removal notification email links to
   `…/projects/<project-id>/datasets` instead of `…/projects/<project-id>/info`.

No schema migration is associated with this entry.

---

## Migration #1: Sample batch property and project association (additive)

| Field | Value |
|---|---|
| **Story** | [FEAT-SAMBAT-01 #1549](https://github.com/qbicsoftware/data-manager-app/issues/1549), [FEAT-SAMBAT-02 #1550](https://github.com/qbicsoftware/data-manager-app/issues/1550), [FEAT-SAMBAT-03 #1551](https://github.com/qbicsoftware/data-manager-app/issues/1551) |
| **Feature** | [FEAT-SAMPLE-BATCH-REMOVAL #1548](https://github.com/qbicsoftware/data-manager-app/issues/1548) |
| **ADRs** | none (ADRs 0008/0009 were removed as too implementation-specific) |
| **Scope** | alter `sample` (add columns + backfill) |
| **Script** | `sql/migrations/add-sample-batch-property-and-project-association.sql` |
| **Target datasource** | `data_management` |

### What it does

Removes the explicit sample batch from the data model by moving the batch name
onto each sample and introducing a direct project association (SAMPLE-R-01,
SAMPLE-R-02, SAMPLE-R-03). This script **adds** the new columns to `sample` and
backfills them from the legacy `sample_batches` tables:

- `sample.batch` ← `sample_batches.batchLabel`
- `sample.registrationTime` ← `sample_batches.createdOn`
- `sample.lastModified` ← `sample_batches.lastModified`
- `sample.project_id` ← derived from `sample.experiment_id` via
  `experiments_datamanager.project`

It is **non-destructive**: the legacy `sample_batches`/`sample_batches_sampleid`
tables and `sample.assigned_batch_id` are **kept in place**. This is deliberate
because the database is shared with the test system and other nodes are running.

**This script must be applied before migration #2.** `project_id` is added as a
plain column here; the real FK constraint is added by migration #2 at release.

### Pre-flight

```sql
-- Confirm the legacy tables and columns exist before applying.
SHOW TABLES LIKE 'sample_batches%';
SELECT COLUMN_NAME FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sample'
   AND COLUMN_NAME IN ('batch','project_id','registrationTime','lastModified','assigned_batch_id');
```

### Apply

```bash
mysql -u <user> -h <host> -P <port> data_management \
    < sql/migrations/add-sample-batch-property-and-project-association.sql
```

### Verify

```sql
SELECT
    COUNT(*) AS total_samples,
    SUM(s.batch IS NULL)                       AS samples_without_batch,
    SUM(s.project_id IS NULL)                  AS samples_without_project,
    SUM(s.registrationTime IS NULL)            AS samples_without_registration_time
FROM `sample` s;
-- Expect samples_without_batch / _project / _registration_time to be low or 0.
-- Investigate any sample that still has a batch (assigned_batch_id IS NOT NULL)
-- but a NULL backfilled value, before proceeding to migration #2.
```

### Rollback

This migration is additive and non-destructive. To roll back, simply drop the
added columns (or leave them; they are unused until the new code is deployed):

```sql
ALTER TABLE `sample` DROP COLUMN `batch`;
ALTER TABLE `sample` DROP COLUMN `project_id`;
ALTER TABLE `sample` DROP COLUMN `registrationTime`;
ALTER TABLE `sample` DROP COLUMN `lastModified`;
```

### Operator notes

- **No downtime required** for this migration — it is additive and safe to run
  while nodes are up.
- **Batch name uniqueness:** before this migration, confirm whether any
  experiment has two distinct batches with the same `batchLabel`. Duplicate
  names within one experiment would produce indistinguishable samples by name
  after migration. See the duplicate-name check queries.
- Migration #2 (finalize) must run only after this one, at the stop-the-world
  window.

---

## Migration #2: Finalize sample batch removal (stop-the-world, destructive)

| Field | Value |
|---|---|
| **Story** | [FEAT-SAMBAT-03 #1551](https://github.com/qbicsoftware/data-manager-app/issues/1551) |
| **Feature** | [FEAT-SAMPLE-BATCH-REMOVAL #1548](https://github.com/qbicsoftware/data-manager-app/issues/1548) |
| **ADRs** | none (ADRs 0008/0009 were removed as too implementation-specific) |
| **Scope** | alter `sample` (FK + drop column) and drop legacy tables |
| **Script** | `sql/migrations/finalize-sample-batch-removal.sql` |
| **Target datasource** | `data_management` |

### What it does

Completes the sample batch removal (SAMPLE-R-02, SAMPLE-R-03) after migration
#1 has populated the new columns. It:

- adds the real FK constraint on `sample.project_id` →
  `projects_datamanager(projectId)`;
- drops the obsolete `sample.assigned_batch_id` column;
- drops the legacy `sample_batches` and `sample_batches_sampleid` tables.

**Destructive — run only during the coordinated stop-the-world downtime
window**, after all nodes have been shut down and after migration #1 has been
applied and verified.

### Pre-flight

```sql
-- Refuse to proceed unless the additive backfill ran and the new columns exist.
SELECT COLUMN_NAME FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sample'
   AND COLUMN_NAME IN ('batch','project_id','registrationTime','lastModified');

-- Confirm no sample that HAS a batch is missing a backfilled value.
SELECT COUNT(*) AS samples_missing_backfilled_values
FROM `sample` s
JOIN `sample_batches` sb ON sb.id = s.assigned_batch_id
WHERE s.assigned_batch_id IS NOT NULL
  AND (s.`batch` IS NULL OR s.`registrationTime` IS NULL OR s.`lastModified` IS NULL);
-- Expect 0. If > 0, STOP: investigate before running this script.
```

### Apply

```bash
# All nodes must be down. The script opens a transaction; review the verify
# output, then COMMIT (or ROLLBACK).
mysql -u <user> -h <host> -P <port> data_management \
    < sql/migrations/finalize-sample-batch-removal.sql
# then, in the client:
COMMIT;   # or ROLLBACK;
```

### Verify

```sql
-- Expect zero legacy batch tables.
SHOW TABLES LIKE 'sample_batches%';

-- Expect batch, project_id, registrationTime, lastModified present and
-- assigned_batch_id absent.
SELECT COLUMN_NAME FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sample'
   AND COLUMN_NAME IN ('batch','project_id','registrationTime','lastModified','assigned_batch_id');

-- Expect the FK constraint is present.
SELECT CONSTRAINT_NAME FROM information_schema.TABLE_CONSTRAINTS
 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sample'
   AND CONSTRAINT_TYPE = 'FOREIGN KEY';
```

### Rollback

**Not safely reversible.** The legacy `sample_batches`/`sample_batches_sampleid`
tables and `sample.assigned_batch_id` are dropped; recreate from a database
backup taken before this migration. Ensure a backup exists before applying.

### Operator notes

- **Full downtime required:** all nodes must be down before applying; bring
  them back up together.
- **Requires a backup** of the `data_management` database taken immediately
  before this migration (it is destructive).
- **Must run after migration #1.** Verify the pre-flight counts are clean before
  applying.
- New code (sample batch removal release) must be deployed together with this
  migration so the application never runs against a mismatched schema.

---

## Migration #3: Create the `pinned_projects` table

| Field | Value |
|---|---|
| **Story** | [FEAT-PINNED-01](../features.md#feat-pinned-01--pin-projects-for-quick-access-on-the-project-overview) — tracked in `docs/features.md`, no GitHub issue |
| **Feature** | `FEAT-PINNED-PROJECTS` · requirement `USER-R-04` |
| **ADRs** | [ADR-0008](../adr/0008-pinned-projects-as-user-preferences.md) |
| **Scope** | new table |
| **Script** | `sql/migrations/create-pinned-projects.sql` |
| **Target datasource** | `data_management` |

### What it does

Creates `pinned_projects`, one row per (user, project) pair the user pinned for quick access on the
project overview. The row carries the pin timestamp (the shortlist is ordered newest pin first) and
a write-once snapshot of the project code and title, which exists only so that a pin whose project
the user can no longer read stays recognisable and removable — see ADR-0008.

`userId` is a bare `varchar(255)` with **no** foreign key to `users`, following the existing
`personal_access_tokens.userId` precedent, so `project-management` gains no schema dependency on the
`identity` context. `projectId` references `projects_datamanager(projectId)` with
`ON DELETE CASCADE`, so project deletion cannot leave orphan pins.

### Pre-flight

```sql
-- The referenced table must exist; it always does in this schema.
SHOW TABLES FROM data_management LIKE 'projects_datamanager';

-- Expect 0 rows: the migration is idempotent, but a non-zero count means it was already applied.
SELECT COUNT(*) FROM information_schema.tables
 WHERE table_schema = 'data_management' AND table_name = 'pinned_projects';
```

### Apply

```bash
mysql -u <user> -h <host> -P <port> data_management \
    < sql/migrations/create-pinned-projects.sql
```

### Verify

```sql
SHOW CREATE TABLE data_management.pinned_projects\G
-- Expect: PRIMARY KEY (userId, projectId),
--         KEY idx_pinned_projects_user_pinned_at (userId, pinnedAt),
--         fk_pinned_projects_project → projects_datamanager(projectId) ON DELETE CASCADE

SELECT COUNT(*) FROM data_management.pinned_projects; -- 0 until users start pinning
```

### Rollback

```sql
-- Destroys user shortlists only; no project data is affected.
DROP TABLE IF EXISTS data_management.pinned_projects;
```

### Operator notes

- Safe to run while the application is live: a new, empty table is created and no existing object is
  locked or altered.
- The application must not start with the new code before the migration is applied; the pinned-project
  row is read on every project overview render and fails with “table not found” otherwise.
- No backfill: pins are created by users in the UI.

---

## Migration #4: Create the user-groups tables (ad-hoc group creation)

| Field | Value |
|---|---|
| **Story** | [FEAT-USER-GROUPS-03 #1561](https://github.com/qbicsoftware/data-manager-app/issues/1561) |
| **Feature** | [FEAT-USER-GROUPS #1558](https://github.com/qbicsoftware/data-manager-app/issues/1558) |
| **ADRs** | 0010 (user-groups bounded context) — pending human approval |
| **Scope** | two new tables |
| **Script** | `sql/migrations/create-user-groups.sql` |
| **Target datasource** | `data_management` |

### What it does

Creates the `user_group` and `group_membership` tables of the new `user-groups` bounded
context (GROUP-R-02, GROUP-NFR-02). Ad-hoc group creation by authenticated researchers becomes
possible: the creator becomes the group OWNER (self-service, no admin involved).

- `user_group.id` is the stable UUID by which the group will be referenced in Spring ACL
  authority SIDs (`"GROUP_<id>"`) once the sharing feature lands;
- `user_group.name` is unique **case-insensitively** via the `utf8mb4_unicode_ci` collation and
  its unique index — the authoritative DB backstop for the duplicate-name acceptance criterion;
- `user_group.status` supports soft dissolve (`ACTIVE` → `DISSOLVED`); dissolved rows are kept
  for traceability;
- `group_membership` carries the group-internal role (`OWNER|MANAGER|MEMBER`) and its composite
  primary key `(group_id, user_id)` makes a user hold at most one membership per group;
- `created_by` / `user_id` are bare varchar identity-user references with **no foreign keys** to
  `users`, following the `personal_access_tokens.userId` / `pinned_projects.userId` precedent
  (bounded-context rule, GROUP-C-01 / ADR-0010).

### Pre-flight

```sql
-- Expect 0 rows: the migration is idempotent, but non-zero means it was already applied.
SELECT COUNT(*) FROM information_schema.tables
 WHERE table_schema = 'data_management' AND table_name IN ('user_group', 'group_membership');
```

### Apply

```bash
mysql -u <user> -h <host> -P <port> data_management \
    < sql/migrations/create-user-groups.sql
```

### Verify

```sql
SHOW CREATE TABLE data_management.user_group\G
-- Expect: PRIMARY KEY (id), UNIQUE KEY uk_user_group_name (name), ENGINE=InnoDB,
--         CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
SHOW CREATE TABLE data_management.group_membership\G
-- Expect: PRIMARY KEY (group_id, user_id), KEY idx_group_membership_user (user_id)

SELECT COUNT(*) FROM data_management.user_group;        -- 0 until users create groups
SELECT COUNT(*) FROM data_management.group_membership;  -- 0 until users join groups
```

### Rollback

```sql
-- The feature is additive; dropping the tables returns the schema to its previous state.
DROP TABLE IF EXISTS data_management.group_membership;
DROP TABLE IF EXISTS data_management.user_group;
```

### Operator notes

- Safe to run while the application is live: two new, empty tables are created and no existing
  object is locked or altered.
- The application must not start with the new code before the migration is applied; group reads
  fail with “table not found” otherwise.
- No backfill: groups are created by users in the UI.
- The case-insensitive name uniqueness depends on the `utf8mb4_unicode_ci` collation; do not
  switch these tables to a case-sensitive collation.

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