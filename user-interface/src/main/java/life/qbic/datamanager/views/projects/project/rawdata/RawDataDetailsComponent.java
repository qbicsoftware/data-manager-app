package life.qbic.datamanager.views.projects.project.rawdata;


import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.dataview.GridLazyDataView;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.data.provider.AbstractDataView;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.LocalDateTimeRenderer;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.PermitAll;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import life.qbic.datamanager.ClientDetailsProvider;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.projectmanagement.application.SortOrder;
import life.qbic.projectmanagement.application.rawdata.RawDataService;
import life.qbic.projectmanagement.application.rawdata.RawDataService.NGSRawData;
import life.qbic.projectmanagement.application.rawdata.RawDataService.ProteomicsRawData;
import life.qbic.projectmanagement.application.sample.SampleInformationService;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.measurement.MeasurementId;
import life.qbic.projectmanagement.domain.model.sample.Sample;
import life.qbic.projectmanagement.domain.model.sample.SampleCode;
import life.qbic.projectmanagement.domain.model.sample.SampleId;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Raw Data Details Component
 * <p></p>
 * Enables the user to manage the registered RawData by providing the ability to
 * access and search the raw data, and enabling them to download the raw data of interest
 */

@SpringComponent
@UIScope
@PermitAll
public class RawDataDetailsComponent extends PageArea implements Serializable {

  private final TabSheet registeredRawDataTabSheet = new TabSheet();
  private String searchTerm = "";
  private final Grid<NGSRawData> ngsRawDataGrid = new Grid<>();
  private final Grid<ProteomicsRawData> proteomicsRawDataGrid = new Grid<>();
  private final Collection<GridLazyDataView<?>> rawDataGridDataViews = new ArrayList<>();
  private final transient RawDataService rawDataService;
  private final transient SampleInformationService sampleInformationService;
  private final List<Tab> tabsInTabSheet = new ArrayList<>();
  private transient Context context;
  private final transient ClientDetailsProvider clientDetailsProvider;

  public RawDataDetailsComponent(@Autowired RawDataService rawDataService,
      @Autowired SampleInformationService sampleInformationService,
      ClientDetailsProvider clientDetailsProvider) {
    this.rawDataService = Objects.requireNonNull(rawDataService);
    this.sampleInformationService = Objects.requireNonNull(sampleInformationService);
    this.clientDetailsProvider = Objects.requireNonNull(clientDetailsProvider);
    createProteomicsRawDataGrid();
    createNGSRawDataGrid();
    add(registeredRawDataTabSheet);
    registeredRawDataTabSheet.addClassName("raw-data-tabsheet");
    addClassName("raw-data-details-component");
  }

  /**
   * Propagates the search Term provided by the user
   * <p>
   * The string based search term is used to filter the raw Data information shown in the
   * grid of each individual tab of the Tabsheet within this component
   *
   * @param searchTerm String based searchTerm for which the properties of each raw data item should
   *                   be filtered for
   */
  public void setSearchedRawDataValue(String searchTerm) {
    this.searchTerm = searchTerm;
    rawDataGridDataViews.forEach(AbstractDataView::refreshAll);
  }

