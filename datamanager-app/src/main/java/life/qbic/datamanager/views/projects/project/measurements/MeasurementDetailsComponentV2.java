package life.qbic.datamanager.views.projects.project.measurements;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.spring.data.VaadinSpringDataHelpers;
import java.io.Serializable;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import life.qbic.application.commons.ApplicationException;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.datamanager.views.general.dialog.AppDialog;
import life.qbic.datamanager.views.general.dialog.DialogBody;
import life.qbic.datamanager.views.general.dialog.DialogFooter;
import life.qbic.datamanager.views.general.dialog.DialogHeader;
import life.qbic.datamanager.views.general.dialog.DialogSection;
import life.qbic.datamanager.views.general.grid.component.FilterGrid;
import life.qbic.datamanager.views.general.grid.component.FilterGridTab;
import life.qbic.datamanager.views.general.grid.component.FilterGridTabSheet;
import life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory;
import life.qbic.projectmanagement.application.measurement.NgsMeasurementLookup;
import life.qbic.projectmanagement.application.measurement.NgsMeasurementLookup.MeasurementFilter;
import life.qbic.projectmanagement.application.measurement.NgsMeasurementLookup.MeasurementInfo;
import life.qbic.projectmanagement.application.measurement.NgsMeasurementLookup.NgsSortKey;
import life.qbic.projectmanagement.application.measurement.NgsMeasurementLookup.SampleInfo;
import life.qbic.projectmanagement.application.measurement.PxpMeasurementLookup;
import life.qbic.projectmanagement.infrastructure.experiment.measurement.jpa.NgsMeasurementJpaRepository.NgsMeasurementFilter;
import life.qbic.projectmanagement.infrastructure.experiment.measurement.jpa.PxpMeasurementJpaRepository.PxpMeasurementFilter;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

/**
 * A component to show detailed information about existing measurements within an experiment.
 */
public class MeasurementDetailsComponentV2 extends PageArea implements Serializable {

  private static final StreamResource ROR_ICON_RESOURCE = new StreamResource("ROR_logo.svg",
      () -> MeasurementDetailsComponentV2.class.getClassLoader()
          .getResourceAsStream("icons/ROR_logo.svg"));
  private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm";
  private final AtomicReference<String> clientTimeZone = new AtomicReference<>();

  private final MessageSourceNotificationFactory messageFactory;

  private final FilterGridTabSheet tabSheet;
  private final SearchTermFilter ngsSearchTermFilter = SearchTermFilter.empty();
  private final SearchTermFilter pxpSearchTermFilter = SearchTermFilter.empty();

  private final transient NgsMeasurementLookup ngsMeasurementLookup;
  private final transient PxpMeasurementLookup pxpMeasurementLookup; /*TODO add service in the middle*/
  private FilterGrid<MeasurementInfo, SearchTermFilter> filterGridNgs;
  private FilterGrid<PxpMeasurementLookup.MeasurementInfo, SearchTermFilter> filterGridPxp;

  /**
   * A filter containing a search term
   *
   * @param searchTerm
   */
  record SearchTermFilter(String searchTerm) implements Serializable {

    static SearchTermFilter empty() {
      return new SearchTermFilter("");
    }

    public SearchTermFilter replaceWith(String searchTerm) {
      return new SearchTermFilter(searchTerm);
    }
  }

  public MeasurementDetailsComponentV2(
      MessageSourceNotificationFactory messageFactory,
      NgsMeasurementLookup ngsMeasurementLookup,
      PxpMeasurementLookup pxpMeasurementLookup) {
    this.messageFactory = requireNonNull(messageFactory);
    this.ngsMeasurementLookup = requireNonNull(ngsMeasurementLookup);
    this.pxpMeasurementLookup = requireNonNull(pxpMeasurementLookup);
    addClassNames("measurement-details-component");

    //setup tab sheet
    tabSheet = new FilterGridTabSheet();
    tabSheet.showPrimaryFeatureButton();
    tabSheet.setCaptionPrimaryAction("Register Measurements");
    tabSheet.showPrimaryFeatureButton();
    tabSheet.setCaptionFeatureAction("Export");
    add(tabSheet);
  }

