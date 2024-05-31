package life.qbic.datamanager.views.projects.project.measurements;

import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.dataview.GridLazyDataView;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.icon.VaadinIcon;
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
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import life.qbic.application.commons.SortOrder;
import life.qbic.datamanager.ClientDetailsProvider;
import life.qbic.datamanager.views.Context;
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
import life.qbic.projectmanagement.domain.model.measurement.ProteomicsSpecificMeasurementMetadata;
import life.qbic.projectmanagement.domain.model.sample.Sample;
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
  private final StreamResource rorIconResource = new StreamResource("ROR_logo.svg",
      () -> getClass().getClassLoader().getResourceAsStream("icons/ROR_logo.svg"));
  private final ClientDetailsProvider clientDetailsProvider;
  private final List<ComponentEventListener<MeasurementSelectionChangedEvent>> listeners = new ArrayList<>();
  private transient Context context;
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

    registeredMeasurementsTabSheet.addSelectedChangeListener(
        selectedChangeEvent -> resetSelectedMeasurements());
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
    if (!this.searchTerm.equals(searchTerm)) {
      resetSelectedMeasurements();
    }
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
        .setTooltipGenerator(ngsMeasurement -> ngsMeasurement.measurementCode().value())
        .setAutoWidth(true);
    ngsMeasurementGrid.addComponentColumn(measurement -> {
          if (measurement.samplePoolGroup().isEmpty()) {
            return new Span(
                String.join(" ", groupSampleInfoIntoCodeAndLabel(measurement.measuredSamples())));
          }
          MeasurementPooledSamplesDialog measurementPooledSamplesDialog = new MeasurementPooledSamplesDialog(
              measurement);
          Icon expandIcon = VaadinIcon.EXPAND_SQUARE.create();
          expandIcon.addClassName("expand-icon");
          expandIcon.addClickListener(event -> measurementPooledSamplesDialog.open());
          Span expandSpan = new Span(new Span("Pooled sample"), expandIcon);
          expandSpan.addClassName("sample-column-cell");
          return expandSpan;
        })
        .setHeader("Sample IDs")
        .setAutoWidth(true);
    ngsMeasurementGrid.addColumn(NGSMeasurement::facility)
        .setHeader("Facility")
        .setTooltipGenerator(NGSMeasurement::facility)
        .setAutoWidth(true);
    ngsMeasurementGrid.addComponentColumn(
            ngsMeasurement -> renderInstrument().createComponent(
                ngsMeasurement.instrument()))
        .setHeader("Instrument")
        .setTooltipGenerator(
            ngsMeasurement -> ngsMeasurement.instrument().formatted())
        .setAutoWidth(true);
    ngsMeasurementGrid.addComponentColumn(
            ngsMeasurement -> renderOrganisation(ngsMeasurement.organisation()))
        .setHeader("Organisation")
        .setTooltipGenerator(measurement -> measurement.organisation().label())
        .setAutoWidth(true);
    ngsMeasurementGrid.addColumn(NGSMeasurement::sequencingReadType)
        .setHeader("Read type")
        .setTooltipGenerator(NGSMeasurement::sequencingReadType)
        .setAutoWidth(true);
    ngsMeasurementGrid.addColumn(ngsMeasurement -> ngsMeasurement.libraryKit().orElse(""))
        .setHeader("Library kit")
        .setTooltipGenerator(ngsMeasurement -> ngsMeasurement.libraryKit().orElse(""))
        .setAutoWidth(true);
    ngsMeasurementGrid.addColumn(ngsMeasurement -> ngsMeasurement.flowCell().orElse(""))
        .setHeader("Flow cell")
        .setTooltipGenerator(ngsMeasurement -> ngsMeasurement.flowCell().orElse(""))
        .setAutoWidth(true);
    ngsMeasurementGrid.addColumn(
            ngsMeasurement -> ngsMeasurement.sequencingRunProtocol().orElse(""))
        .setHeader("Run protocol")
        .setTooltipGenerator(ngsMeasurement -> ngsMeasurement.sequencingRunProtocol().orElse(""))
        .setAutoWidth(true);
    ngsMeasurementGrid.addColumn(ngsMeasurement -> ngsMeasurement.flowCell().orElse(""))
        .setHeader("Flow Cell")
        .setTooltipGenerator(ngsMeasurement -> ngsMeasurement.flowCell().orElse(""))
        .setAutoWidth(true);
    ngsMeasurementGrid.addColumn(
            ngsMeasurement -> asClientLocalDateTime(ngsMeasurement.registrationDate())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
        .setHeader("Registration Date")
        .setTooltipGenerator(
            ngsMeasurement -> asClientLocalDateTime(ngsMeasurement.registrationDate())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
        .setAutoWidth(true);
    ngsMeasurementGrid.addColumn(ngsMeasurement -> ngsMeasurement.comment().orElse(""))
        .setHeader("Comment")
        .setTooltipGenerator(ngsMeasurement -> ngsMeasurement.comment().orElse(""))
        .setAutoWidth(true);
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
            proteomicsMeasurement -> proteomicsMeasurement.measurementCode().value())
        .setAutoWidth(true);
    proteomicsMeasurementGrid.addComponentColumn(measurement -> {
          if (!measurement.isPooledSampleMeasurement()) {
            return new Span(
                String.join(" ", groupSampleInfoIntoCodeAndLabel(measurement.measuredSamples())));
          }
          MeasurementPooledSamplesDialog measurementPooledSamplesDialog = new MeasurementPooledSamplesDialog(
              measurement);
          Icon expandIcon = VaadinIcon.EXPAND_SQUARE.create();
          expandIcon.addClassName("expand-icon");
          expandIcon.addClickListener(event -> measurementPooledSamplesDialog.open());
          Span expandSpan = new Span(new Span("Pooled sample"), expandIcon);
          expandSpan.addClassName("sample-column-cell");
          return expandSpan;
        })
        .setHeader("Sample IDs")
        .setAutoWidth(true);
    proteomicsMeasurementGrid.addComponentColumn(
            proteomicsMeasurement -> renderOrganisation(proteomicsMeasurement.organisation()))
        .setHeader("Organisation")
        .setTooltipGenerator(measurement -> measurement.organisation().label())
        .setAutoWidth(true);
    proteomicsMeasurementGrid.addColumn(ProteomicsMeasurement::facility)
        .setHeader("Facility")
        .setTooltipGenerator(ProteomicsMeasurement::facility)
        .setAutoWidth(true);
    proteomicsMeasurementGrid.addComponentColumn(
            proteomicsMeasurement -> renderInstrument().createComponent(
                proteomicsMeasurement.instrument()))
        .setHeader("Instrument")
        .setTooltipGenerator(
            proteomicsMeasurement -> proteomicsMeasurement.instrument().formatted())
        .setAutoWidth(true);
    proteomicsMeasurementGrid.addColumn(ProteomicsMeasurement::digestionEnzyme)
        .setHeader("Digestion Enzyme").setTooltipGenerator(
            ProteomicsMeasurement::digestionEnzyme)
        .setAutoWidth(true);
    proteomicsMeasurementGrid.addColumn(ProteomicsMeasurement::digestionMethod)
        .setHeader("Digestion Method")
        .setTooltipGenerator(ProteomicsMeasurement::digestionMethod)
        .setAutoWidth(true);
    proteomicsMeasurementGrid.addColumn(ProteomicsMeasurement::injectionVolume)
        .setHeader("Injection Volume")
        .setTooltipGenerator(measurement -> String.valueOf(measurement.injectionVolume()))
        .setAutoWidth(true);
    proteomicsMeasurementGrid.addColumn(ProteomicsMeasurement::lcmsMethod)
        .setHeader("LCMS")
        .setTooltipGenerator(ProteomicsMeasurement::lcmsMethod)
        .setAutoWidth(true);
    proteomicsMeasurementGrid.addColumn(ProteomicsMeasurement::lcColumn)
        .setHeader("LC column")
        .setTooltipGenerator(ProteomicsMeasurement::lcColumn)
        .setAutoWidth(true);
    proteomicsMeasurementGrid.addColumn(ProteomicsMeasurement::enrichmentMethod)
        .setHeader("Enrichment")
        .setTooltipGenerator(ProteomicsMeasurement::enrichmentMethod)
        .setAutoWidth(true);
    proteomicsMeasurementGrid.addColumn(
            measurement -> asClientLocalDateTime(measurement.registrationDate())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
        .setHeader("Registration Date")
        .setTooltipGenerator(measurement -> asClientLocalDateTime(measurement.registrationDate())
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
        .setAutoWidth(true);
    proteomicsMeasurementGrid.addColumn(measurement -> measurement.comment().orElse(""))
        .setHeader("Comment")
        .setTooltipGenerator(measurement -> measurement.comment().orElse(""))
        .setAutoWidth(true);
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
    proteomicsMeasurementGrid.addSelectListener(
        event -> updateSelectedMeasurementsInfo(event.isFromClient()));
    measurementsGridDataViews.add(proteomicsGridDataView);
  }

  private void updateSelectedMeasurementsInfo(boolean isFromClient) {
    listeners.forEach(listener -> listener.onComponentEvent(
        new MeasurementSelectionChangedEvent(this, isFromClient)));
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

  public int getNumberOfSelectedMeasurements() {
    Optional<String> tabLabel = getSelectedTabName();
    if (tabLabel.isPresent()) {
      String label = tabLabel.get();
      if (label.equals("Proteomics")) {
        return getSelectedProteomicsMeasurements().size();
      }
      if (label.equals("Genomics")) {
        return getSelectedNGSMeasurements().size();
      }
    }
    return 0;
  }

  public Set<NGSMeasurement> getSelectedNGSMeasurements() {
    return new HashSet<>(ngsMeasurementGrid.getSelectedItems());
  }

  public Set<ProteomicsMeasurement> getSelectedProteomicsMeasurements() {
    return new HashSet<>(proteomicsMeasurementGrid.getSelectedItems());
  }

  public void refreshGrids() {
    resetSelectedMeasurements();
    measurementsGridDataViews.forEach(AbstractDataView::refreshAll);
  }

  private void resetSelectedMeasurements() {
    proteomicsMeasurementGrid.clearSelectedItems();
    ngsMeasurementGrid.clearSelectedItems();
    updateSelectedMeasurementsInfo(false);
  }

  public void addListener(ComponentEventListener<MeasurementSelectionChangedEvent> listener) {
    listeners.add(listener);
  }

  //TODO introduce custom tab with label and updateable count
  public Optional<String> getSelectedTabName() {
    if (registeredMeasurementsTabSheet.getSelectedTab() != null) {
      return Optional.of(registeredMeasurementsTabSheet.getSelectedTab().getLabel());
    } else {
      return Optional.empty();
    }
  }

  /**
   * <b>Measurement Selection Changed Event</b>
   * <p>
   * Event that indicates that measurements were selected or deselected by the user or a deletion
   * event {@link MeasurementDetailsComponent}
   *
   * @since 1.0.0
   */
  public static class MeasurementSelectionChangedEvent extends
      ComponentEvent<MeasurementDetailsComponent> {

    @Serial
    private static final long serialVersionUID = 1213984633337676231L;

    public MeasurementSelectionChangedEvent(MeasurementDetailsComponent source,
        boolean fromClient) {
      super(source, fromClient);
    }
  }

  public class MeasurementPooledSamplesDialog extends Dialog {

    /**
     * Creates an empty dialog.
     */
    private final Div measurementDetailsDiv = new Div();
    private final Span measurementIdSpan = new Span();

    public MeasurementPooledSamplesDialog(ProteomicsMeasurement proteomicsMeasurement) {
      setLayout();
      setMeasurementId(proteomicsMeasurement.measurementCode().value());
      setPooledProteomicsMeasurementDetails(proteomicsMeasurement);
      setPooledProteomicSampleDetails(proteomicsMeasurement.specificMetadata());
    }

    public MeasurementPooledSamplesDialog(NGSMeasurement ngsMeasurement) {
      setLayout();
      setMeasurementId(ngsMeasurement.measurementCode().value());
      setPooledNgsMeasurementDetails(ngsMeasurement);
      //Todo Replace with specific metadata
      setPooledNgsSampleDetails(ngsMeasurement);
    }

    private void setLayout() {
      setDialogHeader();
      measurementIdSpan.addClassName("bold");
      add(measurementIdSpan);
      measurementDetailsDiv.addClassName("pooled-measurement-details");
      add(measurementDetailsDiv);
      Button closeButton = new Button("Close");
      closeButton.addClickListener(event -> close());
      getFooter().add(closeButton);
      addClassName("measurement-pooled-samples-dialog");
    }

    private void setMeasurementId(String measurementId) {
      measurementIdSpan.setText(String.format("Measurement ID: %s", measurementId));
    }

    private void setPooledProteomicsMeasurementDetails(
        ProteomicsMeasurement proteomicsMeasurement) {
      measurementDetailsDiv.add(
          pooledMeasurementEntry("Sample Pool Group", proteomicsMeasurement.samplePoolGroup()
              .orElseThrow()));
      measurementDetailsDiv.add(
          pooledMeasurementEntry("Labeling Type", proteomicsMeasurement.labelType()));
    }

    private void setPooledProteomicSampleDetails(
        Collection<ProteomicsSpecificMeasurementMetadata> proteomicsSpecificMeasurementMetadata) {
      Grid<ProteomicsSpecificMeasurementMetadata> sampleDetailsGrid = new Grid<>();
      sampleDetailsGrid.addColumn(
              metadata -> retrieveSampleById(metadata.measuredSample()).orElseThrow().label())
          .setHeader("Sample Label")
          .setTooltipGenerator(
              metadata -> retrieveSampleById(metadata.measuredSample()).orElseThrow().label())
          .setAutoWidth(true);
      sampleDetailsGrid.addColumn(
              metadata -> retrieveSampleById(metadata.measuredSample()).orElseThrow().sampleCode()
                  .code())
          .setHeader("Sample Id")
          .setTooltipGenerator(
              metadata -> retrieveSampleById(metadata.measuredSample()).orElseThrow().sampleCode()
                  .code())
          .setAutoWidth(true);
      sampleDetailsGrid.addColumn(ProteomicsSpecificMeasurementMetadata::fractionName)
          .setHeader("Fraction Name")
          .setTooltipGenerator(ProteomicsSpecificMeasurementMetadata::fractionName)
          .setAutoWidth(true);
      sampleDetailsGrid.addColumn(ProteomicsSpecificMeasurementMetadata::label)
          .setHeader("Measurement Label")
          .setTooltipGenerator(ProteomicsSpecificMeasurementMetadata::label)
          .setAutoWidth(true);
      sampleDetailsGrid.setItems(proteomicsSpecificMeasurementMetadata);
      add(sampleDetailsGrid);
    }

    private void setPooledNgsMeasurementDetails(
        NGSMeasurement ngsMeasurement) {
      measurementDetailsDiv.add(
          pooledMeasurementEntry("Sample Pool Group", ngsMeasurement.samplePoolGroup()
              .orElseThrow()));
      //Todo Add measurement specific pooled properties once defined for NGS
    }

    private void setPooledNgsSampleDetails(NGSMeasurement ngsMeasurement) {
      Grid<SampleId> sampleDetailsGrid = new Grid<>();
      //Todo Wire pooled sample specific metadata once defined for NGS
      sampleDetailsGrid.addColumn(sampleId -> retrieveSampleById(sampleId).orElseThrow().label())
          .setHeader("Sample Label")
          .setTooltipGenerator(sampleId -> retrieveSampleById(sampleId).orElseThrow().label())
          .setAutoWidth(true);
      sampleDetailsGrid.addColumn(
              sampleId -> retrieveSampleById(sampleId).orElseThrow().sampleCode().code())
          .setHeader("Sample Id")
          .setTooltipGenerator(
              sampleId -> retrieveSampleById(sampleId).orElseThrow().sampleCode().code())
          .setAutoWidth(true);
      sampleDetailsGrid.addColumn(sampleId -> ngsMeasurement.indexI5().orElse(""))
          .setHeader("Index I5")
          .setTooltipGenerator(sampleId -> ngsMeasurement.indexI5().orElse(""))
          .setAutoWidth(true);
      sampleDetailsGrid.addColumn(sampleId -> ngsMeasurement.indexI7().orElse(""))
          .setHeader("Index I7")
          .setTooltipGenerator(sampleId -> ngsMeasurement.indexI7().orElse(""))
          .setAutoWidth(true);
      sampleDetailsGrid.setItems(ngsMeasurement.measuredSamples());
      add(sampleDetailsGrid);
    }

    //Todo This is non-performant and should be changed
    private Optional<Sample> retrieveSampleById(SampleId sampleId) {
      return sampleInformationService.findSample(sampleId);
    }

    private void setDialogHeader() {
      setHeaderTitle("View Pooled Measurement");
      Icon closeIcon = VaadinIcon.CLOSE_SMALL.create();
      closeIcon.addClassNames("small", "clickable");
      closeIcon.addClickListener(event -> close());
      getHeader().add(closeIcon);
    }

    private Span pooledMeasurementEntry(String propertyLabel, String propertyValue) {
      Span pooledDetailLabel = new Span(String.format("%s:", propertyLabel));
      pooledDetailLabel.addClassName("label");
      Span pooledDetailValue = new Span(propertyValue);
      pooledDetailValue.addClassName("value");
      Span pooledDetail = new Span(pooledDetailLabel, pooledDetailValue);
      pooledDetail.addClassName("pooled-detail");
      return pooledDetail;
    }
  }

}