  /**
   * Provides the {@link ExperimentId} to the {@link GridLazyDataView}s to query the
   * raw data information shown in the grids of this component
   *
   * @param context Context with the projectId and experimentId which contains the measurements with which the raw data is associated
   */
  public void setContext(Context context) {
    resetTabsInTabsheet();
    this.context = context;
    List<GridLazyDataView<?>> dataViewsWithItems = rawDataGridDataViews.stream()
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

  private void addRawDataTab(GridLazyDataView<?> gridLazyDataView) {
    if (gridLazyDataView.getItem(0) instanceof ProteomicsRawData) {
      tabsInTabSheet.add(registeredRawDataTabSheet.add("Proteomics", proteomicsRawDataGrid));
    }
    if (gridLazyDataView.getItem(0) instanceof NGSRawData) {
      tabsInTabSheet.add(registeredRawDataTabSheet.add("Genomics", ngsRawDataGrid));
    }
  }

  private void createNGSRawDataGrid() {
    ngsRawDataGrid.addClassName("raw-data-grid");
    ngsRawDataGrid.addColumn(ngsRawData -> ngsRawData.measurementId().value())
        .setHeader("Measurement Id");
    ngsRawDataGrid.addComponentColumn(
            ngsRawData -> renderSampleCodes().createComponent(ngsRawData.measuredSamples()))
        .setHeader("Sample Ids");
    ngsRawDataGrid.addColumn(new LocalDateTimeRenderer<>(
            ngsRawData -> asClientLocalDateTime(ngsRawData.registrationDate()),
            "yyyy-MM-dd"))
        .setKey("registrationDate")
        .setHeader("Registration Date")
        .setTooltipGenerator(ngsRawData -> {
          LocalDateTime dateTime = asClientLocalDateTime(ngsRawData.registrationDate());
          return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd 'at' hh:mm a"));
        })
        .setAutoWidth(true);
    ngsRawDataGrid.addThemeVariants(GridVariant.LUMO_WRAP_CELL_CONTENT);
    GridLazyDataView<NGSRawData> ngsGridDataView = ngsRawDataGrid.setItems(query -> {
      List<SortOrder> sortOrders = query.getSortOrders().stream().map(
              it -> new SortOrder(it.getSorted(), it.getDirection().equals(SortDirection.ASCENDING)))
          .collect(Collectors.toList());
      // if no order is provided by the grid order by last modified (least priority)
      sortOrders.add(SortOrder.of("measurementCode").ascending());
      return rawDataService.findNGSRawData(searchTerm,
              context.experimentId().orElseThrow(),
              query.getOffset(), query.getLimit(), sortOrders, context.projectId().orElseThrow())
          .stream();
    });
    rawDataGridDataViews.add(ngsGridDataView);
  }

  private void createProteomicsRawDataGrid() {
    proteomicsRawDataGrid.addClassName("raw-data-grid");
    proteomicsRawDataGrid.addColumn(
            proteomicsRawData -> proteomicsRawData.measurementId().value())
        .setHeader("Measurement Code").setAutoWidth(true)
        .setTooltipGenerator(proteomicsRawData -> proteomicsRawData.measurementId().value());
    proteomicsRawDataGrid.addComponentColumn(
        proteomicsRawData -> renderSampleCodes().createComponent(
            proteomicsRawData.measuredSamples())).setHeader("Sample Ids").setAutoWidth(true);
    proteomicsRawDataGrid.addColumn(new LocalDateTimeRenderer<>(
            proteomicsRawData -> asClientLocalDateTime(proteomicsRawData.registrationDate()),
            "yyyy-MM-dd"))
        .setKey("registrationDate")
        .setHeader("Registration Date")
        .setTooltipGenerator(proteomicsRawData -> {
          LocalDateTime dateTime = asClientLocalDateTime(proteomicsRawData.registrationDate());
          return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd 'at' hh:mm a"));
        })
        .setAutoWidth(true);
    GridLazyDataView<ProteomicsRawData> proteomicsGridDataView = proteomicsRawDataGrid.setItems(
        query -> {
          List<SortOrder> sortOrders = query.getSortOrders().stream().map(
                  it -> new SortOrder(it.getSorted(),
                      it.getDirection().equals(SortDirection.ASCENDING)))
              .collect(Collectors.toList());
          // if no order is provided by the grid order by last modified (least priority)
          sortOrders.add(SortOrder.of("measurementCode").ascending());
          return rawDataService.findProteomicsRawData(searchTerm,
                  context.experimentId().orElseThrow(),
                  query.getOffset(), query.getLimit(), sortOrders, context.projectId().orElseThrow())
              .stream();
        });
    rawDataGridDataViews.add(proteomicsGridDataView);
  }

  private LocalDateTime asClientLocalDateTime(Instant instant) {
    ZonedDateTime zonedDateTime = instant.atZone(ZoneId.of(
        this.clientDetailsProvider.latestDetails()
            .map(ClientDetailsProvider.ClientDetails::timeZoneId).orElse("UTC")));
    return zonedDateTime.toLocalDateTime();
  }

  private ComponentRenderer<Div, Collection<SampleId>> renderSampleCodes() {
    return new ComponentRenderer<>(sampleIds -> {
      Div showSampleCodes = new Div();
      List<SampleCode> sampleCodes = sampleInformationService.retrieveSamplesByIds(sampleIds)
          .stream().map(
              Sample::sampleCode).toList();
      showSampleCodes.addClassName("sample-code-column");
      sampleCodes.forEach(sampleCode -> showSampleCodes.add(new Span(sampleCode.code())));
      return showSampleCodes;
    });
  }

  //Todo should the user be able to download measurements from each grid?
  public Collection<MeasurementId> getSelectedMeasurements() {
    List<MeasurementId> selectedMeasurements = new ArrayList<>();
    selectedMeasurements.addAll(
        proteomicsRawDataGrid.getSelectedItems().stream().map(ProteomicsRawData::measurementId)
            .toList());
    selectedMeasurements.addAll(
        proteomicsRawDataGrid.getSelectedItems().stream().map(ProteomicsRawData::measurementId)
            .toList());
    return selectedMeasurements;
  }

}
