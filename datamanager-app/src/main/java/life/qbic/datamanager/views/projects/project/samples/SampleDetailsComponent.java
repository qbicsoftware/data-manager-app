package life.qbic.datamanager.views.projects.project.samples;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
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
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.QuerySortOrder;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.selection.MultiSelectionEvent;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.shared.Registration;
import java.io.InputStream;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import life.qbic.application.commons.FileNameFormatter;
import life.qbic.application.commons.time.DateTimeFormat;
import life.qbic.datamanager.files.export.download.DownloadStreamProvider;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.UiHandle;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.datamanager.views.general.Tag;
import life.qbic.datamanager.views.general.download.DownloadComponent;
import life.qbic.datamanager.views.general.pagination.ListState;
import life.qbic.datamanager.views.general.pagination.ListStateCodec;
import life.qbic.datamanager.views.general.pagination.PaginatedGrid;
import life.qbic.datamanager.views.general.pagination.PaginationBar;
import life.qbic.datamanager.views.general.pagination.Selection;
import life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.api.AsyncProjectService;
import life.qbic.projectmanagement.application.api.AsyncProjectService.SamplePreviewFilter;
import life.qbic.projectmanagement.application.sample.SamplePreview;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.sample.Sample;
import life.qbic.projectmanagement.domain.model.sample.SampleId;
import org.jspecify.annotations.NonNull;
import org.springframework.util.MimeType;

/**
 * Sample Details Component
 * <p>
 * Component embedded within the {@link SampleInformationMain}. It allows the user to see the
 * information associated for all {@link Sample} of each {@link Experiment} within a
 * {@link Project} Additionally it enables the user to register and edit {@link Sample} via the
 * contained
 * {@link
 * life.qbic.datamanager.views.projects.project.samples.registration.batch.RegisterSampleDialog}.
 * <p>
 * The sample list is rendered by a reusable {@link PaginatedGrid} (FEAT-PAG-LIST-02): only the
 * current page is fetched and rendered, a pager reports location and total, and an identifier-based
 * cross-page {@link life.qbic.datamanager.views.general.pagination.Selection} of sample IDs survives
 * page, filter, and sort changes. The component drives the grid in externally-controlled mode so the
 * search field, the action buttons, and the selection bar share one toolbar row next to the search
 * field (matching the measurement lists). Bulk actions (export / edit / delete) apply to the full
 * cross-page selection. The list state is mirrored into the browser URL by the owning route view
 * (USER-R-03).
 */
public class SampleDetailsComponent extends PageArea implements Serializable {

  @Serial
  private static final long serialVersionUID = 2893730975944372088L;

  private static final Logger log = LoggerFactory.logger(SampleDetailsComponent.class);
  private final transient AsyncProjectService asyncProjectService;
  private final MessageSourceNotificationFactory messageFactory;
  private final UiHandle uiHandle = new UiHandle();

  private final DownloadComponent downloadComponent = new DownloadComponent();

  private final AtomicReference<String> clientTimeZone = new AtomicReference<>("UTC");

  /**
   * Banner shown above the grid when a page load failed, so a failed load is never rendered as
   * "no samples registered".
   */
  private final Div errorState = new Div();
  private PaginatedGrid<SamplePreview> paginatedGrid;
  private final String projectId;
  private final String experimentId;
  private final String projectCode;
  private final TextField searchField = new TextField();

  /**
   * Master-detail wrapper: holds the grid and, while a record is open, the detail panel beside it.
   */
  private final Div detailLayout = new Div();

  /**
   * The full record of the currently open sample. Read-only: mutation stays with the bulk actions.
   */
  private final SampleDetailPanel detailPanel = new SampleDetailPanel();

  /**
   * Backdrop for the drawer mode (below the inline breakpoint). Present but hidden while the panel
   * is an inline column, where it must not block the list.
   */
  private final Div detailBackdrop = new Div();

  /**
   * Registration for the Esc shortcut, tracked so it is removed when the panel closes and never
   * fires while no record is open.
   */
  private ShortcutRegistration escShortcut;

