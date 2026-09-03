package life.qbic.projectmanagement.application.associated_dataset;

import static java.util.Objects.requireNonNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import life.qbic.application.commons.Result;
import life.qbic.domain.concepts.DomainEvent;
import life.qbic.domain.concepts.DomainEventDispatcher;
import life.qbic.domain.concepts.DomainEventSubscriber;
import life.qbic.domain.concepts.LocalDomainEventDispatcher;
import life.qbic.identity.api.UserInformationService;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.authorization.ReactiveSecurityContextUtils;
import life.qbic.projectmanagement.application.experiment.ExperimentInformationService;
import life.qbic.projectmanagement.domain.model.associated_dataset.AccessLevel;
import life.qbic.projectmanagement.domain.model.associated_dataset.AssociatedDataset;
import life.qbic.projectmanagement.domain.model.associated_dataset.AssociatedDatasetId;
import life.qbic.projectmanagement.domain.model.associated_dataset.ExternalHandle;
import life.qbic.projectmanagement.domain.model.associated_dataset.InvenioRdmResourceMetadata;
import life.qbic.projectmanagement.domain.model.associated_dataset.ResourceMetadata;
import life.qbic.projectmanagement.domain.model.associated_dataset.SourceType;
import life.qbic.projectmanagement.domain.model.associated_dataset.event.AssociatedDatasetConnectedEvent;
import life.qbic.projectmanagement.domain.model.associated_dataset.event.AssociatedDatasetRemovedEvent;
import life.qbic.projectmanagement.domain.model.associated_dataset.event.AssociatedDatasetsSyncedEvent;
import life.qbic.projectmanagement.domain.model.associated_dataset.repository.AssociatedDatasetRepository;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Application service for associating external datasets with projects.
 *
 * <p>Orchestrates the use cases of searching external data sources,
 * connecting datasets to projects, and listing connected datasets.
 * Depends on the {@link DatasetSource} port for external system
 * interaction and the {@link AssociatedDatasetRepository} for
 * persistence.</p>
 *
 * <p>Per ADR-0003, operations are user-initiated and use the invoking
 * user's identity. Credentials are never borrowed between users.</p>
 *
 * @since 1.12.0
 */
@Service
public class AssociatedDatasetService {

  private static final Logger log = LoggerFactory.logger(AssociatedDatasetService.class);

  private final DatasetSource datasetSource;
  private final AssociatedDatasetRepository associatedDatasetRepository;
  private final SourceInstanceRegistry sourceInstanceRegistry;
  private final ProjectInformationService projectInformationService;
  private final UserInformationService userInformationService;
  private final ExperimentInformationService experimentInformationService;

  public AssociatedDatasetService(
      DatasetSource datasetSource,
      AssociatedDatasetRepository associatedDatasetRepository,
      SourceInstanceRegistry sourceInstanceRegistry,
      ProjectInformationService projectInformationService,
      UserInformationService userInformationService,
      ExperimentInformationService experimentInformationService) {
    this.datasetSource = requireNonNull(datasetSource, "datasetSource must not be null");
    this.associatedDatasetRepository = requireNonNull(associatedDatasetRepository,
        "associatedDatasetRepository must not be null");
    this.sourceInstanceRegistry = requireNonNull(sourceInstanceRegistry,
        "sourceInstanceRegistry must not be null");
    this.projectInformationService = requireNonNull(projectInformationService,
        "projectInformationService must not be null");
    this.userInformationService = requireNonNull(userInformationService,
        "userInformationService must not be null");
    this.experimentInformationService = requireNonNull(experimentInformationService,
        "experimentInformationService must not be null");
  }

  // ── Search ──────────────────────────────────────────────────────────────

  /**
   * Searches an external data source for datasets matching the query,
   * returning paginated results.
   *
   * <p>No project-level permission is required for the search itself —
   * the user must already have access to the project in order to be
   * in the datasets view. The search is an external lookup and does
   * not read or mutate project data.</p>
   *
   * <p>The {@code searchingUserId} is forwarded to the infrastructure
   * adapter so it can resolve any per-user credentials needed (e.g.
   * for restricted dataset search). The application layer never handles
   * authentication material (ADR-0002 D1).</p>
   *
   * @param sourceType     the source system type (e.g. {@link SourceType#INVENIO_RDM})
   * @param instanceId     the configured instance identifier (e.g. "zenodo")
   * @param query          the free-text search term; blank for "list all"
   * @param page           zero-indexed page number
   * @param pageSize       results per page
   * @param searchingUserId the ID of the user performing the search
   *                       (used by the adapter to resolve per-user credentials)
   * @return paginated search results
   * @throws DatasetSourceUnavailableException if the external data repository
   *         could not be reached (network failure, timeout, rate-limit,
   *         server error) or returned unparseable data
   * @throws DatasetSourceNotFoundException   if the {@code instanceId} does
   *         not match any configured repository
   */
  public SearchResult searchDatasets(
      SourceType sourceType,
      String instanceId,
      String query,
      int page,
      int pageSize,
      String searchingUserId) {
    return searchDatasets(sourceType, instanceId, query, null,
        page, pageSize, searchingUserId);
  }

  /**
   * Searches an external data source for datasets matching the query,
   * with an optional access-status filter, returning paginated results.
   *
   * <p>The {@code accessFilter} restricts results to records with a
   * specific access status on the source system.
   * {@link DatasetAccessFilter#RESTRICTED} returns only access-restricted
   * records; {@link DatasetAccessFilter#PUBLIC} returns only publicly
   * accessible records; {@code null} returns all records (no filter).
   * The filter is source-neutral — translation to a source-specific wire
   * term happens in the source adapter.</p>
   *
   * @param sourceType      the source system type
   * @param instanceId      the configured instance identifier
   * @param query           the free-text search term; blank for "list all"
   * @param accessFilter    optional access-status filter, or {@code null}
   *                        for no filter
   * @param page            zero-indexed page number
   * @param pageSize        results per page
   * @param searchingUserId the ID of the user performing the search
   * @return paginated search results
   * @throws DatasetSourceUnavailableException if the external data repository
   *         could not be reached
   * @throws DatasetSourceNotFoundException   if the {@code instanceId} does
   *         not match any configured repository
   * @since 1.12.0
   */
  public SearchResult searchDatasets(
      SourceType sourceType,
      String instanceId,
      String query,
      DatasetAccessFilter accessFilter,
      int page,
      int pageSize,
      String searchingUserId) {
    Objects.requireNonNull(sourceType, "sourceType must not be null");
    Objects.requireNonNull(searchingUserId, "searchingUserId must not be null");
    // resolveInstanceConfig propagates DatasetSourceNotFoundException directly
    var config = resolveInstanceConfig(instanceId);
    var searchQuery = new SearchQuery(query, page, pageSize, accessFilter);
    try {
      return datasetSource.search(searchQuery, config, searchingUserId);
    } catch (AssociatedDatasetServiceException
        | DatasetSearchException e) {
      // interface-level exception already translated — propagate unchanged
      throw e;
    } catch (Exception e) {
      // translate any infrastructure exception (ApplicationException from
      // the DatasetSource port, or truly unexpected runtime errors) into
      // a user-friendly service exception. Technical details stay in the
      // log; the message reaching the caller contains neither URLs,
      // status codes, nor infrastructure names.
      log.error("External search failed for user %s on source type %s".formatted(
          searchingUserId, sourceType), e);
      throw new DatasetSourceUnavailableException(e);
    }
  }

