package life.qbic.projectmanagement.domain.model.associated_dataset;

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
 * @since 1.12.0
 */
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

    // --- Access detail fields (InvenioRDM-specific two-dimension model) ---

    /** Access status of the record's metadata. Never null. */
    InvenioRdmAccessStatus recordAccess,
    /** Access status of the record's files. Never null. */
    InvenioRdmAccessStatus fileAccess,
    /** The date when an embargo lifts (null if not under embargo). */
    LocalDate embargoUntil

) implements ResourceMetadata {

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
   * access dimensions plus embargo state.
   *
   * <p>Rule: the dataset is fully public only when both record and file
   * access are {@link InvenioRdmAccessStatus#PUBLIC} <em>and</em> no
   * embargo is set. Otherwise it is {@link AccessLevel#RESTRICTED}.</p>
   */
  public AccessLevel deriveAccessLevel() {
    if (recordAccess == InvenioRdmAccessStatus.PUBLIC
        && fileAccess == InvenioRdmAccessStatus.PUBLIC
        && embargoUntil == null) {
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
