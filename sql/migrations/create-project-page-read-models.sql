-- =============================================================================
-- Migration: Project page read models (progress, dataset rollup, sample overview)
-- Story:     TBD   (governance gate skipped at project-owner direction)
-- Feature:   TBD   (FEAT-PROJECT-*)
-- ADRs:      TBD   (read-model / query-service ADR; reactive boundary ADR)
--
-- Scope:     Views only (+ one supporting index). No tables, no dropped columns.
-- Script:    sql/migrations/create-project-page-read-models.sql
-- Datasource: data_management
-- =============================================================================
--
-- PURPOSE
--
-- The new project page needs three read models that do not exist yet. All three
-- are projections over existing tables. They exist so the landing page renders
-- from a small, fixed number of queries instead of composing ~7 per-experiment
-- lookups (the N+1 the current service layer would force).
--
--   experiment_progress        one row per experiment: samples, measurements
--                              (per type), measurements with primary data
--                              (total and per type) and primary-data bytes
--                              (total and per type). Serves BOTH the summary
--                              progress band and the project Datasets overview.
--   v_project_sample_overview  one row per sample in a project, with experiment
--                              attribution, condition and Measured/Not measured.
--   v_measurement_experiment   helper: maps a measurement to the experiment(s)
--                              it belongs to, via its measured samples.
--
-- WHY VIEWS, NOT TABLES
--
-- MariaDB has no materialised views, and the data volumes are small (research
-- projects). A view is always consistent and adds no write path. If profiling
-- later shows the landing page is slow, the same projection can be turned into
-- an event-maintained summary table without changing the read-model shape.
--
-- KNOWN TRAPS (must stay visible to reviewers)
--
--  1. remote_measurement_data.measurement_id does NOT hold the measurement PK.
--     It holds the measurementCode. The existing v_*_measurement_sample_json
--     views join `rmd.measurement_id = m.measurementCode`. The join below does
--     the same. The column name is a drift; do not "fix" it by joining the PK.
--
--  2. The existing JSON views INNER JOIN remote_measurement_data, so they only
--     contain measurements that already have data. "Measurements without primary
--     data" is therefore measurement_count - measurements_with_primary_data.
--
--  3. A measurement can, in principle, measure samples from more than one
--     experiment (the JSON views surface this as experiment_ids / NULL). Such a
--     measurement is attributed to each experiment it touches and therefore
--     counted in each. This is documented, not accidental. The 1:1 dataset/
--     experiment assumption in the plan is a product decision, not a DB rule.
--
--  4. deleted rows in remote_measurement_data are excluded (deleted = FALSE).
--
--  4b. A measurementCode is assumed unique per measurement record, but the database does not
--      enforce it: a real instance had two measurement rows sharing one code. Primary-data
--      VOLUME is therefore summed per (experiment, measurementCode) -- the grain
--      remote_measurement_data is actually keyed by -- while the COUNT of measurements with
--      primary data counts measurement records. Counting volume per record would double it.
--
--  4c. NO FILE COUNT IS EXPOSED, on purpose. remote_measurement_data.file_count exists, but a
--      file count is an implementation artifact: it is not comparable across assay types (one
--      NGS measurement is a couple of large files, one proteomics measurement is hundreds of
--      small ones), it cannot be acted on, and pairing it with a byte total invites the reader
--      to divide the two. A published mock-up read "0.0 GB in 24 files" -- digits that look
--      fine and describe nothing -- which is exactly the failure mode to avoid.
--
--  4d. SIZE CAN LEGITIMATELY BE 0. total_filesize_bytes is 0 for many synced measurements: a
--      real project had 24 primary-data files across 8 measurements and 0 total bytes. That
--      means the sizes were never recorded, NOT that the arithmetic failed. Consumers must
--      therefore distinguish "0 bytes" from "unmeasured" and say so, rather than printing
--      "0 B". The summaries above cannot express that distinction -- the column is 0 either
--      way -- so it is the caller's responsibility.
--
--  5. COLLATION DRIFT. The hand-written DDL pins every app table to
--     utf8mb4_unicode_ci, but tables created outside it (Hibernate ddl-auto, or an
--     instance whose server default is utf8mb4_general_ci) end up on
--     utf8mb4_general_ci. A column-to-column comparison across them raises
--     "Illegal mix of collations". A real instance was found carrying all three of
--     latin1_swedish_ci (sample, ngs_measurements, specific_measurement_metadata_ngs,
--     proteomics_measurement), utf8mb4_general_ci (experiments_datamanager,
--     experimental_group) and utf8mb4_unicode_ci (projects_datamanager,
--     remote_measurement_data, associated_dataset) -- often within one join chain.
--     EVERY string equality below therefore pins utf8mb4_unicode_ci on one operand,
--     so the comparison is decided by the explicit collation rather than by whichever
--     collations the two columns happen to carry. latin1 converts up to utf8mb4, so
--     this is lossless. Aligning the instance (ALTER TABLE ... CONVERT TO CHARACTER
--     SET utf8mb4 COLLATE utf8mb4_unicode_ci) is still the right long-term fix; this
--     makes the read models not depend on it, and it is harmless once aligned.
--
-- CONSUMPTION
--
-- Read-only. Mapping follows the established ProjectOverview pattern: a flat
-- @Immutable @Entity over the view is acceptable because it carries no lazy
-- associations, so it is fully hydrated before any scheduler thread hop. Do NOT
-- add associations to a view-backed read entity. Detached records / JdbcClient
-- are the fallback for any non-flat shape (reactive boundary policy, §4.1).
-- =============================================================================


