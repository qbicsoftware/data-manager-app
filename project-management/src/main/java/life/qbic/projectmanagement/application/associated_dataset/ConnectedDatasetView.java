package life.qbic.projectmanagement.application.associated_dataset;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import life.qbic.projectmanagement.domain.model.associated_dataset.AccessLevel;

/**
 * API-contract DTO returned by {@link AssociatedDatasetService#listConnectedDatasetViews
 * (life.qbic.projectmanagement.domain.model.project.ProjectId)} for the UI layer.
 *
 * <p>Flattens the aggregate's universal columns, source-specific metadata
 * fields, and resolved display names (user full name for {@code connectedBy},
 * experiment display name for the linked experiment) into display-ready,
 * source-agnostic top-level fields. The view must never see raw UUIDs for
 * these references.</p>
 *
 * <p>{@code connectedByUserId} and {@code experimentId} are retained as
 * opaque identifiers — useful for row keys or programmatic actions — but
 * the display strings are what human-facing UI cells must render.</p>
 *
 * <p>Null values:
 * <ul>
 *   <li>{@code version} — null when the source did not provide a version</li>
 *   <li>{@code accessLink} — null when not available</li>
 *   <li>{@code resourceType}, {@code community} — null when not provided by the source</li>
 *   <li>{@code accessDetail} — null when not applicable (e.g. PUBLIC datasets)</li>
 *   <li>{@code creators} — empty list when unknown/missing (never null)</li>
 *   <li>{@code experimentId}, {@code experimentName} — null when no experiment was linked</li>
 * </ul>
 *
 * @since 1.12.0
 * @see AssociatedDatasetService#listConnectedDatasetViews
 */
public record ConnectedDatasetView(

    /** Aggregate identity (UUID string). Use as grid row key only. */
    String id,

    /** Human-readable title. Never null. */
    String title,

    /** Persistent identifier (PID / DOI). Never null. */
    String pid,

    /** Coarse access level derived from source-specific metadata. */
    AccessLevel accessLevel,

    /** Version string (e.g. "v1"), or null if not available. */
    String version,

    /** URL to the record on the source instance, or null. */
    String accessLink,

    /** Publication date on the source instance. */
    LocalDate publicationDate,

    /** Display name of the source instance (e.g. "Zenodo", "FDAT"). */
    String resourceProvider,

    /**
     * Individual creator display names (e.g. [{@code "Stanger, Anna"},
     * {@code "Kimmich, Lucca"}]). Empty when unknown. Each element may
     * itself contain commas (v12 InvenioRDM uses "Last, First" format),
     * so callers must not split this field on comma.
     */
    List<String> creators,

    /** Resource type label (e.g. "Dataset"), or null. */
    String resourceType,

    /** Community label (e.g. "QBiC"), or null. */
    String community,

    /**
     * Short access-detail note for restricted datasets, e.g.
     * "Record: public | Files: restricted". Null when not applicable.
     */
    String accessDetail,

    /** Raw user ID of the connecting user (opaque, for logging/keys). */
    String connectedByUserId,

    /**
     * Display name of the user that performed the connection
     * (full name resolved via {@code UserInformationService}).
     * Falls back to the raw user ID if resolution fails.
     */
    String connectedByDisplayName,

    /** Timestamp of the connection. */
    LocalDateTime connectedOn,

    /**
     * Raw experiment ID (UUID string), or null if no experiment is linked.
     * Use as an opaque identifier; prefer {@link #experimentName} for display.
     */
    String experimentId,

    /**
     * Human-readable experiment name resolved via
     * {@code ExperimentInformationService}, or null if no experiment is linked.
     * Falls back to the raw experiment ID string when the experiment cannot be
     * resolved (e.g. it has been deleted).
     */
    String experimentName,

    /** Source system type identifier (e.g. "INVENIO_RDM"). */
    String sourceType
) {

  public ConnectedDatasetView {
    Objects.requireNonNull(id, "id must not be null");
    Objects.requireNonNull(title, "title must not be null");
    Objects.requireNonNull(pid, "pid must not be null");
    Objects.requireNonNull(accessLevel, "accessLevel must not be null");
    Objects.requireNonNull(connectedByUserId, "connectedByUserId must not be null");
    Objects.requireNonNull(connectedByDisplayName, "connectedByDisplayName must not be null");
    Objects.requireNonNull(connectedOn, "connectedOn must not be null");
    Objects.requireNonNull(sourceType, "sourceType must not be null");
  }
}
