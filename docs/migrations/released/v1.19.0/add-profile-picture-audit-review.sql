-- =============================================================================
-- Migration: Add a review marker to the profile picture audit
-- Story:    FEAT-PROFILE-PICTURES (audit review follow-up; requirements/ADR pending)
-- Feature:  FEAT-PROFILE-PICTURES
-- ADRs:     new profile-picture moderation/audit ADR — pending human approval
-- Datasource: data_management
--
-- The audit trail is append-only (a real audit must not lose rows). To let
-- administrators "clean up" the working list without touching the stored
-- images, each audit row gains a review marker:
--
--   * reviewed    — whether an administrator has reviewed the entry
--   * reviewed_by — the reviewing administrator's user id
--   * reviewed_at — when the review happened
--
-- Force-removing a violating image is a separate action and no longer deletes
-- audit rows; it only removes the stored picture.
--
-- Rollback:
--   ALTER TABLE `profile_picture_audit`
--       DROP COLUMN IF EXISTS `reviewed_at`,
--       DROP COLUMN IF EXISTS `reviewed_by`,
--       DROP COLUMN IF EXISTS `reviewed`;
--   DROP INDEX IF EXISTS `idx_profile_picture_audit_reviewed`
--       ON `profile_picture_audit`;
--
-- Operator notes:
--   * Additive and safe while live; existing rows default to not reviewed.
-- =============================================================================

ALTER TABLE `profile_picture_audit`
    ADD COLUMN IF NOT EXISTS `reviewed` bit(1) NOT NULL DEFAULT b'0',
    ADD COLUMN IF NOT EXISTS `reviewed_by` varchar(255) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS `reviewed_at` datetime(6) DEFAULT NULL;

CREATE INDEX IF NOT EXISTS `idx_profile_picture_audit_reviewed`
    ON `profile_picture_audit` (`reviewed`, `created_at`);
