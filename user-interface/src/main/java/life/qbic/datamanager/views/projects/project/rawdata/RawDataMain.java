package life.qbic.datamanager.views.projects.project.rawdata;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.PermitAll;
import java.io.Serial;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.FileNameFormatter;
import life.qbic.datamanager.files.export.download.ByteArrayDownloadStreamProvider;
import life.qbic.datamanager.files.export.rawdata.RawDataUrlFile;
import life.qbic.datamanager.views.AppRoutes.ProjectRoutes;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.account.PersonalAccessTokenMain;
import life.qbic.datamanager.views.general.Disclaimer;
import life.qbic.datamanager.views.general.Main;
import life.qbic.datamanager.views.general.download.DownloadComponent;
import life.qbic.datamanager.views.notifications.ErrorMessage;
import life.qbic.datamanager.views.notifications.StyledNotification;
import life.qbic.datamanager.views.projects.project.experiments.ExperimentMainLayout;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.dataset.RawDataService;
import life.qbic.projectmanagement.application.experiment.ExperimentInformationService;
import life.qbic.projectmanagement.application.measurement.MeasurementMetadata;
import life.qbic.projectmanagement.application.measurement.MeasurementService;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.measurement.MeasurementCode;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;


/**
 * Raw Data Main Component
 * <p>
 * This component hosts the components necessary to show and download the raw data associated with
 * the {@link MeasurementMetadata} within an {@link Experiment} via the provided
 * {@link ExperimentId} and {@link ProjectId} in the URL
 */

@SpringComponent
@UIScope
@Route(value = "projects/:projectId?/experiments/:experimentId?/rawdata", layout = ExperimentMainLayout.class)
@PermitAll
public class RawDataMain extends Main implements BeforeEnterObserver {

  public static final String PROJECT_ID_ROUTE_PARAMETER = "projectId";
  public static final String EXPERIMENT_ID_ROUTE_PARAMETER = "experimentId";
  @Serial
  private static final long serialVersionUID = -4506659645977994192L;
  private static final Logger log = LoggerFactory.logger(RawDataMain.class);
  private final DownloadComponent downloadComponent;
  private final RawDataDetailsComponent rawdataDetailsComponent;
  private final RawDataDownloadInformationComponent rawDataDownloadInformationComponent;
  private final TextField rawDataSearchField = new TextField();
  private final Div content = new Div();
  private final transient ExperimentInformationService experimentInformationService;
  private final transient MeasurementService measurementService;
  private final transient RawDataService rawDataService;
  private final Disclaimer registerMeasurementsDisclaimer;
  private final Disclaimer noRawDataRegisteredDisclaimer;
  private final String rawDataSourceURL;
  private final String documentationUrl;
  private final ProjectInformationService projectInformationService;
  private transient Context context;

  public RawDataMain(@Autowired RawDataDetailsComponent rawDataDetailsComponent,
      @Autowired RawDataDownloadInformationComponent rawDataDownloadInformationComponent,
      @Autowired ExperimentInformationService experimentInformationService,
      @Autowired MeasurementService measurementService,
      @Autowired RawDataService rawDataService,
      @Value("${server.download.api.measurement.url}") String dataSourceURL,
      @Value("${qbic.communication.documentation.url}") String documentationUrl,
      @Autowired ProjectInformationService projectInformationService) {
    this.projectInformationService = Objects.requireNonNull(projectInformationService);
    this.rawdataDetailsComponent = Objects.requireNonNull(rawDataDetailsComponent);
    this.rawDataDownloadInformationComponent = Objects.requireNonNull(
        rawDataDownloadInformationComponent);
    this.experimentInformationService = Objects.requireNonNull(experimentInformationService);
    this.measurementService = Objects.requireNonNull(measurementService);
    this.rawDataService = Objects.requireNonNull(rawDataService);
    this.rawDataSourceURL = Objects.requireNonNull(dataSourceURL);
    this.documentationUrl = Objects.requireNonNull(documentationUrl);
    registerMeasurementsDisclaimer = createNoMeasurementsRegisteredDisclaimer();
    registerMeasurementsDisclaimer.addClassName("no-measurements-registered-disclaimer");
    noRawDataRegisteredDisclaimer = createNoRawDataRegisteredDisclaimer();
    noRawDataRegisteredDisclaimer.addClassName("no-raw-data-registered-disclaimer");
    downloadComponent = new DownloadComponent();

    initContent();
    add(registerMeasurementsDisclaimer);
    add(noRawDataRegisteredDisclaimer);
    add(rawDataDetailsComponent);
    add(rawDataDownloadInformationComponent);
    add(downloadComponent);
    addListeners();
    addClassName("raw-data");
    log.debug(String.format(
        "New instance for %s(#%s) created with %s(#%s) and %s(#%s)",
        getClass().getSimpleName(), System.identityHashCode(this),
        rawdataDetailsComponent.getClass().getSimpleName(),
        System.identityHashCode(rawDataDetailsComponent),
        rawDataDownloadInformationComponent.getClass().getSimpleName(),
        System.identityHashCode(rawDataDownloadInformationComponent)));

  }

