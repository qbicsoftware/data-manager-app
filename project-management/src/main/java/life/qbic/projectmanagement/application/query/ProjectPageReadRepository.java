package life.qbic.projectmanagement.application.query;

import java.util.List;
import life.qbic.projectmanagement.application.api.ProjectQueryService.MeasurementDataFilter;
import life.qbic.projectmanagement.application.api.ProjectQueryService.ProjectSampleFilter;
import life.qbic.projectmanagement.application.query.MeasuredSample;
import life.qbic.projectmanagement.application.query.MeasurementDataRow;

/**
 * Read-only port for the project-page read models.
 *
 * <p>This is the application-layer seam that keeps {@link ProjectQueryService} free of persistence
 * details: the reactive query service depends on this interface, and the JPA implementation lives in
 * the infrastructure module (mirroring {@code ProjectOverviewLookup}). Implementations must be
 * access-controlled for {@code Project} {@code READ}.</p>
 *
 * <p>All methods are blocking by design — the reactive boundary policy (strategy §4.1) puts the
 * scheduler hop in the query service, not here.</p>
 *
 * @see ProjectQueryService
 * @since 1.19.0
 */
public interface ProjectPageReadRepository {

  /**
   * Returns one {@link ExperimentProgress} per experiment of the project.
   *
   * @param projectId the project identifier
   */
  List<ExperimentProgress> findExperimentProgress(String projectId);

  /**
   * Returns a page of {@link ProjectSampleOverview} for the project, matching the filter.
   *
   * @param projectId the project identifier
   * @param offset    the offset from 0 of all matching rows to start at
   * @param limit     the maximum number of rows to return
   * @param filter    the filter and sort to apply
   */
  List<ProjectSampleOverview> findProjectSamples(String projectId, int offset, int limit,
      ProjectSampleFilter filter);

  /**
   * Returns the newest samples of every experiment in the project (at most three per experiment).
   *
   * @param projectId the project identifier
   */
  List<RecentSample> findRecentSamples(String projectId);

  /**
   * Returns the newest measurements of every experiment in the project (at most three per
   * experiment), with their primary-data status.
   *
   * @param projectId the project identifier
   */
  List<RecentMeasurement> findRecentMeasurements(String projectId);

  /**
   * Returns a page of {@link MeasurementDataRow} for one experiment, matching the filter.
   *
   * @param projectId    the project identifier
   * @param experimentId the experiment identifier
   * @param offset       the offset from 0 of all matching rows to start at
   * @param limit        the maximum number of rows to return
   * @param filter       the filter and sort to apply
   */
  List<MeasurementDataRow> findMeasurementData(String projectId, String experimentId, int offset,
      int limit, MeasurementDataFilter filter);

  /**
   * Counts the measurement data entries of one experiment matching the filter.
   *
   * @param projectId    the project identifier
   * @param experimentId the experiment identifier
   * @param filter       the filter to apply
   */
  long countMeasurementData(String projectId, String experimentId, MeasurementDataFilter filter);

  /**
   * Returns the samples one measurement measures, in sample-code order.
   *
   * <p>Fetched per record, not as part of the list read model: a pool can hold fifty or more
   * samples, and a list query carrying every pool's members would be truncated silently by
   * {@code group_concat_max_len}. Emits an empty list for a measurement that measures nothing.</p>
   *
   * @param projectId     the project identifier
   * @param measurementId the internal measurement identifier
   */
  List<MeasuredSample> findMeasuredSamples(String projectId, String measurementId);

  /**
   * Counts the project samples matching the filter.
   *
   * @param projectId the project identifier
   * @param filter    the filter to apply
   */
  long countProjectSamples(String projectId, ProjectSampleFilter filter);
}
