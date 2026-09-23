package life.qbic.datamanager.views.projects.project.rawdata;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.grid.GridMultiSelectionModel;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.selection.MultiSelectionEvent;
import com.vaadin.flow.data.selection.SelectionListener;
import com.vaadin.flow.data.value.ValueChangeMode;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.FileNameFormatter;
import life.qbic.application.commons.FileSizeFormatter;
import life.qbic.application.commons.SortOrder;
import life.qbic.application.commons.time.DateTimeFormat;
import life.qbic.datamanager.files.export.download.DownloadStreamProvider;
import life.qbic.datamanager.files.export.rawdata.RawDataUrlFile;
import life.qbic.datamanager.files.export.rawdata.RawDataUrlFile.RawDataURL;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.GridDetailsItem;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.datamanager.views.general.download.DownloadComponent;
import life.qbic.datamanager.views.general.pagination.ListState;
import life.qbic.datamanager.views.general.pagination.PaginatedGrid;
import life.qbic.datamanager.views.general.pagination.Selection;
import life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory;
import life.qbic.datamanager.views.projects.project.rawdata.pagination.RawDataDomain;
import life.qbic.datamanager.views.projects.project.rawdata.pagination.RawDataListState;
import life.qbic.datamanager.views.projects.project.rawdata.pagination.RawDataSort;
import life.qbic.datamanager.views.projects.project.rawdata.pagination.RawDataTabPagination;
import life.qbic.datamanager.views.projects.project.rawdata.pagination.RawDataTabPagination.RefreshRequestedEvent;
import life.qbic.projectmanagement.application.api.AsyncProjectService;
import life.qbic.projectmanagement.application.api.AsyncProjectService.BasicSampleInformation;
import life.qbic.projectmanagement.application.api.AsyncProjectService.RawDatasetFilter;
import life.qbic.projectmanagement.application.api.AsyncProjectService.RawDatasetInformationIp;
import life.qbic.projectmanagement.application.api.AsyncProjectService.RawDatasetInformationNgs;
import life.qbic.projectmanagement.application.api.AsyncProjectService.RawDatasetInformationPxP;
import org.jspecify.annotations.NonNull;
import org.springframework.util.MimeTypeUtils;

/**
 * Raw Data Details Component (FEAT-PAG-LIST-04, USER-R-01/-R-02/-R-03).
 *
 * <p>Shows the registered raw data of the three measurement domains (genomics / proteomics /
 * immunopeptidomics) as paginated in-memory grids inside a {@link RawDataTabPagination}: only
 * the current page is fetched and rendered, a shared pager reports location and total, and a
 * view-owned {@link Selection} of measurement IDs survives page, filter, and sort changes.
 * The "Export Dataset URLs" action applies to the full cross-page selection, so a user can select
 * datasets across several pages and export exactly those URLs. The "Select all N matching the
 * active filter" action resolves all matching measurement IDs in backend storage.</p>
 */
public class RawDataDetailsComponent extends PageArea implements Serializable {

  private static final Duration MAX_BLOCKING_DURATION = Duration.ofMinutes(5);
  private static final DateTimeFormat RAW_DATA_DATE_TIME_FORMAT = DateTimeFormat.ISO_LOCAL_DATE;
  private static final Map<RawDataDomain, String> TAB_LABELS = Map.of(
      RawDataDomain.NGS, "Genomics",
      RawDataDomain.PXP, "Proteomics",
      RawDataDomain.IP, "Immunopeptidomics");

  private final transient AsyncProjectService asyncProjectService;
  private final DownloadComponent downloadComponent = new DownloadComponent();
  private final String dataSourceEndpoint;
  private final MessageSourceNotificationFactory messageFactory;
  private final AtomicReference<String> clientTimeZone = new AtomicReference<>("UTC");

