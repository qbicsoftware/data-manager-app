package life.qbic.datamanager.views.general.pagination;

import java.util.ArrayList;
import java.util.List;

/**
 * Computes the page-number window displayed by a pager.
 * <p>
 * The window always keeps the first and the last page visible plus the current page and its
 * immediate neighbours; gaps are represented by ellipsis {@link Item items}. The logic is pure so it
 * can be unit-tested without a UI context.
 *
 * @since 1.12.0
 */
public final class PageRange {

  private static final int VISIBLE_NEIGHBOURS = 2;

  private PageRange() {}

  /**
   * Computes the page markers to display for the given pagination state.
   *
   * @param currentPage the currently displayed page, 1-based; clamped into {@code [1, totalPages]}
   * @param totalPages  the total number of pages, must be &gt;= 1
   * @return the ordered list of page markers; ellipsis markers carry {@code isEllipsis() == true}
   * @throws IllegalArgumentException if {@code totalPages < 1}
   */
  public static List<Item> items(int currentPage, int totalPages) {
    if (totalPages < 1) {
      throw new IllegalArgumentException("totalPages must be >= 1, but was " + totalPages);
    }
    int page = Math.max(1, Math.min(currentPage, totalPages));
    if (totalPages == 1) {
      return List.of(new Item(1, false));
    }
    List<Item> result = new ArrayList<>();
    result.add(new Item(1, false));
    int first = Math.max(2, page - VISIBLE_NEIGHBOURS);
    int last = Math.min(totalPages - 1, page + VISIBLE_NEIGHBOURS);
    if (first > 2) {
      result.add(Item.ellipsis());
    }
    for (int p = first; p <= last; p++) {
      result.add(new Item(p, false));
    }
    if (last < totalPages - 1) {
      result.add(Item.ellipsis());
    }
    result.add(new Item(totalPages, false));
    return result;
  }

  /**
   * A single marker displayed in a pager: either a concrete page number or an ellipsis gap.
   *
   * @param pageNumber the page number to display, 0 for an ellipsis marker
   * @param isEllipsis whether this marker represents a gap (and carries no page number)
   */
  public record Item(int pageNumber, boolean isEllipsis) {

    public static Item ellipsis() {
      return new Item(0, true);
    }
  }
}