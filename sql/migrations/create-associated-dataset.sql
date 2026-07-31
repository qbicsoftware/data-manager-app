-- =============================================================================
-- Migration: Create `associated_dataset` table
-- Feature:  FEAT-DATSET-01 — Connecting open, published datasets
-- ADRs:     0001 (domain model), 0002 (credential storage), 0003 (lifecycle)
--
-- This migration establishes the incremental-scripts pattern for the project.
-- Going forward, incremental schema changes go here alongside the canonical
-- DDL in sql/complete-schema.sql. The complete-schema.sql file is always kept
-- in the "applied state" — it serves as the reference for fresh installs.
--
-- Notes about the table design (per ADR-0001):
--   * `associated_dataset` lives in the project-management bounded context.
--   * Universal columns (`title`, `pid`, `version`, `publication_date`) live
--     as regular SQL columns — they are source-agnostic (every external source
--     provides them) and are used for efficient SQL sort/filter.
--   * `resource_metadata` is a MariaDB JSON column. Source-specific metadata
--     (creators, community, access details) is serialized there by the
--     application layer via the sealed ResourceMetadata hierarchy. This keeps
--     the aggregate source-agnostic at its public API boundary. When JSON-path
--     filtering becomes a performance concern (currently expected at
--     <100 rows/project, so it isn't), the escape hatch is a generated virtual
--     column + index — no entity change required.
--   * Soft-delete: setting `connection_state = 'REMOVED'` marks the row as a
--     tombstone (audit retention). Active-query paths exclude REMOVED rows.
--   * `experiment_id` is a logical foreign key to `experiments_datamanager.id`
--     but intentionally has no SQL FK constraint — the application enforces
--     referential integrity so that project-ACL changes don't cascade
--     unexpectedly onto connections.
-- =============================================================================

-- Step 1: Create the table (idempotent — safe to re-run)
-- =============================================================================

CREATE TABLE IF NOT EXISTS `associated_dataset`
(
    `id`                varchar(36) NOT NULL,
    `project_id`        varchar(36)     NOT NULL,
    `source_type`       varchar(32)     NOT NULL    COMMENT 'e.g. INVENIO_RDM',
    `external_handle`   varchar(512)    NOT NULL    COMMENT 'record ID on the source',
    `connection_state`  varchar(16)     NOT NULL    COMMENT 'CONNECTED | REMOVED (soft-delete)',
    `access_level`      varchar(16)     NOT NULL    COMMENT 'coarse access, derived from metadata',

    -- Universal columns (source-agnostic, used for sort/filter)
    `title`             varchar(1024)   DEFAULT NULL,
    `pid`               varchar(255)    DEFAULT NULL,
    `version`           varchar(32)     DEFAULT NULL,
    `publication_date`  date            DEFAULT NULL,

    -- Source-specific metadata (opaque JSON; see ResourceMetadata hierarchy)
    `resource_metadata` json            DEFAULT NULL,

    -- Connection metadata
    `connected_by`      varchar(255)    NOT NULL,
    `connected_on`      timestamp(3)    NOT NULL,
    `experiment_id`     varchar(36)     DEFAULT NULL COMMENT 'optional experiment association',
    `last_synced_at`    timestamp(3)    DEFAULT NULL,

    PRIMARY KEY (`id`),
    KEY `idx_assoc_ds_project`          (`project_id`),
    KEY `idx_assoc_ds_state`            (`connection_state`),
    KEY `idx_assoc_ds_source_type`      (`source_type`),
    KEY `idx_assoc_ds_project_state`    (`project_id`, `connection_state`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
