-- =============================================================================
-- Migration: Second-factor storage, recovery codes and the enforcement grace window
-- Story:    FEAT-MFA-ENRL-01 / FEAT-MFA-LOGIN-01 (MILESTONE-MFA M2-1)
-- Feature:  https://github.com/qbicsoftware/data-manager-app/milestone/1
-- ADRs:     0011 (MFA authentication authority: the application owns the second factor)
-- Datasource: data_management
--
-- Status:   DRAFT — staging/test only. NOT approved for production.
--           Runbook, provisioning and verification: docs/migrations/NEXT.md §"Migration 1".
--           Production requires the AGENTS.md §12 sign-off (schema change) and the M2 PR
--           review; the ADR is accepted, the DDL is not yet. When it is: move the file to
--           `sql/migrations/`, add the index row, and complete the checklist below.
--
-- Purpose: persist self-governed second factors for the `identity` bounded context, the
-- recovery codes that make a lost device survivable, and the per-account grace window that
-- makes the `required` policy safe to switch on for a running service.
--
-- ADR 0011 decides the shape; three of its consequences are visible in the DDL and must not
-- be "simplified" later:
--
--  * There is NO `users.mfaPolicyOverride` column. An override value can express
--    "not required for this account", which is a permanent, silent exemption — the one thing
--    AUTH-R-01 and the ADR rule out. Posture is configuration (off/optional/required) and
--    `user_mfa_grace` carries the only per-account deviation, which expires by itself.
--    A future *tightening-only* admin feature (ADR 0011 item 6) may add a column; it may
--    never add a "NOT_REQUIRED" value.
--
--  * There is NO second throttling table. `auth_attempt` (sql/migrations/auth-attempt-throttling.sql)
--    was built with an open `scope` value and a hashed, FK-free key precisely so
--    second-factor failures reuse it (M5-4). A `userId`-keyed counter cannot even record an
--    attempt against an unknown address, which would turn the throttle into the enumeration
--    oracle AUTH-NFR-03 forbids.
--
--  * TOTP seeds are NOT hashed. A seed has to be recovered to compute the expected code, so
--    a one-way hash destroys the factor. `secretCiphertext` holds AES-256-GCM ciphertext under
--    a master key that lives in the PKCS12 vault under its OWN alias
--    (`qbic.security.vault.second-factor.key-alias`), never the external-credential key: factor
--    material must be rotatable and restorable independently of other secrets (ADR 0011 F1,
--    ADR-0002 S2). Recovery codes are the opposite case and are hashed, deliberately outside the
--    vault-key path, so a lost master key degrades to recovery + admin re-enrolment instead of a
--    total lockout (ADR 0011 B1, milestone risk R8).
--
-- The grace window — the four invariants behind `user_mfa_grace` (ADR 0011 item 3):
--
--  1. A MISSING row means "grant a window", never "deny". The application writes
--     `now + grace` on the first login of an unenrolled account under `required`, so switching
--     the policy on cannot lock out a population that has not been given a deadline. Reading the
--     absence as "no grace" is the lockout bug: it denies everyone at once, at the moment of a
--     configuration change, on a running service.
--  2. The clock starts per account at first login under `required`, not at deployment time, so a
--     user returning from three weeks away still gets a full window.
--  3. Grace buys enrolment only; it is never extended by use, so it cannot decay into an
--     exemption.
--  4. It expires by comparison against an `Instant` in Java, never against SQL `NOW()` — every
--     column is `datetime(6)` with no timezone, so a SQL-side comparison would inherit the
--     database session's zone and disagree with the application by the offset.
--
-- WHY THE FOREIGN KEYS ARE ADDED AT THE END, NOT INLINE
-- ----------------------------------------------------
-- `users.id` is `varchar(255)`, but its *collation* is a property of the database this script
-- runs against, not of this repository. InnoDB refuses a foreign key whose columns do not have
-- exactly the same charset **and** collation, and it reports that refusal as
-- "errno: 150 Foreign key constraint is incorrectly formed" — indistinguishable from a missing
-- parent table. Pinning a collation here is therefore only correct on the database this was
-- written against; doing that was the first version of this script and it failed on the first
-- real deployment.
--
-- So the script does what an operator would do by hand: create the tables, read the parent
-- column's charset/collation from `information_schema`, align the three `userId` columns to it,
-- and only then add the constraints. It is idempotent, it tells you when it skipped the keys and
-- why, and re-running it after fixing the cause completes the job.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 0. Preflight: the parent table must be in *this* schema
-- -----------------------------------------------------------------------------
-- Reads as a result row rather than failing: the tables themselves are harmless without their
-- account rows, and an operator who connected to the wrong database should be told so in words,
-- not with errno 150 at the first CREATE.
SET @users_present = (
    SELECT COUNT(*)
      FROM information_schema.tables
     WHERE table_schema = DATABASE()
       AND table_name = 'users');

