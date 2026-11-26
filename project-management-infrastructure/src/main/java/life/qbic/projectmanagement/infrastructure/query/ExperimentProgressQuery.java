package life.qbic.projectmanagement.infrastructure.query;

/**
 * Project-scoped SQL for the {@code experiment_progress} read model.
 *
 * <p><b>Why this exists next to the view.</b> The {@code experiment_progress} view computes its
 * per-experiment totals with four {@code GROUP BY experiment_id} subqueries. A query against the
 * view filters on the <em>outer</em> {@code experiments_datamanager} row ({@code e.project}), which
 * the database cannot push into those grouped subqueries: it aggregates every project, then the
 * filter throws all but one away. That is the cost this query removes.</p>
 *
 * <p>Here the project is a parameter and is applied <em>inside</em> the derivation: {@code
 * project_experiments} is the project's experiment ids, {@code project_samples} the samples of
 * those experiments, and {@code measured_samples} / {@code ve} are built only from those. Every
 * downstream aggregate therefore sees one project's rows, independent of any optimizer
 * pushdown.</p>
 *
 * <p>Semantics are kept identical to the view (see the migration's trap register in
 * {@code sql/migrations/create-project-page-read-models.sql}):</p>
 * <ul>
 *   <li>remote primary data is joined on {@code remote_measurement_data.measurement_id =
 *       measurementCode} and excludes {@code deleted = TRUE};</li>
 *   <li>a measurement measuring samples of more than one experiment is attributed to each
 *       experiment and counted in each;</li>
 *   <li>primary-data volume is summed once per {@code (experiment, measurementCode)}, while the
 *       count of measurements with primary data counts measurement records;</li>
 *   <li>every string equality pins {@code utf8mb4_unicode_ci} on one operand to survive the
 *       documented collation drift.</li>
 * </ul>
 *
 * @since 1.19.0
 */
final class ExperimentProgressQuery {