  public void refreshNgs() {
    Optional.ofNullable(filterGridNgs)
        .ifPresent(FilterGrid::refreshAll);
  }

  public void refreshPxp() {
    Optional.ofNullable(filterGridPxp)
        .ifPresent(FilterGrid::refreshAll);
  }

  public void setContext(Context context) {
    validateContext(context);
    String projectId = context.projectId().orElseThrow().value();
    String experimentId = context.experimentId().orElseThrow().value();

    // for each domain, create a grid
    var ngsGrid = createNgsGrid();
    var pxpGrid = createPxpGrid();

    // for each domain, configure a filter grid
    filterGridNgs = FilterGrid.<MeasurementInfo, SearchTermFilter>lazy(
        MeasurementInfo.class,
        SearchTermFilter.class,
        ngsGrid,
        this::getNgsSearchTermFilter,
        query ->
        {

          String searchTerm = query.getFilter().map(SearchTermFilter::searchTerm).orElse(
              "");

          return ngsMeasurementLookup.lookupNgsMeasurements(
              projectId, query.getOffset(), query.getLimit(),
              VaadinSpringDataHelpers.toSpringDataSort(query),
              new MeasurementFilter(experimentId, searchTerm));
        },
        query ->
        {
          String searchTerm = query.getFilter().map(SearchTermFilter::searchTerm).orElse(
              "");

          return ngsMeasurementLookup.countNgsMeasurements(projectId,
              new MeasurementFilter(experimentId, searchTerm));
        },
        (searchTerm, filter) -> filter.replaceWith(searchTerm));
    filterGridNgs.itemDisplayLabel("measurement");
    var editNgsButton = new Button("Edit");
    editNgsButton.addClickListener(clicked -> {
      Set<MeasurementInfo> selectedMeasurements = filterGridNgs.selectedElements();
      List<String> selectedMeasurementIds = selectedMeasurements
          .stream()
          .map(MeasurementInfo::measurementId)
          .distinct()
          .toList();
      if (selectedMeasurementIds.isEmpty()) {
        displayMissingSelectionNote();
        return;
      }
      fireEvent(new NgsMeasurementEditRequested(selectedMeasurementIds, this, true));
    });

    var deleteNgsButton = new Button("Delete");
    deleteNgsButton.addClickListener(clicked -> {
      Set<MeasurementInfo> selectedMeasurements = filterGridNgs.selectedElements();
      List<String> selectedMeasurementIds = selectedMeasurements
          .stream()
          .map(MeasurementInfo::measurementId)
          .distinct()
          .toList();
      if (selectedMeasurementIds.isEmpty()) {
        displayMissingSelectionNote();
        return;
      }
      fireEvent(new NgsMeasurementDeletionRequested(selectedMeasurementIds, this, true));
    });
    filterGridNgs.setSecondaryActionGroup(deleteNgsButton, editNgsButton);

    filterGridPxp = FilterGrid.lazy(
        PxpMeasurementLookup.MeasurementInfo.class,
        SearchTermFilter.class,
        pxpGrid,
        this::getPxpSearchTermFilter,
        query ->
        {

          String searchTerm = query.getFilter().map(SearchTermFilter::searchTerm)
              .orElse("");

          return pxpMeasurementLookup.lookupPxpMeasurements(
              projectId, query.getOffset(), query.getLimit(),
              VaadinSpringDataHelpers.toSpringDataSort(query),
              new PxpMeasurementLookup.MeasurementFilter(experimentId, searchTerm));
        },
        query ->
        {
          var searchTerm = query.getFilter().map(SearchTermFilter::searchTerm)
              .orElse("");
          return pxpMeasurementLookup.countPxpMeasurements(projectId,
              new PxpMeasurementLookup.MeasurementFilter(experimentId, searchTerm));
        },
        (searchTerm, filter) -> filter.replaceWith(searchTerm));
    filterGridPxp.itemDisplayLabel("measurement");
    var editPxpButton = new Button("Edit");
    editPxpButton.addClickListener(clicked -> {
      Set<PxpMeasurementLookup.MeasurementInfo> selectedMeasurements = filterGridPxp.selectedElements();
      List<String> selectedMeasurementIds = selectedMeasurements
          .stream()
          .map(PxpMeasurementLookup.MeasurementInfo::measurementId)
          .distinct()
          .toList();
      if (selectedMeasurementIds.isEmpty()) {
        displayMissingSelectionNote();
        return;
      }
      fireEvent(new PxpMeasurementEditRequested(selectedMeasurementIds, this, true));
    });

    var deletePxpButton = new Button("Delete");
    deletePxpButton.addClickListener(clicked -> {
      Set<PxpMeasurementLookup.MeasurementInfo> selectedMeasurements = filterGridPxp.selectedElements();
      List<String> selectedMeasurementIds = selectedMeasurements
          .stream()
          .map(PxpMeasurementLookup.MeasurementInfo::measurementId)
          .distinct()
          .toList();
      if (selectedMeasurementIds.isEmpty()) {
        displayMissingSelectionNote();
        return;
      }
      fireEvent(new NgsMeasurementDeletionRequested(selectedMeasurementIds, this, true));
    });
    filterGridPxp.setSecondaryActionGroup(deletePxpButton, editPxpButton);


    //add corresponding tabs in a defined order
    tabSheet.removeAllTabs();

    if (ngsMeasurementLookup.countNgsMeasurements(projectId,
        new MeasurementFilter(experimentId, "")) > 0) {
      var ngsTab = new FilterGridTab<>("Genomics", filterGridNgs);
      tabSheet.addTab(0, ngsTab);
      tabSheet.addPrimaryAction(ngsTab,
          tab -> fireEvent(new NgsMeasurementRegistrationRequested(this, true)));
      tabSheet.addFeatureAction(ngsTab,
          tab -> {
            List<String> selectedMeasurementIds = tab.filterGrid().selectedElements()
                .stream()
                .map(MeasurementInfo::measurementId)
                .distinct()
                .toList();
            if (selectedMeasurementIds.isEmpty()) {
              displayMissingSelectionNote();
              return;
            }
            fireEvent(new NgsMeasurementExportRequested(selectedMeasurementIds, this, true));
          });
    }

    if (pxpMeasurementLookup.countPxpMeasurements(projectId,
        new PxpMeasurementLookup.MeasurementFilter(experimentId, "")) > 0) {
      var pxpTab = new FilterGridTab<>("Proteomics", filterGridPxp);
      tabSheet.addTab(1, pxpTab);
      tabSheet.addPrimaryAction(pxpTab,
          tab -> fireEvent(new PxpMeasurementRegistrationRequested(this, true)));
      tabSheet.addFeatureAction(pxpTab,
          tab -> {
            List<String> selectedMeasurementIds = tab.filterGrid().selectedElements()
                .stream()
                .map(PxpMeasurementLookup.MeasurementInfo::measurementId)
                .distinct()
                .toList();
            if (selectedMeasurementIds.isEmpty()) {
              displayMissingSelectionNote();
              return;
            }
            fireEvent(new PxpMeasurementExportRequested(selectedMeasurementIds, this, true));
          });
    }
  }