  private final RawDataTabPagination tabPagination;
  private final Grid<RawDatasetInformationNgs> ngsGrid = createNgsRawDataGrid();
  private final PaginatedGrid<RawDatasetInformationNgs> ngsPaginatedGrid;
  private final Grid<RawDatasetInformationPxP> pxpGrid = createPxpRawDataGrid();
  private final PaginatedGrid<RawDatasetInformationPxP> pxpPaginatedGrid;
  private final Grid<RawDatasetInformationIp> ipGrid = createIpRawDataGrid();
  private final PaginatedGrid<RawDatasetInformationIp> ipPaginatedGrid;
  private final TextField ngsSearchField = searchField();
  private final TextField pxpSearchField = searchField();
  private final TextField ipSearchField = searchField();
  private final Map<RawDataDomain, Button> exportButtons = new EnumMap<>(RawDataDomain.class);
  private final Selection ngsSelection = new Selection(() -> updateSelectionBar());
  private final Selection pxpSelection = new Selection(() -> updateSelectionBar());
  private final Selection ipSelection = new Selection(() -> updateSelectionBar());

  private Context context;

  public RawDataDetailsComponent(
      @NonNull AsyncProjectService asyncProjectService,
      @NonNull Context context,
      @NonNull String dataSourceEndpoint,
      @NonNull MessageSourceNotificationFactory messageFactory) {
    this.asyncProjectService = Objects.requireNonNull(asyncProjectService);
    this.dataSourceEndpoint = Objects.requireNonNull(dataSourceEndpoint);
    this.messageFactory = Objects.requireNonNull(messageFactory);
    this.context = Objects.requireNonNull(context);

    addClassName("raw-data-details-component");
    add(downloadComponent);

    tabPagination = new RawDataTabPagination();

    // The three raw data grids are wrapped in the reusable PaginatedGrid (FEAT-PAG-LIST-04).
    // They are driven externally — their own toolbar and pager are suppressed and the shared
    // pager/selection bar of the tab container stay authoritative — so each PaginatedGrid only
    // owns page loading, clamping and the empty state for its tab.
    ngsPaginatedGrid = new PaginatedGrid<>(ngsGrid, this::loadNgsPage,
        info -> info.dataset().measurementId(), "dataset", RawDataSort.DEFAULT, false, false,
        false);
    ngsPaginatedGrid.addPageLoadedListener(event -> {
      applySelectionToGrid(ngsGrid, ngsSelection, RawDataDomain.NGS);
      tabPagination.onPageLoaded(RawDataDomain.NGS, event.getPage(), event.getTotal());
    });
    pxpPaginatedGrid = new PaginatedGrid<>(pxpGrid, this::loadPxpPage,
        info -> info.dataset().measurementId(), "dataset", RawDataSort.DEFAULT, false, false,
        false);
    pxpPaginatedGrid.addPageLoadedListener(event -> {
      applySelectionToGrid(pxpGrid, pxpSelection, RawDataDomain.PXP);
      tabPagination.onPageLoaded(RawDataDomain.PXP, event.getPage(), event.getTotal());
    });
    ipPaginatedGrid = new PaginatedGrid<>(ipGrid, this::loadIpPage,
        info -> info.dataset().measurementId(), "dataset", RawDataSort.DEFAULT, false, false,
        false);
    ipPaginatedGrid.addPageLoadedListener(event -> {
      applySelectionToGrid(ipGrid, ipSelection, RawDataDomain.IP);
      tabPagination.onPageLoaded(RawDataDomain.IP, event.getPage(), event.getTotal());
    });

    tabPagination.addTab(TAB_LABELS.get(RawDataDomain.NGS), RawDataDomain.NGS, ngsTabContent());
    tabPagination.addTab(TAB_LABELS.get(RawDataDomain.PXP), RawDataDomain.PXP, pxpTabContent());
    tabPagination.addTab(TAB_LABELS.get(RawDataDomain.IP), RawDataDomain.IP, ipTabContent());
    tabPagination.attachSelectionBar();
    tabPagination.setSelection(ngsSelection);
    tabPagination.addRefreshRequestedListener(this::onRefreshRequested);
    tabPagination.addSelectionClearedListener(event -> applySelectionToAllGrids());
    tabPagination.addSelectAllResultsListener(event -> selectAllMatching(event.domain()));
    add(tabPagination);

    configureSearch(ngsSearchField, RawDataDomain.NGS);
    configureSearch(pxpSearchField, RawDataDomain.PXP);
    configureSearch(ipSearchField, RawDataDomain.IP);
    configureSortListener(ngsGrid, RawDataDomain.NGS);
    configureSortListener(pxpGrid, RawDataDomain.PXP);
    configureSortListener(ipGrid, RawDataDomain.IP);
    configureSelectionReconciliation(ngsGrid, ngsSelection, RawDataDomain.NGS);
    configureSelectionReconciliation(pxpGrid, pxpSelection, RawDataDomain.PXP);
    configureSelectionReconciliation(ipGrid, ipSelection, RawDataDomain.IP);

    updateSelectionBar();
  }

