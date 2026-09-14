package life.qbic.datamanager.views.general.pagination;

import java.util.Objects;
import life.qbic.application.commons.SortOrder;

/**
 * Immutable description of the state of a paginated list view.
 * <p>
 * The state is the single source of truth for what a paginated list displays: the current page
 * (1-based), the page size, the active free-text filter and the active sort order. It is mirrored
 * into the browser URL by {@link ListStateCodec} so that list views are restorable via browser
 * history and shareable as links (USER-R-03).
 *
 * @param page     the current page number, 1-based
 * @param pageSize the number of items rendered per page
 * @param filter   the active free-text filter, may be blank
 * @param sort     the active sort order
 * @since 1.12.0
 */
public record ListState(int page, int pageSize, String filter, SortOrder sort) {

  public ListState {
    if (page < 1) {
      throw new IllegalArgumentException("page must be >= 1, but was " + page);
    }
    if (pageSize < 1) {
      throw new IllegalArgumentException("pageSize must be >= 1, but was " + pageSize);
    }
    filter = filter == null ? "" : filter;
    sort = Objects.requireNonNull(sort, "sort must not be null");
  }

  /**
   * Returns a copy of this state with the given page, keeping all other fields.
   */
  public ListState withPage(int newPage) {
    return new ListState(newPage, pageSize, filter, sort);
  }

  /**
   * Returns a copy of this state with the given page size, keeping all other fields.
   */
  public ListState withPageSize(int newPageSize) {
    return new ListState(page, newPageSize, filter, sort);
  }

  /**
   * Returns a copy of this state with the given filter, keeping all other fields.
   */
  public ListState withFilter(String newFilter) {
    return new ListState(page, pageSize, newFilter, sort);
  }

  /**
   * Returns a copy of this state with the given sort order, keeping all other fields.
   */
  public ListState withSort(SortOrder newSort) {
    return new ListState(page, pageSize, filter, newSort);
  }
}