  private static @NonNull NgsMeasurementFilter configureDatabaseFilterNgs(
      @NonNull String experimentId,
      @Nullable String searchTerm) {
    return new NgsMeasurementFilter(experimentId,
        Optional.ofNullable(searchTerm).orElse(""));
  }

  private static @NonNull PxpMeasurementFilter configureDatabaseFilterPxp(
      @NonNull String experimentId,
      @Nullable String searchTerm) {
    return new PxpMeasurementFilter(experimentId,
        Optional.ofNullable(searchTerm).orElse(""));
  }


  SearchTermFilter getNgsSearchTermFilter() {
    return this.ngsSearchTermFilter;
  }

  SearchTermFilter getPxpSearchTermFilter() {
    return pxpSearchTermFilter;
  }


  private Grid<MeasurementInfo> createNgsGrid() {
    var ngsGrid = new Grid<MeasurementInfo>();
    ngsGrid.addColumn(MeasurementInfo::measurementCode)
        .setHeader("QBiC Measurement ID")
        .setSortProperty(NgsSortKey.MEASUREMENT_ID.sortKey())
        .setComparator(Comparator.comparing(MeasurementInfo::measurementCode))
        .setAutoWidth(true)
        .setResizable(true)
        .setFrozen(true);
    ngsGrid.addColumn(MeasurementInfo::measurementName)
        .setHeader("Measurement Name")
        .setSortProperty(NgsSortKey.MEASUREMENT_NAME.sortKey())
        .setComparator(Comparator.comparing(MeasurementInfo::measurementCode))
        .setAutoWidth(true)
        .setResizable(true);
    ngsGrid.addComponentColumn(measurementInfo -> renderSamplesNgs(measurementInfo,
            info -> "%s (%s)".formatted(info.sampleLabel(), info.sampleCode())))
        .setHeader("Samples")
        .setSortable(false)
        .setAutoWidth(true)
        .setResizable(true);
    ngsGrid.addColumn(MeasurementInfo::facility)
        .setHeader("Facility")
        .setSortProperty(NgsSortKey.FACILITY.sortKey())
        .setComparator(Comparator.comparing(MeasurementInfo::facility))
        .setAutoWidth(true)
        .setResizable(true);
    ngsGrid.addComponentColumn(
            info -> renderInstrument(info.instrument().label(),
                info.instrument().oboId(),
                info.instrument().iri()))
        .setHeader("Instrument")
        .setSortable(false)
        .setAutoWidth(true)
        .setResizable(true);
    ngsGrid.addComponentColumn(
            info -> renderOrganisation(info.organisation().label(),
                info.organisation().iri()))
        .setHeader("Organisation")
        .setSortable(false)
        .setAutoWidth(true)
        .setResizable(true);

    ngsGrid.addColumn(MeasurementInfo::readType)
        .setHeader("Read type")
        .setSortProperty(NgsSortKey.READ_TYPE.sortKey())
        .setComparator(Comparator.comparing(MeasurementInfo::readType))
        .setAutoWidth(true)
        .setResizable(true);
    ngsGrid.addColumn(MeasurementInfo::libraryKit)
        .setHeader("Library kit")
        .setSortProperty(NgsSortKey.LIBRARY_KIT.sortKey())
        .setComparator(Comparator.comparing(MeasurementInfo::libraryKit))
        .setAutoWidth(true)
        .setResizable(true);
    ngsGrid.addColumn(MeasurementInfo::flowCell)
        .setHeader("Flow cell")
        .setSortProperty(NgsSortKey.FLOW_CELL.sortKey())
        .setComparator(Comparator.comparing(MeasurementInfo::flowCell))
        .setAutoWidth(true)
        .setResizable(true);
    ngsGrid.addColumn(MeasurementInfo::runProtocol)
        .setHeader("Run protocol")
        .setKey(NgsSortKey.RUN_PROTOCOL.sortKey())
        .setComparator(Comparator.comparing(MeasurementInfo::runProtocol))
        .setAutoWidth(true)
        .setResizable(true);
    ngsGrid.addColumn(info -> formatTime(info.registeredAt()))
        .setHeader("Registration Date")
        .setKey(NgsSortKey.REGISTRATION_DATE.sortKey())
        .setComparator(Comparator.comparing(MeasurementInfo::registeredAt))
        .setAutoWidth(true)
        .setResizable(true);
    //TODO add component column for comments
    ngsGrid.addComponentColumn(
            (MeasurementInfo measurementInfo) -> renderSamplesNgs(measurementInfo,
                info -> info.comment()))
        .setHeader("Comment")
        .setSortable(false)
        .setAutoWidth(true)
        .setResizable(true);

    return ngsGrid;
  }

