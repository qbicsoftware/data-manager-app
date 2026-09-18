package life.qbic.datamanager.views.projects.project.measurements;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.checkbox.CheckboxGroupVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.MultiSortPriority;
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.component.grid.GridMultiSelectionModel;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.selection.MultiSelectionEvent;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.shared.Registration;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.SortOrder;
import life.qbic.application.commons.time.DateTimeFormat;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.datamanager.views.general.dialog.AppDialog;
import life.qbic.datamanager.views.general.dialog.DialogBody;
import life.qbic.datamanager.views.general.dialog.DialogFooter;
import life.qbic.datamanager.views.general.dialog.DialogHeader;
import life.qbic.datamanager.views.general.dialog.DialogSection;
import life.qbic.datamanager.views.general.pagination.ListState;
import life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory;
import life.qbic.datamanager.views.projects.project.measurements.pagination.MeasurementDomain;
import life.qbic.datamanager.views.projects.project.measurements.pagination.MeasurementListState;
import life.qbic.datamanager.views.projects.project.measurements.pagination.MeasurementSelection;
import life.qbic.datamanager.views.projects.project.measurements.pagination.MeasurementSort;
import life.qbic.datamanager.views.projects.project.measurements.pagination.MeasurementTabPagination;
import life.qbic.datamanager.views.projects.project.measurements.pagination.MeasurementTabPagination.RefreshRequestedEvent;
import life.qbic.projectmanagement.application.measurement.IpMeasurementLookup;
import life.qbic.projectmanagement.application.measurement.NgsMeasurementLookup;
import life.qbic.projectmanagement.application.measurement.NgsMeasurementLookup.NgsSortKey;
import life.qbic.projectmanagement.application.measurement.PxpMeasurementLookup;
import life.qbic.projectmanagement.application.measurement.PxpMeasurementLookup.MeasurementInfo;
import life.qbic.projectmanagement.application.measurement.PxpMeasurementLookup.PxpSortKey;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Sort;

/**
 * A component to show detailed information about existing measurements within an experiment
 * (ADR-0008, USER-R-01/-R-02/-R-03).
 *
 * <p>The three measurement domains (genomics / proteomics / immunopeptidomics) are shown as
 * paginated in-memory grids inside a {@link MeasurementTabPagination}: only the current page is
 * fetched and rendered, a shared pager reports location and total, and a view-owned
 * {@link MeasurementSelection} of measurement IDs survives page, filter, and sort changes.
 * Bulk actions (export / edit / delete) apply to the full cross-page selection. The "Select all
 * N matching the active filter" action resolves all matching measurement IDs in backend storage
 * (ADR-0008, A1).</p>
 *
 * <p>List state (active tab + per-tab page/size/filter/sort) is owned by the container
 * ({@link MeasurementTabPagination}) and mirrored into the browser URL; this component only
 * performs the lookups, keeps the selections, and fires the same measurement events as before to
 * {@link MeasurementMain}.</p>
 */
public class MeasurementDetailsComponent extends PageArea implements Serializable {

  private static final NumberFormat INJECTION_VOLUME_FORMAT = new DecimalFormat("#.##");
  private static final DateTimeFormat MEASUREMENT_REGISTRATION_DATE_TIME_FORMAT = DateTimeFormat.ISO_LOCAL_DATE_TIME_WHITESPACE_SEPARATED;

  private final AtomicReference<String> clientTimeZone = new AtomicReference<>("UTC");
  private final AtomicInteger clientTimeZoneOffset = new AtomicInteger(0);
  private final MessageSourceNotificationFactory messageFactory;

  private final MeasurementTabPagination tabPagination;
  private final Grid<NgsMeasurementLookup.MeasurementInfo> ngsGrid = createNgsGrid();
  private final Grid<MeasurementInfo> pxpGrid = createPxpGrid();
  private final Grid<IpMeasurementLookup.MeasurementInfo> ipGrid = createIpGrid();
  private final TextField ngsSearchField = searchField();
  private final TextField pxpSearchField = searchField();
  private final TextField ipSearchField = searchField();
  private final Button ngsEditButton = new Button("Edit");
  private final Button ngsDeleteButton = new Button("Delete");
  private final Button pxpEditButton = new Button("Edit");
  private final Button pxpDeleteButton = new Button("Delete");
  private final Button ipEditButton = new Button("Edit");
  private final Button ipDeleteButton = new Button("Delete");
  private final Map<MeasurementDomain, Button> exportButtons = new EnumMap<>(MeasurementDomain.class);
  private final Map<MeasurementDomain, Div> emptyStates = new EnumMap<>(MeasurementDomain.class);
  private final MeasurementSelection ngsSelection = new MeasurementSelection(() -> updateSelectionBar());
  private final MeasurementSelection pxpSelection = new MeasurementSelection(() -> updateSelectionBar());
  private final MeasurementSelection ipSelection = new MeasurementSelection(() -> updateSelectionBar());

  private final transient NgsMeasurementLookup ngsMeasurementLookup;
  private final transient PxpMeasurementLookup pxpMeasurementLookup;
  private final transient IpMeasurementLookup ipMeasurementLookup;

  private static final Map<MeasurementDomain, String> TAB_LABELS = Map.of(
      MeasurementDomain.NGS, "Genomics",
      MeasurementDomain.PXP, "Proteomics",
      MeasurementDomain.IP, "Immunopeptidomics");

  private Context context;

  private void updateSelectionBar() {
    if (tabPagination != null) {
      tabPagination.updateSelectionBar();
    }
    updateActionButtons(MeasurementDomain.NGS, ngsSelection, ngsEditButton, ngsDeleteButton);
    updateActionButtons(MeasurementDomain.PXP, pxpSelection, pxpEditButton, pxpDeleteButton);
    updateActionButtons(MeasurementDomain.IP, ipSelection, ipEditButton, ipDeleteButton);
  }

  // fail-closed: mutation actions hidden until the owning view confirms ACL write scope
  private boolean writeAccess = false;

  private void updateActionButtons(MeasurementDomain domain, MeasurementSelection selection,
      Button editButton, Button deleteButton) {
    editButton.setVisible(writeAccess);
    deleteButton.setVisible(writeAccess);
    boolean hasSelection = selection.count() > 0;
    editButton.setEnabled(hasSelection);
    deleteButton.setEnabled(hasSelection);
    Button exportButton = exportButtons.get(domain);
    if (exportButton != null) {
      exportButton.setEnabled(hasSelection);
    }
  }

  /**
   * Applies the caller's ACL scope: when {@code false}, Edit/Delete are hidden for users
   * with read-only project scope (they must never be offered mutation actions).
   */
  public void setWriteAccess(boolean writeAccess) {
    this.writeAccess = writeAccess;
    updateSelectionBar();
  }

  @Override
  protected void onAttach(AttachEvent attachEvent) {
    super.onAttach(attachEvent);
    attachEvent.getUI().getPage().getExtendedClientDetails().refresh(
        receiver -> {
          clientTimeZoneOffset.set(receiver.getTimezoneOffset());
          clientTimeZone.set(receiver.getTimeZoneId());
        });
  }

  private @NonNull String formatTime(Instant instant, DateTimeFormat dateTimeFormat) {
    return DateTimeFormat.asJavaFormatter(dateTimeFormat, ZoneId.of(clientTimeZone.get()))
        .format(instant);
  }

