package life.qbic.projectmanagement.domain.model.associated_dataset;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.time.LocalDate;

/**
 * Source-agnostic metadata container for an associated dataset.
 *
 * <p>Per ADR-0001, the aggregate root carries source-specific metadata
 * encapsulated behind this sealed hierarchy. The aggregate root stores
 * only the {@code ResourceMetadata} object (as an opaque JSON blob);
 * the concrete subtype (e.g. {@link InvenioRdmResourceMetadata}) is
 * determined by the {@link SourceType} carried on the aggregate.</p>
 *
 * <p>The four accessors on this interface represent the <em>universal
 * columns</em> (per ADR-0001 §Decision §4): fields that are reasonable
 * to extract to regular SQL columns for sort/filter, because they are
 * expected to exist for any external source (Zenodo, FDAT, future LIMS,
 * etc.). Source-specific fields remain inside the concrete subtype and
 * are accessible via a cast from {@code associatedDataset.resourceMetadata()}.</p>
 *
 * <p>Instances are immutable value objects — created at connect-time and
 * persisted as the "snapshot of truth" for the connection lifecycle.</p>
 *
 * @since 1.12.0
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = InvenioRdmResourceMetadata.class, name = "INVENIO_RDM")
})
public sealed interface ResourceMetadata
    permits InvenioRdmResourceMetadata {

  /**
   * The human-readable title of the dataset. Never null.
   */
  String title();

  /**
   * The persistent identifier (PID / DOI) of the dataset. Never null.
   */
  String pid();

  /**
   * The version string (e.g. "v1", "v2.0"), or null if not available.
   */
  String version();

  /**
   * The date when the resource was published or released on the source
   * system. Universal across sources (any external dataset has some
   * notion of release date). May be null if the specific source does
   * not provide one.
   */
  LocalDate publicationDate();

}
