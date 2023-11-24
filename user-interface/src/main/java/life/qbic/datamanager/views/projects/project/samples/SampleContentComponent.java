package life.qbic.datamanager.views.projects.project.samples;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.PermitAll;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import life.qbic.application.commons.ApplicationException;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.notifications.ErrorMessage;
import life.qbic.datamanager.views.notifications.StyledNotification;
import life.qbic.datamanager.views.notifications.SuccessMessage;
import life.qbic.datamanager.views.projects.project.samples.BatchDetailsComponent.DeleteBatchEvent;
import life.qbic.datamanager.views.projects.project.samples.BatchDetailsComponent.EditBatchEvent;
import life.qbic.datamanager.views.projects.project.samples.BatchDetailsComponent.ViewBatchEvent;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.BatchRegistrationDialog;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.BatchRegistrationDialog.ConfirmEvent;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.BatchRegistrationDialog.SampleInfo;
import life.qbic.projectmanagement.application.DeletionService;
import life.qbic.projectmanagement.application.ExperimentInformationService;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.batch.BatchRegistrationService;
import life.qbic.projectmanagement.application.sample.SampleRegistrationService;
import life.qbic.projectmanagement.domain.model.batch.Batch;
import life.qbic.projectmanagement.domain.model.batch.BatchId;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.sample.Sample;
import life.qbic.projectmanagement.domain.model.sample.SampleOrigin;
import life.qbic.projectmanagement.domain.model.sample.SampleRegistrationRequest;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Sample Content component
 * <p>
 * The content component is a {@link Div} container, which is responsible for hosting the components
 * handling the content within the {@link SampleInformationMain}. It propagates the {@link Project}
 * and {@link Experiment} to the components within this container. Additionally, it propagates the
 * {@link Batch} and {@link Sample} information provided in the {@link SampleDetailsComponent} to
 * the {@link SampleInformationMain} and can be easily extended with additional components.
 */
@SpringComponent
@UIScope
@PermitAll
public class SampleContentComponent extends Div {

  @Serial
  private static final long serialVersionUID = -5431288053780884294L;
  private Context context;
  private final transient ProjectInformationService projectInformationService;
  private final transient ExperimentInformationService experimentInformationService;
  private final transient BatchRegistrationService batchRegistrationService;
  private final transient SampleRegistrationService sampleRegistrationService;
  private final transient DeletionService deletionService;
  private final transient SampleDetailsComponent sampleDetailsComponent;
  private final BatchDetailsComponent batchDetailsComponent;

  public SampleContentComponent(@Autowired ProjectInformationService projectInformationService,
      @Autowired ExperimentInformationService experimentInformationService,
      @Autowired BatchRegistrationService batchRegistrationService,
      @Autowired DeletionService deletionService,
      @Autowired SampleRegistrationService sampleRegistrationService,
      @Autowired SampleDetailsComponent sampleDetailsComponent,
      @Autowired BatchDetailsComponent batchDetailsComponent) {
    Objects.requireNonNull(projectInformationService);
    Objects.requireNonNull(experimentInformationService);
    Objects.requireNonNull(batchRegistrationService);
    Objects.requireNonNull(deletionService);
    Objects.requireNonNull(sampleDetailsComponent);
    Objects.requireNonNull(batchDetailsComponent);
    this.projectInformationService = projectInformationService;
    this.experimentInformationService = experimentInformationService;
    this.batchRegistrationService = batchRegistrationService;
    this.sampleRegistrationService = sampleRegistrationService;
    this.deletionService = deletionService;
    this.sampleDetailsComponent = sampleDetailsComponent;
    this.batchDetailsComponent = batchDetailsComponent;
    addBatchUpdateListeners();
  }

