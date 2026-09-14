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
| 1 | [`create-pinned-projects.sql`](../../sql/migrations/create-pinned-projects.sql) | Create `pinned_projects` table holding per-user pinned-project associations | low (additive, new empty table) |

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

## Migration #1: Create the `pinned_projects` table

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