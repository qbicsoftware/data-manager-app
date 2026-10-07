package life.qbic.datamanager.views.projects.project.measurements.pagination;

/**
 * Defaults for the paginated measurement lists ({@code FEAT-PAG-LIST-03}, ADR-0008).
 *
 * <p>The measurement lists deviate from the shared
 * {@link life.qbic.datamanager.views.general.pagination.ListStateCodec#DEFAULT_PAGE_SIZE} of
 * 12: 24 rows fit a full viewport on typical screens, so it is the default for all three
 * measurement tabs. The project overview keeps the shared default of 12.</p>
 */
public final class MeasurementListStateDefaults {

  /** Default number of measurements rendered per page. */
  public static final int DEFAULT_PAGE_SIZE = 24;

  private MeasurementListStateDefaults() {
  }
}