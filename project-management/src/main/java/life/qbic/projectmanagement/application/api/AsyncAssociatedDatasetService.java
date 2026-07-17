package life.qbic.projectmanagement.application.api;

import java.util.List;
import java.util.Optional;
import life.qbic.projectmanagement.application.associated_dataset.ConnectDatasetError;
import life.qbic.projectmanagement.domain.model.associated_dataset.AssociatedDatasetId;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import life.qbic.projectmanagement.domain.model.associated_dataset.SourceType;

/**
 * <b>Async Associated Dataset Service</b>
 *
 * <p>Project-reactor wrapper around the blocking
 * {@link life.qbic.projectmanagement.application.associated_dataset.AssociatedDatasetService}.
 *
 * <p>Offloads the external HTTP resolution (to InvenioRDM / Zenodo / FDAT)
 * plus the JPA persist step to a {@link reactor.core.scheduler.Schedulers#boundedElastic()}
 * worker thread so that the Vaadin UI thread does not stall. Multi-dataset
 * connect requests are fanned out with bounded parallelism (3) to remain
 * inside the unauthenticated rate limits of public InvenioRDM instances.</p>
 *
 * @since 1.12.0
 */
public interface AsyncAssociatedDatasetService {

  /**
   * Connects a single dataset to a project. Returns a {@link Mono} that
   * publishes exactly one {@link ConnectDatasetResponse} — either the
   * resulting {@link AssociatedDatasetId} or the
   * {@link ConnectDatasetError} — on subscription.
   */
  Mono<ConnectDatasetResponse> connectDataset(ConnectDatasetRequest request);

  /**
   * Connects a batch of datasets to a project. Each dataset is resolved
   * concurrently with bounded parallelism. Responses are emitted in
   * <em>insertion order</em> so the UI can map responses back to the
   * original grid rows by {@link ConnectDatasetRequest#requestId()}.
   */
  Flux<ConnectDatasetResponse> connectDatasets(List<ConnectDatasetRequest> requests);

  /**
   * Per-request payload. Mirrors the arguments of the blocking
   * {@link life.qbic.projectmanagement.application.associated_dataset.AssociatedDatasetService#connectDataset}.
   *
   * @param requestId            caller-supplied correlation id, echoed in the response
   * @param projectId            project to connect the dataset to
   * @param sourceType           external source system type
   * @param instanceId           configured instance identifier (e.g. "zenodo")
   * @param externalHandleValue  external record identifier on the source
   * @param experimentId         optional experiment to associate the dataset with
   * @param userId               user performing the action (used for notifications)
   */
  record ConnectDatasetRequest(
      String requestId,
      ProjectId projectId,
      SourceType sourceType,
      String instanceId,
      String externalHandleValue,
      Optional<ExperimentId> experimentId,
      String userId
  ) {}

  /**
   * Per-response payload. Exactly one of {@link #associatedDatasetId()}
   * or {@link #error()} is non-null.
   */
  record ConnectDatasetResponse(
      String requestId,
      AssociatedDatasetId associatedDatasetId,
      ConnectDatasetError error
  ) {}
}