  public MeasurementDetailsComponent(
      MessageSourceNotificationFactory messageFactory,
      NgsMeasurementLookup ngsMeasurementLookup,
      PxpMeasurementLookup pxpMeasurementLookup,
      IpMeasurementLookup ipMeasurementLookup) {
    this.messageFactory = requireNonNull(messageFactory);
    this.ngsMeasurementLookup = requireNonNull(ngsMeasurementLookup);
    this.pxpMeasurementLookup = requireNonNull(pxpMeasurementLookup);
    this.ipMeasurementLookup = requireNonNull(ipMeasurementLookup);
    addClassNames("measurement-details-component", "width-full");

    tabPagination = new MeasurementTabPagination();
    tabPagination.addTab(TAB_LABELS.get(MeasurementDomain.NGS), MeasurementDomain.NGS,
        ngsTabContent());
    tabPagination.addTab(TAB_LABELS.get(MeasurementDomain.PXP), MeasurementDomain.PXP,
        pxpTabContent());
    tabPagination.addTab(TAB_LABELS.get(MeasurementDomain.IP), MeasurementDomain.IP,
        ipTabContent());
    // UX F2: selection bar lives under the search/toolbar row of the active tab
    tabPagination.attachSelectionBar();
    tabPagination.setSelection(ngsSelection);
    tabPagination.addRefreshRequestedListener(this::onRefreshRequested);
    tabPagination.addSelectionClearedListener(event -> reconcileAllGrids());
    tabPagination.addSelectAllResultsListener(
        event -> selectAllMatching(event.domain()));
    add(tabPagination);

    configureSearch(ngsSearchField, MeasurementDomain.NGS);
    configureSearch(pxpSearchField, MeasurementDomain.PXP);
    configureSearch(ipSearchField, MeasurementDomain.IP);
    configureSortListener(ngsGrid, MeasurementDomain.NGS);
    configureSortListener(pxpGrid, MeasurementDomain.PXP);
    configureSortListener(ipGrid, MeasurementDomain.IP);
    configureSelectionReconciliation(ngsGrid, ngsSelection, MeasurementDomain.NGS);
    configureSelectionReconciliation(pxpGrid, pxpSelection, MeasurementDomain.PXP);
    configureSelectionReconciliation(ipGrid, ipSelection, MeasurementDomain.IP);

    // UX F4: bulk actions start disabled — no selection exists yet
    updateSelectionBar();

    // register buttons wiring (fires the same events as the old implementation)
    ngsEditButton.addClickListener(clicked -> fireEditRequested(MeasurementDomain.NGS,
        ngsSelection, NgsMeasurementEditRequested::new));
    ngsDeleteButton.addClickListener(clicked -> fireDeletionRequested(MeasurementDomain.NGS,
        ngsSelection, NgsMeasurementDeletionRequested::new));
    pxpEditButton.addClickListener(clicked -> fireEditRequested(MeasurementDomain.PXP,
        pxpSelection, PxpMeasurementEditRequested::new));
    pxpDeleteButton.addClickListener(clicked -> fireDeletionRequested(MeasurementDomain.PXP,
        pxpSelection, PxpMeasurementDeletionRequested::new));
    ipEditButton.addClickListener(clicked -> fireEditRequested(MeasurementDomain.IP,
        ipSelection, IpMeasurementEditRequested::new));
    ipDeleteButton.addClickListener(clicked -> fireDeletionRequested(MeasurementDomain.IP,
        ipSelection, IpMeasurementDeletionRequested::new));
  }

  // ---- tab content ---------------------------------------------------------

  private Component ngsTabContent() {
    return tabContent(ngsGrid, ngsSearchField, ngsEditButton, ngsDeleteButton,
        MeasurementDomain.NGS, this::exportNgs);
  }

  private Component pxpTabContent() {
    return tabContent(pxpGrid, pxpSearchField, pxpEditButton, pxpDeleteButton,
        MeasurementDomain.PXP, this::exportPxp);
  }

  private Component ipTabContent() {
    return tabContent(ipGrid, ipSearchField, ipEditButton, ipDeleteButton,
        MeasurementDomain.IP, this::exportIp);
  }

  private <T> Component tabContent(Grid<T> grid, TextField searchField, Button editButton,
      Button deleteButton, MeasurementDomain domain, Runnable exporter) {
    Div toolbar = new Div();
    toolbar.addClassName("measurement-tab-toolbar");
    searchField.addClassName("measurement-search");
    Button exportButton = new Button("Export", VaadinIcon.DOWNLOAD.create());
    exportButton.addClassName("measurement-export");
    // UX F5: visual hierarchy — export is the primary bulk action, delete is destructive
    exportButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    editButton.setIcon(VaadinIcon.EDIT.create());
    deleteButton.setIcon(VaadinIcon.TRASH.create());
    deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
    exportButtons.put(domain, exportButton);
    exportButton.addClickListener(clicked -> exporter.run());
    toolbar.add(searchField, exportButton, editButton, deleteButton);

    Div toolbarRight = new Div();
    toolbarRight.addClassName("measurement-tab-toolbar-right");
    // Show/Hide Columns: a per-tab menu over the grid's columns. Kept from the original
    // FilterGrid implementation — one of the most-used features for table-heavy users.
    toolbarRight.add(showHideColumnsMenu(grid));
    toolbar.add(toolbarRight);

    // UX F8: explicit empty-state message instead of a blank grid region
    Div emptyState = new Div();
    emptyState.addClassName("measurement-empty-state");
    emptyState.setVisible(false);
    emptyStates.put(domain, emptyState);

    Div content = new Div();
    content.addClassName("measurement-tab-content");
    content.add(toolbar, emptyState, grid);
    return content;
  }

  /**
   * Builds a "Show/Hide Columns" menu bar over the given grid's columns.
   * <p>
   * Each grid column is represented by a checkbox (checked = visible) labelled with its header
   * text. Toggling a checkbox shows/hides the column immediately. This replicates the widely
   * used control from the previous {@code FilterGrid}-based measurement view.
   */
  private static <T> MenuBar showHideColumnsMenu(Grid<T> grid) {
    MenuBar menuBar = new MenuBar();
    // UX F5: chevron suffix signals "opens a menu", distinguishing it from action buttons
    Span itemContent = new Span(new Span("Show/Hide Columns"),
        VaadinIcon.CHEVRON_DOWN.create());
    var menuItem = menuBar.addItem(itemContent);
    var subMenu = menuItem.getSubMenu();

    CheckboxGroup<Column<T>> checkboxGroup = new CheckboxGroup<>();
    checkboxGroup.setItemLabelGenerator(Column::getHeaderText);
    List<Column<T>> columns = showableColumns(grid);
    checkboxGroup.setItems(columns);
    checkboxGroup.setValue(columns.stream()
        .filter(Column::isVisible)
        .collect(Collectors.toSet()));
    checkboxGroup.addThemeVariants(CheckboxGroupVariant.LUMO_VERTICAL);
    checkboxGroup.addClassNames("flex-vertical");
    // prevent the menu-bar from handling the click (which would close the submenu)
    checkboxGroup.getElement().executeJs(
        "this.addEventListener('click', e => e.stopPropagation());");
    checkboxGroup.addValueChangeListener(event -> {
      Set<Column<T>> selected = event.getValue();
      for (Column<T> column : columns) {
        column.setVisible(selected.contains(column));
      }
    });
    subMenu.addComponent(checkboxGroup);
    return menuBar;
  }

  /**
   * The columns offered in the Show/Hide Columns menu: every column with a header label.
   * The multi-select checkbox column is a {@link Column} too but has no header text; without
   * this filter it would render as an empty first entry in the menu.
   */
  static <T> List<Column<T>> showableColumns(Grid<T> grid) {
    return grid.getColumns().stream()
        .filter(column -> column.getHeaderText() != null && !column.getHeaderText().isBlank())
        .toList();
  }

  private void configureSearch(TextField field, MeasurementDomain domain) {
    field.setPlaceholder("Search Measurements");
    field.setClearButtonVisible(true);
    field.setValueChangeMode(ValueChangeMode.LAZY);
    field.addValueChangeListener(event -> tabPagination.applySearch(domain, event.getValue()));
  }

  private void configureSortListener(Grid<?> grid, MeasurementDomain domain) {
    grid.setMultiSort(false);
    grid.addSortListener(event -> {
      List<GridSortOrder<?>> orders = (List<GridSortOrder<?>>) (List<?>) grid.getSortOrder();
      if (orders.isEmpty()) {
        return;
      }
      GridSortOrder<?> order = orders.get(0);
      String property = sortPropertyOf(order);
      if (property == null || property.isBlank()) {
        return;
      }
      boolean descending = order.getDirection() == SortDirection.DESCENDING;
      SortOrder sortOrder = new SortOrder(property, descending);
      if (!MeasurementSort.allowedSortOrders(domain).contains(sortOrder)) {
        return;
      }
      tabPagination.applySort(domain, sortOrder);
    });
  }

  private static String sortPropertyOf(GridSortOrder<?> order) {
    if (order.getSorted() == null) {
      return null;
    }
    Column<?> column = (Column<?>) order.getSorted();
    // The Column exposes its sort properties only via the sort-order provider; the first
    // QuerySortOrder returned for the clicked direction carries the property name.
    return column.getSortOrder(order.getDirection())
        .findFirst()
        .map(querySortOrder -> querySortOrder.getSorted())
        .orElse(null);
  }

  private void configureSelectionReconciliation(Grid<?> grid, MeasurementSelection selection,
      MeasurementDomain domain) {
    grid.setSelectionMode(Grid.SelectionMode.MULTI);
    // Freeze the selection (checkbox) column so the user always sees the selection state when
    // scrolling horizontally (same as the frozen QBiC Measurement ID column).
    if (grid.getSelectionModel() instanceof GridMultiSelectionModel<?> multiSelectionModel) {
      multiSelectionModel.setSelectionColumnFrozen(true);
    }
    // client changes -> update the ID set (only for rows on the current page)
    @SuppressWarnings("unchecked")
    Grid<Object> objectGrid = (Grid<Object>) grid;
    objectGrid.addSelectionListener(event -> {
      MultiSelectionEvent<Grid<Object>, Object> multi = (MultiSelectionEvent<Grid<Object>, Object>) event;
      Set<Object> added = multi.getAddedSelection();
      Set<Object> removed = multi.getRemovedSelection();
      added.forEach(item -> selection.select(measurementIdOf(domain, item)));
      removed.forEach(item -> selection.deselect(measurementIdOf(domain, item)));
      tabPagination.updateSelectionBar();
    });
  }