  @Override
  protected void onAttach(AttachEvent attachEvent) {
    super.onAttach(attachEvent);
    attachEvent.getUI().getPage().getExtendedClientDetails().refresh(
        receiver -> clientTimeZone.set(receiver.getTimeZoneId()));
  }

  private @NonNull String formatTime(Instant instant, DateTimeFormat dateTimeFormat) {
    return DateTimeFormat.asJavaFormatter(dateTimeFormat, ZoneId.of(clientTimeZone.get()))
        .format(instant);
  }

  // ---- tab content ---------------------------------------------------------

  private Component ngsTabContent() {
    return tabContent(ngsSearchField, RawDataDomain.NGS, this::exportNgs, ngsPaginatedGrid);
  }

  private Component pxpTabContent() {
    return tabContent(pxpSearchField, RawDataDomain.PXP, this::exportPxp, pxpPaginatedGrid);
  }

  private Component ipTabContent() {
    return tabContent(ipSearchField, RawDataDomain.IP, this::exportIp, ipPaginatedGrid);
  }

  private Component tabContent(TextField searchField, RawDataDomain domain, Runnable exporter,
      Component body) {
    Div toolbar = new Div();
    toolbar.addClassName("rawdata-tab-toolbar");
    searchField.addClassName("rawdata-search");
    Button exportButton = new Button("Export Dataset URLs", VaadinIcon.DOWNLOAD.create());
    exportButton.addClassName("rawdata-export");
    exportButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    exportButtons.put(domain, exportButton);
    exportButton.addClickListener(clicked -> exporter.run());
    toolbar.add(searchField, exportButton);

    Div content = new Div();
    content.addClassName("rawdata-tab-content");
    content.add(toolbar, body);
    return content;
  }

  // ---- search / sort / selection wiring -------------------------------------

  private static TextField searchField() {
    TextField field = new TextField();
    field.setSuffixComponent(VaadinIcon.SEARCH.create());
    field.getElement().setAttribute("aria-label", "Search raw datasets");
    return field;
  }

  private void configureSearch(TextField field, RawDataDomain domain) {
    field.setPlaceholder("Search raw datasets");
    field.setClearButtonVisible(true);
    field.setValueChangeMode(ValueChangeMode.LAZY);
    field.addValueChangeListener(event -> tabPagination.applySearch(domain, event.getValue()));
  }