-- -----------------------------------------------------------------------------
-- 1. Helper: measurement -> experiment mapping
-- -----------------------------------------------------------------------------
-- A measurement is attributed to an experiment through the samples it measured
-- (measurement -> specific_measurement_metadata_* -> sample -> experiment).
-- DISTINCT (implicit, via UNION) collapses the sample fan-out, so there is one
-- row per (measurement_type, measurement_id, experiment).
-- -----------------------------------------------------------------------------
CREATE OR REPLACE VIEW v_measurement_experiment AS
SELECT 'NGS' AS measurement_type,
       m.measurement_id,
       m.measurementCode AS measurement_code,
       m.projectId       AS project_id,
       s.experiment_id
FROM ngs_measurements m
         JOIN specific_measurement_metadata_ngs smm ON smm.measurement_id = m.measurement_id COLLATE utf8mb4_unicode_ci
         JOIN sample s ON s.sample_id = smm.sample_id COLLATE utf8mb4_unicode_ci
WHERE s.experiment_id IS NOT NULL
UNION
SELECT 'PXP',
       p.measurement_id,
       p.measurementCode,
       p.projectId,
       s.experiment_id
FROM proteomics_measurement p
         JOIN specific_measurement_metadata_pxp smm ON smm.measurement_id = p.measurement_id COLLATE utf8mb4_unicode_ci
         JOIN sample s ON s.sample_id = smm.sample_id COLLATE utf8mb4_unicode_ci
WHERE s.experiment_id IS NOT NULL
UNION
SELECT 'IP',
       ip.measurement_id,
       ip.measurementCode,
       ip.projectId,
       s.experiment_id
FROM ip_measurements ip
         JOIN specific_measurement_metadata_ip smm ON smm.measurement_id = ip.measurement_id COLLATE utf8mb4_unicode_ci
         JOIN sample s ON s.sample_id = smm.sample_id COLLATE utf8mb4_unicode_ci
WHERE s.experiment_id IS NOT NULL;


-- -----------------------------------------------------------------------------
-- 2. Primary read model: per-experiment progress / dataset rollup
-- -----------------------------------------------------------------------------
CREATE OR REPLACE VIEW experiment_progress AS
SELECT e.id                                           AS experiment_id,
       e.project                                      AS project_id,
       e.experimentName                               AS experiment_name,

       -- Samples
       COALESCE(smp.sample_count, 0)                  AS sample_count,
       COALESCE(smp.measured_sample_count, 0)         AS measured_sample_count,

       -- Measurement metadata registered, per type
       COALESCE(me.measurement_count, 0)              AS measurement_count,
       COALESCE(me.ngs_measurement_count, 0)          AS ngs_measurement_count,
       COALESCE(me.pxp_measurement_count, 0)          AS pxp_measurement_count,
       COALESCE(me.ip_measurement_count, 0)           AS ip_measurement_count,

       -- Primary data (measurement data attached; deleted excluded). Counted in total and per
       -- measurement type: the project page states coverage as "N of M measurements", and the
       -- volume belongs per assay type, because a project total alone says nothing about where
       -- the mass sits. File counts are deliberately absent -- see trap 4b.
       COALESCE(pd_count.measurements_with_primary_data, 0)     AS measurements_with_primary_data,
       COALESCE(pd_count.ngs_measurements_with_primary_data, 0) AS ngs_measurements_with_primary_data,
       COALESCE(pd_count.pxp_measurements_with_primary_data, 0) AS pxp_measurements_with_primary_data,
       COALESCE(pd_count.ip_measurements_with_primary_data, 0)  AS ip_measurements_with_primary_data,
       COALESCE(pd_volume.primary_data_bytes, 0)                AS primary_data_bytes,
       COALESCE(pd_volume.ngs_primary_data_bytes, 0)            AS ngs_primary_data_bytes,
       COALESCE(pd_volume.pxp_primary_data_bytes, 0)            AS pxp_primary_data_bytes,
       COALESCE(pd_volume.ip_primary_data_bytes, 0)             AS ip_primary_data_bytes
