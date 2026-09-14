package life.qbic.datamanager.views.general.pagination;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import java.io.Serial;
import java.util.List;
import java.util.Objects;

/**
 * <b>Pagination Bar</b>
 * <p>
 * A reusable pager for explicit, page-based list navigation (USER-R-01): previous/next buttons, a
 * numbered page window with ellipsis gaps (see {@link PageRange}), a location-and-total label
 * ("Page X of Y — N items") and a page-size selector.
 * <p>
 * The bar is a pure view component: it never loads data itself. The owning list applies the
 * requested change and reports the new state back via {@link #setListState(int, long, int)}, which
 * re-renders the bar without firing further events.
 *
 * @since 1.12.0
 */
public class PaginationBar extends Div {

  @Serial
  private static final long serialVersionUID = 939396623019141596L;

  private final List<Integer> allowedPageSizes;
  private final boolean showPageNumbers;
  private final String itemLabel;
  private final Span infoLabel = new Span();
  private final Div pageButtons = new Div();
  private final Button pageSizeButton;
  private final ContextMenu pageSizeMenu;
  private int currentPageSize;
  private final Button previousButton;
  private final Button nextButton;

  private int currentPage = 1;
  private int totalPages = 1;
  private long totalItems = 0;
  private boolean updating;

  /**
   * Creates a pagination bar with a numbered page window for the given page sizes.
   *
   * @param allowedPageSizes the page sizes offered by the selector, must not be empty
   * @param defaultPageSize  the initially selected page size, must be contained in
   *                         {@code allowedPageSizes}
   * @param itemLabel        the label of the counted items, e.g. "projects"
   */
  public PaginationBar(List<Integer> allowedPageSizes, int defaultPageSize, String itemLabel) {
    this(allowedPageSizes, defaultPageSize, itemLabel, true);
  }

  /**
   * Creates a pagination bar for the given page sizes.
   *
   * @param allowedPageSizes the page sizes offered by the selector, must not be empty
   * @param defaultPageSize  the initially selected page size, must be contained in
   *                         {@code allowedPageSizes}
   * @param itemLabel        the label of the counted items, e.g. "projects"
   * @param showPageNumbers  whether the numbered page window (page buttons) is rendered; a
   *                         compact bar without numbers shows only prev/next, the location-and-
   *                         total label and the page-size selector
   */
  public PaginationBar(List<Integer> allowedPageSizes, int defaultPageSize, String itemLabel,
      boolean showPageNumbers) {
    Objects.requireNonNull(allowedPageSizes, "allowedPageSizes must not be null");
    Objects.requireNonNull(itemLabel, "itemLabel must not be null");
    if (allowedPageSizes.isEmpty()) {
      throw new IllegalArgumentException("at least one page size must be offered");
    }
    if (!allowedPageSizes.contains(defaultPageSize)) {
      throw new IllegalArgumentException(
          "defaultPageSize " + defaultPageSize + " must be among " + allowedPageSizes);
    }
    this.allowedPageSizes = List.copyOf(allowedPageSizes);
    this.showPageNumbers = showPageNumbers;
    this.itemLabel = itemLabel;
    addClassName("pagination-bar");

    previousButton = new Button(VaadinIcon.CHEVRON_LEFT.create());
    previousButton.addClassName("pagination-chevron");
    previousButton.setAriaLabel("Previous page");
    previousButton.addClickListener(event -> fireChange(currentPage - 1, pageSize()));

    nextButton = new Button(VaadinIcon.CHEVRON_RIGHT.create());
    nextButton.addClassName("pagination-chevron");
    nextButton.setAriaLabel("Next page");
    nextButton.addClickListener(event -> fireChange(currentPage + 1, pageSize()));

    pageButtons.addClassName("pagination-pages");
    Div navigation = new Div(previousButton, nextButton);
    navigation.addClassName("pagination-navigation");
    if (showPageNumbers) {
      navigation.addComponentAtIndex(1, pageButtons);
    }

    pageSizeButton = new Button(formatPageSizeLabel(defaultPageSize));
    pageSizeButton.addClassName("pagination-page-size");
    pageSizeButton.setAriaLabel("Items per page");
    currentPageSize = defaultPageSize;

    pageSizeMenu = new ContextMenu(pageSizeButton);
    pageSizeMenu.setOpenOnClick(true);
    allowedPageSizes.forEach(size -> {
      MenuItem item = pageSizeMenu.addItem(String.valueOf(size));
      item.addClickListener(event -> fireChange(currentPage, size));
    });

    infoLabel.addClassName("pagination-info");
    Div controls = new Div(infoLabel, pageSizeButton);
    controls.addClassName("pagination-controls");

    add(navigation, controls);
    render();
  }

