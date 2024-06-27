package life.qbic.datamanager.views.projects.project.samples;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
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
import java.util.Objects;
import java.util.stream.Collectors;
import life.qbic.application.commons.ApplicationException;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.general.Main;
import life.qbic.datamanager.views.notifications.ErrorMessage;
import life.qbic.datamanager.views.notifications.StyledNotification;
import life.qbic.datamanager.views.notifications.SuccessMessage;
import life.qbic.datamanager.views.projects.project.experiments.ExperimentMainLayout;
import life.qbic.datamanager.views.projects.project.samples.BatchDetailsComponent.DeleteBatchEvent;
import life.qbic.datamanager.views.projects.project.samples.BatchDetailsComponent.EditBatchEvent;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.BatchRegistrationDialog;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.BatchRegistrationDialog.ConfirmEvent;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.EditBatchDialog;
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
import life.qbic.projectmanagement.application.sample.SampleRegistrationService;
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
  private final transient ProjectInformationService projectInformationService;
  private final transient ExperimentInformationService experimentInformationService;
  private final transient BatchRegistrationService batchRegistrationService;
  private final transient SampleRegistrationService sampleRegistrationService;
  private final transient SampleInformationService sampleInformationService;
  private final transient DeletionService deletionService;
  private final transient SampleDetailsComponent sampleDetailsComponent;
  private final BatchDetailsComponent batchDetailsComponent;
  private transient Context context;

  public SampleInformationMain(@Autowired ProjectInformationService projectInformationService,
      @Autowired ExperimentInformationService experimentInformationService,
      @Autowired BatchRegistrationService batchRegistrationService,
      @Autowired DeletionService deletionService,
      @Autowired SampleRegistrationService sampleRegistrationService,
      @Autowired SampleInformationService sampleInformationService,
      @Autowired SampleDetailsComponent sampleDetailsComponent,
      @Autowired BatchDetailsComponent batchDetailsComponent) {
    Objects.requireNonNull(projectInformationService);
    Objects.requireNonNull(experimentInformationService);
    Objects.requireNonNull(batchRegistrationService);
    Objects.requireNonNull(sampleRegistrationService);
    Objects.requireNonNull(sampleInformationService);
    Objects.requireNonNull(deletionService);
    Objects.requireNonNull(sampleDetailsComponent);
    Objects.requireNonNull(batchDetailsComponent);
    this.projectInformationService = projectInformationService;
    this.experimentInformationService = experimentInformationService;
    this.batchRegistrationService = batchRegistrationService;
    this.sampleRegistrationService = sampleRegistrationService;
    this.sampleInformationService = sampleInformationService;
    this.deletionService = deletionService;
    this.sampleDetailsComponent = sampleDetailsComponent;
    this.batchDetailsComponent = batchDetailsComponent;
    addClassName("sample");
    reloadOnBatchRegistration();
    sampleDetailsComponent.addCreateBatchListener(event -> onRegisterBatchClicked());
    batchDetailsComponent.addBatchCreationListener(ignored -> onRegisterBatchClicked());
    batchDetailsComponent.addBatchDeletionListener(this::onDeleteBatchClicked);
    batchDetailsComponent.addBatchEditListener(this::onEditBatchClicked);
    log.debug(String.format(
        "New instance for %s(#%s) created with %s(#%s) and %s(#%s)",
        this.getClass().getSimpleName(), System.identityHashCode(this),
        batchDetailsComponent.getClass().getSimpleName(),
        System.identityHashCode(batchDetailsComponent),
        sampleDetailsComponent.getClass().getSimpleName(),
        System.identityHashCode(sampleDetailsComponent)));
  }

  /**
   * Provides the {@link Context} to the components within this page
   * <p>
   * This method serves as an entry point providing the necessary {@link Context} to the components
   * within this cage
   *
   * @param context Context containing the projectId of the selected project
   */
  public void setContext(Context context) {
    this.context = context;
    ProjectId projectId = context.projectId().orElseThrow();
    batchDetailsComponent.setContext(context);
    projectInformationService.find(projectId)
        .ifPresentOrElse(
            project -> {
              sampleDetailsComponent.setContext(context);
              displayComponentInContent(batchDetailsComponent);
              displayComponentInContent(sampleDetailsComponent);
            }, this::displayProjectNotFound);
  }

  private boolean isComponentInContent(Component component) {
    return this.getChildren().collect(Collectors.toSet()).contains(component);
  }

  private void displayComponentInContent(Component component) {
    if (!isComponentInContent(component)) {
      this.add(component);
    }
  }

  private void reloadOnBatchRegistration() {
    sampleDetailsComponent.addCreateBatchListener(
        event -> displayComponentInContent(sampleDetailsComponent));
  }

  private void onRegisterBatchClicked() {
    Experiment experiment = context.experimentId()
        .flatMap(
            id -> experimentInformationService.find(context.projectId().orElseThrow().value(), id))
        .orElseThrow();
    if (experiment.getExperimentalGroups().isEmpty()) {
      return;
    }
    BatchRegistrationDialog dialog = new BatchRegistrationDialog(
        experiment.getName(), new ArrayList<>(experiment.getSpecies()),
        new ArrayList<>(experiment.getSpecimens()), new ArrayList<>(experiment.getAnalytes()),
        experiment.getExperimentalGroups());
    dialog.addCancelListener(cancelEvent -> cancelEvent.getSource().close());
    dialog.addConfirmListener(this::registerBatch);
    dialog.open();
  }

  private void reload() {
    setContext(context);
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
        .onValue(batchId -> displayRegistrationSuccess())
        .onValue(ignored -> reload());
  }

  private List<SampleRegistrationRequest> generateSampleRequestsFromSampleInfo(BatchId batchId,
      List<SampleInfo> sampleInfos) {
    List<SampleRegistrationRequest> sampleRegistrationRequests;
    sampleRegistrationRequests = sampleInfos.stream()
        .map(sample -> new SampleRegistrationRequest(
            sample.getSampleLabel(), sample.getOrganismId(),
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
        sampleInfo.getSampleLabel(), sampleInfo.getOrganismId(),
        sampleInfo.getAnalysisToBePerformed(),
        sampleInfo.getExperimentalGroup(),
        sampleInfo.getSpecies(), sampleInfo.getSpecimen(), sampleInfo.getAnalyte(),
        sampleInfo.getCustomerComment()));
  }

  private void displayUpdateSuccess() {
    SuccessMessage successMessage = new SuccessMessage("Batch update succeeded.", "");
    StyledNotification notification = new StyledNotification(successMessage);
    notification.open();
  }

  private void displayDeletionSuccess() {
    SuccessMessage successMessage = new SuccessMessage("Batch deletion succeeded.", "");
    StyledNotification notification = new StyledNotification(successMessage);
    notification.open();
  }


  private void displayRegistrationSuccess() {
    SuccessMessage successMessage = new SuccessMessage("Batch registration succeeded.", "");
    StyledNotification notification = new StyledNotification(successMessage);
    notification.open();
  }

  private void displayRegistrationFailure() {
    ErrorMessage errorMessage = new ErrorMessage("Batch registration failed.", "");
    StyledNotification notification = new StyledNotification(errorMessage);
    notification.open();
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
        context.experimentId().get());
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
    editBatchDialog.addCancelListener(cancelEvent -> cancelEvent.getSource().close());
    editBatchDialog.addConfirmListener(this::editBatch);
    editBatchDialog.open();
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
        sample.label(), sample.organismId(), experimentalGroup, sample.sampleOrigin()
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
    result.onValue(ignored -> reload());
  }

  private void deleteBatch(DeleteBatchEvent deleteBatchEvent) {
    deletionService.deleteBatch(context.projectId().orElseThrow(),
        deleteBatchEvent.batchId());
    displayDeletionSuccess();
    reload();
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

  private void displayProjectNotFound() {
    this.removeAll();
    ErrorMessage errorMessage = new ErrorMessage("Project not found",
        "Please try to reload the page");
    StyledNotification notification = new StyledNotification(errorMessage);
    notification.open();
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
    setContext(context);
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