  /**
   * The *Details* button of the row that opened the panel, so focus can be returned to it on close.
   */
  private final AtomicReference<Button> detailsButtonInvoker = new AtomicReference<>();
  private final Icon selectionIcon = VaadinIcon.CHECK_SQUARE_O.create();
  private final Span selectionDisplay = new Span();
  private final Button selectAllResultsButton = new Button();
  private final Button clearSelectionButton = new Button("Clear selection");
  private final Div selectionBar = new Div(selectionIcon, selectionDisplay, selectAllResultsButton,
      clearSelectionButton);
  private long totalItemsActive;
  private final PaginationBar paginationBar =
      new PaginationBar(ListStateCodec.ALLOWED_PAGE_SIZES, ListStateCodec.DEFAULT_PAGE_SIZE,
          "samples");
  private final Button exportButton = new Button("Export", VaadinIcon.DOWNLOAD.create());
  private final Button editButton = new Button("Edit", VaadinIcon.EDIT.create());
  private final Button deleteButton = new Button("Delete", VaadinIcon.TRASH.create());

  public SampleDetailsComponent(
      @NonNull AsyncProjectService asyncProjectService,
      @NonNull MessageSourceNotificationFactory messageFactory,
      @NonNull Context context) {
    this.messageFactory = Objects.requireNonNull(messageFactory);
    this.asyncProjectService = Objects.requireNonNull(asyncProjectService);
    this.projectId = context.projectId().orElseThrow().value();
    this.experimentId = context.experimentId().orElseThrow().value();
    this.projectCode = context.projectCode().orElseThrow();
    add(downloadComponent);
    addClassNames("sample-details-component", "sample-details-content");

    addAttachListener(event -> {
      uiHandle.bind(event.getUI());
      event.getUI().getPage().getExtendedClientDetails().refresh(
          receiver -> {
            clientTimeZone.set(receiver.getTimeZoneId());
            // The client time zone arrives asynchronously, potentially after the first page has
            // already rendered; re-render the rows so the date/time columns reflect the client zone.
            paginatedGrid.refreshItems();
          });
    });

    addDetachListener(ignored -> uiHandle.unbind());

    Grid<SamplePreview> sampleGrid = createSamplePreviewGrid();
    // Externally controlled mode: the owning component builds its own toolbar (search + actions),
    // selection bar and pager so all controls sit in one row next to the search field, matching
    // the measurement lists. The grid still owns page loading, clamping and the empty state.
    paginatedGrid = new PaginatedGrid<>(sampleGrid, this::loadPage,
        preview -> preview.sampleId().value(), "sample", SampleSort.DEFAULT, false, false, false);
    paginatedGrid.addClassNames("width-full");

    configureSearch();
    configureSort(sampleGrid);
    configureSelectionReconciliation(sampleGrid);
    configureSelectionBar();
    configureActions();
    configurePagination();

    paginatedGrid.addPageLoadedListener(event -> {
      totalItemsActive = event.getTotal();
      paginationBar.setListState(event.getPage(), event.getTotal(),
          paginatedGrid.listState().pageSize());
      paginationBar.setVisible(event.getTotal() > 0);
      applySelectionToGrid();
      updateSelectionBar();
    });
    paginatedGrid.addSelectionChangeListener(event -> updateSelectionBar());

    Div toolbar = createToolbar(sampleGrid);
    configureErrorState();
    detailLayout.addClassName("sample-detail-layout");
    // Backdrop for the drawer mode only. It lives inside the layout so the stylesheet can scope it
    // to the open state, and it is position:fixed, so it never becomes a grid item.
    detailBackdrop.addClassName("sample-detail-backdrop");
    detailBackdrop.addClickListener(event -> closeDetail());
    detailLayout.add(paginatedGrid, detailPanel, detailBackdrop);
    detailPanel.addCloseListener(event -> closeDetail());
    add(toolbar, selectionBar, errorState, detailLayout, paginationBar);

    updateSelectionBar();
  }

  /**
   * Builds the failure banner shown above the grid when a page load failed. The banner is the
   * inline counterpart to the global error page: the page stays usable, the failure is stated, and
   * a retry re-runs the load.
   */
  private void configureErrorState() {
    errorState.addClassName("sample-error-state");
    errorState.setVisible(false);
    errorState.getElement().setAttribute("role", "alert");

    Span message = new Span("Samples could not be loaded.");
    message.addClassName("sample-error-state-message");

    Button retry = new Button("Try again");
    retry.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    retry.addClassName("sample-error-state-retry");
    retry.addClickListener(event -> paginatedGrid.refresh());

    errorState.add(message, retry);
  }