  private void configureSortListener(Grid<?> grid, RawDataDomain domain) {
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
      if (!RawDataSort.isValid(sortOrder)) {
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
    return column.getSortOrder(order.getDirection())
        .findFirst()
        .map(querySortOrder -> querySortOrder.getSorted())
        .orElse(null);
  }

  private void configureSelectionReconciliation(Grid<?> grid, Selection selection,
      RawDataDomain domain) {
    grid.setSelectionMode(Grid.SelectionMode.MULTI);
    if (grid.getSelectionModel() instanceof GridMultiSelectionModel<?> multiSelectionModel) {
      multiSelectionModel.setSelectionColumnFrozen(true);
    }
    @SuppressWarnings("unchecked")
    Grid<Object> objectGrid = (Grid<Object>) grid;
    objectGrid.addSelectionListener(createSelectionReconciliationListener(objectGrid, selection,
        domain));
  }

  /**
   * Builds the selection listener that translates grid row selection changes into the
   * identifier-based {@link Selection}.
   *
   * <p>Only client-side changes are translated (USER-R-02, ADR-0009): the selection is a
   * cross-page, cross-tab view-owned set, and server-side selection events are fired by
   * Vaadin itself whenever a new page is written to the grid via {@code setItems} (the data
   * provider change deselects every row). Translating those synthetic events would purge every
   * selected measurement that is not on the newly rendered page — the rows are reconciled with
   * the identifier set afterwards by {@link #applySelectionToGrid} instead.</p>
   */
  static SelectionListener<Grid<Object>, Object> createSelectionReconciliationListener(
      Grid<Object> grid, Selection selection, RawDataDomain domain) {
    return event -> {
      if (!event.isFromClient()) {
        return;
      }
      MultiSelectionEvent<Grid<Object>, Object> multi =
          (MultiSelectionEvent<Grid<Object>, Object>) event;
      multi.getAddedSelection().forEach(item -> selection.select(measurementIdOf(domain, item)));
      multi.getRemovedSelection().forEach(item -> selection.deselect(measurementIdOf(domain, item)));
    };
  }

  private static String measurementIdOf(RawDataDomain domain, Object item) {
    return switch (domain) {
      case NGS -> ((RawDatasetInformationNgs) item).dataset().measurementId();
      case PXP -> ((RawDatasetInformationPxP) item).dataset().measurementId();
      case IP -> ((RawDatasetInformationIp) item).dataset().measurementId();
    };
  }

  // ---- page loading / refresh -----------------------------------------------

  private void onRefreshRequested(RefreshRequestedEvent event) {
    RawDataDomain domain = event.domain();
    if (context == null) {
      return;
    }
    updateTabCounts();
    ListState state = tabPagination.listState().stateOf(domain);
    tabPagination.setSelection(selectionFor(domain));
    syncSearchField(domain, state.filter());
    loadAndRender(domain, state, event.scrollGridTopIntoView());
  }

  // per-tab totals as label badges so users see which domains hold raw data
  private void updateTabCounts() {
    String experimentId = context.experimentId().orElseThrow().value();
    String projectId = context.projectId().orElseThrow().value();
    for (RawDataDomain domain : RawDataDomain.values()) {
      long total = switch (domain) {
        case NGS -> asyncProjectService.countRawDataNgs(projectId, experimentId,
            new RawDatasetFilter("", List.of())).blockOptional(MAX_BLOCKING_DURATION).orElse(0);
        case PXP -> asyncProjectService.countRawDataPxp(projectId, experimentId,
            new RawDatasetFilter("", List.of())).blockOptional(MAX_BLOCKING_DURATION).orElse(0);
        case IP -> asyncProjectService.countRawDataIp(projectId, experimentId,
            new RawDatasetFilter("", List.of())).blockOptional(MAX_BLOCKING_DURATION).orElse(0);
      };
      tabPagination.setTabLabel(domain, "%s (%d)".formatted(TAB_LABELS.get(domain), total));
    }
  }

  private void syncSearchField(RawDataDomain domain, String filter) {
    TextField field = switch (domain) {
      case NGS -> ngsSearchField;
      case PXP -> pxpSearchField;
      case IP -> ipSearchField;
    };
    if (!Objects.equals(field.getValue(), filter)) {
      field.setValue(filter == null ? "" : filter);
    }
  }

  private void loadAndRender(RawDataDomain domain, ListState state,
      boolean scrollGridTopIntoView) {
    switch (domain) {
      case NGS -> {
        ngsPaginatedGrid.setListState(state);
        if (scrollGridTopIntoView) {
          ngsGrid.getElement().executeJs(
              "requestAnimationFrame(() => this.scrollIntoView({block: 'start'}))");
        }
      }
      case PXP -> {
        pxpPaginatedGrid.setListState(state);
        if (scrollGridTopIntoView) {
          pxpGrid.getElement().executeJs(
              "requestAnimationFrame(() => this.scrollIntoView({block: 'start'}))");
        }
      }
      case IP -> {
        ipPaginatedGrid.setListState(state);
        if (scrollGridTopIntoView) {
          ipGrid.getElement().executeJs(
              "requestAnimationFrame(() => this.scrollIntoView({block: 'start'}))");
        }
      }
    }
  }

  private RawDatasetFilter rawDataFilter(ListState state) {
    return new RawDatasetFilter(state.filter(), RawDataSort.toApiSortOrders(state.sort()));
  }

  /**
   * Loads a single NGS page for the reusable {@link PaginatedGrid} (the {@code PageLoader} of
   * the NGS grid).
   */
  private PaginatedGrid.Page<RawDatasetInformationNgs> loadNgsPage(ListState state) {
    String experimentId = context.experimentId().orElseThrow().value();
    String projectId = context.projectId().orElseThrow().value();
    int offset = (state.page() - 1) * state.pageSize();
    int limit = state.pageSize();
    RawDatasetFilter filter = rawDataFilter(state);
    int total = asyncProjectService.countRawDataNgs(projectId, experimentId, filter)
        .blockOptional(MAX_BLOCKING_DURATION).orElse(0);
    List<RawDatasetInformationNgs> page = asyncProjectService
        .getRawDatasetInformationNgs(projectId, experimentId, offset, limit, filter)
        .collectList().blockOptional(MAX_BLOCKING_DURATION).orElse(List.of());
    return new PaginatedGrid.Page<>(page, total);
  }

  /**
   * Loads a single Proteomics (PxP) page for the reusable {@link PaginatedGrid}.
   */
  private PaginatedGrid.Page<RawDatasetInformationPxP> loadPxpPage(ListState state) {
    String experimentId = context.experimentId().orElseThrow().value();
    String projectId = context.projectId().orElseThrow().value();
    int offset = (state.page() - 1) * state.pageSize();
    int limit = state.pageSize();
    RawDatasetFilter filter = rawDataFilter(state);
    int total = asyncProjectService.countRawDataPxp(projectId, experimentId, filter)
        .blockOptional(MAX_BLOCKING_DURATION).orElse(0);
    List<RawDatasetInformationPxP> page = asyncProjectService
        .getRawDatasetInformationPxP(projectId, experimentId, offset, limit, filter)
        .collectList().blockOptional(MAX_BLOCKING_DURATION).orElse(List.of());
    return new PaginatedGrid.Page<>(page, total);
  }

  /**
   * Loads a single Immunopeptidomics (IP) page for the reusable {@link PaginatedGrid}.
   */
  private PaginatedGrid.Page<RawDatasetInformationIp> loadIpPage(ListState state) {
    String experimentId = context.experimentId().orElseThrow().value();
    String projectId = context.projectId().orElseThrow().value();
    int offset = (state.page() - 1) * state.pageSize();
    int limit = state.pageSize();
    RawDatasetFilter filter = rawDataFilter(state);
    int total = asyncProjectService.countRawDataIp(projectId, experimentId, filter)
        .blockOptional(MAX_BLOCKING_DURATION).orElse(0);
    List<RawDatasetInformationIp> page = asyncProjectService
        .getRawDatasetInformationIp(projectId, experimentId, offset, limit, filter)
        .collectList().blockOptional(MAX_BLOCKING_DURATION).orElse(List.of());
    return new PaginatedGrid.Page<>(page, total);
  }

  @SuppressWarnings("unchecked")
  private <T> void applySelectionToGrid(Grid<T> grid, Selection selection,
      RawDataDomain domain) {
    grid.getGenericDataView().getItems().toList().forEach(item -> {
      String id = measurementIdOf(domain, item);
      if (selection.contains(id)) {
        grid.select(item);
      } else {
        grid.deselect(item);
      }
    });
  }

  private void applySelectionToAllGrids() {
    applySelectionToGrid(ngsGrid, ngsSelection, RawDataDomain.NGS);
    applySelectionToGrid(pxpGrid, pxpSelection, RawDataDomain.PXP);
    applySelectionToGrid(ipGrid, ipSelection, RawDataDomain.IP);
  }

  private void updateSelectionBar() {
    if (tabPagination != null) {
      tabPagination.updateSelectionBar();
    }
    for (RawDataDomain domain : RawDataDomain.values()) {
      Button exportButton = exportButtons.get(domain);
      if (exportButton != null) {
        exportButton.setEnabled(selectionFor(domain).count() > 0);
      }
    }
  }

  // ---- selection helpers ----------------------------------------------------

  private Selection selectionFor(RawDataDomain domain) {
    return switch (domain) {
      case NGS -> ngsSelection;
      case PXP -> pxpSelection;
      case IP -> ipSelection;
    };
  }

  private void selectAllMatching(RawDataDomain domain) {
    if (context == null) {
      return;
    }
    ListState state = tabPagination.listState().stateOf(domain);
    String projectId = context.projectId().orElseThrow().value();
    String experimentId = context.experimentId().orElseThrow().value();
    RawDatasetFilter filter = rawDataFilter(state);
    List<String> ids = switch (domain) {
      case NGS -> {
        int total = asyncProjectService.countRawDataNgs(projectId, experimentId, filter)
            .blockOptional(MAX_BLOCKING_DURATION).orElse(0);
        yield asyncProjectService
            .getRawDatasetInformationNgs(projectId, experimentId, 0, total, filter)
            .map(info -> info.dataset().measurementId())
            .collectList().blockOptional(MAX_BLOCKING_DURATION).orElse(List.of());
      }
      case PXP -> {
        int total = asyncProjectService.countRawDataPxp(projectId, experimentId, filter)
            .blockOptional(MAX_BLOCKING_DURATION).orElse(0);
        yield asyncProjectService
            .getRawDatasetInformationPxP(projectId, experimentId, 0, total, filter)
            .map(info -> info.dataset().measurementId())
            .collectList().blockOptional(MAX_BLOCKING_DURATION).orElse(List.of());
      }
      case IP -> {
        int total = asyncProjectService.countRawDataIp(projectId, experimentId, filter)
            .blockOptional(MAX_BLOCKING_DURATION).orElse(0);
        yield asyncProjectService
            .getRawDatasetInformationIp(projectId, experimentId, 0, total, filter)
            .map(info -> info.dataset().measurementId())
            .collectList().blockOptional(MAX_BLOCKING_DURATION).orElse(List.of());
      }
    };
    selectionFor(domain).select(Set.copyOf(ids));
    Grid<?> grid = switch (domain) {
      case NGS -> ngsGrid;
      case PXP -> pxpGrid;
      case IP -> ipGrid;
    };
    applySelectionToGrid(grid, selectionFor(domain), domain);
    tabPagination.updateSelectionBar();
  }

  // ---- export ---------------------------------------------------------------

  private void exportNgs() {
    exportUrlFile(ngsSelection, "ngs_measurement_dataset_locations");
  }

  private void exportPxp() {
    exportUrlFile(pxpSelection, "proteomics_measurement_dataset_locations");
  }

  private void exportIp() {
    exportUrlFile(ipSelection, "immunopeptidomics_measurement_dataset_locations");
  }

  private void exportUrlFile(Selection selection, String fileNamePrefix) {
    List<String> measurementIds = new ArrayList<>(selection.selectedIds());
    if (measurementIds.isEmpty()) {
      displayMissingSelectionNote();
      return;
    }
    var projectCode = context.projectCode().orElseThrow();
    var urls = measurementIds.stream()
        .map(id -> new RawDataURL(dataSourceEndpoint, id))
        .toList();
    var file = RawDataUrlFile.create(urls);
    var streamProvider = createStreamProvider(FileNameFormatter.formatWithTimestampedSimple(
        LocalDate.now(), projectCode, fileNamePrefix, "txt"), file);
    downloadComponent.trigger(streamProvider);
  }

  private void displayMissingSelectionNote() {
    messageFactory.toast("rawdata.no-dataset-selected", new Object[]{}, getLocale())
        .open();
  }

  private static DownloadStreamProvider createStreamProvider(String filename, RawDataUrlFile file) {
    return new DownloadStreamProvider() {
      @Override
      public String getFilename() {
        return filename;
      }

      @Override
      public InputStream getStream() {
        return new BufferedInputStream(
            new ByteArrayInputStream(file.getBytes(StandardCharsets.UTF_8)));
      }

      @Override
      public String getContentType() {
        return MimeTypeUtils.TEXT_PLAIN_VALUE;
      }

      @Override
      public Optional<Long> contentLength() {
        return Optional.of((long) file.getBytes(StandardCharsets.UTF_8).length);
      }
    };
  }

  // ---- context --------------------------------------------------------------

  /**
   * Sets the context of the component. The tab layout is built once; changing the context only
   * re-fetches the active tab's page. When the experiment changes, per-tab state is reset and the
   * tabs are re-hidden/showed according to whether raw datasets exist.
   */
  public void setContext(Context context) {
    Objects.requireNonNull(context, "context must not be null");
    context.projectId().orElseThrow(
        () -> new ApplicationException("Context must contain the project id"));
    context.experimentId().orElseThrow(
        () -> new ApplicationException("Context must contain the experiment id"));
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
      RawDataListState defaults = RawDataListState.defaultWith(RawDataDomain.NGS);
      tabPagination.applyExternalState(defaults);
    } else {
      tabPagination.refreshActiveTab();
    }
    refreshTabVisibility();
  }

