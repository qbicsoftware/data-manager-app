-- =============================================================================
-- INSTANCE REPAIR: normalise collations to utf8mb4_unicode_ci
--
-- NOT a release migration. This is an operational repair for a drifted instance
-- (typically a test box). sql/complete-schema.sql already declares
-- utf8mb4_unicode_ci everywhere, so a correct instance needs nothing from this file.
-- It is deliberately not listed in docs/migrations/NEXT.md.
--
-- WHY
--
-- A drifted instance was found carrying three collations at once:
--   latin1_swedish_ci      sample, ngs_measurements, proteomics_measurement,
--                          specific_measurement_metadata_ngs, roles, permissions,
--                          experimental_variables, project_userinfo, ...
--   utf8mb4_general_ci     experiments_datamanager, experimental_group,
--                          experimental_group_variableLevels
--   utf8mb4_unicode_ci     projects_datamanager, remote_measurement_data,
--                          associated_dataset, ...
--
-- A column-to-column comparison across two of those raises
--   Illegal mix of collations (x,IMPLICIT) and (y,IMPLICIT) for operation '='
-- at runtime, in any JOIN/EXISTS/subquery that compares them. Column-to-literal and
-- column-to-parameter comparisons survive because the literal/parameter is coercible,
-- which is why the application mostly works until it does not.
--
-- SCOPE / SAFETY
--
--  * latin1 -> utf8mb4 and utf8mb3 -> utf8mb4 are lossless widenings; no data is lost.
--    The one real risk is pre-existing mojibake: if the application previously wrote
--    UTF-8 bytes into a latin1 column, this preserves the already-wrong bytes rather
--    than repairing them. Check a text column you know holds non-ASCII, before and
--    after.
--  * CONVERT TO rewrites every char/varchar/text column and every index on them. On a
--    large table that is a full rebuild holding a metadata lock: back up, and run it
--    in a maintenance window.
--  * Index width: utf8mb4 needs up to 4 bytes per character against latin1's 1. An
--    indexed varchar longer than 768 characters can exceed the InnoDB key limit
--    (3072 bytes, DYNAMIC row format). No application table does, but check anything
--    you add.
--  * Foreign keys require the parent and child columns to share a charset, so every
--    constraint must be dropped before the conversion and added back afterwards.
--    FOREIGN_KEY_CHECKS=0 is NOT sufficient -- see Step 2.
--
-- =============================================================================


-- -----------------------------------------------------------------------------
-- STEP 0. Back up. Do not skip.
-- -----------------------------------------------------------------------------
-- mysqldump --single-transaction --routines --triggers --events \
--     -u <user> -h <host> <database> > before-collation-repair.sql


-- -----------------------------------------------------------------------------
-- STEP 1. Inspect. Run this first and keep the output.
-- -----------------------------------------------------------------------------
-- Everything that disagrees with the target collation, table by table. The repair
-- column is authoritative for YOUR instance and also catches tables created by
-- Hibernate that are not in the schema (measurement, batch, user_project_view_state,
-- ...). If it lists a table that Step 2 does not, convert it too.
SELECT TABLE_NAME, TABLE_COLLATION,
       CONCAT('ALTER TABLE `', TABLE_NAME,
              '` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;') AS repair
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_TYPE = 'BASE TABLE'
  AND TABLE_COLLATION <> 'utf8mb4_unicode_ci'
ORDER BY TABLE_NAME;

-- And the columns, to see exactly which ones are latin1 / utf8mb4_general_ci:
SELECT TABLE_NAME, COLUMN_NAME, CHARACTER_SET_NAME, COLLATION_NAME
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND COLLATION_NAME IS NOT NULL
  AND COLLATION_NAME <> 'utf8mb4_unicode_ci'
ORDER BY COLLATION_NAME, TABLE_NAME, COLUMN_NAME;


