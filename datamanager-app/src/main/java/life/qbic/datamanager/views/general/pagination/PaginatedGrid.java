package life.qbic.datamanager.views.general.pagination;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridMultiSelectionModel;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.selection.MultiSelectionEvent;
import com.vaadin.flow.data.selection.SelectionEvent;
import com.vaadin.flow.data.value.ValueChangeMode;
import java.io.Serial;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import life.qbic.application.commons.SortOrder;

/**
 * A reusable, explicitly paginated grid (USER-R-01/-R-02/-R-03).
 *
 * <p>The paginated analogue of the lazy {@code FilterGrid}: it decorates a plain Vaadin
 * {@link Grid} with the machinery needed for page-based list navigation instead of endless
 * scroll-loading. The component owns the list state ({@link ListState}), an identifier-based
 * cross-page {@link Selection}, the {@link PaginationBar}, and a search field, and coordinates
 * them so a view only has to supply <i>how</i> a page is loaded.</p>
 *
 * <p>The component is deliberately entity-agnostic: the owning view supplies a
 * {@link PageLoader} (translates the {@link ListState} into an offset/limit + count lookup) and an
 * {@link IdExtractor} (maps a row to the identifier used by the {@link Selection}). Everything else
 * — page clamping, empty states, selection reconciliation against the identifier set, the pager and
 * the search field — is handled here, so samples, measurements and raw datasets do not duplicate
 * the grid glue.</p>
 *
 * @param <T> the type of items rendered in the grid
 * @since 1.19.0
 */
public class PaginatedGrid<T> extends Div {

  @Serial
  private static final long serialVersionUID = 1L;

  private final Grid<T> grid;
  private final PageLoader<T> pageLoader;
  private final Function<T, String> idExtractor;
  private final PaginationBar paginationBar;
  private final Selection selection;
  private final TextField searchField = createSearchField();
  private final Span selectionDisplay = new Span();
  private final Button clearSelectionButton = new Button("Clear selection");
  private final Div emptyState = new Div();
  private ListState listState;
  private final String itemLabel;
  private boolean initialLoadDone;

  /**
   * Loads a single page of items for a list state.
   *
   * @param <T> the item type
   */
  @FunctionalInterface
  public interface PageLoader<T> {

    /**
     * Fetches the items of the current page and the total number of items matching the state's
     * filter.
     *
     * @param state the list state describing the page, page size, filter and sort to load
     * @return the page items and the total count, never {@code null}
     */
    Page<T> load(ListState state);
  }

  /**
   * The result of a page load: the items rendered on the current page and the total number of
   * items matching the active filter.
   *
   * @param items the items of the current page
   * @param total the total number of items matching the filter (all pages)
   * @param <T>   the item type
   */
  public record Page<T>(List<T> items, long total) {

    public Page {
      items = List.copyOf(Objects.requireNonNull(items, "items must not be null"));
      if (total < 0) {
        throw new IllegalArgumentException("total must be >= 0, but was " + total);
      }
    }
  }

  /**
   * Creates a paginated grid wrapping the given, already column-configured grid.
   *
   * @param grid        the grid to decorate; its selection mode is set to MULTI by this component
   * @param pageLoader  loads the current page for a list state (see {@link PageLoader})
   * @param idExtractor maps a row to the identifier used for cross-page selection
   * @param itemLabel   the label of the counted items, e.g. "samples" (used by the pager and the
   *                    selection display)
   * @param defaultSort the fallback sort order, used until the user changes the sort
   */
  public PaginatedGrid(Grid<T> grid, PageLoader<T> pageLoader, Function<T, String> idExtractor,
      String itemLabel, SortOrder defaultSort) {
    this.grid = Objects.requireNonNull(grid, "grid must not be null");
    this.pageLoader = Objects.requireNonNull(pageLoader, "pageLoader must not be null");
    this.idExtractor = Objects.requireNonNull(idExtractor, "idExtractor must not be null");
    this.itemLabel = Objects.requireNonNull(itemLabel, "itemLabel must not be null");
    this.listState = new ListState(1, ListStateCodec.DEFAULT_PAGE_SIZE, "",
        Objects.requireNonNull(defaultSort, "defaultSort must not be null"));
    this.paginationBar = new PaginationBar(ListStateCodec.ALLOWED_PAGE_SIZES,
        ListStateCodec.DEFAULT_PAGE_SIZE, itemLabel);
    this.selection = new Selection(this::updateSelectionDisplay);

    configureGrid();
    configureSearch();
    configureSort();
    configureSelection();
    configurePagination();

    addClassNames("paginated-grid", "flex-vertical", "gap-03", "width-full");
    Div toolbar = new Div(searchField, selectionDisplay, clearSelectionButton);
    toolbar.addClassName("paginated-grid-toolbar");
    emptyState.addClassName("paginated-grid-empty-state");
    emptyState.setVisible(false);
    add(toolbar, grid, emptyState, paginationBar);

    // Load the first page when the component is attached, so the grid shows data immediately
    // without the owning view having to trigger an explicit refresh.
    addAttachListener(event -> {
      if (!initialLoadDone) {
        initialLoadDone = true;
        setListState(listState);
      }
    });
  }