  // ── Connect ─────────────────────────────────────────────────────────────

  /**
   * Connects an external dataset to a project.
   *
   * <p>Resolves the record metadata from the source system, creates the
   * {@link AssociatedDataset} aggregate, persists it, and dispatches
   * domain events (driving notifications to other project members).</p>
   *
   * <p>Requires WRITE permission on the project (ADR-0003).</p>
   *
   * @param projectId           the project to connect the dataset to
   * @param sourceType          the source system type
   * @param instanceId          the configured instance identifier
   * @param externalHandleValue the external record identifier on the source
   * @param experimentId        optional experiment to associate the dataset with
   * @param connectedByUserId   the user ID performing the action
   * @return the resulting {@link AssociatedDatasetId} on success, or an
   *         error code on failure
   */
  @PreAuthorize(
      "hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE')")
  public Result<AssociatedDatasetId, ConnectDatasetError> connectDataset(
      ProjectId projectId,
      SourceType sourceType,
      String instanceId,
      String externalHandleValue,
      @Nullable ExperimentId experimentId,
      String connectedByUserId) {

    Objects.requireNonNull(projectId, "projectId must not be null");
    Objects.requireNonNull(sourceType, "sourceType must not be null");
    Objects.requireNonNull(externalHandleValue, "externalHandleValue must not be null");
    Objects.requireNonNull(connectedByUserId, "connectedByUserId must not be null");
    // experimentId is @Nullable — null means no experiment association

    InstanceConfig config;
    try {
      config = resolveInstanceConfig(instanceId);
    } catch (DatasetSourceNotFoundException e) {
      log.error("Instance not found: %s".formatted(instanceId));
      return Result.fromError(ConnectDatasetError.INSTANCE_NOT_FOUND);
    }

    // 1. Resolve record metadata from the source system
    Optional<ResourceMetadata> metadata;
    try {
      metadata = datasetSource.resolveMetadata(externalHandleValue, config, connectedByUserId);
    } catch (DatasetResolveException e) {
      log.error("Failed to resolve metadata for record %s on instance %s"
          .formatted(externalHandleValue, instanceId), e);
      return Result.fromError(ConnectDatasetError.CONNECT_FAILED);
    } catch (Exception e) {
      // Non-infrastructure errors (shouldn't happen — the interface
      // only declares the two exception types above) are treated as
      // record-not-found to keep the error-path simple.
      log.error("Unexpected error resolving metadata for record %s on instance %s"
          .formatted(externalHandleValue, instanceId), e);
      return Result.fromError(ConnectDatasetError.RECORD_NOT_FOUND);
    }
    if (metadata.isEmpty()) {
      log.warn("Record %s not found on instance %s".formatted(externalHandleValue, instanceId));
      return Result.fromError(ConnectDatasetError.RECORD_NOT_FOUND);
    }

    // 1a. Duplicate check — a dataset with the same PID is already
    //     connected to this project. PID is the dedup key because it is
    //     globally unique and persistent by design (DOI/PID).
    //     This check must happen BEFORE any access link creation: for an
    //     already-connected dataset we short-circuit here without having
    //     provisioned a new access link on the source system that would
    //     otherwise go unused (an orphaned link).
    ResourceMetadata finalMetadata = metadata.get();
    boolean alreadyConnected;
    try {
      alreadyConnected = associatedDatasetRepository.isActiveConnectionPresent(
          projectId, finalMetadata.pid());
    } catch (Exception e) {
      // Persistence/query failures (schema mismatch, table unavailable, etc.)
      // must not silently swallow the connect attempt — log the cause so
      // it surfaces in the application log rather than disappearing into
      // the reactive pipeline's onErrorResume catch-all.
      log.error("Duplicate-check query failed for project %s / PID %s"
          .formatted(projectId, finalMetadata.pid()), e);
      return Result.fromError(ConnectDatasetError.CONNECT_FAILED);
    }
    if (alreadyConnected) {
      log.info("Dataset with PID %s is already connected to project %s — skipping"
          .formatted(finalMetadata.pid(), projectId));
      return Result.fromError(ConnectDatasetError.ALREADY_CONNECTED);
    }

    // 1b. Credential gate — access-restricted datasets require a valid
    //     credential on the source instance. Without a valid PAT, the
    //     access link creation needed for project collaborators will
    //     fail. Hard-block the connect rather than silently succeeding
    //     with an unusable connection.
    //
    //     The created access link (and its link id, needed to revoke it)
    //     is kept in scope for the rest of the connect so that, if a
    //     subsequent step fails, the link is rolled back (revoked) instead
    //     of leaking an orphaned, unused shareable link on the source.
    CreatedAccessLink createdAccessLink = null;
    if (finalMetadata instanceof InvenioRdmResourceMetadata inv) {
      if (inv.deriveAccessLevel() == AccessLevel.RESTRICTED) {
        if (!datasetSource.hasValidCredential(connectedByUserId, config)) {
          log.warn("Cannot connect restricted dataset %s — no valid credential "
              + "for user %s on instance %s"
              .formatted(externalHandleValue, connectedByUserId, instanceId));
          return Result.fromError(ConnectDatasetError.CREDENTIAL_REQUIRED);
        }

        // Create sharable access link for project collaborators
        try {
          CreatedAccessLink link = datasetSource.createAccessLink(
              externalHandleValue, config, connectedByUserId);
          createdAccessLink = link;
          // Update metadata with the instance, access link, and link id
          // (the id is needed to revoke the link later).
          finalMetadata = new InvenioRdmResourceMetadata(
              inv.title(), inv.pid(), inv.version(), link.url(),
              inv.resourceProvider(), inv.creators(), inv.resourceType(),
              inv.community(), inv.publicationDate(), inv.description(),
              inv.recordAccess(), inv.fileAccess(),
              instanceId, link.linkId(), inv.parentHandle());
        } catch (AccessLinkCreationException e) {
          log.error("Failed to create access link for restricted dataset %s on instance %s"
              .formatted(externalHandleValue, instanceId), e);
          return Result.fromError(ConnectDatasetError.ACCESS_LINK_CREATION_FAILED);
        }
      } else {
        // Persist the instance id on public connections as well so that a
        // later sync can resolve the source instance without heuristics
        // (ADR-0005). The access link fields stay empty for public records.
        finalMetadata = new InvenioRdmResourceMetadata(
            inv.title(), inv.pid(), inv.version(), inv.accessLink(),
            inv.resourceProvider(), inv.creators(), inv.resourceType(),
            inv.community(), inv.publicationDate(), inv.description(),
            inv.recordAccess(), inv.fileAccess(),
            instanceId, null, inv.parentHandle());
      }
    }

    // 2. Set up local event dispatcher to cache events emitted during the
    //    aggregate creation, then forward them to the global domain event
    //    dispatcher after save (collect-during, forward-after pattern).
    List<DomainEvent> domainEventsCache = new ArrayList<>();
    var localDomainEventDispatcher = LocalDomainEventDispatcher.instance();
    localDomainEventDispatcher.reset();
    localDomainEventDispatcher.subscribe(
        new ConnectedEventCollectorSubscriber(domainEventsCache));

    // 3. Create the aggregate (emits AssociatedDatasetConnectedEvent)
    AssociatedDataset dataset;
    try {
      dataset = AssociatedDataset.connect(
          projectId,
          sourceType,
          new ExternalHandle(externalHandleValue),
          finalMetadata,
          connectedByUserId,
          experimentId);
    } catch (Exception e) {
      log.error("Failed to create associated dataset aggregate", e);
      revokeAccessLinkIfCreated(createdAccessLink, externalHandleValue, config,
          connectedByUserId);
      return Result.fromError(ConnectDatasetError.CONNECT_FAILED);
    }

    // 4. Persist
    try {
      associatedDatasetRepository.save(dataset);
    } catch (Exception e) {
      log.error("Failed to persist associated dataset for project %s".formatted(projectId), e);
      revokeAccessLinkIfCreated(createdAccessLink, externalHandleValue, config,
          connectedByUserId);
      return Result.fromError(ConnectDatasetError.CONNECT_FAILED);
    }

    // 5. Forward cached events to the global domain event dispatcher
    //    (this triggers notification policy directives like
    //    InformProjectMembersAboutDatasetConnection from Task 6).
    //    The dispatch must not fail the connect itself — the dataset
    //    has already been persisted in step 4 — so any exception here
    //    is logged and swallowed so the connect still returns success.
    try {
      domainEventsCache.forEach(
          domainEvent -> DomainEventDispatcher.instance().dispatch(domainEvent));
    } catch (Exception e) {
      log.warn("Event dispatch failed while forwarding domain event "
          + "after dataset connection on project %s; the connection ".formatted(projectId)
          + "itself succeeded, but collaborators may not have been "
          + "notified: %s".formatted(e.getMessage()));
    }

    log.info("Dataset %s connected to project %s by user %s"
        .formatted(dataset.id(), projectId, connectedByUserId));

    return Result.fromValue(dataset.id());
  }

