package life.qbic.projectmanagement.application.associated_dataset

import java.time.LocalDate
import java.util.concurrent.CopyOnWriteArrayList
import life.qbic.domain.concepts.DomainEvent
import life.qbic.domain.concepts.DomainEventDispatcher
import life.qbic.domain.concepts.DomainEventSubscriber
import life.qbic.domain.concepts.LocalDomainEventDispatcher
import life.qbic.identity.api.UserInformationService
import life.qbic.projectmanagement.application.ProjectInformationService
import life.qbic.projectmanagement.application.api.ProjectOverviewLookup
import life.qbic.projectmanagement.application.experiment.ExperimentInformationService
import life.qbic.projectmanagement.domain.model.associated_dataset.AccessLevel
import life.qbic.projectmanagement.domain.model.associated_dataset.AssociatedDataset
import life.qbic.projectmanagement.domain.model.associated_dataset.ExternalHandle
import life.qbic.projectmanagement.domain.model.associated_dataset.InvenioRdmAccessStatus
import life.qbic.projectmanagement.domain.model.associated_dataset.InvenioRdmResourceMetadata
import life.qbic.projectmanagement.domain.model.associated_dataset.SourceType
import life.qbic.projectmanagement.domain.model.associated_dataset.event.AssociatedDatasetsSyncedEvent
import life.qbic.projectmanagement.domain.model.associated_dataset.repository.AssociatedDatasetRepository
import life.qbic.projectmanagement.domain.model.project.ProjectId
import spock.lang.Specification

/**
 * Unit tests for {@link AssociatedDatasetService#syncDatasets} —
 * dataset synchronisation (DATSET-04/08, ADR-0005).
 *
 * <p>Stubs are hand-rolled Groovy interface coercions (no Spock
 * {@code Mock()}) so the spec runs without the Mockito mock maker.</p>
 *
 * @since 1.13.0
 */
class AssociatedDatasetServiceSyncSpec extends Specification {

  private static final String PROJECT_UUID = "0270ce7f-4092-40e3-9c4c-ce7adb688bf5"
  private static final ProjectId PROJECT = ProjectId.parse(PROJECT_UUID)
  private static final String USER = "user-1"
  private static final String CONNECTOR = "connector-1"
  private static final InstanceConfig CONFIG =
      new InstanceConfig("zenodo", "Zenodo", "https://zenodo.org")

  private static final List<AssociatedDatasetsSyncedEvent> SYNCED_EVENTS = new CopyOnWriteArrayList<>()

  static {
    DomainEventDispatcher.instance().subscribe(
        new DomainEventSubscriber<AssociatedDatasetsSyncedEvent>() {
          @Override
          Class<? extends DomainEvent> subscribedToEventType() {
            return AssociatedDatasetsSyncedEvent
          }

          @Override
          void handleEvent(AssociatedDatasetsSyncedEvent event) {
            SYNCED_EVENTS.add(event)
          }
        })
  }

  def setup() {
    LocalDomainEventDispatcher.instance().reset()
    SYNCED_EVENTS.clear()
  }

  // ── helpers ──────────────────────────────────────────────────────────

  private static InvenioRdmResourceMetadata publicV1() {
    new InvenioRdmResourceMetadata(
        "Public Dataset", "10.5281/zenodo.1", "v1",
        "https://zenodo.org/records/111", "Zenodo", [], "Dataset", null,
        LocalDate.of(2025, 1, 1), null,
        InvenioRdmAccessStatus.PUBLIC, InvenioRdmAccessStatus.PUBLIC,
        "zenodo", null, "parent-1")
  }

  private static InvenioRdmResourceMetadata publicV2() {
    new InvenioRdmResourceMetadata(
        "Public Dataset", "10.5281/zenodo.2", "v2",
        "https://zenodo.org/records/222", "Zenodo", [], "Dataset", null,
        LocalDate.of(2025, 2, 1), null,
        InvenioRdmAccessStatus.PUBLIC, InvenioRdmAccessStatus.PUBLIC,
        null, null, "parent-1")
  }

