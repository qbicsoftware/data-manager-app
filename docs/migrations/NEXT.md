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
| 4 | [`create-assay-standing-view.sql`](../sql/migrations/create-assay-standing-view.sql) | Create `v_project_assay_standing`: one row per primary dataset — one experiment for one analysis method — with measurement counts, primary-data coverage and volume. One derivation shared by the summary and Datasets pages (ADR 0010). View + one index. | Low (non-destructive; needs MariaDB 10.2+) |
| 5 | [`auth-flow-hardening.sql`](../sql/migrations/auth-flow-hardening.sql) | Create `user_verification_token` (single-use, hashed, time-limited confirmation / reset / set-password links, AUTH-R-05). One new table; no existing table altered. Throttling is a **separate script** (#6) so each can be applied and rolled back independently. | Medium (one new table; **links in flight stop working by design** — see operator notes) |
| 6 | [`auth-attempt-throttling.sql`](../sql/migrations/auth-attempt-throttling.sql) | Create `auth_attempt`: server-side throttling for login, resend-confirmation and resend-reset (AUTH-NFR-03, #1682). One new table; no existing table altered; no FK. Replaces the client-side 30 s timer, which a page reload reset. | Low (additive; **behaviour change** — repeated attempts now blocked server-side) |

Each row links to its incremental script. The sections below expand each entry
with apply / verify / rollback detail.

---

## Application properties changes

> **Read this before upgrading.** This release renames two configuration keys and changes two
> defaults. Spring Boot does **not** fail on unknown environment variables or unknown properties, so
> a deployment carrying the old names will start cleanly and silently ignore them. Nothing warns you.

### Renamed keys — old names are now ignored

| Removed | Replacement | Notes |
|---|---|---|
| `routing.email-confirmation.confirmation-parameter` | `routing.email-confirmation.token-parameter` | same meaning, new name |
| `routing.password-reset.reset-parameter` | `routing.password-reset.token-parameter` | same meaning, new name |
| `EMAIL_CONFIRMATION_PARAMETER` (env) | `EMAIL_CONFIRMATION_TOKEN_PARAMETER` | env var renamed |
| `PASSWORD_RESET_PARAMETER` (env) | `PASSWORD_RESET_TOKEN_PARAMETER` | env var renamed |

### Changed defaults

| Key | Old default | New default | Why |
|---|---|---|---|
| `routing.email-confirmation.endpoint` | `/login` | `/registration/confirm` | confirmation moved off `/login` so it no longer mutates state on GET |
| `routing.password-reset.endpoint` | `new-password` | `/registration/new-password` | the old default was a bare, non-navigable path |

**If your deployment explicitly sets either endpoint to the old value**, links will point at routes
that no longer consume tokens. They will appear broken — and for a different reason than the
intentional link-format change below, which will make it harder to diagnose. Check both.

### New keys (all have working defaults)

| Key | Default | Bound |
|---|---|---|
| `qbic.verification-token.confirmation-lifetime` | `PT1H` | max `PT1H` (AUTH-R-05) |
| `qbic.verification-token.reset-lifetime` | `PT5M` | max `PT15M` (AUTH-R-05) |
| `qbic.verification-token.set-password-lifetime` | `PT1H` | max `PT1H` (AUTH-R-05) |

Values are ISO-8601 durations (`PT1H`, `PT5M`, `PT30M`).

⚠️ **An out-of-bound value is a startup failure, not a silent weakening.** The service validates
these in its constructor and refuses to boot if a configured lifetime exceeds the AUTH-R-05 maximum.
Setting `VERIFICATION_RESET_LIFETIME=PT30M` will stop the application from starting. Shorter values
are always allowed.

### Deployment checklist for this release

1. Apply the SQL migration **first** — see the section above. The application cannot issue or consume
   tokens without `user_verification_token`, and the failure is silent: registration still appears to
   succeed, but the confirmation email job fails and no link works.
2. Rename the two environment variables (or the two property keys) in the deployment configuration.
3. Check whether `EMAIL_CONFIRMATION_ENDPOINT` / `PASSWORD_RESET_ENDPOINT` are overridden, and update
   them if they point at the old routes.
4. Decide whether to keep the default lifetimes. No action is needed to accept them.
5. **Use `application.properties.template` as the reference** — it is the single source of truth for a
   working configuration, and it lists every value explicitly. The documentation convention for this
   project is that administrators configure values directly there rather than through environment
   variables; this release follows that convention.

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
  `measurements_with_primary_data` (plus per-type NGS/PXP/IP) and `primary_data_bytes` (plus
  per-type NGS/PXP/IP). Serves both the summary progress band and the project Datasets
  overview. There is deliberately **no file count**: see fact 3 below.
- **`v_project_sample_overview`** — one row per sample in a project, with experiment
  attribution, condition and a `measured` flag; feeds the project Samples list and stats.
- **`v_measurement_experiment`** — helper that maps a measurement to the experiment(s) it
  belongs to, via its measured samples.
- **`idx_sample_experiment`** — `sample` has no index on `experiment_id`; the per-experiment
  sample aggregation groups by it. The statement is guarded and idempotent.

All three are views: non-destructive, always consistent, no write path. No table or column
is created, altered or dropped.

> **Collation drift is handled explicitly.** Instances have been found carrying
> `latin1_swedish_ci` (`sample`, `ngs_measurements`, `specific_measurement_metadata_ngs`,
> `proteomics_measurement`), `utf8mb4_general_ci` (`experiments_datamanager`,
> `experimental_group`) and `utf8mb4_unicode_ci` (`projects_datamanager`,
> `remote_measurement_data`, `associated_dataset`) at the same time. A column-to-column `=`
> across them raises `Illegal mix of collations`. Every string equality in these views
> therefore pins `COLLATE utf8mb4_unicode_ci` on one operand, so the comparison no longer
> depends on the columns' own collations. It is lossless (`latin1` converts up to
> `utf8mb4`) and harmless once an instance is aligned. Aligning the instance with
> `ALTER TABLE … CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci` remains the
> correct long-term fix, and is tracked separately — it is **not** part of this migration.

> **Two source data facts the read models depend on.** Please read before reviewing numbers:
>
> 1. `remote_measurement_data.measurement_id` holds the **measurementCode**, not the
>    measurement primary key. The join is `rmd.measurement_id = measurementCode`, matching
>    the existing `v_*_measurement_sample_json` views. This column name is a drift.
> 2. "Measurements without primary data" is computed as
>    `measurement_count - measurements_with_primary_data`. `deleted = FALSE` is enforced.
> 3. **`primary_data_bytes` can legitimately be 0, and there is no file count to fall back on.**
>    `total_filesize_bytes` is 0 for many synced measurements — a real project had 24
>    primary-data files across 8 measurements and 0 total bytes. That means the sizes were never
>    recorded, not that the arithmetic failed, and the view cannot tell the two apart. Anything
>    rendering this must distinguish "0 bytes" from "unmeasured" rather than printing `0 B`.
>    A file count is deliberately not exposed: it is not comparable across assay types (one NGS
>    measurement is a couple of large files, one proteomics measurement is hundreds of small
>    ones), it cannot be acted on, and pairing it with a byte total invites the reader to divide
>    the two. A mock-up that read `0.0 GB in 24 files` is the failure mode.
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

## Migration #4: Assay standing read model

| Field | Value |
|---|---|
| **Story** | TBD (governance gate skipped at project-owner direction) |
| **Feature** | TBD (`FEAT-PROJECT-*`) |
| **ADRs** | [0010](../adr/0010-analysis-method-attribution.md) (analysis-method attribution) |
| **Scope** | create 1 view + 1 index |
| **Script** | [`sql/migrations/create-assay-standing-view.sql`](../sql/migrations/create-assay-standing-view.sql) |
| **Target datasource** | `data_management` |

### What it does

Adds the read model behind the project's primary datasets: one row per **(experiment, analysis
method)**, which is the identity of a primary dataset.

