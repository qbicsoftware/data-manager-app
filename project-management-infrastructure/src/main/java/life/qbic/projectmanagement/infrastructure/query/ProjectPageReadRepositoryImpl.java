package life.qbic.projectmanagement.infrastructure.query;

import jakarta.persistence.criteria.Predicate;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import life.qbic.application.commons.OffsetBasedRequest;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.api.ProjectQueryService.MeasurementDataFilter;
import life.qbic.projectmanagement.application.api.ProjectQueryService.MeasurementDataSortKey;
import life.qbic.projectmanagement.application.api.ProjectQueryService.ProjectSampleFilter;
import life.qbic.projectmanagement.application.api.ProjectQueryService.ProjectSampleSortKey;
import life.qbic.projectmanagement.application.api.ProjectQueryService.SortDirection;
import life.qbic.projectmanagement.application.api.ProjectQueryService.SortOrder;
import life.qbic.projectmanagement.application.query.ExperimentProgress;
import life.qbic.projectmanagement.application.query.MeasurementDataRow;
import life.qbic.projectmanagement.application.query.ProjectPageReadRepository;
import life.qbic.projectmanagement.application.query.ProjectSampleOverview;
import life.qbic.projectmanagement.application.query.RecentMeasurement;
import life.qbic.projectmanagement.application.query.RecentSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Read-side implementation of {@link ProjectPageReadRepository}. Experiment progress is served by a
 * project-scoped SQL query ({@link ExperimentProgressQuery#BY_PROJECT}); the sample and
 * recent-activity reads remain backed by their read-model views ({@code v_project_sample_overview},
 * {@code experiment_recent_sample}, {@code experiment_recent_measurement}).
 *
 * <p>Access control sits here, on the read port (mirroring {@code LocalRawDatasetLookupService}).
 * The caller of the port is the reactive {@link
 * life.qbic.projectmanagement.application.api.ProjectQueryService}, which propagates the security
 * context onto the scheduler thread, so the {@code @PreAuthorize} check evaluates against the real
 * principal.</p>
 *
 * @since 1.19.0
 */
@Component
public class ProjectPageReadRepositoryImpl implements ProjectPageReadRepository {

  private static final Logger log =
      LoggerFactory.logger(ProjectPageReadRepositoryImpl.class);

  private static final String PROJECT_READ = "hasPermission(#projectId, "
      + "'life.qbic.projectmanagement.domain.model.project.Project', 'READ')";
  private static final String SAMPLE_CODE = "sampleCode";

  /**
   * Maps one measurement entry with its data status. Absent data columns (a waiting entry) map to
   * {@code null}, never to zero: {@code 0} bytes is meaningful and must not be confused with "no
   * datum".
   */
  private static final RowMapper<MeasurementDataRow> MEASUREMENT_DATA_ROW_MAPPER =
      new MeasurementDataRowMapper(new ObjectMapper());

  /**
   * Maps one measurement entry with its data status. Absent data columns (a waiting entry) map to
   * {@code null}, never to zero: {@code 0} bytes is meaningful and must not be confused with "no
   * datum".
   */
  private static final class MeasurementDataRowMapper implements RowMapper<MeasurementDataRow> {

    private final ObjectMapper objectMapper;

    private MeasurementDataRowMapper(ObjectMapper objectMapper) {
      this.objectMapper = objectMapper;
    }

    @Override
    public MeasurementDataRow mapRow(ResultSet resultSet, int rowNumber) throws SQLException {
      return new MeasurementDataRow(
          resultSet.getString("measurement_id"),
          resultSet.getString("experiment_id"),
          resultSet.getString("measurement_type"),
          resultSet.getString("measurement_code"),
          resultSet.getString("measurement_name"),
          resultSet.getString("facility"),
          resultSet.getString("instrument"),
          resultSet.getString("sample_pool"),
          resultSet.getInt("sample_count"),
          toInstant(resultSet, "registered_at"),
          MeasurementDataRow.DataStatus.valueOf(resultSet.getString("data_status")),
          toLong(resultSet, "data_file_count"),
          toLong(resultSet, "data_size_bytes"),
          fileTypes(resultSet.getString("data_file_types")),
          toInstant(resultSet, "data_uploaded_at"));
    }

    /**
     * {@code remote_measurement_data.file_types} is a JSON array of file suffixes written by the
     * external sync, mapped here with the same converter the raw-data views use. An unparseable or
     * absent value degrades to "no suffixes stated" rather than failing the page.
     */
    private Set<String> fileTypes(String json) {
      if (json == null || json.isBlank()) {
        return Set.of();
      }
      try {
        return Set.of(objectMapper.readValue(json, String[].class));
      } catch (JacksonException e) {
        log.error("Could not read file types from '" + json + "'", e);
        return Set.of();
      }
    }

    private static Instant toInstant(ResultSet resultSet, String column) throws SQLException {
      Timestamp timestamp = resultSet.getTimestamp(column);
      return timestamp == null ? null : timestamp.toInstant();
    }

    private static Long toLong(ResultSet resultSet, String column) throws SQLException {
      long value = resultSet.getLong(column);
      return resultSet.wasNull() ? null : value;
    }
  }

  /**
   * Maps the project-scoped progress query ({@link ExperimentProgressQuery#BY_PROJECT}) onto the
   * flat {@link ExperimentProgress} read model. Column aliases match the entity's view columns.
   */
  private static final RowMapper<ExperimentProgress> EXPERIMENT_PROGRESS_ROW_MAPPER =
      (resultSet, rowNumber) -> new ExperimentProgress(
          resultSet.getString("experiment_id"),
          resultSet.getString("project_id"),
          resultSet.getString("experiment_name"),
          resultSet.getInt("sample_count"),
          resultSet.getInt("measured_sample_count"),
          resultSet.getInt("measurement_count"),
          resultSet.getInt("ngs_measurement_count"),
          resultSet.getInt("pxp_measurement_count"),
          resultSet.getInt("ip_measurement_count"),
          resultSet.getInt("measurements_with_primary_data"),
          resultSet.getInt("ngs_measurements_with_primary_data"),
          resultSet.getInt("pxp_measurements_with_primary_data"),
          resultSet.getInt("ip_measurements_with_primary_data"),
          resultSet.getLong("primary_data_bytes"),
          resultSet.getLong("ngs_primary_data_bytes"),
          resultSet.getLong("pxp_primary_data_bytes"),
          resultSet.getLong("ip_primary_data_bytes"));

  private final NamedParameterJdbcTemplate jdbcTemplate;  private final ProjectSampleOverviewRepository sampleOverviewRepository;
  private final RecentSampleRepository recentSampleRepository;
  private final RecentMeasurementRepository recentMeasurementRepository;

  public ProjectPageReadRepositoryImpl(
      @Autowired JdbcTemplate jdbcTemplate,
      @Autowired ProjectSampleOverviewRepository sampleOverviewRepository,
      @Autowired RecentSampleRepository recentSampleRepository,
      @Autowired RecentMeasurementRepository recentMeasurementRepository) {
    this.jdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
    this.sampleOverviewRepository = sampleOverviewRepository;
    this.recentSampleRepository = recentSampleRepository;
    this.recentMeasurementRepository = recentMeasurementRepository;
  }

  @Override
  @PreAuthorize(PROJECT_READ)
  public List<ExperimentProgress> findExperimentProgress(String projectId) {
    // Project-scoped in SQL: the filter is applied inside the derivation, not after it. See
    // ExperimentProgressQuery for why the experiment_progress view could not be used directly.
    return jdbcTemplate.query(ExperimentProgressQuery.BY_PROJECT,
        Map.of("projectId", projectId), EXPERIMENT_PROGRESS_ROW_MAPPER);
  }

  @Override
  @PreAuthorize(PROJECT_READ)
  public List<ProjectSampleOverview> findProjectSamples(String projectId, int offset, int limit,
      ProjectSampleFilter filter) {
    var pageable = new OffsetBasedRequest(offset, limit, sortOf(filter.sortOrders()));
    return sampleOverviewRepository.findAll(specification(projectId, filter), pageable).getContent();
  }

  @Override
  @PreAuthorize(PROJECT_READ)
  public long countProjectSamples(String projectId, ProjectSampleFilter filter) {
    return sampleOverviewRepository.count(specification(projectId, filter));
  }

  @Override
  @PreAuthorize(PROJECT_READ)
  public List<MeasurementDataRow> findMeasurementData(String projectId, String experimentId,
      int offset, int limit, MeasurementDataFilter filter) {
    Map<String, Object> parameters = measurementDataParameters(projectId, experimentId, filter);
    parameters.put("limit", limit);
    parameters.put("offset", offset);
    String orderBy = measurementDataOrderBy(filter);
    return jdbcTemplate.query(MeasurementDataQuery.page(orderBy), parameters,
        MEASUREMENT_DATA_ROW_MAPPER);
  }

  @Override
  @PreAuthorize(PROJECT_READ)
  public long countMeasurementData(String projectId, String experimentId,
      MeasurementDataFilter filter) {
    Long count = jdbcTemplate.queryForObject(MeasurementDataQuery.count(),
        measurementDataParameters(projectId, experimentId, filter), Long.class);
    return count == null ? 0L : count;
  }

  /**
   * Builds the parameter map shared by the page and the count, so both statements are filtered by
   * exactly the same values.
   */
  private static Map<String, Object> measurementDataParameters(String experimentId,
      MeasurementDataFilter filter) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("experimentId", experimentId);
    parameters.put("measurementType",
        filter.measurementType() == null ? "" : filter.measurementType().name());
    parameters.put("dataStatus",
        filter.dataStatus() == null ? "" : filter.dataStatus().name());
    parameters.put("searchTerm", filter.searchTerm());
    parameters.put("searchPattern", "%" + filter.searchTerm().toLowerCase() + "%");
    return parameters;
  }

  private static Map<String, Object> measurementDataParameters(String projectId,
      String experimentId, MeasurementDataFilter filter) {
    Map<String, Object> parameters = measurementDataParameters(experimentId, filter);
    parameters.put("projectId", projectId);
    return parameters;
  }

  /**
   * Resolves the requested sort orders to a validated SQL fragment (ADR-0009). The default is the
   * measurement code ascending, which is also the deterministic tie-break of every other column.
   */
  private static String measurementDataOrderBy(MeasurementDataFilter filter) {
    if (filter.sortOrders().isEmpty()) {
      return MeasurementDataQuery.orderBy(MeasurementDataQuery.SortColumn.MEASUREMENT_ID, true);
    }
    StringBuilder clause = new StringBuilder();
    for (SortOrder<MeasurementDataSortKey> order : filter.sortOrders()) {
      if (!clause.isEmpty()) {
        clause.append(", ");
      }
      clause.append(MeasurementDataQuery.orderBy(sortColumnOf(order.key()),
          order.direction() == SortDirection.ASC));
    }
    return clause.toString();
  }

  private static MeasurementDataQuery.SortColumn sortColumnOf(MeasurementDataSortKey key) {
    return switch (key) {
      case MEASUREMENT_ID -> MeasurementDataQuery.SortColumn.MEASUREMENT_ID;
      case MEASUREMENT_NAME -> MeasurementDataQuery.SortColumn.MEASUREMENT_NAME;
      case REGISTRATION_DATE -> MeasurementDataQuery.SortColumn.REGISTRATION_DATE;
      case DATA_STATUS -> MeasurementDataQuery.SortColumn.DATA_STATUS;
      case SAMPLE_POOL -> MeasurementDataQuery.SortColumn.SAMPLE_POOL;
    };
  }

  @Override
  @PreAuthorize(PROJECT_READ)
  public List<RecentSample> findRecentSamples(String projectId) {
    return recentSampleRepository.findAllByProjectId(projectId);
  }

  @Override
  @PreAuthorize(PROJECT_READ)
  public List<RecentMeasurement> findRecentMeasurements(String projectId) {
    return recentMeasurementRepository.findAllByProjectId(projectId);
  }

  private static Specification<ProjectSampleOverview> specification(String projectId,
      ProjectSampleFilter filter) {
    return (root, query, builder) -> {
      List<Predicate> predicates = new ArrayList<>();
      predicates.add(builder.equal(root.get("projectId"), projectId));
      if (filter.experimentId() != null) {
        predicates.add(builder.equal(root.get("experimentId"), filter.experimentId()));
      }
      if (!filter.sampleName().isBlank()) {
        String like = "%" + filter.sampleName().toLowerCase() + "%";
        predicates.add(builder.or(
            builder.like(builder.lower(root.get("sampleName").as(String.class)), like),
            builder.like(builder.lower(root.get(SAMPLE_CODE).as(String.class)), like)));
      }
      switch (filter.measured()) {
        case MEASURED -> predicates.add(builder.isTrue(root.get("measured").as(Boolean.class)));
        case NOT_MEASURED ->
            predicates.add(builder.isFalse(root.get("measured").as(Boolean.class)));
        case ALL -> {
          // no constraint
        }
      }
      return builder.and(predicates.toArray(new Predicate[0]));
    };
  }

  /**
   * Translates the requested sort orders into a Spring Data {@link Sort}. The sample code is
   * appended as a deterministic tie-break unless it is already part of the sort, so pagination is
   * stable (ADR-0009).
   */
  private static Sort sortOf(List<SortOrder<ProjectSampleSortKey>> orders) {
    List<Sort.Order> sortOrders = new ArrayList<>();
    for (SortOrder<ProjectSampleSortKey> order : orders) {
      sortOrders.add(new Sort.Order(directionOf(order.direction()), propertyOf(order.key())));
    }
    boolean sampleCodeSelected = sortOrders.stream()
        .anyMatch(order -> order.getProperty().equals(SAMPLE_CODE));
    if (!sampleCodeSelected) {
      sortOrders.add(new Sort.Order(Sort.Direction.ASC, SAMPLE_CODE));
    }
    return Sort.by(sortOrders);
  }

  private static Sort.Direction directionOf(SortDirection direction) {
    return direction == SortDirection.ASC ? Sort.Direction.ASC : Sort.Direction.DESC;
  }

  private static String propertyOf(ProjectSampleSortKey key) {
    return switch (key) {
      case SAMPLE_ID -> SAMPLE_CODE;
      case SAMPLE_NAME -> "sampleName";
      case EXPERIMENT -> "experimentName";
      case REGISTRATION_TIME -> "registrationTime";
      case MODIFICATION_TIME -> "lastModified";
    };
  }
}