  private static InvenioRdmResourceMetadata restrictedV1() {
    new InvenioRdmResourceMetadata(
        "Restricted Dataset", "10.5281/zenodo.11", "v1",
        "https://zenodo.org/records/111?token=old", "Zenodo", [], "Dataset", null,
        LocalDate.of(2025, 1, 1), null,
        InvenioRdmAccessStatus.RESTRICTED, InvenioRdmAccessStatus.RESTRICTED,
        "zenodo", "link-old", "parent-1")
  }

  private static InvenioRdmResourceMetadata restrictedV2() {
    new InvenioRdmResourceMetadata(
        "Restricted Dataset", "10.5281/zenodo.12", "v2",
        "https://zenodo.org/records/222", "Zenodo", [], "Dataset", null,
        LocalDate.of(2025, 2, 1), null,
        InvenioRdmAccessStatus.RESTRICTED, InvenioRdmAccessStatus.RESTRICTED,
        null, null, "parent-1")
  }

  private static AssociatedDataset connected(
      InvenioRdmResourceMetadata metadata, String handle = "111") {
    AssociatedDataset.connect(
        PROJECT, SourceType.INVENIO_RDM,
        new ExternalHandle(handle), metadata, CONNECTOR, null)
  }

  private AssociatedDatasetService createService(
      DatasetSource source,
      AssociatedDatasetRepository repository,
      SourceInstanceRegistry registry = [
          find: { id -> Optional.of(new SourceInstanceDescriptor("zenodo", "Zenodo",
              "https://zenodo.org", SourceType.INVENIO_RDM)) },
          findBySourceType: { st -> [new SourceInstanceDescriptor("zenodo", "Zenodo",
              "https://zenodo.org", SourceType.INVENIO_RDM)] }
      ] as SourceInstanceRegistry) {
    // The sync path never queries these collaborators; they only need to
    // satisfy the (NonNull) constructor. Map coercion cannot stub concrete
    // classes, so stub the interface collaborators and pass nulls for the
    // remaining constructor parameters.
    def overviewLookup = [find: { p, f -> [] }] as ProjectOverviewLookup
    def projectInfo = new ProjectInformationService(overviewLookup, null, null, null)
    def userInfo = [findById: { id -> Optional.empty() }] as UserInformationService
    def experimentInfo = new ExperimentInformationService(null, null, null)
    new AssociatedDatasetService(source, repository, registry, projectInfo, userInfo,
        experimentInfo)
  }

  private static SyncDatasetResponse runSync(AssociatedDatasetService service,
      AssociatedDataset dataset) {
    service.syncDatasets(PROJECT, [dataset.id()], USER).blockLast()
  }

  // ── scenarios ─────────────────────────────────────────────────────────

  def "public dataset is updated when the source has a new version"() {
    given:
    def dataset = connected(publicV1())
    def resolved = new ResolvedRecord(publicV2(), "222")
    def source = [
        resolveLatest: { handle, cfg, user -> Optional.of(resolved) },
        hasValidCredential: { u, cfg -> false }
    ] as DatasetSource
    AssociatedDataset saved
    def repository = [
        findById: { id -> Optional.of(dataset) },
        save: { ds -> saved = ds },
        isActiveConnectionPresent: { p, pid -> false }
    ] as AssociatedDatasetRepository
    def service = createService(source, repository)

    when:
    def response = runSync(service, dataset)

    then:
    response.status() == SyncDatasetResponse.SyncStatus.UPDATED
    response.newVersion() == "v2"
    response.title() == "Public Dataset"
    response.pid() == "10.5281/zenodo.2"
    saved != null
    saved.externalHandle().value() == "222"
    saved.version() == "v2"
    SYNCED_EVENTS.size() == 1
    SYNCED_EVENTS[0].updatedRecords().size() == 1
    SYNCED_EVENTS[0].updatedRecords()[0].previousVersion() == "v1"
    SYNCED_EVENTS[0].updatedRecords()[0].newVersion() == "v2"
  }