  private static String measurementIdOf(MeasurementDomain domain, Object item) {
    return switch (domain) {
      case NGS -> ((NgsMeasurementLookup.MeasurementInfo) item).measurementId();
      case PXP -> ((MeasurementInfo) item).measurementId();
      case IP -> ((IpMeasurementLookup.MeasurementInfo) item).measurementId();
    };
  }

  // ---- page loading / refresh -----------------------------------------------

  private void onRefreshRequested(RefreshRequestedEvent event) {
    MeasurementDomain domain = event.domain();
    if (context == null) {
      return;
    }
    updateTabCounts();
    ListState state = tabPagination.listState().stateOf(domain);
    // keep the selection display attached to the active tab
    tabPagination.setSelection(selectionFor(domain));
    syncSearchField(domain, state.filter());
    loadAndRender(domain, state, event.scrollGridTopIntoView());
  }

  // UX F9: per-tab totals as label badges so users see which domains hold measurements
  // without switching tabs (unfiltered counts; filter state is per-tab and would churn labels).
  private void updateTabCounts() {
    String experimentId = context.experimentId().orElseThrow().value();
    String projectId = context.projectId().orElseThrow().value();
    for (MeasurementDomain domain : MeasurementDomain.values()) {
      long total = switch (domain) {
        case NGS -> ngsMeasurementLookup.countNgsMeasurements(projectId,
            NgsMeasurementLookup.MeasurementFilter.forExperiment(experimentId));
        case PXP -> pxpMeasurementLookup.countPxpMeasurements(projectId,
            PxpMeasurementLookup.MeasurementFilter.forExperiment(experimentId));
        case IP -> ipMeasurementLookup.countIpMeasurements(projectId,
            IpMeasurementLookup.MeasurementFilter.forExperiment(experimentId));
      };
      tabPagination.setTabLabel(domain, "%s (%d)".formatted(TAB_LABELS.get(domain), total));
    }
  }

  private void syncSearchField(MeasurementDomain domain, String filter) {
    TextField field = switch (domain) {
      case NGS -> ngsSearchField;
      case PXP -> pxpSearchField;
      case IP -> ipSearchField;
    };
    if (!Objects.equals(field.getValue(), filter)) {
      field.setValue(filter == null ? "" : filter);
    }
  }

  private void loadAndRender(MeasurementDomain domain, ListState state,
      boolean scrollGridTopIntoView) {
    String experimentId = context.experimentId().orElseThrow().value();
    String projectId = context.projectId().orElseThrow().value();
    Sort sort = MeasurementSort.toSpringDataSort(state.sort(), domain);
    int offset = (state.page() - 1) * state.pageSize();
    int limit = state.pageSize();
    String searchTerm = state.filter();
    switch (domain) {
      case NGS -> {
        NgsMeasurementLookup.MeasurementFilter filter =
            NgsMeasurementLookup.MeasurementFilter.forExperiment(experimentId)
                .withSearch(searchTerm, clientTimeZoneOffset.get(),
                    MEASUREMENT_REGISTRATION_DATE_TIME_FORMAT);
        int total = ngsMeasurementLookup.countNgsMeasurements(projectId, filter);
        List<NgsMeasurementLookup.MeasurementInfo> page = ngsMeasurementLookup
            .lookupNgsMeasurements(projectId, offset, limit, sort, filter).toList();
        renderPage(ngsGrid, page, total, state, MeasurementDomain.NGS, scrollGridTopIntoView);
      }
      case PXP -> {
        PxpMeasurementLookup.MeasurementFilter filter =
            PxpMeasurementLookup.MeasurementFilter.forExperiment(experimentId)
                .withSearch(searchTerm, clientTimeZoneOffset.get(),
                    MEASUREMENT_REGISTRATION_DATE_TIME_FORMAT);
        int total = pxpMeasurementLookup.countPxpMeasurements(projectId, filter);
        List<MeasurementInfo> page = pxpMeasurementLookup
            .lookupPxpMeasurements(projectId, offset, limit, sort, filter).toList();
        renderPage(pxpGrid, page, total, state, MeasurementDomain.PXP, scrollGridTopIntoView);
      }
      case IP -> {
        IpMeasurementLookup.MeasurementFilter filter =
            IpMeasurementLookup.MeasurementFilter.forExperiment(experimentId)
                .withSearch(searchTerm, clientTimeZoneOffset.get(),
                    MEASUREMENT_REGISTRATION_DATE_TIME_FORMAT);
        int total = ipMeasurementLookup.countIpMeasurements(projectId, filter);
        List<IpMeasurementLookup.MeasurementInfo> page = ipMeasurementLookup
            .lookupIpMeasurements(projectId, offset, limit, sort, filter).toList();
        renderPage(ipGrid, page, total, state, MeasurementDomain.IP, scrollGridTopIntoView);
      }
    }
  }

  private <T> void renderPage(Grid<T> grid, List<T> page, int total, ListState state,
      MeasurementDomain domain, boolean scrollGridTopIntoView) {
    int totalPages = Math.max(1, (int) Math.ceil((double) total / state.pageSize()));
    int pageToRender = Math.min(state.page(), totalPages);
    if (pageToRender != state.page()) {
      // the requested page lies beyond the last valid page (filter/deletion shrank the result
      // set); re-fetch the clamped page once
      refreshClamped(domain, pageToRender, scrollGridTopIntoView);
      return;
    }
    grid.setItems(page);
    // With explicit pagination the grid shows exactly the current page; render all its rows at
    // their natural height (no internal scroll container) so the whole configured page size is
    // visible and the native page scroll handles overflow.
    grid.setAllRowsVisible(true);
    // UX F3: only for pager-driven page/page-size changes (the pager sits below the grid, so
    // after paging the viewport is left at the bottom) bring the grid's top back into view.
    // Search/sort/tab actions are triggered at the top and must not yank the viewport.
    if (scrollGridTopIntoView) {
      grid.getElement().executeJs(
          "requestAnimationFrame(() => this.scrollIntoView({block: 'start'}))");
    }
    // UX F8: render an explicit empty state distinguishing "nothing registered" from
    // "filter matched nothing" instead of leaving a blank grid region.
    Div emptyState = emptyStates.get(domain);
    boolean noResults = total == 0;
    emptyState.setVisible(noResults);
    grid.setVisible(!noResults);
    if (noResults) {
      String filter = state.filter();
      boolean hasFilter = filter != null && !filter.isBlank();
      emptyState.setText(hasFilter
          ? "No measurements match '" + filter + "'. Clear the search to see all measurements."
          : "No measurements registered yet.");
    }
    reconcileSelection(grid, selectionFor(domain), domain);
    tabPagination.onPageLoaded(domain, pageToRender, total);
  }


  private void refreshClamped(MeasurementDomain domain, int clampedPage,
      boolean scrollGridTopIntoView) {
    ListState current = tabPagination.listState().stateOf(domain);
    ListState clamped = current.withPage(clampedPage);
    tabPagination.applyListState(domain, clamped);
    loadAndRender(domain, clamped, scrollGridTopIntoView);
  }

  @SuppressWarnings("unchecked")
  private <T> void reconcileSelection(Grid<T> grid, MeasurementSelection selection,
      MeasurementDomain domain) {
    grid.getGenericDataView().getItems().toList().forEach(item -> {
      String id = measurementIdOf(domain, item);
      if (selection.contains(id)) {
        grid.select(item);
      } else {
        grid.deselect(item);
      }
    });
  }

  /**
   * Reconciles every grid's visible rows against its tab selection (used after the user
   * cleared the selection so row checkboxes reflect the empty selection).
   */
  private void reconcileAllGrids() {
    reconcileSelection(ngsGrid, ngsSelection, MeasurementDomain.NGS);
    reconcileSelection(pxpGrid, pxpSelection, MeasurementDomain.PXP);
    reconcileSelection(ipGrid, ipSelection, MeasurementDomain.IP);
  }

  // ---- selection helpers ----------------------------------------------------

  private MeasurementSelection selectionFor(MeasurementDomain domain) {
    return switch (domain) {
      case NGS -> ngsSelection;
      case PXP -> pxpSelection;
      case IP -> ipSelection;
    };
  }

