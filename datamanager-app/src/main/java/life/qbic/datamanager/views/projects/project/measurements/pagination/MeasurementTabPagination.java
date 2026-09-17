package life.qbic.datamanager.views.projects.project.measurements.pagination;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.Location;
import java.io.Serial;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import life.qbic.application.commons.SortOrder;
import life.qbic.datamanager.views.general.pagination.ListState;
import life.qbic.datamanager.views.general.pagination.ListStateCodec;
import life.qbic.datamanager.views.general.pagination.PaginationBar;

/**
 * A paginated, tabbed measurement list container (ADR-0008, USER-R-01/-R-02/-R-03).
 *
 * <p>Hosts three tabs (NGS / PxP / IP), each rendering its current page as an in-memory grid,
 * a single shared {@link PaginationBar} that reflects the active tab's page, and a shared
 * selection display with a "Clear selection" affordance. The component is a pure view
 * coordinator: it never queries data itself — the owning view wires a
 * {@link RefreshRequestedEvent} listener that performs the lookup and reports back via
 * {@link #onPageLoaded(MeasurementDomain, int, long)}.</p>
 *
 * <p>The container owns the list state ({@link MeasurementListState}) and mirrors it into the
 * browser URL (ADR-0008, USER-R-03): {@code pushState} for page/page-size/sort changes,
 * {@code replaceState} for debounced search input and tab switches. The route base path is
 * injected by the route view via {@link #setBasePath(String)} so the container can build
 * complete locations; the route view additionally owns the History-API handler plumbing
 * (beforeEnter/beforeLeave) and calls {@link #applyExternalState(MeasurementListState)} when the
 * browser history changes (back/forward, shared links).</p>
 *
 * @since 1.12.0
 */
public class MeasurementTabPagination extends Div {

  @Serial
  private static final long serialVersionUID = 1L;

  private final TabSheet tabSheet = new TabSheet();
  private final PaginationBar paginationBar =
      new PaginationBar(ListStateCodec.ALLOWED_PAGE_SIZES, ListStateCodec.DEFAULT_PAGE_SIZE,
          "measurements");
  private final Icon selectionIcon = VaadinIcon.CHECK_SQUARE_O.create();
  private final Span selectionDisplay = new Span();
  private final Button clearSelectionButton = new Button("Clear selection");
  private final Button selectAllResultsButton = new Button();
  private final Div selectionContainer = new Div();
  private final Map<MeasurementDomain, Tab> tabsByDomain = new EnumMap<>(MeasurementDomain.class);
  private MeasurementListState listState = MeasurementListState.defaultWith(MeasurementDomain.NGS);
  private MeasurementSelection selection;
  private String basePath;
  private boolean suppressTabSwitchEvents;
  private long totalItemsActive;


  public MeasurementTabPagination() {
    addClassName("measurement-tab-pagination");
    tabSheet.addClassName("measurement-tab-sheet");
    configureSelectionBar();
    // selection bar sits above the grid (sticky top) so its information and actions are at
    // the user's eye level when working from the toolbar actions
    add(tabSheet, paginationBar);
    configurePagination();
    configureTabSwitching();
  }

  /**
   * Sets the route base path the container mirrors into the URL (e.g.
   * {@code projects/PROJECT-1/experiments/E-1/measurements}).
   */
  public void setBasePath(String basePath) {
    this.basePath = Objects.requireNonNull(basePath, "basePath must not be null");
  }

  private void configureSelectionBar() {
    selectionIcon.addClassName("measurement-selection-icon");
    selectionDisplay.addClassName("measurement-selection-count");
    clearSelectionButton.addClassName("measurement-clear-selection");
    clearSelectionButton.addClickListener(event -> clearSelection());
    // Gmail-style inline context action: select every result matching the active filter
    selectAllResultsButton.addClassName("measurement-select-all-results");
    selectAllResultsButton.addClickListener(event ->
        fireEvent(new SelectAllResultsRequestedEvent(this, activeTab())));
    selectionContainer.addClassName("measurement-selection-bar");
    selectionContainer.add(selectionIcon, selectionDisplay, selectAllResultsButton,
        clearSelectionButton);
    selectionContainer.setVisible(false);
  }



