package life.qbic.projectmanagement.infrastructure.query;

/**
 * Experiment-scoped SQL for the Measurement Dataset page's read model.
 *
 * <p><b>Why a parameterised query and not a view.</b> The two facts this read model joins are
 * derived in different places: measurements are attributed to an experiment through their
 * <em>samples</em>, while their datum is keyed only by {@code measurementCode}. A view of this
 * shape would aggregate per measurement first and then be filtered on the outer experiment row,
 * which the database cannot push into the aggregation — the same trap that made {@link
 * ExperimentProgressQuery} exist rather than using {@code experiment_progress} directly. Applying
 * the experiment as a parameter, inside the derivation, keeps every aggregate scoped to one
 * experiment independently of optimizer pushdown. It also avoids a schema change.</p>
 *
 * <p><b>The join is a LEFT JOIN, and that is the whole point.</b> The existing
 * {@code v_ngs_measurement_sample_json} / {@code v_pxp_…} / {@code v_ip_…} views {@code INNER JOIN}
 * {@code remote_measurement_data}, so a measurement whose data has not arrived yet is <em>not a
 * row</em> in them. A reader of those views cannot represent the normal post-registration state
 * ("metadata registered, data not uploaded yet"). This query joins the measurement tables directly
 * to {@code remote_measurement_data} with a {@code LEFT JOIN}, so a waiting entry is a row with
 * {@code NULL} data columns rather than a missing row.</p>
 *
 * <p><b>Count and page cannot disagree.</b> Both statements are composed from the same {@link
 * #ENTRIES_CTE} and the same {@link #FILTER} text, so a filter can never be applied to one and not
 * the other — the failure mode that makes a pager lie about its total.</p>
 *
 * <p><b>Semantics.</b></p>
 * <ul>
 *   <li>A measurement is attributed to an experiment when it measures at least one sample of that
 *       experiment (via the per-type {@code specific_measurement_metadata_*} tables). A measurement
 *       measuring samples of several experiments appears in each (decision D8).</li>
 *   <li>The datum is joined on {@code remote_measurement_data.measurement_id =
 *       measurementCode} — the documented key drift between the two tables.</li>
 *   <li>{@code deleted = TRUE} rows are reported as waiting, never as available: the datum was
 *       removed upstream, so there is nothing to hand out. Counting them as available would make
 *       the completeness statement wrong.</li>
 *   <li>{@code sample_count} counts the distinct samples the measurement measures <em>within this
 *       experiment</em>, so a cross-experiment measurement does not overstate either experiment.</li>
 *   <li>COLLATION DRIFT: every string comparison pins {@code utf8mb4_unicode_ci} on one operand.
 *       Instances have been found mixing {@code latin1_swedish_ci}, {@code utf8mb4_general_ci} and
 *       {@code utf8mb4_unicode_ci}; a column-to-column compare across them raises "Illegal mix of
 *       collations". See the long note in {@code sql/migrations/create-project-page-read-models.sql}
 *       (trap 5).</li>
 * </ul>
 *
 * @see life.qbic.projectmanagement.application.query.MeasurementDataRow
 * @since 1.21.0
 */
final class MeasurementDataQuery {

  private MeasurementDataQuery() {
    // static SQL holder
  }

