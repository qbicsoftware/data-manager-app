package life.qbic.datamanager.views.projects.project.samples;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.PermitAll;
import java.io.Serial;
import java.util.Objects;
import java.util.stream.Collectors;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.notifications.ErrorMessage;
import life.qbic.datamanager.views.notifications.StyledNotification;
import life.qbic.datamanager.views.projects.project.samples.BatchDetailsComponent.CreateBatchEvent;
import life.qbic.datamanager.views.projects.project.samples.BatchDetailsComponent.DeleteBatchEvent;
import life.qbic.datamanager.views.projects.project.samples.BatchDetailsComponent.EditBatchEvent;
import life.qbic.datamanager.views.projects.project.samples.BatchDetailsComponent.ViewBatchEvent;
import life.qbic.projectmanagement.application.DeletionService;
import life.qbic.projectmanagement.application.ExperimentInformationService;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.batch.BatchRegistrationService;
import life.qbic.projectmanagement.domain.model.batch.Batch;
import life.qbic.projectmanagement.domain.model.batch.BatchId;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.sample.Sample;
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
  private final transient DeletionService deletionService;
  private final transient SampleDetailsComponent sampleDetailsComponent;
  private final BatchDetailsComponent batchDetailsComponent;

  public SampleContentComponent(@Autowired ProjectInformationService projectInformationService,
      @Autowired ExperimentInformationService experimentInformationService,
      @Autowired BatchRegistrationService batchRegistrationService,
      @Autowired DeletionService deletionService,
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
    this.deletionService = deletionService;
    this.sampleDetailsComponent = sampleDetailsComponent;
    this.batchDetailsComponent = batchDetailsComponent;
    reloadOnBatchRegistration();
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

  private void reloadOnBatchRegistration() {
    sampleDetailsComponent.addBatchRegistrationListener(
        event -> displayComponentInContent(sampleDetailsComponent));
  }

  private void addBatchUpdateListeners() {
    batchDetailsComponent.addBatchCreationListener(this::registerBatch);
    batchDetailsComponent.addBatchDeletionListener(this::openBatchDeletionConfirmation);
    batchDetailsComponent.addBatchEditListener(this::editBatch);
    batchDetailsComponent.addBatchViewListener(this::viewBatch);
  }

  private void registerBatch(CreateBatchEvent createBatchEvent) {
    onRegisterButtonClicked();
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
    var result = batchRegistrationService.updateBatch(editBatchEvent.batchPreview().batchId(),
        editBatchEvent.batchPreview().batchLabel());
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

  private void onRegisterButtonClicked() {
    //ToDo Move Batch Registration from SampleDetailsComponent to here
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
}
