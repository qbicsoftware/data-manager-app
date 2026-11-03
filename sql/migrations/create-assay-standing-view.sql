-- =============================================================================
-- Migration: Assay standing read model (one row per primary dataset)
-- Story:     TBD   (governance gate skipped at project-owner direction)
-- Feature:   TBD   (FEAT-PROJECT-*)
-- ADRs:      0010 (analysis-method attribution), 0007 (read-model conventions)
--
-- Scope:     Views only. No tables, no columns, no dropped objects.
-- Script:    sql/migrations/create-assay-standing-view.sql
-- Datasource: data_management
-- =============================================================================
--
-- PURPOSE
--
-- The project landing page and the project Datasets page must both state which primary datasets a
-- project holds and how far along each is. A primary dataset is the data of ONE EXPERIMENT for ONE
-- ANALYSIS METHOD. It is not declared by a human being: it is the consequence of measurements
-- having been done, and it does not exist before there is data to put in it.
--
-- The analysis method is the dataset's type because that is what a reader selects datasets by when
-- planning an analysis (variant calling, cluster analysis, ...). The omics domain (genomics /
-- proteomics / immunopeptidomics) is far too coarse for that: WES, RNA-Seq and 16S are all
-- genomics and are not interchangeable.
--
-- This view exists so both surfaces read one derivation. Two derivations drift, and a project that
-- holds "2 WES datasets" on one page must not hold "1 Genomics dataset" on another.
--
-- Requires MariaDB 10.2+ (CTEs, window-free but WITH clause).
--
-- =============================================================================
-- METHOD RESOLUTION (read before changing this view)
-- =============================================================================
--
-- The method label of a dataset is resolved per measurement, in this order:
--
--   1. the MEASUREMENT's own record    (PXP / IP: lcmsMethod)
--   2. the SAMPLE's analysis_method    (genomics: the measurement records none)
--
-- This mirrors where the fact actually lives today, and it is an interim shape: the sample-level
-- annotation is a known misfiling (see the grandfather note below), and genomics records no method on
-- the measurement at all -- the NGS registration template has no method column. What is needed is a
-- controlled method vocabulary captured on the measurement in every domain; until then this COALESCE
-- is the least-wrong answer, and the label it produces is free text for PXP/IP.
--
-- The COALESCE order IS the rule. Do not reorder it: a measurement's own record always wins over the
-- sample's annotation, because the measurement is what was done.
--
-- =============================================================================
-- THE GRANDFATHER RELATION (read before changing this view)
-- =============================================================================
--
-- The analysis method is currently annotated on the SAMPLE (sample.analysis_method), not on the
-- measurement. That is the wrong home: one extract can legitimately yield several analysis methods,
-- so the method is a property of the measurement. It cannot simply be moved, because:
--
--   1. it is populated for existing projects and is their only source;
--   2. moving it creates a period with the information in TWO places;
--   3. the sample annotation can never be deleted without destroying the record of projects
--      registered before the move.
--
-- Therefore the sample annotation is a PERMANENTLY SUPPORTED source -- a grandfather relation, not
-- a temporary hack -- and this view is the ONE place allowed to read it to type a dataset
-- (see ADR 0010). When the measurement carries its own annotation, the resolution changes HERE
-- (prefer the measurement, fall back to the sample) and no consumer changes.
--
-- DO NOT resolve the analysis method from sample.analysis_method anywhere else. A second call site
-- doubles the migration surface and lets two surfaces disagree.
--
-- =============================================================================
-- SEMANTICS
-- =============================================================================
--
--  * Grain: one row per (experiment_id, measurement_type, method_label). The measurement type is the
--    omics domain; the method label is resolved as described above and may be free text.
--    Two experiments measured as WES are two datasets; one experiment measured as WES and RNA-Seq
--    is also two.
--
--  * Analysis-method attribution goes through the samples a measurement measured
--    (measurement -> specific_measurement_metadata_* -> sample). Verified against the same join
--    the other project-page read models use.
--
--  * THE DOMAIN COMES FROM THE MEASUREMENT, NOT THE SAMPLE. This is the one thing about this view
--    that cannot be inferred from analysis_method: proteomics and immunopeptidomics share the same
--    analysis method on the sample, and what distinguishes them is what was recorded on the
--    measurement. So `measurement_type` -- which table the measurement lives in, NGS / PXP / IP --
--    is the ONLY reliable source for the omics domain, and it is carried as its own column.
--
--    Deriving the domain from analysis_method (an earlier revision of this view did exactly that)
--    silently reported immunopeptidomics measurements as proteomics, because both samples carry the
--    same analysis method. Never infer the domain from the analysis method.
--
--  * AMBIGUOUS ATTRIBUTION. A measurement can measure samples carrying DIFFERENT analysis methods.
--    Such a measurement is attributed to EACH of them and is therefore counted in each.
--    Consequence, which must be documented wherever these numbers are shown: the per-dataset
--    measurement counts of a project CAN SUM TO MORE THAN the number of measurement records.
--    This is not an error. It follows the precedent already documented for measurements spanning
--    experiments (trap 3 in create-project-page-read-models.sql).
--
--  * Samples registered with a WORKFLOW SCOPE rather than an assay (ISOLATION-ONLY, QC-ONLY,
--    SEQ-ONLY) are excluded. None of the three has an OBI term; they describe which steps are
--    performed, not what was measured, so they cannot type a dataset. See AnalysisMethod.isAssay().
--
--  * Samples with NULL/blank analysis_method AND no measurement method are EXCLUDED rather than
--    grouped into an "unknown"
--    dataset. A dataset that cannot be typed cannot be chosen for an analysis, and materialising an
--    "unknown" dataset would state one that does not exist. Consequence: such measurements are
--    counted in the per-domain progress figures (experiment_progress) but not here. The two figures
--    answer different questions.
--
--  * measurements counts measurement RECORDS; measurements_with_primary_data counts those carrying
--    remote data (deleted = FALSE). primary_data_bytes is summed once per
--    (experiment, analysis_method, measurementCode), so two measurement records sharing one
--    measurementCode cannot double the volume (see trap 4b in create-project-page-read-models.sql).
--
--  * remote_measurement_data.measurement_id holds the measurementCode, NOT the measurement PK --
--    the documented column-name drift (trap 1). Do not "fix" the join below.
--
--  * Every string equality pins utf8mb4_unicode_ci on one operand to survive the documented
--    collation drift.
--
-- =============================================================================
-- ROLLBACK
-- =============================================================================
--
--   DROP VIEW IF EXISTS v_project_assay_standing;
--
-- Non-destructive: no table or column is touched, so dropping the view restores the previous state
-- exactly.
-- =============================================================================