  private void selectAllMatching(MeasurementDomain domain) {
    if (context == null) {
      return;
    }
    ListState state = tabPagination.listState().stateOf(domain);
    String projectId = context.projectId().orElseThrow().value();
    String experimentId = context.experimentId().orElseThrow().value();
    String searchTerm = state.filter();
    Sort sort = MeasurementSort.toSpringDataSort(state.sort(), domain);
    List<String> ids = switch (domain) {
      case NGS -> {
        NgsMeasurementLookup.MeasurementFilter filter =
            NgsMeasurementLookup.MeasurementFilter.forExperiment(experimentId)
                .withSearch(searchTerm, clientTimeZoneOffset.get(),
                    MEASUREMENT_REGISTRATION_DATE_TIME_FORMAT);
        int total = ngsMeasurementLookup.countNgsMeasurements(projectId, filter);
        yield ngsMeasurementLookup.lookupNgsMeasurements(projectId, 0, total, sort, filter)
            .map(NgsMeasurementLookup.MeasurementInfo::measurementId).toList();
      }
      case PXP -> {
        PxpMeasurementLookup.MeasurementFilter filter =
            PxpMeasurementLookup.MeasurementFilter.forExperiment(experimentId)
                .withSearch(searchTerm, clientTimeZoneOffset.get(),
                    MEASUREMENT_REGISTRATION_DATE_TIME_FORMAT);
        int total = pxpMeasurementLookup.countPxpMeasurements(projectId, filter);
        yield pxpMeasurementLookup.lookupPxpMeasurements(projectId, 0, total, sort, filter)
            .map(PxpMeasurementLookup.MeasurementInfo::measurementId).toList();
      }
      case IP -> {
        IpMeasurementLookup.MeasurementFilter filter =
            IpMeasurementLookup.MeasurementFilter.forExperiment(experimentId)
                .withSearch(searchTerm, clientTimeZoneOffset.get(),
                    MEASUREMENT_REGISTRATION_DATE_TIME_FORMAT);
        int total = ipMeasurementLookup.countIpMeasurements(projectId, filter);
        yield ipMeasurementLookup.lookupIpMeasurements(projectId, 0, total, sort, filter)
            .map(IpMeasurementLookup.MeasurementInfo::measurementId).toList();
      }
    };
    selectionFor(domain).select(Set.copyOf(ids));
    // visually reconcile only the current page; the rest stay selected 'invisibly'
    Grid<?> grid = switch (domain) {
      case NGS -> ngsGrid;
      case PXP -> pxpGrid;
      case IP -> ipGrid;
    };
    reconcileSelection(grid, selectionFor(domain), domain);
    tabPagination.updateSelectionBar();
  }

  // ---- actions --------------------------------------------------------------

  private interface IdEventFactory<T extends ComponentEvent<MeasurementDetailsComponent>> {
    T create(List<String> ids, MeasurementDetailsComponent source, boolean fromClient);
  }

  private void fireEditRequested(MeasurementDomain domain, MeasurementSelection selection,
      IdEventFactory<?> factory) {
    List<String> ids = idsOf(selection);
    if (ids.isEmpty()) {
      displayMissingSelectionNote();
      return;
    }
    switch (domain) {
      case NGS -> fireEvent((NgsMeasurementEditRequested) factory.create(ids, this, true));
      case PXP -> fireEvent((PxpMeasurementEditRequested) factory.create(ids, this, true));
      case IP -> fireEvent((IpMeasurementEditRequested) factory.create(ids, this, true));
    }
  }

  private void fireDeletionRequested(MeasurementDomain domain, MeasurementSelection selection,
      IdEventFactory<?> factory) {
    List<String> ids = idsOf(selection);
    if (ids.isEmpty()) {
      displayMissingSelectionNote();
      return;
    }
    switch (domain) {
      case NGS -> fireEvent((NgsMeasurementDeletionRequested) factory.create(ids, this, true));
      case PXP -> fireEvent((PxpMeasurementDeletionRequested) factory.create(ids, this, true));
      case IP -> fireEvent((IpMeasurementDeletionRequested) factory.create(ids, this, true));
    }
  }

  private static List<String> idsOf(MeasurementSelection selection) {
    List<String> ids = new ArrayList<>(selection.selectedIds());
    return ids;
  }

  private void exportNgs() {
    List<String> ids = idsOf(ngsSelection);
    if (ids.isEmpty()) {
      displayMissingSelectionNote();
      return;
    }
    fireEvent(new NgsMeasurementExportRequested(ids, this, true));
  }

  private void exportPxp() {
    List<String> ids = idsOf(pxpSelection);
    if (ids.isEmpty()) {
      displayMissingSelectionNote();
      return;
    }
    fireEvent(new PxpMeasurementExportRequested(ids, this, true));
  }

  private void exportIp() {
    List<String> ids = idsOf(ipSelection);
    if (ids.isEmpty()) {
      displayMissingSelectionNote();
      return;
    }
    fireEvent(new IpMeasurementExportRequested(ids, this, true));
  }

  /**
   * Removes the given measurement IDs from the corresponding domain selection (after successful
   * deletion).
   */
  public void removeFromSelection(MeasurementDomain domain, Set<String> ids) {
    selectionFor(domain).deselect(ids);
    tabPagination.updateSelectionBar();
  }

  // ---- context --------------------------------------------------------------

  /**
   * Sets the context of the component. The tab layout is built once; changing the context only
   * re-fetches the active tab's page. When the experiment changes, per-tab state is reset and the
   * tabs are re-hidden/showed according to whether measurements exist.
   */
  public void setContext(Context context) {
    validateContext(context);
    boolean sameExperiment = this.context != null
        && this.context.experimentId().isPresent()
        && context.experimentId().isPresent()
        && this.context.experimentId().get().equals(context.experimentId().get())
        && this.context.projectId().isPresent()
        && context.projectId().isPresent()
        && this.context.projectId().get().equals(context.projectId().get());
    this.context = context;
    if (!sameExperiment) {
      // reset per-tab state to defaults on a new experiment
      MeasurementListState defaults = MeasurementListState.defaultWith(MeasurementDomain.NGS);
      tabPagination.applyExternalState(defaults);
    } else {
      tabPagination.refreshActiveTab();
    }
    refreshTabVisibility();
  }

  private void refreshTabVisibility() {
    String projectId = context.projectId().orElseThrow().value();
    String experimentId = context.experimentId().orElseThrow().value();
    tabPagination.setTabVisible(MeasurementDomain.NGS,
        ngsMeasurementLookup.countNgsMeasurements(projectId,
            NgsMeasurementLookup.MeasurementFilter.forExperiment(experimentId)) > 0);
    tabPagination.setTabVisible(MeasurementDomain.PXP,
        pxpMeasurementLookup.countPxpMeasurements(projectId,
            PxpMeasurementLookup.MeasurementFilter.forExperiment(experimentId)) > 0);
    tabPagination.setTabVisible(MeasurementDomain.IP,
        ipMeasurementLookup.countIpMeasurements(projectId,
            IpMeasurementLookup.MeasurementFilter.forExperiment(experimentId)) > 0);
  }

  /**
   * Refreshes the genomics grid (re-fetches the current page).
   */
  public void refreshNgs() {
    refreshDomain(MeasurementDomain.NGS);
  }

  /**
   * Refreshes the proteomics grid (re-fetches the current page).
   */
  public void refreshPxp() {
    refreshDomain(MeasurementDomain.PXP);
  }

  /**
   * Refreshes the immunopeptidomics grid (re-fetches the current page).
   */
  public void refreshIp() {
    refreshDomain(MeasurementDomain.IP);
  }

  private void refreshDomain(MeasurementDomain domain) {
    if (context != null) {
      // deletion/registration reloads keep the current viewport; no forced scroll
      loadAndRender(domain, tabPagination.listState().stateOf(domain), false);
      refreshTabVisibility();
    }
  }

  /**
   * @return the pagination container, so the route view can drive URL parsing/history.
   */
  public MeasurementTabPagination getTabPagination() {
    return tabPagination;
  }

  /**
   * Test seam: exposes the grids so specs can pin column configuration (comparators, sort keys)
   * without a running UI. Package-private, not part of the public API.
   */
  Grid<NgsMeasurementLookup.MeasurementInfo> ngsGrid() {
    return ngsGrid;
  }

  Grid<MeasurementInfo> pxpGrid() {
    return pxpGrid;
  }

  Grid<IpMeasurementLookup.MeasurementInfo> ipGrid() {
    return ipGrid;
  }

  /**
   * Delegates the route base path to the container so it can mirror the list state into the URL.
   */
  public void setBasePath(String basePath) {
    tabPagination.setBasePath(basePath);
  }

  private static TextField searchField() {
    TextField field = new TextField();
    field.setSuffixComponent(VaadinIcon.SEARCH.create());
    // UX F6: placeholder-only inputs are inaccessible; provide a programmatic label
    field.getElement().setAttribute("aria-label", "Search measurements");
    return field;
  }

  // ==== the following are carried over unchanged from the previous implementation ====

