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
 * semantics this must stay identical to -- in particular that the method label is
 * {@code sample.analysis_method} (a controlled enum) and that workflow scopes are excluded.</p>
 *
 * <p><b>The method label is not {@code lcmsMethod}.</b> That column is free text with no controlled
 * vocabulary, so grouping by it fragments one method across spellings and produces several rows for
 * one dataset. The PXP/IP label comes from {@code instrument} instead, which is an
 * {@code OntologyTerm} -- a resolved vocabulary term, not a typed string. Where no instrument was
 * recorded, the sample's {@code analysis_method} (an enum) is used. Both are controlled; that is the
 * requirement for a grouping key. See #1660.</p>
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
          -- measurement -> the samples it measured, plus the instrument's vocabulary label where the
          -- measurement records one (PXP / IP). NGS records none: its assays live in the sample
          -- annotation. The NULL is explicitly collated so the UNION does not fail on mixed
          -- collations.
          SELECT smm.measurement_id, m.measurementCode AS measurement_code, 'NGS' AS measurement_type,
                 CAST(NULL AS CHAR CHARACTER SET utf8mb4) COLLATE utf8mb4_unicode_ci
                     AS measurement_method,
                 ps.sample_id
          FROM ngs_measurements m
                   JOIN specific_measurement_metadata_ngs smm
                        ON smm.measurement_id = m.measurement_id COLLATE utf8mb4_unicode_ci
                   JOIN project_samples ps
                        ON ps.sample_id = smm.sample_id COLLATE utf8mb4_unicode_ci
          UNION
          SELECT smm.measurement_id, p.measurementCode, 'PXP',
                 COALESCE(JSON_UNQUOTE(JSON_EXTRACT(p.instrument, '$.label')),
                          JSON_UNQUOTE(JSON_EXTRACT(p.instrument, '$.name')))
                     COLLATE utf8mb4_unicode_ci,
                 ps.sample_id
          FROM proteomics_measurement p
                   JOIN specific_measurement_metadata_pxp smm
                        ON smm.measurement_id = p.measurement_id COLLATE utf8mb4_unicode_ci
                   JOIN project_samples ps
                        ON ps.sample_id = smm.sample_id COLLATE utf8mb4_unicode_ci
          UNION
          SELECT smm.measurement_id, ip.measurementCode, 'IP',
                 COALESCE(JSON_UNQUOTE(JSON_EXTRACT(ip.instrument, '$.label')),
                          JSON_UNQUOTE(JSON_EXTRACT(ip.instrument, '$.name')))
                     COLLATE utf8mb4_unicode_ci,
                 ps.sample_id
          FROM ip_measurements ip
                   JOIN specific_measurement_metadata_ip smm
                        ON smm.measurement_id = ip.measurement_id COLLATE utf8mb4_unicode_ci
                   JOIN project_samples ps
                        ON ps.sample_id = smm.sample_id COLLATE utf8mb4_unicode_ci
      ),
      -- The method label resolves here, once: the measurement's instrument (a controlled vocabulary
      -- term) where it recorded one, otherwise the sample's analysis_method (an enum). Both are
      -- controlled, which is what a grouping key must be -- unlike lcmsMethod, free text, not read.
      dataset_measurements AS (
          SELECT DISTINCT ps.experiment_id,
                          ms.measurement_type,
                          COALESCE(ms.measurement_method,
                                  CAST(ps.analysis_method AS CHAR CHARACTER SET utf8mb4)
                                      COLLATE utf8mb4_unicode_ci) AS method_label,
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
