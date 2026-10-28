package life.qbic.projectmanagement.application.api;

import static life.qbic.projectmanagement.application.authorization.ReactiveSecurityContextUtils.applySecurityContext;
import static life.qbic.projectmanagement.application.authorization.ReactiveSecurityContextUtils.applySecurityContextMany;
import static life.qbic.projectmanagement.application.authorization.ReactiveSecurityContextUtils.reactiveSecurity;

import java.util.Objects;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.query.ExperimentProgress;
import life.qbic.projectmanagement.application.query.MeasurementDataRow;
import life.qbic.projectmanagement.application.query.ProjectPageReadRepository;
import life.qbic.projectmanagement.application.query.ProjectSampleOverview;
import life.qbic.projectmanagement.application.query.RecentMeasurement;
import life.qbic.projectmanagement.application.query.RecentSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.util.retry.Retry;

/**
 * Reactive implementation of {@link ProjectQueryService}.
 *
 * <p>Follows the reactive boundary policy (implementation strategy §4.1): the blocking JPA read
 * runs on the shared {@code elasticScheduler}, the caller's {@link SecurityContext} is propagated
 * across that hop so the access control on {@link ProjectPageReadRepository} evaluates against the
 * right principal, and errors are mapped to the query service's own exception types. The reactive
 * shape buys thread offload and fan-out here — not a non-blocking pipeline over JPA.</p>
 *
 * @since 1.19.0
 */
@Service
public class ProjectQueryServiceImpl implements ProjectQueryService {

  private static final Logger log = LoggerFactory.logger(ProjectQueryServiceImpl.class);

  private final ProjectPageReadRepository readRepository;
  private final Scheduler scheduler;

  public ProjectQueryServiceImpl(@Autowired ProjectPageReadRepository readRepository,
      @Qualifier("elasticScheduler") Scheduler scheduler) {
    this.readRepository = Objects.requireNonNull(readRepository, "readRepository must not be null");
    this.scheduler = Objects.requireNonNull(scheduler, "scheduler must not be null");
  }

  @Override
  public Flux<ExperimentProgress> getExperimentProgress(String projectId) {
    Objects.requireNonNull(projectId, "projectId must not be null");
    SecurityContext securityContext = SecurityContextHolder.getContext();
    return applySecurityContextMany(Flux.defer(
            () -> Flux.fromIterable(readRepository.findExperimentProgress(projectId))))
        .subscribeOn(scheduler)
        .contextWrite(reactiveSecurity(securityContext))
        .retryWhen(defaultRetryStrategy())
        .onErrorMap(cause -> mapToQueryException(cause, "Request for experiment progress failed"));
  }

  @Override
  public Mono<ExperimentProgress> getExperimentProgress(String projectId, String experimentId) {
    Objects.requireNonNull(experimentId, "experimentId must not be null");
    return getExperimentProgress(projectId)
        .filter(progress -> experimentId.equals(progress.experimentId()))
        .next();
  }

  @Override
  public Flux<ProjectSampleOverview> getProjectSamples(String projectId, int offset, int limit,
      ProjectSampleFilter filter) {
    Objects.requireNonNull(projectId, "projectId must not be null");
    Objects.requireNonNull(filter, "filter must not be null");
    SecurityContext securityContext = SecurityContextHolder.getContext();
    return applySecurityContextMany(Flux.defer(
            () -> Flux.fromIterable(
                readRepository.findProjectSamples(projectId, offset, limit, filter))))
        .subscribeOn(scheduler)
        .contextWrite(reactiveSecurity(securityContext))
        .retryWhen(defaultRetryStrategy())
        .onErrorMap(cause -> mapToQueryException(cause, "Request for project samples failed"));
  }

  @Override
  public Mono<Long> countProjectSamples(String projectId, ProjectSampleFilter filter) {
    Objects.requireNonNull(projectId, "projectId must not be null");
    Objects.requireNonNull(filter, "filter must not be null");
    SecurityContext securityContext = SecurityContextHolder.getContext();
    return applySecurityContext(
            Mono.defer(() -> Mono.just(readRepository.countProjectSamples(projectId, filter))))
        .subscribeOn(scheduler)
        .contextWrite(reactiveSecurity(securityContext))
        .retryWhen(defaultRetryStrategy())
        .onErrorMap(cause -> mapToQueryException(cause, "Request to count project samples failed"));
  }

