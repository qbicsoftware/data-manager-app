package life.qbic.datamanager.views.projects.project.dataset;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.ShortcutRegistration;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.checkbox.CheckboxGroupVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.QuerySortOrder;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.selection.MultiSelectionEvent;
import com.vaadin.flow.data.value.ValueChangeMode;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import life.qbic.application.commons.FileSizeFormatter;
import life.qbic.application.commons.SortOrder;
import life.qbic.application.commons.time.DateTimeFormat;
import life.qbic.datamanager.views.general.Tag;
import life.qbic.datamanager.views.general.pagination.ListState;
import life.qbic.datamanager.views.general.pagination.ListStateCodec;
import life.qbic.datamanager.views.general.pagination.PaginatedGrid;
import life.qbic.datamanager.views.general.pagination.PaginationBar;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.api.ProjectQueryService;
import life.qbic.projectmanagement.application.api.ProjectQueryService.MeasurementDataFilter;
import life.qbic.projectmanagement.application.query.MeasurementDataRow;
import life.qbic.projectmanagement.application.query.MeasurementDataRow.DataStatus;
import org.jspecify.annotations.NonNull;

/**
 * The measurement data list of one experiment: every registered measurement with its data status.
 *
 * <p>This replaces two surfaces that each held half of the truth. The measurement lists read the
 * measurement tables and carried no data field; the raw-data lists read views that
 * {@code INNER JOIN} the data table, so a measurement whose data had not arrived yet was not a row
 * at all. One list, one row per measurement, with the second process step stated as a column.</p>
 *
 * <p>The grid is identity-first: measurement ID, name, samples, instrument, registration date and
 * the <b>Data</b> status. The type-specific attributes (12 for genomics, 15 for proteomics, 23 for
 * immunopeptidomics) live in {@link MeasurementDataDetailPanel} and stay reachable as optional
 * grid columns through Show/Hide Columns, so a power user can restore a flat table without a
 * second surface.</p>
 *
 * <p>Segmenting by measurement method is the page's outer structure, because a measurement dataset
 * is the experiment's data for one method: an experiment has one per method it measured, not one in
 * total. Only methods that actually have measurements get a segment — an empty segment is not an
 * empty dataset.</p>
 *
 * @since 1.21.0
 */
public class MeasurementDatasetDetailsComponent extends Div implements Serializable {

  @Serial
  private static final long serialVersionUID = -1L;

  private static final Logger log =
      LoggerFactory.logger(MeasurementDatasetDetailsComponent.class);

  private final transient ProjectQueryService queryService;
  private final String projectId;
  private final String experimentId;

  private final Grid<MeasurementDataRow> grid = new Grid<>();
  private final PaginatedGrid<MeasurementDataRow> paginatedGrid;
  private final PaginationBar paginationBar = new PaginationBar(
      ListStateCodec.ALLOWED_PAGE_SIZES, ListStateCodec.DEFAULT_PAGE_SIZE, "measurement data");
  private final TextField searchField = new TextField();

  /**
   * The data-status filter. It is the reason the status column exists: a reader looking for the
   * outstanding work filters to {@code Waiting} instead of scanning pages for it.
   */
  private final Select<DataStatusFilter> dataStatusFilter = new Select<>();

  private final Select<MeasurementDatasetSegment> segmentFilter = new Select<>();
  private final Div segmentTabs = new Div();
  private final AtomicReference<String> clientTimeZone = new AtomicReference<>("UTC");

  private final Span statusLine = new Span();
  private final Div errorState = new Div();
  private final Icon selectionIcon = VaadinIcon.CHECK_SQUARE_O.create();
  private final Span selectionDisplay = new Span();
  private final Button selectAllResultsButton = new Button();
  private final Button clearSelectionButton = new Button("Clear selection");
  private final Div selectionBar = new Div(selectionIcon, selectionDisplay, selectAllResultsButton,
      clearSelectionButton);
  private long totalItemsActive;

  private final Div detailLayout = new Div();
  private final Div listColumn = new Div();
  private final MeasurementDataDetailPanel detailPanel = new MeasurementDataDetailPanel();
  private final Div detailBackdrop = new Div();
  private ShortcutRegistration escShortcut;
  private final AtomicReference<String> openDetailId = new AtomicReference<>();
  private final AtomicReference<Button> detailsButtonInvoker = new AtomicReference<>();

