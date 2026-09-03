package life.qbic.projectmanagement.domain.model.associated_dataset;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * InvenioRDM-specific record metadata.
 *
 * <p>Carries the metadata fields that InvenioRDM exposes for a record,
 * stored as the canonical "snapshot of truth" on the aggregate at the
 * time the dataset was connected. Source-specific fields live here
 * rather than on the aggregate root itself (ADR-0001).</p>
 *
 * <p>Instances are immutable.</p>
 *
 * <p><strong>Backward-compatible deserialization:</strong> because this
 * record is persisted as a JSON blob in the {@code resource_metadata}
 * column, unknown fields are silently ignored on read. This allows
 * future removals or renames of fields without breaking rows that were
 * written by earlier versions of the application.</p>
 *
 * @since 1.12.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record InvenioRdmResourceMetadata(

    // --- ResourceMetadata interface fields ---

    /** The human-readable title of the dataset. Never null. */
    String title,
    /** The persistent identifier (PID / DOI). Never null. */
    String pid,
    /** The version string (e.g. "v1"), or null if not available. */
    String version,
    /** The source system URL for access, or null. */
    String accessLink,

    // --- InvenioRDM-specific fields ---

    /** Display name of the InvenioRDM instance (e.g. "Zenodo", "FDAT"). */
    String resourceProvider,
    /** Creator(s) of the dataset. Immutable; never null (empty if unknown). */
    List<String> creators,
    /** The resource type (e.g. "Dataset", "Publication"). May be null. */
    String resourceType,
    /** The community the record belongs to (e.g. "QBiC"), or null. */
    String community,
    /** The publication date of the record on the source instance. Never null. */
    LocalDate publicationDate,
    /** A short description or abstract, or null. */
    String description,

    // --- Access detail and link-lifecycle fields ---

    /** Access status of the record's metadata. Never null. */
    InvenioRdmAccessStatus recordAccess,
    /** Access status of the record's files. Never null. */
    InvenioRdmAccessStatus fileAccess,
    /**
     * The configured source-instance identifier (e.g. {@code "zenodo"}),
     * or null.
     *
     * <p>Stores which configured instance the dataset came from so the
     * connection can be resolved and any created access link revoked later
     * (e.g. cleanup on removal) without a separate persisted column. Set
     * at connect time for connections that created a revocable access link.</p>
     *
     * <p>Nullable and optional; legacy rows and public datasets leave it
     * null.</p>
     */
    String instanceId,
    /**
     * The identity of the shareable access link created on the source
     * system for this restricted dataset, or null.
     *
     * <p>This is the external link {@code id} returned by InvenioRDM when
     * creating an access link ({@code AccessLinkResponse.id}). It is stored
     * so the link can be revoked (deleted) again on the source system when
     * the connection is removed or a connect attempt is rolled back. It is
     * <strong>not</strong> the token embedded in {@link #accessLink()} — it
     * targets the DELETE {@code /access/links/{id}} endpoint.</p>
     *
     * <p>Nullable and optional: only set for access-restricted datasets
     * connected with a created access link. Legacy rows written before
     * this field existed deserialize to {@code null} and are simply skipped
     * by revocation.</p>
     */
    String accessLinkId,
    /**
     * The InvenioRDM concept (parent) record identifier of the dataset,
     * or null.
     *
     * <p>InvenioRDM publishes every version as a new record sharing one
     * concept (parent) record. Resolving the parent recid via
     * {@code GET /records/{parentId}} always yields the latest published
     * version (HTTP 302 → latest record). Sync (FEAT-DATSET-04/08) uses
     * this handle to follow version chains; see ADR-0005.</p>
     *
     * <p>Nullable and optional: legacy rows written before this field
     * existed deserialize to {@code null} and resolve the parent from the
     * stored record on their first sync.</p>
     *
     * @since 1.13.0
     */
    String parentHandle

) implements ResourceMetadata {

  /**
   * Backward-compatible constructor leaving {@code instanceId} and
   * {@code accessLinkId} unset (both null).
   *
   * <p>Keeps existing call sites (and persisted {@code resource_metadata}
   * rows deserialized by Jackson's canonical constructor) working without
   * supplying link-lifecycle information. Connections created this way are
   * simply skipped by access-link revocation.</p>
   *
   * @param title            record title
   * @param pid              persistent identifier
   * @param version          version string or null
   * @param accessLink       access URL or null
   * @param resourceProvider display name of the instance
   * @param creators         creator names (nullable → empty)
   * @param resourceType     resource type or null
   * @param community        community or null
   * @param publicationDate  publication date (never null)
   * @param description      abstract or null
   * @param recordAccess     record access status (never null)
   * @param fileAccess       file access status (never null)
   */
  public InvenioRdmResourceMetadata(
      String title,
      String pid,
      String version,
      String accessLink,
      String resourceProvider,
      List<String> creators,
      String resourceType,
      String community,
      LocalDate publicationDate,
      String description,
      InvenioRdmAccessStatus recordAccess,
      InvenioRdmAccessStatus fileAccess) {
    this(title, pid, version, accessLink, resourceProvider, creators,
        resourceType, community, publicationDate, description,
        recordAccess, fileAccess, null, null, null);
  }

  /**
   * Backward-compatible constructor with link-lifecycle fields but without
   * a parent handle (legacy schema / test helpers). Delegates with
   * {@code parentHandle} unset ({@code null}).
   *
   * @param title            record title
   * @param pid              persistent identifier
   * @param version          version string or null
   * @param accessLink       access URL or null
   * @param resourceProvider display name of the instance
   * @param creators         creator names (nullable → empty)
   * @param resourceType     resource type or null
   * @param community        community or null
   * @param publicationDate  publication date (never null)
   * @param description      abstract or null
   * @param recordAccess     record access status (never null)
   * @param fileAccess       file access status (never null)
   * @param instanceId       the configured source instance, or null
   * @param accessLinkId     the access-link id, or null
   * @since 1.12.0 (extended 1.13.0)
   */
  public InvenioRdmResourceMetadata(
      String title,
      String pid,
      String version,
      String accessLink,
      String resourceProvider,
      List<String> creators,
      String resourceType,
      String community,
      LocalDate publicationDate,
      String description,
      InvenioRdmAccessStatus recordAccess,
      InvenioRdmAccessStatus fileAccess,
      String instanceId,
      String accessLinkId) {
    this(title, pid, version, accessLink, resourceProvider, creators,
        resourceType, community, publicationDate, description,
        recordAccess, fileAccess, instanceId, accessLinkId, null);
  }



  public InvenioRdmResourceMetadata {
    Objects.requireNonNull(title, "title must not be null");
    Objects.requireNonNull(pid, "pid must not be null");
    Objects.requireNonNull(resourceProvider, "resourceProvider must not be null");
    Objects.requireNonNull(publicationDate, "publicationDate must not be null");
    Objects.requireNonNull(recordAccess, "recordAccess must not be null");
    Objects.requireNonNull(fileAccess, "fileAccess must not be null");
    // Defensive immutability: normalize null creators to empty, then freeze
    if (creators == null) {
      creators = List.of();
    }
    creators = List.copyOf(creators);
  }

  /**
   * Returns the creators as a single comma-joined display string.
   */
  public String creatorsDisplay() {
    if (creators.isEmpty()) {
      return "—";
    }
    return String.join(", ", creators);
  }

  /**
   * Returns the publication date formatted for display (English locale),
   * or "—" if publicationDate is null.
   */
  public String publicationDateDisplay() {
    if (publicationDate == null) {
      return "—";
    }
    return publicationDate.format(
        java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH));
  }

  /**
   * Derives the coarse {@link AccessLevel} from the two independent
   * access dimensions reported by InvenioRDM.
   *
   * <p>Rule: the dataset is fully public only when both record and file
   * access are {@link InvenioRdmAccessStatus#PUBLIC}. Otherwise it is
   * {@link AccessLevel#RESTRICTED}.</p>
   *
   * <p>Note: InvenioRDM reports {@code access.status = "embargoed"} for
   * time-locked records, which maps to {@link InvenioRdmAccessStatus#EMBARGOED}
   * on the access status fields — so an embargoed record naturally fails
   * this check without requiring a separate embargo-date comparison.</p>
   */
  public AccessLevel deriveAccessLevel() {
    if (recordAccess == InvenioRdmAccessStatus.PUBLIC
        && fileAccess == InvenioRdmAccessStatus.PUBLIC) {
      return AccessLevel.PUBLIC;
    }
    return AccessLevel.RESTRICTED;
  }

  /**
   * Returns a short access detail note suitable for display in a tooltip
   * or badge sub-text, e.g. "Record: public | Files: restricted".
   */
  public String accessDetailDisplay() {
    return "Record: %s | Files: %s".formatted(
        labelFor(recordAccess),
        labelFor(fileAccess));
  }

  private static String labelFor(InvenioRdmAccessStatus status) {
    return switch (status) {
      case PUBLIC -> "public";
      case RESTRICTED -> "restricted";
      case EMBARGOED -> "embargoed";
    };
  }
}