  private void initContent() {
    Span titleField = new Span();
    titleField.setText("Download Raw Data");
    titleField.addClassNames("title");
    content.add(titleField);
    initSearchFieldAndButtonBar();
    add(content);
    content.addClassName("raw-data-main-content");
  }

  private void addListeners() {
    rawDataDownloadInformationComponent.addDownloadUrlListener(this::handleUrlDownload);
    rawDataDownloadInformationComponent.addPersonalAccessTokenNavigationListener(
        event -> UI.getCurrent().navigate(
            PersonalAccessTokenMain.class));
  }

  private void initSearchFieldAndButtonBar() {
    rawDataSearchField.setPlaceholder("Search");
    rawDataSearchField.setClearButtonVisible(true);
    rawDataSearchField.setSuffixComponent(VaadinIcon.SEARCH.create());
    rawDataSearchField.addClassNames("search-field");
    rawDataSearchField.setValueChangeMode(ValueChangeMode.LAZY);
    rawDataSearchField.addValueChangeListener(
        event -> rawdataDetailsComponent.setSearchedRawDataValue((event.getValue())));
    Button downloadRawDataUrl = new Button("Download URL list");
    downloadRawDataUrl.addClassName("primary");
    downloadRawDataUrl.addClickListener(this::handleUrlDownload);
    Span buttonAndField = new Span(rawDataSearchField, downloadRawDataUrl);
    buttonAndField.addClassName("buttonAndField");
    content.add(buttonAndField);
  }


  private void handleUrlDownload(ComponentEvent<?> event) {
    Collection<MeasurementCode> selectedMeasurements = rawdataDetailsComponent.getSelectedMeasurementUrls();
    if (selectedMeasurements.isEmpty()) {
      ErrorMessage errorMessage = new ErrorMessage("No Raw Data Item Selected",
          "Please select at least one measurement to generate an URL from");
      StyledNotification notification = new StyledNotification(errorMessage);
      notification.open();
    }
    var downloadUrls = generateDownloadUrls(selectedMeasurements);
    var currentExperiment = experimentInformationService.find(
            context.projectId().orElseThrow().value(), context.experimentId().orElseThrow())
        .orElseThrow();
    var currentProjectCode = projectInformationService.find(context.projectId().orElseThrow())
        .orElseThrow().getProjectCode().value();

    downloadComponent.trigger(new ByteArrayDownloadStreamProvider() {
      @Override
      public byte[] getBytes() {
        if (downloadUrls.isEmpty()) {
          return new byte[0];
        }
        return RawDataUrlFile
            .create(downloadUrls)
            .getBytes(StandardCharsets.UTF_8);
      }

      @Override
      public String getFilename() {
        return FileNameFormatter.formatWithTimestampedContext(LocalDate.now(), currentProjectCode,
            currentExperiment.getName(), "rawdata urls", "txt");
      }
    });
  }

  private List<RawDataURL> generateDownloadUrls(
      Collection<MeasurementCode> measurementCodeCollection) {
    return measurementCodeCollection.stream().map(measurementCode ->
        new RawDataURL(rawDataSourceURL, measurementCode.value())).toList();
  }

