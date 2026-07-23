package life.qbic.projectmanagement.application.associated_dataset;

import java.time.LocalDate;
import java.util.Objects;

/**
 * A single record found by searching an external data source.
 *
 * <p>Transient DTO — carries enough information for the UI search-results
 * display. Produced by the infrastructure adapter (implementing the
 * {@link DatasetSource} port) and returned to the UI via the
 * {@link AssociatedDatasetService}.</p>
 *
 * <p>Nullable fields: {@code version}, {@code description}, and
 * {@code accessDetail} are null when the source does not provide them
 * or when they are not applicable.</p>
 *
 * @since 1.12.0
 */
public record SearchHit(
    /**
     * The external record identifier on the source (e.g. Zenodo record ID,
     * DOI). Used as the {@code externalHandle} when connecting the dataset.
     */
    String externalHandleValue,

    /** Human-readable title. */
    String title,

    /** Persistent identifier (PID / DOI). */
    String pid,

    /** Version string, or null if unknown. */
    String version,

    /** Publication date on the source. */
    LocalDate publicationDate,

    /** Display name of the source instance (e.g. "Zenodo"). */
    String resourceProvider,

    /** Short description or abstract, or null. */
    String description,

    /**
     * Whether the dataset is publicly accessible.
     * For InvenioRDM: true only when record + file access are public
     * and no embargo is active.
     */
    boolean isPublic,

    /**
     * A source-specific access detail string for display, e.g.
     * "Record: public | Files: restricted". Null if the source has no
     * such distinction.
     */
    String accessDetail
) {

  public SearchHit {
    Objects.requireNonNull(externalHandleValue, "externalHandleValue must not be null");
    Objects.requireNonNull(title, "title must not be null");
    Objects.requireNonNull(pid, "pid must not be null");
    Objects.requireNonNull(publicationDate, "publicationDate must not be null");
    Objects.requireNonNull(resourceProvider, "resourceProvider must not be null");
  }
}