  private void refreshTabVisibility() {
    String projectId = context.projectId().orElseThrow().value();
    String experimentId = context.experimentId().orElseThrow().value();
    tabPagination.setTabVisible(RawDataDomain.NGS,
        asyncProjectService.countRawDataNgs(projectId, experimentId,
            new RawDatasetFilter("", List.of())).blockOptional(MAX_BLOCKING_DURATION).orElse(0) > 0);
    tabPagination.setTabVisible(RawDataDomain.PXP,
        asyncProjectService.countRawDataPxp(projectId, experimentId,
            new RawDatasetFilter("", List.of())).blockOptional(MAX_BLOCKING_DURATION).orElse(0) > 0);
    tabPagination.setTabVisible(RawDataDomain.IP,
        asyncProjectService.countRawDataIp(projectId, experimentId,
            new RawDatasetFilter("", List.of())).blockOptional(MAX_BLOCKING_DURATION).orElse(0) > 0);
  }

  /**
   * @return the pagination container, so the route view can drive URL parsing/history.
   */
  public RawDataTabPagination getTabPagination() {
    return tabPagination;
  }

  /**
   * Delegates the route base path to the container so it can mirror the list state into the URL.
   */
  public void setBasePath(String basePath) {
    tabPagination.setBasePath(basePath);
  }

