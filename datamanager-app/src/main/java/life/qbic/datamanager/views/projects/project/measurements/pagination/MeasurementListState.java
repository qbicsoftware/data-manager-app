package life.qbic.datamanager.views.projects.project.measurements.pagination;

import java.util.Objects;
import life.qbic.datamanager.views.general.pagination.ListState;

/**
 * The complete list state of the measurement view (ADR-0008, USER-R-03).
 *
 * <p>Holds one {@link ListState} per measurement tab ({@link MeasurementDomain}) plus the
 * currently active tab. The URL describes only the active tab's state; the other tabs retain
 * their in-session state so switching tabs restores each tab's last page, filter, and sort.</p>
 *
 * @param activeTab   the currently active tab
 * @param ngsState    the list state of the genomics tab
 * @param pxpState    the list state of the proteomics tab
 * @param ipState     the list state of the immunopeptidomics tab
 * @since 1.19.0
 */
public record MeasurementListState(
    MeasurementDomain activeTab,
    ListState ngsState,
    ListState pxpState,
    ListState ipState) {

  public MeasurementListState {
    Objects.requireNonNull(activeTab, "activeTab must not be null");
    ngsState = requireState(ngsState, MeasurementDomain.NGS);
    pxpState = requireState(pxpState, MeasurementDomain.PXP);
    ipState = requireState(ipState, MeasurementDomain.IP);
  }

  private static ListState requireState(ListState state, MeasurementDomain fallbackDomain) {
    if (state != null) {
      return state;
    }
    return defaultStateFor(fallbackDomain);
  }

  /**
   * @return the default list state for the given domain (page 1, default page size, no filter,
   *         default sort {@code registeredAt desc})
   */
  public static ListState defaultStateFor(MeasurementDomain domain) {
    return new ListState(1, MeasurementListStateDefaults.DEFAULT_PAGE_SIZE, "",
        MeasurementSort.DEFAULT);
  }

  /**
   * The default state with the given active tab.
   */
  public static MeasurementListState defaultWith(MeasurementDomain activeTab) {
    return new MeasurementListState(activeTab, null, null, null);
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
  public ListState stateOf(MeasurementDomain domain) {
    return switch (domain) {
      case NGS -> ngsState;
      case PXP -> pxpState;
      case IP -> ipState;
    };
  }

  /**
   * Returns a copy of this state with the given domain's list state replaced.
   */
  public MeasurementListState withState(MeasurementDomain domain, ListState state) {
    return new MeasurementListState(activeTab,
        domain == MeasurementDomain.NGS ? state : ngsState,
        domain == MeasurementDomain.PXP ? state : pxpState,
        domain == MeasurementDomain.IP ? state : ipState);
  }

  /**
   * Returns a copy of this state with the active tab switched and its list state applied.
   */
  public MeasurementListState withTab(MeasurementDomain tab, ListState tabState) {
    return new MeasurementListState(tab,
        tab == MeasurementDomain.NGS ? tabState : ngsState,
        tab == MeasurementDomain.PXP ? tabState : pxpState,
        tab == MeasurementDomain.IP ? tabState : ipState);
  }
}