  private void configureTabSwitching() {
    tabSheet.addSelectedChangeListener(event -> {
      Tab selected = event.getSelectedTab();
      if (selected == null) {
        return;
      }
      // keep the selection bar attached to the selected tab's content (under its search row)
      attachSelectionBarTo(selected);
      if (suppressTabSwitchEvents) {
        return;
      }
      domainOf(selected).ifPresent(newTab -> {
        if (newTab == listState.activeTab()) {
          return;
        }
        listState = listState.withTab(newTab, listState.stateOf(newTab));
        writeUrl(false);
        fireEvent(new RefreshRequestedEvent(this, newTab));
      });
    });
  }

  private Optional<MeasurementDomain> domainOf(Tab tab) {
    for (Map.Entry<MeasurementDomain, Tab> entry : tabsByDomain.entrySet()) {
      if (entry.getValue() == tab) {
        return Optional.of(entry.getKey());
      }
    }
    return Optional.empty();
  }

  /**
   * Registers a tab with its grid content. The tab label is provided by the caller.
   */
  public void addTab(String label, MeasurementDomain domain, Component content) {
    tabsByDomain.put(domain, tabSheet.add(label, content));
  }

  /**
   * Places the selection bar below the search/toolbar row of the currently active tab.
   * Call once after all tabs have been added.
   */
  public void attachSelectionBar() {
    Tab tab = tabSheet.getSelectedTab();
    if (tab == null && !tabsByDomain.isEmpty()) {
      // before the TabSheet is attached the selection is not initialised; fall back to
      // the first registered tab
      tab = tabsByDomain.entrySet().iterator().next().getValue();
    }
    attachSelectionBarTo(tab);
  }

  /**
   * Pins the selection bar below the app navbar while it would otherwise scroll out of view.
   * Uses a JS fallback (position: fixed with a runtime-measured navbar offset) because the
   * Vaadin TabSheet shadow DOM traps position: sticky; CSS sticky remains as enhancement.


  /**
   * Re-parents the shared selection bar into the given tab's content so it is shown and
   * sticks below that tab's search/toolbar row (F2: visible + eye level, minimal travel).
   */
  private void attachSelectionBarTo(Tab tab) {
    if (tab == null) {
      return;
    }
    Component content = tabSheet.getComponent(tab);
    if (content == null) {
      return;
    }
    Element contentElement = content.getElement();
    selectionContainer.getElement().removeFromParent();
    // insert after the toolbar row (index 0), i.e. directly under the search row
    int insertAt = Math.min(1, contentElement.getChildCount());
    contentElement.insertChild(insertAt, selectionContainer.getElement());
  }

  /**
   * The selection bar component (count, Clear, Gmail-style select-all). It is re-parented
   * into the active tab's content; exposed for the owning view if further wiring is needed.
   */
  public Div selectionBar() {
    return selectionContainer;
  }

  /**
   * Updates a tab's label (e.g. appending a count badge). No-op if the domain was not added.
   */
  public void setTabLabel(MeasurementDomain domain, String label) {
    Tab tab = tabsByDomain.get(domain);
    if (tab != null) {
      tab.setLabel(label);
    }
  }

  private void configurePagination() {
    paginationBar.addChangeListener(event -> {
      ListState current = listState.activeState();
      ListState requested;
      if (event.getPageSize() != current.pageSize()) {
        requested = current.withPageSize(event.getPageSize());
      } else if (event.getPage() != current.page()) {
        requested = current.withPage(event.getPage());
      } else {
        return;
      }
      if (requested.equals(current)) {
        return;
      }
      listState = listState.withState(listState.activeTab(), requested);
      writeUrl(true);
      fireEvent(new RefreshRequestedEvent(this, listState.activeTab()));
    });
  }

  /**
   * Applies a search term to the given tab: resets to page 1 and replaces the URL entry
   * (debounced search must not spam history).
   */
  public void applySearch(MeasurementDomain domain, String searchTerm) {
    ListState current = listState.stateOf(domain);
    String term = searchTerm == null ? "" : searchTerm.trim();
    if (term.equals(current.filter())) {
      return;
    }
    listState = listState.withState(domain, current.withFilter(term).withPage(1));
    writeUrl(false);
    fireEvent(new RefreshRequestedEvent(this, domain));
  }

