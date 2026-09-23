package life.qbic.datamanager.views.projects.project.rawdata.pagination;

import java.util.Objects;
import life.qbic.datamanager.views.general.pagination.ListState;

/**
 * The complete list state of the raw data view (FEAT-PAG-LIST-04, USER-R-03).
 *
 * <p>Holds one {@link ListState} per raw data tab ({@link RawDataDomain}) plus the currently
 * active tab. The URL describes only the active tab's state; the other tabs retain their
 * in-session state so switching tabs restores each tab's last page, filter, and sort.</p>
 *
 * @param activeTab the currently active tab
 * @param ngsState  the list state of the genomics tab
 * @param pxpState  the list state of the proteomics tab
 * @param ipState   the list state of the immunopeptidomics tab
 * @since 1.19.0
 */
public record RawDataListState(
    RawDataDomain activeTab,
    ListState ngsState,
    ListState pxpState,
    ListState ipState) {

  public RawDataListState {
    Objects.requireNonNull(activeTab, "activeTab must not be null");
    ngsState = requireState(ngsState, RawDataDomain.NGS);
    pxpState = requireState(pxpState, RawDataDomain.PXP);
    ipState = requireState(ipState, RawDataDomain.IP);
  }

  private static ListState requireState(ListState state, RawDataDomain fallbackDomain) {
    if (state != null) {
      return state;
    }
    return defaultStateFor(fallbackDomain);
  }

  /**
   * @return the default list state for the given domain (page 1, default page size, no filter,
   *         default sort {@code measurementId asc})
   */
  public static ListState defaultStateFor(RawDataDomain domain) {
    return new ListState(1, RawDataListStateDefaults.DEFAULT_PAGE_SIZE, "", RawDataSort.DEFAULT);
  }

  /**
   * The default state with the given active tab.
   */
  public static RawDataListState defaultWith(RawDataDomain activeTab) {
    return new RawDataListState(activeTab, null, null, null);
  }

  /**
   * @return the list state of the currently active tab
   */
  public ListState activeState() {
    return stateOf(activeTab);
  }

  /**
   * Returns the list state of the given domain, defaulting if absent.
   */
  public ListState stateOf(RawDataDomain domain) {
    return switch (domain) {
      case NGS -> ngsState;
      case PXP -> pxpState;
      case IP -> ipState;
    };
  }

  /**
   * Returns a copy of this state with the given domain's list state replaced.
   */
  public RawDataListState withState(RawDataDomain domain, ListState state) {
    return new RawDataListState(activeTab,
        domain == RawDataDomain.NGS ? state : ngsState,
        domain == RawDataDomain.PXP ? state : pxpState,
        domain == RawDataDomain.IP ? state : ipState);
  }

  /**
   * Returns a copy of this state with the active tab switched and its list state applied.
   */
  public RawDataListState withTab(RawDataDomain tab, ListState tabState) {
    return new RawDataListState(tab,
        tab == RawDataDomain.NGS ? tabState : ngsState,
        tab == RawDataDomain.PXP ? tabState : pxpState,
        tab == RawDataDomain.IP ? tabState : ipState);
  }
}