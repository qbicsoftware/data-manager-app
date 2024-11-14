package life.qbic.datamanager.views.projects.project.info;

import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParam;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.PermitAll;
import java.io.Serial;
import java.util.List;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.Result;
import life.qbic.datamanager.security.UserPermissions;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.general.Main;
import life.qbic.datamanager.views.general.download.OfferDownload;
import life.qbic.datamanager.views.general.download.QualityControlDownload;
import life.qbic.datamanager.views.notifications.CancelConfirmationDialogFactory;
import life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory;
import life.qbic.datamanager.views.notifications.Toast;
import life.qbic.datamanager.views.projects.project.ProjectMainLayout;
import life.qbic.datamanager.views.projects.project.experiments.ExperimentInformationMain;
import life.qbic.datamanager.views.projects.project.experiments.ExperimentListComponent;
import life.qbic.datamanager.views.projects.project.experiments.ExperimentListComponent.AddExperimentClickEvent;
import life.qbic.datamanager.views.projects.project.experiments.ExperimentListComponent.ExperimentSelectionEvent;
import life.qbic.datamanager.views.projects.project.experiments.experiment.create.AddExperimentDialog;
import life.qbic.datamanager.views.projects.project.experiments.experiment.create.AddExperimentDialog.ExperimentAddEvent;
import life.qbic.datamanager.views.projects.project.experiments.experiment.create.AddExperimentDialog.ExperimentDraft;
import life.qbic.datamanager.views.projects.project.info.OfferListComponent.DeleteOfferClickEvent;
import life.qbic.datamanager.views.projects.project.info.OfferListComponent.DownloadOfferClickEvent;
import life.qbic.datamanager.views.projects.project.info.OfferListComponent.OfferInfo;
import life.qbic.datamanager.views.projects.project.info.OfferListComponent.UploadOfferClickEvent;
import life.qbic.datamanager.views.projects.project.info.QualityControlListComponent.DeleteQualityControlEvent;
import life.qbic.datamanager.views.projects.project.info.QualityControlListComponent.DownloadQualityControlEvent;
import life.qbic.datamanager.views.projects.project.info.QualityControlListComponent.QualityControl;
import life.qbic.datamanager.views.projects.purchase.PurchaseItemDeletionConfirmationNotification;
import life.qbic.datamanager.views.projects.purchase.UploadPurchaseDialog;
import life.qbic.datamanager.views.projects.qualityControl.QCItemDeletionConfirmationNotification;
import life.qbic.datamanager.views.projects.qualityControl.UploadQualityControlDialog;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.AddExperimentToProjectService;
import life.qbic.projectmanagement.application.experiment.ExperimentInformationService;
import life.qbic.projectmanagement.application.ontology.SpeciesLookupService;
import life.qbic.projectmanagement.application.ontology.TerminologyService;
import life.qbic.projectmanagement.application.purchase.OfferDTO;
import life.qbic.projectmanagement.application.purchase.ProjectPurchaseService;
import life.qbic.projectmanagement.application.sample.qualitycontrol.QualityControlReport;
import life.qbic.projectmanagement.application.sample.qualitycontrol.QualityControlService;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.sample.qualitycontrol.QualityControlUpload;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * Project Information Main Component
 * <p>
 * This component hosts the components necessary to show and update the {@link Project} information
 * via the provided {@link ProjectId} in the URL
 */

@SpringComponent
@UIScope
@Route(value = "projects/:projectId?/info", layout = ProjectMainLayout.class)
@PermitAll
public class ProjectInformationMain extends Main implements BeforeEnterObserver {

  public static final String PROJECT_ID_ROUTE_PARAMETER = "projectId";
  public static final String EXPERIMENT_ID_ROUTE_PARAMETER = "experimentId";
  @Serial
  private static final long serialVersionUID = 5797835576569148873L;
  private static final Logger log = logger(ProjectInformationMain.class);
  private final transient AddExperimentToProjectService addExperimentToProjectService;
  private final transient ExperimentInformationService experimentInformationService;
  private final transient SpeciesLookupService ontologyTermInformationService;
  private final transient ProjectPurchaseService projectPurchaseService;
  private final transient QualityControlService qualityControlService;
  private final transient UserPermissions userPermissions;
  private final ProjectSummaryComponent projectSummaryComponent;
  private final ExperimentListComponent experimentListComponent;
  private final OfferDownload offerDownload;
  private final QualityControlDownload qualityControlDownload;
  private final OfferListComponent offerListComponent;
  private final QualityControlListComponent qualityControlListComponent;
  private final CancelConfirmationDialogFactory cancelConfirmationDialogFactory;
  private final MessageSourceNotificationFactory messageSourceNotificationFactory;
  private final TerminologyService terminologyService;
  private Context context;

