package life.qbic.datamanager.views.projects.project.rawdata;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.data.provider.CallbackDataProvider.CountCallback;
import com.vaadin.flow.data.provider.CallbackDataProvider.FetchCallback;
import com.vaadin.flow.data.provider.QuerySortOrder;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
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
import life.qbic.application.commons.FileNameFormatter;
import life.qbic.application.commons.time.DateTimeFormat;
import life.qbic.datamanager.files.export.download.DownloadStreamProvider;
import life.qbic.datamanager.files.export.rawdata.RawDataUrlFile;
import life.qbic.datamanager.files.export.rawdata.RawDataUrlFile.RawDataURL;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.GridDetailsItem;
import life.qbic.datamanager.views.UiHandle;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.datamanager.views.general.download.DownloadComponent;
import life.qbic.datamanager.views.general.grid.component.FilterGrid;
import life.qbic.datamanager.views.general.grid.component.FilterGridConfigurations;
import life.qbic.datamanager.views.general.grid.component.FilterGridTab;
import life.qbic.datamanager.views.general.grid.component.FilterGridTabSheet;
import life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory;
import life.qbic.projectmanagement.application.api.AsyncProjectService;
import life.qbic.projectmanagement.application.api.AsyncProjectService.BasicSampleInformation;
import life.qbic.projectmanagement.application.api.AsyncProjectService.RawDataSortingKey;
import life.qbic.projectmanagement.application.api.AsyncProjectService.RawDatasetFilter;
import life.qbic.projectmanagement.application.api.AsyncProjectService.RawDatasetInformationNgs;
import life.qbic.projectmanagement.application.api.AsyncProjectService.RawDatasetInformationPxP;
import life.qbic.projectmanagement.application.api.AsyncProjectService.SortDirection;
import life.qbic.projectmanagement.application.api.AsyncProjectService.SortOrder;
import org.springframework.lang.NonNull;

/**
 * Raw Data Details Component
 * <p></p>
 * Enables the user to manage the registered RawData by providing the ability to access and search
 * the raw data, and enabling them to download the raw data of interest
 */

public class RawDataDetailsComponent extends PageArea implements Serializable {

  private static final Duration MAX_BLOCKING_DURATION = Duration.ofMinutes(5);

  private static final DateTimeFormat RAW_DATA_DATE_TIME_FORMAT = DateTimeFormat.ISO_LOCAL_DATE;
  private final AsyncProjectService asyncProjectService;
  private final DownloadComponent downloadComponent = new DownloadComponent();
  private final UiHandle uiHandle = new UiHandle();
  private final String dataSourceEndpoint;
  private final MessageSourceNotificationFactory messageFactory;
  private final Context context;
  private final AtomicReference<String> clientTimeZone = new AtomicReference<>("UTC");
  private final AtomicInteger clientTimeZoneOffset = new AtomicInteger(0);

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

    // Vaadin requires the download component to be attached to the UI for the download trigger to work
    add(downloadComponent);

    // Hooks the current UI during attach events for safe UI-thread task execution
    addAttachListener(event -> {
      event.getUI().getPage().retrieveExtendedClientDetails(receiver -> {
        clientTimeZone.set(receiver.getTimeZoneId());
        clientTimeZoneOffset.set(receiver.getTimezoneOffset());
      });
      uiHandle.bind(event.getUI());
    });
    // Frees the UI reference from the handler
    addDetachListener(ignored -> uiHandle.unbind());

    var projectId = context.projectId().orElseThrow().value();
    var experimentId = context.experimentId().orElseThrow().value();

