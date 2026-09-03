-- =============================================================================
-- Migration: Create `user_external_credential` table
-- Feature:     FEAT-DATSET-14 — Add credentials for an InvenioRDM instance
-- ADRs:        0002 (credential storage), 0003 (lifecycle)
--
-- Stores per-user, per-instance personal access tokens for external data
-- providers. The table is source-agnostic at the schema level: `source_type`
-- and `instance_id` together identify the provider and instance. Today the
-- only rows will be INVENIO_RDM / zenodo | fdat. Future providers add rows
-- with a different source_type — no schema change required.
--
-- Tokens are AES-256-GCM encrypted at rest (nonce + ciphertext + 16-byte GCM
-- tag). The master key is stored in the PKCS12 vault at deploy time under a
-- dedicated alias (see qbic.security.vault.external-credential.key-alias).
--
-- Unique constraint on (user_id, source_type, instance_id): one token per
-- user per instance. source_type is included to keep the schema fully
-- provider-agnostic.
-- =============================================================================

CREATE TABLE IF NOT EXISTS `user_external_credential`
(
    `id`                varchar(36)     NOT NULL,
    `user_id`           varchar(255)    NOT NULL    COMMENT 'DM user ID',
    `source_type`       varchar(32)     NOT NULL    COMMENT 'e.g. INVENIO_RDM — matches SourceType enum',
    `instance_id`       varchar(64)     NOT NULL    COMMENT 'matches InstanceConfig.id (e.g. zenodo, fdat)',
    `encrypted_token`   varbinary(512)  NOT NULL    COMMENT 'AES-256-GCM: 12-byte nonce ‖ ciphertext ‖ 16-byte auth tag',
    `status`            varchar(16)     NOT NULL    COMMENT 'VALID | INVALIDATED',
    `created_at`        timestamp(3)    NOT NULL,
    `updated_at`        timestamp(3)    NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_src_instance` (`user_id`, `source_type`, `instance_id`),
    KEY `idx_cred_user` (`user_id`),
    KEY `idx_cred_user_src` (`user_id`, `source_type`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
