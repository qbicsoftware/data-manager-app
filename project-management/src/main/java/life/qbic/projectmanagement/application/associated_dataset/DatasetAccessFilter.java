package life.qbic.projectmanagement.application.associated_dataset;

/**
 * An optional access-status filter for searching external datasets.
 *
 * <p>This is a <em>source-neutral</em> selection of dataset accessibility
 * used to narrow a search — distinct from {@link AccessLevel}, which is a
 * domain concept describing the access state of an already-connected
 * dataset. Keeping the search filter and the connection state as separate,
 * differently-named types prevents conflating a search intent with a
 * persisted state.</p>
 *
 * <p>Instances are immutable and side-effect free — they carry no state
 * beyond the constant identity. The translation to a source-specific wire
 * value (e.g. InvenioRDM's {@code access.status} facet) is the
 * responsibility of the source adapter, not of callers of the service.</p>
 *
 * <p>Used as the {@code accessFilter} parameter of
 * {@link AssociatedDatasetService#searchDatasets(SourceType, String, String,
 * DatasetAccessFilter, int, int, String)}. {@code null} means "no filter —
 * return all records".</p>
 *
 * @since 1.12.0
 */
public enum DatasetAccessFilter {

  /**
   * Only datasets that are not publicly accessible without
   * authentication (access-restricted).
   */
  RESTRICTED,

  /**
   * Only datasets that are publicly accessible without authentication.
   */
  PUBLIC;

  /**
   * Returns the externally visible wire representation of this enum
   * constant.
   *
   * <p>Intended only for use by source adapters that must translate this
   * neutral value into an external wire term (e.g. InvenioRDM's
   * {@code access.status} facet). Application and UI code should not call
   * it — doing so would leak source-specific vocabulary out of the
   * adapter, which this type is designed to prevent.</p>
   *
   * @return the wire facet value (e.g. {@code "restricted"}, {@code "open"})
   */
  public String toSourceValue() {
    return switch (this) {
      case RESTRICTED -> "restricted";
      case PUBLIC -> "open";
    };
  }
}