package life.qbic.projectmanagement.application.api;

import java.time.Duration;
import java.util.List;
import life.qbic.application.commons.Result;
import life.qbic.projectmanagement.application.associated_dataset.AssociatedDatasetService;
import life.qbic.projectmanagement.application.associated_dataset.ConnectDatasetError;
import life.qbic.projectmanagement.domain.model.associated_dataset.AssociatedDatasetId;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * <b>Async Associated Dataset Service Implementation</b>
 *
 * <p>Default implementation of {@link AsyncAssociatedDatasetService}.
 * Delegates to the blocking {@link AssociatedDatasetService} inside a
 * {@link Mono#fromCallable} wrapper so the work runs off the Vaadin UI
 * thread. Bounded parallelism (3) is used for multi-dataset connects to
 * stay within the unauthenticated rate limit of public InvenioRDM
 * instances (e.g. Zenodo).</p>
 *
 * @since 1.12.0
 */
public class AsyncAssociatedDatasetServiceImpl implements AsyncAssociatedDatasetService {

  private static final Duration PER_REQUEST_TIMEOUT = Duration.ofSeconds(30);
  private static final int BOUNDED_PARALLELISM = 3;

  private final AssociatedDatasetService syncService;

  public AsyncAssociatedDatasetServiceImpl(AssociatedDatasetService syncService) {
    this.syncService = syncService;
  }

  @Override
  public Mono<ConnectDatasetResponse> connectDataset(ConnectDatasetRequest request) {
    return Mono.fromCallable(() -> {
           Result<AssociatedDatasetId, ConnectDatasetError> result = syncService.connectDataset(
               request.projectId(),
               request.sourceType(),
               request.instanceId(),
               request.externalHandleValue(),
               request.experimentId(),
               request.userId());
           return new ConnectDatasetResponse(
               request.requestId(),
               result.fold(value -> value, error -> null),
               result.fold(value -> null, error -> error));
         })
         .subscribeOn(Schedulers.boundedElastic())
         .timeout(PER_REQUEST_TIMEOUT)
         .onErrorResume(Throwable.class, t ->
             Mono.just(new ConnectDatasetResponse(
                 request.requestId(), null, ConnectDatasetError.CONNECT_FAILED)));
  }

  @Override
  public Flux<ConnectDatasetResponse> connectDatasets(List<ConnectDatasetRequest> requests) {
    return Flux.fromIterable(requests)
        .flatMapSequential(this::connectDataset, BOUNDED_PARALLELISM)
        .subscribeOn(Schedulers.boundedElastic());
  }
}
