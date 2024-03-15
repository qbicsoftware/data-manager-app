package life.qbic.datamanager.views.projects.project.rawdata;


import com.vaadin.flow.component.grid.Grid;
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
import life.qbic.datamanager.views.general.Tag;
import life.qbic.projectmanagement.application.SortOrder;
import life.qbic.projectmanagement.application.rawdata.RawDataService;
import life.qbic.projectmanagement.application.rawdata.RawDataService.NGSRawData;
import life.qbic.projectmanagement.application.rawdata.RawDataService.ProteomicsRawData;
import life.qbic.projectmanagement.application.rawdata.RawDataService.RawDataSampleInformation;
import life.qbic.projectmanagement.application.sample.SampleInformationService;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.measurement.MeasurementId;
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
        .setHeader("Measurement Id").setAutoWidth(true);
    ngsRawDataGrid.addComponentColumn(
            ngsRawData -> renderSampleInformation().createComponent(ngsRawData.measuredSamples()))
        .setHeader("Sample Ids").setFlexGrow(1).setAutoWidth(true);
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
    //Todo Determine if raw data can be generalized
    rawDataGridDataViews.add(ngsGridDataView);
  }

  private void createProteomicsRawDataGrid() {
    proteomicsRawDataGrid.addClassName("raw-data-grid");
    proteomicsRawDataGrid.addColumn(
            proteomicsRawData -> proteomicsRawData.measurementId().value())
        .setHeader("Measurement Code").setAutoWidth(true)
        .setTooltipGenerator(proteomicsRawData -> proteomicsRawData.measurementId().value());
    proteomicsRawDataGrid.addComponentColumn(
            proteomicsRawData -> renderSampleInformation().createComponent(
                proteomicsRawData.measuredSamples())).setHeader("Sample Ids").setFlexGrow(1)
        .setAutoWidth(true);
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
    proteomicsRawDataGrid.setItemDetailsRenderer(createProteomicsRawDataRenderer());
    rawDataGridDataViews.add(proteomicsGridDataView);
  }

  private static ComponentRenderer<Div, ProteomicsRawData> createProteomicsRawDataRenderer() {
    return new ComponentRenderer<>(RawDataDetails::new);
  }

  private static class RawDataDetails extends Div {

    /**
     * Creates a new empty div.
     */
    public RawDataDetails(ProteomicsRawData rawData) {
      addClassName("raw-data-details");
      add(createSingularValueEntry("QBiC Measurement ID", rawData.measurementId().value()));
      add(createSampleLabelsEntry(rawData.measuredSamples()));
      add(createSingularValueEntry("Description", rawData.description()));
      add(createSingularValueEntry("Dataset file name", rawData.dataSetFileName()));
      add(createSingularValueEntry("File size", rawData.fileSize()));
      add(createSingularValueEntry("No. of files", rawData.numberOfFiles()));
      add(createSingularValueEntry("Upload date", String.valueOf(rawData.registrationDate())));
      add(createSingularValueEntry("Checksum", rawData.checksum()));
    }

    private Span createSingularValueEntry(String label, String value) {
      Span entry = new Span();
      entry.addClassName("raw-data-details-entry");
      Span entryLabel = new Span(label + " : ");
      entryLabel.addClassName("bold");
      Span entryValue = new Span(value);
      entryValue.addClassName("raw-data-details-entry-value");
      entry.add(entryLabel, entryValue);
      return entry;
    }

    private Span createSampleLabelsEntry(Collection<RawDataSampleInformation> samples) {
      Span entry = new Span();
      entry.addClassName("raw-data-details-entry");
      Span entryLabel = new Span("Sample labels" + ":");
      entryLabel.addClassName("bold");
      Span entryValue = new Span();
      samples.forEach(sampleInformation -> entryValue.add(
          createSampleInformation(sampleInformation.sampleLabel(),
              sampleInformation.sampleCode().code())));
      entryValue.addClassName("raw-data-details-entry-value");
      entry.add(entryLabel, entryValue);
      return entry;
    }

    private Span createSampleInformation(String label, String sampleCode) {
      Span sampleInformation = new Span();
      sampleInformation.addClassName("sample-information");
      Span sampleLabel = new Span(label);
      Tag sampleCodeTag = new Tag(sampleCode);
      sampleInformation.add(sampleLabel, sampleCodeTag);
      return sampleInformation;
    }

  }

  private LocalDateTime asClientLocalDateTime(Instant instant) {
    ZonedDateTime zonedDateTime = instant.atZone(ZoneId.of(
        this.clientDetailsProvider.latestDetails()
            .map(ClientDetailsProvider.ClientDetails::timeZoneId).orElse("UTC")));
    return zonedDateTime.toLocalDateTime();
  }

  private ComponentRenderer<Div, Collection<RawDataSampleInformation>> renderSampleInformation() {
    return new ComponentRenderer<>(sampleInformationList -> {
      Div showSampleCodes = new Div();
      showSampleCodes.addClassName("sample-information-column");
      sampleInformationList.forEach(sampleInformation ->
          showSampleCodes.add(new Span(sampleInformation.sampleCode().code() + " "),
              new Span("(" + sampleInformation.sampleLabel() + ") ")));
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
