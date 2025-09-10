package life.qbic.datamanager.views.projects.project.rawdata;

import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.grid.dataview.GridLazyDataView;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.data.provider.AbstractDataView;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.PermitAll;
import java.io.Serializable;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import life.qbic.application.commons.SortOrder;
import life.qbic.datamanager.ClientDetailsProvider;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.GridDetailsItem;
import life.qbic.datamanager.views.general.MultiSelectLazyLoadingGrid;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.datamanager.views.projects.project.measurements.MeasurementDetailsComponent.Domain;
import life.qbic.datamanager.views.projects.project.measurements.MeasurementDetailsComponent.MeasurementDomainTab;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.api.AsyncProjectService;
import life.qbic.projectmanagement.application.api.AsyncProjectService.BasicSampleInformation;
import life.qbic.projectmanagement.application.api.AsyncProjectService.RawDatasetInformationNgs;
import life.qbic.projectmanagement.application.api.AsyncProjectService.RawDatasetInformationPxP;
import life.qbic.projectmanagement.application.api.AsyncProjectService.SortFieldRawData;
import life.qbic.projectmanagement.application.api.AsyncProjectService.SortRawData;
import life.qbic.projectmanagement.application.dataset.RemoteRawDataService;
import life.qbic.projectmanagement.application.dataset.RemoteRawDataService.RawData;
import life.qbic.projectmanagement.application.dataset.RemoteRawDataService.RawDataSampleInformation;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.measurement.MeasurementCode;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Raw Data Details Component
 * <p></p>
 * Enables the user to manage the registered RawData by providing the ability to access and search
 * the raw data, and enabling them to download the raw data of interest
 */

@SpringComponent
@UIScope
@PermitAll
public class RawDataDetailsComponent extends PageArea implements Serializable {

  private final TabSheet registeredRawDataTabSheet = new TabSheet();
  private final MultiSelectLazyLoadingGrid<RawDatasetInformationNgs> ngsRawDataGrid = new MultiSelectLazyLoadingGrid<>();
  private final MultiSelectLazyLoadingGrid<RawDatasetInformationPxP> proteomicsRawDataGrid = new MultiSelectLazyLoadingGrid<>();
  private final Collection<GridLazyDataView<?>> rawDataGridDataViews = new ArrayList<>();
  private final MeasurementDomainTab proteomicsTab;
  private final MeasurementDomainTab genomicsTab;
  private final transient RemoteRawDataService remoteRawDataService;
  private final List<MeasurementDomainTab> tabsInTabSheet = new ArrayList<>();
  private final transient ClientDetailsProvider clientDetailsProvider;
  private final AsyncProjectService asyncProjectService;
  private String searchTerm = "";
  private transient Context context;

  private static final Logger log = logger(RawDataDetailsComponent.class);

  public RawDataDetailsComponent(@Autowired RemoteRawDataService remoteRawDataService,
      ClientDetailsProvider clientDetailsProvider,
      AsyncProjectService asyncProjectService) {
    this.remoteRawDataService = Objects.requireNonNull(remoteRawDataService);
    this.clientDetailsProvider = Objects.requireNonNull(clientDetailsProvider);
    proteomicsTab = new MeasurementDomainTab(Domain.PROTEOMICS, 0);
    genomicsTab = new MeasurementDomainTab(Domain.GENOMICS, 0);
    createProteomicsRawDataGrid();
    createNGSRawDataGrid();
    add(registeredRawDataTabSheet);
    registeredRawDataTabSheet.addClassName("raw-data-tabsheet");
    addClassName("raw-data-details-component");
    this.asyncProjectService = asyncProjectService;
    registeredRawDataTabSheet.add(genomicsTab, ngsRawDataGrid);
    registeredRawDataTabSheet.add(proteomicsTab, proteomicsRawDataGrid);
  }

  /**
   * Propagates the search Term provided by the user
   * <p>
   * The string based search term is used to filter the raw Data information shown in the grid of
   * each individual tab of the Tabsheet within this component
   *
   * @param searchTerm String based searchTerm for which the properties of each raw data item should
   *                   be filtered for
   */
  public void setSearchedRawDataValue(String searchTerm) {
    if (!this.searchTerm.equals(searchTerm)) {
      this.searchTerm = searchTerm;
      refreshGrids();
    }
  }

  public void refreshGrids() {
    proteomicsRawDataGrid.clearSelectedItems();
    ngsRawDataGrid.clearSelectedItems();
    rawDataGridDataViews.forEach(AbstractDataView::refreshAll);

  }

  /**
   * Provides the {@link ExperimentId} to the {@link GridLazyDataView}s to query the raw data
   * information shown in the grids of this component
   *
   * @param context Context with the projectId and experimentId which contains the measurements with
   *                which the raw data is associated
   */
  public void setContext(Context context) {
    resetTabsInTabsheet();
    this.context = context;
    initializeTabCounts();
    asyncProjectService.getRawDatasetInformationPxP(context.projectId().orElseThrow().value(),
            context.experimentId().orElseThrow().value(), 0, 1000, new SortRawData(
                SortFieldRawData.REGISTRATION_DATE, AsyncProjectService.SortDirection.DESC), "")
        .doOnNext(info -> log.info(info.toString())).subscribe();
  }