  public ProjectInformationMain(@Autowired ProjectSummaryComponent projectSummaryComponent,
      @Autowired ExperimentListComponent experimentListComponent,
      @Autowired UserPermissions userPermissions,
      @Autowired AddExperimentToProjectService addExperimentToProjectService,
      @Autowired SpeciesLookupService ontologyTermInformationService,
      @Autowired ExperimentInformationService experimentInformationService,
      @Autowired ProjectPurchaseService projectPurchaseService,
      @Autowired QualityControlService qualityControlService,
      @Autowired TerminologyService terminologyService,
      CancelConfirmationDialogFactory cancelConfirmationDialogFactory,
      MessageSourceNotificationFactory messageSourceNotificationFactory) {
    this.projectSummaryComponent = requireNonNull(projectSummaryComponent);
    this.experimentListComponent = requireNonNull(experimentListComponent,
        "experimentListComponent must not be null");
    this.userPermissions = requireNonNull(userPermissions, "userPermissions must not be null");
    this.addExperimentToProjectService = requireNonNull(addExperimentToProjectService,
        "addExperimentToProjectService must not be null");
    this.ontologyTermInformationService = requireNonNull(ontologyTermInformationService,
        "ontologyTermInformationService must not be null");
    this.experimentInformationService = requireNonNull(experimentInformationService,
        "experimentInformationService must not be null");
    this.projectPurchaseService = requireNonNull(projectPurchaseService,
        "projectPurchaseService must not be null");
    this.qualityControlService = requireNonNull(qualityControlService,
        "qualityControlService must not be null");
    this.messageSourceNotificationFactory = requireNonNull(messageSourceNotificationFactory,
        "messageSourceNotificationFactory must not be null");
    this.cancelConfirmationDialogFactory = requireNonNull(cancelConfirmationDialogFactory,
        "cancelConfirmationDialogFactory must not be null");

    offerListComponent = getConfiguredOfferList();
    qualityControlListComponent = getConfiguredQualityControlList();
    offerDownload = new OfferDownload(
        (projectId, offerId) -> projectPurchaseService.getOfferWithContent(projectId, offerId)
            .orElseThrow());

    qualityControlDownload = new QualityControlDownload(
        (projectId, qualityControlId) -> qualityControlService.getQualityControlWithContent(
                projectId, qualityControlId)
            .orElseThrow());

    this.experimentListComponent.addExperimentSelectionListener(this::onExperimentSelectionEvent);
    this.experimentListComponent.addAddButtonListener(this::onAddExperimentClicked);
    addClassName("project");
    add(this.projectSummaryComponent, offerListComponent, offerDownload, experimentListComponent,
        qualityControlListComponent, qualityControlDownload);
    this.terminologyService = terminologyService;
  }

  private static void refreshOffers(ProjectPurchaseService projectPurchaseService, String projectId,
      OfferListComponent offerListComponent) {
    List<OfferInfo> offers = projectPurchaseService.linkedOffers(projectId)
        .stream()
        .map(offer -> new OfferInfo(offer.id(), offer.getFileName(), offer.isSigned()))
        .toList();
    offerListComponent.setOffers(offers);
  }

  /**
   * Extracts {@link ProjectId} from the provided URL before the user accesses the page
   * <p>
   * This method is responsible for checking if the provided {@link ProjectId} is valid and
   * triggering its propagation to the components within the {@link ProjectInformationMain}
   */

