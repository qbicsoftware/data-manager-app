package life.qbic.projectmanagement.domain.model.associated_dataset;

/**
 * The type of external system that hosts the associated dataset.
 *
 * <p>This is the source-agnostic extension point defined by ADR-0001.
 * Adding a new external source (e.g. a LIMS) requires only a new enum
 * value here and the corresponding {@link ResourceMetadata} subtype.</p>
 *
 * @since 1.12.0
 */
public enum SourceType {

  /**
   * An InvenioRDM-based repository (e.g. Zenodo, FDAT).
   */
  INVENIO_RDM;

  public static SourceType parse(String value) {
    try {
      return valueOf(value);
    } catch (IllegalArgumentException | NullPointerException e) {
      throw new IllegalArgumentException("Unknown source type: " + value, e);
    }
  }
}