-- -----------------------------------------------------------------------------
-- STEP 2. Convert the application tables (48 of them).
-- -----------------------------------------------------------------------------
-- Run sql/repair/normalize-collations-step2.sql. It is self-contained and does the three
-- things this step needs, in this order:
--
--   2a. DROP the 31 foreign key constraints. MariaDB refuses to change the character set
--       of a column that participates in a foreign key, even with
--       FOREIGN_KEY_CHECKS=0 -- that check is structural, not validation:
--
--         [HY000][1832] Cannot change column 'experimentId': used in a foreign key
--                       constraint 'data_management/FK25vvdiupupuwmehr3o97dh7fg'
--
--   2b. ALTER TABLE ... CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci for
--       each of the 48 tables.
--
--   2c. ADD the same 31 constraints back, copied verbatim from sql/complete-schema.sql.
--
-- That file is the source of truth for the conversion. It also carries the constraint-count
-- verification (Step 2d, expected: 31) and documents the library-owned tables.


-- -----------------------------------------------------------------------------
-- STEP 3. Recreate every view.
-- -----------------------------------------------------------------------------
-- A view captures the collation of its columns when it is created, so a view made
-- before this repair keeps the old collations and keeps failing. Recreate them.

-- 3a. The views defined in sql/complete-schema.sql. Re-run their CREATE statements
--     from that file (they are plain CREATE VIEW / CREATE OR REPLACE VIEW):
--       project_userinfo
--       project_overview
--       v_ngs_measurement_sample_json
--       v_pxp_measurement_sample_json
--       v_ip_measurement_sample_json

-- 3b. The read models from sql/migrations/. CREATE OR REPLACE VIEW, idempotent:
SOURCE sql/migrations/create-project-page-read-models.sql;
SOURCE sql/migrations/create-experiment-recent-activity-views.sql;

-- If your client does not support SOURCE, run those two scripts the way you applied
-- them originally.


-- -----------------------------------------------------------------------------
-- STEP 4. Optional: library-owned tables.
-- -----------------------------------------------------------------------------
-- JobRunr pins its own id columns to utf8mb3 and its table default to
-- utf8mb4_unicode_ci; ShedLock's table is likewise library-defined. Nothing in the
-- application compares their text columns to application columns, so leaving them is
-- safe, and it avoids fighting the libraries' own schema management. Uncomment only
-- for a uniform instance.

-- SET FOREIGN_KEY_CHECKS = 0;
ALTER TABLE `jobrunr_backgroundjobservers` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `jobrunr_jobs` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `jobrunr_metadata` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `jobrunr_migrations` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `jobrunr_recurring_jobs` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `shedlock` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- SET FOREIGN_KEY_CHECKS = 1;


-- -----------------------------------------------------------------------------
-- STEP 5. Verify. Every result below must be empty (except the last, informational).
-- -----------------------------------------------------------------------------

-- No base table left behind:
SELECT TABLE_NAME, TABLE_COLLATION
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_TYPE = 'BASE TABLE'
  AND TABLE_COLLATION <> 'utf8mb4_unicode_ci';

-- No column left behind:
SELECT TABLE_NAME, COLUMN_NAME, COLLATION_NAME
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND COLLATION_NAME IS NOT NULL
  AND COLLATION_NAME <> 'utf8mb4_unicode_ci';

-- The read models actually execute. Any error names the offending pair:
SELECT * FROM experiment_progress LIMIT 1;
SELECT * FROM v_project_sample_overview LIMIT 1;
SELECT * FROM experiment_recent_sample LIMIT 1;
SELECT * FROM experiment_recent_measurement LIMIT 1;
SELECT * FROM v_measurement_experiment LIMIT 1;
SELECT * FROM v_ngs_measurement_sample_json LIMIT 1;
SELECT * FROM v_pxp_measurement_sample_json LIMIT 1;
SELECT * FROM v_ip_measurement_sample_json LIMIT 1;

-- Informational: the foreign keys, to confirm they survived.
SELECT TABLE_NAME, CONSTRAINT_NAME, REFERENCED_TABLE_NAME
FROM information_schema.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = DATABASE() AND REFERENCED_TABLE_NAME IS NOT NULL
ORDER BY TABLE_NAME, CONSTRAINT_NAME;
