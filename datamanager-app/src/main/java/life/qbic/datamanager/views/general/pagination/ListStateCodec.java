package life.qbic.datamanager.views.general.pagination;

import com.vaadin.flow.router.QueryParameters;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import life.qbic.application.commons.SortOrder;

/**
 * (De)serialises a {@link ListState} to and from the query parameters of a route location.
 * <p>
 * Parameter layout (single-valued, stable order): {@code page}, {@code size}, {@code q},
 * {@code sort} (serialised as {@code propertyName:asc|desc}). Absent or invalid values fall back to
 * sensible defaults so that stale or hand-crafted URLs never break the view.
 *
 * @since 1.12.0
 */
public final class ListStateCodec {

  /** Default number of items rendered per page. */
  public static final int DEFAULT_PAGE_SIZE = 24;

  /** Page sizes offered to the user via the pager. */
  public static final List<Integer> ALLOWED_PAGE_SIZES = List.of(12, 24, 48, 96);

  private static final String PAGE_PARAMETER = "page";
  private static final String PAGE_SIZE_PARAMETER = "size";
  private static final String FILTER_PARAMETER = "q";
  private static final String SORT_PARAMETER = "sort";

  private ListStateCodec() {}

  /**
   * Parses a list state from URL query parameters, applying defaults for absent or invalid values.
   *
   * @param parameters  the query parameters of the current location
   * @param defaultSort the fallback sort order, also used when the URL sort is not among the
   *                    allowed sorts
   * @param allowedSorts the sort orders selectable for the concrete list
   * @return the parsed list state, never {@code null}
   */
  public static ListState parse(QueryParameters parameters, SortOrder defaultSort,
      List<SortOrder> allowedSorts) {
    int page = parameters.getSingleParameter(PAGE_PARAMETER)
        .flatMap(ListStateCodec::parseInt)
        .filter(candidate -> candidate > 0)
        .orElse(1);
    int pageSize = parameters.getSingleParameter(PAGE_SIZE_PARAMETER)
        .flatMap(ListStateCodec::parseInt)
        .filter(ALLOWED_PAGE_SIZES::contains)
        .orElse(DEFAULT_PAGE_SIZE);
    String filter = parameters.getSingleParameter(FILTER_PARAMETER).orElse("");
    SortOrder sort = parameters.getSingleParameter(SORT_PARAMETER)
        .map(ListStateCodec::parseSort)
        .filter(allowedSorts::contains)
        .orElse(defaultSort);
    return new ListState(page, pageSize, filter, sort);
  }

  /**
   * Serialises a list state into query parameters. A blank filter is omitted from the URL.
   */
  public static QueryParameters toQueryParameters(ListState state) {
    Map<String, List<String>> parameters = new LinkedHashMap<>();
    parameters.put(PAGE_PARAMETER, List.of(Integer.toString(state.page())));
    parameters.put(PAGE_SIZE_PARAMETER, List.of(Integer.toString(state.pageSize())));
    if (!state.filter().isBlank()) {
      parameters.put(FILTER_PARAMETER, List.of(state.filter()));
    }
    parameters.put(SORT_PARAMETER, List.of(serialiseSort(state.sort())));
    return new QueryParameters(parameters);
  }

  static String serialiseSort(SortOrder sortOrder) {
    return sortOrder.propertyName() + ":" + (sortOrder.isDescending() ? "desc" : "asc");
  }

  private static SortOrder parseSort(String value) {
    int separator = value.indexOf(':');
    if (separator < 0) {
      return new SortOrder(value, true);
    }
    String propertyName = value.substring(0, separator);
    boolean descending = value.substring(separator + 1).equals("desc");
    return new SortOrder(propertyName, descending);
  }

  private static java.util.Optional<Integer> parseInt(String value) {
    try {
      return java.util.Optional.of(Integer.parseInt(value));
    } catch (NumberFormatException e) {
      return java.util.Optional.empty();
    }
  }
}