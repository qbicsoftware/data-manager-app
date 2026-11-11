-- =============================================================================
-- Migration: Single-use verification tokens
-- Story:    FEAT-AUTH-FLOWS-01 (#1678), FEAT-AUTH-FLOWS-02 (#1680),
--           FEAT-AUTH-FLOWS-05 (#1683)
-- Feature:  FEAT-AUTH-FLOWS (#1672)
-- ADRs:     none — no architectural decision is needed for this change; the
--           design rationale lives in this header and in the PR description
-- Datasource: data_management
--
-- Status:   READY FOR §12 SIGN-OFF — DO NOT APPLY YET.
--           Requires (a) schema-change approval per AGENTS.md §12 and
--           (b) the promotion step described under "Promotion" below.
--
-- Purpose: replace the identifier-in-URL confirmation and password-reset links
-- with single-use, time-limited tokens (AUTH-R-05).
--
-- Scope note: this script creates ONE table. The per-account throttling that
-- AUTH-NFR-03 also asks for is NOT part of this change — it is issue #1682, and
-- shipping its table before its code would leave an unused schema object behind.
--
-- Design notes — read before changing anything:
--
--  * Only a HASH of the token is stored. The raw token exists once, in the
--    outgoing email, and can never be recovered from the database. No admin
--    (and no support script) can read a link back; the only remedy for a lost
--    link is to issue a new one, which AUTH-R-05 already requires.
--
--  * The hash is a plain SHA-256 hex digest, NOT
--    `PersonalAccessTokenEncoder`. That encoder derives its key from a single
--    deployment-wide salt (`qbic.access-token.salt`) supplied as an env var.
--    Consumption here is a `WHERE tokenHash = ?` lookup, so rotating that salt
--    would silently invalidate every link in flight. PBKDF2 stretching is also
--    pointless on a 256-bit `SecureRandom` value — stretching exists to make
--    low-entropy user-chosen secrets expensive to guess. `char(64)` matches
--    the existing `profile_picture.content_hash` convention in this schema.
--
--  * `purpose` distinguishes flows so one token can never cross over into
--    another (a confirmation token must not reset a password). Closed set:
--    EMAIL_CONFIRMATION | PASSWORD_RESET | SET_PASSWORD. `SET_PASSWORD` serves
--    the one-time invitation issued to ORCID-only accounts (#1683); it sets the
--    password and confirms the address in one step, because those accounts are
--    `active = FALSE` and cannot log in otherwise.
--
--  * Superseding: issuing a new token for the same (userId, purpose) invalidates
--    the previous one, so "request a new link" cannot be used to keep old links
--    alive.
--
--  * Three distinct end-states, three distinct columns. `consumedAt` = used,
--    `supersededAt` = replaced by a newer token for the same purpose,
--    `expiresAt` = expired. Collapsing these into one column makes "was this
--    link already used?" unanswerable during support.
--
--  * `expiresAt` MUST be compared in Java against `Instant.now()`, never in SQL
--    against `NOW()`. Every column here is `datetime(6)` with no timezone, so a
--    SQL-side comparison silently inherits the database session's zone while
--    the application holds an `Instant`. Compare in one clock, consistently.
--
--  * Rows are retained after consumption for audit and purged by a scheduled
--    job. The retention period is a compliance decision owned by the security
--    and privacy note (#1669), not by this script.
--
--  * `ON DELETE CASCADE` to `users` is the first incoming foreign key to that
--    table in this schema (none existed before). There is no user-deletion code
--    path today, so this is inert; it exists so orphaned tokens cannot survive
--    a future account removal.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. user_verification_token: email confirmation, password reset, set-password
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user_verification_token`
(
    `id`           bigint(20)   NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `userId`       varchar(255) NOT NULL,
    `purpose`      varchar(32)  NOT NULL
        COMMENT 'EMAIL_CONFIRMATION | PASSWORD_RESET | SET_PASSWORD',
    `tokenHash`    char(64)     NOT NULL
        COMMENT 'Lower-case SHA-256 hex digest of a 32-byte SecureRandom token (Base64URL-encoded in the link). The raw token is never stored.',
    `createdAt`    datetime(6)  NOT NULL,
    `expiresAt`    datetime(6)  NOT NULL,
    `consumedAt`   datetime(6)  DEFAULT NULL
        COMMENT 'Set on first successful use. Non-NULL makes the token unusable.',
    `supersededAt` datetime(6)  DEFAULT NULL
        COMMENT 'Set when a newer token was issued for the same (userId, purpose). Non-NULL makes the token unusable.',

    -- One token may never be reused; the unique hash guarantees that even under
    -- a race between two concurrent consumers.
    UNIQUE KEY `uk_user_verification_token_hash` (`tokenHash`),
    KEY `idx_user_verification_token_user_purpose` (`userId`, `purpose`),
    KEY `idx_user_verification_token_expiry` (`expiresAt`),
    CONSTRAINT `FK_user_verification_token_user`
        FOREIGN KEY (`userId`) REFERENCES `users` (`id`)
            ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- =============================================================================
-- Rollback
-- =============================================================================
-- DROP TABLE IF EXISTS `user_verification_token`;
--
-- WARNING 1 — links in flight. Rollback invalidates every unconsumed
-- confirmation or reset link: users holding one must request a new one. A
-- rollback after go-live therefore needs the operator note from the migration
-- guide, not a silent drop.
--
-- WARNING 2 — audit trail. `user_verification_token` is the only record of which
-- links were issued, consumed, superseded or expired. Dropping it destroys that
-- evidence permanently. Before rolling back, export the table:
--
--   SELECT id, userId, purpose, createdAt, expiresAt, consumedAt, supersededAt
--     INTO OUTFILE '/tmp/user_verification_token-<date>.csv'
--     FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"'
--      LINES TERMINATED BY '\n'
--      FROM user_verification_token;
--
-- (`tokenHash` is deliberately not exported: it is not needed for audit and it
-- is the one column that must not travel.)
--
-- NOTE on decision B1 (no compatibility window): links already delivered keep
-- the OLD format (/login?confirm-email={userId}), which the new code no longer
-- accepts. They stop working by design. The operator note must say so
-- explicitly, and the support script is "request a new link".
-- =============================================================================

-- =============================================================================
-- Promotion (do this only once §12 sign-off is recorded)
-- =============================================================================
-- 1. Move this file to `sql/migrations/auth-flow-hardening.sql`.
-- 2. Add both CREATE TABLE blocks to `sql/complete-schema.sql` — that file must
--    always reflect the applied state (see sql/migrations/README.md).
-- 3. Add a row to the summary table in `docs/migrations/README.md`.
-- 4. Complete the operator runbook in `docs/migrations/NEXT.md` (the ORCID
--    password-backfill procedure lives there, not here — it contains no DDL).
-- 5. Remove the §12 status line from the header above.
-- =============================================================================
