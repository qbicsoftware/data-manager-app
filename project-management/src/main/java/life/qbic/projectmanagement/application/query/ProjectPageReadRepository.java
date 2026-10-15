package life.qbic.projectmanagement.application.query;

import java.util.List;
import life.qbic.projectmanagement.application.api.ProjectQueryService.ProjectSampleFilter;

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
   * Counts the project samples matching the filter.
   *
   * @param projectId the project identifier
   * @param filter    the filter to apply
   */
  long countProjectSamples(String projectId, ProjectSampleFilter filter);
}
