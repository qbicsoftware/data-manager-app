-- =====================================================================================
-- DRAFT — DO NOT APPLY
-- =====================================================================================
-- Story:     FEAT-MFA-* (MILESTONE-MFA phase M1, issue M1-1) — TBD, assigned in M0-4
-- Feature:   FEAT-MFA-ENRL / FEAT-MFA-LOGIN — TBD
-- ADR:       docs/adr/0011-mfa-authentication-authority.md (draft, M0-3)
-- Date:      2026-11-10
-- Status:    draft for review. Requires (a) AUTH-* requirements PR, (b) ADR 0011,
--            (c) schema-change approval per AGENTS.md §12.
--
-- Purpose: persist self-governed second factors for the Data Manager identity context.
--
-- Design notes:
--  * WebAuthn credentials store PUBLIC keys only — no server-side secret to leak.
--  * TOTP secrets are encrypted with the existing PKCS12 vault key (same mechanism as
--    personal access tokens). The column holds ciphertext, never the raw seed.
--  * `users` gains an enrolment timestamp (fast login-path check, single source of truth
--    is still the factor rows) and a policy override used for break-glass and per-cohort
--    rollout without a deploy.
--  * Idempotent: MariaDB 10.2+ required (matches the read-model migrations in this repo).
-- =====================================================================================

-- -------------------------------------------------------------------------------------
-- 1. users: enrolment timestamp + per-user policy override
-- -------------------------------------------------------------------------------------
ALTER TABLE `users`
    ADD COLUMN IF NOT EXISTS `mfaEnrolledAt`    datetime(6) DEFAULT NULL
        COMMENT 'Timestamp of the first completed second-factor enrolment; NULL = not enrolled.',
    ADD COLUMN IF NOT EXISTS `mfaPolicyOverride` varchar(16) DEFAULT NULL
        COMMENT 'NULL = inherit MfaPolicy. Values: REQUIRED, NOT_REQUIRED (break-glass / cohort rollout).';

CREATE INDEX IF NOT EXISTS `idx_users_mfa_enrolled`
    ON `users` (`mfaEnrolledAt`);

-- -------------------------------------------------------------------------------------
-- 2. user_second_factor: one row per enrolled credential
-- -------------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user_second_factor`
(
    `id`              bigint(20)   NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `userId`          varchar(255) NOT NULL,
    `type`            varchar(16)  NOT NULL
        COMMENT 'WEBAUTHN | TOTP',

    -- TOTP: vault-encrypted seed (ciphertext only). NULL for WEBAUTHN.
    `secretEncrypted` text         DEFAULT NULL,

    -- WEBAUTHN: serialised credential record (credential id, public key, sign count,
    -- transports, AAGUID, backup-eligible flags). NULL for TOTP.
    `webauthnCredential` text      DEFAULT NULL,

    `displayName`     varchar(255) NOT NULL
        COMMENT 'User-facing label, e.g. "iPhone (Passkey)" or "YubiKey 5".',
    `createdAt`       datetime(6)  NOT NULL,
    `lastUsedAt`      datetime(6)  DEFAULT NULL,
    `disabledAt`      datetime(6)  DEFAULT NULL
        COMMENT 'Soft delete: a used factor is never hard-deleted (audit trail).',

    UNIQUE KEY `uk_user_second_factor_name` (`userId`, `displayName`),
    KEY `FK_user_second_factor_user` (`userId`),
    CONSTRAINT `FK_user_second_factor_user`
        FOREIGN KEY (`userId`) REFERENCES `users` (`id`)
            ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- -------------------------------------------------------------------------------------
-- 3. user_recovery_code: single-use break-glass codes (hashed, never plaintext)
-- -------------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user_recovery_code`
(
    `id`        bigint(20)   NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `userId`    varchar(255) NOT NULL,
    `codeHash`  varchar(255) NOT NULL
        COMMENT 'Hash of a high-entropy code — not a password, so a fast hash is acceptable; use the same encoder family as PATs for consistency.',
    `createdAt` datetime(6)  NOT NULL,
    `usedAt`    datetime(6)  DEFAULT NULL,

    KEY `FK_user_recovery_code_user` (`userId`),
    CONSTRAINT `FK_user_recovery_code_user`
        FOREIGN KEY (`userId`) REFERENCES `users` (`id`)
            ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- -------------------------------------------------------------------------------------
-- 4. user_login_throttle: failure counters for password AND second-factor step
-- -------------------------------------------------------------------------------------
-- NOTE: there is currently NO lockout/throttling anywhere in the application — this table
-- closes that gap for both steps (MILESTONE-MFA task M4-4).
CREATE TABLE IF NOT EXISTS `user_login_throttle`
(
    `userId`             varchar(255) NOT NULL PRIMARY KEY,
    `failedPasswordCount` int(11)     NOT NULL DEFAULT 0,
    `failedSecondFactorCount` int(11) NOT NULL DEFAULT 0,
    `lockedUntil`         datetime(6) DEFAULT NULL,
    `lastFailureAt`       datetime(6) DEFAULT NULL,

    CONSTRAINT `FK_user_login_throttle_user`
        FOREIGN KEY (`userId`) REFERENCES `users` (`id`)
            ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- =====================================================================================
-- Rollback (destructive — factor registrations are lost, users must re-enrol)
-- =====================================================================================
-- DROP TABLE IF EXISTS `user_login_throttle`;
-- DROP TABLE IF EXISTS `user_recovery_code`;
-- DROP TABLE IF EXISTS `user_second_factor`;
-- DROP INDEX IF EXISTS `idx_users_mfa_enrolled` ON `users`;
-- ALTER TABLE `users`
--     DROP COLUMN IF EXISTS `mfaPolicyOverride`,
--     DROP COLUMN IF EXISTS `mfaEnrolledAt`;
-- =====================================================================================