  @Override
  protected void onAttach(AttachEvent attachEvent) {
    super.onAttach(attachEvent);
    attachEvent.getUI().getPage().retrieveExtendedClientDetails(
        receiver -> clientTimeZone.set(receiver.getTimeZoneId()));
  }


  private @NonNull String formatTime(Instant instant) {
    UI.getCurrent().getPage().retrieveExtendedClientDetails(receiver -> receiver.getTimeZoneId());
    return instant.atZone(ZoneId.of(clientTimeZone.get())).format(
        DateTimeFormatter.ofPattern(DATE_TIME_FORMAT));
  }

  private Grid<PxpMeasurementLookup.MeasurementInfo> createPxpGrid() {
    var pxpGrid = new Grid<PxpMeasurementLookup.MeasurementInfo>();
    pxpGrid.addColumn(PxpMeasurementLookup.MeasurementInfo::measurementCode)
        .setHeader("QBiC Measurement ID")
        .setSortProperty(NgsSortKey.MEASUREMENT_ID.sortKey())
        .setComparator(Comparator.comparing(PxpMeasurementLookup.MeasurementInfo::measurementCode))
        .setAutoWidth(true)
        .setResizable(true)
        .setFrozen(true);
    pxpGrid.addColumn(PxpMeasurementLookup.MeasurementInfo::measurementName)
        .setHeader("Measurement Name")
        .setSortProperty(NgsSortKey.MEASUREMENT_NAME.sortKey())
        .setComparator(Comparator.comparing(PxpMeasurementLookup.MeasurementInfo::measurementCode))
        .setAutoWidth(true)
        .setResizable(true);
    pxpGrid.addComponentColumn(measurementInfo -> renderSamplesPxp(measurementInfo,
            info -> "%s (%s)".formatted(info.sampleLabel(), info.sampleCode())))
        .setHeader("Samples")
        .setSortable(false)
        .setAutoWidth(true)
        .setResizable(true);
    pxpGrid.addColumn(PxpMeasurementLookup.MeasurementInfo::facility)
        .setHeader("Facility")
        .setSortProperty(NgsSortKey.FACILITY.sortKey())
        .setComparator(Comparator.comparing(PxpMeasurementLookup.MeasurementInfo::facility))
        .setAutoWidth(true)
        .setResizable(true);
    pxpGrid.addComponentColumn(
            info -> renderMsDevice(info.msDevice().label(),
                info.msDevice().oboId(),
                info.msDevice().iri()))
        .setHeader("MS Device")
        .setSortable(false)
        .setAutoWidth(true)
        .setResizable(true);
    pxpGrid.addComponentColumn(
            info -> renderOrganisation(info.organisation().label(),
                info.organisation().iri()))
        .setHeader("Organisation")
        .setSortable(false)
        .setAutoWidth(true)
        .setResizable(true);

    pxpGrid.addColumn(info -> formatTime(info.registeredAt()))
        .setHeader("Registration Date")
        .setKey(NgsSortKey.REGISTRATION_DATE.sortKey())
        .setComparator(Comparator.comparing(PxpMeasurementLookup.MeasurementInfo::registeredAt))
        .setAutoWidth(true)
        .setResizable(true);
    pxpGrid.addComponentColumn(measurementInfo -> renderSamplesPxp(measurementInfo,
            PxpMeasurementLookup.SampleInfo::comment))
        .setHeader("Comment")
        .setSortable(false)
        .setAutoWidth(true)
        .setResizable(true);

    return pxpGrid;
  }

