-- =============================================================================
-- Migration: Create the profile picture tables
-- Story:    FEAT-PROF-PIC-01 / FEAT-PROF-PIC-02 (proposed; see
--           docs/plans/FEAT-PROFILE-PICTURES-implementation-plan.md — governance
--           prerequisites, including the requirements PR, must land first)
-- Feature:  FEAT-PROFILE-PICTURES (proposed)
-- ADRs:     new ADR for profile-picture storage — pending human approval
-- Datasource: data_management
--
-- Adds two tables backing the profile-picture feature for user profiles and
-- user-group profiles:
--
--   * profile_picture — one row per owner (user or group) holding the
--     normalized square PNG derivative (256x256) and its SHA-256 content hash.
--     The blob is deliberately isolated in its own table so it never inflates
--     the hot `users` / `user_group` rows and never has to be loaded for
--     ordinary profile reads. `content_hash` drives the immutable delivery URL
--     and cache busting.
--   * profile_picture_audit — append-only trail of SET / REPLACE events,
--     readable by system administrators only. Removal and force-removal are
--     intentionally not recorded (agreed scope).
--
-- Cross-context rule: `owner_id` is a bare varchar reference to an identity
-- user id or a user-groups group id with NO foreign keys. Following the
-- `pinned_projects.userId` and `group_membership.user_id` precedent, profile
-- pictures are a presentation concern and must not create a schema dependency
-- between the identity and user-groups bounded contexts.
--
-- Rollback:
--   DROP TABLE IF EXISTS `profile_picture_audit`;
--   DROP TABLE IF EXISTS `profile_picture`;
--   The feature is additive: no other table, view or column is touched.
--
-- Operator notes:
--   * Safe to run while the application is live — creates two new, empty tables
--     and takes no locks on existing objects.
--   * No backfill is possible or needed: users set pictures in the UI; the
--     identicon fallback is used until one is set.
--   * Expect ~10-30 KB per row (256x256 PNG) plus blob overhead.
-- =============================================================================

CREATE TABLE IF NOT EXISTS `profile_picture`
(
    `id`           bigint(20)   NOT NULL AUTO_INCREMENT,
    `owner_type`   varchar(16)  NOT NULL,
    `owner_id`     varchar(255) NOT NULL,
    `content_type` varchar(32)  NOT NULL,
    `content_hash` char(64)     NOT NULL,
    `width`        smallint     NOT NULL,
    `height`       smallint     NOT NULL,
    `data`         mediumblob   NOT NULL,
    `updated_at`   datetime(6)  NOT NULL,
    `updated_by`   varchar(255) NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_profile_picture_owner` (`owner_type`, `owner_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `profile_picture_audit`
(
    `id`                    bigint(20)   NOT NULL AUTO_INCREMENT,
    `owner_type`            varchar(16)  NOT NULL,
    `owner_id`              varchar(255) NOT NULL,
    `action`                varchar(16)  NOT NULL,
    `actor_id`              varchar(255) NOT NULL,
    `previous_content_hash` char(64)     DEFAULT NULL,
    `new_content_hash`      char(64)     NOT NULL,
    `created_at`            datetime(6)  NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_profile_picture_audit_owner` (`owner_type`, `owner_id`, `created_at`),
    KEY `idx_profile_picture_audit_created` (`created_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
