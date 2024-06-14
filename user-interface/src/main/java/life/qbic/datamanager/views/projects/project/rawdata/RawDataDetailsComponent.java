package life.qbic.datamanager.views.projects.project.rawdata;


import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.grid.dataview.GridLazyDataView;
import com.vaadin.flow.component.tabs.Tab;
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
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.datamanager.views.projects.project.measurements.MeasurementDetailsComponent.MeasurementTechnologyTab;
import life.qbic.projectmanagement.application.dataset.RawDataService;
import life.qbic.projectmanagement.application.dataset.RawDataService.RawData;
import life.qbic.projectmanagement.application.dataset.RawDataService.RawDataSampleInformation;
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
  private final Grid<RawData> ngsRawDataGrid = new Grid<>();
  private final Grid<RawData> proteomicsRawDataGrid = new Grid<>();
  private final Collection<GridLazyDataView<RawData>> rawDataGridDataViews = new ArrayList<>();
  private final MeasurementTechnologyTab proteomicsTab;
  private final MeasurementTechnologyTab genomicsTab;
  private final transient RawDataService rawDataService;
  private final List<MeasurementTechnologyTab> tabsInTabSheet = new ArrayList<>();
  private final transient ClientDetailsProvider clientDetailsProvider;
  private String searchTerm = "";
  private transient Context context;

  public RawDataDetailsComponent(@Autowired RawDataService rawDataService,
      ClientDetailsProvider clientDetailsProvider) {
    this.rawDataService = Objects.requireNonNull(rawDataService);
    this.clientDetailsProvider = Objects.requireNonNull(clientDetailsProvider);
    proteomicsTab = new MeasurementTechnologyTab("Proteomics", 0);
    genomicsTab = new MeasurementTechnologyTab("Genomics", 0);
    createProteomicsRawDataGrid();
    createNGSRawDataGrid();
    add(registeredRawDataTabSheet);
    registeredRawDataTabSheet.addClassName("raw-data-tabsheet");
    addClassName("raw-data-details-component");
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
    this.searchTerm = searchTerm;
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
    List<GridLazyDataView<RawData>> dataViewsWithItems = rawDataGridDataViews.stream()
        .filter(gridLazyDataView -> gridLazyDataView.getItems()
            .findAny().isPresent()).toList();
    if (dataViewsWithItems.isEmpty()) {
      return;
    }
    dataViewsWithItems.forEach(this::addRawDataTab);
  }

  /*Vaadin provides no easy way to remove all tabs in a tabSheet*/
  private void resetTabsInTabsheet() {
    if (!tabsInTabSheet.isEmpty()) {
      tabsInTabSheet.forEach(registeredRawDataTabSheet::remove);
      tabsInTabSheet.clear();
    }
  }

  private void addRawDataTab(GridLazyDataView<RawData> gridLazyDataView) {
    if (gridLazyDataView.getItems().allMatch(rawData -> rawData.measurementCode().isNGSDomain())) {
      tabsInTabSheet.add(genomicsTab);
      registeredRawDataTabSheet.add(genomicsTab, ngsRawDataGrid);
    }
    if (gridLazyDataView.getItems().allMatch(rawData -> rawData.measurementCode().isMSDomain())) {
      tabsInTabSheet.add(proteomicsTab);
      registeredRawDataTabSheet.add(proteomicsTab, proteomicsRawDataGrid);
    }
  }

  private void createNGSRawDataGrid() {
    ngsRawDataGrid.addClassName("raw-data-grid");
    ngsRawDataGrid.addColumn(rawData -> rawData.measurementCode().value())
        .setKey("measurementId")
        .setHeader("Measurement Id");
    ngsRawDataGrid.addColumn(
            rawData -> String.join(" ", groupSampleInfoIntoCodeAndLabel(rawData.sampleInformation())))
        .setKey("sampleIds")
        .setHeader("Sample Ids")
        .setTooltipGenerator(rawData -> String.join(" ",
            groupSampleInfoIntoCodeAndLabel(rawData.sampleInformation())));
    ngsRawDataGrid.addColumn(
            rawData -> convertToLocalDate(rawData.rawDataDatasetInformation().registrationDate()))
        .setKey("uploadDate")
        .setHeader("Upload Date")
        .setTooltipGenerator(
            rawData -> convertToLocalDate(rawData.rawDataDatasetInformation().registrationDate()));
    GridLazyDataView<RawData> ngsGridDataView = ngsRawDataGrid.setItems(query -> {
      List<SortOrder> sortOrders = query.getSortOrders().stream().map(
              it -> new SortOrder(it.getSorted(), it.getDirection().equals(SortDirection.ASCENDING)))
          .collect(Collectors.toList());
      // if no order is provided by the grid order by last modified (least priority)
      sortOrders.add(SortOrder.of("measurementId").ascending());
      return rawDataService.findNGSRawData(searchTerm,
              context.experimentId().orElseThrow(),
              query.getOffset(), query.getLimit(), sortOrders, context.projectId().orElseThrow())
          .stream();
    });
    ngsRawDataGrid.getLazyDataView()
        .addItemCountChangeListener(
            countChangeEvent -> genomicsTab.setMeasurementCount(countChangeEvent.getItemCount()));
    ngsRawDataGrid.setItemDetailsRenderer(renderRawDataItemDetails());
    ngsRawDataGrid.setSelectionMode(SelectionMode.MULTI);
    rawDataGridDataViews.add(ngsGridDataView);
  }

  private void createProteomicsRawDataGrid() {
    proteomicsRawDataGrid.addClassName("raw-data-grid");
    proteomicsRawDataGrid.addColumn(
            rawData -> rawData.measurementCode().value())
        .setKey("measurementId")
        .setHeader("Measurement Id")
        .setTooltipGenerator(rawData -> rawData.measurementCode().value());
    proteomicsRawDataGrid.addColumn(
            rawData -> String.join(" ", groupSampleInfoIntoCodeAndLabel(rawData.sampleInformation())))
        .setKey("sampleIds")
        .setHeader("Sample Ids")
        .setTooltipGenerator(rawData -> String.join(" ",
            groupSampleInfoIntoCodeAndLabel(rawData.sampleInformation())));
    proteomicsRawDataGrid.addColumn(
            rawData -> convertToLocalDate(rawData.rawDataDatasetInformation().registrationDate()))
        .setKey("uploaddate")
        .setHeader("Upload Date")
        .setTooltipGenerator(
            rawData -> convertToLocalDate(rawData.rawDataDatasetInformation().registrationDate()));
    proteomicsRawDataGrid.setItemDetailsRenderer(renderRawDataItemDetails());
    GridLazyDataView<RawData> proteomicsGridDataView = proteomicsRawDataGrid.setItems(
        query -> {
          List<SortOrder> sortOrders = query.getSortOrders().stream().map(
                  it -> new SortOrder(it.getSorted(),
                      it.getDirection().equals(SortDirection.ASCENDING)))
              .collect(Collectors.toList());
          // if no order is provided by the grid order by last modified (least priority)
          sortOrders.add(SortOrder.of("measurementId").ascending());
          return rawDataService.findProteomicsRawData(searchTerm,
                  context.experimentId().orElseThrow(),
                  query.getOffset(), query.getLimit(), sortOrders, context.projectId().orElseThrow())
              .stream();
        });
    proteomicsRawDataGrid.getLazyDataView()
        .addItemCountChangeListener(
            countChangeEvent -> proteomicsTab.setMeasurementCount(countChangeEvent.getItemCount()));
    proteomicsRawDataGrid.setSelectionMode(SelectionMode.MULTI);
    rawDataGridDataViews.add(proteomicsGridDataView);
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
            sampleInformation.sampleLabel(), sampleInformation.sampleCode().code())).toList();
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
    selectedMeasurements.addAll(
        proteomicsRawDataGrid.getSelectedItems().stream().map(RawData::measurementCode)
            .toList());
    selectedMeasurements.addAll(
        ngsRawDataGrid.getSelectedItems().stream().map(RawData::measurementCode)
            .toList());
    return selectedMeasurements;
  }
}