  private static Span renderInstrument(String label, String oboId, String iri) {
    Span instrumentLabel = new Span(label);
    Span instrumentOntologyLink = new Span(oboId);
    instrumentOntologyLink.addClassName("ontology-link");
    Anchor instrumentNameAnchor = new Anchor(iri, instrumentOntologyLink);
    instrumentNameAnchor.setTarget(AnchorTarget.BLANK);
    Span organisationSpan = new Span(instrumentLabel, instrumentNameAnchor);
    organisationSpan.addClassName("instrument-column");
    return organisationSpan;
  }

  private static Span renderMsDevice(String label, String oboId, String iri) {
    return renderInstrument(label, oboId, iri);
  }

  private static Anchor renderOrganisation(String label, String iri) {
    SvgIcon svgIcon = new SvgIcon(ROR_ICON_RESOURCE);
    svgIcon.addClassName("organisation-icon");
    Span organisationLabel = new Span(label);
    Anchor organisationAnchor = new Anchor(iri, organisationLabel, svgIcon);
    organisationAnchor.setTarget(AnchorTarget.BLANK);
    organisationAnchor.addClassName("organisation-entry");
    return organisationAnchor;
  }

  private static Component renderSamplesNgs(MeasurementInfo measurementInfo,
      Function<SampleInfo, String> singleSampleConverter) {
    var sampleInfos = measurementInfo.sampleInfos();
    if (sampleInfos.size() == 1) {
      SampleInfo sampleInfo = sampleInfos.stream().findFirst().orElseThrow();
      String singleSampleText = singleSampleConverter.apply(sampleInfo);
      return new Span(singleSampleText);
    }
    var displayLabel = measurementInfo.samplePool();
    var expandIcon = VaadinIcon.EXPAND_SQUARE.create();
    expandIcon.addClassName("expand-icon");
    var pooledSamplesSpan = new Span(new Span(displayLabel), expandIcon);
    pooledSamplesSpan.addClassNames("sample-column-cell", "clickable");
    pooledSamplesSpan.addClickListener(event -> openPooledSampleDialogNgs(measurementInfo));
    return pooledSamplesSpan;
  }