FROM experiments_datamanager e
         LEFT JOIN (
    SELECT s.experiment_id,
           COUNT(*)            AS sample_count,
           COUNT(ms.sample_id) AS measured_sample_count
    FROM sample s
             LEFT JOIN (
        SELECT sample_id FROM specific_measurement_metadata_ngs
        UNION
        SELECT sample_id FROM specific_measurement_metadata_pxp
        UNION
        SELECT sample_id FROM specific_measurement_metadata_ip
    ) ms ON ms.sample_id = s.sample_id COLLATE utf8mb4_unicode_ci
    WHERE s.experiment_id IS NOT NULL
    GROUP BY s.experiment_id
) smp ON smp.experiment_id COLLATE utf8mb4_unicode_ci = e.id
         LEFT JOIN (
    SELECT ve.experiment_id,
           COUNT(DISTINCT ve.measurement_type, ve.measurement_id) AS measurement_count,
           COUNT(DISTINCT CASE WHEN ve.measurement_type = 'NGS' THEN ve.measurement_id END) AS ngs_measurement_count,
           COUNT(DISTINCT CASE WHEN ve.measurement_type = 'PXP' THEN ve.measurement_id END) AS pxp_measurement_count,
           COUNT(DISTINCT CASE WHEN ve.measurement_type = 'IP'  THEN ve.measurement_id END) AS ip_measurement_count
    FROM v_measurement_experiment ve
    GROUP BY ve.experiment_id
) me ON me.experiment_id COLLATE utf8mb4_unicode_ci = e.id
         -- How many measurement records carry primary data. Counted per measurement record,
         -- because that is what the rail and the experiment card state.
         LEFT JOIN (
    SELECT ve.experiment_id,
           COUNT(DISTINCT ve.measurement_type, ve.measurement_id) AS measurements_with_primary_data,
           COUNT(DISTINCT CASE WHEN ve.measurement_type COLLATE utf8mb4_unicode_ci = 'NGS'
                               THEN ve.measurement_id END)        AS ngs_measurements_with_primary_data,
           COUNT(DISTINCT CASE WHEN ve.measurement_type COLLATE utf8mb4_unicode_ci = 'PXP'
                               THEN ve.measurement_id END)        AS pxp_measurements_with_primary_data,
           COUNT(DISTINCT CASE WHEN ve.measurement_type COLLATE utf8mb4_unicode_ci = 'IP'
                               THEN ve.measurement_id END)        AS ip_measurements_with_primary_data
    FROM v_measurement_experiment ve
             JOIN remote_measurement_data rmd
                  ON rmd.measurement_id = ve.measurement_code COLLATE utf8mb4_unicode_ci
                      AND rmd.deleted = FALSE
    GROUP BY ve.experiment_id
) pd_count ON pd_count.experiment_id COLLATE utf8mb4_unicode_ci = e.id
         -- How much primary data there is. remote_measurement_data is keyed by the
         -- measurementCode, so it is summed once per (experiment, code): two measurement
         -- records sharing one code must not double the volume. Such duplicates exist in the
         -- wild (two codes in a real instance had two measurement rows each), and while they
         -- happen to be unattributed today, counting them per record would silently inflate
         -- the file and byte totals the moment they are attributed.
         LEFT JOIN (
    SELECT codes.experiment_id,
           COALESCE(SUM(rmd.total_filesize_bytes), 0) AS primary_data_bytes,
           COALESCE(SUM(CASE WHEN codes.measurement_type COLLATE utf8mb4_unicode_ci = 'NGS'
                             THEN rmd.total_filesize_bytes END), 0) AS ngs_primary_data_bytes,
           COALESCE(SUM(CASE WHEN codes.measurement_type COLLATE utf8mb4_unicode_ci = 'PXP'
                             THEN rmd.total_filesize_bytes END), 0) AS pxp_primary_data_bytes,
           COALESCE(SUM(CASE WHEN codes.measurement_type COLLATE utf8mb4_unicode_ci = 'IP'
                             THEN rmd.total_filesize_bytes END), 0) AS ip_primary_data_bytes
    FROM (SELECT DISTINCT experiment_id, measurement_code, measurement_type FROM v_measurement_experiment) codes
             JOIN remote_measurement_data rmd
                  ON rmd.measurement_id = codes.measurement_code COLLATE utf8mb4_unicode_ci
                      AND rmd.deleted = FALSE
    GROUP BY codes.experiment_id
) pd_volume ON pd_volume.experiment_id COLLATE utf8mb4_unicode_ci = e.id;