  /**
   * The experiment's measurement entries, one row per measurement, independent of whether its
   * datum has arrived.
   */
  private static final String ENTRIES_CTE = """
      WITH experiment_samples AS (
          SELECT s.sample_id
          FROM sample s
          WHERE s.experiment_id COLLATE utf8mb4_unicode_ci = :experimentId
      ),
      ngs AS (
          SELECT 'GENOMICS'                   AS measurement_type,
                 m.measurement_id,
                 m.measurementCode             AS measurement_code,
                 m.measurementName             AS measurement_name,
                 m.facility,
                 m.instrument,
                 m.samplePool                  AS sample_pool,
                 m.registrationTime            AS registered_at,
                 COUNT(DISTINCT smm.sample_id) AS sample_count,
                 GROUP_CONCAT(DISTINCT smm.sample_id ORDER BY smm.sample_id SEPARATOR ',')
                                               AS sample_ids
          FROM ngs_measurements m
                   JOIN specific_measurement_metadata_ngs smm
                        ON smm.measurement_id = m.measurement_id COLLATE utf8mb4_unicode_ci
                   JOIN experiment_samples es
                        ON es.sample_id = smm.sample_id COLLATE utf8mb4_unicode_ci
          GROUP BY m.measurement_id, m.measurementCode, m.measurementName, m.facility,
                   m.instrument, m.samplePool, m.registrationTime
      ),
      pxp AS (
          SELECT 'PROTEOMICS'                 AS measurement_type,
                 p.measurement_id,
                 p.measurementCode             AS measurement_code,
                 p.measurementName             AS measurement_name,
                 p.facility,
                 p.instrument,
                 p.samplePool                  AS sample_pool,
                 p.registration                AS registered_at,
                 COUNT(DISTINCT smm.sample_id) AS sample_count,
                 GROUP_CONCAT(DISTINCT smm.sample_id ORDER BY smm.sample_id SEPARATOR ',')
                                               AS sample_ids
          FROM proteomics_measurement p
                   JOIN specific_measurement_metadata_pxp smm
                        ON smm.measurement_id = p.measurement_id COLLATE utf8mb4_unicode_ci
                   JOIN experiment_samples es
                        ON es.sample_id = smm.sample_id COLLATE utf8mb4_unicode_ci
          GROUP BY p.measurement_id, p.measurementCode, p.measurementName, p.facility,
                   p.instrument, p.samplePool, p.registration
      ),
      ip AS (
          SELECT 'IMMUNOPEPTIDOMICS'          AS measurement_type,
                 i.measurement_id,
                 i.measurementCode             AS measurement_code,
                 i.measurementName             AS measurement_name,
                 i.facility,
                 i.instrument,
                 i.samplePool                  AS sample_pool,
                 i.registrationTime            AS registered_at,
                 COUNT(DISTINCT smm.sample_id) AS sample_count,
                 GROUP_CONCAT(DISTINCT smm.sample_id ORDER BY smm.sample_id SEPARATOR ',')
                                               AS sample_ids
          FROM ip_measurements i
                   JOIN specific_measurement_metadata_ip smm
                        ON smm.measurement_id = i.measurement_id COLLATE utf8mb4_unicode_ci
                   JOIN experiment_samples es
                        ON es.sample_id = smm.sample_id COLLATE utf8mb4_unicode_ci
          GROUP BY i.measurement_id, i.measurementCode, i.measurementName, i.facility,
                   i.instrument, i.samplePool, i.registrationTime
      ),
      entries AS (
          SELECT * FROM ngs
          UNION ALL
          SELECT * FROM pxp
          UNION ALL
          SELECT * FROM ip
      ),
      measurement_data AS (
          SELECT entries.*,
                 CASE WHEN rmd.deleted = FALSE THEN 'AVAILABLE' ELSE 'WAITING' END AS data_status,
                 rmd.file_count                                                     AS data_file_count,
                 rmd.total_filesize_bytes                                           AS data_size_bytes,
                 rmd.file_types                                                     AS data_file_types,
                 rmd.registration_at                                                AS data_uploaded_at
          FROM entries
                   LEFT JOIN remote_measurement_data rmd
                             ON rmd.measurement_id = entries.measurement_code
                                 COLLATE utf8mb4_unicode_ci
      )
      """;

  /**
   * The single filter expression shared by the page and the count. Every branch is a no-op when its
   * parameter is absent, so the caller always passes all of them.
   */
  private static final String FILTER = """
      WHERE (COALESCE(:measurementType, '') = ''
             OR measurement_type COLLATE utf8mb4_unicode_ci = :measurementType)
        AND (COALESCE(:searchTerm, '') = ''
             OR LOWER(COALESCE(measurement_code, '')) LIKE :searchPattern
             OR LOWER(COALESCE(measurement_name, '')) LIKE :searchPattern
             OR LOWER(COALESCE(facility, '')) LIKE :searchPattern
             OR LOWER(COALESCE(sample_pool, '')) LIKE :searchPattern)
        AND (COALESCE(:dataStatus, '') = ''
             OR data_status COLLATE utf8mb4_unicode_ci = :dataStatus)
      """;

  /** One page of entries with their data status. */
  static String page(String orderByClause) {
    return ENTRIES_CTE
        + """
        SELECT measurement_id,
               :experimentId AS experiment_id,
               measurement_type,
               measurement_code,
               measurement_name,
               facility,
               instrument,
               sample_pool,
               sample_count,
               registered_at,
               data_status,
               data_file_count,
               data_size_bytes,
               data_file_types,
               data_uploaded_at
        FROM measurement_data
        """
        + FILTER
        + "ORDER BY " + orderByClause + "\nLIMIT :limit OFFSET :offset";
  }

