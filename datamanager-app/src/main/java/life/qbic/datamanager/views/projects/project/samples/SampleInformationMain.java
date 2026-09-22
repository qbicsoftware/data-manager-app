package life.qbic.datamanager.views.projects.project.samples;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.Component;
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
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.PermitAll;
import java.io.Serial;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import life.qbic.application.commons.ApplicationException;
import life.qbic.datamanager.configuration.UploadConfiguration;
import life.qbic.datamanager.views.AppRoutes.ProjectRoutes;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.general.Disclaimer;
import life.qbic.datamanager.views.general.DisclaimerConfirmedEvent;
import life.qbic.datamanager.views.general.Main;
import life.qbic.datamanager.views.general.dialog.AlertDialog;
import life.qbic.datamanager.views.general.download.DownloadComponent;
import life.qbic.datamanager.views.general.pagination.ListState;
import life.qbic.datamanager.views.general.pagination.ListStateCodec;
import life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory;
import life.qbic.datamanager.views.projects.project.experiments.ExperimentMainLayout;
import life.qbic.datamanager.views.projects.project.samples.SampleDetailsComponent.SampleDeletionRequested;
import life.qbic.datamanager.views.projects.project.samples.SampleDetailsComponent.SampleEditRequested;
import life.qbic.datamanager.views.projects.project.samples.SampleDetailsComponent.SampleRegistrationRequested;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.EditSampleBatchDialog;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.RegisterSampleBatchDialog;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.DeletionService;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.ProjectOverview;
import life.qbic.projectmanagement.application.api.AsyncProjectService;
import life.qbic.projectmanagement.application.api.AsyncProjectService.ProjectCode;
import life.qbic.projectmanagement.application.confounding.ConfoundingVariableService.ExperimentReference;
import life.qbic.projectmanagement.application.experiment.ExperimentInformationService;
import life.qbic.projectmanagement.application.sample.SampleRegistrationServiceV2;
import life.qbic.projectmanagement.application.sample.SampleValidationService;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.sample.Sample;
import life.qbic.projectmanagement.domain.model.sample.SampleId;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Sample Information Main Component
 * <p>
 * This component hosts the components necessary to show and update the information for all
 * {@link Sample} associated with all {@link Experiment} of a {@link Project} information via the
 * provided {@link ProjectId} in the URL
 */

@Route(value = "projects/:projectId?/experiments/:experimentId?/samples", layout = ExperimentMainLayout.class)
@SpringComponent
@UIScope
@PermitAll
public class SampleInformationMain extends Main implements BeforeEnterObserver, BeforeLeaveObserver {

  @Serial
  private static final long serialVersionUID = 3778218989387044758L;

  /**
   * The framework's own history state change handler, captured before this view installs its own
   * (same pattern as {@code MeasurementMain}).
   */
  private History.HistoryStateChangeHandler routerHistoryStateChangeHandler;
  private final History.HistoryStateChangeHandler listStateHistoryHandler = this::onHistoryStateChange;
  private String basePath;
  private boolean suppressUrlWrite;

  public static final String PROJECT_ID_ROUTE_PARAMETER = "projectId";
  public static final String EXPERIMENT_ID_ROUTE_PARAMETER = "experimentId";
  private static final Logger log = LoggerFactory.logger(SampleInformationMain.class);
  private final transient ExperimentInformationService experimentInformationService;
  private final transient DeletionService deletionService;
  private transient Component sampleDetailsComponent;
  private final DownloadComponent downloadComponent;
  private final Div content = new Div();
  private final Disclaimer noGroupsDefinedDisclaimer;
  private final Disclaimer noSamplesRegisteredDisclaimer;
  private final transient ProjectInformationService projectInformationService;
  private final transient MessageSourceNotificationFactory notificationFactory;
  private final transient SampleValidationService sampleValidationService;
  private final transient SampleRegistrationServiceV2 sampleRegistrationServiceV2;
  private final transient AsyncProjectService asyncProjectService;
  private final transient UploadConfiguration uploadConfiguration;
  private final MessageSourceNotificationFactory messageFactory;
  private transient Context context;

