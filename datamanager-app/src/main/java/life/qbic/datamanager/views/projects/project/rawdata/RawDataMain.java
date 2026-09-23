package life.qbic.datamanager.views.projects.project.rawdata;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.page.History;
import com.vaadin.flow.component.page.History.HistoryStateChangeEvent;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.BeforeLeaveObserver;
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
import life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory;
import life.qbic.datamanager.views.projects.project.experiments.ExperimentMainLayout;
import life.qbic.datamanager.views.projects.project.rawdata.pagination.RawDataListState;
import life.qbic.datamanager.views.projects.project.rawdata.pagination.RawDataListStateCodec;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.api.AsyncProjectService;
import life.qbic.projectmanagement.application.dataset.RemoteRawDataService;
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
 * {@link ExperimentId} and {@link ProjectId} in the URL. The raw data is shown as paginated
 * lists with cross-page selection (FEAT-PAG-LIST-04); the list state (active tab + per-tab
 * page/size/filter/sort) is mirrored into the browser URL (USER-R-03).
 */

@SpringComponent
@UIScope
@Route(value = "projects/:projectId?/experiments/:experimentId?/rawdata", layout = ExperimentMainLayout.class)
@PermitAll
public class RawDataMain extends Main implements BeforeEnterObserver, BeforeLeaveObserver {

  public static final String PROJECT_ID_ROUTE_PARAMETER = "projectId";
  public static final String EXPERIMENT_ID_ROUTE_PARAMETER = "experimentId";
  @Serial
  private static final long serialVersionUID = -4506659645977994192L;
  private static final Logger log = LoggerFactory.logger(RawDataMain.class);
  private final MessageSourceNotificationFactory messageSourceNotificationFactory;
  private final ClientDetailsProvider clientDetailsProvider;
  private final Div rawdataDetailsComponentContainer;
  private final RawDataDownloadInformationComponent rawDataDownloadInformationComponent;
  private final Div content = new Div();
  private final transient MeasurementService measurementService;
  private final transient RemoteRawDataService remoteRawDataService;
  private final Disclaimer registerMeasurementsDisclaimer;
  private final Disclaimer noRawDataRegisteredDisclaimer;
  private final String rawDataSourceURL;
  private final String documentationUrl;
  private final AsyncProjectService asyncProjectService;
  private final RawDataDetailsComponent rawDataDetailsComponent;
  /**
   * The framework's own history state change handler, captured before this view installs its own.
   */
  private History.HistoryStateChangeHandler routerHistoryStateChangeHandler;
  private final History.HistoryStateChangeHandler listStateHistoryHandler =
      this::onHistoryStateChange;
  private transient Context context;

  public RawDataMain(
      @Autowired RawDataDownloadInformationComponent rawDataDownloadInformationComponent,
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
    rawdataDetailsComponentContainer = new Div();
    rawdataDetailsComponentContainer.addClassNames("display-contents");
    rawDataDetailsComponent = new RawDataDetailsComponent(
        asyncProjectService,
        new Context(),
        rawDataSourceURL,
        messageSourceNotificationFactory);

    initContent();
    add(registerMeasurementsDisclaimer);
    add(noRawDataRegisteredDisclaimer);
    add(rawdataDetailsComponentContainer);
    add(rawDataDownloadInformationComponent);
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

    // URL list-state synchronisation (USER-R-03): capture the router handler, install our own,
    // and seed the container from the URL on direct load / reload / shared links.
    History history = UI.getCurrent().getPage().getHistory();
    History.HistoryStateChangeHandler currentHandler = history.getHistoryStateChangeHandler();
    if (currentHandler != listStateHistoryHandler) {
      routerHistoryStateChangeHandler = currentHandler;
    }
    history.setHistoryStateChangeHandler(listStateHistoryHandler);
    String basePath = String.format(ProjectRoutes.RAWDATA, projectID, experimentId);
    rawDataDetailsComponent.setBasePath(basePath);
    RawDataListState urlState = RawDataListStateCodec.parse(
        event.getLocation().getQueryParameters(),
        rawDataDetailsComponent.getTabPagination().listState());

    setRawDataInformation(urlState);
  }

  /**
   * Gives the history state change handler back to the router when this view is left.
   */
  @Override
  public void beforeLeave(BeforeLeaveEvent event) {
    getUI().ifPresent(ui -> ui.getPage().getHistory()
        .setHistoryStateChangeHandler(routerHistoryStateChangeHandler));
  }

  /**
   * Re-applies the list state when the browser history changes (back/forward or router-link
   * navigation). Non-raw-data locations are delegated back to the router handler.
   */
  private void onHistoryStateChange(HistoryStateChangeEvent event) {
    String expectedPath = currentRawDataPath();
    if (expectedPath == null || !expectedPath.equals(event.getLocation().getPath())) {
      if (routerHistoryStateChangeHandler != null) {
        routerHistoryStateChangeHandler.onHistoryStateChange(event);
      }
      return;
    }
    RawDataListState urlState = RawDataListStateCodec.parse(
        event.getLocation().getQueryParameters(),
        rawDataDetailsComponent.getTabPagination().listState());
    rawDataDetailsComponent.getTabPagination().applyExternalState(urlState);
  }

  private String currentRawDataPath() {
    if (context == null || context.projectId().isEmpty() || context.experimentId().isEmpty()) {
      return null;
    }
    return String.format(ProjectRoutes.RAWDATA,
        context.projectId().orElseThrow().value(),
        context.experimentId().orElseThrow().value());
  }

  private void setRawDataInformation(RawDataListState urlState) {
    //Check if measurements exist
    ExperimentId currentExperimentId = context.experimentId().orElseThrow();
    var projectId = context.projectId().orElseThrow();

    if (!measurementService.hasMeasurements(projectId, currentExperimentId)) {
      showRegisterMeasurementDisclaimer();
      return;
    }
    if (!remoteRawDataService.hasRawData(projectId.value(), currentExperimentId)) {
      showNoRawDataRegisteredDisclaimer();
    } else {
      showRawDataForRegisteredMeasurements(urlState);
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

  private void showRawDataForRegisteredMeasurements(RawDataListState urlState) {
    noRawDataRegisteredDisclaimer.setVisible(false);
    registerMeasurementsDisclaimer.setVisible(false);
    content.setVisible(true);
    rawdataDetailsComponentContainer.removeAll();
    // setContext resets the per-tab list state to defaults when a new experiment is shown; apply
    // the URL state afterwards so the initial fetch reflects the tab/page/size/filter/sort of a
    // reloaded or shared link instead of the defaults (USER-R-03).
    rawDataDetailsComponent.setContext(context);
    rawDataDetailsComponent.getTabPagination().applyExternalState(urlState);
    rawdataDetailsComponentContainer.add(rawDataDetailsComponent);
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

}