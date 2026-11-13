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

> *Numbering is sequential within the release and determines apply order. This is a
> **security patch release**: it carries the two auth-flow tables and nothing else.*

| # | Script | Description | Risk |
|---|---|---|---|
| 1 | [`auth-flow-hardening.sql`](../../sql/migrations/auth-flow-hardening.sql) | Create `user_verification_token`: single-use, hashed, time-limited tokens behind email confirmation, password reset and the set-password invitation (`AUTH-R-05`). One new table; no existing table altered. | Medium — one new table, **and links already in flight stop working by design** (decision B1, see the operator note below) |
| 2 | [`auth-attempt-throttling.sql`](../../sql/migrations/auth-attempt-throttling.sql) | Create `auth_attempt`: server-side throttling for login, confirmation-resend and reset-resend (`AUTH-NFR-03`). One new table, no existing table altered, no foreign key. Replaces the client-side 30 s timer, which a page reload reset. | Low (additive) — but a **behaviour change**: repeated attempts are now blocked server-side |

**Apply in this order, and before deploying the application code.** Neither table exists on a 1.20.x
installation, and both are queried on the first request that uses them: without `user_verification_token`
no link can be issued or consumed (the failure is silent — registration appears to succeed and no link
works), and without `auth_attempt` **login fails outright**.

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

## Migration #1: Auth-flow hardening — verification tokens

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
| **Script** | [`sql/migrations/auth-flow-hardening.sql`](../../sql/migrations/auth-flow-hardening.sql) |
| **Target datasource** | `data_management` |

### What it does

Two tables, both additive:

- **`user_verification_token`** — single-use, time-limited tokens for email confirmation
  (`EMAIL_CONFIRMATION`), password reset (`PASSWORD_RESET`) and the one-time set-password invitation
  for ORCID-only accounts (`SET_PASSWORD`). Only a **SHA-256 hash** is stored; the raw token exists
  once, in the outgoing email, and cannot be recovered from the database. Three nullable timestamps
  record how a token stopped being usable: `consumedAt` (used), `supersededAt` (replaced when a
  newer token was issued for the same purpose), or expiry via `expiresAt`.
### Scope decision recorded here — throttling is NOT in this release

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
-- Both tables exist with the expected shape.
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

**Do not drop the table to revert this release.** Both new tables are additive and inert without the
code that reads them, so the correct revert is: redeploy the previous application version and **leave the
schema in place**. Nothing else in the 1.20.x schema references them, and no query breaks because they
exist.

Dropping `user_verification_token` is a last resort, and it costs more than it restores:

```sql
-- LAST RESORT ONLY. Read both warnings first.
DROP TABLE IF EXISTS `user_verification_token`;
```

1. **It invalidates every unconsumed link**, including the ones the reverted code cannot reissue — the
   old identifier-based links are gone from the code and are not restored by this. Users must request new
   links, and on the reverted version they cannot.
2. **It destroys the audit trail.** This table is the only record of which links were issued, consumed,
   superseded or expired. Export before dropping — the statement is in the script header.

There is no state, at any point, in which the pre-change links work again. Reverting the code restores the
old behaviour for *new* links only.

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

## Migration #2: Server-side throttling for the auth flows

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
| **Script** | [`sql/migrations/auth-attempt-throttling.sql`](../../sql/migrations/auth-attempt-throttling.sql) |
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

**Do not drop the table to revert this release.** Redeploy the previous application version and leave the
schema in place: the table is inert without the code, and the reverted code never queries it.

```sql
-- LAST RESORT ONLY: the reverted code does not read this table, so there is no reason to drop it.
DROP TABLE IF EXISTS auth_attempt;
```

Should you drop it anyway: it holds no audit trail — it is a rate-limiting counter whose contents are meaningless
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
