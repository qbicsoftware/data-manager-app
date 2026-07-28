package life.qbic.projectmanagement.domain.model.associated_dataset;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import life.qbic.domain.concepts.LocalDomainEventDispatcher;
import life.qbic.projectmanagement.domain.model.associated_dataset.event.AssociatedDatasetConnectedEvent;
import life.qbic.projectmanagement.domain.model.associated_dataset.event.AssociatedDatasetRemovedEvent;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.project.ProjectId;

/**
 * Aggregate root for an associated dataset connection.
 *
 * <p>An associated dataset links an external record (hosted on Zenodo,
 * FDAT, or a future source) to a Data Manager project. The aggregate
 * carries the connection lifecycle state, the external identity of the
 * record, and a snapshot of its metadata at connect-time.</p>
 *
 * <p>Per ADR-0001, the aggregate is source-agnostic at its API boundary.
 * Source-specific metadata is encapsulated behind the sealed
 * {@link ResourceMetadata} hierarchy, stored as an opaque JSON blob
 * (see field {@code resourceMetadata}). Only the four "universal
 * columns" (title, pid, version, publicationDate) are extracted to
 * regular SQL columns for sort/filter — these are defined on the
 * {@code ResourceMetadata} interface and expected to exist for any
 * external source.</p>
 *
 * <p>Per ADR-0001, removal uses soft-delete: {@link #remove()} transitions
 * the state to {@link ConnectionState#REMOVED} (tombstone retained for
 * audit). The aggregate does not physically delete itself.</p>
 *
 * @since 1.12.0
 */
@Entity(name = "associated_dataset")
public class AssociatedDataset {

  // ── Identity ────────────────────────────────────────────────────────────

  @EmbeddedId
  @AttributeOverride(name = "uuid", column = @Column(name = "id"))
  private AssociatedDatasetId id;

  @Embedded
  @AttributeOverride(name = "projectId", column = @Column(name = "project_id"))
  private ProjectId projectId;

  @Convert(converter = SourceTypeConverter.class)
  @Column(name = "source_type", nullable = false)
  private SourceType sourceType;

  @Column(name = "external_handle", nullable = false)
  private String externalHandle;

  // ── Lifecycle ───────────────────────────────────────────────────────────

  @Convert(converter = ConnectionStateConverter.class)
  @Column(name = "connection_state", nullable = false)
  private ConnectionState connectionState;

  /**
   * Coarse access level derived from the source-specific
   * {@link ResourceMetadata} subtype. For InvenioRDM, derived via
   * {@link InvenioRdmResourceMetadata#deriveAccessLevel()}.
   * Source-agnostic: any source type yields PUBLIC or RESTRICTED.
   */
  @Convert(converter = AccessLevelConverter.class)
  @Column(name = "access_level", nullable = false)
  private AccessLevel accessLevel;

  @Column(name = "connected_by", nullable = false)
  private String connectedBy;

  @Column(name = "connected_on", nullable = false)
  private Instant connectedOn;

  @Column(name = "last_synced_at")
  private Instant lastSyncedAt;

  @Embedded
  @AttributeOverride(name = "uuid", column = @Column(name = "experiment_id"))
  private ExperimentId experimentId;

  // ── Universal columns (from ResourceMetadata interface) ─────────────────
  //
  // These four fields are duplicated from resourceMetadata into regular SQL
  // columns so they can be sorted/filtered efficiently. The source-specific
  // detail remains accessible via the resourceMetadata() accessor.

  @Column(name = "title", length = 1024)
  private String title;

  @Column(name = "pid")
  private String pid;

  @Column(name = "version")
  private String version;

  @Column(name = "publication_date")
  private LocalDate publicationDate;

  // ── Source-specific metadata (opaque JSON) ──────────────────────────────

  /**
   * The full {@link ResourceMetadata} snapshot, serialized as a
   * MariaDB JSON blob. Per ADR-0001, source-specific fields (creators,
   * community, recordAccess, etc.) live exclusively here.
   *
   * <p>The entity exposes this object via {@link #resourceMetadata()}
   * so the application-service layer can extract source-specific
   * details when building DTOs — without storing them as first-class
   * columns on the aggregate.</p>
   */
  @Convert(converter = ResourceMetadataConverter.class)
  @Column(name = "resource_metadata", columnDefinition = "json")
  private ResourceMetadata resourceMetadata;

  // ── Constructor ─────────────────────────────────────────────────────────

  protected AssociatedDataset() {
    // needed by JPA
  }

  private AssociatedDataset(
      AssociatedDatasetId id,
      ProjectId projectId,
      SourceType sourceType,
      ExternalHandle externalHandle,
      ResourceMetadata metadata,
      String connectedBy,
      ExperimentId experimentId) {
    this.id = Objects.requireNonNull(id, "id must not be null");
    this.projectId = Objects.requireNonNull(projectId, "projectId must not be null");
    this.sourceType = Objects.requireNonNull(sourceType, "sourceType must not be null");
    Objects.requireNonNull(externalHandle, "externalHandle must not be null");
    this.externalHandle = externalHandle.value();
    this.connectionState = ConnectionState.CONNECTED;
    Objects.requireNonNull(metadata, "metadata must not be null");
    Objects.requireNonNull(connectedBy, "connectedBy must not be null");
    applyMetadata(metadata);
    this.connectedBy = connectedBy;
    this.connectedOn = Instant.now();
    this.experimentId = experimentId; // nullable — optional during connect
  }