  private static Component renderSamplesPxp(PxpMeasurementLookup.MeasurementInfo measurementInfo,
      Function<PxpMeasurementLookup.SampleInfo, String> singleSampleConverter) {
    var sampleInfos = measurementInfo.sampleInfos();
    if (sampleInfos.size() == 1) {
      var sampleInfo = sampleInfos.stream().findFirst().orElseThrow();
      String singleSampleText = singleSampleConverter.apply(sampleInfo);
      return new Span(singleSampleText);
    }
    var displayLabel = measurementInfo.samplePool();
    var expandIcon = VaadinIcon.EXPAND_SQUARE.create();
    expandIcon.addClassName("expand-icon");
    var pooledSamplesSpan = new Span(new Span(displayLabel), expandIcon);
    pooledSamplesSpan.addClassNames("sample-column-cell", "clickable");
    pooledSamplesSpan.addClickListener(event -> openPooledSampleDialogPxp(measurementInfo));
    return pooledSamplesSpan;
  }

  private static void openPooledSampleDialogNgs(MeasurementInfo measurementInfo) {
    AppDialog dialog = AppDialog.medium();
    DialogHeader.with(dialog, "View Pooled Measurement");
    DialogFooter.withConfirmOnly(dialog, "Close");
    var sampleInfoGrid = new Grid<SampleInfo>();
    sampleInfoGrid.addColumn(SampleInfo::sampleLabel)
        .setHeader("Sample Name")
        .setAutoWidth(true);
    sampleInfoGrid.addColumn(SampleInfo::sampleCode)
        .setHeader("Sample Id")
        .setAutoWidth(true);
    sampleInfoGrid.addColumn(SampleInfo::indexI7)
        .setHeader("Index i7")
        .setAutoWidth(true);
    sampleInfoGrid.addColumn(SampleInfo::indexI5)
        .setHeader("Index i5")
        .setAutoWidth(true);
    sampleInfoGrid.addColumn(SampleInfo::comment)
        .setHeader("Comment")
        .setAutoWidth(true);
    sampleInfoGrid.setItems(measurementInfo.sampleInfos());
    DialogSection measuredSamplesSection = DialogSection.with(
        "Measurement ID: " + measurementInfo.measurementCode(),
        "Sample Pool Group: " + measurementInfo.samplePool(),
        sampleInfoGrid);

    DialogBody.withoutUserInput(dialog, measuredSamplesSection);
    dialog.registerConfirmAction(dialog::close);
    dialog.open();
  }