  /**
   * Reports the current list state to the bar and re-renders it without firing change events.
   *
   * @param page       the currently displayed page, 1-based
   * @param totalItems the total number of items matching the active filter
   * @param pageSize   the currently applied page size
   */
  public void setListState(int page, long totalItems, int pageSize) {
    updating = true;
    try {
      this.totalItems = Math.max(0, totalItems);
      this.totalPages = Math.max(1,
          (int) Math.ceil((double) this.totalItems / pageSize));
      this.currentPage = Math.max(1, Math.min(page, totalPages));
      if (pageSize != currentPageSize) {
        currentPageSize = pageSize;
        pageSizeButton.setText(formatPageSizeLabel(pageSize));
      }
      render();
    } finally {
      updating = false;
    }
  }

  /**
   * Registers a listener notified whenever the user requests a page or page-size change.
   *
   * @param listener the listener to notify
   */
  public void addChangeListener(ComponentEventListener<ChangeEvent> listener) {
    Objects.requireNonNull(listener, "listener must not be null");
    addListener(ChangeEvent.class, listener);
  }

  private int pageSize() {
    return currentPageSize;
  }

  private static String formatPageSizeLabel(int size) {
    return size + " per page";
  }

  private void fireChange(int page, int pageSize) {
    fireEvent(new ChangeEvent(this, true, page, pageSize));
  }

  private void render() {
    infoLabel.setText(
        "Page %d of %d — %d %s".formatted(currentPage, totalPages, totalItems, itemLabel));
    renderPageButtons();
  }

  private void renderPageButtons() {
    if (!showPageNumbers) {
      pageButtons.removeAll();
    } else {
      pageButtons.removeAll();
      PageRange.items(currentPage, totalPages).forEach(item -> {
        if (item.isEllipsis()) {
          Span ellipsis = new Span("…");
          ellipsis.addClassName("pagination-ellipsis");
          pageButtons.add(ellipsis);
          return;
        }
        Button button = new Button(Integer.toString(item.pageNumber()));
        button.addClassName("pagination-page");
        button.setAriaLabel("Go to page " + item.pageNumber());
        if (item.pageNumber() == currentPage) {
          button.addClassName("current");
          button.getElement().setAttribute("aria-current", "page");
        }
        button.addClickListener(event -> fireChange(item.pageNumber(), pageSize()));
        pageButtons.add(button);
      });
    }
    previousButton.setEnabled(currentPage > 1);
    nextButton.setEnabled(currentPage < totalPages);
  }

  /**
   * Event fired when the user requests a page or page-size change.
   * <p>
   * The event carries the requested page and page size; the owning list decides whether clamping or
   * additional rules apply.
   */
  public static class ChangeEvent extends ComponentEvent<PaginationBar> {

    @Serial
    private static final long serialVersionUID = 293015620244716538L;

    private final int page;
    private final int pageSize;

    public ChangeEvent(PaginationBar source, boolean fromClient, int page, int pageSize) {
      super(source, fromClient);
      this.page = page;
      this.pageSize = pageSize;
    }

    /**
     * @return the requested page, 1-based
     */
    public int getPage() {
      return page;
    }

    /**
     * @return the requested page size
     */
    public int getPageSize() {
      return pageSize;
    }
  }
}