  private Grid<NgsMeasurementLookup.MeasurementInfo> createNgsGrid() {
    var ngsGrid = new Grid<NgsMeasurementLookup.MeasurementInfo>();
    ngsGrid.setMultiSort(true, MultiSortPriority.APPEND, true);
    var measurementIdColumn = ngsGrid.addColumn(
            NgsMeasurementLookup.MeasurementInfo::measurementCode)
        .setHeader("QBiC Measurement ID")
        .setSortProperty(NgsSortKey.MEASUREMENT_ID.sortKey())
        .setComparator(Comparator.comparing(NgsMeasurementLookup.MeasurementInfo::measurementCode))
        .setAutoWidth(true)
        .setResizable(true)
        .setFrozen(true);
    ngsGrid.addColumn(NgsMeasurementLookup.MeasurementInfo::measurementName)
        .setHeader("Measurement Name")
        .setSortProperty(NgsSortKey.MEASUREMENT_NAME.sortKey())
        .setComparator(Comparator.comparing(NgsMeasurementLookup.MeasurementInfo::measurementName,
            Comparator.nullsLast(String::compareTo)))
        .setAutoWidth(true)
        .setResizable(true);
    ngsGrid.addComponentColumn(measurementInfo -> renderSamplesNgs(measurementInfo,
            info -> "%s (%s)".formatted(info.sampleLabel(), info.sampleCode())))
        .setHeader("Samples")
        .setSortable(false)
        .setAutoWidth(true)
        .setResizable(true);
    ngsGrid.addColumn(NgsMeasurementLookup.MeasurementInfo::facility)
        .setHeader("Facility")
        .setSortProperty(NgsSortKey.FACILITY.sortKey())
        .setComparator(Comparator.comparing(NgsMeasurementLookup.MeasurementInfo::facility))
        .setAutoWidth(true)
        .setResizable(true);
    ngsGrid.addComponentColumn(
            info -> renderInstrument(info.instrument().label(),
                info.instrument().oboId(),
                info.instrument().iri()))
        .setHeader("Instrument")
        .setSortable(false)
        .setAutoWidth(true)
        .setResizable(true);
    ngsGrid.addComponentColumn(
            info -> renderOrganisation(info.organisation().label(),
                info.organisation().iri()))
        .setHeader("Organisation")
        .setSortable(false)
        .setAutoWidth(true)
        .setResizable(true);

    ngsGrid.addColumn(NgsMeasurementLookup.MeasurementInfo::readType)
        .setHeader("Read type")
        .setSortProperty(NgsSortKey.READ_TYPE.sortKey())
        .setComparator(Comparator.comparing(NgsMeasurementLookup.MeasurementInfo::readType))
        .setAutoWidth(true)
        .setResizable(true);
    ngsGrid.addColumn(NgsMeasurementLookup.MeasurementInfo::libraryKit)
        .setHeader("Library kit")
        .setSortProperty(NgsSortKey.LIBRARY_KIT.sortKey())
        .setComparator(Comparator.comparing(NgsMeasurementLookup.MeasurementInfo::libraryKit))
        .setAutoWidth(true)
        .setResizable(true);
    ngsGrid.addColumn(NgsMeasurementLookup.MeasurementInfo::flowCell)
        .setHeader("Flow cell")
        .setSortProperty(NgsSortKey.FLOW_CELL.sortKey())
        .setComparator(Comparator.comparing(NgsMeasurementLookup.MeasurementInfo::flowCell))
        .setAutoWidth(true)
        .setResizable(true);
    ngsGrid.addColumn(NgsMeasurementLookup.MeasurementInfo::runProtocol)
        .setHeader("Run protocol")
        .setKey(NgsSortKey.RUN_PROTOCOL.sortKey())
        .setComparator(Comparator.comparing(NgsMeasurementLookup.MeasurementInfo::runProtocol))
        .setAutoWidth(true)
        .setResizable(true);
    ngsGrid.addColumn(info -> formatTime(info.registeredAt(),
            MEASUREMENT_REGISTRATION_DATE_TIME_FORMAT))
        .setHeader("Registration Date")
        .setKey(NgsSortKey.REGISTRATION_DATE.sortKey())
        .setComparator(Comparator.comparing(NgsMeasurementLookup.MeasurementInfo::registeredAt))
        .setAutoWidth(true)
        .setResizable(true);
    ngsGrid.addComponentColumn(
            (NgsMeasurementLookup.MeasurementInfo measurementInfo) -> renderSamplesNgs(measurementInfo,
                info -> info.comment()))
        .setHeader("Comment")
        .setSortable(false)
        .setAutoWidth(true)
        .setResizable(true);
    ngsGrid.sort(GridSortOrder.asc(measurementIdColumn).build());
    return ngsGrid;
  }

  private Grid<PxpMeasurementLookup.MeasurementInfo> createPxpGrid() {
    var pxpGrid = new Grid<PxpMeasurementLookup.MeasurementInfo>();
    pxpGrid.setMultiSort(true, MultiSortPriority.APPEND, true);
    var measurementIdColumn = pxpGrid.addColumn(
            PxpMeasurementLookup.MeasurementInfo::measurementCode)
        .setHeader("QBiC Measurement ID")
        .setSortProperty(PxpSortKey.MEASUREMENT_ID.sortKey())
        .setComparator(Comparator.comparing(PxpMeasurementLookup.MeasurementInfo::measurementCode))
        .setAutoWidth(true)
        .setResizable(true)
        .setFrozen(true);
    pxpGrid.addColumn(PxpMeasurementLookup.MeasurementInfo::measurementName)
        .setHeader("Measurement Name")
        .setSortProperty(PxpSortKey.MEASUREMENT_NAME.sortKey())
        .setComparator(Comparator.comparing(PxpMeasurementLookup.MeasurementInfo::measurementName,
            Comparator.nullsLast(String::compareTo)))
        .setAutoWidth(true)
        .setResizable(true);
    pxpGrid.addComponentColumn(measurementInfo -> renderSamplesPxp(measurementInfo,
            info -> "%s (%s)".formatted(info.sampleLabel(), info.sampleCode())))
        .setHeader("Samples")
        .setSortable(false)
        .setAutoWidth(true)
        .setResizable(true);
    pxpGrid.addComponentColumn(
            info -> renderOrganisation(info.organisation().label(),
                info.organisation().iri()))
        .setHeader("Organisation")
        .setSortable(false)
        .setAutoWidth(true)
        .setResizable(true);
    pxpGrid.addColumn(PxpMeasurementLookup.MeasurementInfo::facility)
        .setHeader("Facility")
        .setSortProperty(PxpSortKey.FACILITY.sortKey())
        .setComparator(Comparator.comparing(PxpMeasurementLookup.MeasurementInfo::facility))
        .setAutoWidth(true)
        .setResizable(true);
    pxpGrid.addComponentColumn(
            info -> renderMsDevice(info.msDevice().label(),
                info.msDevice().oboId(),
                info.msDevice().iri()))
        .setHeader("MS Device")
        .setSortable(false)
        .setAutoWidth(true)
        .setResizable(true);
    pxpGrid.addColumn(info -> info.technicalReplicateName())
        .setHeader("Technical Replicate")
        .setKey(PxpSortKey.TECHNICAL_REPLICATE.sortKey())
        .setComparator(
            Comparator.comparing(PxpMeasurementLookup.MeasurementInfo::technicalReplicateName))
        .setAutoWidth(true)
        .setResizable(true);
    pxpGrid.addColumn(PxpMeasurementLookup.MeasurementInfo::digestionEnzyme)
        .setHeader("Digestion Enzyme")
        .setKey(PxpSortKey.DIGESTION_ENZYME.sortKey())
        .setComparator(Comparator.comparing(PxpMeasurementLookup.MeasurementInfo::digestionEnzyme))
        .setAutoWidth(true)
        .setResizable(true);
    pxpGrid.addColumn(info -> info.digestionMethod())
        .setHeader("Digestion Method")
        .setKey(PxpSortKey.DIGESTION_METHOD.sortKey())
        .setComparator(Comparator.comparing(PxpMeasurementLookup.MeasurementInfo::digestionMethod))
        .setAutoWidth(true)
        .setResizable(true);
    pxpGrid.addColumn(info -> INJECTION_VOLUME_FORMAT.format(info.injectionVolume()))
        .setHeader("Injection Volume")
        .setKey(PxpSortKey.INJECTION_VOLUME.sortKey())
        .setComparator(Comparator.comparing(PxpMeasurementLookup.MeasurementInfo::injectionVolume))
        .setAutoWidth(true)
        .setResizable(true);
    pxpGrid.addColumn(info -> info.lcmsMethod())
        .setHeader("LCMS")
        .setKey(PxpSortKey.LCMS_METHOD.sortKey())
        .setComparator(Comparator.comparing(PxpMeasurementLookup.MeasurementInfo::lcmsMethod))
        .setAutoWidth(true)
        .setResizable(true);
    pxpGrid.addColumn(info -> info.lcColumn())
        .setHeader("LC column")
        .setKey(PxpSortKey.LC_COLUMN.sortKey())
        .setComparator(Comparator.comparing(PxpMeasurementLookup.MeasurementInfo::lcColumn))
        .setAutoWidth(true)
        .setResizable(true);
    pxpGrid.addColumn(info -> info.enrichmentMethod())
        .setHeader("Enrichment")
        .setKey(PxpSortKey.ENRICHMENT_METHOD.sortKey())
        .setComparator(Comparator.comparing(PxpMeasurementLookup.MeasurementInfo::enrichmentMethod))
        .setAutoWidth(true)
        .setResizable(true);

    pxpGrid.addColumn(info -> formatTime(info.registeredAt(),
            MEASUREMENT_REGISTRATION_DATE_TIME_FORMAT))
        .setHeader("Registration Date")
        .setKey(PxpSortKey.REGISTRATION_DATE.sortKey())
        .setComparator(Comparator.comparing(PxpMeasurementLookup.MeasurementInfo::registeredAt))
        .setAutoWidth(true)
        .setResizable(true);
    pxpGrid.addComponentColumn(measurementInfo -> renderSamplesPxp(measurementInfo,
            PxpMeasurementLookup.SampleInfo::comment))
        .setHeader("Comment")
        .setSortable(false)
        .setAutoWidth(true)
        .setResizable(true);
    pxpGrid.sort(GridSortOrder.asc(measurementIdColumn).build());
    return pxpGrid;
  }

