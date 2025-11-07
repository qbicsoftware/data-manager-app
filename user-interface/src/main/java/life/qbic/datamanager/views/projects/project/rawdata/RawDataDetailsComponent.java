package life.qbic.datamanager.views.projects.project.rawdata;

import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.QuerySortOrder;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import life.qbic.application.commons.FileNameFormatter;
import life.qbic.datamanager.ClientDetailsProvider;
import life.qbic.datamanager.files.export.download.DownloadStreamProvider;
import life.qbic.datamanager.files.export.rawdata.RawDataUrlFile;
import life.qbic.datamanager.files.export.rawdata.RawDataUrlFile.RawDataURL;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.GridDetailsItem;
import life.qbic.datamanager.views.UiHandle;
import life.qbic.datamanager.views.general.MultiSelectLazyLoadingGrid;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.datamanager.views.general.download.DownloadComponent;
import life.qbic.datamanager.views.general.grid.Filter;
import life.qbic.datamanager.views.general.grid.component.FilterGrid;
import life.qbic.datamanager.views.general.grid.component.FilterGridTab;
import life.qbic.datamanager.views.general.grid.component.FilterGridTabSheet;
import life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory;
import life.qbic.projectmanagement.application.api.AsyncProjectService;
import life.qbic.projectmanagement.application.api.AsyncProjectService.BasicSampleInformation;
import life.qbic.projectmanagement.application.api.AsyncProjectService.RawDataSortingKey;
import life.qbic.projectmanagement.application.api.AsyncProjectService.RawDatasetFilter;
import life.qbic.projectmanagement.application.api.AsyncProjectService.RawDatasetInformationNgs;
import life.qbic.projectmanagement.application.api.AsyncProjectService.RawDatasetInformationPxP;
import life.qbic.projectmanagement.application.dataset.RemoteRawDataService.RawData;
import life.qbic.projectmanagement.application.dataset.RemoteRawDataService.RawDataSampleInformation;
import org.springframework.lang.NonNull;
import reactor.core.publisher.Mono;

/**
 * Raw Data Details Component
 * <p></p>
 * Enables the user to manage the registered RawData by providing the ability to access and search
 * the raw data, and enabling them to download the raw data of interest
 */

public class RawDataDetailsComponent extends PageArea implements Serializable {

  private final transient ClientDetailsProvider clientDetailsProvider;
  private final AsyncProjectService asyncProjectService;
  private final DownloadComponent downloadComponent = new DownloadComponent();
  private final UiHandle uiHandle = new UiHandle();
  private final String dataSourceEndpoint;
  private final MessageSourceNotificationFactory messageFactory;

