package life.qbic.datamanager.views.projects.project.measurements;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.MultiSortPriority;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.data.provider.CallbackDataProvider.CountCallback;
import com.vaadin.flow.data.provider.CallbackDataProvider.FetchCallback;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.spring.data.VaadinSpringDataHelpers;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.time.DateTimeFormat;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.datamanager.views.general.dialog.AppDialog;
import life.qbic.datamanager.views.general.dialog.DialogBody;
import life.qbic.datamanager.views.general.dialog.DialogFooter;
import life.qbic.datamanager.views.general.dialog.DialogHeader;
import life.qbic.datamanager.views.general.dialog.DialogSection;
import life.qbic.datamanager.views.general.grid.component.FilterGrid;
import life.qbic.datamanager.views.general.grid.component.FilterGridConfigurations;
import life.qbic.datamanager.views.general.grid.component.FilterGridTab;
import life.qbic.datamanager.views.general.grid.component.FilterGridTabSheet;
import life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory;
import life.qbic.projectmanagement.application.measurement.IpMeasurementLookup;
import life.qbic.projectmanagement.application.measurement.NgsMeasurementLookup;
import life.qbic.projectmanagement.application.measurement.NgsMeasurementLookup.MeasurementFilter;
import life.qbic.projectmanagement.application.measurement.NgsMeasurementLookup.NgsSortKey;
import life.qbic.projectmanagement.application.measurement.PxpMeasurementLookup;
import life.qbic.projectmanagement.application.measurement.PxpMeasurementLookup.MeasurementInfo;
import life.qbic.projectmanagement.application.measurement.PxpMeasurementLookup.PxpSortKey;
import org.springframework.lang.NonNull;

/**
 * A component to show detailed information about existing measurements within an experiment.
 */
public class MeasurementDetailsComponent extends PageArea implements Serializable {

  private static final StreamResource ROR_ICON_RESOURCE = new StreamResource("ROR_logo.svg",
      () -> MeasurementDetailsComponent.class.getClassLoader()
          .getResourceAsStream("icons/ROR_logo.svg"));
  private static final NumberFormat INJECTION_VOLUME_FORMAT = new DecimalFormat("#.##");
  private static final DateTimeFormat MEASUREMENT_REGISTRATION_DATE_TIME_FORMAT = DateTimeFormat.ISO_LOCAL_DATE_TIME_WHITESPACE_SEPARATED;

  private final AtomicReference<String> clientTimeZone = new AtomicReference<>("UTC");
  private final AtomicInteger clientTimeZoneOffset = new AtomicInteger(0);
  private final MessageSourceNotificationFactory messageFactory;

  private final FilterGridTabSheet tabSheet;
  private final SearchTermFilter ngsSearchTermFilter = SearchTermFilter.empty();
  private final SearchTermFilter pxpSearchTermFilter = SearchTermFilter.empty();
  private final SearchTermFilter ipSearchTermFilter = SearchTermFilter.empty();

  private final transient NgsMeasurementLookup ngsMeasurementLookup;
  private final transient PxpMeasurementLookup pxpMeasurementLookup;
  private final transient IpMeasurementLookup ipMeasurementLookup;
  private FilterGrid<NgsMeasurementLookup.MeasurementInfo, SearchTermFilter> filterGridNgs;
  private FilterGrid<MeasurementInfo, SearchTermFilter> filterGridPxp;
  private FilterGrid<IpMeasurementLookup.MeasurementInfo, SearchTermFilter> filterGridIp;

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

  @Override
  protected void onAttach(AttachEvent attachEvent) {
    super.onAttach(attachEvent);
    attachEvent.getUI().getPage().retrieveExtendedClientDetails(
        receiver -> {
          clientTimeZoneOffset.set(receiver.getTimezoneOffset());
          clientTimeZone.set(receiver.getTimeZoneId());
        });
  }

  private @NonNull String formatTime(Instant instant, DateTimeFormat dateTimeFormat) {
    return DateTimeFormat.asJavaFormatter(dateTimeFormat, ZoneId.of(clientTimeZone.get()))
        .format(instant);
  }

  public MeasurementDetailsComponent(
      MessageSourceNotificationFactory messageFactory,
      NgsMeasurementLookup ngsMeasurementLookup,
      PxpMeasurementLookup pxpMeasurementLookup,
      IpMeasurementLookup ipMeasurementLookup) {
    this.messageFactory = requireNonNull(messageFactory);
    this.ngsMeasurementLookup = requireNonNull(ngsMeasurementLookup);
    this.pxpMeasurementLookup = requireNonNull(pxpMeasurementLookup);
    this.ipMeasurementLookup = requireNonNull(ipMeasurementLookup);
    addClassNames("measurement-details-component", "height-full", "width-full");

    //setup tab sheet
    tabSheet = new FilterGridTabSheet();
    tabSheet.showPrimaryFeatureButton();
    tabSheet.setCaptionPrimaryAction("Register Measurements");
    tabSheet.showPrimaryFeatureButton();
    tabSheet.setCaptionFeatureAction("Export");
    add(tabSheet);
  }

  /**
   * Refreshes the genomics grid.
   */
  public void refreshNgs() {
    Optional.ofNullable(filterGridNgs)
        .ifPresent(FilterGrid::refreshAll);
  }

  /**
   * Refreshes the proteomics grid.
   */
  public void refreshPxp() {
    Optional.ofNullable(filterGridPxp)
        .ifPresent(FilterGrid::refreshAll);
  }

