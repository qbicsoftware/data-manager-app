-- =============================================================================
-- Migration: Create the user groups tables
-- Story:    FEAT-USER-GROUPS-03 (#1561) — Create Ad-hoc Groups Self-Service
-- Feature:  FEAT-USER-GROUPS (#1558) · Requirements: GROUP-R-02, GROUP-NFR-02
-- ADRs:     0010 (user-groups bounded context) — pending human approval
-- Datasource: data_management
--
-- Adds the two tables of the new `user-groups` bounded context, enabling
-- ad-hoc group creation by researchers where the creator becomes the group
-- OWNER (self-service, no admin involvement).
--
--   * user_group — one row per group. `id` is the stable UUID with which the
--     group is referenced by Spring ACL authority SIDs ("GROUP_<id>") once the
--     sharing feature lands; `name` is unique (case-insensitive under the
--     utf8mb4_unicode_ci collation — this is the authoritative backstop for
--     acceptance-criterion (b) of FEAT-USER-GROUPS-03); `description` is
--     optional; `type` distinguishes ORG (admin-managed) from ADHOC
--     (self-service) groups; `status` is ACTIVE|DISSOLVED (soft dissolve keeps
--     the row for traceability, per the strategy); `created_by` is the QBiC
--     user id of the creating user.
--   * group_membership — one row per (group, user) pair. `role` is the
--     group-internal role OWNER|MANAGER|MEMBER (unrelated to project access
--     roles); the composite primary key (group_id, user_id) makes a user hold
--     at most one membership per group.
--
-- Cross-context rule: `created_by` / `user_id` are bare varchar references to
-- identity user ids with NO foreign keys to `users`. The identity context is a
-- separate bounded context; following the `personal_access_tokens.userId` and
-- `pinned_projects.userId` precedent, group data must not gain a schema
-- dependency on `identity`. Withdrawal of a user leaves audit-visible
-- historical rows.
--
-- Reusing a group id after dissolution is impossible in the application: ids
-- are client-generated UUIDs. ACEs and memberships referencing a dissolved
-- group are cleaned up by later stories (revocation/security epic) that remove
-- the group's `GrantedAuthoritySid("GROUP_<id>")` ACEs on dissolve.
--
-- Rollback:
--   DROP TABLE IF EXISTS `group_membership`;
--   DROP TABLE IF EXISTS `user_group`;
--   The feature is additive: no other table, view or column is touched and no
--   existing object depends on these two, so dropping them returns the schema
--   to its previous state.
--
-- Operator notes:
--   * Safe to run while the application is live — creates two new, empty tables
--     and takes no locks on existing objects.
--   * No backfill is possible or needed: groups are created by users in the UI.
--   * Requires the tables' collation to be utf8mb4_unicode_ci (matching every
--     other table in this schema). The case-insensitive name uniqueness depends
--     on it; under a different (e.g. utf8mb4_bin) collation the unique index
--     would become case-sensitive.
-- =============================================================================

CREATE TABLE IF NOT EXISTS `user_group`
(
    `id`          varchar(36)  NOT NULL,
    `name`        varchar(80)  NOT NULL,
    `description` varchar(500) DEFAULT NULL,
    `type`        varchar(16)  NOT NULL,
    `status`      varchar(16)  NOT NULL,
    `created_by`  varchar(255) NOT NULL,
    `created_at`  datetime(6)  NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_group_name` (`name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `group_membership`
(
    `group_id` varchar(36)  NOT NULL,
    `user_id`  varchar(255) NOT NULL,
    `role`     varchar(16)  NOT NULL,
    `joined_at` datetime(6) NOT NULL,
    PRIMARY KEY (`group_id`, `user_id`),
    KEY `idx_group_membership_user` (`user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;