  /**
   * Factory method: connects a dataset to a project and emits the
   * {@link AssociatedDatasetConnectedEvent}.
   *
   * <p>The {@link AccessLevel} of the connection is derived from the
   * provided {@link ResourceMetadata}. For InvenioRDM resources, the
   * coarse access level is PUBLIC only when both record and file access
   * are {@link InvenioRdmAccessStatus#PUBLIC}; otherwise RESTRICTED (see
   * {@link InvenioRdmResourceMetadata#deriveAccessLevel()}).</p>
   */
  public static AssociatedDataset connect(
      ProjectId projectId,
      SourceType sourceType,
      ExternalHandle externalHandle,
      ResourceMetadata metadata,
      String connectedBy,
      ExperimentId experimentId) {
    var dataset = new AssociatedDataset(
        AssociatedDatasetId.create(),
        projectId,
        sourceType,
        externalHandle,
        metadata,
        connectedBy,
        experimentId);
    dataset.emitConnectedEvent();
    return dataset;
  }

  // ── Lifecycle operations ────────────────────────────────────────────────

  /**
   * Removes the connection (soft-delete). Transitions the aggregate to
   * {@link ConnectionState#REMOVED} state and dispatches an
   * {@link AssociatedDatasetRemovedEvent} via the local domain event
   * dispatcher so the application-service layer can forward it to
   * collaborator-notification policy directives.
   *
   * <p>After this call the aggregate should not be surfaced through
   * active queries — the repository excludes REMOVED rows by default
   * (ADR-0001). The row is retained as an audit tombstone.</p>
   *
   * @param removedByUserId the user who performed the removal;
   *                       recorded in the emitted event as the actor
   *                       (may differ from {@link #connectedBy()})
   * @throws IllegalStateException  if the dataset is already removed
   * @throws NullPointerException   if {@code removedByUserId} is null
   */
  public void remove(String removedByUserId) {
    Objects.requireNonNull(removedByUserId, "removedByUserId must not be null");
    if (this.connectionState == ConnectionState.REMOVED) {
      throw new IllegalStateException("Dataset connection is already removed");
    }
    this.connectionState = ConnectionState.REMOVED;
    emitRemovedEvent(removedByUserId);
  }

  /**
   * Returns whether the dataset is in active (connected) state.
   */
  public boolean isConnected() {
    return this.connectionState == ConnectionState.CONNECTED;
  }

  // ── Metadata sync ───────────────────────────────────────────────────────

  /**
   * Replaces the metadata snapshot with the latest state from the source
   * system. Updates the universal columns (title, pid, version,
   * publicationDate) and re-derives the coarse {@link AccessLevel}.
   *
   * @since 1.12.0
   */
  public void updateMetadata(ResourceMetadata metadata) {
    Objects.requireNonNull(metadata, "metadata must not be null");
    applyMetadata(metadata);
    this.lastSyncedAt = Instant.now();
  }

  // ── Accessors ───────────────────────────────────────────────────────────

  public AssociatedDatasetId id() {
    return id;
  }

  public ProjectId projectId() {
    return projectId;
  }

  public SourceType sourceType() {
    return sourceType;
  }

  public ExternalHandle externalHandle() {
    return new ExternalHandle(this.externalHandle);
  }

  public ConnectionState connectionState() {
    return connectionState;
  }

  public AccessLevel accessLevel() {
    return accessLevel;
  }

  /**
   * High-priority universal column: human-readable title, extracted
   * from {@link #resourceMetadata()} for efficient SQL sort/filter
   * (ADR-0001).
   */
  public String title() {
    return title;
  }

  /**
   * High-priority universal column: persistent identifier (PID / DOI),
   * extracted from {@link #resourceMetadata()} for efficient SQL
   * sort/filter (ADR-0001).
   */
  public String pid() {
    return pid;
  }

  public String connectedBy() {
    return connectedBy;
  }

  public Instant connectedOn() {
    return connectedOn;
  }

  public Optional<Instant> lastSyncedAt() {
    return Optional.ofNullable(lastSyncedAt);
  }

  /**
   * High-priority universal column: publication date on the source,
   * extracted from {@link #resourceMetadata()} for efficient SQL
   * sort/filter (ADR-0001).
   */
  public java.time.LocalDate publicationDate() {
    return publicationDate;
  }

  public Optional<ExperimentId> experimentId() {
    return Optional.ofNullable(experimentId);
  }

  /**
   * The full source-specific metadata snapshot. Application/infrastructure
   * layers can inspect this (e.g. {@code instanceof InvenioRdmResourceMetadata})
   * to build DTOs that need source-specific fields. Source-specific fields
   * are <em>not</em> available as top-level accessors on the aggregate —
   * use this object to reach them.
   */
  public ResourceMetadata resourceMetadata() {
    return resourceMetadata;
  }