CREATE OR REPLACE VIEW v_project_assay_standing AS
WITH project_samples AS (
    -- Every project's samples, with the analysis method that types their dataset. No project filter
    -- here: the view is project-agnostic and callers filter on project_id, so one derivation serves
    -- every surface.
    --
    -- ISOLATION-ONLY / QC-ONLY / SEQ-ONLY are EXCLUDED: they name a workflow scope, not an assay.
    -- None of the three has an OBI term (domain experts, terminology table on #1660), and a sample
    -- registered as one of them does not produce a measurement dataset -- grouping it would invent a
    -- dataset that is not a dataset.
    SELECT s.sample_id,
           s.project_id,
           s.experiment_id,
           s.analysis_method
    FROM sample s
    WHERE s.experiment_id IS NOT NULL
      AND s.analysis_method IS NOT NULL
      AND s.analysis_method <> ''
      AND s.analysis_method NOT IN ('ISOLATION_ONLY', 'QC_ONLY', 'SEQ_ONLY')
),
measurement_sample AS (
    -- measurement -> the samples it measured, plus whatever the MEASUREMENT itself records as its
    -- method. Only PXP and IP record one (lcmsMethod); NGS records nothing, so its method has to come
    -- from the sample. See the METHOD RESOLUTION note in the header.
    SELECT smm.measurement_id, m.measurementCode AS measurement_code, 'NGS' AS measurement_type,
           CAST(NULL AS CHAR) AS measurement_method,
           ps.sample_id
    FROM ngs_measurements m
             JOIN specific_measurement_metadata_ngs smm
                  ON smm.measurement_id = m.measurement_id COLLATE utf8mb4_unicode_ci
             JOIN project_samples ps
                  ON ps.sample_id = smm.sample_id COLLATE utf8mb4_unicode_ci
    UNION
    SELECT smm.measurement_id, p.measurementCode, 'PXP',
           NULLIF(TRIM(p.lcmsMethod), ''),
           ps.sample_id
    FROM proteomics_measurement p
             JOIN specific_measurement_metadata_pxp smm
                  ON smm.measurement_id = p.measurement_id COLLATE utf8mb4_unicode_ci
             JOIN project_samples ps
                  ON ps.sample_id = smm.sample_id COLLATE utf8mb4_unicode_ci
    UNION
    SELECT smm.measurement_id, ip.measurementCode, 'IP',
           NULLIF(TRIM(ip.lcmsMethod), ''),
           ps.sample_id
    FROM ip_measurements ip
             JOIN specific_measurement_metadata_ip smm
                  ON smm.measurement_id = ip.measurement_id COLLATE utf8mb4_unicode_ci
             JOIN project_samples ps
                  ON ps.sample_id = smm.sample_id COLLATE utf8mb4_unicode_ci
),
-- One row per (experiment, analysis method, measurement record). DISTINCT collapses the sample
-- fan-out: a measurement measuring ten samples of one method is still one measurement.
-- The method label is resolved here, once: the measurement's own record where it has one (PXP/IP
-- lcmsMethod), otherwise the sample's analysis_method (genomics, which records none).
-- COALESCE order IS the rule -- do not reorder it.
dataset_measurements AS (
    SELECT DISTINCT ps.project_id,
                    ps.experiment_id,
                    ps.analysis_method,
                    ms.measurement_type,
                    COALESCE(ms.measurement_method,
                             CAST(ps.analysis_method AS CHAR))      AS method_label,
                    ms.measurement_id,
                    ms.measurement_code
    FROM measurement_sample ms
             JOIN project_samples ps ON ps.sample_id = ms.sample_id
),
-- The dataset key is (experiment, measurement_type, analysis_method): the measurement type is the
-- omics domain and is the only reliable one -- proteomics and immunopeptidomics share an analysis
-- method on the sample, so the method alone cannot name the domain.
dataset_keys AS (
    SELECT DISTINCT project_id, experiment_id, measurement_type, method_label
    FROM dataset_measurements
),
-- Measurement records per dataset.
counted AS (
    SELECT project_id, experiment_id, measurement_type, method_label,
           COUNT(*) AS measurements
    FROM dataset_measurements
    GROUP BY project_id, experiment_id, measurement_type, method_label
),
-- Measurement records carrying primary data, per dataset.
with_data AS (
    SELECT dm.project_id, dm.experiment_id, dm.measurement_type, dm.method_label,
           COUNT(*) AS measurements_with_primary_data
    FROM dataset_measurements dm
             JOIN remote_measurement_data rmd
                  ON rmd.measurement_id = dm.measurement_code COLLATE utf8mb4_unicode_ci
                      AND rmd.deleted = FALSE
    GROUP BY dm.project_id, dm.experiment_id, dm.measurement_type, dm.method_label
),
-- Bytes per (experiment, analysis method, measurementCode), keyed by code first so that two
-- measurement records sharing one code cannot double the volume.
bytes_by_code AS (
    SELECT dm.project_id, dm.experiment_id, dm.measurement_type, dm.method_label,
           dm.measurement_code,
           MAX(rmd.total_filesize_bytes) AS code_bytes
    FROM dataset_measurements dm
             JOIN remote_measurement_data rmd
                  ON rmd.measurement_id = dm.measurement_code COLLATE utf8mb4_unicode_ci
                      AND rmd.deleted = FALSE
    GROUP BY dm.project_id, dm.experiment_id, dm.measurement_type, dm.method_label,
             dm.measurement_code
),
volume AS (
    SELECT project_id, experiment_id, measurement_type, method_label,
           COALESCE(SUM(code_bytes), 0) AS primary_data_bytes
    FROM bytes_by_code
    GROUP BY project_id, experiment_id, measurement_type, method_label
)
SELECT k.project_id,
       k.experiment_id,
       e.experimentName                               AS experiment_name,
       k.measurement_type,
       k.method_label,
       COALESCE(c.measurements, 0)                    AS measurements,
       COALESCE(w.measurements_with_primary_data, 0)  AS measurements_with_primary_data,
       COALESCE(v.primary_data_bytes, 0)              AS primary_data_bytes
