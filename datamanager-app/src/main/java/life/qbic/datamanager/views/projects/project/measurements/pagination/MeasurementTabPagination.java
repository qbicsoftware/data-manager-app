package life.qbic.datamanager.views.projects.project.measurements.pagination;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.tabs.TabSheet;
import java.io.Serial;
import java.util.Objects;
import life.qbic.datamanager.views.general.pagination.ListState;
import life.qbic.datamanager.views.general.pagination.ListStateCodec;
import life.qbic.datamanager.views.general.pagination.PaginationBar;

/**
 * A paginated, tabbed measurement list container (ADR-0008, USER-R-01/-R-02/-R-03).
 *
 * <p>Hosts three tabs (NGS / PxP / IP), each rendering its current page as an in-memory grid,
 * a single shared {@link PaginationBar} that reflects the active tab's page, and a shared
 * selection display with a "Clear selection" affordance. The component is a pure view
 * coordinator: it never queries data itself — the owning view wires {@link PageChangeListener}
 * and {@link RefreshListener} callbacks that perform the lookup and report back via
 * {@link #onPageLoaded(MeasurementDomain, int, long)}.</p>
 *
 * <p>The tab sheet is rendered statically (all three tabs always present) so per-tab grid/state
 * survives tab switching without re-rendering; tab visibility is controlled by the owning view
 * through {@link #setTabVisible(MeasurementDomain, boolean)} (an experiment without measurements
 * of a domain hides that tab).</p>
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
  private final Span selectionDisplay = new Span();
  private final Button clearSelectionButton = new Button("Clear selection");
  private final Div selectionContainer = new Div();
  private MeasurementListState listState = MeasurementListState.defaultWith(MeasurementDomain.NGS);
  private int ngsTotal;
  private int pxpTotal;
  private int ipTotal;

  public MeasurementTabPagination() {
    addClassName("measurement-tab-pagination");
    tabSheet.addClassName("measurement-tab-sheet");
    configureSelectionBar();
    add(tabSheet, selectionContainer, paginationBar);
    configurePagination();
  }

  private void configureSelectionBar() {
    selectionDisplay.addClassName("measurement-selection-count");
    clearSelectionButton.addClassName("measurement-clear-selection");
    clearSelectionButton.addClickListener(event -> clearSelection());
    selectionContainer.addClassName("measurement-selection-bar");
    selectionContainer.add(selectionDisplay, clearSelectionButton);
    selectionContainer.setVisible(false);
  }

  private void configurePagination() {
    paginationBar.addChangeListener(this::onPaginationChanged);
  }

  private void onPaginationChanged(PaginationBar.ChangeEvent event) {
    ListState current = listState.activeState();
    ListState requested;
    if (event.getPageSize() != current.pageSize()) {
      requested = current.withPageSize(event.getPageSize());
    } else if (event.getPage() != current.page()) {
      requested = current.withPage(event.getPage());
    } else {
      return;
    }
    changeListState(requested);
  }

  /**
   * Requests a page/page-size change for the active tab and forwards it to the owning view.
   */
  private void changeListState(ListState requested) {
    ListState previous = listState.activeState();
    if (previous.equals(requested)) {
      return;
    }
    listState = listState.withState(listState.activeTab(), requested);
    fireEvent(new PageChangeEvent(this, true, requested));
  }

  /**
   * Registers the listener notified whenever the user requests a page/page-size change on the
   * active tab.
   */
  public void addPageChangeListener(ComponentEventListener<PageChangeEvent> listener) {
    addListener(PageChangeEvent.class, listener);
  }

  public void fireRefreshRequested(MeasurementDomain domain) {
    listState = listState.withTab(domain, listState.stateOf(domain));
    fireEvent(new RefreshRequestedEvent(this, domain));
  }

  public void addRefreshRequestedListener(ComponentEventListener<RefreshRequestedEvent> listener) {
    addListener(RefreshRequestedEvent.class, listener);
  }

  /**
   * Reports the loaded page of a tab back to the container so the pager and the total can be
   * updated.
   */
  public void onPageLoaded(MeasurementDomain domain, int page, long totalItems) {
    this.listState = listState.withState(domain, listState.stateOf(domain).withPage(page));
    switch (domain) {
      case NGS -> ngsTotal = (int) totalItems;
      case PXP -> pxpTotal = (int) totalItems;
      case IP -> ipTotal = (int) totalItems;
    }
    if (domain == activeTab()) {
      paginationBar.setListState(page, totalItems, listState.activeState().pageSize());
      paginationBar.setVisible(totalItems > 0);
    }
    updateSelectionBar();
  }

  /**
   * Applies an externally provided state (initial load, URL back/forward, shared link).
   */
  public void applyExternalState(MeasurementListState state) {
    this.listState = state;
    refreshActiveTab();
    updateSelectionBar();
  }

  /**
   * The active tab's current list state.
   */
  public MeasurementListState listState() {
    return listState;
  }

  public MeasurementDomain activeTab() {
    return listState.activeTab();
  }

  public void setActiveTab(MeasurementDomain domain) {
    if (domain != listState.activeTab()) {
      listState = listState.withTab(domain, listState.stateOf(domain));
      refreshActiveTab();
    }
  }

  /**
   * Hides/shows a tab (an experiment without measurements of a domain hides its tab).
   */
  public void setTabVisible(MeasurementDomain domain, boolean visible) {
    String label = switch (domain) {
      case NGS -> "Genomics";
      case PXP -> "Proteomics";
      case IP -> "Immunopeptidomics";
    };
    for (int i = 0; i < tabSheet.getTabCount(); i++) {
      com.vaadin.flow.component.tabs.Tab tab = tabSheet.getTabAt(i);
      if (label.equals(tab.getLabel())) {
        tab.setVisible(visible);
      }
    }
  }

  // ---- internal helpers -----------------------------------------------------

  private void refreshActiveTab() {
    fireEvent(new RefreshRequestedEvent(this, activeTab()));
  }

  private void updateSelectionBar() {
    selectionContainer.setVisible(selectionCount() > 0);
    selectionDisplay.setText(selectionCount() == 1
        ? "1 measurement is selected"
        : "%d measurements are selected".formatted(selectionCount()));
  }

  private int selectionCount() {
    return getSelection() != null ? getSelection().count() : 0;
  }

  // The owning view sets the selection via setSelection; this component only displays its count.
  private MeasurementSelection selection;

  public void setSelection(MeasurementSelection selection) {
    this.selection = Objects.requireNonNull(selection);
  }

  private MeasurementSelection getSelection() {
    return selection;
  }

  private void clearSelection() {
    if (selection != null) {
      selection.clear();
    }
  }

  /** Fired when the user requests a page/page-size change on the active tab. */
  public static class PageChangeEvent extends com.vaadin.flow.component.ComponentEvent<MeasurementTabPagination> {

    @Serial
    private static final long serialVersionUID = 1L;
    private final ListState requested;

    public PageChangeEvent(MeasurementTabPagination source, boolean fromClient, ListState requested) {
      super(source, fromClient);
      this.requested = Objects.requireNonNull(requested);
    }

    public ListState requested() {
      return requested;
    }
  }

  /** Fired when the active tab needs to be re-rendered (initial load, back/forward, tab switch). */
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