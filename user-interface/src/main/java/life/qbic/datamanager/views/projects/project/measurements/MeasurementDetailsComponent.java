package life.qbic.datamanager.views.projects.project.measurements;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.dataview.GridLazyDataView;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.data.provider.AbstractDataView;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.LocalDateTimeRenderer;
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
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import life.qbic.datamanager.ClientDetailsProvider;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.general.InfoBox;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.projectmanagement.application.SortOrder;
import life.qbic.projectmanagement.application.measurement.MeasurementMetadata;
import life.qbic.projectmanagement.application.measurement.MeasurementService;
import life.qbic.projectmanagement.application.sample.SampleInformationService;
import life.qbic.projectmanagement.domain.Organisation;
import life.qbic.projectmanagement.domain.model.OntologyTerm;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.measurement.NGSMeasurement;
import life.qbic.projectmanagement.domain.model.measurement.ProteomicsMeasurement;
import life.qbic.projectmanagement.domain.model.sample.Sample;
import life.qbic.projectmanagement.domain.model.sample.SampleCode;
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
  private final TabSheet registerMeasurementTabSheet = new TabSheet();
  private final Div noMeasurementDisclaimer = new Div();
  private String searchTerm = "";
  private final Grid<NGSMeasurement> ngsMeasurementGrid = new Grid<>();
  private final Grid<ProteomicsMeasurement> proteomicsMeasurementGrid = new Grid<>();
  private final Collection<GridLazyDataView<?>> measurementsGridDataViews = new ArrayList<>();
  private final transient MeasurementService measurementService;
  private final transient SampleInformationService sampleInformationService;
  private final List<Tab> tabsInTabSheet = new ArrayList<>();
  private transient Context context;
  private final StreamResource rorIconResource = new StreamResource("ROR_logo.svg",
      () -> getClass().getClassLoader().getResourceAsStream("icons/ROR_logo.svg"));

  private final ClientDetailsProvider clientDetailsProvider;

  public MeasurementDetailsComponent(@Autowired MeasurementService measurementService, @Autowired SampleInformationService sampleInformationService, ClientDetailsProvider clientDetailsProvider) {
    this.measurementService = Objects.requireNonNull(measurementService);
    this.sampleInformationService = Objects.requireNonNull(sampleInformationService);
    this.clientDetailsProvider = clientDetailsProvider;
    initNoMeasurementDisclaimer();
    createProteomicsGrid();
    createNGSMeasurementGrid();
    add(noMeasurementDisclaimer);
    add(registerMeasurementTabSheet);
    registerMeasurementTabSheet.addClassName("measurement-tabsheet");
    addClassName("measurement-details-component");
  }

  /**
   * Provides the {@link ExperimentId} to the {@link GridLazyDataView}s to query the
   * {@link MeasurementMetadata} shown in the grids of this component
   *
   * @param context Context with the projectId and experimentId containing the samples for which
   *                     measurements could be registered
   */
  public void setContext(Context context) {
    resetTabsInTabsheet();
    this.context = context;
    List<GridLazyDataView<?>> dataViewsWithItems = measurementsGridDataViews.stream()
        .filter(gridLazyDataView -> gridLazyDataView.getItems()
            .findAny().isPresent()).toList();
    /*If none of the measurement types have items show default state with noMeasurement Disclaimer*/
    if (dataViewsWithItems.isEmpty()) {
      noMeasurementDisclaimer.setVisible(true);
      registerMeasurementTabSheet.setVisible(false);
      return;
    }
    noMeasurementDisclaimer.setVisible(false);
    dataViewsWithItems.forEach(this::addMeasurementTab);
    registerMeasurementTabSheet.setVisible(true);
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


  /**
   * Informs the listener that a {@link MeasurementAddClickEvent} has occurred within the disclaimer
   * of this component
   *
   * @param addMeasurementListener listener which will be informed if a
   *                               {@link MeasurementAddClickEvent} has been fired
   */
  public void addRegisterMeasurementClickedListener(
      ComponentEventListener<MeasurementAddClickEvent> addMeasurementListener) {
    addListener(MeasurementAddClickEvent.class, addMeasurementListener);
  }

  /*Vaadin provides no easy way to remove all tabs in a tabSheet*/
  private void resetTabsInTabsheet() {
    if (!tabsInTabSheet.isEmpty()) {
      tabsInTabSheet.forEach(registerMeasurementTabSheet::remove);
      tabsInTabSheet.clear();
    }
  }

  private void addMeasurementTab(GridLazyDataView<?> gridLazyDataView) {
    if (gridLazyDataView.getItem(0) instanceof ProteomicsMeasurement) {
      tabsInTabSheet.add(registerMeasurementTabSheet.add("Proteomics", proteomicsMeasurementGrid));
    }
    if (gridLazyDataView.getItem(0) instanceof NGSMeasurement) {
      tabsInTabSheet.add(registerMeasurementTabSheet.add("Genomics", ngsMeasurementGrid));
    }
  }

  private void createNGSMeasurementGrid() {
    ngsMeasurementGrid.addClassName("measurement-grid");
    ngsMeasurementGrid.addColumn(ngsMeasurement -> ngsMeasurement.measurementCode().value())
        .setHeader("Measurement Code")
        .setAutoWidth(true)
        .setTooltipGenerator(
            ngsMeasurement -> ngsMeasurement.measurementCode().value())
        .setFlexGrow(0);
    ngsMeasurementGrid.addComponentColumn(ngsMeasurement -> renderSampleCodes()
            .createComponent(ngsMeasurement.measuredSamples()))
        .setHeader("Sample Codes")
        .setAutoWidth(true);
    ngsMeasurementGrid.addComponentColumn(ngsMeasurement -> renderOrganisation()
            .createComponent(ngsMeasurement.organisation()))
        .setHeader("Organisation")
        .setTooltipGenerator(ngsMeasurement -> ngsMeasurement.organisation().label())
        .setAutoWidth(true)
        .setFlexGrow(0);
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
        .setAutoWidth(true)
        .setFlexGrow(0);
    ngsMeasurementGrid.addColumn(
            NGSMeasurement::sequencingReadType)
        .setHeader("Read Type")
        .setTooltipGenerator(NGSMeasurement::sequencingReadType)
        .setAutoWidth(true);
    ngsMeasurementGrid.addColumn(
            ngsMeasurement -> ngsMeasurement.libraryKit().orElse(""))
        .setHeader("Library Kit")
        .setTooltipGenerator(ngsMeasurement -> ngsMeasurement.libraryKit().orElse(""))
        .setAutoWidth(true);
    ngsMeasurementGrid.addColumn(
            ngsMeasurement -> ngsMeasurement.flowCell().orElse(""))
        .setHeader("Flow Cell")
        .setTooltipGenerator(ngsMeasurement -> ngsMeasurement.flowCell().orElse(""))
        .setAutoWidth(true);
    ngsMeasurementGrid.addColumn(
            ngsMeasurement -> ngsMeasurement.sequencingRunProtocol().orElse(""))
        .setHeader("Run Protocol")
        .setTooltipGenerator(ngsMeasurement -> ngsMeasurement.sequencingRunProtocol().orElse(""))
        .setAutoWidth(true);
    ngsMeasurementGrid.addColumn(ngsMeasurement -> ngsMeasurement.indexI7().orElse(""))
        .setHeader("Index I7")
        .setTooltipGenerator(ngsMeasurement -> ngsMeasurement.indexI7().orElse(""))
        .setAutoWidth(true);
    ngsMeasurementGrid.addColumn(
            ngsMeasurement -> ngsMeasurement.indexI5().orElse(""))
        .setHeader("Index I5")
        .setTooltipGenerator(ngsMeasurement -> ngsMeasurement.indexI5().orElse(""))
        .setAutoWidth(true);
    ngsMeasurementGrid.addColumn(new LocalDateTimeRenderer<>(
            ngsMeasurement -> asClientLocalDateTime(ngsMeasurement.registrationDate()),
            "yyyy-MM-dd"))
        .setKey("registrationDate")
        .setHeader("Registration Date")
        .setTooltipGenerator(ngsMeasurement -> {
          LocalDateTime dateTime = asClientLocalDateTime(ngsMeasurement.registrationDate());
          return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd 'at' hh:mm a"));
        })
        .setAutoWidth(true);
    ngsMeasurementGrid.addColumn(ngsMeasurement -> ngsMeasurement.comment().orElse(""))
        .setHeader("Comment")
        .setTooltipGenerator(ngsMeasurement -> ngsMeasurement.comment().orElse(""))
        .setAutoWidth(true);
    ngsMeasurementGrid.addThemeVariants(GridVariant.LUMO_WRAP_CELL_CONTENT);
    GridLazyDataView<NGSMeasurement> ngsGridDataView = ngsMeasurementGrid.setItems(query -> {
      List<SortOrder> sortOrders = query.getSortOrders().stream().map(
              it -> new SortOrder(it.getSorted(), it.getDirection().equals(SortDirection.ASCENDING)))
          .collect(Collectors.toList());
      // if no order is provided by the grid order by last modified (least priority)
      sortOrders.add(SortOrder.of("measurementCode").ascending());
      return measurementService.findNGSMeasurements(searchTerm,
          context.experimentId().orElseThrow(),
          query.getOffset(), query.getLimit(), sortOrders, context.projectId().orElseThrow()).stream();
    });
    measurementsGridDataViews.add(ngsGridDataView);
  }

  private void createProteomicsGrid() {
    proteomicsMeasurementGrid.addClassName("measurement-grid");
    proteomicsMeasurementGrid.addColumn(
            proteomicsMeasurement -> proteomicsMeasurement.measurementCode().value())
        .setHeader("Measurement Code")
        .setAutoWidth(true)
        .setTooltipGenerator(
            proteomicsMeasurement -> proteomicsMeasurement.measurementCode().value())
        .setFlexGrow(0);
    proteomicsMeasurementGrid.addColumn(
            proteomicsMeasurement -> proteomicsMeasurement.label().orElse(""))
        .setHeader("Measurement Label")
        .setTooltipGenerator(proteomicsMeasurement -> proteomicsMeasurement.label().orElse(""))
        .setAutoWidth(true)
        .setFlexGrow(1);
    proteomicsMeasurementGrid.addColumn(
            proteomicsMeasurement -> proteomicsMeasurement.labelingType().orElse(""))
        .setHeader("Measurement Label Type")
        .setTooltipGenerator(
            proteomicsMeasurement -> proteomicsMeasurement.labelingType().orElse(""))
        .setAutoWidth(true)
        .setFlexGrow(1);
    proteomicsMeasurementGrid.addComponentColumn(proteomicsMeasurement -> renderSampleCodes()
            .createComponent(proteomicsMeasurement.measuredSamples()))
        .setHeader("Sample Codes")
        .setAutoWidth(true);
    proteomicsMeasurementGrid.addComponentColumn(proteomicsMeasurement -> renderOrganisation()
            .createComponent(proteomicsMeasurement.organisation()))
        .setHeader("Organisation")
        .setTooltipGenerator(proteomicsMeasurement -> proteomicsMeasurement.organisation().label())
        .setAutoWidth(true)
        .setFlexGrow(0);
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
        .setAutoWidth(true)
        .setFlexGrow(0);
    proteomicsMeasurementGrid.addColumn(
            proteomicsMeasurement -> proteomicsMeasurement.fraction().orElse(""))
        .setHeader("Fraction Name")
        .setTooltipGenerator(proteomicsMeasurement -> proteomicsMeasurement.fraction().orElse(""))
        .setAutoWidth(true);
    proteomicsMeasurementGrid.addColumn(ProteomicsMeasurement::digestionMethod)
        .setHeader("Digestion Method")
        .setTooltipGenerator(ProteomicsMeasurement::digestionMethod)
        .setAutoWidth(true);
    proteomicsMeasurementGrid.addColumn(ProteomicsMeasurement::digestionEnzyme)
        .setHeader("Digestion Enzyme")
        .setTooltipGenerator(ProteomicsMeasurement::digestionEnzyme)
        .setAutoWidth(true);
    proteomicsMeasurementGrid.addColumn(ProteomicsMeasurement::enrichmentMethod)
        .setHeader("Enrichment Method")
        .setTooltipGenerator(ProteomicsMeasurement::enrichmentMethod)
        .setAutoWidth(true);
    proteomicsMeasurementGrid.addColumn(ProteomicsMeasurement::injectionVolume)
        .setHeader("Injection Volume")
        .setTooltipGenerator(
            proteomicsMeasurement -> String.valueOf(proteomicsMeasurement.injectionVolume()))
        .setAutoWidth(true);
    proteomicsMeasurementGrid.addColumn(ProteomicsMeasurement::lcColumn)
        .setHeader("LC Column")
        .setTooltipGenerator(ProteomicsMeasurement::lcColumn)
        .setAutoWidth(true);
    proteomicsMeasurementGrid.addColumn(ProteomicsMeasurement::lcmsMethod)
        .setHeader("LCMS Method")
        .setTooltipGenerator(ProteomicsMeasurement::lcmsMethod)
        .setAutoWidth(true);
    proteomicsMeasurementGrid.addColumn(
            proteomicsMeasurement -> proteomicsMeasurement.samplePoolGroup().orElse(""))
        .setHeader("Sample Pool Group")
        .setTooltipGenerator(
            proteomicsMeasurement -> proteomicsMeasurement.samplePoolGroup().orElse(""))
        .setAutoWidth(true);
    proteomicsMeasurementGrid.addColumn(new LocalDateTimeRenderer<>(
                    proteomicsMeasurement -> asClientLocalDateTime(proteomicsMeasurement.registrationDate()),
                    "yyyy-MM-dd"))
            .setKey("registrationDate")
            .setHeader("Registration Date")
            .setTooltipGenerator(proteomicsMeasurement -> {
              LocalDateTime dateTime = asClientLocalDateTime(proteomicsMeasurement.registrationDate());
              return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd 'at' hh:mm a"));
            })
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
          return measurementService.findProteomicsMeasurement(searchTerm,
              context.experimentId().orElseThrow(),
              query.getOffset(), query.getLimit(), sortOrders, context.projectId().orElseThrow()).stream();
        });
    measurementsGridDataViews.add(proteomicsGridDataView);
  }

  private LocalDateTime asClientLocalDateTime(Instant instant) {
    ZonedDateTime zonedDateTime = instant.atZone(ZoneId.of(
            this.clientDetailsProvider.latestDetails().map(ClientDetailsProvider.ClientDetails::timeZoneId).orElse("UTC")));
    return zonedDateTime.toLocalDateTime();
  }

  private ComponentRenderer<Anchor, Organisation> renderOrganisation() {
    return new ComponentRenderer<>(organisation-> {
      SvgIcon svgIcon = new SvgIcon(rorIconResource);
      svgIcon.addClassName("organisation-icon");
      Span organisationLabel = new Span(organisation.label());
      String organisationUrl = organisation.IRI();
      Anchor organisationAnchor = new Anchor(organisationUrl, organisationLabel, svgIcon);
      organisationAnchor.setTarget(AnchorTarget.BLANK);
      organisationAnchor.addClassName("organisation-column-entry");
      return organisationAnchor;
    });
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

  private ComponentRenderer<Div, Collection<SampleId>> renderSampleCodes() {
    return new ComponentRenderer<>(sampleIds-> {
      Div showSampleCodes = new Div();
        List<SampleCode> sampleCodes = sampleInformationService.retrieveSamplesByIds(sampleIds).stream().map(Sample::sampleCode).toList();
        showSampleCodes.addClassName("sample-code-column");
        sampleCodes.forEach(sampleCode -> showSampleCodes.add(new Span(sampleCode.code())));
        return showSampleCodes;
    });
  }

  private void initNoMeasurementDisclaimer() {
    Span disclaimerTitle = new Span("Manage your measurement metadata");
    disclaimerTitle.addClassName("no-measurement-registered-title");
    noMeasurementDisclaimer.add(disclaimerTitle);
    Div noMeasurementDisclaimerContent = new Div();
    noMeasurementDisclaimerContent.addClassName("no-measurement-registered-content");
    Span noMeasurementText1 = new Span("Start by downloading the required metadata template");
    Span noMeasurementText2 = new Span(
        "Fill the metadata sheet and register your measurement metadata.");
    noMeasurementDisclaimerContent.add(noMeasurementText1);
    noMeasurementDisclaimerContent.add(noMeasurementText2);
    noMeasurementDisclaimer.add(noMeasurementDisclaimerContent);
    InfoBox availableTemplatesInfo = new InfoBox();
    availableTemplatesInfo.setInfoText(
        "You can download the measurement metadata template from the Templates component above");
    availableTemplatesInfo.setClosable(false);
    noMeasurementDisclaimer.add(availableTemplatesInfo);
    Button registerMeasurements = new Button("Register Measurements");
    registerMeasurements.addClassName("primary");
    noMeasurementDisclaimer.add(registerMeasurements);
    registerMeasurements.addClickListener(
        event -> fireEvent(new MeasurementAddClickEvent(this, event.isFromClient())));
    noMeasurementDisclaimer.addClassName("no-measurements-registered-disclaimer");
  }


  /**
   * Measurement Add Click Event
   * <p></p>
   * ComponentEvent which informs the system that {@link MeasurementMetadata} is intended to be
   * added to the system
   */
  public static class MeasurementAddClickEvent extends
      ComponentEvent<MeasurementDetailsComponent> {

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public MeasurementAddClickEvent(MeasurementDetailsComponent source, boolean fromClient) {
      super(source, fromClient);
    }
  }
}