  /**
   * Refreshes the immunopeptidomics grid.
   */
  public void refreshIp() {
    Optional.ofNullable(filterGridIp)
        .ifPresent(FilterGrid::refreshAll);
  }

  /**
   * Sets the context of the component and refreshes the view to display updated information.
   *
   * @param context the context for this component
   */
  public void setContext(Context context) {
    validateContext(context);
    String projectId = context.projectId().orElseThrow().value();
    String experimentId = context.experimentId().orElseThrow().value();

    //add corresponding tabs in a defined order
    tabSheet.removeAllTabs();
    if (ngsMeasurementsExist(projectId, experimentId)) {
      filterGridNgs = filterGridNgs(createNgsGrid(), projectId, experimentId);
      addNgsTab(tabSheet, 0, "Genomics", filterGridNgs);
    }
    if (pxpMeasurementsExist(projectId, experimentId)) {
      filterGridPxp = filterGridPxp(createPxpGrid(), projectId, experimentId);
      addPxpTab(tabSheet, 1, "Proteomics", filterGridPxp);
    }
    if (ipMeasurementsExist(projectId, experimentId)) {
      filterGridIp = filterGridIp(createIpGrid(), projectId, experimentId);
      addIpTab(tabSheet, 2, "Immunopeptidomics", filterGridIp);
    }
  }

  private boolean pxpMeasurementsExist(String projectId, String experimentId) {
    return pxpMeasurementLookup.countPxpMeasurements(projectId,
        PxpMeasurementLookup.MeasurementFilter.forExperiment(experimentId)) > 0;
  }

  private boolean ngsMeasurementsExist(String projectId, String experimentId) {
    return ngsMeasurementLookup.countNgsMeasurements(projectId,
        MeasurementFilter.forExperiment(experimentId)) > 0;
  }

  private boolean ipMeasurementsExist(String projectId, String experimentId) {
    return ipMeasurementLookup.countIpMeasurements(projectId,
        IpMeasurementLookup.MeasurementFilter.forExperiment(experimentId)) > 0;
  }