  public RawDataDetailsComponent(
      @NonNull ClientDetailsProvider clientDetailsProvider,
      @NonNull AsyncProjectService asyncProjectService,
      @NonNull Context context,
      @NonNull String dataSourceEndpoint,
      @NonNull MessageSourceNotificationFactory messageFactory) {
    this.clientDetailsProvider = Objects.requireNonNull(clientDetailsProvider);
    this.asyncProjectService = Objects.requireNonNull(asyncProjectService);
    this.dataSourceEndpoint = Objects.requireNonNull(dataSourceEndpoint);
    this.messageFactory = Objects.requireNonNull(messageFactory);
    addClassName("raw-data-details-component");

    // Vaadin requires the download component to be attached to the UI for the download trigger to work
    add(downloadComponent);

    // Hooks the current UI during attach events for safe UI-thread task execution
    addAttachListener(event -> uiHandle.bind(event.getUI()));
    // Frees the UI reference from the handler
    addDetachListener(ignored -> uiHandle.unbind());

    var projectId = context.projectId().orElseThrow().value();
    var experimentId = context.experimentId().orElseThrow().value();

    var multiSelectGridNgs = createNgsRawDataGrid();
    var filterGridNgs = createNgsFilterGrid(multiSelectGridNgs, projectId, experimentId);

    var multiSelectGridPxp = createPxpRawDataGrid();
    var filterGridPxp = createPxpFilterGrid(multiSelectGridPxp, projectId, experimentId);

    var filterTabNgs = new FilterGridTab<>("NGS", filterGridNgs);
    var filterTabPxp = new FilterGridTab<>("Proteomics", filterGridPxp);
    var filterTabSheet = new FilterGridTabSheet(filterTabNgs, filterTabPxp);

    asyncProjectService.countMeasurementsNgs(projectId, experimentId, new RawDatasetFilter("", List.of()))
        .onErrorResume(ignored -> Mono.just(0))
        .subscribe(count -> uiHandle.onUiAndPush(() -> filterTabNgs.setItemCount(count)));

    asyncProjectService.countMeasurementsPxp(projectId, experimentId, new RawDatasetFilter("", List.of()))
        .onErrorResume(ignored -> Mono.just(0))
        .subscribe(count -> uiHandle.onUiAndPush(() -> filterTabPxp.setItemCount(count)));

    filterTabSheet.hidePrimaryActionButton();

    filterTabSheet.setCaptionFeatureAction("Export Dataset Locations");

    var projectCode = context.projectCode().orElse("unknown_project_code");

    filterTabSheet.addPrimaryFeatureButtonListener(event -> {

      filterTabSheet.whenSelectedGrid(RawDatasetInformationNgs.class, grid -> {
        var ids = grid.selectedElements().stream()
            .map(info -> info.dataset().measurementId())
            .map(id -> new RawDataURL(dataSourceEndpoint, id))
            .toList();

        if (ids.isEmpty()) {
          displayMissingSelectionNote();
          return;
        }

        var file = RawDataUrlFile.create(ids);
        var streamProvider = createStreamProvider(FileNameFormatter.formatWithTimestampedSimple(
            LocalDate.now(), projectCode, "ngs_measurement_dataset_locations", "txt"), file);
        downloadComponent.trigger(streamProvider);
      });

      filterTabSheet.whenSelectedGrid(RawDatasetInformationPxP.class, grid -> {
        var ids = grid.selectedElements().stream()
            .map(info -> info.dataset().measurementId())
            .map(id -> new RawDataURL(dataSourceEndpoint, id))
            .toList();

        if (ids.isEmpty()) {
          displayMissingSelectionNote();
          return;
        }

        var file = RawDataUrlFile.create(ids);
        var streamProvider = createStreamProvider(FileNameFormatter.formatWithTimestampedSimple(
            LocalDate.now(), projectCode, "proteomics_measurement_dataset_locations", "txt"), file);
        downloadComponent.trigger(streamProvider);
      });

    });

    add(filterTabSheet);
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
        return new BufferedInputStream(new ByteArrayInputStream(file.getBytes(StandardCharsets.UTF_8)));
      }
    };
  }


  private static final Map<RawDataDetailsComponent.UiSortKey, RawDataSortingKey> SORT_KEY_MAP = new EnumMap<>(
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
        RawDataDetailsComponent.UiSortKey.values()).collect(
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

  private FilterGrid<RawDatasetInformationPxP>  createPxpFilterGrid(
      MultiSelectLazyLoadingGrid<RawDatasetInformationPxP> multiSelectGridPxp, String projectId,
      String experimentId) {
    var filterGrid = new FilterGrid<>(RawDatasetInformationPxP.class, multiSelectGridPxp,
        DataProvider.fromFilteringCallbacks(
            query -> {
              var filter = query.getFilter().orElse(new RawDataFilter(""));
              var offset = query.getOffset();
              var limit = query.getLimit();
              var sortOrders = sortOrdersToApi(query.getSortOrders());
              var rawDataFilter = createNgsRawDataFilter(filter, sortOrders);

              return asyncProjectService.getRawDatasetInformationPxP(projectId, experimentId,
                  offset, limit, rawDataFilter).collectList().blockOptional().orElse(List.of()).stream();
            }, query -> {
              var filter = query.getFilter().orElse(new RawDataFilter(""));
              var sortOrders = sortOrdersToApi(query.getSortOrders());
              var rawDataFilter = createNgsRawDataFilter(filter, sortOrders);
              return asyncProjectService.countMeasurementsNgs(projectId, experimentId,
                  rawDataFilter).blockOptional().orElse(0);
            }
        ), new RawDataFilter(""), (filter, term) -> new RawDataFilter(term));
    filterGrid.searchFieldPlaceholder("Search measurements");
    filterGrid.itemDisplayLabel("measurement");
    return filterGrid;
  }


  private FilterGrid<RawDatasetInformationNgs> createNgsFilterGrid(
      MultiSelectLazyLoadingGrid<RawDatasetInformationNgs> multiSelectNgsGrid, String projectId,
      String experimentId) {
    var filterGrid = new FilterGrid<>(RawDatasetInformationNgs.class, multiSelectNgsGrid,
        DataProvider.fromFilteringCallbacks(
            query -> {
              var filter = query.getFilter().orElse(new RawDataFilter(""));
              var offset = query.getOffset();
              var limit = query.getLimit();
              var sortOrders = sortOrdersToApi(query.getSortOrders());
              var rawDataFilter = createNgsRawDataFilter(filter, sortOrders);

              return asyncProjectService.getRawDatasetInformationNgs(projectId, experimentId,
                  offset, limit, rawDataFilter).collectList().blockOptional().orElse(List.of()).stream();
            }, query -> {
              var filter = query.getFilter().orElse(new RawDataFilter(""));
              var sortOrders = sortOrdersToApi(query.getSortOrders());
              var rawDataFilter = createNgsRawDataFilter(filter, sortOrders);
              return asyncProjectService.countMeasurementsNgs(projectId, experimentId,
                  rawDataFilter).blockOptional().orElse(0);
            }
        ), new RawDataFilter(""), (filter, term) -> new RawDataFilter(term));
    filterGrid.searchFieldPlaceholder("Search measurements");
    filterGrid.itemDisplayLabel("measurement");
    return filterGrid;
  }

  private static RawDatasetFilter createNgsRawDataFilter(Filter filter,
      List<AsyncProjectService.SortOrder<RawDataSortingKey>> sortOrders) {
    return new RawDatasetFilter(filter.searchTerm().orElse(""), sortOrders);
  }

  private static List<AsyncProjectService.SortOrder<RawDataSortingKey>> sortOrdersToApi(
      List<com.vaadin.flow.data.provider.QuerySortOrder> uiSortOrders)
      throws IllegalArgumentException {
    return uiSortOrders.stream()
        .map(RawDataDetailsComponent::sortOrdersToApi)
        .toList();
  }

  private static AsyncProjectService.SortDirection sortDirectionToApi(
      com.vaadin.flow.data.provider.SortDirection uiSortDirection) {
    return uiSortDirection == com.vaadin.flow.data.provider.SortDirection.ASCENDING
        ? AsyncProjectService.SortDirection.ASC : AsyncProjectService.SortDirection.DESC;
  }

  private static AsyncProjectService.SortOrder<RawDataSortingKey> sortOrdersToApi(QuerySortOrder uiSortOrder)
      throws IllegalArgumentException {
    var uiSortKeyValue = uiSortOrder.getSorted();
    var uiSortKey = RawDataDetailsComponent.UiSortKey.from(uiSortKeyValue).orElseThrow(
        () -> new IllegalArgumentException("No ui sort key provided for value: " + uiSortKeyValue));
    var apiKey = SORT_KEY_MAP.get(uiSortKey);
    if (apiKey == null) {
      throw new IllegalArgumentException("No api key provided for value: " + uiSortKey);
    }
    return new AsyncProjectService.SortOrder<>(apiKey, sortDirectionToApi(uiSortOrder.getDirection()));
  }

  private MultiSelectLazyLoadingGrid<RawDatasetInformationNgs> createNgsRawDataGrid() {
    MultiSelectLazyLoadingGrid<RawDatasetInformationNgs> grid = new MultiSelectLazyLoadingGrid<>();
    grid.addClassName("raw-data-grid");
    grid.addColumn(
            rawData -> rawData.dataset().measurementId())
        .setKey(UiSortKey.MEASUREMENT_ID.value())
        .setHeader("Measurement Id");
    grid.addColumn(
            rawData -> rawData.linkedSampleInformation().stream().map(
                BasicSampleInformation::sampleName).collect(Collectors.joining(",")))
        .setKey(UiSortKey.SAMPLE_NAME.value())
        .setHeader("Sample Name")
        .setSortable(false);
    grid.addColumn(
            rawData -> convertToLocalDate(Date.from(rawData.dataset().registrationDate())))
        .setKey(UiSortKey.UPLOAD_DATE.value())
        .setHeader("Upload Date");
    grid.setItemDetailsRenderer(renderRawDataNgs());
    return grid;
  }

  private MultiSelectLazyLoadingGrid<RawDatasetInformationPxP> createPxpRawDataGrid() {
    MultiSelectLazyLoadingGrid<RawDatasetInformationPxP> grid = new MultiSelectLazyLoadingGrid<>();
    grid.addClassName("raw-data-grid");
    grid.addColumn(
            rawData -> rawData.dataset().measurementId())
        .setKey(UiSortKey.MEASUREMENT_ID.value())
        .setHeader("Measurement Id");
    grid.addColumn(
            rawData -> rawData.linkedSampleInformation().stream().map(
                BasicSampleInformation::sampleName).collect(Collectors.joining(",")))
        .setKey(UiSortKey.SAMPLE_NAME.value())
        .setHeader("Sample Name");
    grid.addColumn(
            rawData -> convertToLocalDate(Date.from(rawData.dataset().registrationDate())))
        .setKey(UiSortKey.UPLOAD_DATE.value())
        .setHeader("Upload Date");
    grid.setItemDetailsRenderer(renderRawDataPxp());
    return grid;
  }

  private ComponentRenderer<GridDetailsItem, RawDatasetInformationPxP> renderRawDataPxp() {
    return new ComponentRenderer<>(rawData -> {
      GridDetailsItem rawDataItem = new GridDetailsItem();
      rawDataItem.addListEntry("Sample Ids", rawData.linkedSampleInformation().stream().map(
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
      rawDataItem.addListEntry("Sample Ids", rawData.linkedSampleInformation().stream().map(
          BasicSampleInformation::sampleName).toList());
      rawDataItem.addEntry("Number of Files",
          String.valueOf(rawData.dataset().numberOfFiles()));
      rawDataItem.addEntry("File Size", String.valueOf(rawData.dataset().totalSizeBytes()));
      rawDataItem.addListEntry("File Suffixes", rawData.dataset().fileTypes());
      return rawDataItem;
    });
  }


  private ComponentRenderer<GridDetailsItem, RawData> renderRawDataItemDetails() {
    return new ComponentRenderer<>(rawData -> {
      GridDetailsItem rawDataItem = new GridDetailsItem();
      rawDataItem.addListEntry("Sample Ids",
          groupSampleInfoIntoCodeAndLabel(rawData.sampleInformation()));
      rawDataItem.addEntry("Number of Files",
          String.valueOf(rawData.rawDataDatasetInformation().numberOfFiles()));
      rawDataItem.addEntry("File Size", rawData.rawDataDatasetInformation().fileSize());
      rawDataItem.addListEntry("File Suffixes", rawData.rawDataDatasetInformation().fileEndings());
      return rawDataItem;
    });
  }

  private Collection<String> groupSampleInfoIntoCodeAndLabel(
      Collection<RawDataSampleInformation> sampleInformationCollection) {
    return sampleInformationCollection.stream().map(
        sampleInformation -> String.format("%s (%s)",
            sampleInformation.sampleName(), sampleInformation.sampleCode().code())).toList();
  }

  private String convertToLocalDate(Date date) {
    return date.toInstant()
        .atZone(ZoneId.of(clientDetailsProvider
            .latestDetails()
            .orElseThrow()
            .timeZoneId()))
        .format(DateTimeFormatter.ISO_LOCAL_DATE);
  }

  private static class RawDataFilter implements Filter {

    private String filter;

    public RawDataFilter(@NonNull String filter) {
      this.filter = Objects.requireNonNull(filter);
    }

    @Override
    public Optional<String> searchTerm() {
      return Optional.ofNullable(filter);
    }
  }
}
