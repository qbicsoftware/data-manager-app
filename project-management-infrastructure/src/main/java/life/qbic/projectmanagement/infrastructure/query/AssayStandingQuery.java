package life.qbic.projectmanagement.infrastructure.query;

/**
 * Project-scoped SQL for the project's assay standing: one row per primary dataset.
 *
 * <p><b>Grain.</b> One row per {@code (experiment, analysis method)}. A primary dataset is the data
 * of one experiment for one analysis method, so two experiments measured as WES are two datasets and
 * one experiment measured as WES and RNA-Seq is two as well. That pair is the identity; an analysis
 * method on its own is not, which is why this query does not aggregate them together.</p>
 *
 * <p><b>Where the analysis method is read from.</b> {@code sample.analysis_method}. That is today's
 * annotation and a known grandfather relation: it belongs on the measurement and will move there.
 * This query is the <em>single</em> place that knows it (see ADR 0010), so the move is a change to
 * this statement and its row mapping and to nothing else. Do not resolve the analysis method from
 * samples anywhere else -- a second call site would double the migration surface.</p>
 *
 * <p><b>Attribution.</b> A measurement is attributed to an analysis method through the samples it
 * measured ({@code specific_measurement_metadata_*} -> {@code sample}). When one measurement measures
 * samples carrying different analysis methods, it is attributed to each and therefore counted in
 * each, so the per-dataset measurement counts of a project can sum to more than the number of
 * measurement records. This follows the precedent already documented for measurements spanning
 * experiments (trap 3 in {@code sql/migrations/create-project-page-read-models.sql}).</p>
 *
 * <p><b>Kept identical to {@code ExperimentProgressQuery} where they overlap:</b> project scoping is
 * applied inside the derivation rather than after it, remote primary data joins on
 * {@code remote_measurement_data.measurement_id = measurementCode} and excludes
 * {@code deleted = TRUE}, volume is summed once per {@code (experiment, measurementCode)}, and every
 * string equality pins {@code utf8mb4_unicode_ci} on one operand to survive the documented collation
 * drift.</p>
 *
 * @see ExperimentProgressQuery
 * @since 1.22.0
 */
final class AssayStandingQuery {

  /**
   * One row per {@code (experiment, analysis method)}, parameterised by {@code :projectId}.
   *
   * <p>Samples without an analysis method are excluded: they cannot be attributed to a dataset, and
   * inventing an "unknown" dataset would state a dataset that does not exist. The consequence --
   * that measurements of such samples are not counted here while they <em>are</em> counted in the
   * per-type progress figures -- is deliberate, because the two figures answer different
   * questions.</p>
   *
   * <p>The three aggregates are separate subqueries joined on the dataset key, so each counts at its
   * own grain and none of them is collapsed by a {@code DISTINCT} inside an aggregate:
   * measurement records are counted per record, and volume is summed once per
   * {@code (experiment, analysis method, measurementCode)} so that two measurement records sharing
   * one code do not double it.</p>
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
      ),
      measurement_sample AS (
          SELECT smm.measurement_id, m.measurementCode AS measurement_code, 'NGS' AS measurement_type,
                 ps.sample_id
          FROM ngs_measurements m
                   JOIN specific_measurement_metadata_ngs smm
                        ON smm.measurement_id = m.measurement_id COLLATE utf8mb4_unicode_ci
                   JOIN project_samples ps
                        ON ps.sample_id = smm.sample_id COLLATE utf8mb4_unicode_ci
          UNION
          SELECT smm.measurement_id, p.measurementCode, 'PXP',
                 ps.sample_id
          FROM proteomics_measurement p
                   JOIN specific_measurement_metadata_pxp smm
                        ON smm.measurement_id = p.measurement_id COLLATE utf8mb4_unicode_ci
                   JOIN project_samples ps
                        ON ps.sample_id = smm.sample_id COLLATE utf8mb4_unicode_ci
          UNION
          SELECT smm.measurement_id, ip.measurementCode, 'IP',
                 ps.sample_id
          FROM ip_measurements ip
                   JOIN specific_measurement_metadata_ip smm
                        ON smm.measurement_id = ip.measurement_id COLLATE utf8mb4_unicode_ci
                   JOIN project_samples ps
                        ON ps.sample_id = smm.sample_id COLLATE utf8mb4_unicode_ci
      ),
      -- One row per (experiment, analysis method, measurement record). DISTINCT collapses the
      -- sample fan-out: a measurement measuring ten samples of one method is still one measurement.
      dataset_measurements AS (
          SELECT DISTINCT ps.experiment_id, ps.analysis_method,
                          ms.measurement_type, ms.measurement_id, ms.measurement_code
          FROM measurement_sample ms
                   JOIN project_samples ps ON ps.sample_id = ms.sample_id
      ),
      dataset_keys AS (
          SELECT DISTINCT experiment_id, analysis_method FROM dataset_measurements
      ),
      -- Measurement records per dataset.
      counted AS (
          SELECT experiment_id, analysis_method,
                 COUNT(*) AS measurements
          FROM dataset_measurements
          GROUP BY experiment_id, analysis_method
      ),
      -- Measurement records carrying primary data, per dataset.
      with_data AS (
          SELECT dm.experiment_id, dm.analysis_method,
                 COUNT(*) AS measurements_with_primary_data
          FROM dataset_measurements dm
                   JOIN remote_measurement_data rmd
                        ON rmd.measurement_id = dm.measurement_code COLLATE utf8mb4_unicode_ci
                            AND rmd.deleted = FALSE
          GROUP BY dm.experiment_id, dm.analysis_method
      ),
      -- Bytes per (experiment, analysis method, measurementCode), then summed per dataset. Keyed by
      -- code first, so two measurement records sharing one code cannot double the volume.
      bytes_by_code AS (
          SELECT dm.experiment_id, dm.analysis_method, dm.measurement_code,
                 MAX(rmd.total_filesize_bytes) AS code_bytes
          FROM dataset_measurements dm
                   JOIN remote_measurement_data rmd
                        ON rmd.measurement_id = dm.measurement_code COLLATE utf8mb4_unicode_ci
                            AND rmd.deleted = FALSE
          GROUP BY dm.experiment_id, dm.analysis_method, dm.measurement_code
      ),
      volume AS (
          SELECT experiment_id, analysis_method, COALESCE(SUM(code_bytes), 0) AS primary_data_bytes
          FROM bytes_by_code
          GROUP BY experiment_id, analysis_method
      )
      SELECT pe.experiment_id,
             pe.experiment_name,
             k.analysis_method,
             COALESCE(c.measurements, 0)                  AS measurements,
             COALESCE(w.measurements_with_primary_data, 0) AS measurements_with_primary_data,
             COALESCE(v.primary_data_bytes, 0)            AS primary_data_bytes
      FROM dataset_keys k
               JOIN project_experiments pe
                    ON pe.experiment_id COLLATE utf8mb4_unicode_ci = k.experiment_id
               LEFT JOIN counted c
                         ON c.experiment_id COLLATE utf8mb4_unicode_ci = k.experiment_id
                             AND c.analysis_method = k.analysis_method
               LEFT JOIN with_data w
                         ON w.experiment_id COLLATE utf8mb4_unicode_ci = k.experiment_id
                             AND w.analysis_method = k.analysis_method
               LEFT JOIN volume v
                         ON v.experiment_id COLLATE utf8mb4_unicode_ci = k.experiment_id
                             AND v.analysis_method = k.analysis_method
      ORDER BY pe.experiment_name, k.analysis_method
      """;

  private AssayStandingQuery() {
    // static SQL holder
  }
}
