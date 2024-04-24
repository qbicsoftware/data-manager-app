package life.qbic.datamanager.views.projects.project.measurements;

import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.grid.dataview.GridLazyDataView;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.data.provider.AbstractDataView;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.PermitAll;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import life.qbic.application.commons.SortOrder;
import life.qbic.datamanager.ClientDetailsProvider;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.GridDetailsItem;
import life.qbic.datamanager.views.general.MultiSelectLazyLoadingGrid;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.measurement.MeasurementMetadata;
import life.qbic.projectmanagement.application.measurement.MeasurementService;
import life.qbic.projectmanagement.application.sample.SampleInformationService;
import life.qbic.projectmanagement.domain.Organisation;
import life.qbic.projectmanagement.domain.model.OntologyTerm;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.measurement.NGSMeasurement;
import life.qbic.projectmanagement.domain.model.measurement.ProteomicsMeasurement;
import life.qbic.projectmanagement.domain.model.sample.SampleId;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Enables the user to manage the registered {@link MeasurementMetadata} by providing the ability to
 * register new measurements, search already registered measurements and view measurements dependent
 * on the lab facility (Proteomics, Genomics, Imaging...)
 */
@SpringComponent
@UIScope
@PermitAll
public class MeasurementDetailsComponent extends PageArea implements Serializable {

  @Serial
  private static final long serialVersionUID = 5086686432247130622L;
  private static final Logger log = logger(MeasurementDetailsComponent.class);
  private final TabSheet registeredMeasurementsTabSheet = new TabSheet();
  private final MultiSelectLazyLoadingGrid<NGSMeasurement> ngsMeasurementGrid = new MultiSelectLazyLoadingGrid<>();
  private final MultiSelectLazyLoadingGrid<ProteomicsMeasurement> proteomicsMeasurementGrid = new MultiSelectLazyLoadingGrid<>();
  private final Collection<GridLazyDataView<?>> measurementsGridDataViews = new ArrayList<>();
  private final transient MeasurementService measurementService;
  private final transient SampleInformationService sampleInformationService;
  private final List<Tab> tabsInTabSheet = new ArrayList<>();
  private transient Context context;
  private final StreamResource rorIconResource = new StreamResource("ROR_logo.svg",
      () -> getClass().getClassLoader().getResourceAsStream("icons/ROR_logo.svg"));
  private final ClientDetailsProvider clientDetailsProvider;
  private final List<ComponentEventListener<MeasurementSelectionChangedEvent>> listeners = new ArrayList<>();
  private String searchTerm = "";

  public MeasurementDetailsComponent(@Autowired MeasurementService measurementService,
      @Autowired SampleInformationService sampleInformationService,
      ClientDetailsProvider clientDetailsProvider) {
    this.measurementService = Objects.requireNonNull(measurementService);
    this.sampleInformationService = Objects.requireNonNull(sampleInformationService);
    this.clientDetailsProvider = clientDetailsProvider;
    createProteomicsGrid();
    createNGSMeasurementGrid();
    add(registeredMeasurementsTabSheet);
    registeredMeasurementsTabSheet.addClassName("measurement-tabsheet");
    addClassName("measurement-details-component");
  }

  /**
   * Provides the {@link ExperimentId} to the {@link GridLazyDataView}s to query the
   * {@link MeasurementMetadata} shown in the grids of this component
   *
   * @param context Context with the projectId and experimentId containing the samples for which
   *                measurements could be registered
   */
  public void setContext(Context context) {
    resetTabsInTabsheet();
    this.context = context;
    List<GridLazyDataView<?>> dataViewsWithItems = measurementsGridDataViews.stream()
        .filter(gridLazyDataView -> gridLazyDataView.getItems()
            .findAny().isPresent()).toList();
    dataViewsWithItems.forEach(this::addMeasurementTab);
  }

  /**
   * Propagates the search Term provided by the user
   * <p>
   * The string based search term is used to filter the {@link MeasurementMetadata} shown in the
   * grid of each individual tab of the Tabsheet within this component
   *
   * @param searchTerm String based searchTerm for which the properties of each measurement should
   *                   be filtered for
   */
  public void setSearchedMeasurementValue(String searchTerm) {
    this.searchTerm = searchTerm;
    measurementsGridDataViews.forEach(AbstractDataView::refreshAll);
  }