  private void configureGrid() {
    grid.setSelectionMode(Grid.SelectionMode.MULTI);
    if (grid.getSelectionModel() instanceof GridMultiSelectionModel<?> multiSelectionModel) {
      multiSelectionModel.setSelectionColumnFrozen(true);
    }
    // With explicit pagination the grid shows exactly the current page; render all rows at their
    // natural height so the native page scroll handles overflow (USER-NFR-01).
    grid.setAllRowsVisible(true);
  }

  /**
   * Wires grid column sorting into the list state. A column click updates {@link ListState#sort}
   * to the clicked column's sort property and direction, and reloads the first page. Paginated
   * grids are single-sort (ADR-0009): only the first sort order is applied. The {@link PageLoader}
   * is responsible for validating the property against the backend sort keys.
   */
  private void configureSort() {
    grid.setMultiSort(false);
    grid.addSortListener(event -> {
      List<GridSortOrder<T>> orders = grid.getSortOrder();
      if (orders.isEmpty()) {
        return;
      }
      GridSortOrder<T> order = orders.get(0);
      if (order.getSorted() == null) {
        return;
      }
      String property = order.getSorted()
          .getSortOrder(order.getDirection())
          .findFirst()
          .map(com.vaadin.flow.data.provider.QuerySortOrder::getSorted)
          .orElse(null);
      if (property == null || property.isBlank()) {
        return;
      }
      boolean descending = order.getDirection() == SortDirection.DESCENDING;
      setListState(listState.withSort(new SortOrder(property, descending)).withPage(1));
    });
  }

  private void configureSearch() {
    searchField.addValueChangeListener(event -> {
      String filter = event.getValue() == null ? "" : event.getValue().trim();
      if (filter.equals(listState.filter())) {
        return;
      }
      setListState(listState.withFilter(filter).withPage(1));
    });
  }

  private void configureSelection() {
    @SuppressWarnings("unchecked")
    Grid<Object> objectGrid = (Grid<Object>) grid;
    objectGrid.addSelectionListener(this::onGridSelectionChanged);
    clearSelectionButton.addClickListener(event -> {
      selection.clear();
      applySelectionToGrid();
    });
  }

  /**
   * Translates grid row selection changes into the identifier-based {@link Selection}. Only
   * client-side changes are translated (ADR-0009): server-side events fired by Vaadin when a new
   * page is written via {@code setItems} would otherwise purge every selected identifier that is
   * not on the newly rendered page. The rows are reconciled against the identifier set afterwards
   * by {@link #applySelectionToGrid}.
   */
  private void onGridSelectionChanged(SelectionEvent<Grid<Object>, Object> event) {
    if (!event.isFromClient()) {
      return;
    }
    MultiSelectionEvent<Grid<Object>, Object> multi =
        (MultiSelectionEvent<Grid<Object>, Object>) event;
    multi.getAddedSelection().forEach(item -> selection.select(idExtractor.apply((T) item)));
    multi.getRemovedSelection().forEach(item -> selection.deselect(idExtractor.apply((T) item)));
  }

  private void configurePagination() {
    paginationBar.addChangeListener(event -> {
      ListState current = listState;
      ListState requested;
      if (event.getPageSize() != current.pageSize()) {
        requested = current.withPageSize(event.getPageSize()).withPage(1);
      } else {
        requested = current.withPage(event.getPage());
      }
      if (requested.equals(current)) {
        return;
      }
      setListState(requested);
    });
  }

  /**
   * Applies a new list state, loads the page and updates the grid, the pager, the empty state and
   * the selection display.
   *
   * @param state the state to apply; the page is clamped to the last valid page when it lies
   *              beyond the result set
   */
  public void setListState(ListState state) {
    Page<T> page = pageLoader.load(state);
    int totalPages = Math.max(1, (int) Math.ceil((double) page.total() / state.pageSize()));
    int pageToRender = Math.min(state.page(), totalPages);
    if (pageToRender != state.page()) {
      // the requested page lies beyond the last valid page (filter/deletion shrank the result
      // set); re-fetch the clamped page once
      this.listState = state.withPage(pageToRender);
      setListState(this.listState);
      return;
    }
    this.listState = state.withPage(pageToRender);
    grid.setItems(page.items());
    renderEmptyState(page.items().isEmpty(), !state.filter().isBlank());
    paginationBar.setListState(pageToRender, page.total(), state.pageSize());
    paginationBar.setVisible(page.total() > 0);
    applySelectionToGrid();
    updateSelectionDisplay();
    fireEvent(new PageLoadedEvent(this, pageToRender, page.total()));
  }