  public SampleInformationMain(@Autowired ExperimentInformationService experimentInformationService,
      @Autowired DeletionService deletionService,
      @Autowired AsyncProjectService asyncProjectService,
      ProjectInformationService projectInformationService,
      MessageSourceNotificationFactory notificationFactory,
      SampleValidationService sampleValidationService,
      SampleRegistrationServiceV2 sampleRegistrationServiceV2,
      MessageSourceNotificationFactory messageSourceNotificationFactory,
      UploadConfiguration uploadConfiguration) {
    this.downloadComponent = new DownloadComponent();
    this.uploadConfiguration = uploadConfiguration;
    this.experimentInformationService = requireNonNull(experimentInformationService,
        "ExperimentInformationService cannot be null");
    this.deletionService = requireNonNull(deletionService,
        "DeletionService cannot be null");
    this.sampleDetailsComponent = new Div();
    this.projectInformationService = projectInformationService;
    this.notificationFactory = requireNonNull(notificationFactory,
        "messageSourceNotificationFactory must not be null");
    this.sampleValidationService = sampleValidationService;
    this.sampleRegistrationServiceV2 = sampleRegistrationServiceV2;

    this.asyncProjectService = requireNonNull(asyncProjectService);

    noGroupsDefinedDisclaimer = createNoGroupsDefinedDisclaimer();
    noGroupsDefinedDisclaimer.setVisible(false);

    noSamplesRegisteredDisclaimer = createNoSamplesRegisteredDisclaimer();
    noSamplesRegisteredDisclaimer.setVisible(false);

    add(noGroupsDefinedDisclaimer, noSamplesRegisteredDisclaimer);
    initContent();
    add(sampleDetailsComponent);
    add(downloadComponent);

    this.messageFactory = messageSourceNotificationFactory;
  }

  private static boolean noExperimentGroupsInExperiment(Experiment experiment) {
    return experiment.getExperimentalGroups().isEmpty();
  }

  private void initContent() {
    Span titleField = new Span();
    titleField.setText("Register sample batch");
    titleField.addClassNames("title");
    content.add(titleField);
    add(content);
    content.addClassName("sample-main-content");
  }

  private void onRegisterBatchClicked() {
    ProjectId projectId = context.projectId().orElseThrow();
    ExperimentId experimentId = context.experimentId().orElseThrow();

    Experiment experiment = experimentInformationService.find(projectId.value(), experimentId)
        .orElseThrow();

    if (experiment.getExperimentalGroups().isEmpty()) {
      return;
    }
    ProjectOverview projectOverview = projectInformationService.findOverview(projectId)
        .orElseThrow();
    RegisterSampleBatchDialog registerSampleBatchDialog = new RegisterSampleBatchDialog(
        asyncProjectService, messageFactory, experimentId.value(),
        projectId.value(), projectOverview.projectCode(),
        uploadConfiguration);
    UI ui = UI.getCurrent();
    registerSampleBatchDialog.addConfirmListener(event -> {
      var sampleMetadata = new ArrayList<>(event.validatedSampleMetadata());
      event.getSource().close();
      var pendingToast = notificationFactory.pendingTaskToast("task.in-progress",
          new Object[]{"Sample registration for %d samples".formatted(sampleMetadata.size())},
          getLocale());
      ui.access(pendingToast::open);

      CompletableFuture<Void> registrationTask = sampleRegistrationServiceV2
          .registerSamples(sampleMetadata, projectId,
              new ExperimentReference(experimentId.value()))
          .orTimeout(5, TimeUnit.MINUTES);
      try {
        registrationTask
            .exceptionally(e -> {
              ui.access(() -> {
                //this needs to come before all the success events
                pendingToast.close();
                notificationFactory.toast("task.failed",
                    new Object[]{"Sample registration"}, getLocale()).open();
              });
              throw new HandledException(e);
            })
            .thenRun(() -> ui.access(this::setBatchAndSampleInformation))
            .thenRun(() -> ui.access(() -> {
              pendingToast.close();
              displayRegistrationSuccess();
            }))
            .exceptionally(e -> {
              //we need to make sure we do not swallow exceptions but still stay in the exceptional state.
              throw new HandledException(e); //we need the future to complete exceptionally
            });
      } catch (HandledException e) {
        // we only log the exception as the user was presented with the error already and nothing we can do here.
        log.error(e.getMessage(), e);
      }
    });
    registerSampleBatchDialog.addCancelListener(
        event -> showCancelConfirmationDialog(event.getSource()));
    registerSampleBatchDialog.setEscAction(
        () -> showCancelConfirmationDialog(registerSampleBatchDialog));
    registerSampleBatchDialog.open();
  }