  // ---- grids ----------------------------------------------------------------

  private Grid<RawDatasetInformationNgs> createNgsRawDataGrid() {
    Grid<RawDatasetInformationNgs> grid = new Grid<>();
    grid.addClassName("raw-data-grid");
    var measurementIdColumn = grid.addColumn(
            rawData -> rawData.dataset().measurementId())
        .setKey("measurementId")
        .setSortProperty("measurementId")
        .setHeader("Measurement Id");

    grid.addColumn(RawDatasetInformationNgs::measurementName)
        .setHeader("Measurement Name")
        .setSortable(false);

    grid.addColumn(
            rawData -> rawData.linkedSampleInformation().stream().map(
                BasicSampleInformation::sampleName).collect(Collectors.joining(",")))
        .setKey("sampleName")
        .setHeader("Sample Name")
        .setSortable(false);
    grid.addColumn(rawData -> formatTime(rawData.dataset().registrationDate(),
            RAW_DATA_DATE_TIME_FORMAT))
        .setKey("uploadDate")
        .setSortProperty("uploadDate")
        .setHeader("Upload Date");
    grid.setItemDetailsRenderer(renderRawDataNgs());
    grid.sort(GridSortOrder.asc(measurementIdColumn).build());
    return grid;
  }

  private Grid<RawDatasetInformationPxP> createPxpRawDataGrid() {
    Grid<RawDatasetInformationPxP> grid = new Grid<>();
    grid.addClassName("raw-data-grid");
    var measurementIdColumn = grid.addColumn(
            rawData -> rawData.dataset().measurementId())
        .setKey("measurementId")
        .setSortProperty("measurementId")
        .setHeader("Measurement Id");

    grid.addColumn(RawDatasetInformationPxP::measurementName)
        .setHeader("Measurement Name")
        .setSortable(false);

    grid.addColumn(
            rawData -> rawData.linkedSampleInformation().stream().map(
                BasicSampleInformation::sampleName).collect(Collectors.joining(",")))
        .setKey("sampleName")
        .setHeader("Sample Name");
    grid.addColumn(
            rawData -> formatTime(rawData.dataset().registrationDate(), RAW_DATA_DATE_TIME_FORMAT))
        .setKey("uploadDate")
        .setSortProperty("uploadDate")
        .setHeader("Upload Date");
    grid.setItemDetailsRenderer(renderRawDataPxp());
    grid.sort(GridSortOrder.asc(measurementIdColumn).build());
    return grid;
  }

