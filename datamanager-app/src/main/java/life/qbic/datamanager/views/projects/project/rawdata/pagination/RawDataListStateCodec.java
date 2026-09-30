package life.qbic.datamanager.views.projects.project.rawdata.pagination;

import com.vaadin.flow.router.QueryParameters;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import life.qbic.datamanager.views.general.pagination.ListState;
import life.qbic.datamanager.views.general.pagination.ListStateCodec;

/**
 * (De)serialises the tabbed raw data list state to and from the URL query parameters
 * (FEAT-PAG-LIST-04, USER-R-03).
 *
 * <p>The raw data route carries a {@code tab=ngs|pxp|ip} parameter plus the flat
 * {@code page/size/q/sort} parameters of the <b>active</b> tab, using the exact parameter layout
 * of {@link ListStateCodec} for the list state itself. In-session, each tab keeps its own
 * {@link ListState} — {@link #parse(QueryParameters, RawDataListState)} restores the active tab's
 * state from the URL and keeps the other tabs' current in-session state.</p>
 *
 * <p>Serialisation writes the active tab and its list state; the inactive tabs' states are not
 * part of the URL (deliberate: one URL describes one view).</p>
 *
 * @since 1.19.0
 */
public final class RawDataListStateCodec {

  private static final String TAB_PARAMETER = "tab";

  private RawDataListStateCodec() {}

  /**
   * Parses the raw data list state from the URL query parameters.
   *
   * @param parameters the query parameters of the current location
   * @param current    the current in-session state per domain; the active tab's entry is
   *                   overwritten from the URL, the others are kept
   * @return the parsed state; the active tab is {@link ListStateCodec#parse parsed} from the URL,
   *         the remaining tabs retain their passed-in state
   */
  public static RawDataListState parse(QueryParameters parameters, RawDataListState current) {
    RawDataDomain activeTab = RawDataDomain.fromUrlValue(
        parameters.getSingleParameter(TAB_PARAMETER).orElse(null));
    ListState activeState = ListStateCodec.parse(parameters, RawDataSort.DEFAULT, RawDataSort.SORTS,
        RawDataListStateDefaults.DEFAULT_PAGE_SIZE);
    return current.withTab(activeTab, activeState);
  }

  /**
   * Serialises the active tab and its list state into query parameters.
   *
   * @param state the state to serialise; only the active tab is written to the URL
   * @return query parameters carrying {@code tab} plus the active tab's {@code page/size/q/sort}
   */
  public static QueryParameters toQueryParameters(RawDataListState state) {
    Map<String, List<String>> parameters = new LinkedHashMap<>();
    parameters.put(TAB_PARAMETER,
        List.of(state.activeTab().urlValue()));
    ListState activeState = state.stateOf(state.activeTab());
    Map<String, List<String>> stateParameters = ListStateCodec
        .toQueryParameters(activeState).getParameters();
    parameters.putAll(stateParameters);
    return new QueryParameters(mapWithStableOrder(parameters));
  }

  private static Map<String, List<String>> mapWithStableOrder(
      Map<String, List<String>> input) {
    Map<String, List<String>> ordered = new LinkedHashMap<>();
    // parameter layout: tab, page, size, q, sort (stable, single-valued)
    ordered.put(TAB_PARAMETER, input.get(TAB_PARAMETER));
    ordered.put("page", input.get("page"));
    ordered.put("size", input.get("size"));
    if (input.containsKey("q")) {
      ordered.put("q", input.get("q"));
    }
    ordered.put("sort", input.get("sort"));
    return ordered;
  }
}