  /**
   * Callback executed before navigation to attaching Component chain is made.
   *
   * @param event before navigation event with event details
   */
  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    String projectID = event.getRouteParameters().get(PROJECT_ID_ROUTE_PARAMETER)
        .orElseThrow();
    if (!ProjectId.isValid(projectID)) {
      throw new ApplicationException("invalid project id " + projectID);
    }
    ProjectId parsedProjectId = ProjectId.parse(projectID);
    context = new Context().with(parsedProjectId);
    String experimentId = event.getRouteParameters().get(EXPERIMENT_ID_ROUTE_PARAMETER)
        .orElseThrow();
    if (!ExperimentId.isValid(experimentId)) {
      throw new ApplicationException("invalid experiment id " + experimentId);
    }
    ExperimentId parsedExperimentId = ExperimentId.parse(experimentId);
    this.context = context.with(parsedExperimentId);
    setRawDataInformation();
  }

  private void setRawDataInformation() {
    //Check if measurements exist
    ExperimentId currentExperimentId = context.experimentId().orElseThrow();
    if (!measurementService.hasMeasurements(currentExperimentId)) {
      showRegisterMeasurementDisclaimer();
      return;
    }
    if (!rawDataService.hasRawData(currentExperimentId)) {
      showNoRawDataRegisteredDisclaimer();
    } else {
      showRawDataForRegisteredMeasurements();
    }
  }

  private void showRegisterMeasurementDisclaimer() {
    noRawDataRegisteredDisclaimer.setVisible(false);
    content.setVisible(false);
    rawdataDetailsComponent.setVisible(false);
    rawDataDownloadInformationComponent.setVisible(false);
    registerMeasurementsDisclaimer.setVisible(true);
  }

  private void showNoRawDataRegisteredDisclaimer() {
    registerMeasurementsDisclaimer.setVisible(false);
    content.setVisible(false);
    rawdataDetailsComponent.setVisible(false);
    rawDataDownloadInformationComponent.setVisible(false);
    noRawDataRegisteredDisclaimer.setVisible(true);
  }

  private void showRawDataForRegisteredMeasurements() {
    noRawDataRegisteredDisclaimer.setVisible(false);
    registerMeasurementsDisclaimer.setVisible(false);
    content.setVisible(true);
    rawdataDetailsComponent.setContext(context);
    rawDataDownloadInformationComponent.setVisible(true);
    rawdataDetailsComponent.setVisible(true);
  }

  private Disclaimer createNoMeasurementsRegisteredDisclaimer() {
    Disclaimer noMeasurementsRegisteredDisclaimer = Disclaimer.createWithTitle(
        "Register your measurements first",
        "You have to register measurements before raw data download is possible",
        "Register Measurements");
    noMeasurementsRegisteredDisclaimer.addDisclaimerConfirmedListener(
        this::routeToMeasurementCreation);
    return noMeasurementsRegisteredDisclaimer;
  }

  private Disclaimer createNoRawDataRegisteredDisclaimer() {
    Disclaimer noRawDataRegistered = Disclaimer.createWithTitle(
        "Register your raw data first",
        "Raw data should be registered before you can view and download raw data files.\n"
            + "You can refer to our documentation to register raw data for your measurements.",
        "View Documentation");
    noRawDataRegistered.addDisclaimerConfirmedListener(
        this::routeToRawDataDocumentation);
    return noRawDataRegistered;
  }

  private void routeToRawDataDocumentation(ComponentEvent<?> componentEvent) {
    if (componentEvent.isFromClient()) {
      componentEvent.getSource().getUI().ifPresent(ui -> ui.getPage()
          .open(documentationUrl, "_blank"));
    }
  }

  private void routeToMeasurementCreation(ComponentEvent<?> componentEvent) {
    if (componentEvent.isFromClient()) {
      String currentExperimentId = context.experimentId().orElseThrow().value();
      String currentProjectId = context.projectId().orElseThrow().value();
      String routeToMeasurementPage = String.format(ProjectRoutes.MEASUREMENTS,
          currentProjectId,
          currentExperimentId);
      log.debug(String.format(
          "Rerouting to measurement page for experiment %s of project %s: %s",
          currentExperimentId, currentProjectId, routeToMeasurementPage));
      componentEvent.getSource().getUI().ifPresent(ui -> ui.navigate(routeToMeasurementPage));
    }
  }

  public record RawDataURL(String serverURL, String measurementCode) {

    @Override
    public String toString() {
      return String.format("%s/%s", serverURL(), measurementCode());
    }
  }

}