A primary dataset is the data of one experiment for one analysis method. It is not declared: it is
the consequence of measurements having been done. The analysis method is its type because that is
what a reader selects datasets by when planning an analysis; the omics domain is too coarse, since
WES, RNA-Seq and 16S are all genomics and are not interchangeable.

The view exists so the summary page and the Datasets page read **one** derivation. Two derivations
drift, and a project holding "2 WES datasets" on one page must not hold "1 Genomics dataset" on
another.

**Requires MariaDB 10.2+** (CTEs).

### Read this before changing the view

The analysis method is annotated on the **sample** (`sample.analysis_method`), not the measurement.
That is the wrong home and a known mistake, but it cannot simply be moved: it is the only source for
existing projects, and it can never be deleted without destroying their record. It is therefore a
**permanently supported grandfather relation**, and this view is the one place allowed to read it to
type a dataset (ADR 0010).

Two consequences an operator will notice in the data:

- A measurement measuring samples with **different** analysis methods is counted in **each**, so
  per-dataset measurement counts can sum to more than the measurement total. Not an error.
- Samples with **no** analysis method are excluded, so their measurements appear in the per-domain
  progress figures but not here.

### Pre-flight

```sql
SELECT VERSION();

-- How much is affected: measurements whose samples carry no analysis method (excluded from this
-- view, still counted by experiment_progress).
SELECT COUNT(*)
FROM sample
WHERE analysis_method IS NULL OR analysis_method = '';
```