  /**
   * The active segment, or {@code null} while the page is still deciding which segments exist.
   * A null segment means "no filtering by method", which is also what the query expects.
   */
  private MeasurementDatasetSegment activeSegment;
  private DataStatusFilter activeDataStatus = DataStatusFilter.ALL;

  public MeasurementDatasetDetailsComponent(@NonNull ProjectQueryService queryService,
      @NonNull String projectId, @NonNull String experimentId) {
    this.queryService = Objects.requireNonNull(queryService);
    this.projectId = Objects.requireNonNull(projectId);
    this.experimentId = Objects.requireNonNull(experimentId);
    addClassNames("measurement-dataset-details", "measurement-dataset-details-content");

    paginatedGrid = new PaginatedGrid<>(grid, this::loadPage,
        MeasurementDataRow::measurementId, "measurement data", MeasurementDataSort.DEFAULT,
        false, false, false);
    paginatedGrid.addClassNames("width-full");

    configureGrid();
    configureSearch();
    configureFilters();
    configureSort();
    configureSelectionReconciliation();
    configureRowHighlight();
    configureRowActivation();
    configureSelectionBar();
    configurePagination();
    configureErrorState();

    paginatedGrid.addPageLoadedListener(event -> {
      totalItemsActive = event.getTotal();
      paginationBar.setListState(event.getPage(), event.getTotal(),
          paginatedGrid.listState().pageSize());
      paginationBar.setVisible(event.getTotal() > 0);
      applySelectionToGrid();
      updateSelectionBar();
    });

    listColumn.addClassName("measurement-dataset-list");
    listColumn.add(paginatedGrid, paginationBar);
    detailLayout.addClassName("measurement-dataset-detail-layout");
    detailBackdrop.addClassName("measurement-dataset-detail-backdrop");
    detailBackdrop.addClickListener(event -> closeDetail());
    detailLayout.add(listColumn, detailPanel, detailBackdrop);
    detailPanel.addCloseListener(event -> closeDetail());

    add(segmentTabs, createToolbar(), selectionBar, errorState, detailLayout);
    updateSelectionBar();
  }

  @Override
  protected void onAttach(AttachEvent attachEvent) {
    super.onAttach(attachEvent);
    attachEvent.getUI().getPage().getExtendedClientDetails().refresh(
        receiver -> {
          clientTimeZone.set(receiver.getTimeZoneId());
          // The client time zone arrives asynchronously, possibly after the first page rendered;
          // re-render so the date column reflects the reader's zone.
          paginatedGrid.refreshItems();
        });
  }

  // ── grid ────────────────────────────────────────────────────────────────

  private void configureGrid() {
    grid.addClassName("measurement-dataset-grid");
    grid.setColumnReorderingAllowed(true);

    var idColumn = grid.addColumn(MeasurementDataRow::measurementCode)
        .setHeader("Measurement ID")
        .setSortProperty("measurementId")
        .setComparator(MeasurementDataRow::measurementCode)
        .setAutoWidth(true)
        .setFlexGrow(0)
        .setTooltipGenerator(MeasurementDataRow::measurementCode)
        .setFrozen(true);
    grid.addColumn(row -> textOrDash(row.measurementName()))
        .setHeader("Measurement name")
        .setSortProperty("measurementName")
        .setTooltipGenerator(MeasurementDataRow::measurementName)
        .setAutoWidth(true)
        .setResizable(true);
    grid.addColumn(row -> row.samplePool() == null || row.samplePool().isBlank()
            ? row.sampleCount() == 1 ? "1 sample" : row.sampleCount() + " samples"
            : row.samplePool())
        .setHeader("Samples")
        .setSortProperty("samplePool")
        .setAutoWidth(true)
        .setResizable(true);
    grid.addColumn(row -> textOrDash(row.facility()))
        .setHeader("Facility")
        .setAutoWidth(true)
        .setResizable(true);
    var registeredColumn = grid.addColumn(row -> formatTime(row.registeredAt()))
        .setHeader("Registration date")
        .setSortProperty("registrationDate")
        .setComparator(MeasurementDataRow::registeredAt)
        .setAutoWidth(true)
        .setResizable(true);
    // The one status column. It is what drives attention and what the whole merge exists to
    // state, so it is visible by default rather than hidden behind the column menu.
    grid.addComponentColumn(this::dataStatusCell)
        .setHeader("Data")
        .setSortProperty("dataStatus")
        .setComparator((left, right) -> left.dataStatus().compareTo(right.dataStatus()))
        .setAutoWidth(true)
        .setResizable(true);
    grid.addComponentColumn(this::detailsCell)
        .setHeader("")
        .setAutoWidth(true)
        .setFlexGrow(0);

    addHiddenDetailColumns();
    // Default order: most recently registered first. Metadata is registered before its data
    // arrives, so the newest rows are also the ones most likely to still be waiting.
    grid.sort(GridSortOrder.desc(registeredColumn).build());
  }

