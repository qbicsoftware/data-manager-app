-- =============================================================================
-- Migration: Create the pinned_projects table
-- Story:    docs/features.md — FEAT-PINNED-01 (tracked in docs only, no GitHub issue)
-- Feature:  FEAT-PINNED-PROJECTS · Requirement: USER-R-04
-- ADRs:     0008 (pinned projects are user preferences, not project state)
-- Datasource: data_management
--
-- Adds one table holding the per-user pinned-project associations that back the
-- quick-access shortlist on the project overview:
--
--   * userId              — owner of the pin. A bare varchar reference to the
--                           identity user id, with NO foreign key to `users`.
--                           This follows the existing `personal_access_tokens.userId`
--                           precedent: `identity` is a separate bounded context and
--                           project-management preference data must not gain a schema
--                           dependency on it.
--   * projectId           — the pinned project; FK to projects_datamanager with
--                           ON DELETE CASCADE, so deleting a project cannot leave
--                           orphan pins behind.
--   * pinnedAt            — creation timestamp; the shortlist is ordered by it
--                           (newest pin first).
--   * projectCodeSnapshot / projectTitleSnapshot — the project label as readable at
--                           pin creation time, written once and never updated. They
--                           exist solely so a pin whose project the user can no
--                           longer read stays recognisable and removable (ADR-0008).
--                           For a readable pin the live overview wins and these
--                           columns are ignored. They must never be exported,
--                           indexed or used in RO-Crate output.
--
-- The composite primary key (userId, projectId) makes "a user pins the same project
-- twice" impossible at the database level. The per-user count is bounded in the
-- application service (PinnedProjectService.MAX_PINNED_PROJECTS), not by a database
-- constraint, because a cap expressed in DDL cannot be reviewed as a product rule.
--
-- Secondary index:
--   The read path is `where userId = ? order by pinnedAt desc`. The primary key
--   covers the userId filter but not the ordering, so the extra (userId, pinnedAt)
--   index lets MariaDB satisfy both from the index. The table is tiny (≤ 5 rows per
--   user), so this is hygiene rather than a hot path.
--
-- Rollback:
--   DROP TABLE IF EXISTS `data_management`.`pinned_projects`;
--   The feature is additive: no other table, view or column is touched and no
--   existing object depends on this one, so dropping it returns the schema to its
--   previous state. Losing the table loses only user shortlists, not project data.
--
-- Operator notes:
--   * Safe to run while the application is live — it creates a new, empty table and
--     takes no locks on existing objects.
--   * No backfill is possible or needed: pins are created by users in the UI.
--   * Requires `projects_datamanager` to exist (always true in this schema).
-- =============================================================================

CREATE TABLE IF NOT EXISTS `pinned_projects`
(
    `userId`               varchar(255) NOT NULL,
    `projectId`            varchar(255) NOT NULL,
    `pinnedAt`             datetime(6)  NOT NULL,
    `projectCodeSnapshot`  varchar(255)  DEFAULT NULL,
    `projectTitleSnapshot` varchar(255)  DEFAULT NULL,
    PRIMARY KEY (`userId`, `projectId`),
    KEY `idx_pinned_projects_user_pinned_at` (`userId`, `pinnedAt`),
    CONSTRAINT `fk_pinned_projects_project` FOREIGN KEY (`projectId`) REFERENCES `projects_datamanager` (`projectId`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
