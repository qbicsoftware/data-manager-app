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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import life.qbic.application.commons.ApplicationException;
import life.qbic.datamanager.download.DownloadProvider;
import life.qbic.datamanager.templates.TemplateService;
import life.qbic.datamanager.views.AppRoutes.Projects;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.general.Disclaimer;
import life.qbic.datamanager.views.general.DisclaimerConfirmedEvent;
import life.qbic.datamanager.views.general.Main;
import life.qbic.datamanager.views.notifications.CancelConfirmationDialogFactory;
import life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory;
import life.qbic.datamanager.views.notifications.StyledNotification;
import life.qbic.datamanager.views.notifications.SuccessMessage;
import life.qbic.datamanager.views.projects.project.experiments.ExperimentMainLayout;
import life.qbic.datamanager.views.projects.project.samples.BatchDetailsComponent.DeleteBatchEvent;
import life.qbic.datamanager.views.projects.project.samples.BatchDetailsComponent.EditBatchEvent;
import life.qbic.datamanager.views.projects.project.samples.download.SampleInformationXLSXProvider;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.BatchRegistrationDialog.ConfirmEvent;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.EditBatchDialog;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.RegisterSampleBatchDialog;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.SampleBatchInformationSpreadsheet;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.SampleBatchInformationSpreadsheet.SampleInfo;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.DeletionService;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.batch.BatchRegistrationService;
import life.qbic.projectmanagement.application.batch.SampleUpdateRequest;
import life.qbic.projectmanagement.application.batch.SampleUpdateRequest.SampleInformation;
import life.qbic.projectmanagement.application.experiment.ExperimentInformationService;
import life.qbic.projectmanagement.application.sample.SampleInformationService;
import life.qbic.projectmanagement.application.sample.SamplePreview;
import life.qbic.projectmanagement.application.sample.SampleRegistrationService;
import life.qbic.projectmanagement.application.sample.SampleRegistrationServiceV2;
import life.qbic.projectmanagement.application.sample.SampleValidationService;
import life.qbic.projectmanagement.domain.model.batch.BatchId;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalGroup;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.sample.Sample;
import life.qbic.projectmanagement.domain.model.sample.SampleId;
import life.qbic.projectmanagement.domain.model.sample.SampleOrigin;
import life.qbic.projectmanagement.domain.model.sample.SampleRegistrationRequest;
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
  private final transient BatchRegistrationService batchRegistrationService;
  private final transient SampleRegistrationService sampleRegistrationService;
  private final transient SampleInformationService sampleInformationService;
  private final transient DeletionService deletionService;
  private final transient SampleDetailsComponent sampleDetailsComponent;
  private final BatchDetailsComponent batchDetailsComponent;

  private final DownloadProvider metadataDownload;
  private final SampleInformationXLSXProvider sampleInformationXLSXProvider;

  private final Div content = new Div();
  private final TextField searchField = new TextField();
  private final Disclaimer noGroupsDefinedDisclaimer;
  private final Disclaimer noSamplesRegisteredDisclaimer;
  private final ProjectInformationService projectInformationService;
  private final CancelConfirmationDialogFactory cancelConfirmationDialogFactory;
  private final MessageSourceNotificationFactory messageSourceNotificationFactory;
  private final SampleValidationService sampleValidationService;
  private final TemplateService templateService;
  private final SampleRegistrationServiceV2 sampleRegistrationServiceV2;
  private transient Context context;

  public SampleInformationMain(@Autowired ExperimentInformationService experimentInformationService,
      @Autowired BatchRegistrationService batchRegistrationService,
      @Autowired DeletionService deletionService,
      @Autowired SampleRegistrationService sampleRegistrationService,
      @Autowired SampleInformationService sampleInformationService,
      @Autowired SampleDetailsComponent sampleDetailsComponent,
      @Autowired BatchDetailsComponent batchDetailsComponent,
      ProjectInformationService projectInformationService,
      CancelConfirmationDialogFactory cancelConfirmationDialogFactory,
      MessageSourceNotificationFactory messageSourceNotificationFactory,
      SampleValidationService sampleValidationService,
      TemplateService templateService, SampleRegistrationServiceV2 sampleRegistrationServiceV2) {
    this.experimentInformationService = requireNonNull(experimentInformationService,
        "ExperimentInformationService cannot be null");
    this.batchRegistrationService = requireNonNull(batchRegistrationService,
        "BatchRegistrationService cannot be null");
    this.sampleRegistrationService = requireNonNull(sampleRegistrationService,
        "SampleRegistrationService cannot be null");
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
    this.messageSourceNotificationFactory = requireNonNull(messageSourceNotificationFactory,
        "messageSourceNotificationFactory must not be null");
    this.sampleValidationService = sampleValidationService;
    this.templateService = templateService;
    noGroupsDefinedDisclaimer = createNoGroupsDefinedDisclaimer();
    noGroupsDefinedDisclaimer.setVisible(false);

    noSamplesRegisteredDisclaimer = createNoSamplesRegisteredDisclaimer();
    noSamplesRegisteredDisclaimer.setVisible(false);

    sampleInformationXLSXProvider = new SampleInformationXLSXProvider();
    metadataDownload = new DownloadProvider(sampleInformationXLSXProvider);

    add(noGroupsDefinedDisclaimer, noSamplesRegisteredDisclaimer);
    initContent();
    add(sampleDetailsComponent, batchDetailsComponent);
    add(metadataDownload);

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
    this.sampleRegistrationServiceV2 = sampleRegistrationServiceV2;
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
    Button metadataDownloadButton = new Button("Download Sample Metadata",
        event -> downloadSampleMetadata());
    Span buttonBar = new Span(metadataDownloadButton);
    buttonBar.addClassName("button-bar");
    Span buttonsAndSearch = new Span(searchField, buttonBar);
    buttonsAndSearch.addClassName("buttonAndField");
    content.add(buttonsAndSearch);
  }

  private void downloadSampleMetadata() {
    List<SamplePreview> samplePreviews = sampleInformationService.retrieveSamplePreviewsForExperiment(
        context.experimentId()
            .orElseThrow());

    Comparator<String> natOrder = Comparator.naturalOrder();

    var result = samplePreviews.stream()
        // sort by measurement codes first, then by sample codes
        .sorted(Comparator.comparing(SamplePreview::sampleCode, natOrder)
            .thenComparing(SamplePreview::sampleName, natOrder)).toList();
    sampleInformationXLSXProvider.setSamples(result,
        projectInformationService.find(context.projectId().orElseThrow()).orElseThrow()
            .getProjectCode().value());
    metadataDownload.trigger();
  }

  private void onRegisterBatchClicked() {
    Experiment experiment = context.experimentId()
        .flatMap(
            id -> experimentInformationService.find(context.projectId().orElseThrow().value(), id))
        .orElseThrow();
    if (experiment.getExperimentalGroups().isEmpty()) {
      return;
    }
    RegisterSampleBatchDialog registerSampleBatchDialog = new RegisterSampleBatchDialog(
        sampleValidationService, templateService,
        context.experimentId().map(ExperimentId::value).orElseThrow(),
        context.projectId().map(ProjectId::value).orElseThrow());
    registerSampleBatchDialog.addConfirmListener(event -> {
      event.getSource().taskInProgress("Register the sample batch metadata",
          "It may take about a minute for the registration task to complete.");
      UI ui = event.getSource().getUI().orElseThrow();
      CompletableFuture<Void> registrationTask = sampleRegistrationServiceV2.registerSamples(
              event.validatedSampleMetadata(),
              context.projectId()
                  .orElseThrow(), event.batchName(), event.isPilot())
          .orTimeout(5, TimeUnit.MINUTES);
      registrationTask
          .thenRun(() -> ui.access(() -> event.getSource().close()))
          .exceptionally(e -> null /*TODO show failed*/);
      event.getSource().close();
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

  private void registerBatch(ConfirmEvent confirmEvent) {
    String batchLabel = confirmEvent.getData().batchName();
    List<SampleInfo> samples = confirmEvent.getData().samples();
    List<SampleRegistrationRequest> sampleRegistrationRequests = batchRegistrationService.registerBatch(
            batchLabel, false,
            context.projectId().orElseThrow())
        .map(batchId -> generateSampleRequestsFromSampleInfo(batchId, samples))
        .onError(responseCode -> displayRegistrationFailure())
        .valueOrElseThrow(() ->
            new ApplicationException("Could not create sample registration requests"));
    sampleRegistrationService.registerSamples(sampleRegistrationRequests,
            context.projectId().orElseThrow())
        .onError(responseCode -> displayRegistrationFailure())
        .onValue(ignored -> fireEvent(new BatchRegisteredEvent(this, false)))
        .onValue(ignored -> confirmEvent.getSource().close())
        .onValue(batchId -> displayRegistrationSuccess(batchLabel))
        .onValue(ignored -> setBatchAndSampleInformation());
  }

  private List<SampleRegistrationRequest> generateSampleRequestsFromSampleInfo(BatchId batchId,
      List<SampleInfo> sampleInfos) {
    List<SampleRegistrationRequest> sampleRegistrationRequests;
    sampleRegistrationRequests = sampleInfos.stream()
        .map(sample -> new SampleRegistrationRequest(
            sample.getSampleName(), sample.getBiologicalReplicate(),
            batchId,
            context.experimentId().orElseThrow(),
            sample.getExperimentalGroup().id(),
            SampleOrigin.create(sample.getSpecies(), sample.getSpecimen(), sample.getAnalyte()),
            sample.getAnalysisToBePerformed(),
            sample.getCustomerComment()))
        .toList();
    return sampleRegistrationRequests;
  }

  private SampleUpdateRequest generateSampleUpdateRequestFromSampleInfo(
      SampleInfo sampleInfo) {
    return new SampleUpdateRequest(sampleInfo.getSampleId(), new SampleInformation(
        sampleInfo.getSampleName(), sampleInfo.getBiologicalReplicate(),
        sampleInfo.getAnalysisToBePerformed(),
        sampleInfo.getExperimentalGroup(),
        sampleInfo.getSpecies(), sampleInfo.getSpecimen(), sampleInfo.getAnalyte(),
        sampleInfo.getCustomerComment()));
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
      String routeToExperimentPage = String.format(Projects.EXPERIMENT,
          context.projectId().orElseThrow().value(),
          experimentId);
      log.debug(String.format(
          "Rerouting to experiment page for experiment %s of project %s: %s",
          experimentId, context.projectId().orElseThrow().value(), routeToExperimentPage));
      componentEvent.getSource().getUI().ifPresent(ui -> ui.navigate(routeToExperimentPage));
    }
  }

  private void displayUpdateSuccess() {
    SuccessMessage successMessage = new SuccessMessage("Batch update succeeded.", "");
    StyledNotification notification = new StyledNotification(successMessage);
    notification.open();
  }

  private void displayDeletionSuccess(String batchLabel) {
    messageSourceNotificationFactory.toast("sample-batch.deleted.success", new String[]{batchLabel},
            getLocale())
        .open();
  }

  private void displayRegistrationSuccess(String batchLabel) {
    messageSourceNotificationFactory.toast("sample-batch.registered.success",
            new String[]{batchLabel},
            getLocale())
        .open();

  }

  private void displayRegistrationFailure() {
    messageSourceNotificationFactory.dialog(
            "sample-batch.register.failure",
            MessageSourceNotificationFactory.EMPTY_PARAMETERS, getLocale())
        .open();
  }

  private void onEditBatchClicked(EditBatchEvent editBatchEvent) {
    Experiment experiment = context.experimentId()
        .flatMap(
            id -> experimentInformationService.find(context.projectId().orElseThrow().value(), id))
        .orElseThrow();
    List<Sample> samples = sampleInformationService.retrieveSamplesForBatch(
        editBatchEvent.batchPreview().batchId()).stream().toList();
    var experimentalGroups = experimentInformationService.experimentalGroupsFor(
        context.projectId().orElseThrow().value(),
        context.experimentId().orElseThrow());
    // need to create mutable list to order samples
    List<SampleBatchInformationSpreadsheet.SampleInfo> sampleInfos = new ArrayList<>(
        samples.stream()
            .map(sample -> convertSampleToSampleInfo(sample, experimentalGroups)).toList());
    sampleInfos.sort(Comparator.comparing(o -> o.getSampleCode().code()));
    EditBatchDialog editBatchDialog = new EditBatchDialog(experiment.getName(),
        experiment.getSpecies().stream().toList(), experiment.getSpecimens().stream().toList(),
        experiment.getAnalytes().stream().toList(), experiment.getExperimentalGroups(),
        editBatchEvent.batchPreview()
            .batchId(), editBatchEvent.batchPreview().batchLabel(), sampleInfos,
        this::isSampleRemovable);
    editBatchDialog.addCancelListener(
        cancelEvent -> showCancelConfirmationDialog(editBatchDialog));
    editBatchDialog.setEscAction(escEvent -> showCancelConfirmationDialog(editBatchDialog));
    editBatchDialog.addConfirmListener(this::editBatch);
    editBatchDialog.open();
  }

  private void showCancelConfirmationDialog(EditBatchDialog editBatchDialog) {
    cancelConfirmationDialogFactory
        .cancelConfirmationDialog(
            it2 -> editBatchDialog.close(),
            "sample-batch.edit",
            getLocale())
        .open();
  }

  private boolean isSampleRemovable(SampleId sampleId) {
    return deletionService.isSampleRemovable(sampleId);
  }

  private SampleBatchInformationSpreadsheet.SampleInfo convertSampleToSampleInfo(Sample sample,
      Collection<ExperimentalGroup> experimentalGroups) {
    ExperimentalGroup experimentalGroup = experimentalGroups.stream()
        .filter(expGrp -> expGrp.id() == sample.experimentalGroupId())
        .findFirst().orElseThrow();
    /*We currently allow replicates independent of experimental groups which is why we have to parse all replicates */
    return SampleBatchInformationSpreadsheet.SampleInfo.create(sample.sampleId(),
        sample.sampleCode(), sample.analysisMethod(),
        sample.label(), sample.biologicalReplicate(), experimentalGroup, sample.sampleOrigin()
            .getSpecies(), sample.sampleOrigin().getSpecimen(), sample.sampleOrigin().getAnalyte(),
        sample.comment().orElse(""));
  }

  private void editBatch(EditBatchDialog.ConfirmEvent confirmEvent) {
    boolean isPilot = false;
    Collection<SampleRegistrationRequest> createdSamples = generateSampleRequestsFromSampleInfo(
        confirmEvent.getData().batchId(), confirmEvent.getData().addedSamples());
    Collection<SampleUpdateRequest> editedSamples = confirmEvent.getData().changedSamples().stream()
        .map(this::generateSampleUpdateRequestFromSampleInfo).toList();
    Collection<SampleId> deletedSamples = confirmEvent.getData().removedSamples().stream()
        .map(SampleInfo::getSampleId).toList();
    var result = batchRegistrationService.editBatch(confirmEvent.getData().batchId(),
        confirmEvent.getData().batchName(), isPilot, createdSamples, editedSamples,
        deletedSamples, context.projectId().orElseThrow());
    result.onValue(ignored -> confirmEvent.getSource().close());
    result.onValue(batchId -> displayUpdateSuccess());
    result.onValue(ignored -> setBatchAndSampleInformation());
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

  public static class BatchRegisteredEvent extends ComponentEvent<SampleInformationMain> {

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public BatchRegisteredEvent(SampleInformationMain source, boolean fromClient) {
      super(source, fromClient);
    }
  }
}