  /**
   * Applies a sort order to the given tab: resets to page 1 and pushes a new history entry.
   */
  public void applySort(MeasurementDomain domain, SortOrder sortOrder) {
    ListState current = listState.stateOf(domain);
    Objects.requireNonNull(sortOrder, "sortOrder must not be null");
    if (sortOrder.equals(current.sort())) {
      return;
    }
    listState = listState.withState(domain, current.withSort(sortOrder).withPage(1));
    writeUrl(true);
    fireEvent(new RefreshRequestedEvent(this, domain));
  }

  public void addRefreshRequestedListener(ComponentEventListener<RefreshRequestedEvent> listener) {
    addListener(RefreshRequestedEvent.class, listener);
  }

  /**
   * Fired when the user asks to extend the selection to every measurement matching the
   * active tab's filter (cross-page). The owning view performs the backend lookup.
   */
  public void addSelectAllResultsListener(
      ComponentEventListener<SelectAllResultsRequestedEvent> listener) {
    addListener(SelectAllResultsRequestedEvent.class, listener);
  }

  /**
   * Reports the loaded page of a tab back to the container so the pager and the total can be
   * updated.
   *
   * @param domain     the tab that was loaded
   * @param page       the (possibly clamped) page that was rendered, 1-based
   * @param totalItems the total number of measurements matching the tab's active filter
   */
  public void onPageLoaded(MeasurementDomain domain, int page, long totalItems) {
    ListState current = listState.stateOf(domain);
    this.listState = listState.withState(domain, current.withPage(page));
    if (domain == activeTab()) {
      this.totalItemsActive = totalItems;
      paginationBar.setListState(page, totalItems, listState.activeState().pageSize());
      paginationBar.setVisible(totalItems > 0);
    }
    updateSelectionBar();
  }

  /**
   * Applies an externally provided state (initial load, URL back/forward, shared link) and asks
   * the owning view to re-fetch the active tab's page.
   */
  public void applyExternalState(MeasurementListState state) {
    this.listState = state;
    suppressTabSwitchEvents = true;
    try {
      selectTabForActiveDomain();
    } finally {
      suppressTabSwitchEvents = false;
    }
    refreshActiveTab();
    updateSelectionBar();
  }

  private void selectTabForActiveDomain() {
    Tab tab = tabsByDomain.get(listState.activeTab());
    if (tab != null && tabSheet.getSelectedTab() != tab) {
      tabSheet.setSelectedTab(tab);
    }
  }

  /**
   * Asks the owning view to re-fetch the currently active tab's page (used after a context
   * change and on initial/external state application).
   */
  public void refreshActiveTab() {
    fireEvent(new RefreshRequestedEvent(this, listState.activeTab()));
  }

  /**
   * The container's complete list state (active tab + per-tab states).
   */
  public MeasurementListState listState() {
    return listState;
  }

  /**
   * Applies a new list state for one domain without touching the URL (used when the rendered
   * page was clamped after a filter or deletion shrank the result set).
   */
  public void applyListState(MeasurementDomain domain, ListState newState) {
    this.listState = listState.withState(domain, newState);
  }

  public MeasurementDomain activeTab() {
    return listState.activeTab();
  }

  /**
   * Programmatically switches the active tab (without writing a new history entry).
   */
  public void setActiveTab(MeasurementDomain domain) {
    if (domain != listState.activeTab()) {
      listState = listState.withTab(domain, listState.stateOf(domain));
      suppressTabSwitchEvents = true;
      try {
        selectTabForActiveDomain();
      } finally {
        suppressTabSwitchEvents = false;
      }
      fireEvent(new RefreshRequestedEvent(this, domain));
    }
  }

  /**
   * Hides/shows a tab (an experiment without measurements of a domain hides its tab).
   */
  public void setTabVisible(MeasurementDomain domain, boolean visible) {
    Tab tab = tabsByDomain.get(domain);
    if (tab != null) {
      tab.setVisible(visible);
    }
  }