SET @users_type = (
    SELECT column_type
      FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'users'
       AND column_name = 'id');

SET @users_charset = (
    SELECT character_set_name
      FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'users'
       AND column_name = 'id');

SET @users_collation = (
    SELECT collation_name
      FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'users'
       AND column_name = 'id');

-- 1 = the parent is usable for a foreign key.
SET @fk_possible = IF(@users_present = 1 AND @users_type = 'varchar(255)', 1, 0);

SELECT IF(@users_present = 0,
          'WARNING: no `users` table in this schema. Tables are created, foreign keys are SKIPPED. Connect to the data-management database (or apply the base schema), then re-run this script.',
          IF(@fk_possible = 0,
             CONCAT('WARNING: users.id is ', COALESCE(@users_type, 'missing'),
                    ' but these tables use varchar(255) — foreign keys are SKIPPED.'),
             CONCAT('Preflight OK: users.id is ', @users_type, ' CHARACTER SET ',
                    @users_charset, ' COLLATE ', @users_collation,
                    ' — foreign keys are added after column alignment.'))
       ) AS mfa_preflight;

-- The data-management database is identifiable by its project tables. `users` also lives in
-- the identity/user-management schema, so "does users exist here" alone cannot tell an operator
-- that they picked the right database — this caught a real wrong-schema apply.
SET @projects_present = (
    SELECT COUNT(*)
      FROM information_schema.tables
     WHERE table_schema = DATABASE()
       AND table_name = 'projects_datamanager');

SELECT IF(@users_present = 1 AND @projects_present = 0,
          'WARNING: this schema has `users` but no `projects_datamanager`. The second-factor tables belong in the data-management database — check which schema your client is connected to (the application names it in DATAMANAGEMENT_DB_URL) before continuing.',
          'Schema check OK: data-management tables present')
       AS mfa_schema_check;