    final FilterGridTabSheet filterTabSheet = new FilterGridTabSheet();
    filterTabSheet.removeAllTabs();
    if (asyncProjectService.countRawDataNgs(projectId, experimentId,
        new RawDatasetFilter("", List.of())).block(MAX_BLOCKING_DURATION) > 0) {
      var filterGridNgs = createNgsFilterGrid(createNgsRawDataGrid(), projectId, experimentId);
      addNgsTab(filterTabSheet, 0, "Genomics", filterGridNgs);
    }
    if (asyncProjectService.countRawDataPxp(projectId, experimentId,
        new RawDatasetFilter("", List.of())).block(MAX_BLOCKING_DURATION) > 0) {
      var filterGridPxp = createPxpFilterGrid(createPxpRawDataGrid(), projectId, experimentId);
      addPxpTab(filterTabSheet, 1, "Proteomics", filterGridPxp);
    }
    filterTabSheet.hidePrimaryFeatureButton();
    filterTabSheet.setCaptionPrimaryAction("Export Dataset URLs");
    add(filterTabSheet);

  }

  private void addNgsTab(FilterGridTabSheet tabSheet, int index, String name,
      FilterGrid<RawDatasetInformationNgs, ?> filterGrid) {
    var projectCode = context.projectCode().orElseThrow();
    var ngsTab = new FilterGridTab<>(name, filterGrid);
    tabSheet.addTab(index, ngsTab);
    tabSheet.addPrimaryAction(ngsTab, tab -> {
      var grid = tab.filterGrid();
      Set<RawDatasetInformationNgs> selectedDatasets = grid.selectedElements();
      if (selectedDatasets.isEmpty()) {
        displayMissingSelectionNote();
        return;
      }
      var ids = selectedDatasets.stream()
          .map(info -> info.dataset().measurementId())
          .map(id -> new RawDataURL(dataSourceEndpoint, id))
          .toList();

      var file = RawDataUrlFile.create(ids);
      var streamProvider = createStreamProvider(FileNameFormatter.formatWithTimestampedSimple(
          LocalDate.now(), projectCode, "proteomics_measurement_dataset_locations", "txt"), file);
      downloadComponent.trigger(streamProvider);
    });
  }

  private void addPxpTab(FilterGridTabSheet tabSheet, int index, String name,
      FilterGrid<RawDatasetInformationPxP, ?> filterGrid) {
    var projectCode = context.projectCode().orElseThrow();
    var pxpTab = new FilterGridTab<>(name, filterGrid);
    tabSheet.addTab(index, pxpTab);
    tabSheet.addPrimaryAction(pxpTab, tab -> {
      var grid = tab.filterGrid();
      Set<RawDatasetInformationPxP> selectedDatasets = grid.selectedElements();
      if (selectedDatasets.isEmpty()) {
        displayMissingSelectionNote();
        return;
      }
      var ids = selectedDatasets.stream()
          .map(info -> info.dataset().measurementId())
          .map(id -> new RawDataURL(dataSourceEndpoint, id))
          .toList();

      var file = RawDataUrlFile.create(ids);
      var streamProvider = createStreamProvider(FileNameFormatter.formatWithTimestampedSimple(
          LocalDate.now(), projectCode, "ngs_measurement_dataset_locations", "txt"), file);
      downloadComponent.trigger(streamProvider);
    });
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
    };
  }


  private static final Map<UiSortKey, RawDataSortingKey> SORT_KEY_MAP = new EnumMap<>(
      UiSortKey.class);

  static {
    SORT_KEY_MAP.put(UiSortKey.SAMPLE_NAME, RawDataSortingKey.SAMPLE_NAME);
    SORT_KEY_MAP.put(UiSortKey.MEASUREMENT_ID, RawDataSortingKey.MEASUREMENT_ID);
    SORT_KEY_MAP.put(UiSortKey.UPLOAD_DATE, RawDataSortingKey.UPLOAD_DATE);
  }

  private enum UiSortKey {
    MEASUREMENT_ID("measurementId"),
    SAMPLE_NAME("sampleName"),
    UPLOAD_DATE("uploadDate");


    private static final Map<String, UiSortKey> LOOKUP = Arrays.stream(
        UiSortKey.values()).collect(
        Collectors.toMap(UiSortKey::value, Function.identity()));

    private final String value;

    UiSortKey(String value) {
      this.value = value;
    }

    static Optional<UiSortKey> from(String value) {
      return Optional.ofNullable(LOOKUP.getOrDefault(value, null));
    }

    String value() {
      return value;
    }
  }

  private FilterGrid<RawDatasetInformationPxP, ?> createPxpFilterGrid(
      Grid<RawDatasetInformationPxP> multiSelectGridPxp, String projectId,
      String experimentId) {

    FetchCallback<RawDatasetInformationPxP, RawDataFilter> fetchCallback = query -> {
      var filter = query.getFilter().orElse(new RawDataFilter(""));
      var offset = query.getOffset();
      var limit = query.getLimit();
      var sortOrders = sortOrdersToApi(query.getSortOrders());
      var rawDataFilter = new RawDatasetFilter(filter.searchTerm().orElse(""), sortOrders);

      return asyncProjectService.getRawDatasetInformationPxP(projectId, experimentId,
              offset, limit, rawDataFilter)
          .collectList()
          .blockOptional(MAX_BLOCKING_DURATION)
          .orElse(List.of())
          .stream();
    };

    CountCallback<RawDatasetInformationPxP, RawDataFilter> countCallback = query -> {
      var filter = query.getFilter().orElse(new RawDataFilter(""));
      var sortOrders = sortOrdersToApi(query.getSortOrders());
      var rawDataFilter = new RawDatasetFilter(filter.searchTerm().orElse(""), sortOrders);
      return asyncProjectService.countRawDataPxp(projectId,
              experimentId, rawDataFilter)
          .blockOptional(MAX_BLOCKING_DURATION)
          .orElse(0);
    };

    var pxpGridConfiguration = FilterGridConfigurations.lazy(
        fetchCallback, countCallback);
    var filterGrid = FilterGrid.create(RawDatasetInformationPxP.class,
        RawDataFilter.class,
        pxpGridConfiguration.applyConfiguration(multiSelectGridPxp),
        () -> new RawDataFilter(""),
        (searchTerm, filter) -> new RawDataFilter(searchTerm));

    filterGrid.searchFieldPlaceholder("Search raw datasets");
    filterGrid.itemDisplayLabel("dataset");
    return filterGrid;
  }


  private FilterGrid<RawDatasetInformationNgs, ?> createNgsFilterGrid(
      Grid<RawDatasetInformationNgs> multiSelectNgsGrid, String projectId,
      String experimentId) {

    FetchCallback<RawDatasetInformationNgs, RawDataFilter> fetchCallback = query -> {
      var sortOrders = sortOrdersToApi(query.getSortOrders());
      var filter = query.getFilter().orElse(new RawDataFilter(""));
      var rawDataFilter = new RawDatasetFilter(filter.searchTerm().orElse(""), sortOrders);

      var offset = query.getOffset();
      var limit = query.getLimit();

      return asyncProjectService.getRawDatasetInformationNgs(projectId, experimentId,
              offset, limit, rawDataFilter)
          .collectList()
          .blockOptional(MAX_BLOCKING_DURATION)
          .orElse(List.of())
          .stream();
    };

    CountCallback<RawDatasetInformationNgs, RawDataFilter> countCallback = query -> {
      var sortOrders = sortOrdersToApi(query.getSortOrders());
      var filter = query.getFilter().orElse(new RawDataFilter(""));
      var rawDataFilter = new RawDatasetFilter(filter.searchTerm().orElse(""), sortOrders);

      return asyncProjectService.countRawDataNgs(projectId, experimentId, rawDataFilter)
          .blockOptional(MAX_BLOCKING_DURATION)
          .orElse(0);
    };
    var ngsGridConfiguration = FilterGridConfigurations.lazy(
        fetchCallback, countCallback);
    var filterGrid = FilterGrid.create(RawDatasetInformationNgs.class,
        RawDataFilter.class,
        ngsGridConfiguration.applyConfiguration(multiSelectNgsGrid),
        () -> new RawDataFilter(""),
        (searchTerm, filter) -> new RawDataFilter(searchTerm));

    filterGrid.searchFieldPlaceholder("Search raw datasets");
    filterGrid.itemDisplayLabel("dataset");
    return filterGrid;
  }

  private static List<SortOrder<RawDataSortingKey>> sortOrdersToApi(
      List<QuerySortOrder> uiSortOrders)
      throws IllegalArgumentException {
    return uiSortOrders.stream()
        .map(RawDataDetailsComponent::sortOrdersToApi)
        .toList();
  }

  private static SortDirection sortDirectionToApi(
      com.vaadin.flow.data.provider.SortDirection uiSortDirection) {
    return uiSortDirection == com.vaadin.flow.data.provider.SortDirection.ASCENDING
        ? SortDirection.ASC : SortDirection.DESC;
  }

  private static SortOrder<RawDataSortingKey> sortOrdersToApi(
      QuerySortOrder uiSortOrder)
      throws IllegalArgumentException {
    var uiSortKeyValue = uiSortOrder.getSorted();
    var uiSortKey = UiSortKey.from(uiSortKeyValue).orElseThrow(
        () -> new IllegalArgumentException("No ui sort key provided for value: " + uiSortKeyValue));
    var apiKey = SORT_KEY_MAP.get(uiSortKey);
    if (apiKey == null) {
      throw new IllegalArgumentException("No api key provided for value: " + uiSortKey);
    }
    return new SortOrder<>(apiKey,
        sortDirectionToApi(uiSortOrder.getDirection()));
  }

  private Grid<RawDatasetInformationNgs> createNgsRawDataGrid() {
    Grid<RawDatasetInformationNgs> grid = new Grid<>();
    grid.addClassName("raw-data-grid");
    grid.addColumn(
            rawData -> rawData.dataset().measurementId())
        .setKey(UiSortKey.MEASUREMENT_ID.value())
        .setSortProperty(UiSortKey.MEASUREMENT_ID.value())
        .setHeader("Measurement Id");

    grid.addColumn(
            rawData -> rawData.linkedSampleInformation().stream().map(
                BasicSampleInformation::sampleName).collect(Collectors.joining(",")))
        .setKey(UiSortKey.SAMPLE_NAME.value())
        .setHeader("Sample Name")
        .setSortable(false);
    grid.addColumn(rawData -> formatTime(rawData.dataset().registrationDate()))
        .setKey(UiSortKey.UPLOAD_DATE.value())
        .setSortProperty(UiSortKey.UPLOAD_DATE.value())
        .setHeader("Upload Date");
    grid.setItemDetailsRenderer(renderRawDataNgs());
    return grid;
  }

  private @NonNull String formatTime(Instant instant) {
    String zoneId = clientTimeZone.get();
    return instant.atZone(ZoneId.of(zoneId)).format(DateTimeFormat.asJavaFormatter(
        RAW_DATA_DATE_TIME_FORMAT));
  }

  private Grid<RawDatasetInformationPxP> createPxpRawDataGrid() {
    Grid<RawDatasetInformationPxP> grid = new Grid<>();
    grid.addClassName("raw-data-grid");
    grid.addColumn(
            rawData -> rawData.dataset().measurementId())
        .setKey(UiSortKey.MEASUREMENT_ID.value())
        .setSortProperty(UiSortKey.MEASUREMENT_ID.value())
        .setHeader("Measurement Id");
    grid.addColumn(
            rawData -> rawData.linkedSampleInformation().stream().map(
                BasicSampleInformation::sampleName).collect(Collectors.joining(",")))
        .setKey(UiSortKey.SAMPLE_NAME.value())
        .setHeader("Sample Name");
    grid.addColumn(
            rawData -> formatTime(rawData.dataset().registrationDate()))
        .setKey(UiSortKey.UPLOAD_DATE.value())
        .setSortProperty(UiSortKey.UPLOAD_DATE.value())
        .setHeader("Upload Date");
    grid.setItemDetailsRenderer(renderRawDataPxp());
    return grid;
  }

  private ComponentRenderer<GridDetailsItem, RawDatasetInformationPxP> renderRawDataPxp() {
    return new ComponentRenderer<>(rawData -> {
      GridDetailsItem rawDataItem = new GridDetailsItem();
      rawDataItem.addListEntry("Sample Name(s)", rawData.linkedSampleInformation().stream().map(
          BasicSampleInformation::sampleName).toList());
      rawDataItem.addEntry("Number of Files",
          String.valueOf(rawData.dataset().numberOfFiles()));
      rawDataItem.addEntry("File Size", String.valueOf(rawData.dataset().totalSizeBytes()));
      rawDataItem.addListEntry("File Suffixes", rawData.dataset().fileTypes());
      return rawDataItem;
    });
  }

  private ComponentRenderer<GridDetailsItem, RawDatasetInformationNgs> renderRawDataNgs() {
    return new ComponentRenderer<>(rawData -> {
      GridDetailsItem rawDataItem = new GridDetailsItem();
      rawDataItem.addListEntry("Sample Name(s)", rawData.linkedSampleInformation().stream().map(
          BasicSampleInformation::sampleName).toList());
      rawDataItem.addEntry("Number of Files",
          String.valueOf(rawData.dataset().numberOfFiles()));
      rawDataItem.addEntry("File Size", String.valueOf(rawData.dataset().totalSizeBytes()));
      rawDataItem.addListEntry("File Suffixes", rawData.dataset().fileTypes());
      return rawDataItem;
    });
  }

  private static class RawDataFilter {

    private final String searchTerm;

    public RawDataFilter(@NonNull String searchTerm) {
      this.searchTerm = Objects.requireNonNull(searchTerm);
    }

    public Optional<String> searchTerm() {
      return Optional.ofNullable(searchTerm);
    }
  }
}