  /*Vaadin provides no easy way to remove all tabs in a tabSheet*/
  private void resetTabsInTabsheet() {
    if (!tabsInTabSheet.isEmpty()) {
      tabsInTabSheet.forEach(registeredMeasurementsTabSheet::remove);
      tabsInTabSheet.clear();
    }
  }

  private void addMeasurementTab(GridLazyDataView<?> gridLazyDataView) {
    if (gridLazyDataView.getItem(0) instanceof ProteomicsMeasurement) {
      tabsInTabSheet.add(
          registeredMeasurementsTabSheet.add("Proteomics", proteomicsMeasurementGrid));
    }
    if (gridLazyDataView.getItem(0) instanceof NGSMeasurement) {
      tabsInTabSheet.add(registeredMeasurementsTabSheet.add("Genomics", ngsMeasurementGrid));
    }
  }

  private void createNGSMeasurementGrid() {
    ngsMeasurementGrid.addClassName("measurement-grid");
    ngsMeasurementGrid.addColumn(ngsMeasurement -> ngsMeasurement.measurementCode().value())
        .setHeader("Measurement ID")
        .setTooltipGenerator(ngsMeasurement -> ngsMeasurement.measurementCode().value());
    ngsMeasurementGrid.addColumn(measurement -> String.join(" ",
            groupSampleInfoIntoCodeAndLabel(measurement.measuredSamples())))
        .setHeader("Sample IDs")
        .setTooltipGenerator(measurement -> String.join(" ",
            groupSampleInfoIntoCodeAndLabel(measurement.measuredSamples()))).setFlexGrow(1);
    ngsMeasurementGrid.addColumn(NGSMeasurement::facility)
        .setHeader("Facility")
        .setTooltipGenerator(NGSMeasurement::facility);
    ngsMeasurementGrid.addComponentColumn(
            ngsMeasurement -> renderInstrument().createComponent(
                ngsMeasurement.instrument()))
        .setHeader("Instrument")
        .setTooltipGenerator(
            ngsMeasurement -> ngsMeasurement.instrument().formatted())
        .setAutoWidth(true);
    ngsMeasurementGrid.addColumn(ngsMeasurement -> ngsMeasurement.comment().orElse(""))
        .setHeader("Comment")
        .setTooltipGenerator(ngsMeasurement -> ngsMeasurement.comment().orElse(""));
    ngsMeasurementGrid.setItemDetailsRenderer(new ComponentRenderer<>(ngsMeasurement -> {
      GridDetailsItem measurementItem = new GridDetailsItem();
      measurementItem.addListEntry("Sample Ids",
          groupSampleInfoIntoCodeAndLabel(ngsMeasurement.measuredSamples()));
      measurementItem.addComponentEntry("Organisation",
          renderOrganisation(ngsMeasurement.organisation()));
      measurementItem.addEntry("Read Type", ngsMeasurement.sequencingReadType());
      measurementItem.addEntry("Library Kit", ngsMeasurement.libraryKit().orElse(""));
      measurementItem.addEntry("Flow Cell", ngsMeasurement.flowCell().orElse(""));
      measurementItem.addEntry("Run Protocol", ngsMeasurement.sequencingRunProtocol().orElse(""));
      measurementItem.addEntry("Index I7", ngsMeasurement.indexI7().orElse(""));
      measurementItem.addEntry("Index I5", ngsMeasurement.indexI5().orElse(""));
      measurementItem.addEntry("Registration Date",
          asClientLocalDateTime(ngsMeasurement.registrationDate())
              .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
      return measurementItem;
    }));
    GridLazyDataView<NGSMeasurement> ngsGridDataView = ngsMeasurementGrid.setItems(query -> {
      List<SortOrder> sortOrders = query.getSortOrders().stream().map(
              it -> new SortOrder(it.getSorted(), it.getDirection().equals(SortDirection.ASCENDING)))
          .collect(Collectors.toList());
      // if no order is provided by the grid order by last modified (least priority)
      sortOrders.add(SortOrder.of("measurementCode").ascending());
      return measurementService.findNGSMeasurements(searchTerm,
              context.experimentId().orElseThrow(),
              query.getOffset(), query.getLimit(), sortOrders, context.projectId().orElseThrow())
          .stream();
    });
    ngsMeasurementGrid.addSelectListener(
        event -> updateSelectedMeasurementsInfo(event.isFromClient()));
    measurementsGridDataViews.add(ngsGridDataView);
  }

  private void createProteomicsGrid() {
    proteomicsMeasurementGrid.addClassName("measurement-grid");
    proteomicsMeasurementGrid.addColumn(
            proteomicsMeasurement -> proteomicsMeasurement.measurementCode().value())
        .setHeader("Measurement ID")
        .setTooltipGenerator(
            proteomicsMeasurement -> proteomicsMeasurement.measurementCode().value());
    proteomicsMeasurementGrid.addColumn(measurement -> String.join(" ",
            groupSampleInfoIntoCodeAndLabel(measurement.measuredSamples())))
        .setHeader("Sample IDs")
        .setTooltipGenerator(measurement -> String.join(" ",
            groupSampleInfoIntoCodeAndLabel(measurement.measuredSamples())));
    proteomicsMeasurementGrid.addColumn(ProteomicsMeasurement::facility)
        .setHeader("Facility")
        .setTooltipGenerator(ProteomicsMeasurement::facility);
    proteomicsMeasurementGrid.addComponentColumn(
            proteomicsMeasurement -> renderInstrument().createComponent(
                proteomicsMeasurement.instrument()))
        .setHeader("Instrument")
        .setTooltipGenerator(
            proteomicsMeasurement -> proteomicsMeasurement.instrument().formatted())
        .setAutoWidth(true);
    proteomicsMeasurementGrid.addColumn(
            proteomicsMeasurement -> proteomicsMeasurement.labelingType().orElse(""))
        .setHeader("Label Type")
        .setTooltipGenerator(
            proteomicsMeasurement -> proteomicsMeasurement.labelingType().orElse(""));
    proteomicsMeasurementGrid.addColumn(measurement -> measurement.comment().orElse(""))
        .setHeader("Comment")
        .setTooltipGenerator(measurement -> measurement.comment().orElse(""));
    proteomicsMeasurementGrid.setItemDetailsRenderer(
        new ComponentRenderer<>(proteomicsMeasurement -> {
          GridDetailsItem measurementItem = new GridDetailsItem();
          measurementItem.addListEntry("Sample Ids",
              groupSampleInfoIntoCodeAndLabel(proteomicsMeasurement.measuredSamples()));
          measurementItem.addComponentEntry("Organisation",
              renderOrganisation(proteomicsMeasurement.organisation()));
          measurementItem.addEntry("Digestion Enzyme", proteomicsMeasurement.digestionEnzyme());
          measurementItem.addEntry("Digestion Method", proteomicsMeasurement.digestionMethod());
          measurementItem.addEntry("Injection Volume",
              String.valueOf(proteomicsMeasurement.injectionVolume()));
          measurementItem.addEntry("LCMS Method", proteomicsMeasurement.lcColumn());
          measurementItem.addEntry("Enrichment Method", proteomicsMeasurement.enrichmentMethod());
          measurementItem.addEntry("Fraction Name", proteomicsMeasurement.fraction().orElse(""));
          measurementItem.addEntry("Measurement Label", proteomicsMeasurement.label().orElse(""));
          measurementItem.addEntry("Sample Pool Group",
              proteomicsMeasurement.samplePoolGroup().orElse(""));
          measurementItem.addEntry("Registration Date",
              asClientLocalDateTime(proteomicsMeasurement.registrationDate())
                  .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
          return measurementItem;
        }));
    GridLazyDataView<ProteomicsMeasurement> proteomicsGridDataView = proteomicsMeasurementGrid.setItems(
        query -> {
          List<SortOrder> sortOrders = query.getSortOrders().stream().map(
                  it -> new SortOrder(it.getSorted(),
                      it.getDirection().equals(SortDirection.ASCENDING)))
              .collect(Collectors.toList());
          // if no order is provided by the grid order by last modified (least priority)
          sortOrders.add(SortOrder.of("measurementCode").ascending());
          return measurementService.findProteomicsMeasurements(searchTerm,
                  context.experimentId().orElseThrow(),
                  query.getOffset(), query.getLimit(), sortOrders, context.projectId().orElseThrow())
              .stream();
        });
    proteomicsMeasurementGrid.addSelectListener(event -> updateSelectedMeasurementsInfo(event.isFromClient()));
    measurementsGridDataViews.add(proteomicsGridDataView);
  }

  private void updateSelectedMeasurementsInfo(boolean isFromClient) {
    listeners.forEach(listener -> listener.onComponentEvent(new MeasurementSelectionChangedEvent(this, isFromClient)));
  }

  private LocalDateTime asClientLocalDateTime(Instant instant) {
    ZonedDateTime zonedDateTime = instant.atZone(ZoneId.of(
        this.clientDetailsProvider.latestDetails()
            .map(ClientDetailsProvider.ClientDetails::timeZoneId).orElse("UTC")));
    return zonedDateTime.toLocalDateTime();
  }

  private Anchor renderOrganisation(Organisation organisation) {
    SvgIcon svgIcon = new SvgIcon(rorIconResource);
    svgIcon.addClassName("organisation-icon");
    Span organisationLabel = new Span(organisation.label());
    String organisationUrl = organisation.IRI();
    Anchor organisationAnchor = new Anchor(organisationUrl, organisationLabel, svgIcon);
    organisationAnchor.setTarget(AnchorTarget.BLANK);
    organisationAnchor.addClassName("organisation-entry");
    return organisationAnchor;
  }

  private ComponentRenderer<Span, OntologyTerm> renderInstrument() {
    return new ComponentRenderer<>(instrument -> {
      Span instrumentLabel = new Span(instrument.getLabel());
      Span instrumentOntologyLink = new Span(instrument.getName().replace("_", ":"));
      instrumentOntologyLink.addClassName("ontology-link");
      Anchor instrumentNameAnchor = new Anchor(instrument.getClassIri(), instrumentOntologyLink);
      instrumentNameAnchor.setTarget(AnchorTarget.BLANK);
      Span organisationSpan = new Span(instrumentLabel, instrumentNameAnchor);
      organisationSpan.addClassName("instrument-column");
      return organisationSpan;
    });
  }

  private Collection<String> groupSampleInfoIntoCodeAndLabel(Collection<SampleId> sampleIds) {
    return sampleInformationService.retrieveSamplesByIds(sampleIds).stream()
        .map(sample -> String.format("%s (%s)", sample.sampleCode().code(), sample.label()))
        .toList();
  }

  public Set<MeasurementMetadata> getSelectedMeasurements() {
    return Stream.concat(ngsMeasurementGrid.getSelectedItems().stream(),
        proteomicsMeasurementGrid.getSelectedItems().stream()).collect(Collectors.toSet());
  }

  public void refreshGrids() {
    resetSelectedMeasurements();
    measurementsGridDataViews.forEach(AbstractDataView::refreshAll);
  }

  private void resetSelectedMeasurements() {
    updateSelectedMeasurementsInfo(false);

    proteomicsMeasurementGrid.clearSelectedItems();
    ngsMeasurementGrid.clearSelectedItems();
  }

  public Set<? extends MeasurementMetadata> getAllDisplayedMeasurements() {
    String label = registeredMeasurementsTabSheet.getSelectedTab().getLabel();
    if(label.equals("Proteomics")) {
      return proteomicsMeasurementGrid.getLazyDataView().getItems().collect(Collectors.toSet());
    }
    if(label.equals("Genomics")) {
      return ngsMeasurementGrid.getLazyDataView().getItems().collect(Collectors.toSet());
    }
    log.warn("Could not fetch measurements because tab with label %s unknown.".formatted(label));
    return Collections.emptySet();
  }

  public void addListener(ComponentEventListener<MeasurementSelectionChangedEvent> listener) {
    listeners.add(listener);
  }

  /**
   * <b>Measurement Selection Changed Event</b>
   * <p>
   * Event that indicates that measurements were selected or deselected by the user or a deletion event
   * {@link MeasurementDetailsComponent}
   *
   * @since 1.0.0
   */
  public class MeasurementSelectionChangedEvent extends
      ComponentEvent<MeasurementDetailsComponent> {

    @Serial
    private static final long serialVersionUID = 1213984633337676231L;

    public MeasurementSelectionChangedEvent(MeasurementDetailsComponent source, boolean fromClient) {
      super(source, fromClient);
    }
  }

}