  private Grid<IpMeasurementLookup.MeasurementInfo> createIpGrid() {
    var ipGrid = new Grid<IpMeasurementLookup.MeasurementInfo>();
    ipGrid.setMultiSort(true, MultiSortPriority.APPEND, true);
    var measurementIdColumn = ipGrid.addColumn(IpMeasurementLookup.MeasurementInfo::measurementCode)
        .setHeader("QBiC Measurement ID")
        .setSortProperty(IpMeasurementLookup.IpSortKey.MEASUREMENT_ID.sortKey())
        .setComparator(
            Comparator.comparing(IpMeasurementLookup.MeasurementInfo::measurementCode))
        .setAutoWidth(true)
        .setResizable(true)
        .setFrozen(true);
    ipGrid.addColumn(IpMeasurementLookup.MeasurementInfo::measurementName)
        .setHeader("Measurement Name")
        .setSortProperty(IpMeasurementLookup.IpSortKey.MEASUREMENT_NAME.sortKey())
        .setComparator(Comparator.comparing(IpMeasurementLookup.MeasurementInfo::measurementName,
            Comparator.nullsLast(String::compareTo)))
        .setAutoWidth(true)
        .setResizable(true);
    ipGrid.addComponentColumn(measurementInfo -> renderSamplesIp(measurementInfo,
            info -> "%s (%s)".formatted(info.sampleLabel(), info.sampleCode())))
        .setHeader("Samples")
        .setSortable(false)
        .setAutoWidth(true)
        .setResizable(true);
    ipGrid.addComponentColumn(
            info -> renderOrganisation(info.organisation().label(),
                info.organisation().iri()))
        .setHeader("Organisation")
        .setSortable(false)
        .setAutoWidth(true)
        .setResizable(true);
    ipGrid.addColumn(IpMeasurementLookup.MeasurementInfo::facility)
        .setHeader("Facility")
        .setSortProperty(IpMeasurementLookup.IpSortKey.FACILITY.sortKey())
        .setComparator(Comparator.comparing(IpMeasurementLookup.MeasurementInfo::facility))
        .setAutoWidth(true)
        .setResizable(true);
    ipGrid.addComponentColumn(
            info -> renderInstrument(info.instrument().label(),
                info.instrument().oboId(),
                info.instrument().iri()))
        .setHeader("Instrument")
        .setSortable(false)
        .setAutoWidth(true)
        .setResizable(true);
    ipGrid.addColumn(IpMeasurementLookup.MeasurementInfo::mhcAntibody)
        .setHeader("MHC Antibody")
        .setSortProperty(IpMeasurementLookup.IpSortKey.MHC_ANTIBODY.sortKey())
        .setComparator(
            Comparator.comparing(IpMeasurementLookup.MeasurementInfo::mhcAntibody))
        .setAutoWidth(true)
        .setResizable(true);
    ipGrid.addColumn(IpMeasurementLookup.MeasurementInfo::mhcTypingMethod)
        .setHeader("MHC Typing Method")
        .setSortProperty(IpMeasurementLookup.IpSortKey.MHC_TYPING_METHOD.sortKey())
        .setComparator(
            Comparator.comparing(IpMeasurementLookup.MeasurementInfo::mhcTypingMethod))
        .setAutoWidth(true)
        .setResizable(true);
    ipGrid.addColumn(IpMeasurementLookup.MeasurementInfo::enrichmentMethod)
        .setHeader("Enrichment Method")
        .setSortProperty(IpMeasurementLookup.IpSortKey.ENRICHMENT_METHOD.sortKey())
        .setComparator(
            Comparator.comparing(IpMeasurementLookup.MeasurementInfo::enrichmentMethod))
        .setAutoWidth(true)
        .setResizable(true);
    ipGrid.addColumn(IpMeasurementLookup.MeasurementInfo::lcmsMethod)
        .setHeader("LCMS Method")
        .setSortProperty(IpMeasurementLookup.IpSortKey.LCMS_METHOD.sortKey())
        .setComparator(Comparator.comparing(IpMeasurementLookup.MeasurementInfo::lcmsMethod))
        .setAutoWidth(true)
        .setResizable(true);
    ipGrid.addColumn(IpMeasurementLookup.MeasurementInfo::lcColumn)
        .setHeader("LC Column")
        .setSortProperty(IpMeasurementLookup.IpSortKey.LC_COLUMN.sortKey())
        .setComparator(Comparator.comparing(IpMeasurementLookup.MeasurementInfo::lcColumn))
        .setAutoWidth(true)
        .setResizable(true);
    ipGrid.addColumn(IpMeasurementLookup.MeasurementInfo::dataAcquisition)
        .setHeader("Data Acquisition")
        .setSortProperty(IpMeasurementLookup.IpSortKey.DATA_ACQUISITION.sortKey())
        .setComparator(
            Comparator.comparing(IpMeasurementLookup.MeasurementInfo::dataAcquisition))
        .setAutoWidth(true)
        .setResizable(true);
    ipGrid.addColumn(IpMeasurementLookup.MeasurementInfo::massRange)
        .setHeader("Mass Range")
        .setSortProperty(IpMeasurementLookup.IpSortKey.MASS_RANGE.sortKey())
        .setComparator(Comparator.comparing(IpMeasurementLookup.MeasurementInfo::massRange))
        .setAutoWidth(true)
        .setResizable(true);
    ipGrid.addColumn(IpMeasurementLookup.MeasurementInfo::retentionTimeRange)
        .setHeader("Retention Time Range")
        .setSortProperty(IpMeasurementLookup.IpSortKey.RETENTION_TIME_RANGE.sortKey())
        .setComparator(
            Comparator.comparing(IpMeasurementLookup.MeasurementInfo::retentionTimeRange))
        .setAutoWidth(true)
        .setResizable(true);
    ipGrid.addColumn(IpMeasurementLookup.MeasurementInfo::chargeRange)
        .setHeader("Charge Range")
        .setSortProperty(IpMeasurementLookup.IpSortKey.CHARGE_RANGE.sortKey())
        .setComparator(Comparator.comparing(IpMeasurementLookup.MeasurementInfo::chargeRange))
        .setAutoWidth(true)
        .setResizable(true);
    ipGrid.addColumn(IpMeasurementLookup.MeasurementInfo::ionMobilityRange)
        .setHeader("Ion Mobility Range")
        .setSortProperty(IpMeasurementLookup.IpSortKey.ION_MOBILITY_RANGE.sortKey())
        .setComparator(
            Comparator.comparing(IpMeasurementLookup.MeasurementInfo::ionMobilityRange))
        .setAutoWidth(true)
        .setResizable(true);
    ipGrid.addColumn(IpMeasurementLookup.MeasurementInfo::sampleMass)
        .setHeader("Sample Mass (mg)")
        .setSortProperty(IpMeasurementLookup.IpSortKey.SAMPLE_MASS.sortKey())
        .setComparator(
            Comparator.comparing(IpMeasurementLookup.MeasurementInfo::sampleMass))
        .setAutoWidth(true)
        .setResizable(true);
    ipGrid.addColumn(IpMeasurementLookup.MeasurementInfo::sampleVolume)
        .setHeader("Sample Volume (µl)")
        .setSortProperty(IpMeasurementLookup.IpSortKey.SAMPLE_VOLUME.sortKey())
        .setComparator(
            Comparator.comparing(IpMeasurementLookup.MeasurementInfo::sampleVolume))
        .setAutoWidth(true)
        .setResizable(true);
    ipGrid.addColumn(IpMeasurementLookup.MeasurementInfo::cycleFractionName)
        .setHeader("Cycle/Fraction Name")
        .setSortProperty(IpMeasurementLookup.IpSortKey.CYCLE_FRACTION_NAME.sortKey())
        .setComparator(
            Comparator.comparing(IpMeasurementLookup.MeasurementInfo::cycleFractionName))
        .setAutoWidth(true)
        .setResizable(true);
    ipGrid.addColumn(IpMeasurementLookup.MeasurementInfo::prepDate)
        .setHeader("Prep Date")
        .setSortProperty(IpMeasurementLookup.IpSortKey.PREP_DATE.sortKey())
        .setComparator(
            Comparator.comparing(IpMeasurementLookup.MeasurementInfo::prepDate))
        .setAutoWidth(true)
        .setResizable(true);
    ipGrid.addColumn(IpMeasurementLookup.MeasurementInfo::msRunDate)
        .setHeader("MS Run Date")
        .setSortProperty(IpMeasurementLookup.IpSortKey.MS_RUN_DATE.sortKey())
        .setComparator(
            Comparator.comparing(IpMeasurementLookup.MeasurementInfo::msRunDate))
        .setAutoWidth(true)
        .setResizable(true);
    ipGrid.addColumn(info -> formatTime(info.registeredAt(),
            MEASUREMENT_REGISTRATION_DATE_TIME_FORMAT))
        .setHeader("Registration Date")
        .setKey(IpMeasurementLookup.IpSortKey.REGISTRATION_DATE.sortKey())
        .setComparator(Comparator.comparing(IpMeasurementLookup.MeasurementInfo::registeredAt))
        .setAutoWidth(true)
        .setResizable(true);
    ipGrid.addColumn(info -> info.comment() != null ? info.comment() : "")
        .setHeader("Comment")
        .setSortable(false)
        .setAutoWidth(true)
        .setResizable(true);
    ipGrid.sort(GridSortOrder.asc(measurementIdColumn).build());
    return ipGrid;
  }

