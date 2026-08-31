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
import life.qbic.projectmanagement.domain.model.associated_dataset.repository.AssociatedDatasetRepository;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.springframework.lang.Nullable;
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
   * specific access status on the source system. For InvenioRDM,
   * {@code "restricted"} returns only access-restricted records;
   * {@code "open"} returns only publicly accessible records;
   * {@code null} returns all records (no filter).</p>
   *
   * @param sourceType      the source system type
   * @param instanceId      the configured instance identifier
   * @param query           the free-text search term; blank for "list all"
   * @param accessFilter    optional access-status filter (e.g. "restricted");
   *                        {@code null} for no filter
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
      String accessFilter,
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

    // 1a. Credential gate — access-restricted datasets require a valid
    //     credential on the source instance. Without a valid PAT, the
    //     access link creation needed for project collaborators will
    //     fail. Hard-block the connect rather than silently succeeding
    //     with an unusable connection.
    ResourceMetadata finalMetadata = metadata.get();
    if (finalMetadata instanceof InvenioRdmResourceMetadata inv
        && inv.deriveAccessLevel() == AccessLevel.RESTRICTED) {
      if (!datasetSource.hasValidCredential(connectedByUserId, config)) {
        log.warn("Cannot connect restricted dataset %s — no valid credential "
            + "for user %s on instance %s"
            .formatted(externalHandleValue, connectedByUserId, instanceId));
        return Result.fromError(ConnectDatasetError.CREDENTIAL_REQUIRED);
      }

      // Create sharable access link for project collaborators
      try {
        String accessLinkUrl = datasetSource.createAccessLink(
            externalHandleValue, config, connectedByUserId);
        // Update metadata with the access link
        finalMetadata = new InvenioRdmResourceMetadata(
            inv.title(), inv.pid(), inv.version(), accessLinkUrl,
            inv.resourceProvider(), inv.creators(), inv.resourceType(),
            inv.community(), inv.publicationDate(), inv.description(),
            inv.recordAccess(), inv.fileAccess());
      } catch (AccessLinkCreationException e) {
        log.error("Failed to create access link for restricted dataset %s on instance %s"
            .formatted(externalHandleValue, instanceId), e);
        return Result.fromError(ConnectDatasetError.ACCESS_LINK_CREATION_FAILED);
      }
    }

    // 1b. Duplicate check — a dataset with the same PID is already
    //     connected to this project. PID is the dedup key because it is
    //     globally unique and persistent by design (DOI/PID).
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
      return Result.fromError(ConnectDatasetError.CONNECT_FAILED);
    }

    // 4. Persist
    try {
      associatedDatasetRepository.save(dataset);
    } catch (Exception e) {
      log.error("Failed to persist associated dataset for project %s".formatted(projectId), e);
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