  // ── Internals ───────────────────────────────────────────────────────────

  /**
   * Stores the full metadata snapshot and re-derives the universal
   * columns (title, pid, version, publicationDate) plus the coarse
   * {@link AccessLevel}. Source-specific derivation (e.g. for InvenioRDM)
   * happens via a sealed-subtype check on {@code metadata} — this is
   * the natural Java pattern for sealed hierarchies and keeps the
   * aggregate's public API source-agnostic.
   */
  private void applyMetadata(ResourceMetadata metadata) {
    // Universal columns — extracted from the interface
    this.title = metadata.title();
    this.pid = metadata.pid();
    this.version = metadata.version();
    this.publicationDate = metadata.publicationDate();

    // Full metadata snapshot + source-specific access-level derivation
    this.resourceMetadata = metadata;
    this.accessLevel = deriveAccessLevel(metadata);
  }

  private AccessLevel deriveAccessLevel(ResourceMetadata metadata) {
    if (metadata instanceof InvenioRdmResourceMetadata inv) {
      return inv.deriveAccessLevel();
    }
    throw new IllegalStateException(
        "Unsupported ResourceMetadata subtype: " + metadata.getClass().getName());
  }

  private void emitConnectedEvent() {
    var event = AssociatedDatasetConnectedEvent.create(
        this.id, this.projectId, this.connectedBy, this.title, this.pid);
    LocalDomainEventDispatcher.instance().dispatch(event);
  }

  /**
   * Dispatches an {@link AssociatedDatasetRemovedEvent} via the
   * {@link LocalDomainEventDispatcher}. The application-service layer
   * subscribes to these events (collect-during, forward-after pattern)
   * and forwards them to the global dispatcher, which drives the
   * collaborator-notification policy directives.
   *
   * <p>The event's actor is the user who performed the removal
   * ({@code removedByUserId}), which may differ from the user who
   * originally connected the dataset ({@code connectedBy}).</p>
   */
  private void emitRemovedEvent(String removedByUserId) {
    var event = AssociatedDatasetRemovedEvent.create(
        this.id, this.projectId, removedByUserId, this.title, this.pid);
    LocalDomainEventDispatcher.instance().dispatch(event);
  }

  // ── JPA AttributeConverters ─────────────────────────────────────────────

  static class SourceTypeConverter implements AttributeConverter<SourceType, String> {
    @Override
    public String convertToDatabaseColumn(SourceType attribute) {
      return attribute == null ? null : attribute.name();
    }

    @Override
    public SourceType convertToEntityAttribute(String dbData) {
      return dbData == null ? null : SourceType.valueOf(dbData);
    }
  }

  static class ConnectionStateConverter implements AttributeConverter<ConnectionState, String> {
    @Override
    public String convertToDatabaseColumn(ConnectionState attribute) {
      return attribute == null ? null : attribute.name();
    }

    @Override
    public ConnectionState convertToEntityAttribute(String dbData) {
      return dbData == null ? null : ConnectionState.valueOf(dbData);
    }
  }

  static class AccessLevelConverter implements AttributeConverter<AccessLevel, String> {
    @Override
    public String convertToDatabaseColumn(AccessLevel attribute) {
      return attribute == null ? null : attribute.name();
    }

    @Override
    public AccessLevel convertToEntityAttribute(String dbData) {
      return dbData == null ? null : AccessLevel.valueOf(dbData);
    }
  }

  /**
   * Serializes/deserializes the sealed {@link ResourceMetadata} hierarchy
   * to/from a MariaDB JSON blob. Jackson's {@code @JsonTypeInfo} on the
   * interface ensures correct polymorphic type resolution.
   */
  static class ResourceMetadataConverter implements AttributeConverter<ResourceMetadata, String> {

    private static final ObjectMapper MAPPER;

    static {
      MAPPER = new ObjectMapper();
      MAPPER.registerModule(new JavaTimeModule());
      MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public String convertToDatabaseColumn(ResourceMetadata attribute) {
      if (attribute == null) {
        return null;
      }
      try {
        return MAPPER.writeValueAsString(attribute);
      } catch (JsonProcessingException e) {
        throw new IllegalStateException("Failed to serialize ResourceMetadata", e);
      }
    }

    @Override
    public ResourceMetadata convertToEntityAttribute(String dbData) {
      if (dbData == null || dbData.isBlank()) {
        return null;
      }
      try {
        return MAPPER.readValue(dbData, ResourceMetadata.class);
      } catch (JsonProcessingException e) {
        // Surface the offending payload (truncated) to aid diagnosis — a
        // schema drift between what was persisted and what the record
        // accepts is the usual cause, and Jackson's default message
        // doesn't include the raw JSON.
        String preview = dbData.length() > 200
            ? dbData.substring(0, 200) + "..."
            : dbData;
        throw new IllegalStateException(
            "Failed to deserialize ResourceMetadata — payload: " + preview, e);
      }
    }
  }
}
