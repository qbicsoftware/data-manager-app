package life.qbic.datamanager.views.projects.project.samples;

import static java.util.Objects.requireNonNull;

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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import life.qbic.application.commons.ApplicationException;
import life.qbic.datamanager.files.export.FileNameFormatter;
import life.qbic.datamanager.files.export.download.WorkbookDownloadStreamProvider;
import life.qbic.datamanager.files.export.sample.TemplateService;
import life.qbic.datamanager.views.AppRoutes.ProjectRoutes;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.general.Disclaimer;
import life.qbic.datamanager.views.general.DisclaimerConfirmedEvent;
import life.qbic.datamanager.views.general.Main;
import life.qbic.datamanager.views.general.download.DownloadComponent;
import life.qbic.datamanager.views.notifications.CancelConfirmationDialogFactory;
import life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory;
import life.qbic.datamanager.views.projects.project.experiments.ExperimentMainLayout;
import life.qbic.datamanager.views.projects.project.samples.BatchDetailsComponent.DeleteBatchEvent;
import life.qbic.datamanager.views.projects.project.samples.BatchDetailsComponent.EditBatchEvent;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.EditSampleBatchDialog;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.RegisterSampleBatchDialog;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.DeletionService;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.ProjectOverview;
import life.qbic.projectmanagement.application.confounding.ConfoundingVariableService.ExperimentReference;
import life.qbic.projectmanagement.application.experiment.ExperimentInformationService;
import life.qbic.projectmanagement.application.sample.SampleInformationService;
import life.qbic.projectmanagement.application.sample.SampleRegistrationServiceV2;
import life.qbic.projectmanagement.application.sample.SampleValidationService;
import life.qbic.projectmanagement.domain.model.batch.BatchId;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.sample.Sample;
import org.apache.poi.ss.usermodel.Workbook;
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
public class SampleInformationMain extends Main implements BeforeEnterObserver {

  public static final String PROJECT_ID_ROUTE_PARAMETER = "projectId";
  public static final String EXPERIMENT_ID_ROUTE_PARAMETER = "experimentId";
  @Serial
  private static final long serialVersionUID = 3778218989387044758L;
  private static final Logger log = LoggerFactory.logger(SampleInformationMain.class);
  private final transient ExperimentInformationService experimentInformationService;
  private final transient SampleInformationService sampleInformationService;
  private final transient DeletionService deletionService;
  private final transient SampleDetailsComponent sampleDetailsComponent;
  private final BatchDetailsComponent batchDetailsComponent;

  private final DownloadComponent downloadComponent;

  private final Div content = new Div();
  private final TextField searchField = new TextField();
  private final Disclaimer noGroupsDefinedDisclaimer;
  private final Disclaimer noSamplesRegisteredDisclaimer;
  private final transient ProjectInformationService projectInformationService;
  private final transient CancelConfirmationDialogFactory cancelConfirmationDialogFactory;
  private final transient MessageSourceNotificationFactory notificationFactory;
  private final transient SampleValidationService sampleValidationService;
  private final transient TemplateService templateService;
  private final transient SampleRegistrationServiceV2 sampleRegistrationServiceV2;
  private transient Context context;

  public SampleInformationMain(@Autowired ExperimentInformationService experimentInformationService,
      @Autowired DeletionService deletionService,
      @Autowired SampleInformationService sampleInformationService,
      @Autowired SampleDetailsComponent sampleDetailsComponent,
      @Autowired BatchDetailsComponent batchDetailsComponent,
      ProjectInformationService projectInformationService,
      CancelConfirmationDialogFactory cancelConfirmationDialogFactory,
      MessageSourceNotificationFactory notificationFactory,
      SampleValidationService sampleValidationService,
      TemplateService templateService, SampleRegistrationServiceV2 sampleRegistrationServiceV2) {
    this.downloadComponent = new DownloadComponent();

    this.experimentInformationService = requireNonNull(experimentInformationService,
        "ExperimentInformationService cannot be null");
    this.sampleInformationService = requireNonNull(sampleInformationService,
        "SampleInformationService cannot be null");
    this.deletionService = requireNonNull(deletionService,
        "DeletionService cannot be null");
    this.sampleDetailsComponent = requireNonNull(sampleDetailsComponent,
        "SampleDetailsComponent cannot be null");
    this.batchDetailsComponent = requireNonNull(batchDetailsComponent,
        "BatchDetailsComponent cannot be null");
    this.projectInformationService = projectInformationService;
    this.cancelConfirmationDialogFactory = requireNonNull(cancelConfirmationDialogFactory,
        "cancelConfirmationDialogFactory must not be null");
    this.notificationFactory = requireNonNull(notificationFactory,
        "messageSourceNotificationFactory must not be null");
    this.sampleValidationService = sampleValidationService;
    this.sampleRegistrationServiceV2 = sampleRegistrationServiceV2;

    this.templateService = templateService;
    noGroupsDefinedDisclaimer = createNoGroupsDefinedDisclaimer();
    noGroupsDefinedDisclaimer.setVisible(false);

    noSamplesRegisteredDisclaimer = createNoSamplesRegisteredDisclaimer();
    noSamplesRegisteredDisclaimer.setVisible(false);

    add(noGroupsDefinedDisclaimer, noSamplesRegisteredDisclaimer);
    initContent();
    add(sampleDetailsComponent, batchDetailsComponent);

    batchDetailsComponent.addBatchCreationListener(ignored -> onRegisterBatchClicked());
    batchDetailsComponent.addBatchDeletionListener(this::onDeleteBatchClicked);
    batchDetailsComponent.addBatchEditListener(this::onEditBatchClicked);

    addClassName("sample");
    log.debug(String.format(
        "New instance for %s(#%s) created with %s(#%s) and %s(#%s)",
        this.getClass().getSimpleName(), System.identityHashCode(this),
        batchDetailsComponent.getClass().getSimpleName(),
        System.identityHashCode(batchDetailsComponent),
        sampleDetailsComponent.getClass().getSimpleName(),
        System.identityHashCode(sampleDetailsComponent)));
    add(downloadComponent);
  }