  def "no-op sync reports UP_TO_DATE and emits no notification event"() {
    given:
    def dataset = connected(publicV1())
    // resolveLatest returns the same record (instance/link fields left null
    // by the adapter — the service preserves the stored runtime fields).
    def resolved = new ResolvedRecord(new InvenioRdmResourceMetadata(
        "Public Dataset", "10.5281/zenodo.1", "v1",
        "https://zenodo.org/records/111", "Zenodo", [], "Dataset", null,
        LocalDate.of(2025, 1, 1), null,
        InvenioRdmAccessStatus.PUBLIC, InvenioRdmAccessStatus.PUBLIC,
        null, null, "parent-1"), "111")
    def source = [
        resolveLatest: { handle, cfg, user -> Optional.of(resolved) },
        hasValidCredential: { u, cfg -> false }
    ] as DatasetSource
    AssociatedDataset saved
    def repository = [
        findById: { id -> Optional.of(dataset) },
        save: { ds -> saved = ds },
        isActiveConnectionPresent: { p, pid -> false }
    ] as AssociatedDatasetRepository
    def service = createService(source, repository)

    when:
    def response = runSync(service, dataset)

    then:
    response.status() == SyncDatasetResponse.SyncStatus.UP_TO_DATE
    saved != null
    SYNCED_EVENTS.isEmpty()
  }

  def "first sync of a legacy connection (no parent handle stored) is still UP_TO_DATE"() {
    given: "a legacy public connection without parentHandle or instanceId"
    def legacy = new InvenioRdmResourceMetadata(
        "Public Dataset", "10.5281/zenodo.1", "v1",
        "https://zenodo.org/records/111", "Zenodo", [], "Dataset", null,
        LocalDate.of(2025, 1, 1), null,
        InvenioRdmAccessStatus.PUBLIC, InvenioRdmAccessStatus.PUBLIC,
        null, null, null)
    def dataset = connected(legacy)
    and: "the source now reports the same record but exposes the concept recid"
    def resolved = new ResolvedRecord(new InvenioRdmResourceMetadata(
        "Public Dataset", "10.5281/zenodo.1", "v1",
        "https://zenodo.org/records/111", "Zenodo", [], "Dataset", null,
        LocalDate.of(2025, 1, 1), null,
        InvenioRdmAccessStatus.PUBLIC, InvenioRdmAccessStatus.PUBLIC,
        null, null, "parent-1"), "111")
    def source = [
        resolveLatest: { handle, cfg, user -> Optional.of(resolved) },
        hasValidCredential: { u, cfg -> false }
    ] as DatasetSource
    AssociatedDataset saved
    def repository = [
        findById: { id -> Optional.of(dataset) },
        save: { ds -> saved = ds },
        isActiveConnectionPresent: { p, pid -> false }
    ] as AssociatedDatasetRepository
    def service = createService(source, repository)

    when:
    def response = runSync(service, dataset)

    then:
    response.status() == SyncDatasetResponse.SyncStatus.UP_TO_DATE
    saved != null
    (saved.resourceMetadata() as InvenioRdmResourceMetadata).parentHandle() == null
    SYNCED_EVENTS.isEmpty()
  }

  def "restricted dataset without a credential is short-circuited with CREDENTIAL_REQUIRED"() {
    given:
    def dataset = connected(restrictedV1())
    def resolveCalls = 0
    def source = [
        resolveLatest: { handle, cfg, user -> resolveCalls++; Optional.empty() },
        hasValidCredential: { u, cfg -> false }
    ] as DatasetSource
    def repository = [
        findById: { id -> Optional.of(dataset) },
        save: { ds -> },
        isActiveConnectionPresent: { p, pid -> false }
    ] as AssociatedDatasetRepository
    def service = createService(source, repository)

    when:
    def response = runSync(service, dataset)

    then:
    response.status() == SyncDatasetResponse.SyncStatus.FAILED
    response.error() == SyncDatasetError.CREDENTIAL_REQUIRED
    resolveCalls == 0
    SYNCED_EVENTS.isEmpty()
  }

