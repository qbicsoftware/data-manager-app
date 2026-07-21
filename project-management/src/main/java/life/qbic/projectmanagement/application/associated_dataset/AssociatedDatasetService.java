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
import life.qbic.application.commons.ApplicationException;
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
import life.qbic.projectmanagement.domain.model.associated_dataset.repository.AssociatedDatasetRepository;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
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
   * @throws ApplicationException if the instance is not configured or the
   *         external search fails
   */
  public SearchResult searchDatasets(
      SourceType sourceType,
      String instanceId,
      String query,
      int page,
      int pageSize,
      String searchingUserId) {
    Objects.requireNonNull(sourceType, "sourceType must not be null");
    Objects.requireNonNull(searchingUserId, "searchingUserId must not be null");
    var config = resolveInstanceConfig(instanceId);
    var searchQuery = new SearchQuery(query, page, pageSize);
    try {
      return datasetSource.search(searchQuery, config, searchingUserId);
    } catch (ApplicationException e) {
      log.error("Search failed on instance %s: %s".formatted(instanceId, e.getMessage()));
      throw e;
    } catch (Exception e) {
      log.error("Unexpected error searching instance %s".formatted(instanceId), e);
      throw new ApplicationException("Search failed on instance " + instanceId, e);
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
      Optional<ExperimentId> experimentId,
      String connectedByUserId) {

    Objects.requireNonNull(projectId, "projectId must not be null");
    Objects.requireNonNull(sourceType, "sourceType must not be null");
    Objects.requireNonNull(externalHandleValue, "externalHandleValue must not be null");
    Objects.requireNonNull(connectedByUserId, "connectedByUserId must not be null");
    Objects.requireNonNull(experimentId, "experimentId must not be null (use Optional.empty())");

    InstanceConfig config;
    try {
      config = resolveInstanceConfig(instanceId);
    } catch (ApplicationException e) {
      log.warn("Instance not found: %s".formatted(instanceId));
      return Result.fromError(ConnectDatasetError.INSTANCE_NOT_FOUND);
    }

    // 1. Resolve record metadata from the source system
    Optional<ResourceMetadata> metadata;
    try {
      metadata = datasetSource.resolveMetadata(externalHandleValue, config, connectedByUserId);
    } catch (Exception e) {
      log.error("Failed to resolve metadata for record %s on instance %s"
          .formatted(externalHandleValue, instanceId), e);
      return Result.fromError(ConnectDatasetError.RECORD_NOT_FOUND);
    }
    if (metadata.isEmpty()) {
      log.warn("Record %s not found on instance %s".formatted(externalHandleValue, instanceId));
      return Result.fromError(ConnectDatasetError.RECORD_NOT_FOUND);
    }

    // 1b. Duplicate check — a dataset with the same PID is already
    //     connected to this project. PID is the dedup key because it is
    //     globally unique and persistent by design (DOI/PID).
    if (associatedDatasetRepository.isActiveConnectionPresent(
        projectId, metadata.get().pid())) {
      log.info("Dataset with PID %s is already connected to project %s — skipping"
          .formatted(metadata.get().pid(), projectId));
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
          metadata.get(),
          connectedByUserId,
          experimentId.orElse(null));
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
          + "after dataset connection on project {}; the connection "
          + "itself succeeded, but collaborators may not have been "
          + "notified: {}".formatted(projectId, e.getMessage()));
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
      Optional<ExperimentId> experimentId,
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
        .onErrorResume(Throwable.class, t ->
            Mono.just(new ConnectDatasetResponse(
                request.requestId(), null, ConnectDatasetError.CONNECT_FAILED)));
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
      throw new ApplicationException("Project not found: %s".formatted(projectId));
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
    Map<String, String> result = new HashMap<>(userIds.size());
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
    Map<ExperimentId, String> result = new HashMap<>(experimentIds.size());
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
        ds.accessLevel(),
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
        .orElseThrow(() -> new ApplicationException(
            "No source instance configured with id: " + instanceId));
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
}