  private Grid<RawDatasetInformationIp> createIpRawDataGrid() {
    Grid<RawDatasetInformationIp> grid = new Grid<>();
    grid.addClassName("raw-data-grid");
    var measurementIdColumn = grid.addColumn(
            rawData -> rawData.dataset().measurementId())
        .setKey("measurementId")
        .setSortProperty("measurementId")
        .setHeader("Measurement Id");

    grid.addColumn(RawDatasetInformationIp::measurementName)
        .setHeader("Measurement Name")
        .setSortable(false);

    grid.addColumn(
            rawData -> rawData.linkedSampleInformation().stream().map(
                BasicSampleInformation::sampleName).collect(Collectors.joining(",")))
        .setKey("sampleName")
        .setHeader("Sample Name");
    grid.addColumn(
            rawData -> formatTime(rawData.dataset().registrationDate(), RAW_DATA_DATE_TIME_FORMAT))
        .setKey("uploadDate")
        .setSortProperty("uploadDate")
        .setHeader("Upload Date");
    grid.setItemDetailsRenderer(renderRawDataIp());
    grid.sort(GridSortOrder.asc(measurementIdColumn).build());
    return grid;
  }

  private ComponentRenderer<GridDetailsItem, RawDatasetInformationNgs> renderRawDataNgs() {
    return new ComponentRenderer<>(rawData -> {
      GridDetailsItem rawDataItem = new GridDetailsItem();
      rawDataItem.addListEntry("Sample Name(s)", rawData.linkedSampleInformation().stream().map(
          BasicSampleInformation::sampleName).toList());
      rawDataItem.addEntry("Number of Files",
          String.valueOf(rawData.dataset().numberOfFiles()));
      rawDataItem.addEntry("File Size", FileSizeFormatter.formatBytes(rawData.dataset().totalSizeBytes()));
      rawDataItem.addListEntry("File Suffixes", rawData.dataset().fileTypes());
      return rawDataItem;
    });
  }