  @Override
  public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
    String projectID = beforeEnterEvent.getRouteParameters().get(PROJECT_ID_ROUTE_PARAMETER)
        .orElseThrow();
    if (!ProjectId.isValid(projectID)) {
      throw new ApplicationException("invalid project id " + projectID);
    }
    ProjectId parsedProjectId = ProjectId.parse(projectID);
    if (userPermissions.readProject(parsedProjectId)) {
      this.context = new Context().with(parsedProjectId);
      setContext(context);
    } else {
      beforeEnterEvent.rerouteToError(NotFoundException.class);
    }
  }

  private OfferListComponent getConfiguredOfferList() {
    OfferListComponent component = new OfferListComponent();
    component.addDeleteOfferClickListener(this::onDeleteOfferClicked);
    component.addDownloadOfferClickListener(this::onDownloadOfferClicked);
    component.addUploadOfferClickListener(
        event -> onUploadOfferClicked(event, projectPurchaseService,
            context.projectId().orElseThrow().value()));
    return component;
  }

  private void onDownloadOfferClicked(DownloadOfferClickEvent downloadOfferClickEvent) {
    offerDownload.trigger(context.projectId().orElseThrow().value(),
        downloadOfferClickEvent.offerId());
  }

  private void onDeleteOfferClicked(DeleteOfferClickEvent deleteOfferClickEvent) {
    PurchaseItemDeletionConfirmationNotification purchaseItemDeletionConfirmationNotification = new PurchaseItemDeletionConfirmationNotification();
    purchaseItemDeletionConfirmationNotification.open();
    purchaseItemDeletionConfirmationNotification.addConfirmListener(event -> {
      projectPurchaseService.deleteOffer(context.projectId().orElseThrow().value(),
          deleteOfferClickEvent.offerId());
      deleteOfferClickEvent.getSource().remove(deleteOfferClickEvent.offerId());
      offerDownload.removeHref();
      purchaseItemDeletionConfirmationNotification.close();
    });
    purchaseItemDeletionConfirmationNotification.addCancelListener(
        event -> purchaseItemDeletionConfirmationNotification.close());
  }

  private void onUploadOfferClicked(UploadOfferClickEvent uploadOfferClickEvent,
      ProjectPurchaseService projectPurchaseService,
      String projectId) {
    UploadPurchaseDialog dialog = new UploadPurchaseDialog();
    dialog.addConfirmListener(confirmEvent -> {
      List<OfferDTO> offerDTOs = confirmEvent.getSource().purchaseItems().stream()
          .map(it -> new OfferDTO(it.signed(), it.fileName(), it.content()))
          .toList();
      projectPurchaseService.addPurchases(projectId, offerDTOs);
      refreshOffers(projectPurchaseService, projectId, uploadOfferClickEvent.getSource());
      confirmEvent.getSource().close();
    });
    dialog.addCancelListener(cancelEvent -> cancelEvent.getSource().close());
    dialog.open();
  }

  private QualityControlListComponent getConfiguredQualityControlList() {
    QualityControlListComponent component = new QualityControlListComponent();
    component.addDeleteQualityControlListener(this::onDeleteQualityControlClicked);
    component.addDownloadQualityControlListener(this::onDownloadQualityControlClicked);
    component.addUploadQualityControlListener(
        event -> onUploadQualityControlClicked());
    return component;
  }

  private void onDownloadQualityControlClicked(
      DownloadQualityControlEvent downloadQualityControlEvent) {
    qualityControlDownload.trigger(context.projectId().orElseThrow().value(),
        downloadQualityControlEvent.qualityControlId());
  }

  private void onDeleteQualityControlClicked(DeleteQualityControlEvent deleteQualityControlEvent) {
    QCItemDeletionConfirmationNotification qcItemDeletionConfirmationNotification = new QCItemDeletionConfirmationNotification();
    qcItemDeletionConfirmationNotification.open();
    qcItemDeletionConfirmationNotification.addConfirmListener(event -> {
      qualityControlService.deleteQualityControl(context.projectId().orElseThrow().value(),
          deleteQualityControlEvent.qualityControlId());
      qualityControlDownload.removeHref();
      refreshQualityControls();
      qcItemDeletionConfirmationNotification.close();
    });
    qcItemDeletionConfirmationNotification.addCancelListener(
        event -> qcItemDeletionConfirmationNotification.close());
  }

  private void onUploadQualityControlClicked() {
    UploadQualityControlDialog dialog = new UploadQualityControlDialog(
        context.projectId().orElseThrow(), experimentInformationService);
    dialog.addConfirmListener(confirmEvent -> {
      List<QualityControlReport> qualityControlReports = confirmEvent.getSource()
          .qualityControlItems()
          .stream()
          .map(it -> new QualityControlReport(it.fileName(), it.experimentId(), it.content()))
          .toList();
      qualityControlService.addQualityControls(context.projectId().orElseThrow().toString(),
          qualityControlReports);
      refreshQualityControls();
      confirmEvent.getSource().close();
    });
    dialog.addCancelListener(cancelEvent -> cancelEvent.getSource().close());
    dialog.open();
  }

  private void refreshQualityControls() {
    List<QualityControl> qualityControls = qualityControlService.linkedQualityControls(
            context.projectId().orElseThrow().value())
        .stream()
        .map(qualityControl -> {
          String experimentName = getExperimentNameFromQualityControl(qualityControl);
          return new QualityControl(qualityControl.id(), qualityControl.getFileName(),
              experimentName);
        })
        .toList();
    qualityControlListComponent.setQualityControls(qualityControls);
  }

  private String getExperimentNameFromQualityControl(QualityControlUpload qualityControl) {
    return qualityControl.experimentId().map(
        experimentId -> experimentInformationService.find(context.projectId().orElseThrow().value(),
                experimentId).map(Experiment::getName)
            .orElse("")).orElse("");
  }

  private void onAddExperimentClicked(AddExperimentClickEvent event) {
    log.debug("Add experiment clicked: " + event);
    showAddExperimentDialog();
  }

  private void onExperimentSelectionEvent(ExperimentSelectionEvent event) {
    routeToExperiment(event.getExperimentId());
  }

  private void setContext(Context context) {
    this.context = context;
    projectSummaryComponent.setContext(context);
    experimentListComponent.setContext(context);
    refreshOffers(projectPurchaseService, context.projectId().orElseThrow().value(),
        offerListComponent);
    refreshQualityControls();
  }

  private void onExperimentAddEvent(ExperimentAddEvent event) {
    ProjectId projectId = context.projectId().orElseThrow();
    ExperimentId createdExperiment = createExperiment(projectId, event.getExperimentDraft());
    event.getSource().close();
    displayExperimentCreationSuccess(event.getExperimentDraft().getExperimentName());
    routeToExperiment(createdExperiment);
  }

  private void routeToExperiment(ExperimentId experimentId) {
    RouteParameters routeParameters = new RouteParameters(
        new RouteParam(PROJECT_ID_ROUTE_PARAMETER,
            context.projectId().map(ProjectId::value).orElseThrow()),
        new RouteParam(EXPERIMENT_ID_ROUTE_PARAMETER, experimentId.value()));
    getUI().ifPresent(ui -> ui.navigate(ExperimentInformationMain.class, routeParameters));
    log.debug("re-routing to ExperimentInformation page for experiment " + experimentId.value());
  }

  private void showAddExperimentDialog() {
    var creationDialog = new AddExperimentDialog(ontologyTermInformationService, terminologyService);
    creationDialog.addExperimentAddEventListener(this::onExperimentAddEvent);
    creationDialog.addCancelListener(cancelEvent -> showCancelConfirmationDialog(creationDialog));
    creationDialog.setEscAction(() -> showCancelConfirmationDialog(creationDialog));
    creationDialog.open();
  }

  private void showCancelConfirmationDialog(AddExperimentDialog creationDialog) {
    cancelConfirmationDialogFactory.cancelConfirmationDialog(
            it -> creationDialog.close(),
            "experiment.create", getLocale())
        .open();
  }

  private void displayExperimentCreationSuccess(String experimentName) {
    Toast toast = messageSourceNotificationFactory.toast("experiment.created.success",
        new Object[]{experimentName}, getLocale());
    toast.open();
  }

  private ExperimentId createExperiment(ProjectId projectId,
      ExperimentDraft experimentDraft) {
    Result<ExperimentId, RuntimeException> result = addExperimentToProjectService.addExperimentToProject(
        projectId,
        experimentDraft.getExperimentName(),
        experimentDraft.getSpecies(),
        experimentDraft.getSpecimens(),
        experimentDraft.getAnalytes(),
        experimentDraft.getSpeciesIcon().getLabel(),
        experimentDraft.getSpecimenIcon().getLabel());
    if (result.isValue()) {
      return result.getValue();
    } else {
      throw new ApplicationException("Experiment Creation failed");
    }
  }
}