FROM dataset_keys k
         JOIN experiments_datamanager e
              ON e.id COLLATE utf8mb4_unicode_ci = k.experiment_id
         LEFT JOIN counted c
                   ON c.project_id = k.project_id
                       AND c.experiment_id COLLATE utf8mb4_unicode_ci = k.experiment_id
                       AND c.measurement_type = k.measurement_type
                       AND c.method_label COLLATE utf8mb4_unicode_ci =
                           k.method_label COLLATE utf8mb4_unicode_ci
         LEFT JOIN with_data w
                   ON w.project_id = k.project_id
                       AND w.experiment_id COLLATE utf8mb4_unicode_ci = k.experiment_id
                       AND w.measurement_type = k.measurement_type
                       AND w.method_label COLLATE utf8mb4_unicode_ci =
                           k.method_label COLLATE utf8mb4_unicode_ci
         LEFT JOIN volume v
                   ON v.project_id = k.project_id
                       AND v.experiment_id COLLATE utf8mb4_unicode_ci = k.experiment_id
                       AND v.measurement_type = k.measurement_type
                       AND v.method_label COLLATE utf8mb4_unicode_ci =
                           k.method_label COLLATE utf8mb4_unicode_ci;

-- Supporting index: the view's hot path is the sample -> experiment -> analysis_method join.
-- `sample(experiment_id)` already exists (create-project-page-read-models.sql); this one keeps the
-- project-scoped filter from scanning all samples of all projects.
SET @index_exists := (SELECT COUNT(*)
                      FROM information_schema.statistics
                      WHERE table_schema = DATABASE()
                        AND table_name = 'sample'
                        AND index_name = 'idx_sample_project_experiment');
SET @ddl := IF(@index_exists = 0,
               'CREATE INDEX idx_sample_project_experiment ON sample (project_id, experiment_id, analysis_method)',
               'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