  /**
   * Appends the type-specific and audit columns, hidden. They keep their headers so the
   * Show/Hide Columns menu can restore a flat table without a second surface — the same convention
   * the samples page uses for the columns its detail panel took over.
   */
  private void addHiddenDetailColumns() {
    grid.addColumn(row -> String.valueOf(row.sampleCount()))
        .setHeader("Sample count")
        .setAutoWidth(true)
        .setVisible(false);
    grid.addColumn(row -> formatTime(row.dataUploadedAt()))
        .setHeader("Upload date")
        .setAutoWidth(true)
        .setResizable(true)
        .setVisible(false);
    grid.addColumn(row -> row.hasRecordedSize()
            ? FileSizeFormatter.formatBytes(row.dataSizeBytes())
            : "")
        .setHeader("Total size")
        .setAutoWidth(true)
        .setResizable(true)
        .setVisible(false);
    grid.addColumn(row -> String.join(", ", row.dataFileTypes().stream().sorted().toList()))
        .setHeader("File suffixes")
        .setAutoWidth(true)
        .setResizable(true)
        .setVisible(false);
    grid.addColumn(row -> row.dataFileCount() == null ? "" : String.valueOf(row.dataFileCount()))
        .setHeader("Files")
        .setAutoWidth(true)
        .setResizable(true)
        .setVisible(false);
  }

  /**
   * Renders the data status. The state is carried by the text, never by colour alone: a reader who
   * does not perceive the tint still reads "Waiting" or "Available".
   */
  private Span dataStatusCell(MeasurementDataRow row) {
    Span cell = new Span();
    if (row.dataStatus() == DataStatus.WAITING) {
      cell.setText("Waiting");
      cell.addClassName("measurement-dataset-status--waiting");
    } else {
      // The facts that make the datum usable are stated with the status, so the reader does not
      // have to open the panel to know whether the data is even the right shape.
      cell.setText("Available \u00b7 " + dataFacts(row));
      cell.addClassName("measurement-dataset-status--available");
    }
    cell.addClassName("measurement-dataset-status");
    cell.getElement().setAttribute("title", cell.getText());
    return cell;
  }

  private static String dataFacts(MeasurementDataRow row) {
    List<String> facts = new ArrayList<>();
    if (row.fileCountOrZero() > 0) {
      facts.add(row.fileCountOrZero() == 1 ? "1 file" : row.fileCountOrZero() + " files");
    }
    if (row.hasRecordedSize()) {
      facts.add(FileSizeFormatter.formatBytes(row.dataSizeBytes()));
    }
    return facts.isEmpty() ? "no file details recorded" : String.join(" \u00b7 ", facts);
  }

  private ComponentRenderer<Div, MeasurementDataRow> detailsCellRenderer() {
    return new ComponentRenderer<>(this::detailsCellContainer);
  }

  private Div detailsCellContainer(MeasurementDataRow row) {
    Button details = new Button(VaadinIcon.ANGLE_RIGHT.create());
    details.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    details.addClassName("measurement-dataset-details-button");
    // The label carries the code so a screen-reader row list stays meaningful: a bare "Open
    // details" repeated per row would not say which measurement it belongs to.
    details.getElement().setAttribute("aria-label",
        "Open details for " + row.measurementCode());
    details.addClickListener(event -> openDetail(row, details));
    Div wrapper = new Div(details);
    wrapper.addClassName("measurement-dataset-details-cell");
    return wrapper;
  }

