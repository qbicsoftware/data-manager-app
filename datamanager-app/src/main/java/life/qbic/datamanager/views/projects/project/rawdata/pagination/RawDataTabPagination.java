package life.qbic.datamanager.views.projects.project.rawdata.pagination;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
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
import life.qbic.datamanager.views.general.pagination.Selection;

/**
 * A paginated, tabbed raw dataset list container (FEAT-PAG-LIST-04, USER-R-01/-R-02/-R-03).
 *
 * <p>Hosts three tabs (NGS / PxP / IP), each rendering its current page as an in-memory grid,
 * a single shared {@link PaginationBar} that reflects the active tab's page, and a shared
 * selection display with a "Clear selection" affordance. The component is a pure view
 * coordinator: it never queries data itself — the owning view wires a
 * {@link RefreshRequestedEvent} listener that performs the lookup and reports back via
 * {@link #onPageLoaded(RawDataDomain, int, long)}.</p>
 *
 * <p>The container owns the list state ({@link RawDataListState}) and mirrors it into the
 * browser URL (USER-R-03): {@code pushState} for page/page-size/sort changes,
 * {@code replaceState} for debounced search input and tab switches. The route base path is
 * injected by the route view via {@link #setBasePath(String)} so the container can build
 * complete locations; the route view additionally owns the History-API handler plumbing
 * (beforeEnter/beforeLeave) and calls {@link #applyExternalState(RawDataListState)} when the
 * browser history changes (back/forward, shared links).</p>
 *
 * @since 1.19.0
 */
public class RawDataTabPagination extends Div {

  @Serial
  private static final long serialVersionUID = 1L;

  // Vaadin: Tabs is meant for in-place navigation without a self-scrolling panel; TabSheet
  // wraps panels in overflow:auto hosts (its documented 'scrollable panels' purpose), which
  // traps position:sticky. We use the plain Tabs strip + our own content area (sized to
  // content, page-scrolls), so the sticky selection bar works without fighting the platform.
  private final Tabs tabs = new Tabs();
  private final Div contentArea = new Div();
  private final Map<RawDataDomain, Component> contentByDomain =
      new EnumMap<>(RawDataDomain.class);
  private final PaginationBar paginationBar =
      new PaginationBar(ListStateCodec.ALLOWED_PAGE_SIZES,
          RawDataListStateDefaults.DEFAULT_PAGE_SIZE, "datasets");
  private final Icon selectionIcon = VaadinIcon.CHECK_SQUARE_O.create();
  private final Span selectionDisplay = new Span();
  private final Button clearSelectionButton = new Button("Clear selection");
  private final Button selectAllResultsButton = new Button();
  private final Div selectionContainer = new Div();
  private final Map<RawDataDomain, Tab> tabsByDomain = new EnumMap<>(RawDataDomain.class);
  private RawDataListState listState = RawDataListState.defaultWith(RawDataDomain.NGS);
  private Selection selection;
  private String basePath;
  private boolean suppressTabSwitchEvents;
  private long totalItemsActive;

  public RawDataTabPagination() {
    addClassName("rawdata-tab-pagination");
    tabs.addClassName("rawdata-tabs");
    contentArea.addClassName("rawdata-content-area");
    configureSelectionBar();
    add(tabs, contentArea, paginationBar);
    configurePagination();
    configureTabSwitching();
  }

  /**
   * Sets the route base path the container mirrors into the URL (e.g.
   * {@code projects/PROJECT-1/experiments/E-1/rawdata}).
   */
  public void setBasePath(String basePath) {
    this.basePath = Objects.requireNonNull(basePath, "basePath must not be null");
  }

  private void configureSelectionBar() {
    selectionIcon.addClassName("rawdata-selection-icon");
    selectionDisplay.addClassName("rawdata-selection-count");
    clearSelectionButton.addClassName("rawdata-clear-selection");
    clearSelectionButton.addClickListener(event -> clearSelection());
    // Gmail-style inline context action: select every result matching the active filter
    selectAllResultsButton.addClassName("rawdata-select-all-results");
    selectAllResultsButton.addClickListener(event ->
        fireEvent(new SelectAllResultsRequestedEvent(this, activeTab())));
    selectionContainer.addClassName("rawdata-selection-bar");
    selectionContainer.add(selectionIcon, selectionDisplay, selectAllResultsButton,
        clearSelectionButton);
    selectionContainer.setVisible(false);
  }