  private void initializeTabCounts() {
    genomicsTab.setMeasurementCount(remoteRawDataService.countNGSDatasets(
        context.experimentId().orElseThrow()));
    proteomicsTab.setMeasurementCount(remoteRawDataService.countProteomicsDatasets(
        context.experimentId().orElseThrow()));
  }

  /*Vaadin provides no easy way to remove all tabs in a tabSheet*/
  private void resetTabsInTabsheet() {
    if (!tabsInTabSheet.isEmpty()) {
      tabsInTabSheet.forEach(registeredRawDataTabSheet::remove);
      tabsInTabSheet.clear();
    }
  }

  private void createNGSRawDataGrid() {
    ngsRawDataGrid.addClassName("raw-data-grid");
    ngsRawDataGrid.addColumn(
            rawData -> rawData.dataset().measurementId())
        .setKey("measurementId")
        .setHeader("Measurement Id");
    ngsRawDataGrid.addColumn(
            rawData -> rawData.linkedSampleInformation().stream().map(
                BasicSampleInformation::sampleName).collect(Collectors.joining(",")))
        .setKey("sampleIds")
        .setHeader("Sample Name");
    ngsRawDataGrid.addColumn(
            rawData -> convertToLocalDate(Date.from(rawData.dataset().registrationDate())))
        .setKey("uploaddate")
        .setHeader("Upload Date");
    ngsRawDataGrid.setItemDetailsRenderer(renderRawDataNgs());
    GridLazyDataView<RawDatasetInformationNgs> ngsGridDataView = ngsRawDataGrid.setItems(
        query -> {
          List<SortOrder> sortOrders = query.getSortOrders().stream().map(
                  it -> new SortOrder(it.getSorted(),
                      it.getDirection().equals(SortDirection.ASCENDING)))
              .collect(Collectors.toList());
          // if no order is provided by the grid order by last modified (least priority)
          sortOrders.add(SortOrder.of("measurementId").ascending());
          return asyncProjectService.getRawDatasetInformationNgs(
              context.projectId().orElseThrow().value(),
              context.experimentId().orElseThrow().value(), query.getOffset(), query.getLimit(),
              new SortRawData(SortFieldRawData.REGISTRATION_DATE,
                  AsyncProjectService.SortDirection.DESC), searchTerm).toStream();

        });

    ngsRawDataGrid.getLazyDataView().addItemCountChangeListener(
        countChangeEvent -> genomicsTab.setMeasurementCount(
            (int) ngsGridDataView.getItems().count()));

    rawDataGridDataViews.add(ngsGridDataView);
  }

  private void createProteomicsRawDataGrid() {
    proteomicsRawDataGrid.addClassName("raw-data-grid");
    proteomicsRawDataGrid.addColumn(
            rawData -> rawData.dataset().measurementId())
        .setKey("measurementId")
        .setHeader("Measurement Id");
    proteomicsRawDataGrid.addColumn(
            rawData -> rawData.linkedSampleInformation().stream().map(
                BasicSampleInformation::sampleName).collect(Collectors.joining(",")))
        .setKey("sampleIds")
        .setHeader("Sample Name");
    proteomicsRawDataGrid.addColumn(
            rawData -> convertToLocalDate(Date.from(rawData.dataset().registrationDate())))
        .setKey("uploaddate")
        .setHeader("Upload Date");
    proteomicsRawDataGrid.setItemDetailsRenderer(renderRawDataPxp());
    GridLazyDataView<RawDatasetInformationPxP> proteomicsGridDataView = proteomicsRawDataGrid.setItems(
        query -> {
          List<SortOrder> sortOrders = query.getSortOrders().stream().map(
                  it -> new SortOrder(it.getSorted(),
                      it.getDirection().equals(SortDirection.ASCENDING)))
              .collect(Collectors.toList());
          // if no order is provided by the grid order by last modified (least priority)
          sortOrders.add(SortOrder.of("measurementId").ascending());
          return asyncProjectService.getRawDatasetInformationPxP(
              context.projectId().orElseThrow().value(),
              context.experimentId().orElseThrow().value(), query.getOffset(), query.getLimit(),
              new SortRawData(SortFieldRawData.REGISTRATION_DATE,
                  AsyncProjectService.SortDirection.DESC), searchTerm).toStream();

        });
    proteomicsRawDataGrid.getLazyDataView().addItemCountChangeListener(
        countChangeEvent -> proteomicsTab.setMeasurementCount(
            (int) proteomicsGridDataView.getItems().count()));

    rawDataGridDataViews.add(proteomicsGridDataView);
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

  public Collection<MeasurementCode> getSelectedMeasurementUrls() {
    List<MeasurementCode> selectedMeasurements = new ArrayList<>();
//    selectedMeasurements.addAll(
//        proteomicsRawDataGrid.getSelectedItems().stream().map(RawData::measurementCode)
//            .toList());
//    selectedMeasurements.addAll(
//        ngsRawDataGrid.getSelectedItems().stream().map(RawData::measurementCode)
//            .toList());
    return selectedMeasurements;
  }
}