  private void showCancelConfirmationDialog(RegisterSampleBatchDialog dialog) {
    AlertDialog.alert(this)
        .warning()
        .title("Discard changes?")
        .message("By aborting the editing process and closing the dialog, you will lose all information entered.")
        .confirmButton("Discard changes", () -> dialog.close())
        .cancelButton("Keep editing", () -> {})
        .build()
        .open();
  }

  private Disclaimer createNoSamplesRegisteredDisclaimer() {
    Disclaimer noSamplesDefinedCard = Disclaimer.createWithTitle(
        "Manage your samples in one place",
        "Start your project by registering the first sample batch", "Register batch");
    noSamplesDefinedCard.addClassName("no-samples-registered-disclaimer");
    noSamplesDefinedCard.addDisclaimerConfirmedListener(
        event -> onRegisterBatchClicked());
    return noSamplesDefinedCard;
  }

  private Disclaimer createNoGroupsDefinedDisclaimer() {
    Disclaimer noGroupsDefindedDisclaimer = Disclaimer.createWithTitle(
        "Design your experiment first",
        "Start the sample registration process by defining experimental groups",
        "Add groups");
    noGroupsDefindedDisclaimer.addClassName("no-experimental-groups-registered-disclaimer");
    noGroupsDefindedDisclaimer.addDisclaimerConfirmedListener(this::onNoGroupsDefinedClicked);
    return noGroupsDefindedDisclaimer;
  }

  private void onNoGroupsDefinedClicked(DisclaimerConfirmedEvent event) {
    routeToExperimentalGroupCreation(event, context.experimentId().orElseThrow().value());
  }

  private void routeToExperimentalGroupCreation(ComponentEvent<?> componentEvent,
      String experimentId) {
    if (componentEvent.isFromClient()) {
      String routeToExperimentPage = String.format(ProjectRoutes.EXPERIMENT,
          context.projectId().orElseThrow().value(),
          experimentId);
      log.debug(String.format(
          "Rerouting to experiment page for experiment %s of project %s: %s",
          experimentId, context.projectId().orElseThrow().value(), routeToExperimentPage));
      componentEvent.getSource().getUI().ifPresent(ui -> ui.navigate(routeToExperimentPage));
    }
  }

  private void displayUpdateSuccess() {
    notificationFactory.toast("sample.updated.success", new String[]{}, getLocale())
        .open();
  }

  private void displayDeletionSuccess(int numberOfDeleted) {
    notificationFactory.toast("sample.deleted.success", new String[]{String.valueOf(numberOfDeleted)},
            getLocale())
        .open();
  }

  private void displayRegistrationSuccess() {
    notificationFactory.toast("sample.registered.success",
            new String[]{},
            getLocale())
        .open();

  }

  private void onEditSamplesClicked(SampleEditRequested editRequest) {
    ProjectId projectId = context.projectId().orElseThrow();
    ExperimentId experimentId = context.experimentId().orElseThrow();

    Experiment experiment = experimentInformationService.find(projectId.value(), experimentId)
        .orElseThrow();

    if (experiment.getExperimentalGroups().isEmpty()) {
      return;
    }
    ProjectOverview projectOverview = projectInformationService.findOverview(projectId)
        .orElseThrow();
    var sampleIds = editRequest.sampleIds().stream()
        .map(SampleId::value)
        .collect(Collectors.toSet());
    var editSampleBatchDialog = new EditSampleBatchDialog(
        asyncProjectService, messageFactory,
        sampleIds,
        experimentId.value(),
        projectId.value(),
        projectOverview.projectCode(),
        sampleValidationService,
        uploadConfiguration);
    UI ui = UI.getCurrent();
    editSampleBatchDialog.addConfirmListener(event -> {
      var sampleMetadata = new ArrayList<>(event.validatedSampleMetadata());
      event.getSource().close();
      var pendingToast = notificationFactory.pendingTaskToast("task.in-progress",
          new Object[]{"Sample update for %d samples".formatted(sampleMetadata.size())},
          getLocale());
      ui.access(pendingToast::open);

      CompletableFuture<Void> editTask = sampleRegistrationServiceV2.updateSamples(
              sampleMetadata,
              projectId,
              new ExperimentReference(context.experimentId().orElseThrow().value()))
          .orTimeout(5, TimeUnit.MINUTES);
      try {
        editTask
            .exceptionally(e -> {
              ui.access(() -> {
                //this needs to come before all the success events
                pendingToast.close();
                notificationFactory.toast("task.failed",
                        new String[]{"Sample update"}, getLocale())
                    .open();
              });
              throw new HandledException(e);
            })
            .thenRun(() -> ui.access(this::setBatchAndSampleInformation))
            .thenRun(() -> ui.access(() -> {
              pendingToast.close();
              displayUpdateSuccess();
            }))
            .exceptionally(e -> {
              //we need to make sure we do not swallow exceptions but still stay in the exceptional state.
              throw new HandledException(e); //we need the future to complete exceptionally
            });
      } catch (HandledException e) {
        // we only log the exception as the user was presented with the error already and nothing we can do here.
        log.error(e.getMessage(), e);
      }
    });
    editSampleBatchDialog.addCancelListener(
        event -> showCancelConfirmationDialog(event.getSource()));
    editSampleBatchDialog.setEscAction(
        () -> showCancelConfirmationDialog(editSampleBatchDialog));
    editSampleBatchDialog.open();
  }

