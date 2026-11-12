-- =============================================================================
-- Migration: Server-side throttling for unauthenticated auth flows
-- Story:    FEAT-AUTH-FLOWS-03 (#1675), Task #1682
-- Feature:  FEAT-AUTH-FLOWS (#1672)
-- ADRs:     none — no architectural decision is recorded; the rationale below is
--           the decision record for this change
-- Datasource: data_management
--
-- Status:   AWAITING AGENTS.md §12 SIGN-OFF.
--           This creates a new table. Per §12, a schema change must be approved by
--           a human reviewer before it is applied anywhere but a throwaway database.
--           Until sign-off is recorded, treat this file as a reviewable draft: it is
--           safe to apply to a scratch instance and has been, but the operator
--           runbook in docs/migrations/NEXT.md must not be followed for production.
--
-- Purpose: implement the throttling clause of AUTH-NFR-03 — "repeated attempts
-- against these flows shall be throttled per account and per request origin".
--
-- Scope: ONE table. It serves three flows now (LOGIN,
-- EMAIL_CONFIRMATION_RESEND, PASSWORD_RESET_RESEND) and is deliberately designed
-- to serve second-factor throttling later (MILESTONE-MFA M5-4) without a second
-- table and without a schema change: `scope` is an open varchar and `attemptKey`
-- is an opaque hash.
--
-- Design notes — read before changing anything. Three of these are load-bearing
-- and were established empirically, not by preference:
--
--  * `attemptKey` is a HASH, never a raw address, and there is deliberately NO
--    FOREIGN KEY to `users`.
--
--    An attempt against an address that does not exist MUST be counted exactly
--    like an attempt against one that does. With an FK, the unknown address could
--    not be recorded at all, so the mere presence or absence of throttling would
--    answer "does this account exist?" — the enumeration oracle AUTH-NFR-03
--    forbids. The same reasoning rules out a lockout keyed on a real user row.
--
--    The key material is `scope \0 normalised-address` (SHA-256). Normalisation
--    is mandatory: `EmailAddress` performs no case folding or stripping, so
--    without it `Alice@x.de`, `alice@x.de` and `alice@x.de ` would be three
--    buckets pointing at one account, and varying capitalisation would grant
--    unlimited reset mail per victim.
--
--  * The counter increment MUST be an upsert, not read-then-write.
--
--    Two concurrent attempts both read `attemptCount = 3` and both write `4`, so
--    an attacker gains a free attempt per race. The code does
--    `INSERT … ON DUPLICATE KEY UPDATE attemptCount = attemptCount + 1` in a
--    single statement, which is why `(scope, attemptKey)` is a UNIQUE KEY and not
--    merely an index.
--
--    PORTABILITY: `ON DUPLICATE KEY UPDATE` is MariaDB/MySQL syntax. H2 accepts it
--    only with `MODE=MySQL`; default H2 rejects it. The throttling specs therefore
--    run H2 in MySQL mode, or they would pass against an upsert production never
--    executes.
--
--  * The window length is STORED PER ROW (`windowSeconds`), and staleness is
--    evaluated as `windowStartAt + windowSeconds <= now`.
--
--    The obvious design is to let the caller pass `windowStart = now - window`
--    and reset whenever `windowStartAt < windowStart`. That is a SLIDING
--    comparison and it is broken: every call computes a later boundary, so the
--    stored start is always earlier than it and every attempt is treated as a
--    fresh window. The counter then never exceeds 1, the threshold is never
--    reached, and the throttle silently never engages — measured at
--    [1, 1, 1, 1, 1] over five consecutive attempts. Storing the length per row
--    makes the window fixed and self-contained.
--
--    Consequence for operators: changing `qbic.auth-throttle.*.window` takes
--    effect on the next attempt (the upsert rewrites `windowSeconds`) but does
--    not retroactively re-window an in-flight row's count.
--
--  * `blockedUntil` is stored, but the "is this blocked now?" decision is made in
--    Java against an `Instant`, never in SQL against `NOW()`.
--
--    Every column is `datetime(6)` with no timezone. A SQL-side comparison would
--    silently inherit the database session's zone while the application holds an
--    Instant, so the two would disagree by the offset. Same rule as
--    `user_verification_token.expiresAt`.
--
--  * SCOPE OF THIS RELEASE — account scope only.
--
--    AUTH-NFR-03 asks for throttling per account AND per request origin. Only
--    account scope ships, because the application cannot currently determine a
--    client address:
--      - the app runs behind HAProxy, which terminates TLS;
--      - `server.forward-headers-strategy` is not set, no RemoteIpValve is
--        configured, and nothing in the codebase reads `getRemoteAddr()` or
--        `X-Forwarded-For`;
--      - therefore `getRemoteAddr()` returns the PROXY.
--
--    Keying an origin bucket on that address would put every user of the
--    deployment into a single row, and one anonymous attacker filling it would
--    block the entire user population — a self-inflicted global denial of service,
--    strictly worse than the gap this table closes.
--
--    Doing it correctly needs trusted forwarded headers: a trusted-proxy allowlist,
--    plus HAProxy OVERWRITING rather than appending `X-Forwarded-For` (the header
--    is client-supplied; trusting the leftmost entry lets an attacker mint a fresh
--    bucket per request). That work is tracked separately and also unblocks
--    passkeys and the ORCID `{baseUrl}` redirect — see
--    docs/plans/MILESTONE-MFA-self-governed-2fa.md §1a condition 1.
--
--    Adding origin scope later needs NO schema change: insert a second row with
--    a different `scope` value and an origin-derived key.
--
--  * Residual risk accepted with account scope alone: this stops credential
--    stuffing against a known target, and stops mail-bombing one inbox. It does
--    NOT stop one source sending one reset mail to many different addresses —
--    that is volume/cost abuse, mitigated by the SMTP relay's own rate limits,
--    and it is not account takeover.
--
--  * Rows are pruned, not retained forever. `lastAttemptAt` is indexed for the
--    purge sweep. See the retention note in docs/migrations/NEXT.md: the purge is
--    deliberately NOT implemented in this change (retention is a compliance
--    decision owned by #1669), so the table will grow until that lands.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- auth_attempt: throttling for the unauthenticated auth flows
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `auth_attempt`
(
    `id`            bigint(20)  NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `scope`         varchar(32) NOT NULL
        COMMENT 'LOGIN | EMAIL_CONFIRMATION_RESEND | PASSWORD_RESET_RESEND (open set: added without DDL)',
    `attemptKey`    char(64)    NOT NULL
        COMMENT 'Lower-case SHA-256 hex digest of scope NUL normalised-identifier. Never a raw address.',
    `attemptCount`  int(11)     NOT NULL DEFAULT 0
        COMMENT 'Attempts in the current window. Incremented by the DATABASE (upsert), never read-then-write.',
    `windowStartAt` datetime(6) NOT NULL
        COMMENT 'Start of the counting window, written in UTC.',
    `windowSeconds` int(11)     NOT NULL DEFAULT 900
        COMMENT 'Length of the counting window, stored per row. The adapter compares the row''s OWN window end (windowStartAt + windowSeconds) against now. An earlier design passed now-minus-window as a boundary; because each call computed a later boundary the stored start was always earlier, so every attempt reset the count and the counter could never exceed 1.',
    `blockedUntil`  datetime(6) DEFAULT NULL
        COMMENT 'Set when the threshold is crossed. Evaluated in Java against Instant.now().',
    `lastAttemptAt` datetime(6) NOT NULL
        COMMENT 'Most recent attempt; drives the purge sweep.',

    -- UNIQUE, not just an index: this is what makes the upsert atomic. A plain
    -- index would allow duplicate rows per (scope, key) and silently split the count.
    UNIQUE KEY `uk_auth_attempt_scope_key` (`scope`, `attemptKey`),
    KEY `idx_auth_attempt_last` (`lastAttemptAt`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- =============================================================================
-- Rollback
-- =============================================================================
-- DROP TABLE IF EXISTS `auth_attempt`;
--
-- Safe to roll back. This table holds no audit trail: it is a rate-limiting
-- counter whose contents are meaningless once the code that reads it is gone.
-- Dropping it loses only the current block state, so rolling back re-enables
-- unthrottled attempts — which is the pre-change behaviour, not a data loss.
-- No other table references it and it references nothing.
-- =============================================================================

-- =============================================================================
-- Promotion checklist (complete these before the release cut)
-- =============================================================================
-- 1. [ ] Record AGENTS.md §12 sign-off for the new table in the PR description.
-- 2. [ ] Add the CREATE TABLE block to `sql/complete-schema.sql`, so a fresh
--     install matches an upgraded one.
-- 3. [ ] Add/update the row in the summary table in `docs/migrations/README.md`.
-- 4. [ ] Complete the operator runbook section in `docs/migrations/NEXT.md`,
--     including the "throttling is now active" note and the threshold properties.
-- 5. [ ] Remove the AWAITING status line from the header above.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Idempotent upgrade of an EXISTING auth_attempt table
-- -----------------------------------------------------------------------------
-- `CREATE TABLE IF NOT EXISTS` above is a NO-OP when the table already exists. That
-- matters here because this script is applied more than once across its life:
--
--   * the table was created during testing, and
--   * `windowSeconds` was added afterwards, when the sliding-window bug was fixed.
--
-- Without the statements below, re-applying the script against an existing table
-- silently leaves the old shape in place — no error, no warning, and the application
-- then fails at runtime with "Unknown column 'windowSeconds'". That is exactly what
-- happened on the test server.
--
-- MariaDB/MySQL have no `ADD COLUMN IF NOT EXISTS`, so each column is added only when
-- it is actually missing, via a prepared statement over information_schema.
-- -----------------------------------------------------------------------------

SET @dbname = DATABASE();

-- `windowSeconds`: added when the fixed-window fix landed.
SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE `auth_attempt`
           ADD COLUMN `windowSeconds` int(11) NOT NULL DEFAULT 900
           COMMENT ''Length of the counting window, stored per row. The adapter compares the row''''s own window end (windowStartAt + windowSeconds) against now.''',
        'DO 0'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @dbname
      AND TABLE_NAME = 'auth_attempt'
      AND COLUMN_NAME = 'windowSeconds'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Defensive: a row written by the pre-fix code has windowSeconds backfilled by the
-- DEFAULT above, so no data fix is needed. Clearing any stale block is also not
-- necessary — the adapter clears it when the window rolls over.