-- -----------------------------------------------------------------------------
-- 3. Project-level sample overview (project Samples page)
-- -----------------------------------------------------------------------------
-- One row per sample in a project, with everything the flat, paginated,
-- read-only list needs: identity, experiment attribution, condition, status.
-- `measured` is the sample's lifecycle for the list's status column.
-- -----------------------------------------------------------------------------
CREATE OR REPLACE VIEW v_project_sample_overview AS
SELECT s.project_id,
       s.experiment_id,
       e.experimentName      AS experiment_name,
       s.sample_id,
       s.code                AS sample_code,
       s.label               AS sample_name,
       s.batch,
       s.experimentalGroupId AS experimental_group_id,
       g.name                AS experimental_group_name,
       s.analysis_method,
       s.species,
       s.specimen,
       s.analyte,
       s.registrationTime    AS registration_time,
       s.lastModified        AS last_modified,
       CASE
           WHEN EXISTS (SELECT 1 FROM specific_measurement_metadata_ngs x WHERE x.sample_id = s.sample_id COLLATE utf8mb4_unicode_ci)
               OR EXISTS (SELECT 1 FROM specific_measurement_metadata_pxp x WHERE x.sample_id = s.sample_id COLLATE utf8mb4_unicode_ci)
               OR EXISTS (SELECT 1 FROM specific_measurement_metadata_ip x WHERE x.sample_id = s.sample_id COLLATE utf8mb4_unicode_ci)
               THEN TRUE
           ELSE FALSE
           END                AS measured
FROM sample s
         LEFT JOIN experiments_datamanager e
                   ON e.id COLLATE utf8mb4_unicode_ci = s.experiment_id
         LEFT JOIN experimental_group g
                   ON g.experimentalGroupId COLLATE utf8mb4_unicode_ci = s.experimentalGroupId
WHERE s.project_id IS NOT NULL;


-- -----------------------------------------------------------------------------
-- 4. Supporting index
-- -----------------------------------------------------------------------------
-- `sample` has no index on experiment_id (only the PK and the experimental_group
-- FK). The per-experiment sample aggregation groups by experiment_id, so add one.
-- Guarded, so the script is idempotent. `sample.project_id` is already indexed by
-- the FK added in finalize-sample-batch-removal.sql.
-- -----------------------------------------------------------------------------
SET @has_idx := (SELECT COUNT(*)
                 FROM information_schema.STATISTICS
                 WHERE TABLE_SCHEMA = DATABASE()
                   AND TABLE_NAME = 'sample'
                   AND INDEX_NAME = 'idx_sample_experiment');
SET @sql := IF(@has_idx = 0,
    'CREATE INDEX `idx_sample_experiment` ON `sample` (`experiment_id`)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;


-- -----------------------------------------------------------------------------
-- 5. Verification (run before commit; read-only)
-- -----------------------------------------------------------------------------
-- Expected: one row per experiment; sample_count and measurement_count >= 0;
-- measurements_with_primary_data <= measurement_count.
-- SELECT * FROM experiment_progress ORDER BY project_id, experiment_name LIMIT 20;

-- Expected: samples of a project, measured flag present.
-- SELECT experiment_name, sample_code, measured
--   FROM v_project_sample_overview WHERE project_id = ? LIMIT 20;

-- Expected: no measurement attributed to more than one experiment in a single
-- project (if this returns rows, the mixed-experiment case in trap 3 applies).
-- SELECT measurement_type, measurement_id, COUNT(DISTINCT experiment_id) AS n
--   FROM v_measurement_experiment
--  GROUP BY measurement_type, measurement_id HAVING n > 1;


-- -----------------------------------------------------------------------------
-- Rollback
-- -----------------------------------------------------------------------------
-- DROP VIEW IF EXISTS experiment_progress;
-- DROP VIEW IF EXISTS v_project_sample_overview;
-- DROP VIEW IF EXISTS v_measurement_experiment;
-- DROP INDEX idx_sample_experiment ON sample;