### Apply

```bash
mysql -u <user> -h <host> -P <port> data_management \
    < sql/migrations/create-assay-standing-view.sql
```

### Verify

```sql
SHOW CREATE VIEW v_project_assay_standing;

-- One row per (experiment, analysis method) for a project.
SELECT experiment_name, analysis_method, measurements, measurements_with_primary_data
FROM v_project_assay_standing
WHERE project_id = '<projectId>'
ORDER BY experiment_name, analysis_method;

-- The index the script adds.
SELECT index_name
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name = 'sample'
  AND index_name = 'idx_sample_project_experiment'
LIMIT 1;
```

**Cross-check against the other read model.** The two figures answer different questions and are
expected to differ, but the direction must make sense:

```sql
SELECT
  (SELECT SUM(measurements) FROM v_project_assay_standing WHERE project_id = '<projectId>')
    AS assay_measurements,
  (SELECT SUM(measurement_count) FROM experiment_progress WHERE project_id = '<projectId>')
    AS progress_measurements;
```

`assay_measurements` may exceed `progress_measurements` (ambiguous attribution). It must **not** be
lower for a project whose samples all carry an analysis method.

### Rollback

```sql
DROP VIEW IF EXISTS v_project_assay_standing;
DROP INDEX idx_sample_project_experiment ON sample;
```

Non-destructive: no table or column is modified.

---

## Migration #5: Auth-flow hardening — verification tokens and attempt throttling

**Status:** ✅ **approved and promoted.** The AGENTS.md §12 schema-change sign-off was recorded, the
script moved to `sql/migrations/`, and its `CREATE TABLE` block was added to
`sql/complete-schema.sql` so a fresh install matches an upgraded one. **Apply the SQL before
deploying the application code** — see the deployment checklist below.