  private com.vaadin.flow.component.Component detailsCell(MeasurementDataRow row) {
    return detailsCellContainer(row);
  }

  // ── toolbar, search, filters ────────────────────────────────────────────

  private Div createToolbar() {
    searchField.addClassName("measurement-dataset-search");
    Div toolbar = new Div(searchField, segmentFilter, dataStatusFilter);
    toolbar.addClassName("measurement-dataset-toolbar");
    Div toolbarRight = new Div();
    toolbarRight.addClassName("measurement-dataset-toolbar-right");
    toolbarRight.add(showHideColumnsMenu(grid));
    toolbar.add(toolbarRight);
    return toolbar;
  }

  private void configureSearch() {
    searchField.setPlaceholder("Search measurement data");
    searchField.setSuffixComponent(VaadinIcon.SEARCH.create());
    searchField.setClearButtonVisible(true);
    searchField.setValueChangeMode(ValueChangeMode.LAZY);
    searchField.addValueChangeListener(event -> {
      String filter = event.getValue() == null ? "" : event.getValue().trim();
      if (filter.equals(paginatedGrid.listState().filter())) {
        return;
      }
      paginatedGrid.setListState(paginatedGrid.listState().withFilter(filter).withPage(1));
    });
  }

  private void configureFilters() {
    dataStatusFilter.setLabel("Data");
    dataStatusFilter.setItems(DataStatusFilter.values());
    dataStatusFilter.setItemLabelGenerator(DataStatusFilter::label);
    dataStatusFilter.setValue(DataStatusFilter.ALL);
    dataStatusFilter.addValueChangeListener(event -> {
      DataStatusFilter value = event.getValue() == null ? DataStatusFilter.ALL : event.getValue();
      if (value == activeDataStatus) {
        return;
      }
      activeDataStatus = value;
      paginatedGrid.setListState(paginatedGrid.listState().withPage(1));
    });

    // The segment filter only appears when the experiment measured more than one method: with a
    // single method a "filter" that cannot change anything is a dead control.
    segmentFilter.setLabel("Measurement method");
    segmentFilter.setVisible(false);
    segmentFilter.addValueChangeListener(event -> {
      MeasurementDatasetSegment value = event.getValue();
      if (value == null || value == activeSegment) {
        return;
      }
      activeSegment = value;
      paginatedGrid.setListState(paginatedGrid.listState().withPage(1));
    });
  }

  private void configureSort() {
    grid.setMultiSort(false);
    grid.addSortListener(event -> {
      List<GridSortOrder<MeasurementDataRow>> orders = grid.getSortOrder();
      if (orders.isEmpty()) {
        return;
      }
      GridSortOrder<MeasurementDataRow> order = orders.getFirst();
      String property = sortPropertyOf(order);
      if (property == null || property.isBlank()) {
        return;
      }
      boolean descending = order.getDirection() == SortDirection.DESCENDING;
      SortOrder sortOrder = new SortOrder(property, descending);
      if (!MeasurementDataSort.allowedSortOrders().contains(sortOrder)) {
        return;
      }
      ListState current = paginatedGrid.listState();
      if (sortOrder.equals(current.sort())) {
        return;
      }
      paginatedGrid.setListState(current.withSort(sortOrder).withPage(1));
    });
  }

  private static String sortPropertyOf(GridSortOrder<?> order) {
    if (order.getSorted() == null) {
      return null;
    }
    return order.getSorted().getSortOrder(order.getDirection())
        .findFirst()
        .map(QuerySortOrder::getSorted)
        .orElse(null);
  }

