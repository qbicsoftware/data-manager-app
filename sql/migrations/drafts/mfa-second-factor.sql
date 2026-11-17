-- =============================================================================
-- Migration: Second-factor storage, recovery codes and the enforcement grace window
-- Story:    FEAT-MFA-ENRL-01 / FEAT-MFA-LOGIN-01 (MILESTONE-MFA M2-1)
-- Feature:  https://github.com/qbicsoftware/data-manager-app/milestone/1
-- ADRs:     0011 (MFA authentication authority: the application owns the second factor)
-- Datasource: data_management
--
-- Status:   DRAFT — DO NOT APPLY.
--           Requires AGENTS.md §12 sign-off (schema change) and the M2 PR review. The ADR
--           is accepted; this DDL is not yet approved for production. Move the file to
--           `sql/migrations/` and complete the promotion checklist below when it is.
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
-- =============================================================================

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
    `disabledAt`         datetime(6)  DEFAULT NULL
        COMMENT 'Soft delete. A removed factor is never hard-deleted, so the audit trail (AUTH-R-04) keeps showing when it existed and when it stopped.',

    KEY `idx_user_second_factor_user` (`userId`, `disabledAt`),
    CONSTRAINT `FK_user_second_factor_user`
        FOREIGN KEY (`userId`) REFERENCES `users` (`id`)
            ON DELETE CASCADE,
    CONSTRAINT `chk_user_second_factor_shape`
        CHECK ((`type` = 'TOTP' AND `secretCiphertext` IS NOT NULL AND `webauthnCredential` IS NULL)
            OR (`type` = 'WEBAUTHN' AND `webauthnCredential` IS NOT NULL AND
                `secretCiphertext` IS NULL))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 2. user_recovery_code: single-use break-glass codes (hashed, never plaintext)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user_recovery_code`
(
    `id`            bigint(20)   NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `userId`        varchar(255) NOT NULL,
    `codeHash`      varchar(255) NOT NULL
        COMMENT 'PBKDF2 hash of a high-entropy code (per-code salt inside the hash string, as the PAT encoder already does). Deliberately NOT encrypted with the vault master key: these must still verify when that key is lost (ADR 0011 B1).',
    `createdAt`     datetime(6)  NOT NULL,
    `usedAt`        datetime(6)  DEFAULT NULL
        COMMENT 'Single use. Set inside the consuming transaction.',
    `invalidatedAt` datetime(6)  DEFAULT NULL
        COMMENT 'Set on regeneration: issuing a new set retires every unused code of the previous set (milestone M3-4). A code is usable only while usedAt and invalidatedAt are both NULL.',

    -- Globally unique, not per user: the recovery login path resolves a code without knowing
    -- the account (that is the point — the account is what the user is trying to reach), so the
    -- hash is the lookup key and two users must not share one.
    UNIQUE KEY `uk_user_recovery_code_hash` (`codeHash`),
    KEY `idx_user_recovery_code_user` (`userId`),
    CONSTRAINT `FK_user_recovery_code_user`
        FOREIGN KEY (`userId`) REFERENCES `users` (`id`)
            ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

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

    CONSTRAINT `FK_user_mfa_grace_user`
        FOREIGN KEY (`userId`) REFERENCES `users` (`id`)
            ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

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
--     matches an upgraded one.
-- 3. [ ] Add the row to the summary table in `sql/migrations/README.md` and to
--     `docs/migrations/NEXT.md` with pre-flight / apply / verify / rollback.
-- 4. [ ] Provision `second-factor-master-key` in the PKCS12 vault on every node before the
--     release that first reads it, and add key restore to the DR runbook (ADR 0011, risk R8).
-- 5. [ ] Remove the DRAFT status line from the header above and move the file out of `drafts/`.
-- =============================================================================