| Field | Value |
|---|---|
| **Story** | [`FEAT-AUTH-FLOWS-01`](https://github.com/qbicsoftware/data-manager-app/issues/1673) ([#1678](https://github.com/qbicsoftware/data-manager-app/issues/1678)), [`FEAT-AUTH-FLOWS-02`](https://github.com/qbicsoftware/data-manager-app/issues/1674) ([#1680](https://github.com/qbicsoftware/data-manager-app/issues/1680)), [`FEAT-AUTH-FLOWS-05`](https://github.com/qbicsoftware/data-manager-app/issues/1677) ([#1683](https://github.com/qbicsoftware/data-manager-app/issues/1683)) |
| **Feature** | [`FEAT-AUTH-FLOWS`](https://github.com/qbicsoftware/data-manager-app/issues/1672) |
| **ADRs** | none — no architectural decision required; rationale is in the script header |
| **Scope** | create 1 table; no existing table altered |
| **Script** | [`sql/migrations/auth-flow-hardening.sql`](../sql/migrations/auth-flow-hardening.sql) |
| **Target datasource** | `data_management` |

### What it does

One table, additive:

- **`user_verification_token`** — single-use, time-limited tokens for email confirmation
  (`EMAIL_CONFIRMATION`), password reset (`PASSWORD_RESET`) and the one-time set-password invitation
  for ORCID-only accounts (`SET_PASSWORD`). Only a **SHA-256 hash** is stored; the raw token exists
  once, in the outgoing email, and cannot be recovered from the database. Three nullable timestamps
  record how a token stopped being usable: `consumedAt` (used), `supersededAt` (replaced when a
  newer token was issued for the same purpose), or expiry via `expiresAt`.

The throttling table `auth_attempt` is **migration #6**, not part of this script — the two are
independent so either can be rolled back without the other.

### Scope decision recorded here — origin scope is not in this release

`AUTH-NFR-03` asks for throttling *per account and per request origin*. Only **account scope** ships
in this release. Origin scope is deliberately deferred because the application cannot currently
determine a client address:

- the app runs behind HAProxy, which terminates TLS;
- `server.forward-headers-strategy` is unset, no `RemoteIpValve` is configured, and nothing in the
  codebase reads `getRemoteAddr()` or `X-Forwarded-For`;
- therefore `getRemoteAddr()` returns the **proxy**.

Keying a bucket on that address would put every user of the deployment into one row, and a single
anonymous attacker filling it would block the entire user population — a self-inflicted global
denial of service, worse than the gap the throttle closes.

Doing it correctly requires trusted forwarded headers: a trusted-proxy allowlist, plus HAProxy
**overwriting** rather than appending `X-Forwarded-For` (the header is client-supplied; trusting the
leftmost entry lets an attacker mint a fresh bucket per request). That work is tracked separately and
also unblocks passkeys and the ORCID `{baseUrl}` redirect — see
[the milestone plan](../plans/MILESTONE-MFA-self-governed-2fa.md) §1a condition 1. It needs **no schema change**: `scope` is an open
`varchar` and `attemptKey` is always an opaque hash.

**Residual risk accepted:** per-account throttling stops credential stuffing against a known target
and stops mail-bombing one inbox. It does not stop an attacker sending one reset mail to many
different addresses from one source — that is volume/cost abuse, mitigated upstream by the SMTP
relay's own rate limits, and it is not account takeover.

### ⚠️ Links in flight stop working (decision B1)

This is the one user-visible consequence and it must be communicated, not discovered.

Links already delivered carry the **old** format — `/login?confirm-email={userId}` for confirmation
and `/registration/reset-password?…={userId}` for reset — which the new code no longer accepts.
They stop working **by design**; no dual-mode compatibility path is shipped (decision B1). Any user
holding an old link must request a new one.

Before applying, tell support/helpdesk:

> "After the release, older confirmation and password-reset emails will no longer work. If a user
> reports a dead link, the answer is: request a new link from the login page. Do not ask them to
> resend the original email."

### Pre-flight

```sql
-- The table must be absent (any row means a previous partial apply — stop and investigate).
SELECT TABLE_NAME
  FROM information_schema.TABLES
 WHERE TABLE_SCHEMA = DATABASE()
   AND TABLE_NAME = 'user_verification_token';

-- `users` must exist and be referenced by the new FK. Confirm the id column type matches
-- varchar(255); a mismatch fails the FK at apply time.
SELECT COLUMN_NAME, COLUMN_TYPE
  FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = DATABASE()
   AND TABLE_NAME = 'users'
   AND COLUMN_NAME = 'id';
```

### Apply

```bash
mysql -u <user> -h <host> -P <port> data_management \
    < sql/migrations/auth-flow-hardening.sql
```

### Verify

```sql
-- The table exists with the expected shape.
SHOW CREATE TABLE user_verification_token;

-- The hash column must be char(64) — a varchar(255) here means an older draft was applied.
SELECT COLUMN_NAME, COLUMN_TYPE
  FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = DATABASE()
   AND TABLE_NAME = 'user_verification_token'
   AND COLUMN_NAME = 'tokenHash';

-- The FK to users is present and cascades.
SELECT CONSTRAINT_NAME, DELETE_RULE
  FROM information_schema.REFERENTIAL_CONSTRAINTS
 WHERE CONSTRAINT_SCHEMA = DATABASE()
   AND TABLE_NAME = 'user_verification_token';
```

Functional check after the code is deployed: register a user, follow the confirmation link once
(address confirmed), follow it again (rejected). Request a reset link, follow it, set a new password,
follow it again (rejected).

### Rollback

```sql
DROP TABLE IF EXISTS `user_verification_token`;
```

**Two warnings, both in the script header as well:**

1. **Rollback invalidates every unconsumed link.** Users holding one must request a new one. A
   rollback after go-live needs the operator note above, not a silent drop.
2. **Rollback destroys the audit trail.** `user_verification_token` is the only record of which links
   were issued, consumed, superseded or expired. Export before dropping — the exact statement is in
   the script header.

### Operator notes

- **Non-destructive.** One new table; no existing table, column, view or index is touched. Safe to
  apply on a live database with the application running.
- **Nothing reads these tables until the new application code ships.** Applying the DDL alone changes
  no behaviour — old links keep working until the code deploy.
- **Retention is not decided here.** Consumed rows are kept for audit and are meant to be purged by a
  scheduled job; the retention period is owned by the security and privacy note
  ([#1669](https://github.com/qbicsoftware/data-manager-app/issues/1669)). Until that is settled, do
  not implement a purge — and note that the audit export above may be the only copy.
- **There is no admin or support path to read a token back.** Only hashes are stored. A user who lost
  their link gets a new one; that is the only remedy, and it is what AUTH-R-05 requires.

---

## ORCID-only accounts: password backfill runbook (`FEAT-AUTH-FLOWS-05`, #1683)

**No DDL in this section.** It is a data migration *procedure*, deliberately kept out of the SQL
script: an operator running `auth-flow-hardening.sql` must not be able to conclude that the backfill
was applied. **Do not run any of this before the code for #1683 is deployed.**

`AUTH-R-07` (decision D1b) requires every local account to hold a password. Accounts created through
the old ORCID registration path have `encryptedPassword IS NULL`. They are not fixed by an `UPDATE` —
a password cannot be generated meaningfully, it must be *chosen by the person*, and it must arrive
through the verified email address. Each affected row therefore receives a one-time `SET_PASSWORD`
invitation token.

### Pre-flight — run and record the counts BEFORE the release

```sql
SELECT COUNT(*) AS orcid_only_accounts
  FROM users
 WHERE encryptedPassword IS NULL
   AND oidcId IS NOT NULL;

SELECT COUNT(*) AS orcid_only_and_inactive
  FROM users
 WHERE encryptedPassword IS NULL
   AND oidcId IS NOT NULL
   AND active = FALSE;
```

Most of this population is `active = FALSE` (a newly registered ORCID account is created inactive and
must confirm its email), so **the invitation link has to work for an inactive account** and must set
the password *and* confirm the address in one step. That is the `SET_PASSWORD` purpose.

### Execution

The invitation is issued by the application, not by SQL — it must be idempotent across restarts and
must not double-issue. Open points to settle before running, recorded so they are not discovered in
production:

- **Who triggers it?** A JobRunr job, an admin action, or a one-off script? Must be decided and
  recorded on #1683.
- **How is double-issue prevented?** An `existing unconsumed SET_PASSWORD token` check, or
  supersede-on-reissue (which `user_verification_token` already supports via `supersededAt`).
- **What if the mailbox is dead?** The account cannot be recovered and must not be deleted
  (`FEAT-AUTH-FLOWS-05` is explicit: *do not delete accounts*). It stays passwordless, and the
  invariant below stays unmet for that row — this must be an accepted, listed exception rather than a
  silent failure.

### Post-migration invariant — verify after the rollout completes

```sql
SELECT COUNT(*) FROM users WHERE encryptedPassword IS NULL;   -- expected 0
```

**This invariant is release-coupled, not script-coupled.** It is only reachable once the #1683 code
stops creating passwordless ORCID accounts. Running this migration alone cannot satisfy it, and a
non-zero count after the rollout means either an un-reached user or a mailbox that cannot be
delivered to — investigate each row individually before declaring the release shipped.

---

## Migration #6: Server-side throttling for the auth flows

**Status:** ✅ **approved.** The AGENTS.md §12 sign-off for the `auth_attempt` table was recorded by the
Product Owner — see
[#1690](https://github.com/qbicsoftware/data-manager-app/pull/1690#issuecomment-6847303615). The approved
scope is the creation of `auth_attempt` only: one new table, no existing table altered, no foreign key.

The script was validated beforehand on a throwaway MariaDB 12.3 instance: it applies twice idempotently,
rolls back cleanly, and the upgrade path from the earlier table shape works (including adding a missing
`windowSeconds` column, which `CREATE TABLE IF NOT EXISTS` would silently skip).

| Field | Value |
|---|---|
| **Story** | [`FEAT-AUTH-FLOWS-03`](https://github.com/qbicsoftware/data-manager-app/issues/1675) ([#1682](https://github.com/qbicsoftware/data-manager-app/issues/1682)) |
| **Feature** | [`FEAT-AUTH-FLOWS`](https://github.com/qbicsoftware/data-manager-app/issues/1672) |
| **ADRs** | none — the design rationale is the script header |
| **Scope** | create 1 table; no existing table altered; **no foreign key** |
| **Script** | [`sql/migrations/auth-attempt-throttling.sql`](../sql/migrations/auth-attempt-throttling.sql) |
| **Target datasource** | `data_management` |

### What it does

Creates **`auth_attempt`**, the counter behind server-side throttling for the three unauthenticated
auth flows: `LOGIN`, `EMAIL_CONFIRMATION_RESEND`, `PASSWORD_RESET_RESEND`.

This replaces the 30-second resend lockout in `EmailConfirmationMain`, which was a `java.util.Timer`
over a field in a UI-scoped bean — reloading the page reset it, so it was not a control at all.

### What changes for users

Repeated failed logins, and repeated "resend" requests, are now blocked **server-side** and survive a
reload. The block is temporary.

**The visitor is not told they are throttled.** A throttled login shows the same
"Incorrect username or password" as a wrong password, and a throttled resend shows the same
"check your inbox" panel as a successful one. This is deliberate and is required by `AUTH-NFR-03`: a
distinct "too many attempts" screen would confirm to an attacker that the address they are guessing
at is a real account. Helpdesk scripts must not promise a specific error message for throttling.

### Thresholds are configuration, not constants

Defaults ship in `application.properties`; override per deployment. Thresholds are deployment
configuration because they are tuned from telemetry and differ per environment.

| Property | Env var | Default | Meaning |
|---|---|---|---|
| `qbic.auth-throttle.login.max-attempts` | `AUTH_THROTTLE_LOGIN_MAX_ATTEMPTS` | `10` | failed logins allowed per window |
| `qbic.auth-throttle.login.window` | `AUTH_THROTTLE_LOGIN_WINDOW` | `PT15M` | counting window |
| `qbic.auth-throttle.login.block-duration` | `AUTH_THROTTLE_LOGIN_BLOCK_DURATION` | `PT15M` | how long a block lasts |
| `qbic.auth-throttle.resend.max-attempts` | `AUTH_THROTTLE_RESEND_MAX_ATTEMPTS` | `3` | resends allowed per window |
| `qbic.auth-throttle.resend.window` | `AUTH_THROTTLE_RESEND_WINDOW` | `PT15M` | counting window |
| `qbic.auth-throttle.resend.block-duration` | `AUTH_THROTTLE_RESEND_BLOCK_DURATION` | `PT15M` | how long a block lasts |

A non-positive `max-attempts`, window or block-duration is a **startup failure**, not a silent
no-op — the same fail-fast posture as the verification-token lifetimes.

### ⚠️ Scope decision — account scope only, not origin scope

> **Operators: the proxy requirements live in
> [`docs/operations/installation-and-administration.md`](../operations/installation-and-administration.md) §1.**
> Read it before you enable `server.forward-headers-strategy`, and read §1.8 for the residual risk
> this section describes below.

`AUTH-NFR-03` asks for throttling *per account and per request origin*. **Only account scope ships.**
Origin scope is deferred because the application cannot currently determine a client address:

- the app runs behind HAProxy, which terminates TLS;
- `server.forward-headers-strategy` is unset, no `RemoteIpValve` is configured, and nothing reads
  `getRemoteAddr()` or `X-Forwarded-For`;
- therefore `getRemoteAddr()` returns the **proxy**.

Keying a bucket on that address would put every user of the deployment into one row, and a single
anonymous attacker filling it would block the entire user population — worse than the gap it closes.
Doing it correctly needs a trusted-proxy allowlist plus HAProxy **overwriting** rather than appending
`X-Forwarded-For`. That also unblocks passkeys and the ORCID `{baseUrl}` redirect — see
[the milestone plan](../plans/MILESTONE-MFA-self-governed-2fa.md) §1a condition 1. It needs **no
schema change**: `scope` is an open `varchar` and `attemptKey` is an opaque hash.

**Residual risk accepted:** account-scope throttling stops credential stuffing against a known target
and stops mail-bombing one inbox. It does **not** stop one source sending one reset mail to many
different addresses — volume abuse, mitigated by the SMTP relay's own limits, and not account
takeover. It also does not prevent an attacker locking a *specific known victim* out for the block
duration; that is the cost of not having origin scope yet, and it is why the block is temporary.

### Apply

```sql
-- 1. Verify the table does not exist yet (should return 0 rows).
SHOW TABLES LIKE 'auth_attempt';

-- 2. Apply.
SOURCE sql/migrations/auth-attempt-throttling.sql;

-- 3. Verify.
SHOW CREATE TABLE auth_attempt\G
-- Expect: PRIMARY KEY (id), UNIQUE KEY uk_auth_attempt_scope_key (scope, attemptKey),
--         KEY idx_auth_attempt_last (lastAttemptAt), NO foreign keys.
```

**Apply before deploying the application code.** The code queries the table on the first login
attempt; without it, every login fails with a SQL error.

### Verify after deployment

```sql
-- Buckets exist and are being used (should grow after failed logins).
SELECT scope, COUNT(*) AS buckets, SUM(attemptCount) AS attempts FROM auth_attempt GROUP BY scope;

-- Rows currently blocked.
SELECT scope, attemptCount, blockedUntil FROM auth_attempt
 WHERE blockedUntil IS NOT NULL AND blockedUntil > UTC_TIMESTAMP(6);
```

Functional check: fail a login 11 times (default limit is 10) and confirm the 11th shows the same
"Incorrect username or password" message as the first — **not** a "too many attempts" screen. Then
wait for the block to expire and confirm login works again.

### Rollback

```sql
DROP TABLE IF EXISTS auth_attempt;
```

Safe. This table holds no audit trail — it is a rate-limiting counter whose contents are meaningless
without the code that reads it. Dropping it loses only the current block state, so rolling back
re-enables unthrottled attempts, which is the pre-change behaviour rather than a data loss. No other
table references it and it references nothing.

### Retention (not implemented — deliberate)

Rows are not pruned yet. `lastAttemptAt` is indexed so a scheduled sweep can call
`JpaAuthAttemptCounter.purgeOlderThan(Instant)`. The purge is **deliberately not scheduled** in this
change: retention is a compliance decision owned by
[#1669](https://github.com/qbicsoftware/data-manager-app/issues/1669). Until it lands the table grows
without bound — it is small (one row per address that has attempted a login or a resend within
retention), but it is unbounded, and that is recorded here rather than left to be discovered.

### A deadlock that only appears on MariaDB

Recorded because it will be hit again by anyone simplifying this code. The first implementation reset
an elapsed window with a separate `UPDATE … WHERE windowStartAt < ?` before the upsert. On real
InnoDB, 16 concurrent attempts on one bucket deadlock:

```
Deadlock found when trying to get lock; try restarting transaction
INSERT INTO auth_attempt ...
```

The `UPDATE` takes locks across the unique-key range and the `INSERT` then needs an insert-intention
lock on the same range. **It does not reproduce on H2** — only on MariaDB. Fixed by folding the window
roll-over into the single `INSERT … ON DUPLICATE KEY UPDATE` statement (assignment order matters:
MySQL evaluates them left to right). This is the same failure the verification-token reissue hit
(see [`../plans/spike-verification-token-flow.md`](../plans/spike-verification-token-flow.md),
Finding 6).

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
