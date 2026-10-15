# Migration: Next release

**Current pom version:** 1.19.0  
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
| 1 | *(automatic — bundled with JobRunr)* | **JobRunr 8.7.1 → 9.0.x** auto-applies its bundled migration `v017__change_job_stats_view_to_sql_security_invoker.sql` on first startup: the `jobrunr_jobs_stats` view is recreated with `SQL SECURITY INVOKER` (MariaDB/MySQL). Atomic `CREATE OR REPLACE VIEW`; no table/index DDL, no downtime. Applied once and recorded in `jobrunr_migrations`. | Low |
| 2 | [`create-project-page-read-models.sql`](../sql/migrations/create-project-page-read-models.sql) | Create the project-page read-model views `experiment_progress`, `v_project_sample_overview`, `v_measurement_experiment`, plus a supporting index on `sample(experiment_id)`. Views + one index only; no table or column changes. | Low (non-destructive) |
| 3 | [`create-experiment-recent-activity-views.sql`](../sql/migrations/create-experiment-recent-activity-views.sql) | Create the recent-activity views `experiment_recent_sample` and `experiment_recent_measurement` (newest three per experiment, via `ROW_NUMBER`) for the experiment snapshot panels. Views only. | Low (non-destructive; needs MariaDB 10.2+) |

Each row links to its incremental script. The sections below expand each entry
with apply / verify / rollback detail.

---

## Application properties changes

*Document any new, removed, or renamed `application.properties` entries here.
If there are none this release, delete this section.*

---

## Migration #2: Project page read models

| Field | Value |
|---|---|
| **Story** | TBD (governance gate skipped at project-owner direction) |
| **Feature** | TBD (`FEAT-PROJECT-*`) |
| **ADRs** | TBD (read-model / query-service ADR; reactive boundary ADR) |
| **Scope** | create 3 views + 1 index |
| **Script** | [`sql/migrations/create-project-page-read-models.sql`](../sql/migrations/create-project-page-read-models.sql) |
| **Target datasource** | `data_management` |

### What it does

Adds the read models the reworked project page renders from, so the landing page is served
by a small, fixed number of queries instead of composing roughly seven lookups per
experiment (the N+1 the current service layer would force).

- **`experiment_progress`** — one row per experiment: `sample_count`,
  `measured_sample_count`, `measurement_count` (plus per-type NGS/PXP/IP),
  `measurements_with_primary_data`, `primary_data_files`, `primary_data_bytes`. Serves both
the summary progress band and the project Datasets overview.
- **`v_project_sample_overview`** — one row per sample in a project, with experiment
  attribution, condition and a `measured` flag; feeds the project Samples list and stats.
- **`v_measurement_experiment`** — helper that maps a measurement to the experiment(s) it
  belongs to, via its measured samples.
- **`idx_sample_experiment`** — `sample` has no index on `experiment_id`; the per-experiment
  sample aggregation groups by it. The statement is guarded and idempotent.

All three are views: non-destructive, always consistent, no write path. No table or column
is created, altered or dropped.

> **Two source data facts the read models depend on.** Please read before reviewing numbers:
>
> 1. `remote_measurement_data.measurement_id` holds the **measurementCode**, not the
>    measurement primary key. The join is `rmd.measurement_id = measurementCode`, matching
>    the existing `v_*_measurement_sample_json` views. This column name is a drift.
> 2. "Measurements without primary data" is computed as
>    `measurement_count - measurements_with_primary_data`. `deleted = FALSE` is enforced.
>
> A measurement that measures samples from more than one experiment is attributed to each
> experiment and counted in each (see *Verify*).

### Pre-flight

```sql
-- Confirm the source tables exist.
SELECT TABLE_NAME
  FROM information_schema.TABLES
 WHERE TABLE_SCHEMA = DATABASE()
   AND TABLE_NAME IN ('sample', 'experiments_datamanager', 'ngs_measurements',
                      'proteomics_measurement', 'ip_measurements',
                      'specific_measurement_metadata_ngs',
                      'specific_measurement_metadata_pxp',
                      'specific_measurement_metadata_ip',
                      'remote_measurement_data');

-- Confirm the measurementCode join convention (non-empty result proves it).
SELECT m.measurementCode, rmd.measurement_id
  FROM ngs_measurements m
  JOIN remote_measurement_data rmd ON rmd.measurement_id = m.measurementCode
 LIMIT 5;
```

### Apply

```bash
mysql -u <user> -h <host> -P <port> data_management \
    < sql/migrations/create-project-page-read-models.sql
```

### Verify