  private static <T> MenuBar showHideColumnsMenu(Grid<T> grid) {
    MenuBar menuBar = new MenuBar();
    Span itemContent = new Span(new Span("Show/Hide Columns"), VaadinIcon.CHEVRON_DOWN.create());
    var subMenu = menuBar.addItem(itemContent).getSubMenu();
    CheckboxGroup<Column<T>> checkboxGroup = new CheckboxGroup<>();
    checkboxGroup.setItemLabelGenerator(Column::getHeaderText);
    List<Column<T>> columns = grid.getColumns().stream()
        .filter(column -> column.getHeaderText() != null && !column.getHeaderText().isBlank())
        .toList();
    checkboxGroup.setItems(columns);
    checkboxGroup.setValue(columns.stream().filter(Column::isVisible).collect(Collectors.toSet()));
    checkboxGroup.addThemeVariants(CheckboxGroupVariant.LUMO_VERTICAL);
    checkboxGroup.addClassNames("flex-vertical");
    checkboxGroup.getElement()
        .executeJs("this.addEventListener('click', e => e.stopPropagation());");
    checkboxGroup.addValueChangeListener(event -> {
      Set<Column<T>> selected = event.getValue();
      columns.forEach(column -> column.setVisible(selected.contains(column)));
    });
    subMenu.addComponent(checkboxGroup);
    return menuBar;
  }

  // ── selection ───────────────────────────────────────────────────────────

  private void configureSelectionReconciliation() {
    grid.addSelectionListener(event -> {
      if (!event.isFromClient()) {
        return;
      }
      MultiSelectionEvent<Grid<MeasurementDataRow>, MeasurementDataRow> multi =
          (MultiSelectionEvent<Grid<MeasurementDataRow>, MeasurementDataRow>) event;
      multi.getAddedSelection().forEach(item -> paginatedGrid.select(
          Set.of(item.measurementId())));
      multi.getRemovedSelection().forEach(item -> paginatedGrid.deselect(
          Set.of(item.measurementId())));
    });
  }

  private void configureSelectionBar() {
    selectionIcon.addClassName("measurement-dataset-selection-icon");
    selectionDisplay.addClassName("measurement-dataset-selection-count");
    selectAllResultsButton.addClassName("measurement-dataset-select-all-results");
    clearSelectionButton.addClassName("measurement-dataset-clear-selection");
    selectionBar.addClassName("measurement-dataset-selection-bar");
    selectionBar.setVisible(false);
    clearSelectionButton.addClickListener(ignored -> {
      paginatedGrid.deselect(Set.copyOf(paginatedGrid.selectedIds()));
      applySelectionToGrid();
    });
    selectAllResultsButton.addClickListener(ignored -> selectAllMatching());
  }

  private void configurePagination() {
    paginationBar.addChangeListener(event -> {
      ListState current = paginatedGrid.listState();
      ListState requested = event.getPageSize() != current.pageSize()
          ? current.withPageSize(event.getPageSize()).withPage(1)
          : current.withPage(event.getPage());
      if (requested.equals(current)) {
        return;
      }
      paginatedGrid.setListState(requested);
    });
  }

  private void configureErrorState() {
    errorState.addClassName("measurement-dataset-error-state");
    errorState.setVisible(false);
    errorState.getElement().setAttribute("role", "alert");
    Span message = new Span("Measurement data could not be loaded.");
    message.addClassName("measurement-dataset-error-state-message");
    Button retry = new Button("Try again");
    retry.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    retry.addClickListener(event -> paginatedGrid.refresh());
    errorState.add(message, retry);
  }

  private void updateSelectionBar() {
    int count = paginatedGrid.selectedIds().size();
    boolean hasSelection = count > 0;
    selectionBar.setVisible(hasSelection);
    if (count > 0 && count == totalItemsActive) {
      selectionDisplay.setText(count == 1
          ? "The single measurement matching the filter is selected"
          : "All %d measurements matching the filter are selected".formatted(count));
      selectAllResultsButton.setVisible(false);
    } else {
      selectionDisplay.setText(count == 1
          ? "1 measurement is selected"
          : "%d measurements are selected".formatted(count));
      boolean offerSelectAll = count > 0 && count < totalItemsActive;
      selectAllResultsButton.setVisible(offerSelectAll);
      if (offerSelectAll) {
        selectAllResultsButton.setText(
            "Select all %d matching measurements".formatted(totalItemsActive));
      }
    }
    clearSelectionButton.setVisible(hasSelection);
  }

  private void selectAllMatching() {
    MeasurementDataFilter filter = currentFilter(paginatedGrid.listState());
    long total = queryService.countMeasurementData(projectId, experimentId, filter)
        .blockOptional().orElse(0L);
    Set<String> ids = new HashSet<>(queryService.getMeasurementData(projectId, experimentId, 0,
            (int) total, filter)
        .map(MeasurementDataRow::measurementId)
        .collectList().blockOptional().orElse(List.of()));
    paginatedGrid.select(ids);
    applySelectionToGrid();
    updateSelectionBar();
  }

