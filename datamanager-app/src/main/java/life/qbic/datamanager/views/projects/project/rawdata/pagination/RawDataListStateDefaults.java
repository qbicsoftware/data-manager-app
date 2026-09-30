package life.qbic.datamanager.views.projects.project.rawdata.pagination;

/**
 * Defaults for the paginated raw dataset lists ({@code FEAT-PAG-LIST-04}).
 *
 * <p>The raw data lists deviate from the shared
 * {@link life.qbic.datamanager.views.general.pagination.ListStateCodec#DEFAULT_PAGE_SIZE} of
 * 12: 24 rows fit a full viewport on typical screens, so it is the default for all three raw
 * data tabs (matching the paginated measurement lists, ADR-0008).</p>
 */
public final class RawDataListStateDefaults {

  /** Default number of raw datasets rendered per page. */
  public static final int DEFAULT_PAGE_SIZE = 24;

  private RawDataListStateDefaults() {
  }
}