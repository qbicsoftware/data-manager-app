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
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.ApplicationException.ErrorCode;
import life.qbic.application.commons.ApplicationException.ErrorParameters;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.notifications.StyledNotification;
import life.qbic.datamanager.views.notifications.SuccessMessage;
import life.qbic.datamanager.views.projects.project.samples.BatchDetailsComponent.BatchPreview.ViewBatchEvent;
import life.qbic.datamanager.views.projects.project.samples.BatchDetailsComponent.DeleteBatchEvent;
import life.qbic.datamanager.views.projects.project.samples.BatchDetailsComponent.EditBatchEvent;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.BatchRegistrationDialog;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.BatchRegistrationDialog.ConfirmEvent;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.EditBatchDialog;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.EditBatchDialog.RemoveRowEvent;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.SampleBatchInformationSpreadsheet;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.SampleBatchInformationSpreadsheet.SampleInfo;
import life.qbic.projectmanagement.application.DeletionService;
import life.qbic.projectmanagement.application.ExperimentInformationService;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.batch.BatchRegistrationService;
import life.qbic.projectmanagement.application.batch.SampleUpdateRequest;
import life.qbic.projectmanagement.application.batch.SampleUpdateRequest.SampleInformation;
import life.qbic.projectmanagement.application.sample.SampleInformationService;
import life.qbic.projectmanagement.application.sample.SampleRegistrationService;
import life.qbic.projectmanagement.domain.model.batch.Batch;
import life.qbic.projectmanagement.domain.model.batch.BatchId;
import life.qbic.projectmanagement.domain.model.experiment.BiologicalReplicate;
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
  private final transient SampleInformationService sampleInformationService;
  private final transient DeletionService deletionService;
  private final transient SampleDetailsComponent sampleDetailsComponent;
  private final BatchDetailsComponent batchDetailsComponent;

  public SampleContentComponent(@Autowired ProjectInformationService projectInformationService,
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
    reloadOnBatchRegistration();
    sampleDetailsComponent.addCreateBatchListener(event -> onRegisterBatchClicked());
    batchDetailsComponent.addBatchCreationListener(ignored -> onRegisterBatchClicked());
    batchDetailsComponent.addBatchDeletionListener(this::onDeleteBatchClicked);
    batchDetailsComponent.addBatchEditListener(this::onEditBatchClicked);
    batchDetailsComponent.addBatchViewListener(this::onViewBatchClicked);
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
            }, () -> {
              throw new ApplicationException(ErrorCode.INVALID_PROJECT_CODE,
                  ErrorParameters.of(projectId.value()));
            });
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
        .flatMap(experimentInformationService::find)
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
        .valueOrElseThrow(error -> new ApplicationException(
            "Could not create sample registration requests. Response code: " + error));
    sampleRegistrationService.registerSamples(
            sampleRegistrationRequests,
            context.projectId().orElseThrow())
        .onValue(ignored -> fireEvent(new BatchRegisteredEvent(this, false)))
        .onValue(batchId -> displayRegistrationSuccess())
        .onValue(ignored -> reload())
        .onValue(ignored -> confirmEvent.getSource().close())
        .onError(error -> {
          confirmEvent.getSource().close();
          throw new ApplicationException(
              "Could not register samples. Response code: " + error);
        });
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

  private SampleUpdateRequest generateSampleUpdateRequestFromSampleInfo(
      SampleInfo sampleInfo) {
    return new SampleUpdateRequest(sampleInfo.getSampleId(), new SampleInformation(
        sampleInfo.getSampleLabel(), sampleInfo.getAnalysisToBePerformed(),
        sampleInfo.getBiologicalReplicate(), sampleInfo.getExperimentalGroup(),
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

  private void onViewBatchClicked(ViewBatchEvent viewBatchEvent) {
    ConfirmDialog confirmDialog = new ConfirmDialog();
    confirmDialog.setText(("This is where I'd show all of my Samples"));
    confirmDialog.open();
    confirmDialog.addConfirmListener(event -> confirmDialog.close());
  }

  private void onEditBatchClicked(EditBatchEvent editBatchEvent) {
    Experiment experiment = context.experimentId()
        .flatMap(experimentInformationService::find)
        .orElseThrow();
    List<Sample> samples = sampleInformationService.retrieveSamplesForBatch(
        editBatchEvent.batchPreview().batchId()).stream().toList();
    var experimentalGroups = experimentInformationService.experimentalGroupsFor(
        context.experimentId().orElseThrow());

    // need to create mutable list to order samples
    List<SampleBatchInformationSpreadsheet.SampleInfo> sampleInfos = new ArrayList<>(samples.stream()
        .map(sample -> convertSampleToSampleInfo(sample, experimentalGroups)).toList());
    sampleInfos.sort(Comparator.comparing(o -> o.getSampleCode().code()));

    EditBatchDialog editBatchDialog = new EditBatchDialog(experiment.getName(),
        experiment.getSpecies().stream().toList(), experiment.getSpecimens().stream().toList(),
        experiment.getAnalytes().stream().toList(), experiment.getExperimentalGroups(),
        editBatchEvent.batchPreview()
            .batchId(), editBatchEvent.batchPreview().batchLabel(), sampleInfos);
    editBatchDialog.addCancelListener(cancelEvent -> cancelEvent.getSource().close());
    editBatchDialog.addConfirmListener(this::editBatch);
    editBatchDialog.addRemoveRowListener(this::removeRowFromBatch);
    editBatchDialog.open();
  }

  private SampleBatchInformationSpreadsheet.SampleInfo convertSampleToSampleInfo(Sample sample,
      Collection<ExperimentalGroup> experimentalGroups) {
    ExperimentalGroup experimentalGroup = experimentalGroups.stream()
        .filter(expGrp -> expGrp.id() == sample.experimentalGroupId())
        .findFirst().orElseThrow();
    /*We currently allow replicates independent of experimental groups which is why we have to parse all replicates */
    BiologicalReplicate biologicalReplicate = experimentalGroups.stream()
        .map(ExperimentalGroup::biologicalReplicates).flatMap(Collection::stream).filter(
            biologicalReplicate1 -> biologicalReplicate1.id()
                .equals(sample.biologicalReplicateId())).findFirst().orElseThrow();
    return SampleBatchInformationSpreadsheet.SampleInfo.create(sample.sampleId(),
        sample.sampleCode(), sample.analysisMethod(),
        sample.label(),
        biologicalReplicate, experimentalGroup, sample.sampleOrigin()
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
    result.onValue(ignored -> reload())
        .onValue(ignored -> confirmEvent.getSource().close())
        .onValue(batchId -> displayUpdateSuccess());
    result.onError(error -> {
      confirmEvent.getSource().close();
      throw new ApplicationException("error code: " + error);
    });
  }

  private void removeRowFromBatch(EditBatchDialog.RemoveRowEvent removeRowEvent) {
    EditBatchDialog dialog = removeRowEvent.getSource();
    if(deletionService.isSampleRemovable(removeRowEvent.getSampleInfo().getSampleId(),
        context.projectId().orElseThrow())) {
      dialog.removeRow();
    } else {
      dialog.displayRemoveRowError();
    }
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