  private void configureTabSwitching() {
    tabs.addSelectedChangeListener(event -> {
      Tab selected = event.getSelectedTab();
      if (selected == null) {
        return;
      }
      // show only the selected tab's content; keep the selection bar under its search row
      domainOf(selected).ifPresent(this::showOnly);
      // The selection bar is a single shared component that is re-parented into the active
      // tab's content; on every tab switch it must follow into the newly selected tab, or it
      // stays inside the previous (hidden) tab's content and becomes invisible.
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

  private void showOnly(RawDataDomain domain) {
    for (Map.Entry<RawDataDomain, Component> entry : contentByDomain.entrySet()) {
      entry.getValue().setVisible(entry.getKey() == domain);
    }
  }

  private Optional<RawDataDomain> domainOf(Tab tab) {
    for (Map.Entry<RawDataDomain, Tab> entry : tabsByDomain.entrySet()) {
      if (entry.getValue() == tab) {
        return Optional.of(entry.getKey());
      }
    }
    return Optional.empty();
  }

  /**
   * Registers a tab with its grid content. The tab label is provided by the caller.
   */
  public void addTab(String label, RawDataDomain domain, Component content) {
    Tab tab = new Tab(label);
    tabs.add(tab);
    contentByDomain.put(domain, content);
    tabsByDomain.put(domain, tab);
    contentArea.add(content);
  }

  /**
   * Places the selection bar below the search/toolbar row of the currently active tab.
   * Call once after all tabs have been added.
   */
  public void attachSelectionBar() {
    Tab tab = tabs.getSelectedTab();
    if (tab == null && !tabsByDomain.isEmpty()) {
      // before the Tabs are attached the selection is not initialised; fall back to the
      // first registered tab
      tab = tabsByDomain.entrySet().iterator().next().getValue();
    }
    attachSelectionBarTo(tab);
    // no selection-change event fires on initial mount; ensure only the active tab's
    // content is visible so the inactive grids do not stack below the page
    domainOf(tab).ifPresent(this::showOnly);
  }

  /**
   * Re-parents the shared selection bar into the given tab's content so it is shown and
   * sticks below that tab's search/toolbar row.
   */
  private void attachSelectionBarTo(Tab tab) {
    domainOf(tab).ifPresent(domain -> {
      Component content = contentByDomain.get(domain);
      if (content == null) {
        return;
      }
      Element contentElement = content.getElement();
      selectionContainer.getElement().removeFromParent();
      // insert after the toolbar row (index 0), i.e. directly under the search row
      int insertAt = Math.min(1, contentElement.getChildCount());
      contentElement.insertChild(insertAt, selectionContainer.getElement());
    });
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
  public void setTabLabel(RawDataDomain domain, String label) {
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
      // pager-driven change: the pager sits below the grid, so after paging the viewport is
      // left at the bottom; ask the view to bring the grid top back into view.
      fireEvent(new RefreshRequestedEvent(this, listState.activeTab(), true));
    });
  }

  /**
   * Applies a search term to the given tab: resets to page 1 and replaces the URL entry
   * (debounced search must not spam history).
   */
  public void applySearch(RawDataDomain domain, String searchTerm) {
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
  public void applySort(RawDataDomain domain, SortOrder sortOrder) {
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
   * Fired when the user asks to extend the selection to every dataset matching the active tab's
   * filter (cross-page). The owning view performs the backend lookup.
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
   * @param totalItems the total number of datasets matching the tab's active filter
   */
  public void onPageLoaded(RawDataDomain domain, int page, long totalItems) {
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
  public void applyExternalState(RawDataListState state) {
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
    if (tab == null) {
      return;
    }
    if (tabs.getSelectedTab() != tab) {
      tabs.setSelectedTab(tab);
    } else {
      // already selected (e.g. external state with same tab): no event fires, so make
      // sure the visibility reflects the desired active tab anyway
      showOnly(listState.activeTab());
    }
    // The selection bar is a single shared component re-parented into the active tab's
    // content. Cover the programmatic paths (initial/URL state application, visibility
    // fallback) that don't fire a selection-change event.
    attachSelectionBarTo(tab);
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
  public RawDataListState listState() {
    return listState;
  }

  public RawDataDomain activeTab() {
    return listState.activeTab();
  }

  /**
   * Programmatically switches the active tab (without writing a new history entry).
   */
  public void setActiveTab(RawDataDomain domain) {
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
   * Hides/shows a tab (an experiment without raw datasets of a domain hides its tab).
   * <p>If the currently active tab is being hidden, the selection is automatically moved to the
   * first remaining visible tab (in registration order NGS → PxP → IP), so the view never ends
   * up showing an empty hidden tab while other tabs contain datasets.</p>
   */
  public void setTabVisible(RawDataDomain domain, boolean visible) {
    Tab tab = tabsByDomain.get(domain);
    if (tab != null) {
      tab.setVisible(visible);
    }
    if (!visible && domain == listState.activeTab()) {
      firstVisibleDomain().ifPresent(this::setActiveTab);
    }
  }

  private Optional<RawDataDomain> firstVisibleDomain() {
    return tabsByDomain.entrySet().stream()
        .filter(entry -> entry.getValue().isVisible())
        .map(Map.Entry::getKey)
        .findFirst();
  }

  private void writeUrl(boolean push) {
    if (basePath == null) {
      return;
    }
    UI ui = UI.getCurrent();
    if (ui == null) {
      return;
    }
    Location location =
        new Location(basePath, RawDataListStateCodec.toQueryParameters(listState));
    if (push) {
      ui.getPage().getHistory().pushState(null, location);
    } else {
      ui.getPage().getHistory().replaceState(null, location);
    }
  }

  // ---- selection -----------------------------------------------------------

  /**
   * Attaches the selection whose count is displayed. The owning view switches this on tab
   * changes; the container reacts to {@link Selection} mutations via
   * {@link #updateSelectionBar()}.
   */
  public void setSelection(Selection selection) {
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
    // UX: scope disambiguation — when the selection covers the whole filtered result set,
    // say so explicitly instead of showing a bare count next to "Page 1 of N".
    if (count > 0 && count == totalItemsActive) {
      selectionDisplay.setText(count == 1
          ? "The single dataset matching the filter is selected"
          : "All %d datasets matching the filter are selected".formatted(count));
      selectAllResultsButton.setVisible(false);
      return;
    }
    selectionDisplay.setText(count == 1
        ? "1 dataset is selected"
        : "%d datasets are selected".formatted(count));
    // Gmail-style inline context action: offer to extend a partial selection to every result
    // matching the active filter (cross-page), only while the selection is still partial.
    boolean offerSelectAll = count > 0 && count < totalItemsActive;
    selectAllResultsButton.setVisible(offerSelectAll);
    if (offerSelectAll) {
      selectAllResultsButton.setText(
          "Select all %d matching datasets".formatted(totalItemsActive));
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

  /** Fired when the user asks to select every dataset matching the active filter. */
  public static class SelectAllResultsRequestedEvent extends
      com.vaadin.flow.component.ComponentEvent<RawDataTabPagination> {

    @Serial
    private static final long serialVersionUID = 1L;

    private final RawDataDomain domain;

    public SelectAllResultsRequestedEvent(RawDataTabPagination source,
        RawDataDomain domain) {
      super(source, false);
      this.domain = Objects.requireNonNull(domain);
    }

    public RawDataDomain domain() {
      return domain;
    }
  }

  /** Fired when the user clears the selection via the "Clear selection" affordance. */
  public static class SelectionClearedEvent extends
      com.vaadin.flow.component.ComponentEvent<RawDataTabPagination> {

    @Serial
    private static final long serialVersionUID = 1L;

    public SelectionClearedEvent(RawDataTabPagination source) {
      super(source, false);
    }
  }

  /** Fired when the active tab needs to be re-rendered (initial load, back/forward, paging, search, sort, tab switch). */
  public static class RefreshRequestedEvent extends
      com.vaadin.flow.component.ComponentEvent<RawDataTabPagination> {

    @Serial
    private static final long serialVersionUID = 1L;
    private final RawDataDomain domain;
    private final boolean scrollGridTopIntoView;

    public RefreshRequestedEvent(RawDataTabPagination source, RawDataDomain domain) {
      this(source, domain, false);
    }

    /**
     * @param scrollGridTopIntoView whether the owning view should scroll the rendered grid's
     *                              top into viewport after the refetch. Only pager-driven
     *                              page/page-size changes request this (the pager sits below
     *                              the grid, so after paging the user is left at the bottom);
     *                              search/sort/tab actions are triggered at the top and must
     *                              not yank the viewport.
     */
    public RefreshRequestedEvent(RawDataTabPagination source, RawDataDomain domain,
        boolean scrollGridTopIntoView) {
      super(source, false);
      this.domain = Objects.requireNonNull(domain);
      this.scrollGridTopIntoView = scrollGridTopIntoView;
    }

    public RawDataDomain domain() {
      return domain;
    }

    public boolean scrollGridTopIntoView() {
      return scrollGridTopIntoView;
    }
  }
}