  def "restricted dataset with insufficient rights resolves to CREDENTIAL_INSUFFICIENT"() {
    given:
    def dataset = connected(restrictedV1())
    def source = [
        resolveLatest: { handle, cfg, user ->
          throw new DatasetAccessDeniedException("denied")
        },
        hasValidCredential: { u, cfg -> true }
    ] as DatasetSource
    def repository = [
        findById: { id -> Optional.of(dataset) },
        save: { ds -> },
        isActiveConnectionPresent: { p, pid -> false }
    ] as AssociatedDatasetRepository
    def service = createService(source, repository)

    when:
    def response = runSync(service, dataset)

    then:
    response.status() == SyncDatasetResponse.SyncStatus.FAILED
    response.error() == SyncDatasetError.CREDENTIAL_INSUFFICIENT
    SYNCED_EVENTS.isEmpty()
  }

  def "record deleted on the source resolves to RECORD_NOT_FOUND and the connection is kept"() {
    given:
    def dataset = connected(publicV1())
    def source = [
        resolveLatest: { handle, cfg, user -> Optional.empty() },
        hasValidCredential: { u, cfg -> false }
    ] as DatasetSource
    AssociatedDataset saved
    def repository = [
        findById: { id -> Optional.of(dataset) },
        save: { ds -> saved = ds },
        isActiveConnectionPresent: { p, pid -> false }
    ] as AssociatedDatasetRepository
    def service = createService(source, repository)

    when:
    def response = runSync(service, dataset)

    then:
    response.status() == SyncDatasetResponse.SyncStatus.FAILED
    response.error() == SyncDatasetError.RECORD_NOT_FOUND
    saved == null
    dataset.isConnected()
    SYNCED_EVENTS.isEmpty()
  }

  def "restricted version bump refreshes the access link, updates the snapshot and revokes the stale link"() {
    given:
    def dataset = connected(restrictedV1())
    def resolved = new ResolvedRecord(restrictedV2(), "222")
    def revoked = new ArrayList<String>()
    def source = [
        resolveLatest: { handle, cfg, user -> Optional.of(resolved) },
        hasValidCredential: { u, cfg -> true },
        createAccessLink: { handle, cfg, user ->
          new CreatedAccessLink("https://zenodo.org/records/222?token=new", "link-new")
        },
        revokeAccessLink: { linkId, handle, cfg, user -> revoked.add(linkId) }
    ] as DatasetSource
    AssociatedDataset saved
    def repository = [
        findById: { id -> Optional.of(dataset) },
        save: { ds -> saved = ds },
        isActiveConnectionPresent: { p, pid -> false }
    ] as AssociatedDatasetRepository
    def service = createService(source, repository)

    when:
    def response = runSync(service, dataset)

    then:
    response.status() == SyncDatasetResponse.SyncStatus.UPDATED
    response.newVersion() == "v2"
    saved != null
    saved.externalHandle().value() == "222"
    (saved.resourceMetadata() as InvenioRdmResourceMetadata).accessLinkId() == "link-new"
    (saved.resourceMetadata() as InvenioRdmResourceMetadata).parentHandle() == "parent-1"
    revoked == ["link-old"]
    SYNCED_EVENTS.size() == 1
  }

  def "restricted version bump fails atomically when the access link cannot be created"() {
    given:
    def dataset = connected(restrictedV1())
    def resolved = new ResolvedRecord(restrictedV2(), "222")
    def source = [
        resolveLatest: { handle, cfg, user -> Optional.of(resolved) },
        hasValidCredential: { u, cfg -> true },
        createAccessLink: { handle, cfg, user ->
          throw new AccessLinkCreationException("not the owner")
        }
    ] as DatasetSource
    AssociatedDataset saved
    def repository = [
        findById: { id -> Optional.of(dataset) },
        save: { ds -> saved = ds },
        isActiveConnectionPresent: { p, pid -> false }
    ] as AssociatedDatasetRepository
    def service = createService(source, repository)

    when:
    def response = runSync(service, dataset)

    then:
    response.status() == SyncDatasetResponse.SyncStatus.FAILED
    response.error() == SyncDatasetError.ACCESS_LINK_REFRESH_FAILED
    saved == null
    dataset.version() == "v1"
    SYNCED_EVENTS.isEmpty()
  }