  private static void openPooledSampleDialogPxp(
      PxpMeasurementLookup.MeasurementInfo measurementInfo) {
    AppDialog dialog = AppDialog.medium();
    DialogHeader.with(dialog, "View Pooled Measurement");
    DialogFooter.withConfirmOnly(dialog, "Close");
    var sampleInfoGrid = new Grid<PxpMeasurementLookup.SampleInfo>();
    sampleInfoGrid.addColumn(PxpMeasurementLookup.SampleInfo::sampleLabel)
        .setHeader("Sample Name")
        .setAutoWidth(true);
    sampleInfoGrid.addColumn(PxpMeasurementLookup.SampleInfo::sampleCode)
        .setHeader("Sample Id")
        .setAutoWidth(true);
    sampleInfoGrid.addColumn(PxpMeasurementLookup.SampleInfo::comment)
        .setHeader("Comment")
        .setAutoWidth(true);
    sampleInfoGrid.setItems(measurementInfo.sampleInfos());
    DialogSection measuredSamplesSection = DialogSection.with(
        "Measurement ID: " + measurementInfo.measurementCode(),
        "Sample Pool Group: " + measurementInfo.samplePool(),
        sampleInfoGrid);

    DialogBody.withoutUserInput(dialog, measuredSamplesSection);
    dialog.registerConfirmAction(dialog::close);
    dialog.open();
  }


  private void validateContext(Context context) throws ContextValidationException {
    if (isNull(context)) {
      throw new ContextValidationException("Context cannot be null");
    }
    context.projectId().orElseThrow(
        () -> new ContextValidationException("Context must contain the project id"));
    context.experimentId().orElseThrow(
        () -> new ContextValidationException("Context must contain the experiment id"));
  }

  private static final class ContextValidationException extends ApplicationException {

    public ContextValidationException(String message) {
      super(message);
    }
  }

  private void displayMissingSelectionNote() {
    messageFactory.toast("measurement.no-measurements-selected", new Object[]{}, getLocale())
        .open();
  }


  public static class NgsMeasurementRegistrationRequested extends
      ComponentEvent<MeasurementDetailsComponentV2> {

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public NgsMeasurementRegistrationRequested(MeasurementDetailsComponentV2 source,
        boolean fromClient) {
      super(source, fromClient);
    }
  }

  public Registration addNgsRegisterListener(
      ComponentEventListener<NgsMeasurementRegistrationRequested> listener) {
    return addListener(NgsMeasurementRegistrationRequested.class, listener);
  }

  public static class NgsMeasurementEditRequested extends
      ComponentEvent<MeasurementDetailsComponentV2> {

    private final List<String> measurementIds;

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public NgsMeasurementEditRequested(List<String> measurementIds,
        MeasurementDetailsComponentV2 source, boolean fromClient) {
      super(source, fromClient);
      this.measurementIds = measurementIds.stream().toList();
    }

    public List<String> measurementIds() {
      return measurementIds;
    }
  }