  private void addPxpTab(FilterGridTabSheet tabSheet, int index, String name,
      FilterGrid<MeasurementInfo, SearchTermFilter> filterGrid) {
    var pxpTab = new FilterGridTab<>(name, filterGrid);
    tabSheet.addTab(index, pxpTab);
    tabSheet.addPrimaryAction(pxpTab,
        tab -> fireEvent(new PxpMeasurementRegistrationRequested(this, true)));
    tabSheet.addFeatureAction(pxpTab,
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
          fireEvent(new PxpMeasurementExportRequested(selectedMeasurementIds, this, true));
        });
  }

  private void addNgsTab(FilterGridTabSheet tabSheet, int index, String name,
      FilterGrid<NgsMeasurementLookup.MeasurementInfo, SearchTermFilter> filterGrid) {
    var ngsTab = new FilterGridTab<>(name, filterGrid);
    tabSheet.addTab(index, ngsTab);
    tabSheet.addPrimaryAction(ngsTab,
        tab -> fireEvent(new NgsMeasurementRegistrationRequested(this, true)));
    tabSheet.addFeatureAction(ngsTab,
        tab -> {
          List<String> selectedMeasurementIds = tab.filterGrid().selectedElements()
              .stream()
              .map(NgsMeasurementLookup.MeasurementInfo::measurementId)
              .distinct()
              .toList();
          if (selectedMeasurementIds.isEmpty()) {
            displayMissingSelectionNote();
            return;
          }
          fireEvent(new NgsMeasurementExportRequested(selectedMeasurementIds, this, true));
        });
  }

  private FilterGrid<NgsMeasurementLookup.MeasurementInfo, SearchTermFilter> filterGridNgs(
      Grid<NgsMeasurementLookup.MeasurementInfo> ngsGrid, String projectId,
      String experimentId) {
    FetchCallback<NgsMeasurementLookup.MeasurementInfo, SearchTermFilter> fetchCallback = query ->
    {
      String searchTerm = query.getFilter().map(SearchTermFilter::searchTerm).orElse("");
      return ngsMeasurementLookup.lookupNgsMeasurements(
          projectId, query.getOffset(), query.getLimit(),
          VaadinSpringDataHelpers.toSpringDataSort(query),
          MeasurementFilter.forExperiment(experimentId)
              .withSearch(searchTerm, clientTimeZoneOffset.get(),
                  MEASUREMENT_REGISTRATION_DATE_TIME_FORMAT));
    };
    CountCallback<NgsMeasurementLookup.MeasurementInfo, SearchTermFilter> countCallback = query ->
    {
      String searchTerm = query.getFilter().map(SearchTermFilter::searchTerm).orElse("");
      return ngsMeasurementLookup.countNgsMeasurements(projectId,
          MeasurementFilter.forExperiment(experimentId)
              .withSearch(searchTerm, clientTimeZoneOffset.get(),
                  MEASUREMENT_REGISTRATION_DATE_TIME_FORMAT));
    };
    var ngsGridConfiguration = FilterGridConfigurations.lazy(
        fetchCallback,
        countCallback);
    var filterGrid = FilterGrid.create(
        NgsMeasurementLookup.MeasurementInfo.class,
        SearchTermFilter.class,
        ngsGridConfiguration.applyConfiguration(ngsGrid),
        this::getNgsSearchTermFilter,
        (searchTerm, filter) -> filter.replaceWith(searchTerm));

    filterGrid.itemDisplayLabel("measurement");
    filterGrid.searchFieldPlaceholder("Search Measurements");
    var editNgsButton = new Button("Edit");
    editNgsButton.addClickListener(clicked -> {
      Set<NgsMeasurementLookup.MeasurementInfo> selectedMeasurements = filterGrid.selectedElements();
      List<String> selectedMeasurementIds = selectedMeasurements
          .stream()
          .map(NgsMeasurementLookup.MeasurementInfo::measurementId)
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
      Set<NgsMeasurementLookup.MeasurementInfo> selectedMeasurements = filterGrid.selectedElements();
      List<String> selectedMeasurementIds = selectedMeasurements
          .stream()
          .map(NgsMeasurementLookup.MeasurementInfo::measurementId)
          .distinct()
          .toList();
      if (selectedMeasurementIds.isEmpty()) {
        displayMissingSelectionNote();
        return;
      }
      fireEvent(new NgsMeasurementDeletionRequested(selectedMeasurementIds, this, true));
    });
    filterGrid.setSecondaryActionGroup(deleteNgsButton, editNgsButton);
    return filterGrid;
  }

  private FilterGrid<MeasurementInfo, SearchTermFilter> filterGridPxp(
      Grid<MeasurementInfo> pxpGrid, String projectId, String experimentId) {
    FetchCallback<MeasurementInfo, SearchTermFilter> fetchCallback = query -> {
      String searchTerm = query.getFilter().map(SearchTermFilter::searchTerm)
          .orElse("");
      return pxpMeasurementLookup.lookupPxpMeasurements(
          projectId, query.getOffset(), query.getLimit(),
          VaadinSpringDataHelpers.toSpringDataSort(query),
          PxpMeasurementLookup.MeasurementFilter.forExperiment(experimentId)
              .withSearch(searchTerm, clientTimeZoneOffset.get(),
                  MEASUREMENT_REGISTRATION_DATE_TIME_FORMAT));
    };

    CountCallback<MeasurementInfo, SearchTermFilter> countCallback = query -> {
      var searchTerm = query.getFilter().map(SearchTermFilter::searchTerm)
          .orElse("");
      return pxpMeasurementLookup.countPxpMeasurements(projectId,
          PxpMeasurementLookup.MeasurementFilter.forExperiment(experimentId)
              .withSearch(searchTerm, clientTimeZoneOffset.get(),
                  MEASUREMENT_REGISTRATION_DATE_TIME_FORMAT));
    };

    var configuration = FilterGridConfigurations.lazy(fetchCallback, countCallback);

    var filterGrid = FilterGrid.create(
        MeasurementInfo.class,
        SearchTermFilter.class,
        configuration.applyConfiguration(pxpGrid),
        this::getPxpSearchTermFilter,
        (searchTerm, filter) -> filter.replaceWith(searchTerm));

    filterGrid.itemDisplayLabel("measurement");
    filterGrid.searchFieldPlaceholder("Search Measurements");
    var editPxpButton = new Button("Edit");
    editPxpButton.addClickListener(clicked -> {
      Set<MeasurementInfo> selectedMeasurements = filterGrid.selectedElements();
      List<String> selectedMeasurementIds = selectedMeasurements
          .stream()
          .map(MeasurementInfo::measurementId)
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
      Set<MeasurementInfo> selectedMeasurements = filterGrid.selectedElements();
      List<String> selectedMeasurementIds = selectedMeasurements
          .stream()
          .map(MeasurementInfo::measurementId)
          .distinct()
          .toList();
      if (selectedMeasurementIds.isEmpty()) {
        displayMissingSelectionNote();
        return;
      }
      fireEvent(new PxpMeasurementDeletionRequested(selectedMeasurementIds, this, true));
    });
    filterGrid.setSecondaryActionGroup(deletePxpButton, editPxpButton);
    return filterGrid;
  }


  SearchTermFilter getNgsSearchTermFilter() {
    return this.ngsSearchTermFilter;
  }

  SearchTermFilter getPxpSearchTermFilter() {
    return pxpSearchTermFilter;
  }

  SearchTermFilter getIpSearchTermFilter() {
    return ipSearchTermFilter;
  }


  private Grid<NgsMeasurementLookup.MeasurementInfo> createNgsGrid() {
    var ngsGrid = new Grid<NgsMeasurementLookup.MeasurementInfo>();
    ngsGrid.setMultiSort(true, MultiSortPriority.APPEND, true);
    ngsGrid.addColumn(NgsMeasurementLookup.MeasurementInfo::measurementCode)
        .setHeader("QBiC Measurement ID")
        .setSortProperty(NgsSortKey.MEASUREMENT_ID.sortKey())
        .setComparator(Comparator.comparing(NgsMeasurementLookup.MeasurementInfo::measurementCode))
        .setAutoWidth(true)
        .setResizable(true)
        .setFrozen(true);
    ngsGrid.addColumn(NgsMeasurementLookup.MeasurementInfo::measurementName)
        .setHeader("Measurement Name")
        .setSortProperty(NgsSortKey.MEASUREMENT_NAME.sortKey())
        .setComparator(Comparator.comparing(NgsMeasurementLookup.MeasurementInfo::measurementCode))
        .setAutoWidth(true)
        .setResizable(true);
    ngsGrid.addComponentColumn(measurementInfo -> renderSamplesNgs(measurementInfo,
            info -> "%s (%s)".formatted(info.sampleLabel(), info.sampleCode())))
        .setHeader("Samples")
        .setSortable(false)
        .setAutoWidth(true)
        .setResizable(true);
    ngsGrid.addColumn(NgsMeasurementLookup.MeasurementInfo::facility)
        .setHeader("Facility")
        .setSortProperty(NgsSortKey.FACILITY.sortKey())
        .setComparator(Comparator.comparing(NgsMeasurementLookup.MeasurementInfo::facility))
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

    ngsGrid.addColumn(NgsMeasurementLookup.MeasurementInfo::readType)
        .setHeader("Read type")
        .setSortProperty(NgsSortKey.READ_TYPE.sortKey())
        .setComparator(Comparator.comparing(NgsMeasurementLookup.MeasurementInfo::readType))
        .setAutoWidth(true)
        .setResizable(true);
    ngsGrid.addColumn(NgsMeasurementLookup.MeasurementInfo::libraryKit)
        .setHeader("Library kit")
        .setSortProperty(NgsSortKey.LIBRARY_KIT.sortKey())
        .setComparator(Comparator.comparing(NgsMeasurementLookup.MeasurementInfo::libraryKit))
        .setAutoWidth(true)
        .setResizable(true);
    ngsGrid.addColumn(NgsMeasurementLookup.MeasurementInfo::flowCell)
        .setHeader("Flow cell")
        .setSortProperty(NgsSortKey.FLOW_CELL.sortKey())
        .setComparator(Comparator.comparing(NgsMeasurementLookup.MeasurementInfo::flowCell))
        .setAutoWidth(true)
        .setResizable(true);
    ngsGrid.addColumn(NgsMeasurementLookup.MeasurementInfo::runProtocol)
        .setHeader("Run protocol")
        .setKey(NgsSortKey.RUN_PROTOCOL.sortKey())
        .setComparator(Comparator.comparing(NgsMeasurementLookup.MeasurementInfo::runProtocol))
        .setAutoWidth(true)
        .setResizable(true);
    ngsGrid.addColumn(info -> formatTime(info.registeredAt(),
            MEASUREMENT_REGISTRATION_DATE_TIME_FORMAT))
        .setHeader("Registration Date")
        .setKey(NgsSortKey.REGISTRATION_DATE.sortKey())
        .setComparator(Comparator.comparing(NgsMeasurementLookup.MeasurementInfo::registeredAt))
        .setAutoWidth(true)
        .setResizable(true);
    ngsGrid.addComponentColumn(
            (NgsMeasurementLookup.MeasurementInfo measurementInfo) -> renderSamplesNgs(measurementInfo,
                info -> info.comment()))
        .setHeader("Comment")
        .setSortable(false)
        .setAutoWidth(true)
        .setResizable(true);

    return ngsGrid;
  }

  private Grid<PxpMeasurementLookup.MeasurementInfo> createPxpGrid() {
    var pxpGrid = new Grid<PxpMeasurementLookup.MeasurementInfo>();
    pxpGrid.setMultiSort(true, MultiSortPriority.APPEND, true);
    pxpGrid.addColumn(PxpMeasurementLookup.MeasurementInfo::measurementCode)
        .setHeader("QBiC Measurement ID")
        .setSortProperty(PxpSortKey.MEASUREMENT_ID.sortKey())
        .setComparator(Comparator.comparing(PxpMeasurementLookup.MeasurementInfo::measurementCode))
        .setAutoWidth(true)
        .setResizable(true)
        .setFrozen(true);
    pxpGrid.addColumn(PxpMeasurementLookup.MeasurementInfo::measurementName)
        .setHeader("Measurement Name")
        .setSortProperty(PxpSortKey.MEASUREMENT_NAME.sortKey())
        .setComparator(Comparator.comparing(PxpMeasurementLookup.MeasurementInfo::measurementCode))
        .setAutoWidth(true)
        .setResizable(true);
    pxpGrid.addComponentColumn(measurementInfo -> renderSamplesPxp(measurementInfo,
            info -> "%s (%s)".formatted(info.sampleLabel(), info.sampleCode())))
        .setHeader("Samples")
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
    pxpGrid.addColumn(PxpMeasurementLookup.MeasurementInfo::facility)
        .setHeader("Facility")
        .setSortProperty(PxpSortKey.FACILITY.sortKey())
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
    pxpGrid.addColumn(info -> info.technicalReplicateName())
        .setHeader("Technical Replicate")
        .setKey(PxpSortKey.TECHNICAL_REPLICATE.sortKey())
        .setComparator(
            Comparator.comparing(PxpMeasurementLookup.MeasurementInfo::technicalReplicateName))
        .setAutoWidth(true)
        .setResizable(true);
    pxpGrid.addColumn(PxpMeasurementLookup.MeasurementInfo::digestionEnzyme)
        .setHeader("Digestion Enzyme")
        .setKey(PxpSortKey.DIGESTION_ENZYME.sortKey())
        .setComparator(Comparator.comparing(PxpMeasurementLookup.MeasurementInfo::digestionEnzyme))
        .setAutoWidth(true)
        .setResizable(true);
    pxpGrid.addColumn(info -> info.digestionMethod())
        .setHeader("Digestion Method")
        .setKey(PxpSortKey.DIGESTION_METHOD.sortKey())
        .setComparator(Comparator.comparing(PxpMeasurementLookup.MeasurementInfo::digestionMethod))
        .setAutoWidth(true)
        .setResizable(true);
    pxpGrid.addColumn(info -> INJECTION_VOLUME_FORMAT.format(info.injectionVolume()))
        .setHeader("Injection Volume")
        .setKey(PxpSortKey.INJECTION_VOLUME.sortKey())
        .setComparator(Comparator.comparing(PxpMeasurementLookup.MeasurementInfo::injectionVolume))
        .setAutoWidth(true)
        .setResizable(true);
    pxpGrid.addColumn(info -> info.lcmsMethod())
        .setHeader("LCMS")
        .setKey(PxpSortKey.LCMS_METHOD.sortKey())
        .setComparator(Comparator.comparing(PxpMeasurementLookup.MeasurementInfo::lcmsMethod))
        .setAutoWidth(true)
        .setResizable(true);
    pxpGrid.addColumn(info -> info.lcColumn())
        .setHeader("LC column")
        .setKey(PxpSortKey.LC_COLUMN.sortKey())
        .setComparator(Comparator.comparing(PxpMeasurementLookup.MeasurementInfo::lcColumn))
        .setAutoWidth(true)
        .setResizable(true);
    pxpGrid.addColumn(info -> info.enrichmentMethod())
        .setHeader("Enrichment")
        .setKey(PxpSortKey.ENRICHMENT_METHOD.sortKey())
        .setComparator(Comparator.comparing(PxpMeasurementLookup.MeasurementInfo::enrichmentMethod))
        .setAutoWidth(true)
        .setResizable(true);

    pxpGrid.addColumn(info -> formatTime(info.registeredAt(),
            MEASUREMENT_REGISTRATION_DATE_TIME_FORMAT))
        .setHeader("Registration Date")
        .setKey(PxpSortKey.REGISTRATION_DATE.sortKey())
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

  private void addIpTab(FilterGridTabSheet tabSheet, int index, String name,
      FilterGrid<IpMeasurementLookup.MeasurementInfo, SearchTermFilter> filterGrid) {
    var ipTab = new FilterGridTab<>(name, filterGrid);
    tabSheet.addTab(index, ipTab);
    tabSheet.addPrimaryAction(ipTab,
        tab -> fireEvent(new IpMeasurementRegistrationRequested(this, true)));
    tabSheet.addFeatureAction(ipTab,
        tab -> {
          List<String> selectedMeasurementIds = tab.filterGrid().selectedElements()
              .stream()
              .map(IpMeasurementLookup.MeasurementInfo::measurementId)
              .distinct()
              .toList();
          if (selectedMeasurementIds.isEmpty()) {
            displayMissingSelectionNote();
            return;
          }
          fireEvent(new IpMeasurementExportRequested(selectedMeasurementIds, this, true));
        });
    var deleteIpButton = new Button("Delete");
    deleteIpButton.addClickListener(clicked -> {
      Set<IpMeasurementLookup.MeasurementInfo> selectedMeasurements = filterGrid.selectedElements();
      List<String> selectedMeasurementIds = selectedMeasurements
          .stream()
          .map(IpMeasurementLookup.MeasurementInfo::measurementId)
          .distinct()
          .toList();
      if (selectedMeasurementIds.isEmpty()) {
        displayMissingSelectionNote();
        return;
      }
      fireEvent(new IpMeasurementDeletionRequested(selectedMeasurementIds, this, true));
    });
    filterGrid.setSecondaryActionGroup(deleteIpButton);
  }

  private FilterGrid<IpMeasurementLookup.MeasurementInfo, SearchTermFilter> filterGridIp(
      Grid<IpMeasurementLookup.MeasurementInfo> ipGrid, String projectId, String experimentId) {
    FetchCallback<IpMeasurementLookup.MeasurementInfo, SearchTermFilter> fetchCallback = query -> {
      String searchTerm = query.getFilter().map(SearchTermFilter::searchTerm).orElse("");
      return ipMeasurementLookup.lookupIpMeasurements(
          projectId, query.getOffset(), query.getLimit(),
          VaadinSpringDataHelpers.toSpringDataSort(query),
          IpMeasurementLookup.MeasurementFilter.forExperiment(experimentId)
              .withSearch(searchTerm, clientTimeZoneOffset.get(),
                  MEASUREMENT_REGISTRATION_DATE_TIME_FORMAT));
    };

    CountCallback<IpMeasurementLookup.MeasurementInfo, SearchTermFilter> countCallback = query -> {
      String searchTerm = query.getFilter().map(SearchTermFilter::searchTerm).orElse("");
      return ipMeasurementLookup.countIpMeasurements(projectId,
          IpMeasurementLookup.MeasurementFilter.forExperiment(experimentId)
              .withSearch(searchTerm, clientTimeZoneOffset.get(),
                  MEASUREMENT_REGISTRATION_DATE_TIME_FORMAT));
    };

    var configuration = FilterGridConfigurations.lazy(fetchCallback, countCallback);

    var filterGrid = FilterGrid.create(
        IpMeasurementLookup.MeasurementInfo.class,
        SearchTermFilter.class,
        configuration.applyConfiguration(ipGrid),
        this::getIpSearchTermFilter,
        (searchTerm, filter) -> filter.replaceWith(searchTerm));

    filterGrid.itemDisplayLabel("measurement");
    filterGrid.searchFieldPlaceholder("Search Measurements");
    return filterGrid;
  }

  private Grid<IpMeasurementLookup.MeasurementInfo> createIpGrid() {
    var ipGrid = new Grid<IpMeasurementLookup.MeasurementInfo>();
    ipGrid.setMultiSort(true, MultiSortPriority.APPEND, true);
    ipGrid.addColumn(IpMeasurementLookup.MeasurementInfo::measurementCode)
        .setHeader("QBiC Measurement ID")
        .setSortProperty(IpMeasurementLookup.IpSortKey.MEASUREMENT_ID.sortKey())
        .setComparator(
            Comparator.comparing(IpMeasurementLookup.MeasurementInfo::measurementCode))
        .setAutoWidth(true)
        .setResizable(true)
        .setFrozen(true);
    ipGrid.addColumn(IpMeasurementLookup.MeasurementInfo::measurementName)
        .setHeader("Measurement Name")
        .setSortProperty(IpMeasurementLookup.IpSortKey.MEASUREMENT_NAME.sortKey())
        .setComparator(
            Comparator.comparing(IpMeasurementLookup.MeasurementInfo::measurementCode))
        .setAutoWidth(true)
        .setResizable(true);
    ipGrid.addComponentColumn(measurementInfo -> renderSamplesIp(measurementInfo,
            info -> "%s (%s)".formatted(info.sampleLabel(), info.sampleCode())))
        .setHeader("Samples")
        .setSortable(false)
        .setAutoWidth(true)
        .setResizable(true);
    ipGrid.addComponentColumn(
            info -> renderOrganisation(info.organisation().label(),
                info.organisation().iri()))
        .setHeader("Organisation")
        .setSortable(false)
        .setAutoWidth(true)
        .setResizable(true);
    ipGrid.addColumn(IpMeasurementLookup.MeasurementInfo::facility)
        .setHeader("Facility")
        .setSortProperty(IpMeasurementLookup.IpSortKey.FACILITY.sortKey())
        .setComparator(Comparator.comparing(IpMeasurementLookup.MeasurementInfo::facility))
        .setAutoWidth(true)
        .setResizable(true);
    ipGrid.addComponentColumn(
            info -> renderInstrument(info.instrument().label(),
                info.instrument().oboId(),
                info.instrument().iri()))
        .setHeader("Instrument")
        .setSortable(false)
        .setAutoWidth(true)
        .setResizable(true);
    ipGrid.addColumn(IpMeasurementLookup.MeasurementInfo::mhcAntibody)
        .setHeader("MHC Antibody")
        .setSortProperty(IpMeasurementLookup.IpSortKey.MHC_ANTIBODY.sortKey())
        .setComparator(
            Comparator.comparing(IpMeasurementLookup.MeasurementInfo::mhcAntibody))
        .setAutoWidth(true)
        .setResizable(true);
    ipGrid.addColumn(IpMeasurementLookup.MeasurementInfo::mhcTypingMethod)
        .setHeader("MHC Typing Method")
        .setSortProperty(IpMeasurementLookup.IpSortKey.MHC_TYPING_METHOD.sortKey())
        .setComparator(
            Comparator.comparing(IpMeasurementLookup.MeasurementInfo::mhcTypingMethod))
        .setAutoWidth(true)
        .setResizable(true);
    ipGrid.addColumn(IpMeasurementLookup.MeasurementInfo::enrichmentMethod)
        .setHeader("Enrichment Method")
        .setSortProperty(IpMeasurementLookup.IpSortKey.ENRICHMENT_METHOD.sortKey())
        .setComparator(
            Comparator.comparing(IpMeasurementLookup.MeasurementInfo::enrichmentMethod))
        .setAutoWidth(true)
        .setResizable(true);
    ipGrid.addColumn(IpMeasurementLookup.MeasurementInfo::lcmsMethod)
        .setHeader("LCMS Method")
        .setSortProperty(IpMeasurementLookup.IpSortKey.LCMS_METHOD.sortKey())
        .setComparator(Comparator.comparing(IpMeasurementLookup.MeasurementInfo::lcmsMethod))
        .setAutoWidth(true)
        .setResizable(true);
    ipGrid.addColumn(IpMeasurementLookup.MeasurementInfo::lcColumn)
        .setHeader("LC Column")
        .setSortProperty(IpMeasurementLookup.IpSortKey.LC_COLUMN.sortKey())
        .setComparator(Comparator.comparing(IpMeasurementLookup.MeasurementInfo::lcColumn))
        .setAutoWidth(true)
        .setResizable(true);
    ipGrid.addColumn(IpMeasurementLookup.MeasurementInfo::dataAcquisition)
        .setHeader("Data Acquisition")
        .setSortProperty(IpMeasurementLookup.IpSortKey.DATA_ACQUISITION.sortKey())
        .setComparator(
            Comparator.comparing(IpMeasurementLookup.MeasurementInfo::dataAcquisition))
        .setAutoWidth(true)
        .setResizable(true);
    ipGrid.addColumn(IpMeasurementLookup.MeasurementInfo::massRange)
        .setHeader("Mass Range")
        .setSortProperty(IpMeasurementLookup.IpSortKey.MASS_RANGE.sortKey())
        .setComparator(Comparator.comparing(IpMeasurementLookup.MeasurementInfo::massRange))
        .setAutoWidth(true)
        .setResizable(true);
    ipGrid.addColumn(IpMeasurementLookup.MeasurementInfo::retentionTimeRange)
        .setHeader("Retention Time Range")
        .setSortProperty(IpMeasurementLookup.IpSortKey.RETENTION_TIME_RANGE.sortKey())
        .setComparator(
            Comparator.comparing(IpMeasurementLookup.MeasurementInfo::retentionTimeRange))
        .setAutoWidth(true)
        .setResizable(true);
    ipGrid.addColumn(IpMeasurementLookup.MeasurementInfo::chargeRange)
        .setHeader("Charge Range")
        .setSortProperty(IpMeasurementLookup.IpSortKey.CHARGE_RANGE.sortKey())
        .setComparator(Comparator.comparing(IpMeasurementLookup.MeasurementInfo::chargeRange))
        .setAutoWidth(true)
        .setResizable(true);
    ipGrid.addColumn(IpMeasurementLookup.MeasurementInfo::ionMobilityRange)
        .setHeader("Ion Mobility Range")
        .setSortProperty(IpMeasurementLookup.IpSortKey.ION_MOBILITY_RANGE.sortKey())
        .setComparator(
            Comparator.comparing(IpMeasurementLookup.MeasurementInfo::ionMobilityRange))
        .setAutoWidth(true)
        .setResizable(true);
    ipGrid.addColumn(IpMeasurementLookup.MeasurementInfo::sampleMass)
        .setHeader("Sample Mass (mg)")
        .setSortProperty(IpMeasurementLookup.IpSortKey.SAMPLE_MASS.sortKey())
        .setComparator(
            Comparator.comparing(IpMeasurementLookup.MeasurementInfo::sampleMass))
        .setAutoWidth(true)
        .setResizable(true);
    ipGrid.addColumn(IpMeasurementLookup.MeasurementInfo::sampleVolume)
        .setHeader("Sample Volume (µl)")
        .setSortProperty(IpMeasurementLookup.IpSortKey.SAMPLE_VOLUME.sortKey())
        .setComparator(
            Comparator.comparing(IpMeasurementLookup.MeasurementInfo::sampleVolume))
        .setAutoWidth(true)
        .setResizable(true);
    ipGrid.addColumn(IpMeasurementLookup.MeasurementInfo::cycleFractionName)
        .setHeader("Cycle/Fraction Name")
        .setSortProperty(IpMeasurementLookup.IpSortKey.CYCLE_FRACTION_NAME.sortKey())
        .setComparator(
            Comparator.comparing(IpMeasurementLookup.MeasurementInfo::cycleFractionName))
        .setAutoWidth(true)
        .setResizable(true);
    ipGrid.addColumn(IpMeasurementLookup.MeasurementInfo::prepDate)
        .setHeader("Prep Date")
        .setSortProperty(IpMeasurementLookup.IpSortKey.PREP_DATE.sortKey())
        .setComparator(
            Comparator.comparing(IpMeasurementLookup.MeasurementInfo::prepDate))
        .setAutoWidth(true)
        .setResizable(true);
    ipGrid.addColumn(IpMeasurementLookup.MeasurementInfo::msRunDate)
        .setHeader("MS Run Date")
        .setSortProperty(IpMeasurementLookup.IpSortKey.MS_RUN_DATE.sortKey())
        .setComparator(
            Comparator.comparing(IpMeasurementLookup.MeasurementInfo::msRunDate))
        .setAutoWidth(true)
        .setResizable(true);
    ipGrid.addColumn(info -> formatTime(info.registeredAt(),
            MEASUREMENT_REGISTRATION_DATE_TIME_FORMAT))
        .setHeader("Registration Date")
        .setKey(IpMeasurementLookup.IpSortKey.REGISTRATION_DATE.sortKey())
        .setComparator(Comparator.comparing(IpMeasurementLookup.MeasurementInfo::registeredAt))
        .setAutoWidth(true)
        .setResizable(true);
    ipGrid.addColumn(info -> info.comment() != null ? info.comment() : "")
        .setHeader("Comment")
        .setSortable(false)
        .setAutoWidth(true)
        .setResizable(true);
    return ipGrid;
  }

  private static Component renderSamplesIp(IpMeasurementLookup.MeasurementInfo measurementInfo,
      Function<IpMeasurementLookup.SampleInfo, String> singleSampleConverter) {
    var sampleInfos = measurementInfo.sampleInfos();
    if (sampleInfos.size() == 1) {
      var sampleInfo = sampleInfos.stream().findFirst().orElseThrow();
      String singleSampleText = singleSampleConverter.apply(sampleInfo);
      return new Span(singleSampleText);
    }
    var displayLabel = measurementInfo.samplePool();
    var expandIcon = VaadinIcon.EXPAND_SQUARE.create();
    expandIcon.addClassNames("expand-icon", "icon-size-m", "color-primary",
        "padding-horizontal-02");
    var pooledSamplesSpan = new Span(new Span(displayLabel), expandIcon);
    pooledSamplesSpan.addClassNames("sample-column-cell", "clickable");
    pooledSamplesSpan.addClickListener(
        event -> openPooledSampleDialogIp(measurementInfo));
    return pooledSamplesSpan;
  }

  private static void openPooledSampleDialogIp(
      IpMeasurementLookup.MeasurementInfo measurementInfo) {
    AppDialog dialog = AppDialog.medium();
    DialogHeader.with(dialog, "View Pooled Measurement");
    DialogFooter.withConfirmOnly(dialog, "Close");
    var sampleInfoGrid = new Grid<IpMeasurementLookup.SampleInfo>();
    sampleInfoGrid.addColumn(IpMeasurementLookup.SampleInfo::sampleLabel)
        .setHeader("Sample Name")
        .setAutoWidth(true);
    sampleInfoGrid.addColumn(IpMeasurementLookup.SampleInfo::sampleCode)
        .setHeader("Sample Id")
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

  private static Component renderSamplesNgs(NgsMeasurementLookup.MeasurementInfo measurementInfo,
      Function<NgsMeasurementLookup.SampleInfo, String> singleSampleConverter) {
    var sampleInfos = measurementInfo.sampleInfos();
    if (sampleInfos.size() == 1) {
      NgsMeasurementLookup.SampleInfo sampleInfo = sampleInfos.stream().findFirst().orElseThrow();
      String singleSampleText = singleSampleConverter.apply(sampleInfo);
      return new Span(singleSampleText);
    }
    var displayLabel = measurementInfo.samplePool();
    var expandIcon = VaadinIcon.EXPAND_SQUARE.create();
    expandIcon.addClassNames("expand-icon", "icon-size-m", "color-primary",
        "padding-horizontal-02");
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
    expandIcon.addClassNames("expand-icon", "icon-size-m", "color-primary",
        "padding-horizontal-02");
    var pooledSamplesSpan = new Span(new Span(displayLabel), expandIcon);
    pooledSamplesSpan.addClassNames("sample-column-cell", "clickable");
    pooledSamplesSpan.addClickListener(event -> openPooledSampleDialogPxp(measurementInfo));
    return pooledSamplesSpan;
  }

  private static void openPooledSampleDialogNgs(
      NgsMeasurementLookup.MeasurementInfo measurementInfo) {
    AppDialog dialog = AppDialog.medium();
    DialogHeader.with(dialog, "View Pooled Measurement");
    DialogFooter.withConfirmOnly(dialog, "Close");
    var sampleInfoGrid = new Grid<NgsMeasurementLookup.SampleInfo>();
    sampleInfoGrid.addColumn(NgsMeasurementLookup.SampleInfo::sampleLabel)
        .setHeader("Sample Name")
        .setAutoWidth(true);
    sampleInfoGrid.addColumn(NgsMeasurementLookup.SampleInfo::sampleCode)
        .setHeader("Sample Id")
        .setAutoWidth(true);
    sampleInfoGrid.addColumn(NgsMeasurementLookup.SampleInfo::indexI7)
        .setHeader("Index i7")
        .setAutoWidth(true);
    sampleInfoGrid.addColumn(NgsMeasurementLookup.SampleInfo::indexI5)
        .setHeader("Index i5")
        .setAutoWidth(true);
    sampleInfoGrid.addColumn(NgsMeasurementLookup.SampleInfo::comment)
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
      ComponentEvent<MeasurementDetailsComponent> {

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public NgsMeasurementRegistrationRequested(MeasurementDetailsComponent source,
        boolean fromClient) {
      super(source, fromClient);
    }
  }

  public Registration addNgsRegisterListener(
      ComponentEventListener<NgsMeasurementRegistrationRequested> listener) {
    return addListener(NgsMeasurementRegistrationRequested.class, listener);
  }

  public static class NgsMeasurementEditRequested extends
      ComponentEvent<MeasurementDetailsComponent> {

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
        MeasurementDetailsComponent source, boolean fromClient) {
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
      ComponentEvent<MeasurementDetailsComponent> {

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
        MeasurementDetailsComponent source, boolean fromClient) {
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
      ComponentEvent<MeasurementDetailsComponent> {

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
        MeasurementDetailsComponent source,
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
      ComponentEvent<MeasurementDetailsComponent> {

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public PxpMeasurementRegistrationRequested(MeasurementDetailsComponent source,
        boolean fromClient) {
      super(source, fromClient);
    }
  }

  public Registration addPxpRegisterListener(
      ComponentEventListener<PxpMeasurementRegistrationRequested> listener) {
    return addListener(PxpMeasurementRegistrationRequested.class, listener);
  }

  public static class PxpMeasurementEditRequested extends
      ComponentEvent<MeasurementDetailsComponent> {

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
        MeasurementDetailsComponent source, boolean fromClient) {
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
      ComponentEvent<MeasurementDetailsComponent> {

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
        MeasurementDetailsComponent source, boolean fromClient) {
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
      ComponentEvent<MeasurementDetailsComponent> {

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
        MeasurementDetailsComponent source,
        boolean fromClient) {
      super(source, fromClient);
      this.measurementIds = measurementIds.stream().toList();
    }

    public List<String> measurementIds() {
      return measurementIds;
    }
  }

  public Registration addPxpDeletionListener(
      ComponentEventListener<PxpMeasurementDeletionRequested> listener) {
    return addListener(PxpMeasurementDeletionRequested.class, listener);
  }

  public static class IpMeasurementRegistrationRequested extends
      ComponentEvent<MeasurementDetailsComponent> {

    public IpMeasurementRegistrationRequested(MeasurementDetailsComponent source,
        boolean fromClient) {
      super(source, fromClient);
    }
  }

  public Registration addIpRegisterListener(
      ComponentEventListener<IpMeasurementRegistrationRequested> listener) {
    return addListener(IpMeasurementRegistrationRequested.class, listener);
  }

  public static class IpMeasurementDeletionRequested extends
      ComponentEvent<MeasurementDetailsComponent> {

    private final List<String> measurementIds;

    public IpMeasurementDeletionRequested(List<String> measurementIds,
        MeasurementDetailsComponent source,
        boolean fromClient) {
      super(source, fromClient);
      this.measurementIds = measurementIds.stream().toList();
    }

    public List<String> measurementIds() {
      return measurementIds;
    }
  }

  public Registration addIpDeletionListener(
      ComponentEventListener<IpMeasurementDeletionRequested> listener) {
    return addListener(IpMeasurementDeletionRequested.class, listener);
  }

  public static class IpMeasurementExportRequested extends
      ComponentEvent<MeasurementDetailsComponent> {

    private final List<String> measurementIds;

    public IpMeasurementExportRequested(List<String> measurementIds,
        MeasurementDetailsComponent source, boolean fromClient) {
      super(source, fromClient);
      this.measurementIds = measurementIds.stream().toList();
    }

    public List<String> measurementIds() {
      return measurementIds;
    }
  }

  public Registration addIpExportListener(
      ComponentEventListener<IpMeasurementExportRequested> listener) {
    return addListener(IpMeasurementExportRequested.class, listener);
  }

}