  def "persistence failure after link creation rolls the new access link back"() {
    given:
    def dataset = connected(restrictedV1())
    def resolved = new ResolvedRecord(restrictedV2(), "222")
    def revoked = new ArrayList<String>()
    def source = [
        resolveLatest: { handle, cfg, user -> Optional.of(resolved) },
        hasValidCredential: { u, cfg -> true },
        createAccessLink: { handle, cfg, user ->
          new CreatedAccessLink("https://zenodo.org/records/222?token=new", "link-rollback")
        },
        revokeAccessLink: { linkId, handle, cfg, user -> revoked.add(linkId) }
    ] as DatasetSource
    def repository = [
        findById: { id -> Optional.of(dataset) },
        save: { ds -> throw new RuntimeException("DB down") },
        isActiveConnectionPresent: { p, pid -> false }
    ] as AssociatedDatasetRepository
    def service = createService(source, repository)

    when:
    def response = runSync(service, dataset)

    then:
    response.status() == SyncDatasetResponse.SyncStatus.FAILED
    response.error() == SyncDatasetError.SYNC_FAILED
    revoked == ["link-rollback"]
    SYNCED_EVENTS.isEmpty()
  }

  def "sync is skipped when the target version is already connected (duplicate guard)"() {
    given:
    def dataset = connected(publicV1())
    def resolved = new ResolvedRecord(publicV2(), "222")
    def source = [
        resolveLatest: { handle, cfg, user -> Optional.of(resolved) },
        hasValidCredential: { u, cfg -> false }
    ] as DatasetSource
    AssociatedDataset saved
    def repository = [
        findById: { id -> Optional.of(dataset) },
        save: { ds -> saved = ds },
        isActiveConnectionPresent: { p, pid -> true }
    ] as AssociatedDatasetRepository
    def service = createService(source, repository)

    when:
    def response = runSync(service, dataset)

    then:
    response.status() == SyncDatasetResponse.SyncStatus.FAILED
    response.error() == SyncDatasetError.ALREADY_CONNECTED
    saved == null
    dataset.version() == "v1"
    SYNCED_EVENTS.isEmpty()
  }

  def "a trigger with one update and one no-op emits a single summary event"() {
    given:
    def updatedDataset = connected(publicV1(), "111")
    def unchangedDataset = connected(publicV1(), "121")
    def ids = [updatedDataset.id(), unchangedDataset.id()]
    def resolvedUpdate = new ResolvedRecord(publicV2(), "222")
    def resolvedNoop = new ResolvedRecord(new InvenioRdmResourceMetadata(
        "Public Dataset", "10.5281/zenodo.1", "v1",
        "https://zenodo.org/records/121", "Zenodo", [], "Dataset", null,
        LocalDate.of(2025, 1, 1), null,
        InvenioRdmAccessStatus.PUBLIC, InvenioRdmAccessStatus.PUBLIC,
        null, null, "parent-1"), "121")
    def source = [
        resolveLatest: { handle, cfg, user ->
          handle == "111" ? Optional.of(resolvedUpdate) : Optional.of(resolvedNoop)
        },
        hasValidCredential: { u, cfg -> false }
    ] as DatasetSource
    AssociatedDataset saved
    def repository = [
        findById: { id -> Optional.of(id == ids[0] ? updatedDataset : unchangedDataset) },
        save: { ds -> saved = ds },
        isActiveConnectionPresent: { p, pid -> false }
    ] as AssociatedDatasetRepository
    def service = createService(source, repository)

    when:
    def responses = service.syncDatasets(
        PROJECT, [updatedDataset.id(), unchangedDataset.id()], USER).collectList().block()

    then:
    responses.size() == 2
    responses.count { it.status() == SyncDatasetResponse.SyncStatus.UPDATED } == 1
    responses.count { it.status() == SyncDatasetResponse.SyncStatus.UP_TO_DATE } == 1
    SYNCED_EVENTS.size() == 1
    SYNCED_EVENTS[0].updatedRecords().size() == 1
  }
}