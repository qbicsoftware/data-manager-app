-- =============================================================================
-- Migration: Per-user project view state ("new since you last looked")
-- Story:     TBD   (G9; deferred, needs its own ADR)
-- Feature:   TBD   (FEAT-PROJECT-*)
-- ADRs:      TBD   (user preferences vs project state — cf. ADR-0008)
--
-- Scope:     New table. No existing table touched.
-- Script:    sql/migrations/create-user-project-view-state.sql
-- Datasource: data_management
-- Status:    OPTIONAL / DEFERRED — do not apply until the last-seen model is
--            approved. Included so the schema decision is reviewable alongside
--            the read models.
-- =============================================================================
--
-- PURPOSE
--
-- The summary's dataset-standing block wants to close with "1 added since you
-- last looked 4 days ago". `project_overview.lastConnectedOn` is project-wide and
-- cannot answer a per-user question, so the read is per (user, project).
--
-- WHERE THIS BELONGS
--
-- ADR-0008 established that user-scoped state (pinned projects) is a user
-- preference, not project state. This table follows the same rule: it is keyed
-- by userId and is owned by the user, not the project. It lives in
-- data_management only because it references projects_datamanager.
--
-- WHY A LAST-VIEWED TIMESTAMP, NOT AN EVENT LOG
--
-- "New since last looked" only needs the previous view instant, not a history.
-- The newest connected-dataset `connected_on` is already on `associated_dataset`
-- (and aggregated in project_overview). A single upsert timestamp per
-- (user, project) is enough:
--     added_since_last_look = COUNT(*) FROM associated_dataset
--        WHERE project_id = ? AND connection_state = 'CONNECTED'
--          AND connected_on > lastViewedAt
--
-- NULL userId is meaningless, hence NOT NULL. No FK on userId: the users table
-- is owned by the identity context and other user-reference tables
-- (e.g. pinned_projects) deliberately do not FK it either.
-- =============================================================================

CREATE TABLE IF NOT EXISTS `user_project_view_state`
(
    `userId`       varchar(255) NOT NULL,
    `projectId`    varchar(255) NOT NULL,
    `lastViewedAt` datetime(6)  NOT NULL,
    PRIMARY KEY (`userId`, `projectId`),
    KEY `idx_user_project_view_state_user_viewed` (`userId`, `lastViewedAt`),
    CONSTRAINT `fk_user_project_view_state_project`
        FOREIGN KEY (`projectId`) REFERENCES `projects_datamanager` (`projectId`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;


-- -----------------------------------------------------------------------------
-- Verification
-- -----------------------------------------------------------------------------
-- SHOW CREATE TABLE user_project_view_state;


-- -----------------------------------------------------------------------------
-- Rollback
-- -----------------------------------------------------------------------------
-- DROP TABLE IF EXISTS user_project_view_state;