  public Registration addNgsEditListener(
      ComponentEventListener<NgsMeasurementEditRequested> listener) {
    return addListener(NgsMeasurementEditRequested.class, listener);
  }

  public static class NgsMeasurementExportRequested extends
      ComponentEvent<MeasurementDetailsComponentV2> {

    private final List<String> measurementIds;

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public NgsMeasurementExportRequested(List<String> measurementIds,
        MeasurementDetailsComponentV2 source, boolean fromClient) {
      super(source, fromClient);
      this.measurementIds = measurementIds.stream().toList();
    }

    public List<String> measurementIds() {
      return measurementIds;
    }
  }

  public Registration addNgsExportListener(
      ComponentEventListener<NgsMeasurementExportRequested> listener) {
    return addListener(NgsMeasurementExportRequested.class, listener);
  }

  public static class NgsMeasurementDeletionRequested extends
      ComponentEvent<MeasurementDetailsComponentV2> {

    private final List<String> measurementIds;

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public NgsMeasurementDeletionRequested(List<String> measurementIds,
        MeasurementDetailsComponentV2 source,
        boolean fromClient) {
      super(source, fromClient);
      this.measurementIds = measurementIds.stream().toList();
    }

    public List<String> measurementIds() {
      return measurementIds;
    }
  }

  public Registration addNgsDeletionListener(
      ComponentEventListener<NgsMeasurementDeletionRequested> listener) {
    return addListener(NgsMeasurementDeletionRequested.class, listener);
  }


  public static class PxpMeasurementRegistrationRequested extends
      ComponentEvent<MeasurementDetailsComponentV2> {

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public PxpMeasurementRegistrationRequested(MeasurementDetailsComponentV2 source,
        boolean fromClient) {
      super(source, fromClient);
    }
  }

  public Registration addPxpRegisterListener(
      ComponentEventListener<PxpMeasurementRegistrationRequested> listener) {
    return addListener(PxpMeasurementRegistrationRequested.class, listener);
  }

  public static class PxpMeasurementEditRequested extends
      ComponentEvent<MeasurementDetailsComponentV2> {

    private final List<String> measurementIds;

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public PxpMeasurementEditRequested(List<String> measurementIds,
        MeasurementDetailsComponentV2 source, boolean fromClient) {
      super(source, fromClient);
      this.measurementIds = measurementIds.stream().toList();
    }

    public List<String> measurementIds() {
      return measurementIds;
    }
  }

  public Registration addPxpEditListener(
      ComponentEventListener<PxpMeasurementEditRequested> listener) {
    return addListener(PxpMeasurementEditRequested.class, listener);
  }

  public static class PxpMeasurementExportRequested extends
      ComponentEvent<MeasurementDetailsComponentV2> {

    private final List<String> measurementIds;

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public PxpMeasurementExportRequested(List<String> measurementIds,
        MeasurementDetailsComponentV2 source, boolean fromClient) {
      super(source, fromClient);
      this.measurementIds = measurementIds.stream().toList();
    }

    public List<String> measurementIds() {
      return measurementIds;
    }
  }

  public Registration addPxpExportListener(
      ComponentEventListener<PxpMeasurementExportRequested> listener) {
    return addListener(PxpMeasurementExportRequested.class, listener);
  }

  public static class PxpMeasurementDeletionRequested extends
      ComponentEvent<MeasurementDetailsComponentV2> {

    private final List<String> measurementIds;

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public PxpMeasurementDeletionRequested(List<String> measurementIds,
        MeasurementDetailsComponentV2 source,
        boolean fromClient) {
      super(source, fromClient);
      this.measurementIds = measurementIds.stream().toList();
    }

    public List<String> measurementIds() {
      return measurementIds;
    }
  }

  public Registration addPxpDeletionListener(
      ComponentEventListener<NgsMeasurementDeletionRequested> listener) {
    return addListener(NgsMeasurementDeletionRequested.class, listener);
  }


}