-- -----------------------------------------------------------------------------
-- 1. user_second_factor: one row per enrolled credential
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user_second_factor`
(
    `id`                 bigint(20)   NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `userId`             varchar(255) NOT NULL,
    `type`               varchar(16)  NOT NULL
        COMMENT 'TOTP | WEBAUTHN (open set: a new type is added without DDL)',

    -- Exactly one of the next two columns is populated. Enforced by CHECK below as well,
    -- because a row that describes neither a TOTP nor a WebAuthn credential is not a factor.
    `secretCiphertext`   text
        COMMENT 'TOTP only. Base64(nonce||ciphertext||GCM tag), AES-256-GCM under the second-factor master key in the PKCS12 vault. Never a raw seed and never a PAT-style hash — the seed must be recoverable to verify a code.',
    `webauthnCredential` text
        COMMENT 'WEBAUTHN only. Serialised credential (credentialId, public key, signature counter, transports, AAGUID, backup flags). Public key material only — there is no server-side secret to leak.',

    `displayName`        varchar(255) NOT NULL
        COMMENT 'User-facing label, e.g. "iPhone (passkey)" or "YubiKey 5". Not unique: rows are soft-deleted, so a removed factor must be re-addable under the same name.',
    `createdAt`          datetime(6)  NOT NULL,
    `lastUsedAt`         datetime(6)  DEFAULT NULL,
    `lastAcceptedStep`   bigint(20)   DEFAULT NULL
        COMMENT 'Highest RFC 6238 time step this factor has accepted. Replay protection (MFA-20): a code is valid for 30s and tolerated +/-1 step, so an already-seen code stays usable for 90s unless the step it matched is refused once it is not newer than this. A step counter, not a timestamp — every datetime column here has no zone.',
    `disabledAt`         datetime(6)  DEFAULT NULL
        COMMENT 'Soft delete. A removed factor is never hard-deleted, so the audit trail (AUTH-R-04) keeps showing when it existed and when it stopped.',

    KEY `idx_user_second_factor_user` (`userId`, `disabledAt`),
    CONSTRAINT `chk_user_second_factor_shape`
        CHECK ((`type` = 'TOTP' AND `secretCiphertext` IS NOT NULL AND `webauthnCredential` IS NULL)
            OR (`type` = 'WEBAUTHN' AND `webauthnCredential` IS NOT NULL AND
                `secretCiphertext` IS NULL))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- Upgrading a database that already ran an earlier draft of this file (which lacked the
-- replay-protection column) needs only this one statement:
--
--   ALTER TABLE `user_second_factor` ADD COLUMN `lastAcceptedStep` bigint(20) DEFAULT NULL
--       COMMENT 'Highest RFC 6238 time step accepted by this factor; replay protection (MFA-20)';
--
-- Existing rows are NULL, which means "no code accepted yet" — correct, and it does not need a
-- backfill: the mark only has to be right from the next verification onwards.

-- -----------------------------------------------------------------------------
-- 2. user_recovery_code: single-use break-glass codes (hashed, never plaintext)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user_recovery_code`
(
    `id`            bigint(20)   NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `userId`        varchar(255) NOT NULL,
    `codeHash`      varchar(255) NOT NULL
        COMMENT 'Keyed HMAC-SHA-256 hex digest of a high-entropy code. Deterministic on purpose: the login path looks a code up by hash without knowing the account. Deliberately NOT encrypted with the vault master key — these must still verify when that key is lost (ADR 0011 B1).',
    `createdAt`     datetime(6)  NOT NULL,
    `usedAt`        datetime(6)  DEFAULT NULL
        COMMENT 'Single use. Set inside the consuming transaction.',
    `invalidatedAt` datetime(6)  DEFAULT NULL
        COMMENT 'Set on regeneration: issuing a new set retires every unused code of the previous set (milestone M3-4). A code is usable only while usedAt and invalidatedAt are both NULL.',

    -- Globally unique, not per user: the recovery login path resolves a code without knowing
    -- the account (that is the point — the account is what the user is trying to reach), so the
    -- hash is the lookup key and two users must not share one.
    UNIQUE KEY `uk_user_recovery_code_hash` (`codeHash`),
    KEY `idx_user_recovery_code_user` (`userId`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- -----------------------------------------------------------------------------
-- 3. user_mfa_grace: the per-account window that makes a policy flip safe
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user_mfa_grace`
(
    `userId`     varchar(255) NOT NULL PRIMARY KEY,
    `startedAt`  datetime(6)  NOT NULL
        COMMENT 'First login of this account under `required` with no factor enrolled. The window is per account, not per deployment.',
    `graceUntil` datetime(6)  NOT NULL
        COMMENT 'Absolute deadline (startedAt + configured grace). Absence of this row means "grant a window now", never "deny".',
    `reminderDueSentAt`   datetime(6) NULL
        COMMENT 'When the "one day left" reminder was sent. Stored so the reminder is idempotent across restarts and instances.',
    `reminderFinalSentAt` datetime(6) NULL
        COMMENT 'When the final "hours left" reminder was sent.'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- -----------------------------------------------------------------------------
-- 4. Align the three `userId` columns with `users.id`
-- -----------------------------------------------------------------------------
-- Only runs when the parent is usable. A no-op ALTER (`DO 0`) keeps the block idempotent and
-- keeps the script runnable through clients that reject conditionals.
SET @align_factor = IF(@fk_possible = 1,
    CONCAT('ALTER TABLE `user_second_factor` MODIFY `userId` varchar(255) CHARACTER SET ',
           @users_charset, ' COLLATE ', @users_collation, ' NOT NULL'),
    'DO 0');
PREPARE stmt FROM @align_factor;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @align_recovery = IF(@fk_possible = 1,
    CONCAT('ALTER TABLE `user_recovery_code` MODIFY `userId` varchar(255) CHARACTER SET ',
           @users_charset, ' COLLATE ', @users_collation, ' NOT NULL'),
    'DO 0');
PREPARE stmt FROM @align_recovery;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @align_grace = IF(@fk_possible = 1,
    CONCAT('ALTER TABLE `user_mfa_grace` MODIFY `userId` varchar(255) CHARACTER SET ',
           @users_charset, ' COLLATE ', @users_collation, ' NOT NULL'),
    'DO 0');
PREPARE stmt FROM @align_grace;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------------------------
-- 5. Add the foreign keys, once, when the parent allows it
-- -----------------------------------------------------------------------------
SET @fk_factor = IF(@fk_possible = 1
    AND (SELECT COUNT(*) FROM information_schema.table_constraints
          WHERE constraint_schema = DATABASE()
            AND table_name = 'user_second_factor'
            AND constraint_name = 'FK_user_second_factor_user') = 0,
    'ALTER TABLE `user_second_factor` ADD CONSTRAINT `FK_user_second_factor_user` FOREIGN KEY (`userId`) REFERENCES `users` (`id`) ON DELETE CASCADE',
    'DO 0');
PREPARE stmt FROM @fk_factor;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @fk_recovery = IF(@fk_possible = 1
    AND (SELECT COUNT(*) FROM information_schema.table_constraints
          WHERE constraint_schema = DATABASE()
            AND table_name = 'user_recovery_code'
            AND constraint_name = 'FK_user_recovery_code_user') = 0,
    'ALTER TABLE `user_recovery_code` ADD CONSTRAINT `FK_user_recovery_code_user` FOREIGN KEY (`userId`) REFERENCES `users` (`id`) ON DELETE CASCADE',
    'DO 0');
PREPARE stmt FROM @fk_recovery;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @fk_grace = IF(@fk_possible = 1
    AND (SELECT COUNT(*) FROM information_schema.table_constraints
          WHERE constraint_schema = DATABASE()
            AND table_name = 'user_mfa_grace'
            AND constraint_name = 'FK_user_mfa_grace_user') = 0,
    'ALTER TABLE `user_mfa_grace` ADD CONSTRAINT `FK_user_mfa_grace_user` FOREIGN KEY (`userId`) REFERENCES `users` (`id`) ON DELETE CASCADE',
    'DO 0');
PREPARE stmt FROM @fk_grace;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- =============================================================================
-- Rollback
-- =============================================================================
-- DROP TABLE IF EXISTS `user_mfa_grace`;
-- DROP TABLE IF EXISTS `user_recovery_code`;
-- DROP TABLE IF EXISTS `user_second_factor`;
--
-- Safe while nothing is enrolled: the tables are empty and no other table references them.
-- Destructive once factors exist — every enrolled user must re-enrol, and a user with no
-- recovery code and no factor cannot log in until an administrator re-enrols them (M6-1).
-- Rolling back is therefore a decision about the population, not just about code:
-- announce it, and keep a copy of `user_second_factor` before dropping it if re-enrolment
-- has to be avoided.
-- =============================================================================

-- =============================================================================
-- Promotion checklist (complete before the release cut)
-- =============================================================================
-- 1. [ ] Record AGENTS.md §12 sign-off for the three new tables in the PR description.
-- 2. [ ] Add the three CREATE TABLE blocks to `sql/complete-schema.sql`, so a fresh install
--     matches an upgraded one. (There the FK can be inline: `users` is created in the same
--     script with a pinned collation.)
-- 3. [ ] Add the row to the summary table in `sql/migrations/README.md` and to
--     `docs/migrations/NEXT.md` with pre-flight / apply / verify / rollback.
-- 4. [ ] Provision `second-factor-master-key` in the PKCS12 vault on every node before the
--     release that first reads it, and add key restore to the DR runbook (ADR 0011, risk R8).
-- 5. [ ] Remove the DRAFT status line from the header above and move the file out of `drafts/`.
-- =============================================================================