  private void showCancelConfirmationDialog(EditSampleBatchDialog editBatchDialog) {
    AlertDialog.alert(this)
        .warning()
        .title("Discard changes?")
        .message("By aborting the editing process and closing the dialog, you will lose all information entered.")
        .confirmButton("Discard changes", () -> editBatchDialog.close())
        .cancelButton("Keep editing", () -> {})
        .build()
        .open();
  }

  private void deleteSamples(SampleDeletionRequested deletionRequest) {
    var projectId = context.projectId().orElseThrow();
    var pendingToast = notificationFactory.pendingTaskToast("task.in-progress",
        new Object[]{"Sample deletion for %d samples".formatted(deletionRequest.sampleIds().size())},
        getLocale());
    pendingToast.open();

    CompletableFuture<Void> deletionTask = deletionService.deleteSamplesAsync(projectId,
            deletionRequest.sampleIds())
        .orTimeout(5, TimeUnit.MINUTES);
    deletionTask
        .thenRun(() -> getUI().ifPresent(ui -> ui.access(() -> {
          pendingToast.close();
          displayDeletionSuccess(deletionRequest.sampleIds().size());
          setBatchAndSampleInformation();
        })))
        .exceptionally(e -> {
          log.error("Sample deletion failed", e);
          getUI().ifPresent(ui -> ui.access(() -> {
            pendingToast.close();
            notificationFactory.toast("task.failed",
                new Object[]{"Sample deletion"}, getLocale()).open();
          }));
          return null;
        });
  }

  private void onDeleteSamplesClicked(SampleDeletionRequested deletionRequest) {
    AlertDialog.danger(this,
        "Samples will be deleted",
        "Deleting these samples will also delete the data connected to them. Proceed?",
        "Delete samples",
        "Keep samples",
        () -> deleteSamples(deletionRequest)).open();
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

    this.context = context.withProjectCode(asyncProjectService.getProjectCode(projectID)
        .blockOptional()
        .map(ProjectCode::value)
        .orElse(null));

    // URL list-state synchronisation (USER-R-03, FEAT-PAG-LIST-02): capture the router handler,
    // install our own, and seed the list from the URL on direct load / reload / shared links.
    History history = UI.getCurrent().getPage().getHistory();
    History.HistoryStateChangeHandler currentHandler = history.getHistoryStateChangeHandler();
    if (currentHandler != listStateHistoryHandler) {
      routerHistoryStateChangeHandler = currentHandler;
    }
    history.setHistoryStateChangeHandler(listStateHistoryHandler);
    basePath = String.format(ProjectRoutes.SAMPLES, projectID, experimentId);

    // Building the component auto-loads the default list state and would mirror it into the URL;
    // suppress that initial write until the URL state has been applied so a reloaded or shared
    // link is restored exactly, not overwritten by the defaults.
    suppressUrlWrite = true;
    try {
      setBatchAndSampleInformation();
      if (sampleDetailsComponent instanceof SampleDetailsComponent sampleDetails) {
        ListState urlState = ListStateCodec.parse(event.getLocation().getQueryParameters(),
            SampleSort.DEFAULT, SampleSort.allowedSortOrders());
        sampleDetails.applyExternalState(urlState);
      }
    } finally {
      suppressUrlWrite = false;
    }
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
   * navigation). Non-sample locations are delegated back to the router handler.
   */
  private void onHistoryStateChange(HistoryStateChangeEvent event) {
    String expectedPath = currentSamplesPath();
    if (expectedPath == null || !expectedPath.equals(event.getLocation().getPath())) {
      if (routerHistoryStateChangeHandler != null) {
        routerHistoryStateChangeHandler.onHistoryStateChange(event);
      }
      return;
    }
    if (sampleDetailsComponent instanceof SampleDetailsComponent sampleDetails) {
      suppressUrlWrite = true;
      try {
        ListState urlState = ListStateCodec.parse(event.getLocation().getQueryParameters(),
            SampleSort.DEFAULT, SampleSort.allowedSortOrders());
        sampleDetails.applyExternalState(urlState);
      } finally {
        suppressUrlWrite = false;
      }
    }
  }