  private void applySelectionToGrid() {
    paginatedGrid.grid().getGenericDataView().getItems().forEach(item -> {
      if (paginatedGrid.selectedIds().contains(item.measurementId())) {
        paginatedGrid.grid().select(item);
      } else {
        paginatedGrid.grid().deselect(item);
      }
    });
  }

  // ── row activation and the detail panel ─────────────────────────────────

  private void configureRowHighlight() {
    grid.setPartNameGenerator(row -> {
      String openId = openDetailId.get();
      return openId != null && openId.equals(row.measurementId()) ? "open-detail-row" : null;
    });
  }

  /**
   * Opens the record when a row is clicked.
   *
   * <p>This deliberately does not touch the grid's selection: Vaadin toggles selection on a row
   * click only when the selection column's {@code autoSelect} is enabled, and it is not. Reading an
   * entry must never add it to a bulk selection, or a later Delete could hit a measurement the user
   * only inspected. The row's own Details button stays as the keyboard path, because the item-click
   * event is bound to the mouse click only.</p>
   */
  private void configureRowActivation() {
    grid.addItemClickListener(event -> openDetail(event.getItem(), null));
  }

  private void openDetail(MeasurementDataRow row, Button invoker) {
    detailsButtonInvoker.set(invoker);
    detailPanel.show(row, this::formatTime);
    detailLayout.addClassName("measurement-dataset-detail-layout--open");
    openDetailId.set(row.measurementId());
    grid.getDataProvider().refreshAll();
    if (escShortcut != null) {
      escShortcut.remove();
    }
    escShortcut = Shortcuts.addShortcutListener(this, event -> closeDetail(), Key.ESCAPE);
  }

  private void closeDetail() {
    detailPanel.close();
    detailLayout.removeClassName("measurement-dataset-detail-layout--open");
    openDetailId.set(null);
    grid.getDataProvider().refreshAll();
    if (escShortcut != null) {
      escShortcut.remove();
      escShortcut = null;
    }
    Button invoker = detailsButtonInvoker.getAndSet(null);
    if (invoker != null) {
      invoker.getElement().executeJs("this.focus()");
    }
  }

  // ── data ────────────────────────────────────────────────────────────────

  private PaginatedGrid.Page<MeasurementDataRow> loadPage(ListState state) {
    int offset = (state.page() - 1) * state.pageSize();
    int limit = state.pageSize();
    MeasurementDataFilter filter = currentFilter(state);
    try {
      // Page and count are filtered identically by construction: both take this one filter, which
      // is the only way the pager's total can agree with its own content.
      long total = queryService.countMeasurementData(projectId, experimentId, filter)
          .blockOptional().orElse(0L);
      List<MeasurementDataRow> page = queryService.getMeasurementData(projectId, experimentId,
              offset, limit, filter)
          .collectList().blockOptional().orElse(List.of());
      errorState.setVisible(false);
      return new PaginatedGrid.Page<>(page, total);
    } catch (RuntimeException e) {
      // A failure must not be rendered as "no measurement data", which is a legitimate and very
      // different state (every entry waiting).
      log.error("Could not load the measurement data page", e);
      errorState.setVisible(true);
      return new PaginatedGrid.Page<>(List.of(), 0);
    }
  }

  private MeasurementDataFilter currentFilter(ListState state) {
    List<ProjectQueryService.SortOrder<ProjectQueryService.MeasurementDataSortKey>> orders =
        new ArrayList<>();
    orders.add(MeasurementDataSort.toApiSortOrder(state.sort()));
    if (!"measurementId".equals(state.sort().propertyName())) {
      orders.add(new ProjectQueryService.SortOrder<>(
          ProjectQueryService.MeasurementDataSortKey.MEASUREMENT_ID,
          ProjectQueryService.SortDirection.ASC));
    }
    AssayTypeValue type = activeSegment == null ? null : new AssayTypeValue(activeSegment);
    return new MeasurementDataFilter(state.filter(), type.value(),
        activeDataStatus.status(), orders);
  }