  // rendering helpers (unchanged)

  private static Component renderSamplesIp(IpMeasurementLookup.MeasurementInfo measurementInfo,
      Function<IpMeasurementLookup.SampleInfo, String> singleSampleConverter) {
    var sampleInfos = measurementInfo.sampleInfos();
    if (sampleInfos.size() == 1) {
      var sampleInfo = sampleInfos.stream().findFirst().orElseThrow();
      String singleSampleText = singleSampleConverter.apply(sampleInfo);
      return new Span(singleSampleText);
    }
    var displayLabel = measurementInfo.samplePool();
    var expandIcon = VaadinIcon.EXPAND_SQUARE.create();
    expandIcon.addClassNames("expand-icon", "icon-size-m", "color-primary",
        "padding-horizontal-02");
    var pooledSamplesSpan = new Span(new Span(displayLabel), expandIcon);
    pooledSamplesSpan.addClassNames("sample-column-cell", "clickable");
    pooledSamplesSpan.addClickListener(
        event -> openPooledSampleDialogIp(measurementInfo));
    return pooledSamplesSpan;
  }

  private static void openPooledSampleDialogIp(
      IpMeasurementLookup.MeasurementInfo measurementInfo) {
    AppDialog dialog = AppDialog.medium();
    DialogHeader.with(dialog, "View Pooled Measurement");
    DialogFooter.withConfirmOnly(dialog, "Close");
    var sampleInfoGrid = new Grid<IpMeasurementLookup.SampleInfo>();
    sampleInfoGrid.addColumn(IpMeasurementLookup.SampleInfo::sampleLabel)
        .setHeader("Sample Name")
        .setAutoWidth(true);
    sampleInfoGrid.addColumn(IpMeasurementLookup.SampleInfo::sampleCode)
        .setHeader("Sample Id")
        .setAutoWidth(true);
    sampleInfoGrid.setItems(measurementInfo.sampleInfos());
    DialogSection measuredSamplesSection = DialogSection.with(
        "Measurement ID: " + measurementInfo.measurementCode(),
        "Sample Pool Group: " + measurementInfo.samplePool(),
        sampleInfoGrid);

    DialogBody.withoutUserInput(dialog, measuredSamplesSection);
    dialog.registerConfirmAction(dialog::close);
    dialog.open();
  }

  private static Span renderInstrument(String label, String oboId, String iri) {
    Span instrumentLabel = new Span(label);
    Span instrumentOntologyLink = new Span(oboId);
    instrumentOntologyLink.addClassName("ontology-link");
    Anchor instrumentNameAnchor = new Anchor(iri, instrumentOntologyLink);
    instrumentNameAnchor.setTarget(AnchorTarget.BLANK);
    Span organisationSpan = new Span(instrumentLabel, instrumentNameAnchor);
    organisationSpan.addClassName("instrument-column");
    return organisationSpan;
  }

  private static Span renderMsDevice(String label, String oboId, String iri) {
    return renderInstrument(label, oboId, iri);
  }

  private static Anchor renderOrganisation(String label, String iri) {
    DownloadHandler downloadHandler =
        DownloadHandler.forClassResource(MeasurementDetailsComponent.class, "/icons/ROR_logo.svg");
    SvgIcon svgIcon = new SvgIcon(downloadHandler);
    svgIcon.addClassName("organisation-icon");
    Span organisationLabel = new Span(label);
    Anchor organisationAnchor = new Anchor(iri, organisationLabel, svgIcon);
    organisationAnchor.setTarget(AnchorTarget.BLANK);
    organisationAnchor.addClassName("organisation-entry");
    return organisationAnchor;
  }

  private static Component renderSamplesNgs(NgsMeasurementLookup.MeasurementInfo measurementInfo,
      Function<NgsMeasurementLookup.SampleInfo, String> singleSampleConverter) {
    var sampleInfos = measurementInfo.sampleInfos();
    if (sampleInfos.size() == 1) {
      NgsMeasurementLookup.SampleInfo sampleInfo = sampleInfos.stream().findFirst().orElseThrow();
      String singleSampleText = singleSampleConverter.apply(sampleInfo);
      return new Span(singleSampleText);
    }
    var displayLabel = measurementInfo.samplePool();
    var expandIcon = VaadinIcon.EXPAND_SQUARE.create();
    expandIcon.addClassNames("expand-icon", "icon-size-m", "color-primary",
        "padding-horizontal-02");
    var pooledSamplesSpan = new Span(new Span(displayLabel), expandIcon);
    pooledSamplesSpan.addClassNames("sample-column-cell", "clickable");
    pooledSamplesSpan.addClickListener(event -> openPooledSampleDialogNgs(measurementInfo));
    return pooledSamplesSpan;
  }

  private static Component renderSamplesPxp(PxpMeasurementLookup.MeasurementInfo measurementInfo,
      Function<PxpMeasurementLookup.SampleInfo, String> singleSampleConverter) {
    var sampleInfos = measurementInfo.sampleInfos();
    if (sampleInfos.size() == 1) {
      var sampleInfo = sampleInfos.stream().findFirst().orElseThrow();
      String singleSampleText = singleSampleConverter.apply(sampleInfo);
      return new Span(singleSampleText);
    }
    var displayLabel = measurementInfo.samplePool();
    var expandIcon = VaadinIcon.EXPAND_SQUARE.create();
    expandIcon.addClassNames("expand-icon", "icon-size-m", "color-primary",
        "padding-horizontal-02");
    var pooledSamplesSpan = new Span(new Span(displayLabel), expandIcon);
    pooledSamplesSpan.addClassNames("sample-column-cell", "clickable");
    pooledSamplesSpan.addClickListener(event -> openPooledSampleDialogPxp(measurementInfo));
    return pooledSamplesSpan;
  }

  private static void openPooledSampleDialogNgs(
      NgsMeasurementLookup.MeasurementInfo measurementInfo) {
    AppDialog dialog = AppDialog.medium();
    DialogHeader.with(dialog, "View Pooled Measurement");
    DialogFooter.withConfirmOnly(dialog, "Close");
    var sampleInfoGrid = new Grid<NgsMeasurementLookup.SampleInfo>();
    sampleInfoGrid.addColumn(NgsMeasurementLookup.SampleInfo::sampleLabel)
        .setHeader("Sample Name")
        .setAutoWidth(true);
    sampleInfoGrid.addColumn(NgsMeasurementLookup.SampleInfo::sampleCode)
        .setHeader("Sample Id")
        .setAutoWidth(true);
    sampleInfoGrid.addColumn(NgsMeasurementLookup.SampleInfo::indexI7)
        .setHeader("Index i7")
        .setAutoWidth(true);
    sampleInfoGrid.addColumn(NgsMeasurementLookup.SampleInfo::indexI5)
        .setHeader("Index i5")
        .setAutoWidth(true);
    sampleInfoGrid.addColumn(NgsMeasurementLookup.SampleInfo::comment)
        .setHeader("Comment")
        .setAutoWidth(true);
    sampleInfoGrid.setItems(measurementInfo.sampleInfos());
    DialogSection measuredSamplesSection = DialogSection.with(
        "Measurement ID: " + measurementInfo.measurementCode(),
        "Sample Pool Group: " + measurementInfo.samplePool(),
        sampleInfoGrid);

    DialogBody.withoutUserInput(dialog, measuredSamplesSection);
    dialog.registerConfirmAction(dialog::close);
    dialog.open();
  }