  private static boolean noExperimentGroupsInExperiment(Experiment experiment) {
    return experiment.getExperimentalGroups().isEmpty();
  }

  private void initContent() {
    Span titleField = new Span();
    titleField.setText("Register sample batch");
    titleField.addClassNames("title");
    content.add(titleField);
    initSearchFieldAndButtonBar();
    add(content);
    content.addClassName("sample-main-content");
  }

  private void initSearchFieldAndButtonBar() {
    searchField.setPlaceholder("Search");
    searchField.setClearButtonVisible(true);
    searchField.setSuffixComponent(VaadinIcon.SEARCH.create());
    searchField.addClassNames("search-field");
    searchField.setValueChangeMode(ValueChangeMode.LAZY);
    searchField.addValueChangeListener(
        event -> sampleDetailsComponent.onSearchFieldValueChanged((event.getValue())));
    Button metadataDownloadButton = new Button("Download sample metadata",
        event -> downloadSampleMetadata());
    Span buttonBar = new Span(metadataDownloadButton);
    buttonBar.addClassName("button-bar");
    Span buttonsAndSearch = new Span(searchField, buttonBar);
    buttonsAndSearch.addClassName("buttonAndField");
    content.add(buttonsAndSearch);
  }

  private void downloadSampleMetadata() {
    var projectId = context.projectId().orElseThrow();
    var experimentId = context.experimentId().orElseThrow();
    var projectCode = projectInformationService.findOverview(projectId)
        .map(ProjectOverview::projectCode).orElseThrow();
    var experimentName = experimentInformationService.find(projectId.value(), experimentId)
        .map(Experiment::getName).orElseThrow();
    downloadComponent.trigger(new WorkbookDownloadStreamProvider() {
      @Override
      public String getFilename() {
        return FileNameFormatter.formatWithTimestampedContext(LocalDate.now(), projectCode,
            experimentName,
            "sample information",
            "xlsx");
      }

      @Override
      public Workbook getWorkbook() {
        return templateService.sampleBatchInformationXLSXTemplate(projectId.value(),
            experimentId.value());
      }
    });
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
        sampleValidationService, templateService, experimentId.value(),
        projectId.value(), projectOverview.projectCode());
    UI ui = UI.getCurrent();
    registerSampleBatchDialog.addConfirmListener(event -> {
      var sampleMetadata = new ArrayList<>(event.validatedSampleMetadata());
      event.getSource().close();
      var pendingToast = notificationFactory.pendingTaskToast("task.in-progress",
          new Object[]{"Sample registration for batch %s".formatted(event.batchName())},
          getLocale());
      ui.access(pendingToast::open);

      CompletableFuture<Void> registrationTask = sampleRegistrationServiceV2.registerSamples(
              sampleMetadata,
              projectId, event.batchName(), false, new ExperimentReference(experimentId.value()))
          .orTimeout(5, TimeUnit.MINUTES);
      try {
        registrationTask
            .exceptionally(e -> {
              ui.access(() -> {
                //this needs to come before all the success events
                pendingToast.close();
                notificationFactory.toast("task.failed",
                    new Object[]{"Registration of batch '%s'".formatted(event.batchName())},
                    getLocale()).open();
              });
              throw new HandledException(e);
            })
            .thenRun(() -> ui.access(this::setBatchAndSampleInformation))
            .thenRun(() -> ui.access(() -> {
              pendingToast.close();
              displayRegistrationSuccess(event.batchName());
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
    cancelConfirmationDialogFactory.cancelConfirmationDialog(it -> dialog.close(),
            "sample-batch.register", getLocale())
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

  private void displayUpdateSuccess(String batchName) {
    notificationFactory.toast("sample-batch.updated.success", new String[]{batchName}, getLocale())
        .open();
  }

  private void displayDeletionSuccess(String batchLabel) {
    notificationFactory.toast("sample-batch.deleted.success", new String[]{batchLabel},
            getLocale())
        .open();
  }

  private void displayRegistrationSuccess(String batchLabel) {
    notificationFactory.toast("sample-batch.registered.success",
            new String[]{batchLabel},
            getLocale())
        .open();

  }

  private void displayUpdateFailure() {
    notificationFactory.dialog("sample-batch.update.failure",
            MessageSourceNotificationFactory.EMPTY_PARAMETERS,
            getLocale())
        .open();
  }

  private void onEditBatchClicked(EditBatchEvent editBatchEvent) {
    ProjectId projectId = context.projectId().orElseThrow();
    ExperimentId experimentId = context.experimentId().orElseThrow();

    Experiment experiment = experimentInformationService.find(projectId.value(), experimentId)
        .orElseThrow();

    if (experiment.getExperimentalGroups().isEmpty()) {
      return;
    }
    ProjectOverview projectOverview = projectInformationService.findOverview(projectId)
        .orElseThrow();
    BatchId batchId = editBatchEvent.batchPreview().batchId();
    String batchLabel = editBatchEvent.batchPreview().batchLabel();
    var editSampleBatchDialog = new EditSampleBatchDialog(
        sampleValidationService, templateService, batchId, batchLabel, experimentId.value(),
        projectId.value(), projectOverview.projectCode());
    UI ui = UI.getCurrent();
    editSampleBatchDialog.addConfirmListener(event -> {
      var sampleMetadata = new ArrayList<>(event.validatedSampleMetadata());
      event.getSource().close();
      var pendingToast = notificationFactory.pendingTaskToast("task.in-progress",
          new Object[]{"Update for batch %s".formatted(event.batchName())},
          getLocale());
      ui.access(pendingToast::open);

      CompletableFuture<Void> editTask = sampleRegistrationServiceV2.updateSamples(
              sampleMetadata,
              projectId,
              batchId,
              event.batchName(),
              false,
              new ExperimentReference(context.experimentId().orElseThrow().value()))
          .orTimeout(5, TimeUnit.MINUTES);
      try {
        editTask
            .exceptionally(e -> {
              ui.access(() -> {
                //this needs to come before all the success events
                pendingToast.close();
                notificationFactory.toast("task.failed",
                        new String[]{"Update of batch '%s'".formatted(event.batchName())}, getLocale())
                    .open();
              });
              throw new HandledException(e);
            })
            .thenRun(() -> ui.access(this::setBatchAndSampleInformation))
            .thenRun(() -> ui.access(() -> {
              pendingToast.close();
              displayUpdateSuccess(event.batchName());
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
    cancelConfirmationDialogFactory
        .cancelConfirmationDialog(
            it -> editBatchDialog.close(),
            "sample-batch.edit",
            getLocale())
        .open();
  }

  private void deleteBatch(DeleteBatchEvent deleteBatchEvent) {
    deletionService.deleteBatch(context.projectId().orElseThrow(),
        deleteBatchEvent.batchId());
    displayDeletionSuccess(deleteBatchEvent.batchLabel());
    setBatchAndSampleInformation();
  }

  private void onDeleteBatchClicked(DeleteBatchEvent deleteBatchEvent) {
    BatchDeletionConfirmationNotification batchDeletionConfirmationNotification = new BatchDeletionConfirmationNotification();
    batchDeletionConfirmationNotification.open();
    batchDeletionConfirmationNotification.addConfirmListener(event -> {
      deleteBatch(deleteBatchEvent);
      batchDeletionConfirmationNotification.close();
    });
    batchDeletionConfirmationNotification.addCancelListener(
        event -> batchDeletionConfirmationNotification.close());
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
    setBatchAndSampleInformation();
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
      reloadBatchInformation();
      reloadSampleInformation();
      showBatchAndSampleInformation();
    }
  }

  private boolean noSamplesRegisteredInExperiment(Experiment experiment) {
    return sampleInformationService.retrieveSamplesForExperiment(experiment.experimentId())
        .map(Collection::isEmpty)
        .onError(error -> {
          throw new ApplicationException("Unexpected response code : " + error);
        })
        .getValue();
  }

  private void showRegisterGroupsDisclaimer() {
    content.setVisible(false);
    sampleDetailsComponent.setVisible(false);
    batchDetailsComponent.setVisible(false);
    noSamplesRegisteredDisclaimer.setVisible(false);
    noGroupsDefinedDisclaimer.setVisible(true);
  }

  private void showRegisterBatchDisclaimer() {
    content.setVisible(false);
    sampleDetailsComponent.setVisible(false);
    batchDetailsComponent.setVisible(false);
    noGroupsDefinedDisclaimer.setVisible(false);
    noSamplesRegisteredDisclaimer.setVisible(true);
  }

  private void showBatchAndSampleInformation() {
    noSamplesRegisteredDisclaimer.setVisible(false);
    noGroupsDefinedDisclaimer.setVisible(false);
    content.setVisible(true);
    sampleDetailsComponent.setVisible(true);
    batchDetailsComponent.setVisible(true);
    searchField.setValue("");
  }

  private void reloadBatchInformation() {
    batchDetailsComponent.setContext(context);
  }

  private void reloadSampleInformation() {
    sampleDetailsComponent.setContext(context);
  }

  private static class HandledException extends RuntimeException {

    public HandledException(Throwable cause) {
      super(cause);
    }
  }
}