  /** The total number of entries matching the same filter as {@link #page(String)}. */
  static String count() {
    return ENTRIES_CTE + "SELECT COUNT(*) FROM measurement_data\n" + FILTER;
  }

  /**
   * The assay type of one measurement, used to decide which per-type table holds its sample links.
   *
   * <p>Read here rather than supplied by the caller, so the table name can never be chosen from
   * outside.</p>
   */
  static final String MEASUREMENT_TYPE_BY_ID = """
      SELECT measurement_type
      FROM (
          SELECT 'GENOMICS' AS measurement_type, measurement_id FROM ngs_measurements
          UNION ALL
          SELECT 'PROTEOMICS', measurement_id FROM proteomics_measurement
          UNION ALL
          SELECT 'IMMUNOPEPTIDOMICS', measurement_id FROM ip_measurements
      ) typed
      WHERE measurement_id COLLATE utf8mb4_unicode_ci = :measurementId COLLATE utf8mb4_unicode_ci
      """;

  /**
   * The samples one measurement measures, joined through the per-type metadata table.
   *
   * <p>Read per record when a detail panel opens, never as part of the list query: a pool can hold
   * fifty or more samples, and {@code group_concat_max_len} would truncate a combined list without
   * raising an error. Ordered by sample code so the listing is stable between openings.</p>
   *
   * <p>The table name comes from {@link MeasurementMetadataTable}, a closed enum, because a table
   * name cannot be a bind parameter and must never be caller-supplied.</p>
   */
  static String measuredSamples(MeasurementMetadataTable table) {
    return """
        SELECT s.sample_id   AS sample_id,
               s.code        AS sample_code,
               s.label       AS sample_label
        FROM %s smm
                 JOIN sample s ON s.sample_id = smm.sample_id COLLATE utf8mb4_unicode_ci
        WHERE smm.measurement_id = :measurementId COLLATE utf8mb4_unicode_ci
        ORDER BY s.code ASC
        """.formatted(table.tableName());
  }

  /**
   * The per-type table holding a measurement's sample links. A closed set: the table name is
   * interpolated into the statement, so it must never come from a caller.
   */
  enum MeasurementMetadataTable {
    GENOMICS("specific_measurement_metadata_ngs"),
    PROTEOMICS("specific_measurement_metadata_pxp"),
    IMMUNOPEPTIDOMICS("specific_measurement_metadata_ip");

    private final String tableName;

    MeasurementMetadataTable(String tableName) {
      this.tableName = tableName;
    }

    String tableName() {
      return tableName;
    }

    /** Resolves the table for a read-model assay type name, as stored in the entries CTE. */
    static MeasurementMetadataTable forAssayTypeName(String assayTypeName) {
      return switch (assayTypeName) {
        case "GENOMICS" -> GENOMICS;
        case "PROTEOMICS" -> PROTEOMICS;
        case "IMMUNOPEPTIDOMICS" -> IMMUNOPEPTIDOMICS;
        default -> throw new IllegalArgumentException(
            "Unknown assay type: " + assayTypeName);
      };
    }
  }

  /**
   * Sortable columns of the measurement entry list.
   *
   * <p>An enum with an explicit SQL fragment, <em>not</em> a caller-supplied string: the sort column
   * is interpolated into the statement, so it must never come from raw user input. This is the
   * validation ADR-0009 requires.</p>
   */
  enum SortColumn {
    MEASUREMENT_ID("measurement_code"),
    MEASUREMENT_NAME("measurement_name"),
    REGISTRATION_DATE("registered_at"),
    DATA_STATUS("data_status"),
    SAMPLE_POOL("sample_pool");

    private final String sql;

    SortColumn(String sql) {
      this.sql = sql;
    }

    String sql() {
      return sql;
    }
  }

  /**
   * Builds the {@code ORDER BY} clause. The measurement code is appended as a deterministic
   * tie-break unless it is already the sort column, so offset/limit pagination never drifts across
   * page boundaries (ADR-0009).
   *
   * @param column    the requested sort column, never {@code null}
   * @param ascending whether to sort ascending
   */
  static String orderBy(SortColumn column, boolean ascending) {
    String direction = ascending ? "ASC" : "DESC";
    if (column == SortColumn.MEASUREMENT_ID) {
      return column.sql() + " " + direction;
    }
    return column.sql() + " " + direction + ", measurement_code ASC";
  }
}
