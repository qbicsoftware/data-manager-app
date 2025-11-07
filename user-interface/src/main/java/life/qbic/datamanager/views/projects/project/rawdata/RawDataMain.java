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
import java.util.Objects;
import life.qbic.application.commons.ApplicationException;
import life.qbic.datamanager.ClientDetailsProvider;
import life.qbic.datamanager.views.AppRoutes.ProjectRoutes;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.account.PersonalAccessTokenMain;
import life.qbic.datamanager.views.general.Disclaimer;
import life.qbic.datamanager.views.general.Main;
import life.qbic.datamanager.views.general.download.DownloadComponent;
import life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory;
import life.qbic.datamanager.views.projects.project.experiments.ExperimentMainLayout;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.api.AsyncProjectService;
import life.qbic.projectmanagement.application.dataset.RemoteRawDataService;
import life.qbic.projectmanagement.application.experiment.ExperimentInformationService;
import life.qbic.projectmanagement.application.measurement.MeasurementMetadata;
import life.qbic.projectmanagement.application.measurement.MeasurementService;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
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
  private final MessageSourceNotificationFactory messageSourceNotificationFactory;
  private final ClientDetailsProvider clientDetailsProvider;
  private Div rawdataDetailsComponentContainer;
  private final RawDataDownloadInformationComponent rawDataDownloadInformationComponent;
  private final TextField rawDataSearchField = new TextField();
  private final Div content = new Div();
  private final transient MeasurementService measurementService;
  private final transient RemoteRawDataService remoteRawDataService;
  private final Disclaimer registerMeasurementsDisclaimer;
  private final Disclaimer noRawDataRegisteredDisclaimer;
  private final String rawDataSourceURL;
  private final String documentationUrl;
  private final AsyncProjectService asyncProjectService;
  private transient Context context;

  public RawDataMain(
      @Autowired RawDataDownloadInformationComponent rawDataDownloadInformationComponent,
      @Autowired ExperimentInformationService experimentInformationService,
      @Autowired MeasurementService measurementService,
      @Autowired RemoteRawDataService remoteRawDataService,
      @Value("${server.download.api.measurement.url}") String dataSourceURL,
      @Value("${qbic.communication.documentation.url}") String documentationUrl,
      @Autowired AsyncProjectService asyncProjectService,
      @Autowired ClientDetailsProvider clientDetailsProvider,
      MessageSourceNotificationFactory messageSourceNotificationFactory) {
    this.rawDataDownloadInformationComponent = Objects.requireNonNull(
        rawDataDownloadInformationComponent);
    this.measurementService = Objects.requireNonNull(measurementService);
    this.remoteRawDataService = Objects.requireNonNull(remoteRawDataService);
    this.rawDataSourceURL = Objects.requireNonNull(dataSourceURL);
    this.documentationUrl = Objects.requireNonNull(documentationUrl);
    this.asyncProjectService = Objects.requireNonNull(asyncProjectService);
    this.messageSourceNotificationFactory = Objects.requireNonNull(messageSourceNotificationFactory);
    this.clientDetailsProvider = Objects.requireNonNull(clientDetailsProvider);
    registerMeasurementsDisclaimer = createNoMeasurementsRegisteredDisclaimer();
    registerMeasurementsDisclaimer.addClassName("no-measurements-registered-disclaimer");
    noRawDataRegisteredDisclaimer = createNoRawDataRegisteredDisclaimer();
    noRawDataRegisteredDisclaimer.addClassName("no-raw-data-registered-disclaimer");
    downloadComponent = new DownloadComponent();
    rawdataDetailsComponentContainer = new Div();

    initContent();
    add(registerMeasurementsDisclaimer);
    add(noRawDataRegisteredDisclaimer);
    add(rawdataDetailsComponentContainer);
    add(rawDataDownloadInformationComponent);
    add(downloadComponent);
    addListeners();
    addClassName("raw-data");

  }

  private void initContent() {
    Span titleField = new Span();
    titleField.setText("Download Raw Data");
    titleField.addClassNames("title");
    content.add(titleField);
    add(content);
    content.addClassName("raw-data-main-content");
  }

  private void addListeners() {
    rawDataDownloadInformationComponent.addPersonalAccessTokenNavigationListener(
        event -> UI.getCurrent().navigate(
            PersonalAccessTokenMain.class));
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
    asyncProjectService.getProjectCode(projectID).blockOptional()
        .ifPresent(projectCode -> context = context.withProjectCode(projectCode.value()));
    setRawDataInformation();
  }

  private void setRawDataInformation() {
    //Check if measurements exist
    ExperimentId currentExperimentId = context.experimentId().orElseThrow();
    if (!measurementService.hasMeasurements(currentExperimentId)) {
      showRegisterMeasurementDisclaimer();
      return;
    }
    if (!remoteRawDataService.hasRawData(currentExperimentId)) {
      showNoRawDataRegisteredDisclaimer();
    } else {
      showRawDataForRegisteredMeasurements();
    }
  }

  private void showRegisterMeasurementDisclaimer() {
    noRawDataRegisteredDisclaimer.setVisible(false);
    content.setVisible(false);
    rawdataDetailsComponentContainer.setVisible(false);
    rawDataDownloadInformationComponent.setVisible(false);
    registerMeasurementsDisclaimer.setVisible(true);
  }

  private void showNoRawDataRegisteredDisclaimer() {
    registerMeasurementsDisclaimer.setVisible(false);
    content.setVisible(false);
    rawdataDetailsComponentContainer.setVisible(false);
    rawDataDownloadInformationComponent.setVisible(false);
    noRawDataRegisteredDisclaimer.setVisible(true);
  }

  private void showRawDataForRegisteredMeasurements() {
    noRawDataRegisteredDisclaimer.setVisible(false);
    registerMeasurementsDisclaimer.setVisible(false);
    content.setVisible(true);
    rawdataDetailsComponentContainer.removeAll();
    rawdataDetailsComponentContainer.add(
        new RawDataDetailsComponent(
            clientDetailsProvider,
            asyncProjectService,
            context,
            rawDataSourceURL,
            messageSourceNotificationFactory));
    rawDataDownloadInformationComponent.setVisible(true);
    rawdataDetailsComponentContainer.setVisible(true);
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
