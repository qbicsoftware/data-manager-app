-- =============================================================================
-- Migration: Add sample batch property and project association (additive)
-- Story:    FEAT-SAMBAT-01 #1549, FEAT-SAMBAT-02 #1550, FEAT-SAMBAT-03 #1551
-- Feature:  FEAT-SAMPLE-BATCH-REMOVAL #1548
-- ADRs:     (none — ADRs 0008/0009 were removed as too implementation-specific)
--
-- Datasource: data_management
--
-- NON-DESTRUCTIVE additive migration for the sample batch removal. It adds the
-- new columns to `sample` and backfills them from the legacy `sample_batches`
-- tables. The legacy tables and `sample.assigned_batch_id` are KEPT in place,
-- because the database is shared with the test system and other nodes are
-- running. A separate stop-the-world script
-- (finalize-sample-batch-removal.sql) drops them at the coordinated downtime
-- window.
--
-- Column mapping (sample_batches -> sample):
--   sample_batches.batchLabel   -> sample.batch
--   sample_batches.createdOn    -> sample.registrationTime
--   sample_batches.lastModified -> sample.lastModified
--   (derived)                   -> sample.project_id (via experiment -> project)
--
-- `project_id` is added as a plain column here; the real FK constraint is added
-- only in the finalize (production stop-the-world) script per SAMPLE-R-02.
-- =============================================================================

-- Step 1: Add the new columns to `sample` (idempotent via information_schema guard)
-- =============================================================================

SET @has_batch := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sample' AND COLUMN_NAME = 'batch');
SET @sql := IF(@has_batch = 0,
    'ALTER TABLE `sample` ADD COLUMN `batch` varchar(255) DEFAULT NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_project_id := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sample' AND COLUMN_NAME = 'project_id');
SET @sql := IF(@has_project_id = 0,
    'ALTER TABLE `sample` ADD COLUMN `project_id` varchar(255) DEFAULT NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_registration_time := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sample' AND COLUMN_NAME = 'registrationTime');
SET @sql := IF(@has_registration_time = 0,
    'ALTER TABLE `sample` ADD COLUMN `registrationTime` datetime(6) DEFAULT NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_last_modified := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sample' AND COLUMN_NAME = 'lastModified');
SET @sql := IF(@has_last_modified = 0,
    'ALTER TABLE `sample` ADD COLUMN `lastModified` datetime(6) DEFAULT NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Step 2: Backfill sample.batch, sample.registrationTime, sample.lastModified
--         from the sample's assigned batch.
-- =============================================================================

UPDATE `sample` s
JOIN `sample_batches` sb ON sb.id = s.assigned_batch_id
SET s.`batch`            = sb.`batchLabel`,
    s.`registrationTime` = sb.`createdOn`,
    s.`lastModified`     = sb.`lastModified`
WHERE s.assigned_batch_id IS NOT NULL;

-- Step 3: Backfill sample.project_id from the sample's experiment's owning project.
-- =============================================================================

UPDATE `sample` s
JOIN `experiments_datamanager` e ON e.id = s.experiment_id
SET s.`project_id` = e.`project`
WHERE s.experiment_id IS NOT NULL AND e.`project` IS NOT NULL;

-- Step 4: Verification summary.
-- =============================================================================

SELECT
    COUNT(*) AS total_samples,
    SUM(s.batch IS NULL)                       AS samples_without_batch,
    SUM(s.project_id IS NULL)                  AS samples_without_project,
    SUM(s.registrationTime IS NULL)            AS samples_without_registration_time,
    SUM(s.assigned_batch_id IS NULL)           AS samples_without_assigned_batch
FROM `sample` s;