  private static void openPooledSampleDialogPxp(
      PxpMeasurementLookup.MeasurementInfo measurementInfo) {
    AppDialog dialog = AppDialog.medium();
    DialogHeader.with(dialog, "View Pooled Measurement");
    DialogFooter.withConfirmOnly(dialog, "Close");
    var sampleInfoGrid = new Grid<PxpMeasurementLookup.SampleInfo>();
    sampleInfoGrid.addColumn(PxpMeasurementLookup.SampleInfo::sampleLabel)
        .setHeader("Sample Name")
        .setAutoWidth(true);
    sampleInfoGrid.addColumn(PxpMeasurementLookup.SampleInfo::sampleCode)
        .setHeader("Sample Id")
        .setAutoWidth(true);
    sampleInfoGrid.addColumn(PxpMeasurementLookup.SampleInfo::comment)
        .setHeader("Comment")
        .setAutoWidth(true);
    sampleInfoGrid.setItems(measurementInfo.sampleInfos());
    DialogSection measuredSamplesSection = DialogSection.with(
        "Measurement ID: " + measurementInfo.measurementCode(),
        "Sample Pool Group: " + measurementInfo.samplePool(),
        sampleInfoGrid);

    DialogBody.withoutUserInput(dialog, measuredSamplesSection);
    dialog.registerConfirmAction(dialog::close);
    dialog.open();
  }

  private void validateContext(Context context) throws ContextValidationException {
    if (isNull(context)) {
      throw new ContextValidationException("Context cannot be null");
    }
    context.projectId().orElseThrow(
        () -> new ContextValidationException("Context must contain the project id"));
    context.experimentId().orElseThrow(
        () -> new ContextValidationException("Context must contain the experiment id"));
  }

  private static final class ContextValidationException extends ApplicationException {

    public ContextValidationException(String message) {
      super(message);
    }
  }

  private void displayMissingSelectionNote() {
    messageFactory.toast("measurement.no-measurements-selected", new Object[]{}, getLocale())
        .open();
  }

  // ===== event classes (unchanged contract) =====

  public static class NgsMeasurementRegistrationRequested extends
      ComponentEvent<MeasurementDetailsComponent> {

    public NgsMeasurementRegistrationRequested(MeasurementDetailsComponent source,
        boolean fromClient) {
      super(source, fromClient);
    }
  }

  public Registration addNgsRegisterListener(
      ComponentEventListener<NgsMeasurementRegistrationRequested> listener) {
    return addListener(NgsMeasurementRegistrationRequested.class, listener);
  }

  public static class NgsMeasurementEditRequested extends
      ComponentEvent<MeasurementDetailsComponent> {

    private final List<String> measurementIds;

    public NgsMeasurementEditRequested(List<String> measurementIds,
        MeasurementDetailsComponent source, boolean fromClient) {
      super(source, fromClient);
      this.measurementIds = measurementIds.stream().toList();
    }

    public List<String> measurementIds() {
      return measurementIds;
    }
  }

  public Registration addNgsEditListener(
      ComponentEventListener<NgsMeasurementEditRequested> listener) {
    return addListener(NgsMeasurementEditRequested.class, listener);
  }

  public static class NgsMeasurementExportRequested extends
      ComponentEvent<MeasurementDetailsComponent> {

    private final List<String> measurementIds;

    public NgsMeasurementExportRequested(List<String> measurementIds,
        MeasurementDetailsComponent source, boolean fromClient) {
      super(source, fromClient);
      this.measurementIds = measurementIds.stream().toList();
    }

    public List<String> measurementIds() {
      return measurementIds;
    }
  }

  public Registration addNgsExportListener(
      ComponentEventListener<NgsMeasurementExportRequested> listener) {
    return addListener(NgsMeasurementExportRequested.class, listener);
  }

  public static class NgsMeasurementDeletionRequested extends
      ComponentEvent<MeasurementDetailsComponent> {

    private final List<String> measurementIds;

    public NgsMeasurementDeletionRequested(List<String> measurementIds,
        MeasurementDetailsComponent source, boolean fromClient) {
      super(source, fromClient);
      this.measurementIds = measurementIds.stream().toList();
    }

    public List<String> measurementIds() {
      return measurementIds;
    }
  }

  public Registration addNgsDeletionListener(
      ComponentEventListener<NgsMeasurementDeletionRequested> listener) {
    return addListener(NgsMeasurementDeletionRequested.class, listener);
  }

  public static class PxpMeasurementRegistrationRequested extends
      ComponentEvent<MeasurementDetailsComponent> {

    public PxpMeasurementRegistrationRequested(MeasurementDetailsComponent source,
        boolean fromClient) {
      super(source, fromClient);
    }
  }

  public Registration addPxpRegisterListener(
      ComponentEventListener<PxpMeasurementRegistrationRequested> listener) {
    return addListener(PxpMeasurementRegistrationRequested.class, listener);
  }

  public static class PxpMeasurementEditRequested extends
      ComponentEvent<MeasurementDetailsComponent> {

    private final List<String> measurementIds;

    public PxpMeasurementEditRequested(List<String> measurementIds,
        MeasurementDetailsComponent source, boolean fromClient) {
      super(source, fromClient);
      this.measurementIds = measurementIds.stream().toList();
    }

    public List<String> measurementIds() {
      return measurementIds;
    }
  }

  public Registration addPxpEditListener(
      ComponentEventListener<PxpMeasurementEditRequested> listener) {
    return addListener(PxpMeasurementEditRequested.class, listener);
  }

  public static class PxpMeasurementExportRequested extends
      ComponentEvent<MeasurementDetailsComponent> {

    private final List<String> measurementIds;

    public PxpMeasurementExportRequested(List<String> measurementIds,
        MeasurementDetailsComponent source, boolean fromClient) {
      super(source, fromClient);
      this.measurementIds = measurementIds.stream().toList();
    }

    public List<String> measurementIds() {
      return measurementIds;
    }
  }

  public Registration addPxpExportListener(
      ComponentEventListener<PxpMeasurementExportRequested> listener) {
    return addListener(PxpMeasurementExportRequested.class, listener);
  }

  public static class PxpMeasurementDeletionRequested extends
      ComponentEvent<MeasurementDetailsComponent> {

    private final List<String> measurementIds;

    public PxpMeasurementDeletionRequested(List<String> measurementIds,
        MeasurementDetailsComponent source, boolean fromClient) {
      super(source, fromClient);
      this.measurementIds = measurementIds.stream().toList();
    }

    public List<String> measurementIds() {
      return measurementIds;
    }
  }

  public Registration addPxpDeletionListener(
      ComponentEventListener<PxpMeasurementDeletionRequested> listener) {
    return addListener(PxpMeasurementDeletionRequested.class, listener);
  }

  public static class IpMeasurementRegistrationRequested extends
      ComponentEvent<MeasurementDetailsComponent> {

    public IpMeasurementRegistrationRequested(MeasurementDetailsComponent source,
        boolean fromClient) {
      super(source, fromClient);
    }
  }

  public Registration addIpRegisterListener(
      ComponentEventListener<IpMeasurementRegistrationRequested> listener) {
    return addListener(IpMeasurementRegistrationRequested.class, listener);
  }

  public static class IpMeasurementEditRequested extends
      ComponentEvent<MeasurementDetailsComponent> {

    private final List<String> measurementIds;

    public IpMeasurementEditRequested(List<String> measurementIds,
        MeasurementDetailsComponent source, boolean fromClient) {
      super(source, fromClient);
      this.measurementIds = measurementIds.stream().toList();
    }

    public List<String> measurementIds() {
      return measurementIds;
    }
  }

  public Registration addIpEditListener(
      ComponentEventListener<IpMeasurementEditRequested> listener) {
    return addListener(IpMeasurementEditRequested.class, listener);
  }

  public static class IpMeasurementDeletionRequested extends
      ComponentEvent<MeasurementDetailsComponent> {

    private final List<String> measurementIds;

    public IpMeasurementDeletionRequested(List<String> measurementIds,
        MeasurementDetailsComponent source, boolean fromClient) {
      super(source, fromClient);
      this.measurementIds = measurementIds.stream().toList();
    }

    public List<String> measurementIds() {
      return measurementIds;
    }
  }

  public Registration addIpDeletionListener(
      ComponentEventListener<IpMeasurementDeletionRequested> listener) {
    return addListener(IpMeasurementDeletionRequested.class, listener);
  }

  public static class IpMeasurementExportRequested extends
      ComponentEvent<MeasurementDetailsComponent> {

    private final List<String> measurementIds;

    public IpMeasurementExportRequested(List<String> measurementIds,
        MeasurementDetailsComponent source, boolean fromClient) {
      super(source, fromClient);
      this.measurementIds = measurementIds.stream().toList();
    }

    public List<String> measurementIds() {
      return measurementIds;
    }
  }

  public Registration addIpExportListener(
      ComponentEventListener<IpMeasurementExportRequested> listener) {
    return addListener(IpMeasurementExportRequested.class, listener);
  }
}