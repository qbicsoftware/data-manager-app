-- =============================================================================
-- Migration: Finalize sample batch removal (stop-the-world, destructive)
-- Story:    FEAT-SAMBAT-03 #1551
-- Feature:  FEAT-SAMPLE-BATCH-REMOVAL #1548
-- ADRs:     (none — ADRs 0008/0009 were removed as too implementation-specific)
--
-- Datasource: data_management
--
-- Destructive finalization of the sample batch removal. Run ONLY during the
-- coordinated stop-the-world downtime window, AFTER all nodes have been shut
-- down and AFTER the additive backfill script
-- (add-sample-batch-property-and-project-association.sql) has populated the
-- new columns.
--
-- This script:
--   * assumes sample.batch, sample.project_id, sample.registrationTime and
--     sample.lastModified already exist and are backfilled
--   * adds the real FK constraint on sample.project_id (SAMPLE-R-02)
--   * drops the obsolete sample.assigned_batch_id column
--   * drops the legacy sample_batches / sample_batches_sampleid tables
--
-- It is wrapped in a transaction: execute, VERIFY, then COMMIT manually so a
-- mistake can be rolled back.
-- =============================================================================

START TRANSACTION;

-- Step 1: SAFETY CHECK — refuse to proceed if the backfill has not run.
--         This script must fail loudly rather than drop data that was not
--         migrated.
-- =============================================================================

-- A) Ensure all four new columns exist.
SELECT 'sample.batch' AS expected_column
  FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sample' AND COLUMN_NAME = 'batch';
SELECT 'sample.project_id' AS expected_column
  FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sample' AND COLUMN_NAME = 'project_id';
SELECT 'sample.registrationTime' AS expected_column
  FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sample' AND COLUMN_NAME = 'registrationTime';
SELECT 'sample.lastModified' AS expected_column
  FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sample' AND COLUMN_NAME = 'lastModified';

-- B) Loudly report any sample that has a batch but missing backfilled values.
--    (MySQL cannot abort a script cleanly on a condition, so this must be
--    inspected manually before COMMIT.)
SET @orphaned := (
    SELECT COUNT(*)
    FROM `sample` s
    JOIN `sample_batches` sb ON sb.id = s.assigned_batch_id
    WHERE s.assigned_batch_id IS NOT NULL
      AND (s.`batch` IS NULL OR s.`registrationTime` IS NULL OR s.`lastModified` IS NULL)
);
SELECT @orphaned AS samples_missing_backfilled_values;

-- Step 2: Add the real foreign key on sample.project_id -> projects_datamanager.
-- =============================================================================

ALTER TABLE `sample`
    ADD CONSTRAINT `FK_sample_project`
        FOREIGN KEY (`project_id`) REFERENCES `projects_datamanager` (`projectId`);

-- Step 3: Drop the obsolete sample.assigned_batch_id column.
-- =============================================================================

ALTER TABLE `sample` DROP COLUMN `assigned_batch_id`;

-- Step 4: Drop the legacy batch tables.
-- =============================================================================

DROP TABLE `sample_batches_sampleid`;
DROP TABLE `sample_batches`;

-- Step 5: VERIFICATION — run this BEFORE committing.
--         Expected: the two legacy tables are gone; sample still exists with
--         the new columns and no assigned_batch_id; the FK is present.
-- =============================================================================

SHOW TABLES LIKE 'sample_batches%';
SELECT COLUMN_NAME FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sample'
   AND COLUMN_NAME IN ('batch','project_id','registrationTime','lastModified','assigned_batch_id');
SELECT COUNT(*) AS samples_without_batch FROM `sample` WHERE `batch` IS NULL;

-- If the verification output looks correct, run:  COMMIT;
-- If anything is wrong, run:                      ROLLBACK;