  private String currentSamplesPath() {
    if (context == null || context.projectId().isEmpty() || context.experimentId().isEmpty()) {
      return null;
    }
    return String.format(ProjectRoutes.SAMPLES,
        context.projectId().orElseThrow().value(),
        context.experimentId().orElseThrow().value());
  }

  private void writeUrl(ListState state) {
    if (basePath == null || suppressUrlWrite) {
      return;
    }
    UI ui = UI.getCurrent();
    if (ui == null) {
      return;
    }
    Location location = new Location(basePath, ListStateCodec.toQueryParameters(state));
    ui.getPage().getHistory().pushState(null, location);
  }

  private void setBatchAndSampleInformation() {
    var experiment = experimentInformationService.find(context.projectId().orElseThrow().value(),
        context.experimentId()
            .orElseThrow()).orElseThrow();
    if (noExperimentGroupsInExperiment(experiment)) {
      showRegisterGroupsDisclaimer();
      return;
    }
    if (noSamplesRegisteredInExperiment(experiment)) {
      showRegisterBatchDisclaimer();
    } else {
      reloadSampleInformation();
      showBatchAndSampleInformation();
    }
  }

  private boolean noSamplesRegisteredInExperiment(Experiment experiment) {
    var result = asyncProjectService
        .getSamples(context.projectId().orElseThrow().value(), experiment.experimentId().value())
        .blockFirst();
    return result == null;
  }

  private void showRegisterGroupsDisclaimer() {
    content.setVisible(false);
    sampleDetailsComponent.setVisible(false);
    noSamplesRegisteredDisclaimer.setVisible(false);
    noGroupsDefinedDisclaimer.setVisible(true);
  }

  private void showRegisterBatchDisclaimer() {
    content.setVisible(false);
    sampleDetailsComponent.setVisible(false);
    noGroupsDefinedDisclaimer.setVisible(false);
    noSamplesRegisteredDisclaimer.setVisible(true);
  }

  private void showBatchAndSampleInformation() {
    noSamplesRegisteredDisclaimer.setVisible(false);
    noGroupsDefinedDisclaimer.setVisible(false);
    content.setVisible(true);
    sampleDetailsComponent.setVisible(true);
  }

  private void reloadSampleInformation() {
    remove(sampleDetailsComponent);
    var sampleDetails = new SampleDetailsComponent(asyncProjectService, messageFactory, context);
    sampleDetails.addSampleRegistrationListener(ignored -> onRegisterBatchClicked());
    sampleDetails.addSampleEditListener(this::onEditSamplesClicked);
    sampleDetails.addSampleDeletionListener(this::onDeleteSamplesClicked);
    // Mirror the list state into the URL whenever a page is loaded (page/filter/sort change),
    // so the list is restorable and shareable (USER-R-03, FEAT-PAG-LIST-02).
    sampleDetails.addPageLoadedListener(event -> {
      if (sampleDetailsComponent instanceof SampleDetailsComponent component) {
        writeUrl(component.listState());
      }
    });
    sampleDetailsComponent = sampleDetails;
    add(sampleDetailsComponent);
  }

  private static class HandledException extends RuntimeException {

    public HandledException(Throwable cause) {
      super(cause);
    }
  }
}