  @Override
  public Flux<MeasurementDataRow> getMeasurementData(String projectId, String experimentId,
      int offset, int limit, MeasurementDataFilter filter) {
    Objects.requireNonNull(projectId, "projectId must not be null");
    Objects.requireNonNull(experimentId, "experimentId must not be null");
    Objects.requireNonNull(filter, "filter must not be null");
    SecurityContext securityContext = SecurityContextHolder.getContext();
    return applySecurityContextMany(Flux.defer(
            () -> Flux.fromIterable(
                readRepository.findMeasurementData(projectId, experimentId, offset, limit,
                    filter))))
        .subscribeOn(scheduler)
        .contextWrite(reactiveSecurity(securityContext))
        .retryWhen(defaultRetryStrategy())
        .onErrorMap(
            cause -> mapToQueryException(cause, "Request for measurement data failed"));
  }

  @Override
  public Mono<Long> countMeasurementData(String projectId, String experimentId,
      MeasurementDataFilter filter) {
    Objects.requireNonNull(projectId, "projectId must not be null");
    Objects.requireNonNull(experimentId, "experimentId must not be null");
    Objects.requireNonNull(filter, "filter must not be null");
    SecurityContext securityContext = SecurityContextHolder.getContext();
    return applySecurityContext(
            Mono.defer(() -> Mono.just(
                readRepository.countMeasurementData(projectId, experimentId, filter))))
        .subscribeOn(scheduler)
        .contextWrite(reactiveSecurity(securityContext))
        .retryWhen(defaultRetryStrategy())
        .onErrorMap(
            cause -> mapToQueryException(cause, "Request to count measurement data failed"));
  }

  @Override
  public Flux<RecentSample> getRecentSamples(String projectId) {
    Objects.requireNonNull(projectId, "projectId must not be null");
    SecurityContext securityContext = SecurityContextHolder.getContext();
    return applySecurityContextMany(Flux.defer(
            () -> Flux.fromIterable(readRepository.findRecentSamples(projectId))))
        .subscribeOn(scheduler)
        .contextWrite(reactiveSecurity(securityContext))
        .retryWhen(defaultRetryStrategy())
        .onErrorMap(cause -> mapToQueryException(cause, "Request for recent samples failed"));
  }

  @Override
  public Flux<RecentMeasurement> getRecentMeasurements(String projectId) {
    Objects.requireNonNull(projectId, "projectId must not be null");
    SecurityContext securityContext = SecurityContextHolder.getContext();
    return applySecurityContextMany(Flux.defer(
            () -> Flux.fromIterable(readRepository.findRecentMeasurements(projectId))))
        .subscribeOn(scheduler)
        .contextWrite(reactiveSecurity(securityContext))
        .retryWhen(defaultRetryStrategy())
        .onErrorMap(cause -> mapToQueryException(cause, "Request for recent measurements failed"));
  }

  @Override
  public Flux<RecentSample> getRecentSamples(String projectId, String experimentId) {
    Objects.requireNonNull(experimentId, "experimentId must not be null");
    return getRecentSamples(projectId)
        .filter(sample -> experimentId.equals(sample.experimentId()));
  }

  @Override
  public Flux<RecentMeasurement> getRecentMeasurements(String projectId, String experimentId) {
    Objects.requireNonNull(experimentId, "experimentId must not be null");
    return getRecentMeasurements(projectId)
        .filter(measurement -> experimentId.equals(measurement.experimentId()));
  }

  private static Retry defaultRetryStrategy() {
    return Retry.maxInARow(5)
        .doBeforeRetry(retrySignal -> log.warn("Query operation failed (" + retrySignal + ")"));
  }

  private static Throwable mapToQueryException(Throwable cause, String message) {
    Throwable error = Exceptions.isRetryExhausted(cause) && cause.getCause() != null
        ? cause.getCause()
        : cause;
    if (error instanceof ProjectQueryService.RequestFailedException
        || error instanceof ProjectQueryService.AccessDeniedException) {
      return error;
    }
    if (error instanceof org.springframework.security.access.AccessDeniedException) {
      return new ProjectQueryService.AccessDeniedException(message, error);
    }
    return new ProjectQueryService.RequestFailedException(message, error);
  }
}
