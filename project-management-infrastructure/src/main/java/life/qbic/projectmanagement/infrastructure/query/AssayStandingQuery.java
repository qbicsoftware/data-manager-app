package life.qbic.projectmanagement.infrastructure.query;

/**
 * Project-scoped SQL for the project's assay standing: one row per primary dataset.
 *
 * <p><b>Why this is not a plain view read.</b> {@code v_project_assay_standing} computes its totals
 * with several {@code GROUP BY} subqueries. Filtering the view on an outer column
 * ({@code project_id}) is an anti-pattern the database cannot optimise away: it aggregates every
 * project, then the filter discards all but one. On a real instance that made the project page slow
 * to load. This is the same problem {@link ExperimentProgressQuery} exists to solve, and it is solved
 * the same way.</p>
 *
 * <p><b>Here the project is a parameter applied inside the derivation.</b>
 * {@code project_experiments} is the project's experiment ids, {@code project_samples} the samples of
 * those experiments, and every aggregate is built only from those. See the view's header for the
 * semantics this must stay identical to -- in particular the method resolution order
 * ({@code lcmsMethod} first, {@code sample.analysis_method} second) and the exclusion of workflow
 * scopes.</p>
 *
 * @see ExperimentProgressQuery
 * @see <a href="https://github.com/qbicsoftware/data-manager-app/issues/1660">#1660</a>
 * @since 1.22.0
 */
final class AssayStandingQuery {

  /**
   * The project-scoped form of {@code v_project_assay_standing}, parameterised by
   * {@code :projectId}.
   *
   * <p>Kept semantically identical to the view. Any change to the view's derivation must be mirrored
   * here, or the two surfaces disagree; the view remains the canonical definition and the place to
   * read the reasoning.</p>
   */
  static final String BY_PROJECT = """
      WITH project_experiments AS (
          SELECT e.id AS experiment_id, e.experimentName AS experiment_name
          FROM experiments_datamanager e
          WHERE e.project = :projectId
      ),
      project_samples AS (
          SELECT s.sample_id, s.experiment_id, s.analysis_method
          FROM sample s
                   JOIN project_experiments pe
                        ON pe.experiment_id COLLATE utf8mb4_unicode_ci = s.experiment_id
          WHERE s.analysis_method IS NOT NULL
            AND s.analysis_method <> ''
            -- Workflow scopes are not assays: no OBI term, and a sample registered as one does not
            -- produce a measurement dataset. See AnalysisMethod.isAssay().
            AND s.analysis_method NOT IN ('ISOLATION_ONLY', 'QC_ONLY', 'SEQ_ONLY')
      ),
      measurement_sample AS (
          -- measurement -> the samples it measured, plus whatever the MEASUREMENT records as its
          -- method. Only PXP and IP record one (lcmsMethod); NGS records nothing.
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
      -- Method resolution happens here, once: the measurement's own record where it has one
      -- (PXP/IP lcmsMethod), otherwise the sample's analysis_method (genomics, which records none).
      -- The COALESCE order IS the rule -- a measurement's own record always wins.
      dataset_measurements AS (
          SELECT DISTINCT ps.experiment_id,
                          ms.measurement_type,
                          COALESCE(ms.measurement_method, CAST(ps.analysis_method AS CHAR))
                              AS method_label,
                          ms.measurement_id,
                          ms.measurement_code
          FROM measurement_sample ms
                   JOIN project_samples ps ON ps.sample_id = ms.sample_id
      ),
      dataset_keys AS (
          SELECT DISTINCT experiment_id, measurement_type, method_label FROM dataset_measurements
      ),
      counted AS (
          SELECT experiment_id, measurement_type, method_label,
                 COUNT(*) AS measurements
          FROM dataset_measurements
          GROUP BY experiment_id, measurement_type, method_label
      ),
      with_data AS (
          SELECT dm.experiment_id, dm.measurement_type, dm.method_label,
                 COUNT(*) AS measurements_with_primary_data
          FROM dataset_measurements dm
                   JOIN remote_measurement_data rmd
                        ON rmd.measurement_id = dm.measurement_code COLLATE utf8mb4_unicode_ci
                            AND rmd.deleted = FALSE
          GROUP BY dm.experiment_id, dm.measurement_type, dm.method_label
      ),
      -- Bytes per (experiment, domain, method, measurementCode), keyed by code first so that two
      -- measurement records sharing one code cannot double the volume (view trap 4b).
      bytes_by_code AS (
          SELECT dm.experiment_id, dm.measurement_type, dm.method_label, dm.measurement_code,
                 MAX(rmd.total_filesize_bytes) AS code_bytes
          FROM dataset_measurements dm
                   JOIN remote_measurement_data rmd
                        ON rmd.measurement_id = dm.measurement_code COLLATE utf8mb4_unicode_ci
                            AND rmd.deleted = FALSE
          GROUP BY dm.experiment_id, dm.measurement_type, dm.method_label, dm.measurement_code
      ),
      volume AS (
          SELECT experiment_id, measurement_type, method_label,
                 COALESCE(SUM(code_bytes), 0) AS primary_data_bytes
          FROM bytes_by_code
          GROUP BY experiment_id, measurement_type, method_label
      )
      SELECT pe.experiment_id,
             :projectId              AS project_id,
             pe.experiment_name,
             k.measurement_type,
             k.method_label,
             COALESCE(c.measurements, 0)                   AS measurements,
             COALESCE(w.measurements_with_primary_data, 0) AS measurements_with_primary_data,
             COALESCE(v.primary_data_bytes, 0)             AS primary_data_bytes
      FROM dataset_keys k
               JOIN project_experiments pe
                    ON pe.experiment_id COLLATE utf8mb4_unicode_ci = k.experiment_id
               LEFT JOIN counted c
                         ON c.experiment_id COLLATE utf8mb4_unicode_ci = k.experiment_id
                             AND c.measurement_type = k.measurement_type
                             AND c.method_label COLLATE utf8mb4_unicode_ci =
                                 k.method_label COLLATE utf8mb4_unicode_ci
               LEFT JOIN with_data w
                         ON w.experiment_id COLLATE utf8mb4_unicode_ci = k.experiment_id
                             AND w.measurement_type = k.measurement_type
                             AND w.method_label COLLATE utf8mb4_unicode_ci =
                                 k.method_label COLLATE utf8mb4_unicode_ci
               LEFT JOIN volume v
                         ON v.experiment_id COLLATE utf8mb4_unicode_ci = k.experiment_id
                             AND v.measurement_type = k.measurement_type
                             AND v.method_label COLLATE utf8mb4_unicode_ci =
                                 k.method_label COLLATE utf8mb4_unicode_ci
      ORDER BY pe.experiment_name, k.measurement_type, k.method_label
      """;

  private AssayStandingQuery() {
    // static SQL holder
  }
}