```sql
-- Views present.
SHOW CREATE VIEW experiment_progress;

-- One row per experiment; counts are consistent.
SELECT * FROM experiment_progress LIMIT 20;

-- Measurements attributed to more than one experiment (trap: mixed experiments).
-- Empty is expected; rows are not an error, just an attribution note.
SELECT measurement_type, measurement_id, COUNT(DISTINCT experiment_id) AS experiments
  FROM v_measurement_experiment
 GROUP BY measurement_type, measurement_id
HAVING experiments > 1;

-- Primary-data counts exclude deleted rows.
SELECT COUNT(*) FROM remote_measurement_data WHERE deleted = TRUE;
```

### Rollback

```sql
DROP VIEW IF EXISTS experiment_progress;
DROP VIEW IF EXISTS v_project_sample_overview;
DROP VIEW IF EXISTS v_measurement_experiment;
DROP INDEX idx_sample_experiment ON sample;
```

### Operator notes

- **Non-destructive and safe on a live database.** No downtime; the application may keep
  running while this is applied. The new views are inert until the new application code
  reads them.
- **Index build.** `CREATE INDEX idx_sample_experiment` is a secondary-index build on
  `sample`. InnoDB performs it online, but it takes a brief lock at the start and end; on a
  large `sample` table allow a short maintenance window for the statement even though the
  view creation itself is instantaneous.
- **No backfill, no data movement.** The views read live tables, so their numbers are
  always current.
- If the landing page later proves slow, the same read-model shape can be served by an
  event-maintained summary table without changing any consumer.

---

## Deferred — not part of this release

[`create-user-project-view-state.sql`](../sql/migrations/create-user-project-view-state.sql)
(adds the `user_project_view_state` table for the summary's "new since you last looked")
was drafted alongside the read models but is **deliberately not included** in this release:
where per-user view state belongs is still an open decision (cf. ADR-0008, user preferences
vs project state). **Do not apply it** until that decision is recorded; it will be promoted
to a numbered migration in a later release.

---

## Migration #3: Recent-activity read models (experiment snapshot panels)

| Field | Value |
|---|---|
| **Story** | TBD (governance gate skipped at project-owner direction) |
| **Feature** | TBD (`FEAT-PROJECT-*`) |
| **ADRs** | TBD (read-model / query-service ADR) |
| **Scope** | create 2 views |
| **Script** | [`sql/migrations/create-experiment-recent-activity-views.sql`](../sql/migrations/create-experiment-recent-activity-views.sql) |
| **Target datasource** | `data_management` |

### What it does

Adds the read models behind the experiment snapshot panels on the project page: the newest three
samples and the newest three measurements per experiment.

- **`experiment_recent_sample`** — newest samples per experiment, ordered by
  `COALESCE(lastModified, registrationTime)` descending, with the sample code as tie-break.
- **`experiment_recent_measurement`** — newest measurements per experiment across the three type
tables (union), ordered by registration descending, left-joined to `remote_measurement_data`
(joined on the measurementCode, `deleted = FALSE`) so each row carries its primary-data file count
and size. A measurement measuring samples of more than one experiment is attributed to each (D8).

Both use `ROW_NUMBER() OVER (PARTITION BY experiment_id ORDER BY …) <= 3`, so a page renders from one
query per view instead of a per-experiment N+1. **Requires MariaDB 10.2+.**

### Pre-flight

```sql
SELECT VERSION();
```

### Apply

```bash
mysql -u <user> -h <host> -P <port> data_management \
    < sql/migrations/create-experiment-recent-activity-views.sql
```

### Verify

```sql
-- At most three rows per experiment (empty = good).
SELECT experiment_id, COUNT(*) AS n FROM experiment_recent_sample
 GROUP BY experiment_id HAVING n > 3;
SELECT experiment_id, COUNT(*) AS n FROM experiment_recent_measurement
 GROUP BY experiment_id HAVING n > 3;
```

### Rollback

```sql
DROP VIEW IF EXISTS experiment_recent_measurement;
DROP VIEW IF EXISTS experiment_recent_sample;
```

### Operator notes

- Non-destructive and safe on a live database; no downtime, no data movement.
- Needs MariaDB 10.2 or newer (window functions).

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
4. **Copy the referenced scripts** into `released/v<version>/` and repoint the
   guide at those copies (a released guide must be self-contained).
5. **Add a release-wide pre-flight section** to the new file if applicable.
6. **Reset `NEXT.md`:** create a fresh empty copy from the template above
   for the *subsequent* release.

See [`README.md`](README.md) for the full migration documentation structure.