  // ── Reactive (non-blocking) connect ─────────────────────────────────────

  private static final Duration PER_REQUEST_TIMEOUT = Duration.ofSeconds(30);
  private static final int BOUNDED_PARALLELISM = 3;

  /**
   * Per-request payload mirroring {@link #connectDataset}. Used by the
   * reactive {@link #connectDatasetAsync(ConnectDatasetRequest)} and
   * {@link #connectDatasets}. Carries a {@code requestId} so the UI can
   * correlate responses back to the original grid row.
   */
  public record ConnectDatasetRequest(
      String requestId,
      ProjectId projectId,
      SourceType sourceType,
      String instanceId,
      String externalHandleValue,
      @Nullable ExperimentId experimentId,
      String userId
  ) {}

  /**
   * Per-response payload returned by the reactive connect methods.
   * Exactly one of {@link #associatedDatasetId()} or {@link #error()} is
   * non-null.
   */
  public record ConnectDatasetResponse(
      String requestId,
      AssociatedDatasetId associatedDatasetId,
      ConnectDatasetError error
  ) {}

  /**
   * Reactive counterpart of {@link #connectDataset}. Runs the blocking
   * call on a {@link Schedulers#boundedElastic()} worker thread, with a
   * per-request 30s timeout. Errors (including timeout and network
   * exceptions) are wrapped into a
   * {@link ConnectDatasetError#CONNECT_FAILED} so the reactive stream
   * never terminates in {@code onError} — enabling the UI to tally
   * partial successes and failures independently.
   */
  @PreAuthorize(
      "hasPermission(#request.projectId(), 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE')")
  public Mono<ConnectDatasetResponse> connectDatasetAsync(ConnectDatasetRequest request) {
    SecurityContext securityContext = SecurityContextHolder.getContext();
    return Mono.fromCallable(() -> {
           Result<AssociatedDatasetId, ConnectDatasetError> result = connectDataset(
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
        // Propagate caller's SecurityContext to the boundedElastic worker
        // thread so Spring Security `@PreAuthorize` on connectDataset
        // resolves correctly.
        .contextWrite(ReactiveSecurityContextUtils.reactiveSecurity(securityContext))
        .subscribeOn(Schedulers.boundedElastic())
        .timeout(PER_REQUEST_TIMEOUT)
        .onErrorResume(Throwable.class, t -> {
          // Safety net: any exception escaping connectDataset() (schema
          // errors, unexpected runtime exceptions, timeouts) is converted
          // into CONNECT_FAILED so the caller can tally partial failures.
          // The log.error here is critical — without it, uncaught errors
          // become silent failures that the user can only see as a
          // generic toast with no entry in the application log.
          log.error("Async connect pipeline failed for request %s: %s"
              .formatted(request.requestId(), t.getMessage()), t);
          return Mono.just(new ConnectDatasetResponse(
              request.requestId(), null, ConnectDatasetError.CONNECT_FAILED));
        });
  }

  /**
   * Connects a batch of datasets to a project. Each dataset is resolved
   * concurrently with bounded parallelism (3, matching unauthenticated
   * rate limits of public InvenioRDM instances). Responses are emitted
   * in insertion order so the UI can map them back to grid rows.
   */
  public Flux<ConnectDatasetResponse> connectDatasets(List<ConnectDatasetRequest> requests) {
    return Flux.fromIterable(requests)
        .flatMapSequential(this::connectDatasetAsync, BOUNDED_PARALLELISM);
  }

  // ── Remove dataset ──────────────────────────────────────────────────────

  /**
   * Removes (soft-deletes) an existing dataset connection from a project.
   *
   * <p>The aggregate transitions to
   * {@link life.qbic.projectmanagement.domain.model.associated_dataset.ConnectionState#REMOVED}
   * and is retained in the database as an audit tombstone
   * (ADR-0001). A {@link AssociatedDatasetRemovedEvent} is emitted so the
   * collaborator-notification policy directive can inform other project
   * members via email.</p>
   *
   * <p>Per ADR-0003, only users with {@code WRITE} permission on the
   * parent project may remove a connection — callers must have resolved
   * the project permission before invoking this method (the aggregate
   * does not carry a separate ACL entry). If the caller lacks WRITE
   * permission this method must not be invoked; the UI layer is
   * responsible for hiding or disabling the remove action.</p>
   *
   * @param associatedDatasetIdStr the identifier of the dataset connection
   *                               to remove (UUID string)
   * @param removedByUserId        the user performing the removal;
   *                               recorded in the emitted event as the
   *                               actor (may differ from the original
   *                               connector)
   * @return success with the removed dataset's ID, or an error code
   * @throws NullPointerException if either argument is {@code null}
   * @since 1.12.0
   */
  public Result<AssociatedDatasetId, RemoveDatasetError> removeDataset(
      String associatedDatasetIdStr, String removedByUserId) {
    Objects.requireNonNull(associatedDatasetIdStr, "associatedDatasetId must not be null");
    Objects.requireNonNull(removedByUserId, "removedByUserId must not be null");

    // 1. Lookup the dataset — not found → DATASET_NOT_FOUND
    var parsedId = AssociatedDatasetId.parse(associatedDatasetIdStr);
    Optional<AssociatedDataset> datasetOpt;
    try {
      datasetOpt = associatedDatasetRepository.findById(parsedId);
    } catch (Exception e) {
      // Persistence/query failures are surfaced as REMOVAL_FAILED (the
      // lookup is part of the removal process).
      log.error("Repository lookup failed for dataset %s"
          .formatted(associatedDatasetIdStr), e);
      return Result.fromError(RemoveDatasetError.REMOVAL_FAILED);
    }
    if (datasetOpt.isEmpty()) {
      log.warn("Dataset %s not found in repository — cannot remove"
          .formatted(associatedDatasetIdStr));
      return Result.fromError(RemoveDatasetError.DATASET_NOT_FOUND);
    }
    var dataset = datasetOpt.orElseThrow();

    // 2. If already removed, return DATASET_ALREADY_REMOVED.
    //    (dataset.remove(…) would throw IllegalStateException for this —
    //    translate cleanly rather than propagating the runtime exception.)
    if (!dataset.isConnected()) {
      log.info("Dataset %s is already in REMOVED state — skipping"
          .formatted(associatedDatasetIdStr));
      return Result.fromError(RemoveDatasetError.DATASET_ALREADY_REMOVED);
    }

    // 3. Set up local event dispatcher to cache events emitted during the
    //    aggregate mutation, then forward them to the global domain event
    //    dispatcher after save (collect-during, forward-after pattern).
    List<DomainEvent> domainEventsCache = new ArrayList<>();
    var localDomainEventDispatcher = LocalDomainEventDispatcher.instance();
    localDomainEventDispatcher.reset();
    localDomainEventDispatcher.subscribe(
        new RemovedEventCollectorSubscriber(domainEventsCache));

    // 4. Domain mutation (emits AssociatedDatasetRemovedEvent)
    try {
      dataset.remove(removedByUserId);
    } catch (IllegalStateException e) {
      // Defensive — already covered by the isConnected() check above,
      // but keep as a safety net (e.g. concurrent remove race).
      log.warn("Concurrent removal detected on dataset %s: %s"
          .formatted(associatedDatasetIdStr, e.getMessage()));
      return Result.fromError(RemoveDatasetError.DATASET_ALREADY_REMOVED);
    } catch (Exception e) {
      log.error("Failed removing dataset. Domain mutation failed for dataset %s"
          .formatted(associatedDatasetIdStr), e);
      return Result.fromError(RemoveDatasetError.REMOVAL_FAILED);
    }

    // 5. Persist the state transition
    try {
      associatedDatasetRepository.save(dataset);
    } catch (Exception e) {
      log.error("Failed to persist removal of dataset %s"
          .formatted(associatedDatasetIdStr), e);
      return Result.fromError(RemoveDatasetError.REMOVAL_FAILED);
    }

    // 5a. Best-effort revocation of the shareable access link (if the
    //     removed connection had created one on the source, e.g. a
    //     restricted InvenioRDM dataset). The removal must not fail if
    //     the external revoke fails — the local soft-delete stays
    //     authoritative and the failure is logged for manual clean-up.
    revokeAccessLinkIfPresent(dataset, removedByUserId);

    // 6. Forward cached events to the global dispatcher — same best-effort
    //    semantics as connectDataset(): failure here is logged but does not
    //    roll the removal back (the dataset state is already saved).
    try {
      domainEventsCache.forEach(
          domainEvent -> DomainEventDispatcher.instance().dispatch(domainEvent));
    } catch (Exception e) {
      log.warn("Event dispatch failed while forwarding removal domain event "
          + "after dataset removal on project %s; the removal itself succeeded, ".formatted(dataset.projectId())
          + "but collaborators may not have been notified: %s".formatted(e.getMessage()));
    }

    log.info("Dataset %s removed from project %s by user %s"
        .formatted(dataset.id(), dataset.projectId(), removedByUserId));

    return Result.fromValue(dataset.id());
  }

  // ── Reactive (non-blocking) remove ──────────────────────────────────────

  /**
   * Reactive counterpart of {@link #removeDataset}. Runs the blocking
   * call on a {@link Schedulers#boundedElastic()} worker thread. Errors (including
   * timeout and infrastructure exceptions) are wrapped into a
   * {@link RemoveDatasetError#REMOVAL_FAILED} so the reactive stream
   * never terminates in {@code onError} — enabling the UI to show a
   * generic failure toast while the application log captures the full
   * cause.
   */
  public Mono<Result<AssociatedDatasetId, RemoveDatasetError>> removeDatasetAsync(
      String associatedDatasetId, String removedByUserId) {
    SecurityContext securityContext = SecurityContextHolder.getContext();
    return Mono.fromCallable(() -> removeDataset(associatedDatasetId, removedByUserId))
        // Propagate the caller's SecurityContext to the boundedElastic
        // worker thread so Spring Security {@code @PreAuthorize} on
        // removeDataset resolves correctly.
        .contextWrite(ReactiveSecurityContextUtils.reactiveSecurity(securityContext))
        .subscribeOn(Schedulers.boundedElastic())
        .onErrorResume(Throwable.class, t -> {
          // Safety net: any exception escaping removeDataset() (schema
          // errors, unexpected runtime exceptions, timeouts) is converted
          // into REMOVAL_FAILED so the UI can show a generic failure
          // toast. The log.error here is critical — without it, uncaught
          // errors become silent failures only visible in the application
          // log, not in the user's feedback.
          log.error("Async remove pipeline failed for dataset %s: %s"
              .formatted(associatedDatasetId, t.getMessage()), t);
          return Mono.just(Result.fromError(RemoveDatasetError.REMOVAL_FAILED));
        });
  }

  // ── Sync connected datasets ────────────────────────────────────────────

  /**
   * Synchronises the given connected datasets of a project with their
   * source instances (DATSET-04/08, ADR-0005).
   *
   * <p>Each dataset is processed on a bounded-elastic worker thread with
   * bounded parallelism (matching the connect flow) and a per-request
   * timeout. Responses are emitted in insertion order so the caller can
   * map them back to the requested rows.</p>
   *
   * <p>Requires {@code WRITE} permission on the project (story ACs — a
   * sync updates the project's linked snapshot; amends ADR-0003 §5).</p>
   *
   * <p>After the trigger completes, a single
   * {@link AssociatedDatasetsSyncedEvent} is dispatched — only when at
   * least one dataset was actually updated — so the notification
   * directive can send one combined email to the project members
   * (ADR-0005 N1).</p>
   *
   * @param projectId the project the datasets belong to
   * @param datasetIds the connections to sync (may be a single id)
   * @param userId    the invoking user — the only identity whose
   *                  credentials are used (never-borrow-credentials)
   * @return per-dataset outcomes in request order
   */
  @PreAuthorize(
      "hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE')")
  public Flux<SyncDatasetResponse> syncDatasets(
      ProjectId projectId, List<AssociatedDatasetId> datasetIds, String userId) {
    Objects.requireNonNull(projectId, "projectId must not be null");
    Objects.requireNonNull(datasetIds, "datasetIds must not be null");
    Objects.requireNonNull(userId, "userId must not be null");

    SecurityContext securityContext = SecurityContextHolder.getContext();
    List<SyncDatasetResponse> updated = new CopyOnWriteArrayList<>();
    return Flux.fromIterable(datasetIds)
        .flatMapSequential(id -> Mono.fromCallable(() ->
                syncDatasetCore(projectId, id, userId))
            // Propagate the caller's SecurityContext to the worker thread
            // so Spring Security `@PreAuthorize` on the public method
            // resolves correctly.
            .contextWrite(ReactiveSecurityContextUtils.reactiveSecurity(securityContext))
            .subscribeOn(Schedulers.boundedElastic())
            .timeout(PER_REQUEST_TIMEOUT)
            .onErrorResume(Throwable.class, t -> {
              // Safety net: any exception escaping syncDatasetCore is
              // converted into SYNC_FAILED so the caller can tally
              // partial failures.
              log.error("Async sync pipeline failed for dataset %s: %s"
                  .formatted(id.value(), t.getMessage()), t);
              return Mono.just(SyncDatasetResponse.failed(id, SyncDatasetError.SYNC_FAILED));
            }), BOUNDED_PARALLELISM)
        .doOnNext(response -> {
          if (response.status() == SyncDatasetResponse.SyncStatus.UPDATED) {
            updated.add(response);
          }
        })
        .doOnComplete(() -> emitSyncSummaryEvent(projectId, userId, updated));
  }

  /**
   * Blocking sync of one dataset (runs on a worker thread via
   * {@link #syncDatasets}).
   */
  private SyncDatasetResponse syncDatasetCore(
      ProjectId projectId, AssociatedDatasetId datasetId, String userId) {

    // 1. Load the connection
    Optional<AssociatedDataset> foundOpt;
    try {
      foundOpt = associatedDatasetRepository.findById(datasetId);
    } catch (Exception e) {
      log.error("Sync lookup failed for dataset %s".formatted(datasetId.value()), e);
      return SyncDatasetResponse.failed(datasetId, SyncDatasetError.SYNC_FAILED);
    }
    if (foundOpt.isEmpty() || !foundOpt.get().isConnected()) {
      return SyncDatasetResponse.failed(datasetId, SyncDatasetError.DATASET_NOT_FOUND);
    }
    var dataset = foundOpt.get();

    // 2. Resolve the instance this dataset belongs to
    InstanceConfig config;
    try {
      config = resolveInstanceConfigForDataset(dataset);
    } catch (DatasetSourceNotFoundException e) {
      log.warn("Cannot sync dataset %s — no configured instance matches"
          .formatted(datasetId.value()));
      return SyncDatasetResponse.failed(datasetId, SyncDatasetError.SYNC_FAILED);
    }

    ResourceMetadata storedMetadata = dataset.resourceMetadata();
    boolean storedRestricted = storedMetadata instanceof InvenioRdmResourceMetadata inv
        && inv.deriveAccessLevel() == AccessLevel.RESTRICTED;

    // 3. Short-circuit: metadata-restricted record without a usable
    //    credential (ADR-0005 A1 — deterministic, no HTTP call needed)
    if (storedRestricted && !datasetSource.hasValidCredential(userId, config)) {
      log.info("Sync of restricted dataset %s skipped — no valid credential for user %s on instance %s"
          .formatted(datasetId.value(), userId, config.id()));
      return SyncDatasetResponse.failed(datasetId, SyncDatasetError.CREDENTIAL_REQUIRED);
    }

    // 4. Resolve the latest state (follows the version chain)
    Optional<ResolvedRecord> resolvedOpt;
    try {
      resolvedOpt = datasetSource.resolveLatest(
          dataset.externalHandle().value(), config, userId);
    } catch (DatasetAccessDeniedException e) {
      boolean hasCredential = datasetSource.hasValidCredential(userId, config);
      return SyncDatasetResponse.failed(datasetId, hasCredential
          ? SyncDatasetError.CREDENTIAL_INSUFFICIENT
          : SyncDatasetError.CREDENTIAL_REQUIRED);
    } catch (DatasetResolveException e) {
      log.error("Sync resolve failed for dataset %s".formatted(datasetId.value()), e);
      return SyncDatasetResponse.failed(datasetId, SyncDatasetError.SYNC_FAILED);
    }
    if (resolvedOpt.isEmpty()) {
      return SyncDatasetResponse.failed(datasetId, SyncDatasetError.RECORD_NOT_FOUND);
    }
    ResolvedRecord resolved = resolvedOpt.get();
    if (!(resolved.metadata() instanceof InvenioRdmResourceMetadata latestMetadata)) {
      log.error("Sync produced unsupported metadata type for dataset %s"
          .formatted(datasetId.value()));
      return SyncDatasetResponse.failed(datasetId, SyncDatasetError.SYNC_FAILED);
    }

    // 5. Did the record move? (new version / new record id)
    String storedVersion = storedMetadata.version();
    boolean versionChanged = !Objects.equals(storedVersion, latestMetadata.version());
    boolean handleChanged = !Objects.equals(
        resolved.externalHandleValue(), dataset.externalHandle().value());
    boolean recordChanged = versionChanged || handleChanged;

    // 6. Duplicate guard: another active connection already carries the
    //    target version's PID — do not create a duplicate (plan §Edge cases)
    if (recordChanged && !Objects.equals(latestMetadata.pid(), storedMetadata.pid())
        && associatedDatasetRepository.isActiveConnectionPresent(projectId, latestMetadata.pid())) {
      log.info("Sync of dataset %s skipped — a connection to version %s "
          + "already exists in project %s"
          .formatted(datasetId.value(), latestMetadata.pid(), projectId.value()));
      return SyncDatasetResponse.failed(datasetId, SyncDatasetError.ALREADY_CONNECTED);
    }

    // 7. Access-link refresh for restricted version bumps — hard gate
    //    (ADR-0005 L1): the new version only commits if a fresh sharable
    //    link could be created on the latest record.
    CreatedAccessLink refreshedLink = null;
    if (recordChanged && latestMetadata.deriveAccessLevel() == AccessLevel.RESTRICTED) {
      try {
        refreshedLink = datasetSource.createAccessLink(
            resolved.externalHandleValue(), config, userId);
      } catch (AccessLinkCreationException e) {
        log.error("Sync of restricted dataset %s failed — cannot refresh the access link on the new version"
            .formatted(datasetId.value()), e);
        return SyncDatasetResponse.failed(datasetId, SyncDatasetError.ACCESS_LINK_REFRESH_FAILED);
      }
    }

    // 8. Build the candidate snapshot with correct runtime fields
    //    (instance id, access link, parent handle — see ADR-0005)
    InvenioRdmResourceMetadata candidate =
        buildCandidate(storedMetadata, latestMetadata, recordChanged, refreshedLink, config);

    // 9. Apply + persist. No domain event is emitted by the aggregate
    //    mutation — the summary event is emitted per trigger (step 10).
    String oldHandle = dataset.externalHandle().value();
    AssociatedDataset.SyncChange change;
    try {
      change = dataset.sync(candidate);
      if (handleChanged) {
        dataset.updateExternalHandle(new ExternalHandle(resolved.externalHandleValue()));
      }
      associatedDatasetRepository.save(dataset);
    } catch (Exception e) {
      log.error("Sync persist failed for dataset %s".formatted(datasetId.value()), e);
      // Integrity rollback: the new access link must not linger unused.
      if (refreshedLink != null && refreshedLink.linkId() != null) {
        revokeAccessLinkBestEffort(refreshedLink.linkId(),
            resolved.externalHandleValue(), config, userId);
      }
      return SyncDatasetResponse.failed(datasetId, SyncDatasetError.SYNC_FAILED);
    }

    // 10. Post-commit: revoke the stale access link of the previous
    //     version (best-effort; failures are logged, not blocking).
    if (refreshedLink != null && storedMetadata instanceof InvenioRdmResourceMetadata invStored
        && invStored.accessLinkId() != null) {
      revokeAccessLinkBestEffort(invStored.accessLinkId(), oldHandle, config, userId);
    }

    if (!change.metadataChanged()) {
      return SyncDatasetResponse.upToDate(datasetId);
    }
    return SyncDatasetResponse.updated(
        datasetId, change.previousVersion(), change.newVersion(),
        change.accessStatusChanged(), dataset.title(), dataset.pid());
  }

  /**
   * Builds the candidate snapshot for a sync, carrying the runtime fields
   * that are <em>connection-local</em> rather than source facts:
   * instance id, access link (identity/id), and parent handle.
   *
   * <ul>
   *   <li>Record unchanged: preserve the stored runtime fields verbatim so
   *       a no-op sync compares equal and is not misreported as updated.</li>
   *   <li>Record changed: adopt the fresh metadata, replace the access link
   *       (restricted → newly created link; public → new record's self
   *       link), and keep the instance id (the dataset stays on its
   *       instance).</li>
   * </ul>
   */
  private InvenioRdmResourceMetadata buildCandidate(
      ResourceMetadata storedMetadata,
      InvenioRdmResourceMetadata latest,
      boolean recordChanged,
      @Nullable CreatedAccessLink refreshedLink,
      InstanceConfig config) {

    boolean restrictedLatest = latest.deriveAccessLevel() == AccessLevel.RESTRICTED;
    String preservedInstanceId = null;
    String preservedLink = null;
    String preservedLinkId = null;
    String preservedParentHandle = null;
    if (storedMetadata instanceof InvenioRdmResourceMetadata invStored) {
      preservedInstanceId = invStored.instanceId();
      preservedLink = invStored.accessLink();
      preservedLinkId = invStored.accessLinkId();
      preservedParentHandle = invStored.parentHandle();
    }

    if (!recordChanged) {
      // Same record — keep the stored runtime fields (preserve equality
      // so a no-op is detected by the aggregate diff).
      String parentHandle = preservedParentHandle != null
          ? preservedParentHandle : latest.parentHandle();
      return new InvenioRdmResourceMetadata(
          latest.title(), latest.pid(), latest.version(), preservedLink,
          latest.resourceProvider(), latest.creators(), latest.resourceType(),
          latest.community(), latest.publicationDate(), latest.description(),
          latest.recordAccess(), latest.fileAccess(),
          preservedInstanceId, preservedLinkId, parentHandle);
    }

    if (restrictedLatest && refreshedLink != null) {
      // New version of a restricted record — fresh access link.
      return new InvenioRdmResourceMetadata(
          latest.title(), latest.pid(), latest.version(), refreshedLink.url(),
          latest.resourceProvider(), latest.creators(), latest.resourceType(),
          latest.community(), latest.publicationDate(), latest.description(),
          latest.recordAccess(), latest.fileAccess(),
          config.id(), refreshedLink.linkId(), latest.parentHandle());
    }

    // New version of a public record (or restricted without a stored link
    // path) — self link of the newest record; no access link id.
    return new InvenioRdmResourceMetadata(
        latest.title(), latest.pid(), latest.version(), latest.accessLink(),
        latest.resourceProvider(), latest.creators(), latest.resourceType(),
        latest.community(), latest.publicationDate(), latest.description(),
        latest.recordAccess(), latest.fileAccess(),
        config.id(), null, latest.parentHandle());
  }

  /**
   * Dispatches one {@link AssociatedDatasetsSyncedEvent} after a sync
   * trigger, only when at least one dataset was actually updated
   * (ADR-0005 N1 — no emails for no-op syncs or failures). A dispatch
   * failure is logged but never fails the sync itself.
   */
  private void emitSyncSummaryEvent(
      ProjectId projectId, String userId, List<SyncDatasetResponse> updated) {
    if (updated.isEmpty()) {
      return;
    }
    List<AssociatedDatasetsSyncedEvent.UpdatedRecord> records = updated.stream()
        .map(response -> new AssociatedDatasetsSyncedEvent.UpdatedRecord(
            response.datasetId(), response.title(), response.pid(),
            response.previousVersion(), response.newVersion(), response.accessStatusChanged()))
        .toList();
    var event = AssociatedDatasetsSyncedEvent.create(projectId, userId, records);
    try {
      DomainEventDispatcher.instance().dispatch(event);
    } catch (Exception e) {
      log.warn("Failed dispatching sync summary event for project %s: %s"
          .formatted(projectId.value(), e.getMessage()));
    }
  }

  /**
   * Resolves the {@link InstanceConfig} a dataset belongs to. Primary
   * source: the instance id persisted on the metadata snapshot. Legacy
   * rows without an instance id fall back to matching the record's
   * access link against the configured instance base URLs.
   */
  private InstanceConfig resolveInstanceConfigForDataset(AssociatedDataset dataset) {
    if (dataset.resourceMetadata() instanceof InvenioRdmResourceMetadata inv
        && inv.instanceId() != null && !inv.instanceId().isBlank()) {
      return resolveInstanceConfig(inv.instanceId());
    }
    // Legacy fallback: best URL match
    String accessLink = dataset.resourceMetadata() instanceof InvenioRdmResourceMetadata inv
        ? inv.accessLink() : null;
    if (accessLink != null && !accessLink.isBlank()) {
      for (SourceInstanceDescriptor descriptor :
          sourceInstanceRegistry.findBySourceType(SourceType.INVENIO_RDM)) {
        if (accessLink.startsWith(descriptor.baseUrl())) {
          return descriptor.toInstanceConfig();
        }
      }
    }
    throw new DatasetSourceNotFoundException();
  }

  /**
   * Best-effort access-link revocation for sync lifecycle (stale link
   * after a version bump, or rollback of a freshly created link after a
   * persistence failure). Never throws — failures are logged so operators
   * can clean up orphaned links manually.
   */
  private void revokeAccessLinkBestEffort(
      String accessLinkId, String externalHandleValue,
      InstanceConfig config, String actingUserId) {
    try {
      datasetSource.revokeAccessLink(accessLinkId, externalHandleValue, config, actingUserId);
      log.info("Revoked access link {} on instance {} (sync lifecycle)"
          .formatted(accessLinkId, config.id()));
    } catch (Exception e) {
      log.error("Failed to revoke access link {} on instance {} during sync; "
              + "the link may need manual clean-up"
              .formatted(accessLinkId, config.id()), e);
    }
  }

  // ── List connected datasets ─────────────────────────────────────────────

  /**
   * Lists all actively connected datasets for a project as display-ready
   * {@link ConnectedDatasetView} DTOs.
   *
   * <p>Each view is enriched with resolved display names: the connecting
   * user's full name (via {@link UserInformationService}) and the linked
   * experiment's display name (via {@link ExperimentInformationService}).
   * Resolution failures fall back to the raw UUID so the view always
   * renders something human-readable.</p>
   *
   * <p>Resolution is performed via a collect-distinct-then-batch-resolve
   * strategy: distinct user IDs and experiment IDs are collected first,
   * resolved exactly once, and then applied to all rows — avoiding N+1
   * queries regardless of dataset count.</p>
   *
   * <p>Requires READ permission on the project (ADR-0003).</p>
   *
   * @param projectId the project to list connected datasets for
   * @return the list of display-ready dataset views (never null, empty
   *         if none); soft-deleted (REMOVED) connections are excluded
   */
  @PreAuthorize(
      "hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ')")
  public List<ConnectedDatasetView> listConnectedDatasetViews(ProjectId projectId) {
    Objects.requireNonNull(projectId, "projectId must not be null");
    if (projectInformationService.find(projectId).isEmpty()) {
      // The project existence check is redundant with the @PreAuthorize
      // ACL guard — if the project has been deleted between ACL creation
      // and this call, no datasets exist anyway. Returning an empty list
      // is both logically correct and avoids leaking internal state via
      // an exception message.
      return List.of();
    }
    List<AssociatedDataset> datasets = associatedDatasetRepository.findByProject(projectId);

    // 1. Collect distinct user IDs and experiment IDs present in this batch
    Set<String> distinctUserIds = datasets.stream()
        .map(AssociatedDataset::connectedBy)
        .collect(Collectors.toSet());
    Set<ExperimentId> distinctExperimentIds = datasets.stream()
        .map(AssociatedDataset::experimentId)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toSet());

    // 2. Resolve each distinct ID exactly once
    Map<String, String> userDisplayNames = resolveUserDisplayNames(distinctUserIds);
    Map<ExperimentId, String> experimentDisplayNames = resolveExperimentDisplayNames(
        projectId, distinctExperimentIds);

    // 3. Build display-ready views
    return datasets.stream()
        .map(ds -> toView(ds, userDisplayNames, experimentDisplayNames))
        .toList();
  }

  /**
   * Resolves a set of distinct user IDs to their full display names via
   * {@link UserInformationService}. Unresolvable IDs are silently omitted
   * from the returned map; callers fall back to the raw UUID.
   */
  private Map<String, String> resolveUserDisplayNames(Set<String> userIds) {
    Map<String, String> result = HashMap.newHashMap(userIds.size());
    for (String userId : userIds) {
      userInformationService.findById(userId)
          .ifPresent(info -> result.put(userId, info.fullName()));
    }
    return result;
  }

  /**
   * Resolves a set of distinct experiment IDs to their display names via
   * {@link ExperimentInformationService}. Unresolvable IDs (e.g. when an
   * experiment has been deleted) are omitted; callers fall back to the
   * raw UUID string.
   */
  private Map<ExperimentId, String> resolveExperimentDisplayNames(
      ProjectId projectId, Set<ExperimentId> experimentIds) {
    Map<ExperimentId, String> result = HashMap.newHashMap(experimentIds.size());
    for (ExperimentId eid : experimentIds) {
      experimentInformationService.find(projectId.value(), eid)
          .ifPresent(exp -> result.put(eid, exp.getName()));
    }
    return result;
  }

  /**
   * Maps a domain aggregate to a {@link ConnectedDatasetView}, extracting
   * source-specific fields (currently only InvenioRDM) into flat top-level
   * fields and substituting resolved display names.
   */
  private ConnectedDatasetView toView(
      AssociatedDataset ds,
      Map<String, String> userDisplayNames,
      Map<ExperimentId, String> experimentDisplayNames) {

    ResourceMetadata metadata = ds.resourceMetadata();

    // Source-type-independent fields
    String displayName = userDisplayNames.getOrDefault(ds.connectedBy(), ds.connectedBy());
    String expDisplay = ds.experimentId()
        .map(eid -> experimentDisplayNames.getOrDefault(eid, eid.value()))
        .orElse(null);

    // Source-specific fields (InvenioRDM-only for now)
    String version;
    String accessLink;
    String resourceProvider;
    List<String> creators;
    String resourceType;
    String community;
    String accessDetail;

    if (metadata instanceof InvenioRdmResourceMetadata inv) {
      version = inv.version();
      accessLink = inv.accessLink();
      resourceProvider = inv.resourceProvider();
      creators = inv.creators();
      resourceType = inv.resourceType();
      community = inv.community();
      accessDetail = ds.accessLevel() == AccessLevel.RESTRICTED
          ? inv.accessDetailDisplay()
          : null;
    } else {
      // Source-agnostic fallback — universal columns only
      version = metadata.version();
      accessLink = null;
      resourceProvider = ds.sourceType().name();
      creators = List.of();
      resourceType = null;
      community = null;
      accessDetail = null;
    }

    return new ConnectedDatasetView(
        ds.id().value(),
        ds.title(),
        ds.pid(),
        ds.accessLevel() == AccessLevel.PUBLIC,
        version,
        accessLink,
        ds.publicationDate(),
        resourceProvider,
        creators,
        resourceType,
        community,
        accessDetail,
        ds.connectedBy(),
        displayName,
        ds.connectedOn(),
        ds.lastSyncedAt().orElse(null),
        ds.experimentId().map(ExperimentId::value).orElse(null),
        expDisplay,
        ds.sourceType().name()
    );
  }

  // ── Available instances ─────────────────────────────────────────────────

  /**
   * Returns the list of configured source instances for the given source
   * type, as descriptors (without credentials). Used by the UI to
   * populate the instance selector combo box.
   *
   * @param sourceType the source system type
   * @return configured instances; never null
   */
  public List<SourceInstanceDescriptor> availableInstances(SourceType sourceType) {
    return sourceInstanceRegistry.findBySourceType(sourceType);
  }

  // ── Internals ───────────────────────────────────────────────────────────

  /**
   * Best-effort, non-blocking access-link revocation used to roll back a
   * created access link when a connect attempt fails after the link has
   * been created. Never fails the caller: revocation failures are logged
   * loudly so operators can clean up the orphaned link manually, but the
   * primary connect error is always surfaced unchanged.
   *
   * <p>No-op when no link was created ({@code null}) or when the source
   * did not expose a link id (see {@link CreatedAccessLink#linkId()}).</p>
   *
   * @param createdAccessLink the created link, or null if none was created
   * @param externalHandleValue the record identifier the link belongs to
   * @param config             the target instance
   * @param actingUserId       the user who performed the connect
   */
  private void revokeAccessLinkIfCreated(
      @Nullable CreatedAccessLink createdAccessLink,
      String externalHandleValue,
      InstanceConfig config,
      String actingUserId) {
    if (createdAccessLink == null || createdAccessLink.linkId() == null) {
      return;
    }
    try {
      datasetSource.revokeAccessLink(createdAccessLink.linkId(),
          externalHandleValue, config, actingUserId);
      log.info("Revoked access link {} on instance {} after failed connect"
          .formatted(createdAccessLink.linkId(), config.id()));
    } catch (Exception e) {
      // Never mask the primary connect failure. Log so operators can
      // revoke the orphaned link manually.
      log.error("Failed to revoke access link {} on instance {} after a "
              + "failed connect; the link may be orphaned and needs manual clean-up"
              .formatted(createdAccessLink.linkId(), config.id()), e);
    }
  }

  /**
   * Best-effort access-link revocation when a dataset connection is
   * removed through {@link #removeDataset}. Never fails the removal: a
   * revocation failure is logged loudly so the link can be cleaned up
   * manually, but the local soft-delete must remain authoritative.
   *
   * <p>Skips datasets with no access link id — public datasets and legacy
   * connections created before the link id was persisted.</p>
   */
  private void revokeAccessLinkIfPresent(AssociatedDataset dataset,
      String actingUserId) {
    ResourceMetadata metadata = dataset.resourceMetadata();
    if (!(metadata instanceof InvenioRdmResourceMetadata inv)
        || inv.accessLinkId() == null || inv.instanceId() == null) {
      return;
    }
    InstanceConfig config;
    try {
      config = resolveInstanceConfig(inv.instanceId());
    } catch (Exception e) {
      log.error("Cannot resolve instance config to revoke access link {} for "
          + "dataset {}".formatted(inv.accessLinkId(), dataset.id()), e);
      return;
    }
    try {
      datasetSource.revokeAccessLink(inv.accessLinkId(),
          dataset.externalHandle().value(), config, actingUserId);
      log.info("Revoked access link {} on instance {} for removed dataset {}"
          .formatted(inv.accessLinkId(), config.id(), dataset.id()));
    } catch (Exception e) {
      log.error("Failed to revoke access link {} for removed dataset {}; "
              + "the link may need manual clean-up"
              .formatted(inv.accessLinkId(), dataset.id()), e);
    }
  }

  private InstanceConfig resolveInstanceConfig(String instanceId) {
    var descriptor = sourceInstanceRegistry.find(instanceId)
        .orElseThrow(() -> {
          log.warn("No source instance configured with id: %s".formatted(instanceId));
          return new DatasetSourceNotFoundException();
        });
    return descriptor.toInstanceConfig();
  }

  /**
   * Subscribes specifically to {@link AssociatedDatasetConnectedEvent}
   * and caches it for later forwarding to the global
   * {@link DomainEventDispatcher} (collect-during, forward-after
   * pattern, same as {@code MeasurementService}).
   *
   * <p>The {@code LocalDomainEventDispatcher} uses <em>exact type
   * matching</em> ({@code ==}) when filtering subscribers, so the
   * subscriber must return the actual event class, not a parent type.</p>
   */
  private record ConnectedEventCollectorSubscriber(
      List<DomainEvent> domainEventsCache
  ) implements DomainEventSubscriber<AssociatedDatasetConnectedEvent> {

    @Override
    public Class<? extends DomainEvent> subscribedToEventType() {
      return AssociatedDatasetConnectedEvent.class;
    }

    @Override
    public void handleEvent(AssociatedDatasetConnectedEvent event) {
      domainEventsCache.add(event);
    }
  }

  /**
   * Subscribes specifically to {@link AssociatedDatasetRemovedEvent}
   * and caches it for later forwarding to the global
   * {@link DomainEventDispatcher} (collect-during, forward-after
   * pattern, same as {@code connectDataset()}).
   *
   * <p>The {@code LocalDomainEventDispatcher} uses <em>exact type
   * matching</em> ({@code ==}) when filtering subscribers, so the
   * subscriber must return the actual event class, not a parent type.</p>
   *
   * @since 1.12.0
   */
  private record RemovedEventCollectorSubscriber(
      List<DomainEvent> domainEventsCache
  ) implements DomainEventSubscriber<AssociatedDatasetRemovedEvent> {

    @Override
    public Class<? extends DomainEvent> subscribedToEventType() {
      return AssociatedDatasetRemovedEvent.class;
    }

    @Override
    public void handleEvent(AssociatedDatasetRemovedEvent event) {
      domainEventsCache.add(event);
    }
  }
}