  /**
   * The project-scoped form of {@code experiment_progress}, parameterised by {@code :projectId}.
   */
  static final String BY_PROJECT = """
      WITH project_experiments AS (
          SELECT e.id AS experiment_id
          FROM experiments_datamanager e
          WHERE e.project = :projectId
      ),
      project_samples AS (
          SELECT s.sample_id, s.experiment_id
          FROM sample s
                   JOIN project_experiments pe
                        ON pe.experiment_id COLLATE utf8mb4_unicode_ci = s.experiment_id
      ),
      measured_samples AS (
          SELECT smm.sample_id
          FROM specific_measurement_metadata_ngs smm
                   JOIN project_samples ps
                        ON ps.sample_id = smm.sample_id COLLATE utf8mb4_unicode_ci
          UNION
          SELECT smm.sample_id
          FROM specific_measurement_metadata_pxp smm
                   JOIN project_samples ps
                        ON ps.sample_id = smm.sample_id COLLATE utf8mb4_unicode_ci
          UNION
          SELECT smm.sample_id
          FROM specific_measurement_metadata_ip smm
                   JOIN project_samples ps
                        ON ps.sample_id = smm.sample_id COLLATE utf8mb4_unicode_ci
      ),
      ve AS (
          SELECT 'NGS' AS measurement_type,
                 m.measurement_id,
                 m.measurementCode AS measurement_code,
                 ps.experiment_id
          FROM ngs_measurements m
                   JOIN specific_measurement_metadata_ngs smm
                        ON smm.measurement_id = m.measurement_id COLLATE utf8mb4_unicode_ci
                   JOIN project_samples ps
                        ON ps.sample_id = smm.sample_id COLLATE utf8mb4_unicode_ci
          UNION
          SELECT 'PXP',
                 p.measurement_id,
                 p.measurementCode,
                 ps.experiment_id
          FROM proteomics_measurement p
                   JOIN specific_measurement_metadata_pxp smm
                        ON smm.measurement_id = p.measurement_id COLLATE utf8mb4_unicode_ci
                   JOIN project_samples ps
                        ON ps.sample_id = smm.sample_id COLLATE utf8mb4_unicode_ci
          UNION
          SELECT 'IP',
                 ip.measurement_id,
                 ip.measurementCode,
                 ps.experiment_id
          FROM ip_measurements ip
                   JOIN specific_measurement_metadata_ip smm
                        ON smm.measurement_id = ip.measurement_id COLLATE utf8mb4_unicode_ci
                   JOIN project_samples ps
                        ON ps.sample_id = smm.sample_id COLLATE utf8mb4_unicode_ci
      )
      SELECT e.id                                             AS experiment_id,
             e.project                                        AS project_id,
             e.experimentName                                 AS experiment_name,
             COALESCE(smp.sample_count, 0)                    AS sample_count,
             COALESCE(smp.measured_sample_count, 0)           AS measured_sample_count,
             COALESCE(me.measurement_count, 0)                AS measurement_count,
             COALESCE(me.ngs_measurement_count, 0)            AS ngs_measurement_count,
             COALESCE(me.pxp_measurement_count, 0)            AS pxp_measurement_count,
             COALESCE(me.ip_measurement_count, 0)             AS ip_measurement_count,
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
          SELECT ps.experiment_id,
                 COUNT(*)            AS sample_count,
                 COUNT(ms.sample_id) AS measured_sample_count
          FROM project_samples ps
                   LEFT JOIN measured_samples ms
                             ON ms.sample_id = ps.sample_id COLLATE utf8mb4_unicode_ci
          GROUP BY ps.experiment_id
      ) smp ON smp.experiment_id COLLATE utf8mb4_unicode_ci = e.id
               LEFT JOIN (
          SELECT ve.experiment_id,
                 COUNT(DISTINCT ve.measurement_type, ve.measurement_id) AS measurement_count,
                 COUNT(DISTINCT CASE WHEN ve.measurement_type COLLATE utf8mb4_unicode_ci = 'NGS'
                                     THEN ve.measurement_id END)        AS ngs_measurement_count,
                 COUNT(DISTINCT CASE WHEN ve.measurement_type COLLATE utf8mb4_unicode_ci = 'PXP'
                                     THEN ve.measurement_id END)        AS pxp_measurement_count,
                 COUNT(DISTINCT CASE WHEN ve.measurement_type COLLATE utf8mb4_unicode_ci = 'IP'
                                     THEN ve.measurement_id END)        AS ip_measurement_count
          FROM ve
          GROUP BY ve.experiment_id
      ) me ON me.experiment_id COLLATE utf8mb4_unicode_ci = e.id
               LEFT JOIN (
          SELECT ve.experiment_id,
                 COUNT(DISTINCT ve.measurement_type, ve.measurement_id) AS measurements_with_primary_data,
                 COUNT(DISTINCT CASE WHEN ve.measurement_type COLLATE utf8mb4_unicode_ci = 'NGS'
                                     THEN ve.measurement_id END)        AS ngs_measurements_with_primary_data,
                 COUNT(DISTINCT CASE WHEN ve.measurement_type COLLATE utf8mb4_unicode_ci = 'PXP'
                                     THEN ve.measurement_id END)        AS pxp_measurements_with_primary_data,
                 COUNT(DISTINCT CASE WHEN ve.measurement_type COLLATE utf8mb4_unicode_ci = 'IP'
                                     THEN ve.measurement_id END)        AS ip_measurements_with_primary_data
          FROM ve
                   JOIN remote_measurement_data rmd
                        ON rmd.measurement_id = ve.measurement_code COLLATE utf8mb4_unicode_ci
                            AND rmd.deleted = FALSE
          GROUP BY ve.experiment_id
      ) pd_count ON pd_count.experiment_id COLLATE utf8mb4_unicode_ci = e.id
               LEFT JOIN (
          SELECT codes.experiment_id,
                 COALESCE(SUM(rmd.total_filesize_bytes), 0) AS primary_data_bytes,
                 COALESCE(SUM(CASE WHEN codes.measurement_type COLLATE utf8mb4_unicode_ci = 'NGS'
                                   THEN rmd.total_filesize_bytes END), 0) AS ngs_primary_data_bytes,
                 COALESCE(SUM(CASE WHEN codes.measurement_type COLLATE utf8mb4_unicode_ci = 'PXP'
                                   THEN rmd.total_filesize_bytes END), 0) AS pxp_primary_data_bytes,
                 COALESCE(SUM(CASE WHEN codes.measurement_type COLLATE utf8mb4_unicode_ci = 'IP'
                                   THEN rmd.total_filesize_bytes END), 0) AS ip_primary_data_bytes
          FROM (SELECT DISTINCT experiment_id, measurement_code, measurement_type FROM ve) codes
                   JOIN remote_measurement_data rmd
                        ON rmd.measurement_id = codes.measurement_code COLLATE utf8mb4_unicode_ci
                            AND rmd.deleted = FALSE
          GROUP BY codes.experiment_id
      ) pd_volume ON pd_volume.experiment_id COLLATE utf8mb4_unicode_ci = e.id
      WHERE e.project = :projectId
      """;

  private ExperimentProgressQuery() {
    // static SQL holder
  }
}