  private void renderEmptyState(boolean isEmpty, boolean hasFilter) {
    emptyState.setVisible(isEmpty);
    if (isEmpty) {
      emptyState.setText(hasFilter
          ? "No %ss match the current search. Clear the search to see all %ss.".formatted(itemLabel,
              itemLabel)
          : "No %ss registered yet.".formatted(itemLabel));
    }
  }

  /**
   * Applies the identifier-based {@link Selection} to the grid's visible rows so that
   * checkboxes reflect the cross-page selection on the current page.
   */
  private void applySelectionToGrid() {
    grid.getGenericDataView().getItems().forEach(item -> {
      if (selection.contains(idExtractor.apply(item))) {
        grid.select(item);
      } else {
        grid.deselect(item);
      }
    });
  }

  private void updateSelectionDisplay() {
    int count = selection.count();
    if (count > 0) {
      selectionDisplay.setText("%d %s selected".formatted(count, count == 1 ? itemLabel : itemLabel + "s"));
      selectionDisplay.setVisible(true);
      clearSelectionButton.setVisible(true);
    } else {
      selectionDisplay.setVisible(false);
      clearSelectionButton.setVisible(false);
    }
    fireEvent(new SelectionChangeEvent(this, selection.selectedIds()));
  }

  /**
   * Reloads the current page with the current list state.
   */
  public void refresh() {
    setListState(listState);
  }

  /**
   * Applies an externally provided list state (initial page load, URL back/forward, shared link)
   * and reloads the page.
   */
  public void applyExternalState(ListState state) {
    setListState(state);
  }

  /**
   * @return the currently applied list state
   */
  public ListState listState() {
    return listState;
  }

  /**
   * @return the set of selected item identifiers across all pages
   */
  public Set<String> selectedIds() {
    return selection.selectedIds();
  }

  /**
   * Selects the given item identifiers (e.g. a "select all N matching the filter" action that
   * resolves identifiers in backend storage) and reconciles the visible rows.
   */
  public void select(Set<String> ids) {
    selection.select(ids);
    applySelectionToGrid();
    updateSelectionDisplay();
  }

  /**
   * Removes the given item identifiers from the selection (e.g. after a deletion) and reconciles
   * the visible rows.
   */
  public void deselect(Set<String> ids) {
    selection.deselect(ids);
    applySelectionToGrid();
    updateSelectionDisplay();
  }

  /**
   * @return the items of the current page that are selected
   */
  public Set<T> selectedElements() {
    return grid.getSelectedItems();
  }

  /**
   * Sets the placeholder of the search field.
   */
  public void setSearchPlaceholder(String placeholder) {
    searchField.setPlaceholder(placeholder);
  }

  /**
   * @return the wrapped grid, e.g. to add bulk-action buttons or expose columns
   */
  public Grid<T> grid() {
    return grid;
  }

  /**
   * Registers a listener notified whenever the user changes a page or the page size.
   */
  public void addPageLoadedListener(ComponentEventListener<PageLoadedEvent> listener) {
    addListener(PageLoadedEvent.class, listener);
  }

  /**
   * Registers a listener notified whenever the cross-page selection changes.
   */
  public void addSelectionChangeListener(
      ComponentEventListener<SelectionChangeEvent> listener) {
    addListener(SelectionChangeEvent.class, listener);
  }

  private static TextField createSearchField() {
    TextField field = new TextField();
    field.setSuffixComponent(VaadinIcon.SEARCH.create());
    field.setClearButtonVisible(true);
    field.setValueChangeMode(ValueChangeMode.LAZY);
    return field;
  }

  /**
   * Fired after a page has been loaded.
   */
  public static class PageLoadedEvent extends ComponentEvent<PaginatedGrid<?>> {

    private final int page;
    private final long total;

    public PageLoadedEvent(PaginatedGrid<?> source, int page, long total) {
      super(source, false);
      this.page = page;
      this.total = total;
    }

    public int getPage() {
      return page;
    }

    public long getTotal() {
      return total;
    }
  }

  /**
   * Fired whenever the cross-page selection changes.
   */
  public static class SelectionChangeEvent extends ComponentEvent<PaginatedGrid<?>> {

    private final Set<String> selectedIds;

    public SelectionChangeEvent(PaginatedGrid<?> source, Set<String> selectedIds) {
      super(source, false);
      this.selectedIds = Set.copyOf(selectedIds);
    }

    public Set<String> getSelectedIds() {
      return selectedIds;
    }
  }
}