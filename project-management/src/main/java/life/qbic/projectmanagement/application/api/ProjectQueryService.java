package life.qbic.projectmanagement.application.api;

import java.util.List;
import java.util.Objects;
import life.qbic.projectmanagement.application.query.ExperimentProgress;
import life.qbic.projectmanagement.application.query.ProjectSampleOverview;
import life.qbic.projectmanagement.application.query.RecentMeasurement;
import life.qbic.projectmanagement.application.query.RecentSample;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Read-only query service for the project page.
 *
 * <p>This is the read side of the project page, deliberately separate from {@link
 * AsyncProjectService} (the command boundary) so that read models can grow without turning that
 * interface into a god interface (implementation strategy §4, decision D4). It returns flat
 * view-backed read models ({@link ExperimentProgress}, {@link ProjectSampleOverview}), never domain
 * aggregates.</p>
 *
 * <p><b>Reactive boundary (strategy §4.1).</b> The contract is reactive, but over the blocking JPA
 * stack this is thread offload and fan-out, not a non-blocking pipeline. The implementing class
 * propagates the security context across the scheduler hop; every call is access-controlled for
 * {@code Project} {@code READ}. Errors are emitted as {@link RequestFailedException} or {@link
 * AccessDeniedException}.</p>
 *
 * @since 1.19.0
 */
public interface ProjectQueryService {

  /**
   * Streams one {@link ExperimentProgress} per experiment of the project, in no particular order.
   *
   * @param projectId the project identifier
   * @throws RequestFailedException if the request could not be executed
   * @throws AccessDeniedException  if the caller lacks {@code READ} on the project
   */
  Flux<ExperimentProgress> getExperimentProgress(String projectId);

  /**
   * Streams the progress of a single experiment. Emits nothing if the experiment does not belong to
   * the project.
   *
   * @param projectId    the project identifier
   * @param experimentId the experiment identifier
   */
  Mono<ExperimentProgress> getExperimentProgress(String projectId, String experimentId);

  /**
   * Streams a page of {@link ProjectSampleOverview} for the project, matching the filter.
   *
   * @param projectId the project identifier
   * @param offset    the offset from 0 of all matching rows to start at
   * @param limit     the maximum number of rows to return
   * @param filter    the filter and sort to apply; use {@link ProjectSampleFilter#all()} for none
   * @throws RequestFailedException if the request could not be executed
   * @throws AccessDeniedException  if the caller lacks {@code READ} on the project
   */
  Flux<ProjectSampleOverview> getProjectSamples(String projectId, int offset, int limit,
      ProjectSampleFilter filter);

  /**
   * Counts the project samples matching the filter, for the pager's total.
   *
   * @param projectId the project identifier
   * @param filter    the filter to apply; use {@link ProjectSampleFilter#all()} for none
   * @throws RequestFailedException if the request could not be executed
   * @throws AccessDeniedException  if the caller lacks {@code READ} on the project
   */
  Mono<Long> countProjectSamples(String projectId, ProjectSampleFilter filter);

  /**
   * Streams the newest samples of every experiment in the project (at most three per experiment),
   * newest first — the Samples snapshot panel of the experiment cards.
   *
   * @param projectId the project identifier
   * @throws RequestFailedException if the request could not be executed
   * @throws AccessDeniedException  if the caller lacks {@code READ} on the project
   */
  Flux<RecentSample> getRecentSamples(String projectId);

  /**
   * Streams the newest samples of one experiment (at most three), newest first — the Samples card
   * of the experiment overview.
   *
   * @param projectId    the project identifier
   * @param experimentId the experiment identifier
   */
  Flux<RecentSample> getRecentSamples(String projectId, String experimentId);

  /**
   * Streams the newest measurements of every experiment in the project (at most three per
   * experiment), newest first — the Dataset snapshot panel of the experiment cards.
   *
   * @param projectId the project identifier
   * @throws RequestFailedException if the request could not be executed
   * @throws AccessDeniedException  if the caller lacks {@code READ} on the project
   */
  Flux<RecentMeasurement> getRecentMeasurements(String projectId);

  /**
   * Streams the newest measurements of one experiment (at most three), newest first — the Dataset
   * card of the experiment overview.
   *
   * @param projectId    the project identifier
   * @param experimentId the experiment identifier
   */
  Flux<RecentMeasurement> getRecentMeasurements(String projectId, String experimentId);

  /**
   * Filter and sort for the project sample list.
   *
   * @param sampleName   a case-insensitive substring matched against the sample's name and code;
   *                     blank means no name filter
   * @param experimentId restricts the list to one experiment; {@code null} means all experiments
   * @param measured     restricts by measured status
   * @param sortOrders   the requested sort orders; empty applies a deterministic default
   */
  record ProjectSampleFilter(String sampleName, String experimentId, MeasuredFilter measured,
                             List<SortOrder<ProjectSampleSortKey>> sortOrders) {

    public ProjectSampleFilter {
      sampleName = sampleName == null ? "" : sampleName;
      experimentId = experimentId == null || experimentId.isBlank() ? null : experimentId;
      measured = measured == null ? MeasuredFilter.ALL : measured;
      sortOrders = sortOrders == null ? List.of() : List.copyOf(sortOrders);
    }

    /** A filter that selects everything and leaves sorting to the implementation default. */
    public static ProjectSampleFilter all() {
      return new ProjectSampleFilter("", null, MeasuredFilter.ALL, List.of());
    }
  }

  /** Restricts the project sample list by measured status. */
  enum MeasuredFilter {
    ALL, MEASURED, NOT_MEASURED
  }

  /** Sortable properties of the project sample list. */
  enum ProjectSampleSortKey {
    SAMPLE_ID,
    SAMPLE_NAME,
    EXPERIMENT,
    REGISTRATION_TIME,
    MODIFICATION_TIME
  }

  /**
   * A single sort instruction for a project sample list query.
   *
   * @param key       the property to sort by
   * @param direction the sort direction
   * @param <T>       the sort key type
   */
  record SortOrder<T>(T key, SortDirection direction) {

    public SortOrder {
      Objects.requireNonNull(key, "key must not be null");
      Objects.requireNonNull(direction, "direction must not be null");
    }
  }

  /** Sort direction of a {@link SortOrder}. */
  enum SortDirection {
    ASC, DESC
  }

  /** Signals that a project query could not be executed. */
  class RequestFailedException extends RuntimeException {

    public RequestFailedException(String message) {
      super(message);
    }

    public RequestFailedException(String message, Throwable cause) {
      super(message, cause);
    }
  }

  /** Signals that the caller has insufficient rights for a project query. */
  class AccessDeniedException extends RuntimeException {

    public AccessDeniedException(String message) {
      super(message);
    }

    public AccessDeniedException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