  private ComponentRenderer<GridDetailsItem, RawDatasetInformationPxP> renderRawDataPxp() {
    return new ComponentRenderer<>(rawData -> {
      GridDetailsItem rawDataItem = new GridDetailsItem();
      rawDataItem.addListEntry("Sample Name(s)", rawData.linkedSampleInformation().stream().map(
          BasicSampleInformation::sampleName).toList());
      rawDataItem.addEntry("Number of Files",
          String.valueOf(rawData.dataset().numberOfFiles()));
      rawDataItem.addEntry("File Size", FileSizeFormatter.formatBytes(rawData.dataset().totalSizeBytes()));
      rawDataItem.addListEntry("File Suffixes", rawData.dataset().fileTypes());
      return rawDataItem;
    });
  }

  private ComponentRenderer<GridDetailsItem, RawDatasetInformationIp> renderRawDataIp() {
    return new ComponentRenderer<>(rawData -> {
      GridDetailsItem rawDataItem = new GridDetailsItem();
      rawDataItem.addListEntry("Sample Name(s)", rawData.linkedSampleInformation().stream().map(
          BasicSampleInformation::sampleName).toList());
      rawDataItem.addEntry("Number of Files",
          String.valueOf(rawData.dataset().numberOfFiles()));
      rawDataItem.addEntry("File Size", FileSizeFormatter.formatBytes(rawData.dataset().totalSizeBytes()));
      rawDataItem.addListEntry("File Suffixes", rawData.dataset().fileTypes());
      return rawDataItem;
    });
  }
}