  private void writeUrl(boolean push) {
    if (basePath == null) {
      return;
    }
    UI ui = UI.getCurrent();
    if (ui == null) {
      return;
    }
    Location location = new Location(basePath, MeasurementListStateCodec.toQueryParameters(listState));
    if (push) {
      ui.getPage().getHistory().pushState(null, location);
    } else {
      ui.getPage().getHistory().replaceState(null, location);
    }
  }

  // ---- selection -----------------------------------------------------------

  /**
   * Attaches the selection whose count is displayed. The owning view switches this on tab
   * changes; the container reacts to {@link MeasurementSelection} mutations via
   * {@link #updateSelectionBar()}.
   */
  public void setSelection(MeasurementSelection selection) {
    this.selection = Objects.requireNonNull(selection, "selection must not be null");
    // a new selection scope is attached; do not compare it against the previous tab's total
    this.totalItemsActive = 0;
    updateSelectionBar();
  }

  /**
   * Re-renders the selection count from the attached selection.
   */
  public void updateSelectionBar() {
    int count = selection == null ? 0 : selection.count();
    selectionContainer.setVisible(count > 0);
    // UX F1: scope disambiguation — when the selection covers the whole filtered result set,
    // say so explicitly instead of showing a bare count next to "Page 1 of N".
    if (count > 0 && count == totalItemsActive) {
      selectionDisplay.setText(count == 1
          ? "The single measurement matching the filter is selected"
          : "All %d measurements matching the filter are selected".formatted(count));
      selectAllResultsButton.setVisible(false);
      return;
    }
    selectionDisplay.setText(count == 1
        ? "1 measurement is selected"
        : "%d measurements are selected".formatted(count));
    // Gmail-style inline context action: offer to extend a partial selection to every result
    // matching the active filter (cross-page), only while the selection is still partial.
    boolean offerSelectAll = count > 0 && count < totalItemsActive;
    selectAllResultsButton.setVisible(offerSelectAll);
    if (offerSelectAll) {
      selectAllResultsButton.setText(
          "Select all %d matching measurements".formatted(totalItemsActive));
    }
  }

  private void clearSelection() {
    if (selection != null) {
      selection.clear();
      // the grid rows are owned by the view; let it reconcile the newly empty selection
      fireEvent(new SelectionClearedEvent(this));
    }
  }

  /**
   * Fired after the user cleared the selection; the owning view reconciles its grids so
   * the row checkboxes reflect the empty selection.
   */
  public void addSelectionClearedListener(
      ComponentEventListener<SelectionClearedEvent> listener) {
    addListener(SelectionClearedEvent.class, listener);
  }

  /** Fired when the user asks to select every measurement matching the active filter. */
  public static class SelectAllResultsRequestedEvent extends
      com.vaadin.flow.component.ComponentEvent<MeasurementTabPagination> {

    @Serial
    private static final long serialVersionUID = 1L;

    private final MeasurementDomain domain;

    public SelectAllResultsRequestedEvent(MeasurementTabPagination source,
        MeasurementDomain domain) {
      super(source, false);
      this.domain = Objects.requireNonNull(domain);
    }

    public MeasurementDomain domain() {
      return domain;
    }
  }

  /** Fired when the user clears the selection via the "Clear selection" affordance. */
  public static class SelectionClearedEvent extends
      com.vaadin.flow.component.ComponentEvent<MeasurementTabPagination> {

    @Serial
    private static final long serialVersionUID = 1L;

    public SelectionClearedEvent(MeasurementTabPagination source) {
      super(source, false);
    }
  }

  /** Fired when the active tab needs to be re-rendered (initial load, back/forward, paging, search, sort, tab switch). */
  public static class RefreshRequestedEvent extends
      com.vaadin.flow.component.ComponentEvent<MeasurementTabPagination> {

    @Serial
    private static final long serialVersionUID = 1L;
    private final MeasurementDomain domain;

    public RefreshRequestedEvent(MeasurementTabPagination source, MeasurementDomain domain) {
      super(source, false);
      this.domain = Objects.requireNonNull(domain);
    }

    public MeasurementDomain domain() {
      return domain;
    }
  }
}