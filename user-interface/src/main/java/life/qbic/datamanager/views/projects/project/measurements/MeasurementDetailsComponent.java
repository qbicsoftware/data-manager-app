package life.qbic.datamanager.views.projects.project.measurements;

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
import life.qbic.datamanager.views.general.CopyToClipBoardComponent;
import life.qbic.datamanager.views.general.MultiSelectLazyLoadingGrid;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.datamanager.views.general.Tag;
import life.qbic.datamanager.views.general.Tag.TagColor;
import life.qbic.projectmanagement.application.measurement.MeasurementMetadata;
import life.qbic.projectmanagement.application.measurement.MeasurementService;
import life.qbic.projectmanagement.application.sample.SampleInformationService;
import life.qbic.projectmanagement.domain.Organisation;
import life.qbic.projectmanagement.domain.model.OntologyTerm;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.measurement.NGSIndex;
import life.qbic.projectmanagement.domain.model.measurement.NGSMeasurement;
import life.qbic.projectmanagement.domain.model.measurement.NGSSpecificMeasurementMetadata;
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

  public static final String CLICKABLE = "clickable";
  @Serial
  private static final long serialVersionUID = 5086686432247130622L;
  private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm";
  private final TabSheet registeredMeasurementsTabSheet = new TabSheet();
  private final MultiSelectLazyLoadingGrid<NGSMeasurement> ngsMeasurementGrid = new MultiSelectLazyLoadingGrid<>();
  private final MultiSelectLazyLoadingGrid<ProteomicsMeasurement> proteomicsMeasurementGrid = new MultiSelectLazyLoadingGrid<>();
  private final MeasurementTechnologyTab proteomicsTab;
  private final MeasurementTechnologyTab genomicsTab;
  private final Collection<GridLazyDataView<?>> measurementsGridDataViews = new ArrayList<>();
  private final transient MeasurementService measurementService;
  private final transient SampleInformationService sampleInformationService;
  private final List<MeasurementTechnologyTab> tabsInTabSheet = new ArrayList<>();
  private final StreamResource rorIconResource = new StreamResource("ROR_logo.svg",
      () -> getClass().getClassLoader().getResourceAsStream("icons/ROR_logo.svg"));
  private final transient ClientDetailsProvider clientDetailsProvider;
  private final List<ComponentEventListener<MeasurementSelectionChangedEvent>> listeners = new ArrayList<>();
  private transient Context context;
  private String searchTerm = "";

  public MeasurementDetailsComponent(@Autowired MeasurementService measurementService,
      @Autowired SampleInformationService sampleInformationService,
      ClientDetailsProvider clientDetailsProvider) {
    this.measurementService = Objects.requireNonNull(measurementService);
    this.sampleInformationService = Objects.requireNonNull(sampleInformationService);
    this.clientDetailsProvider = clientDetailsProvider;
    proteomicsTab = new MeasurementTechnologyTab("Proteomics", 0);
    genomicsTab = new MeasurementTechnologyTab("Genomics", 0);
    createProteomicsGrid();
    createNGSMeasurementGrid();
    add(registeredMeasurementsTabSheet);
    registeredMeasurementsTabSheet.addClassName("measurement-tabsheet");
    addClassName("measurement-details-component");
    registeredMeasurementsTabSheet.addSelectedChangeListener(event -> resetSelectedMeasurements());
  }

  /**
   * Provides the {@link ExperimentId} to the {@link GridLazyDataView}s to search the
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
    initializeTabCounts();
  }

  private void initializeTabCounts() {
    genomicsTab.setMeasurementCount((int) measurementService.countNGSMeasurements(
        context.experimentId().orElseThrow()));
    proteomicsTab.setMeasurementCount((int) measurementService.countProteomicsMeasurements(
        context.experimentId().orElseThrow()));
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
      refreshGrids();
    }
    this.searchTerm = searchTerm;
  }

  /*Vaadin provides no easy way to remove all tabs in a tabSheet*/
  private void resetTabsInTabsheet() {
    if (!tabsInTabSheet.isEmpty()) {
      tabsInTabSheet.forEach(registeredMeasurementsTabSheet::remove);
      tabsInTabSheet.clear();
    }
  }

  private void addMeasurementTab(GridLazyDataView<?> gridLazyDataView) {
    if (gridLazyDataView.getItems().findAny().isEmpty()) {
      return;
    }
    if (gridLazyDataView.getItem(0) instanceof ProteomicsMeasurement) {
      tabsInTabSheet.add(proteomicsTab);
      registeredMeasurementsTabSheet.add(proteomicsTab, proteomicsMeasurementGrid);
    }
    if (gridLazyDataView.getItem(0) instanceof NGSMeasurement) {
      tabsInTabSheet.add(genomicsTab);
      registeredMeasurementsTabSheet.add(genomicsTab, ngsMeasurementGrid);
    }
    refreshGrids();
  }

  private void createNGSMeasurementGrid() {
    ngsMeasurementGrid.addClassName("measurement-grid");
    ngsMeasurementGrid.addComponentColumn(ngsMeasurement -> {
          Span measurementCell = new Span();
          String measurementCode = ngsMeasurement.measurementCode().value();
          CopyToClipBoardComponent copyToClipBoardComponent = new CopyToClipBoardComponent(
              measurementCode);
          copyToClipBoardComponent.setIconSize("1em");
          measurementCell.add(new Span(measurementCode), copyToClipBoardComponent);
          measurementCell.addClassName("measurement-column-cell");
          return measurementCell;
        })
        .setHeader("Measurement ID")
        .setAutoWidth(true)
        .setFlexGrow(0);
    ngsMeasurementGrid.addComponentColumn(measurement -> {
          if (measurement.isSingleSampleMeasurement()) {
            return new Span(
                String.join(" ", groupSampleInfoIntoCodeAndLabel(measurement.measuredSamples())));
          }
          return createNGSPooledSampleComponent(measurement);
        })
        .setTooltipGenerator(measurement -> {
          if (measurement.isSingleSampleMeasurement()) {
            return String.join(" ", groupSampleInfoIntoCodeAndLabel(measurement.measuredSamples()));
          } else {
            return "";
          }
        })
        .setHeader("Samples")
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
    ngsMeasurementGrid.addColumn(
            ngsMeasurement -> asClientLocalDateTime(ngsMeasurement.registrationDate())
                .format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT)))
        .setHeader("Registration Date")
        .setTooltipGenerator(
            ngsMeasurement -> asClientLocalDateTime(ngsMeasurement.registrationDate())
                .format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT)))
        .setAutoWidth(true);
    ngsMeasurementGrid.addComponentColumn(measurement -> {
          if (measurement.isSingleSampleMeasurement()) {
            Span singularComment = new Span();
            var optMetadata = measurement.specificMeasurementMetadata().stream().findFirst();
            optMetadata.ifPresent(metadata -> singularComment.setText(metadata.comment().orElse("")));
            return singularComment;
          } else {
            return createNGSPooledSampleComponent(measurement);
          }
        })
        .setHeader("Comment")
        .setTooltipGenerator(measurement -> {
          if (measurement.isSingleSampleMeasurement()) {
            var optMetadata = measurement.specificMeasurementMetadata().stream().findFirst();
            if (optMetadata.isPresent()) {
              return optMetadata.get().comment().orElse("");
            }
          }
          return "";
        })
        .setAutoWidth(true);
    GridLazyDataView<NGSMeasurement> ngsGridDataView = ngsMeasurementGrid.setItems(query -> {
      List<SortOrder> sortOrders = query.getSortOrders().stream().map(
              it -> new SortOrder(it.getSorted(), it.getDirection().equals(SortDirection.ASCENDING)))
          .collect(Collectors.toList());
      sortOrders.add(SortOrder.of("measurementCode").ascending());
      return measurementService.findNGSMeasurements(searchTerm,
              context.experimentId().orElseThrow(),
              query.getOffset(), query.getLimit(), sortOrders, context.projectId().orElseThrow())
          .stream();
    });
    ngsGridDataView
        .addItemCountChangeListener(
            countChangeEvent -> genomicsTab.setMeasurementCount(
                (int) ngsGridDataView.getItems().count()));
    ngsMeasurementGrid.addSelectListener(
        event -> updateSelectedMeasurementsInfo(event.isFromClient()));
    measurementsGridDataViews.add(ngsGridDataView);
  }

  private void createProteomicsGrid() {
    proteomicsMeasurementGrid.addClassName("measurement-grid");
    proteomicsMeasurementGrid.addComponentColumn(measurement -> {
          Span measurementCell = new Span();
          String measurementCode = measurement.measurementCode().value();
          CopyToClipBoardComponent copyToClipBoardComponent = new CopyToClipBoardComponent(
              measurementCode);
          copyToClipBoardComponent.setIconSize("1em");
          measurementCell.add(new Span(measurementCode), copyToClipBoardComponent);
          measurementCell.addClassName("measurement-column-cell");
          return measurementCell;
        })
        .setHeader("Measurement ID")
        .setAutoWidth(true)
        .setFlexGrow(0);
    proteomicsMeasurementGrid.addComponentColumn(measurement -> {
          if (measurement.isSingleSampleMeasurement()) {
            return new Span(
                String.join(" ", groupSampleInfoIntoCodeAndLabel(measurement.measuredSamples())));
          }
          return createProteomicsPooledSampleComponent(measurement);
        })
        .setHeader("Samples")
        .setTooltipGenerator(measurement -> {
          if (measurement.isSingleSampleMeasurement()) {
            return String.join(" ", groupSampleInfoIntoCodeAndLabel(measurement.measuredSamples()));
          }
          return "";
        })
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
                proteomicsMeasurement.msDevice()))
        .setHeader("MS Device")
        .setTooltipGenerator(
            proteomicsMeasurement -> proteomicsMeasurement.msDevice().formatted())
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
                .format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT)))
        .setHeader("Registration Date")
        .setTooltipGenerator(measurement -> asClientLocalDateTime(measurement.registrationDate())
            .format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT)))
        .setAutoWidth(true);
    proteomicsMeasurementGrid.addComponentColumn(measurement -> {
          if (measurement.isSingleSampleMeasurement()) {
            Span singularComment = new Span();
            var optMetadata = measurement.specificMetadata().stream().findFirst();
            optMetadata.ifPresent(metadata -> singularComment.setText(metadata.comment().orElse("")));
            return singularComment;
          } else {
            return createProteomicsPooledSampleComponent(measurement);
          }
        })
        .setHeader("Comment")
        .setTooltipGenerator(measurement -> {
          if (measurement.isSingleSampleMeasurement()) {
            var optMetadata = measurement.specificMetadata().stream().findFirst();
            if (optMetadata.isPresent()) {
              return optMetadata.get().comment().orElse("");
            }
          }
          return "";
        })
        .setAutoWidth(true);
    GridLazyDataView<ProteomicsMeasurement> proteomicsGridDataView = proteomicsMeasurementGrid.setItems(
        query -> {
          List<SortOrder> sortOrders = query.getSortOrders().stream().map(
                  it -> new SortOrder(it.getSorted(),
                      it.getDirection().equals(SortDirection.ASCENDING)))
              .collect(Collectors.toList());
          sortOrders.add(SortOrder.of("measurementCode").ascending());
          return measurementService.findProteomicsMeasurements(searchTerm,
                  context.experimentId().orElseThrow(),
                  query.getOffset(), query.getLimit(), sortOrders, context.projectId().orElseThrow())
              .stream();

        });
    proteomicsGridDataView
        .addItemCountChangeListener(
            countChangeEvent -> proteomicsTab.setMeasurementCount(
                (int) proteomicsGridDataView.getItems().count()));
    proteomicsMeasurementGrid.addSelectListener(
        event -> updateSelectedMeasurementsInfo(event.isFromClient()));
    measurementsGridDataViews.add(proteomicsGridDataView);
  }

  private Span createProteomicsPooledSampleComponent(ProteomicsMeasurement measurement) {
    MeasurementPooledSamplesDialog measurementPooledSamplesDialog = new MeasurementPooledSamplesDialog(
        measurement);
    Icon expandIcon = VaadinIcon.EXPAND_SQUARE.create();
    expandIcon.addClassName("expand-icon");
    Span expandSpan = new Span(new Span("Pooled sample"), expandIcon);
    expandSpan.addClassNames("sample-column-cell", CLICKABLE);
    expandSpan.addClickListener(event -> measurementPooledSamplesDialog.open());
    return expandSpan;
  }

  private Span createNGSPooledSampleComponent(NGSMeasurement measurement) {
    MeasurementPooledSamplesDialog measurementPooledSamplesDialog = new MeasurementPooledSamplesDialog(
        measurement);
    Icon expandIcon = VaadinIcon.EXPAND_SQUARE.create();
    expandIcon.addClassName("expand-icon");
    Span expandSpan = new Span(new Span("Pooled sample"), expandIcon);
    expandSpan.addClassNames("sample-column-cell", CLICKABLE);
    expandSpan.addClickListener(event -> measurementPooledSamplesDialog.open());
    return expandSpan;
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
      Span instrumentOntologyLink = new Span(instrument.getOboId().replace("_", ":"));
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
        .map(sample -> String.format("%s (%s)", sample.label(), sample.sampleCode().code()))
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

  public Optional<String> getSelectedTabName() {
    if (tabsInTabSheet.isEmpty()) {
      return Optional.empty();
    }
    return Optional.ofNullable(registeredMeasurementsTabSheet.getSelectedTab())
        .map(tab -> ((MeasurementTechnologyTab) tab).getTabLabel());
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

  public static class MeasurementTechnologyTab extends Tab {

    private final Span countBadge;
    private final Span technologyNameComponent;
    private final String technology;

    public MeasurementTechnologyTab(String technology, int measurementCount) {
      this.technology = technology;
      technologyNameComponent = new Span();
      this.countBadge = createBadge();
      Span sampleCountComponent = new Span();
      sampleCountComponent.add(countBadge);
      this.add(technologyNameComponent, sampleCountComponent);
      setTechnologyName(technology);
      setMeasurementCount(measurementCount);
      addClassName("tab-with-count");
    }

    /**
     * Helper method for creating a badge.
     */
    private static Span createBadge() {
      Tag tag = new Tag(String.valueOf(0));
      tag.setTagColor(TagColor.CONTRAST);
      return tag;
    }

    public String getTabLabel() {
      return technology;
    }

    /**
     * Setter method for specifying the number of measurements of the technology type shown in this
     * component
     *
     * @param measurementCount number of samples associated with the experiment shown in this
     *                         component
     */
    public void setMeasurementCount(int measurementCount) {
      countBadge.setText(String.valueOf(measurementCount));
    }

    public void setTechnologyName(String technologyName) {
      this.technologyNameComponent.setText(technologyName);
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
      setPooledNgsSampleDetails(ngsMeasurement.specificMeasurementMetadata());
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
          .setHeader("Sample Name")
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
      sampleDetailsGrid.addColumn(ProteomicsSpecificMeasurementMetadata::label)
          .setHeader("Measurement Label")
          .setTooltipGenerator(ProteomicsSpecificMeasurementMetadata::label)
          .setAutoWidth(true);
      sampleDetailsGrid.addColumn(metadata -> metadata.comment().orElse(""))
          .setHeader("comment")
          .setTooltipGenerator(metadata -> metadata.comment().orElse(""))
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

    private void setPooledNgsSampleDetails(
        Collection<NGSSpecificMeasurementMetadata> ngsSpecificMeasurementMetadata) {
      Grid<NGSSpecificMeasurementMetadata> sampleDetailsGrid = new Grid<>();
      sampleDetailsGrid.addColumn(
              metadata -> retrieveSampleById(metadata.measuredSample()).orElseThrow().label())
          .setHeader("Sample Name")
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
      sampleDetailsGrid.addColumn(metadata -> metadata.index().map(NGSIndex::indexI7).orElse(""))
          .setHeader("Index I7")
          .setTooltipGenerator(metadata -> metadata.index().map(NGSIndex::indexI7).orElse(""))
          .setAutoWidth(true);
      sampleDetailsGrid.addColumn(metadata -> metadata.index().map(NGSIndex::indexI5).orElse(""))
          .setHeader("Index I5")
          .setTooltipGenerator(metadata -> metadata.index().map(NGSIndex::indexI5).orElse(""))
          .setAutoWidth(true);
      sampleDetailsGrid.addColumn(metadata -> metadata.comment().orElse(""))
          .setHeader("comment")
          .setTooltipGenerator(metadata -> metadata.comment().orElse(""))
          .setAutoWidth(true);
      sampleDetailsGrid.setItems(ngsSpecificMeasurementMetadata);
      add(sampleDetailsGrid);
    }

    //Todo This is non-performant and should be changed
    private Optional<Sample> retrieveSampleById(SampleId sampleId) {
      return sampleInformationService.findSample(sampleId);
    }

    private void setDialogHeader() {
      setHeaderTitle("View Pooled Measurement");
      Icon closeIcon = VaadinIcon.CLOSE_SMALL.create();
      closeIcon.addClassNames("small", CLICKABLE);
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