  private Div createToolbar(Grid<SamplePreview> grid) {
    searchField.addClassName("sample-search");
    Div toolbar = new Div(searchField, exportButton, editButton, deleteButton);
    toolbar.addClassName("sample-toolbar");
    Div toolbarRight = new Div();
    toolbarRight.addClassName("sample-toolbar-right");
    toolbarRight.add(showHideColumnsMenu(grid));
    toolbar.add(toolbarRight);
    return toolbar;
  }

  private void configureSelectionBar() {    selectionIcon.addClassName("sample-selection-icon");
    selectionDisplay.addClassName("sample-selection-count");
    selectAllResultsButton.addClassName("sample-select-all-results");
    clearSelectionButton.addClassName("sample-clear-selection");
    selectionBar.addClassName("sample-selection-bar");
    selectionBar.setVisible(false);
    clearSelectionButton.addClickListener(ignored -> {
      paginatedGrid.deselect(Set.copyOf(paginatedGrid.selectedIds()));
      applySelectionToGrid();
    });
    selectAllResultsButton.addClickListener(ignored -> selectAllMatching());
  }

  private void configureSearch() {
    searchField.setPlaceholder("Search samples");
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

  private void configureSort(Grid<SamplePreview> grid) {
    grid.setMultiSort(false);
    grid.addSortListener(event -> {
      List<GridSortOrder<SamplePreview>> orders = grid.getSortOrder();
      if (orders.isEmpty()) {
        return;
      }
      GridSortOrder<SamplePreview> order = orders.getFirst();
      String property = sortPropertyOf(order);
      if (property == null || property.isBlank()) {
        return;
      }
      boolean descending = order.getDirection() == SortDirection.DESCENDING;
      life.qbic.application.commons.SortOrder sortOrder =
          new life.qbic.application.commons.SortOrder(property, descending);
      if (!SampleSort.allowedSortOrders().contains(sortOrder)) {
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
    var column = order.getSorted();
    return column.getSortOrder(order.getDirection())
        .findFirst()
        .map(QuerySortOrder::getSorted)
        .orElse(null);
  }

  /**
   * Translates grid row selection changes into the identifier-based cross-page {@link Selection}.
   * Only client-side changes are translated (ADR-0009): the grid is driven externally, so the
   * PaginatedGrid does not wire its own reconciliation; server-side deselection events Vaadin fires
   * when a new page is written must not purge off-page selections — the rows are reconciled against
   * the identifier set by {@link #applySelectionToGrid} on every page load instead.
   */
  private void configureSelectionReconciliation(Grid<?> grid) {
    @SuppressWarnings("unchecked")
    Grid<Object> objectGrid = (Grid<Object>) grid;
    objectGrid.addSelectionListener(event -> {
      if (!event.isFromClient()) {
        return;
      }
      MultiSelectionEvent<Grid<Object>, Object> multi =
          (MultiSelectionEvent<Grid<Object>, Object>) event;
      multi.getAddedSelection().forEach(item -> paginatedGrid.select(
          Set.of(((SamplePreview) item).sampleId().value())));
      multi.getRemovedSelection().forEach(item -> paginatedGrid.deselect(
          Set.of(((SamplePreview) item).sampleId().value())));
    });
  }

  private void configureActions() {
    exportButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    exportButton.addClickListener(clicked -> onExportClicked());

    editButton.addClickListener(clicked -> onEditClicked());

    deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
    deleteButton.addClickListener(clicked -> onDeleteClicked());
  }

  private void configurePagination() {
    paginationBar.addChangeListener(event -> {
      ListState current = paginatedGrid.listState();
      ListState requested;
      if (event.getPageSize() != current.pageSize()) {
        requested = current.withPageSize(event.getPageSize()).withPage(1);
      } else {
        requested = current.withPage(event.getPage());
      }
      if (requested.equals(current)) {
        return;
      }
      paginatedGrid.setListState(requested);
    });
  }

  /**
   * Builds a "Show/Hide Columns" menu over the given grid's columns, matching the control on the
   * measurement lists.
   */
  private static <T> MenuBar showHideColumnsMenu(Grid<T> grid) {
    MenuBar menuBar = new MenuBar();
    Span itemContent = new Span(new Span("Show/Hide Columns"), VaadinIcon.CHEVRON_DOWN.create());
    var menuItem = menuBar.addItem(itemContent);
    var subMenu = menuItem.getSubMenu();
    CheckboxGroup<Column<T>> checkboxGroup = new CheckboxGroup<>();
    checkboxGroup.setItemLabelGenerator(Column::getHeaderText);
    List<Column<T>> columns = grid.getColumns().stream()
        .filter(column -> column.getHeaderText() != null && !column.getHeaderText().isBlank())
        .toList();
    checkboxGroup.setItems(columns);
    checkboxGroup.setValue(columns.stream().filter(Column::isVisible).collect(Collectors.toSet()));
    checkboxGroup.addThemeVariants(CheckboxGroupVariant.LUMO_VERTICAL);
    checkboxGroup.addClassNames("flex-vertical");
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

  private void updateSelectionBar() {
    int count = paginatedGrid.selectedIds().size();
    boolean hasSelection = count > 0;
    selectionBar.setVisible(hasSelection);
    // scope disambiguation: when the selection covers the whole filtered result set, say so
    // explicitly instead of showing a bare count next to the pager total
    if (count > 0 && count == totalItemsActive) {
      selectionDisplay.setText(count == 1
          ? "The single sample matching the filter is selected"
          : "All %d samples matching the filter are selected".formatted(count));
      selectAllResultsButton.setVisible(false);
    } else {
      selectionDisplay.setText(count == 1
          ? "1 sample is selected"
          : "%d samples are selected".formatted(count));
      // Gmail-style inline context action: extend a partial selection to every result matching the
      // active filter (cross-page), only while the selection is still partial.
      boolean offerSelectAll = count > 0 && count < totalItemsActive;
      selectAllResultsButton.setVisible(offerSelectAll);
      if (offerSelectAll) {
        selectAllResultsButton.setText("Select all %d matching samples".formatted(totalItemsActive));
      }
    }
    clearSelectionButton.setVisible(hasSelection);
    updateActionButtons();
  }

  /**
   * Selects every sample matching the active filter (cross-page), resolving all matching sample IDs
   * in backend storage.
   */
  private void selectAllMatching() {
    ListState state = paginatedGrid.listState();
    SamplePreviewFilter filter = new SamplePreviewFilter(state.filter(), apiSortOrders(state));
    int total = asyncProjectService.countSamples(projectId, experimentId, filter).blockOptional()
        .orElse(0);
    Set<String> ids = new HashSet<>(asyncProjectService.getSamplePreviews(projectId, experimentId,
            0, total,
            filter)
        .map(preview -> preview.sampleId().value())
        .collectList().blockOptional().orElse(List.of()));
    paginatedGrid.select(ids);
    applySelectionToGrid();
    updateSelectionBar();
  }

  private void updateActionButtons() {
    boolean hasSelection = !paginatedGrid.selectedIds().isEmpty();
    exportButton.setEnabled(hasSelection);
    // editing is scope-agnostic: the edit dialog always offers to download all sample metadata and
    // additionally the selected samples when a selection exists (mirroring the measurement edit
    // dialog), so it is always available regardless of the current selection.
    editButton.setEnabled(true);
    deleteButton.setEnabled(hasSelection);
  }

  private void applySelectionToGrid() {
    paginatedGrid.grid().getGenericDataView().getItems().forEach(item -> {
      if (paginatedGrid.selectedIds().contains(item.sampleId().value())) {
        paginatedGrid.grid().select(item);
      } else {
        paginatedGrid.grid().deselect(item);
      }
    });
  }

  private void onExportClicked() {
    Set<String> selectedSampleIds = paginatedGrid.selectedIds();
    if (selectedSampleIds.isEmpty()) {
      messageFactory.toast("sample.no-sample-selected", new Object[]{}, getLocale())
          .open();
      return;
    }
    triggerSampleMetadataDownload(selectedSampleIds, projectId, experimentId, projectCode);
  }

  private void onEditClicked() {
    List<SampleId> selectedSampleIds = selectedSampleIds().stream()
        .toList();
    // An empty selection is allowed: the edit dialog defaults to downloading all sample metadata
    // and offers a "download selected" convenience template only when a selection exists.
    fireEvent(new SampleEditRequested(selectedSampleIds, this, true));
  }

  private void onDeleteClicked() {
    List<SampleId> selectedSampleIds = selectedSampleIds().stream()
        .toList();
    if (selectedSampleIds.isEmpty()) {
      messageFactory.toast("sample.no-sample-selected", new Object[]{}, getLocale()).open();
      return;
    }
    fireEvent(new SampleDeletionRequested(selectedSampleIds, this, true));
  }

  private Set<SampleId> selectedSampleIds() {
    return paginatedGrid.selectedIds().stream()
        .map(SampleId::parse)
        .collect(Collectors.toSet());
  }

  private ComponentRenderer<Div, SamplePreview> createDetailsRenderer() {
    return new ComponentRenderer<>(preview -> {
      Button details = new Button(VaadinIcon.ANGLE_RIGHT.create());
      details.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
      details.addClassName("sample-details-button");
      // The label carries the sample code so a screen-reader row list stays meaningful: a bare
      // "Open details" repeated per row would not say which sample it belongs to.
      details.getElement().setAttribute("aria-label",
          "Open details for " + preview.sampleCode());
      details.addClickListener(event -> {
        detailsButtonInvoker.set(details);
        openDetail(preview);
      });
      Div wrapper = new Div(details);
      wrapper.addClassName("sample-details-cell");
      return wrapper;
    });
  }

  /**
   * Opens the detail panel for the given sample. The panel is independent of the grid's bulk
   * selection: opening a record never adds it to the selection, so a subsequent bulk delete cannot
   * hit a sample the user only inspected.
   */
  private void openDetail(SamplePreview preview) {
    detailPanel.show(preview, instant -> formatTime(instant, DateTimeFormat.SIMPLE_DATE_TIME_SHORT));
    detailLayout.addClassName("sample-detail-layout--open");
    // Esc closes the record. The listener is attached only while a record is open, so the key is
    // free for the page's other controls at all other times, and it is removed on close to avoid
    // accumulating registrations.
    if (escShortcut != null) {
      escShortcut.remove();
    }
    escShortcut = Shortcuts.addShortcutListener(this,
        event -> closeDetail(), Key.ESCAPE);
  }

  /**
   * Closes the detail panel and returns focus to the *Details* button of the row that opened it,
   * so keyboard users keep their place in the list.
   */
  private void closeDetail() {
    detailPanel.close();
    detailLayout.removeClassName("sample-detail-layout--open");
    if (escShortcut != null) {
      escShortcut.remove();
      escShortcut = null;
    }
    Button invoker = detailsButtonInvoker.getAndSet(null);
    if (invoker != null) {
      invoker.getElement().executeJs("this.focus()");
    }
  }

  private static ComponentRenderer<Div, SamplePreview> createConditionRenderer() {
    return new ComponentRenderer<>(SampleDetailsComponent::createTagCollection,
        SampleDetailsComponent::fillTagCollection);
  }

  private static void fillTagCollection(Div div, SamplePreview samplePreview) {
    samplePreview
        .experimentalGroup()
        .condition()
        .getVariableLevels().stream()
        .map(variableLevel -> "%s: %s %s".formatted(variableLevel.variableName().value(),
                variableLevel.experimentalValue().value(),
                variableLevel.experimentalValue().unit().orElse(""))
            .trim())
        .map(s -> {
          Tag tag = new Tag(s);
          tag.setTitle(s);
          return tag;
        })
        .forEach(div::add);
  }

  private static Div createTagCollection() {
    Div tagCollection = new Div();
    tagCollection.addClassName("tag-collection");
    return tagCollection;
  }

  private Grid<SamplePreview> createSamplePreviewGrid() {
    Grid<SamplePreview> sampleGrid = new Grid<>();
    // Identity columns only. The full record is read in the detail panel, so the grid stays
    // scannable: the omitted columns are constant per experiment (species, specimen, analyte,
    // analysis method) or are audit values (registration/modification time). They remain
    // reachable through the Show/Hide Columns menu, which is built from the grid's own columns.
    var sampleIdColumn = sampleGrid.addColumn(SamplePreview::sampleCode)
        .setHeader("Sample ID")
        .setSortProperty("sampleId")
        .setComparator(SamplePreview::sampleCode)
        .setAutoWidth(true)
        .setFlexGrow(0)
        .setTooltipGenerator(SamplePreview::sampleCode)
        .setFrozen(true);
    sampleGrid.addColumn(SamplePreview::sampleName)
        .setHeader("Sample name")
        .setSortProperty("sampleName")
        .setTooltipGenerator(SamplePreview::sampleName)
        .setAutoWidth(true)
        .setResizable(true);
    sampleGrid.addColumn(createConditionRenderer())
        .setHeader("Condition")
        .setSortProperty("condition")
        .setAutoWidth(true)
        .setResizable(true);
    sampleGrid.addColumn(SamplePreview::batchLabel)
        .setHeader("Batch")
        .setSortProperty("batch")
        .setTooltipGenerator(SamplePreview::batchLabel)
        .setAutoWidth(true)
        .setResizable(true);
    sampleGrid.addColumn(createDetailsRenderer())
        .setHeader("")
        .setAutoWidth(true)
        .setFlexGrow(0);
    sampleGrid.addClassName("sample-grid");
    sampleGrid.setColumnReorderingAllowed(true);
    // The columns retired from the default view stay available through the Show/Hide Columns menu,
    // so a power user can restore the flat table without a separate surface. They are appended
    // hidden: the grid is created with the identity set visible.
    addHiddenDetailColumns(sampleGrid);
    sampleGrid.sort(GridSortOrder.asc(sampleIdColumn).build());
    return sampleGrid;
  }

  /**
   * Appends the columns retired from the default identity view, hidden. They keep their sort
   * properties and tooltips so that restoring them via the Show/Hide Columns menu yields the same
   * table the page showed before the rework.
   */
  private void addHiddenDetailColumns(Grid<SamplePreview> sampleGrid) {
    sampleGrid.addColumn(SamplePreview::biologicalReplicate)
        .setHeader("Biological Replicate")
        .setSortProperty("biologicalReplicate")
        .setTooltipGenerator(SamplePreview::biologicalReplicate)
        .setAutoWidth(true)
        .setResizable(true)
        .setVisible(false);
    sampleGrid.addColumn(preview -> preview.species().getLabel())
        .setHeader("Species")
        .setSortProperty("species")
        .setTooltipGenerator(preview -> preview.species().formatted())
        .setAutoWidth(true)
        .setResizable(true)
        .setVisible(false);
    sampleGrid.addColumn(preview -> preview.specimen().getLabel())
        .setHeader("Specimen")
        .setSortProperty("specimen")
        .setTooltipGenerator(preview -> preview.specimen().formatted())
        .setAutoWidth(true)
        .setVisible(false);
    sampleGrid.addColumn(preview -> preview.analyte().getLabel())
        .setHeader("Analyte")
        .setSortProperty("analyte")
        .setTooltipGenerator(preview -> preview.analyte().formatted())
        .setAutoWidth(true)
        .setResizable(true)
        .setVisible(false);
    sampleGrid.addColumn(preview -> preview.analysisMethod().label())
        .setHeader("Analysis to Perform")
        .setSortProperty("analysisMethod")
        .setTooltipGenerator(samplePreview -> samplePreview.analysisMethod().label())
        .setAutoWidth(true)
        .setResizable(true)
        .setVisible(false);
    sampleGrid.addColumn(preview -> formatTime(preview.registrationTime(),
            DateTimeFormat.SIMPLE_DATE_TIME_SHORT))
        .setHeader("Registration time")
        .setSortProperty("registrationTime")
        .setComparator(SamplePreview::registrationTime)
        .setAutoWidth(true)
        .setResizable(true)
        .setVisible(false);
    sampleGrid.addColumn(preview -> formatTime(preview.lastModified(),
            DateTimeFormat.SIMPLE_DATE_TIME_SHORT))
        .setHeader("Modification time")
        .setSortProperty("lastModified")
        .setComparator(SamplePreview::lastModified)
        .setAutoWidth(true)
        .setResizable(true)
        .setVisible(false);
    sampleGrid.addColumn(SamplePreview::comment)
        .setHeader("Comment")
        .setSortProperty("comment")
        .setTooltipGenerator(SamplePreview::comment)
        .setAutoWidth(true)
        .setResizable(true)
        .setVisible(false);
  }

  private String formatTime(Instant instant, DateTimeFormat dateTimeFormat) {
    if (instant == null) {
      return "";
    }
    return DateTimeFormat.asJavaFormatter(dateTimeFormat, ZoneId.of(clientTimeZone.get()))
        .format(instant);
  }

  /**
   * Loads a single page of samples for the {@link PaginatedGrid}. Translates the list state into
   * the backend lookup and returns the page items together with the total count of samples matching
   * the active filter.
   */
  private PaginatedGrid.Page<SamplePreview> loadPage(ListState state) {
    int offset = (state.page() - 1) * state.pageSize();
    int limit = state.pageSize();
    SamplePreviewFilter filter = new SamplePreviewFilter(state.filter(), apiSortOrders(state));
    try {
      int total = asyncProjectService.countSamples(projectId, experimentId, filter)
          .blockOptional()
          .orElseThrow(() -> new IllegalStateException("sample count returned no value"));
      List<SamplePreview> page = asyncProjectService.getSamplePreviews(projectId, experimentId,
              offset, limit, filter)
          .collectList()
          .blockOptional()
          .orElseThrow(() -> new IllegalStateException("sample page returned no value"));
      errorState.setVisible(false);
      return new PaginatedGrid.Page<>(page, total);
    } catch (RuntimeException e) {
      // A backend failure used to be swallowed into an empty page, which made a failed load
      // indistinguishable from "no samples registered". Surface it as an error state instead: the
      // grid renders empty, but the banner above it says the load failed and offers a retry.
      log.error("Could not load the sample page", e);
      errorState.setVisible(true);
      return new PaginatedGrid.Page<>(List.of(), 0);
    }
  }

  /**
   * Translates a list state's sort into the API sort orders. The backend applies the orders as
   * given, so the deterministic sample-code tie-break is appended to keep offset/limit pagination
   * stable across page boundaries (equal sort values would otherwise duplicate or drop rows).
   */
  private static List<life.qbic.projectmanagement.application.api.AsyncProjectService.SortOrder<AsyncProjectService.SamplePreviewSortKey>> apiSortOrders(
      ListState state) {
    List<life.qbic.projectmanagement.application.api.AsyncProjectService.SortOrder<AsyncProjectService.SamplePreviewSortKey>> sortOrders =
        new java.util.ArrayList<>();
    sortOrders.add(SampleSort.toApiSortOrder(state.sort()));
    if (!state.sort().propertyName().equals("sampleId")) {
      sortOrders.add(new life.qbic.projectmanagement.application.api.AsyncProjectService.SortOrder<>(
          AsyncProjectService.SamplePreviewSortKey.SAMPLE_ID,
          life.qbic.projectmanagement.application.api.AsyncProjectService.SortDirection.ASC));
    }
    return sortOrders;
  }

  private void triggerSampleMetadataDownload(
      @NonNull Set<String> sampleIds,
      String projectId,
      String experimentId,
      String projectCode) {

    var pendingTaskToast = messageFactory.pendingTaskToast("sample.fetching-metadata",
        new Object[]{sampleIds.size()}, getLocale());
    pendingTaskToast.open();
    asyncProjectService.sampleInformationTemplate(projectId, experimentId, sampleIds,
            MimeType.valueOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .subscribe(digitalObject -> uiHandle
            .onUiAndPush(() -> {
              pendingTaskToast.close();
              messageFactory.toast("sample.metadata-fetched", new Object[]{sampleIds.size()},
                  getLocale()).open();
              downloadComponent.trigger(new DownloadStreamProvider() {
                @Override
                public String getFilename() {
                  return FileNameFormatter.formatWithTimestampedSimple(LocalDate.now(), projectCode,
                      "sample_metadata", "xlsx");
                }

                @Override
                public InputStream getStream() {
                  return digitalObject.content();
                }

                @Override
                public String getContentType() {
                  return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                }

                @Override
                public Optional<Long> contentLength() {
                  return Optional.empty();
                }
              });
            }));
  }

  /**
   * Applies an externally provided list state (initial load, URL back/forward, shared link) and
   * reloads the page. The search field is kept in sync so it reflects the restored filter.
   */
  public void applyExternalState(ListState state) {
    paginatedGrid.applyExternalState(state);
    syncSearchField(state.filter());
  }

  private void syncSearchField(String filter) {
    String value = filter == null ? "" : filter;
    if (!Objects.equals(searchField.getValue(), value)) {
      searchField.setValue(value);
    }
  }

  /**
   * Reloads the current page with the current list state (e.g. after registration, edit or
   * deletion).
   */
  public void refresh() {
    paginatedGrid.refresh();
  }

  /**
   * @return the currently applied list state
   */
  public ListState listState() {
    return paginatedGrid.listState();
  }

  /**
   * Registers a listener notified whenever a page is loaded, so the owning route view can mirror
   * the list state into the URL.
   */
  public void addPageLoadedListener(ComponentEventListener<PaginatedGrid.PageLoadedEvent> listener) {
    paginatedGrid.addPageLoadedListener(listener);
  }

  /**
   * @return the wrapped {@link PaginatedGrid}
   */
  public PaginatedGrid<SamplePreview> paginatedGrid() {
    return paginatedGrid;
  }

  /**
   * Test seams: exposes the selection-bar controls so specs can pin the cross-page selection
   * affordances (count, "select all N matching", clear) without a running UI. Package-private, not
   * part of the public API.
   */
  Button selectAllResultsButton() {
    return selectAllResultsButton;
  }

  Span selectionDisplay() {
    return selectionDisplay;
  }

  Button clearSelectionButton() {
    return clearSelectionButton;
  }

  /**
   * Register a {@link ComponentEventListener} that gets informed with a {@link SampleEditRequested}
   * as soon as a user wants to edit samples.
   *
   * @param listener a listener on the sample edit trigger
   */
  public Registration addSampleEditListener(ComponentEventListener<SampleEditRequested> listener) {
    return addListener(SampleEditRequested.class, listener);
  }

  /**
   * Register a {@link ComponentEventListener} that gets informed with a
   * {@link SampleDeletionRequested} as soon as a user wants to delete selected samples.
   *
   * @param listener a listener on the sample deletion trigger
   */
  public Registration addSampleDeletionListener(
      ComponentEventListener<SampleDeletionRequested> listener) {
    return addListener(SampleDeletionRequested.class, listener);
  }

  /**
   * <b>Sample Edit Requested</b>
   *
   * <p>Indicates that a user wants to edit the samples within the {@link SampleDetailsComponent} of
   * a project.</p>
   */
  public static class SampleEditRequested extends ComponentEvent<SampleDetailsComponent> {

    @Serial
    private static final long serialVersionUID = -2696352875376621564L;
    private final List<SampleId> sampleIds;

    public SampleEditRequested(List<SampleId> sampleIds, SampleDetailsComponent source,
        boolean fromClient) {
      super(source, fromClient);
      this.sampleIds = sampleIds;
    }

    public List<SampleId> sampleIds() {
      return sampleIds;
    }
  }

  /**
   * <b>Sample Deletion Requested</b>
   *
   * <p>Indicates that a user wants to delete the selected samples within the
   * {@link SampleDetailsComponent} of a project.</p>
   */
  public static class SampleDeletionRequested extends ComponentEvent<SampleDetailsComponent> {

    @Serial
    private static final long serialVersionUID = 3051871590996074855L;
    private final List<SampleId> sampleIds;

    public SampleDeletionRequested(List<SampleId> sampleIds, SampleDetailsComponent source,
        boolean fromClient) {
      super(source, fromClient);
      this.sampleIds = sampleIds;
    }

    public List<SampleId> sampleIds() {
      return sampleIds;
    }
  }
}
