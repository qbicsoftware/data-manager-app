-- =============================================================================
-- Migration: Recent-activity read models for the experiment snapshot panels
-- Story:     TBD   (governance gate skipped at project-owner direction)
-- Feature:   TBD   (FEAT-PROJECT-*)
-- ADRs:      TBD   (read-model / query-service ADR)
--
-- Scope:     Views only. No tables, no columns.
-- Script:    sql/migrations/create-experiment-recent-activity-views.sql
-- Datasource: data_management
-- =============================================================================
--
-- PURPOSE
--
-- The project page's experiment cards carry an inline "at a glance" snapshot: the newest three
-- samples and the newest three measurements per experiment, for the Samples and Dataset panels.
--
-- These are top-N-per-group queries, which is what window functions are for. Doing them in SQL
-- keeps the page to a fixed number of queries instead of a per-experiment N+1, and keeps the
-- "newest per experiment" rule next to the other project-page read models.
--
-- Requires MariaDB 10.2+ (ROW_NUMBER window function).
--
-- SEMANTICS
--
--  * "Newest" is by last modification where a record has one, else registration:
--    samples order by COALESCE(lastModified, registrationTime); measurements have no
--    modification timestamp, so they order by registration.
--  * The sample code / measurement code is a deterministic tie-break.
--  * A measurement measuring samples of more than one experiment is attributed to each
--    experiment (see experiment_progress / D8).
--  * Primary-data columns come from remote_measurement_data, joined on the measurementCode
--    (the known drift) and excluding deleted rows.
--  * COLLATION DRIFT: every string equality pins utf8mb4_unicode_ci on one operand. See
--    the long note in create-project-page-read-models.sql (trap 5): instances have been
--    found mixing latin1_swedish_ci, utf8mb4_general_ci and utf8mb4_unicode_ci, and a
--    column-to-column compare across them raises "Illegal mix of collations".
--
-- CONSUMPTION
--
-- Read-only. Map to flat @Immutable entities (no associations), per the reactive boundary
-- policy (strategy §4.1).
-- =============================================================================


-- -----------------------------------------------------------------------------
-- 1. Newest three samples per experiment
-- -----------------------------------------------------------------------------
CREATE OR REPLACE VIEW experiment_recent_sample AS
SELECT id,
       project_id,
       experiment_id,
       sample_id,
       sample_code,
       sample_name,
       changed
FROM (
    SELECT s.sample_id                                     AS id,
           s.project_id,
           s.experiment_id,
           s.sample_id,
           s.code                                          AS sample_code,
           s.label                                         AS sample_name,
           COALESCE(s.lastModified, s.registrationTime)    AS changed,
           ROW_NUMBER() OVER (
               PARTITION BY s.experiment_id
               ORDER BY COALESCE(s.lastModified, s.registrationTime) DESC, s.code ASC
           )                                               AS rn
    FROM sample s
    WHERE s.experiment_id IS NOT NULL
) ranked
WHERE rn <= 3;


-- -----------------------------------------------------------------------------
-- 2. Newest three measurements per experiment, with primary-data status
-- -----------------------------------------------------------------------------
CREATE OR REPLACE VIEW experiment_recent_measurement AS
SELECT id,
       project_id,
       experiment_id,
       measurement_type,
       measurement_id,
       measurement_code,
       measurement_name,
       primary_data_files,
       primary_data_bytes,
       changed
FROM (
    SELECT CONCAT(x.experiment_id, '|', x.measurement_type, '|', x.measurement_id) AS id,
           x.project_id,
           x.experiment_id,
           x.measurement_type,
           x.measurement_id,
           x.measurement_code,
           x.measurement_name,
           rmd.file_count                                                         AS primary_data_files,
           rmd.total_filesize_bytes                                               AS primary_data_bytes,
           x.changed,
           ROW_NUMBER() OVER (
               PARTITION BY x.experiment_id
               ORDER BY x.changed DESC, x.measurement_code ASC
           )                                                                      AS rn
    FROM (
        SELECT 'NGS' AS measurement_type,
               m.measurement_id,
               m.measurementCode AS measurement_code,
               m.measurementName AS measurement_name,
               m.registrationTime AS changed,
               s.experiment_id,
               s.project_id
        FROM ngs_measurements m
                 JOIN specific_measurement_metadata_ngs smm ON smm.measurement_id = m.measurement_id COLLATE utf8mb4_unicode_ci
                 JOIN sample s ON s.sample_id = smm.sample_id COLLATE utf8mb4_unicode_ci
        WHERE s.experiment_id IS NOT NULL
        UNION
        SELECT 'PXP',
               p.measurement_id,
               p.measurementCode,
               p.measurementName,
               p.registration,
               s.experiment_id,
               s.project_id
        FROM proteomics_measurement p
                 JOIN specific_measurement_metadata_pxp smm ON smm.measurement_id = p.measurement_id COLLATE utf8mb4_unicode_ci
                 JOIN sample s ON s.sample_id = smm.sample_id COLLATE utf8mb4_unicode_ci
        WHERE s.experiment_id IS NOT NULL
        UNION
        SELECT 'IP',
               ip.measurement_id,
               ip.measurementCode,
               ip.measurementName,
               ip.registrationTime,
               s.experiment_id,
               s.project_id
        FROM ip_measurements ip
                 JOIN specific_measurement_metadata_ip smm ON smm.measurement_id = ip.measurement_id COLLATE utf8mb4_unicode_ci
                 JOIN sample s ON s.sample_id = smm.sample_id COLLATE utf8mb4_unicode_ci
        WHERE s.experiment_id IS NOT NULL
    ) x
             LEFT JOIN remote_measurement_data rmd
                       ON rmd.measurement_id = x.measurement_code COLLATE utf8mb4_unicode_ci AND rmd.deleted = FALSE
) ranked
WHERE rn <= 3;


-- -----------------------------------------------------------------------------
-- Verification (read-only)
-- -----------------------------------------------------------------------------
-- SELECT experiment_id, COUNT(*) FROM experiment_recent_sample GROUP BY experiment_id;  -- <= 3
-- SELECT experiment_id, COUNT(*) FROM experiment_recent_measurement GROUP BY experiment_id;  -- <= 3


-- -----------------------------------------------------------------------------
-- Rollback
-- -----------------------------------------------------------------------------
-- DROP VIEW IF EXISTS experiment_recent_measurement;
-- DROP VIEW IF EXISTS experiment_recent_sample;