  /**
   * Provides the {@link Context} to the components within this container
   * <p>
   * This method serves as an entry point providing the necessary {@link ProjectId} to components
   * within this container
   *
   * @param context projectId of the selected project
   */
  public void setContext(Context context) {
    this.context = context;
    ProjectId projectId = context.projectId().orElseThrow();
    ExperimentId experimentId = context.experimentId().orElseThrow();
    batchDetailsComponent.setExperiment(
        experimentInformationService.find(experimentId).orElseThrow());
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

  private void addBatchUpdateListeners() {
    sampleDetailsComponent.addCreateBatchListener(event -> onRegisterButtonClicked());
    batchDetailsComponent.addBatchCreationListener(event -> onRegisterButtonClicked());
    batchDetailsComponent.addBatchDeletionListener(this::openBatchDeletionConfirmation);
    batchDetailsComponent.addBatchEditListener(this::editBatch);
    batchDetailsComponent.addBatchViewListener(this::viewBatch);
  }

  private void onRegisterButtonClicked() {
    Experiment experiment = context.experimentId()
        .flatMap(experimentInformationService::find)
        .orElseThrow();
    BatchRegistrationDialog dialog = new BatchRegistrationDialog(
        experiment.getName(), new ArrayList<>(experiment.getSpecies()),
        new ArrayList<>(experiment.getSpecimens()), new ArrayList<>(experiment.getAnalytes()),
        experiment.getExperimentalGroups());
    dialog.addCancelListener(cancelEvent -> cancelEvent.getSource().close());
    dialog.addConfirmListener(this::registerBatch);
    dialog.open();
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
            sample.getSampleLabel(),
            batchId,
            context.experimentId().orElseThrow(),
            sample.getExperimentalGroup().id(),
            sample.getBiologicalReplicate().id(),
            SampleOrigin.create(sample.getSpecies(), sample.getSpecimen(), sample.getAnalyte()),
            sample.getAnalysisToBePerformed(),
            sample.getCustomerComment()))
        .toList();
    return sampleRegistrationRequests;
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

  //Todo Determine how samples should be loaded and shown within batch
  private void viewBatch(ViewBatchEvent viewBatchEvent) {
    ConfirmDialog confirmDialog = new ConfirmDialog();
    confirmDialog.setText(String.format("This is where I'd show all of my %s Samples",
        viewBatchEvent.batchPreview().sampleCount()));
    confirmDialog.open();
    confirmDialog.addConfirmListener(event -> confirmDialog.close());
  }

  private void editBatch(EditBatchEvent editBatchEvent) {
    //ToDo the current design has no way to set the isPilot batch information?
    boolean isPilot = false;
    Collection<SampleRegistrationRequest> createdSamples = generateSampleRequestsFromSampleInfo(
        editBatchEvent.batchPreview()
            .batchId(), new ArrayList<>());
    Collection<Sample> editedSamples = new ArrayList<>();
    Collection<Sample> deletedSamples = new ArrayList<>();
    var result = batchRegistrationService.editBatch(editBatchEvent.batchPreview().batchId(),
        editBatchEvent.batchPreview().batchLabel(), isPilot, createdSamples, editedSamples,
        deletedSamples, context.projectId().orElseThrow());
    result.onValue(editedBatchId -> reload());
  }

  private void deleteBatch(BatchId batchId) {
    var result = deletionService.deleteBatch(batchId);
    result.onValue(deletedBatchId -> reload());
  }

  private void openBatchDeletionConfirmation(DeleteBatchEvent deleteBatchEvent) {
    BatchDeletionConfirmationNotification batchDeletionConfirmationNotification = new BatchDeletionConfirmationNotification(
        deleteBatchEvent.batchPreview().sampleCount());
    batchDeletionConfirmationNotification.open();
    batchDeletionConfirmationNotification.addConfirmListener(event -> {
      deleteBatch(deleteBatchEvent.batchPreview().batchId());
      batchDeletionConfirmationNotification.close();
    });
    batchDeletionConfirmationNotification.addCancelListener(
        event -> batchDeletionConfirmationNotification.close());
  }

  //ToDo replace with actual reload
  private void reload() {
    setContext(context);
  }

  private void displayProjectNotFound() {
    this.removeAll();
    ErrorMessage errorMessage = new ErrorMessage("Project not found",
        "Please try to reload the page");
    StyledNotification notification = new StyledNotification(errorMessage);
    notification.open();
  }

  public static class BatchRegisteredEvent extends ComponentEvent<SampleContentComponent> {

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public BatchRegisteredEvent(SampleContentComponent source, boolean fromClient) {
      super(source, fromClient);
    }
  }
}