  /**
   * Applies the segments that actually hold measurements, and selects the first one.
   *
   * <p>Segment visibility is derived from the measurements, never from data presence: a method
   * whose data has not arrived yet still has a dataset to show, and hiding its segment was the
   * class of defect that made the proteomics tab disappear from the raw-data page.</p>
   *
   * @param available the segments that hold at least one measurement
   */
  public void applySegments(List<MeasurementDatasetSegment> available) {
    segmentTabs.removeAll();
    if (available.isEmpty()) {
      segmentFilter.setVisible(false);
      return;
    }
    if (available.size() > 1) {
      segmentFilter.setItems(available);
      segmentFilter.setItemLabelGenerator(MeasurementDatasetSegment::label);
      segmentFilter.setVisible(true);
      if (activeSegment == null || !available.contains(activeSegment)) {
        activeSegment = available.getFirst();
        segmentFilter.setValue(activeSegment);
      }
    } else {
      segmentFilter.setVisible(false);
      activeSegment = available.getFirst();
    }
  }

  /**
   * States what the page currently holds, above the list.
   *
   * @param total    the number of entries in the experiment
   * @param waiting  how many of them are still waiting for their data
   */
  public void updateStatusLine(long total, long waiting) {
    if (total <= 0) {
      statusLine.setText("No measurement data registered yet");
    } else {
      String entries = total == 1 ? "1 measurement entry" : "%d measurement entries".formatted(total);
      String pending = waiting == 0
          ? "all with data available"
          : waiting == 1
              ? "1 waiting for data"
              : "%d waiting for data".formatted(waiting);
      statusLine.setText(entries + " \u00b7 " + pending);
    }
    if (statusLine.getParent().isEmpty()) {
      addComponentAsFirst(statusLine);
    }
    statusLine.addClassName("measurement-dataset-status-line");
  }

  /**
   * Registers a listener notified after every page load, so the owning route view can mirror the
   * list state into the browser URL (USER-R-03).
   *
   * @param listener the listener to add
   */
  public void addPageLoadedListener(
      com.vaadin.flow.component.ComponentEventListener<PaginatedGrid.PageLoadedEvent> listener) {
    paginatedGrid.addPageLoadedListener(listener);
  }

  /** @return the list state currently shown, for the owning route view to mirror into the URL. */
  public ListState listState() {
    return paginatedGrid.listState();
  }

  /** @return the underlying paginated grid, for the owning view's URL wiring. */
  public PaginatedGrid<MeasurementDataRow> paginatedGrid() {
    return paginatedGrid;
  }

  /** Re-applies list state from the URL without letting it echo back into the URL. */
  public void applyExternalState(ListState state) {
    paginatedGrid.applyExternalState(state);
  }

  /** Reloads the current page in place, preserving page, filter and sort. */
  public void refresh() {
    paginatedGrid.refresh();
  }

  private String formatTime(Instant instant) {
    if (instant == null) {
      return "";
    }
    return DateTimeFormat.asJavaFormatter(DateTimeFormat.SIMPLE_DATE_TIME_SHORT,
            ZoneId.of(clientTimeZone.get()))
        .format(instant);
  }

  private static String textOrDash(String value) {
    return value == null || value.isBlank() ? "\u2014" : value;
  }

  /** The data-status filter's options, with the label each carries in the toolbar. */
  public enum DataStatusFilter {
    ALL("All", null),
    AVAILABLE("Available", DataStatus.AVAILABLE),
    WAITING("Waiting", DataStatus.WAITING);

    private final String label;
    private final DataStatus status;

    DataStatusFilter(String label, DataStatus status) {
      this.label = label;
      this.status = status;
    }

    public String label() {
      return label;
    }

    public DataStatus status() {
      return status;
    }
  }

  /**
   * Bridges the page's segment to the read model's {@link life.qbic.projectmanagement.application.query.AssayType}
   * without the view having to construct enum names by string.
   */
  private record AssayTypeValue(
      life.qbic.projectmanagement.application.query.AssayType value) {

    private AssayTypeValue(MeasurementDatasetSegment segment) {
      this(life.qbic.projectmanagement.application.query.AssayType.valueOf(segment.apiValue()));
    }
  }
}
