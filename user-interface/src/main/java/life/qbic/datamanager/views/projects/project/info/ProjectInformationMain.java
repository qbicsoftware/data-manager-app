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
import life.qbic.datamanager.views.notifications.StyledNotification;
import life.qbic.datamanager.views.notifications.SuccessMessage;
import life.qbic.datamanager.views.projects.project.ProjectMainLayout;
import life.qbic.datamanager.views.projects.project.experiments.ExperimentInformationMain;
import life.qbic.datamanager.views.projects.project.experiments.ExperimentListComponent;
import life.qbic.datamanager.views.projects.project.experiments.ExperimentListComponent.AddExperimentClickEvent;
import life.qbic.datamanager.views.projects.project.experiments.ExperimentListComponent.ExperimentSelectionEvent;
import life.qbic.datamanager.views.projects.project.experiments.experiment.create.AddExperimentDialog;
import life.qbic.datamanager.views.projects.project.experiments.experiment.create.AddExperimentDialog.ExperimentAddEvent;
import life.qbic.datamanager.views.projects.project.experiments.experiment.create.AddExperimentDialog.ExperimentDraft;
import life.qbic.datamanager.views.projects.project.info.OfferList.DeleteOfferClickEvent;
import life.qbic.datamanager.views.projects.project.info.OfferList.DownloadOfferClickEvent;
import life.qbic.datamanager.views.projects.project.info.OfferList.OfferInfo;
import life.qbic.datamanager.views.projects.project.info.OfferList.UploadOfferClickEvent;
import life.qbic.datamanager.views.projects.project.info.QualityControlList.DeleteQualityControlEvent;
import life.qbic.datamanager.views.projects.project.info.QualityControlList.DownloadQualityControlEvent;
import life.qbic.datamanager.views.projects.project.info.QualityControlList.QualityControl;
import life.qbic.datamanager.views.projects.purchase.UploadPurchaseDialog;
import life.qbic.datamanager.views.projects.qualityControl.UploadQualityControlDialog;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.AddExperimentToProjectService;
import life.qbic.projectmanagement.application.ExperimentInformationService;
import life.qbic.projectmanagement.application.OntologyTermInformationService;
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

  @Serial
  private static final long serialVersionUID = 5797835576569148873L;
  private static final Logger log = logger(ProjectInformationMain.class);
  private final transient AddExperimentToProjectService addExperimentToProjectService;
  private final transient ExperimentInformationService experimentInformationService;
  private final transient OntologyTermInformationService ontologyTermInformationService;
  private final transient ProjectPurchaseService projectPurchaseService;
  private final transient QualityControlService qualityControlService;
  private final transient UserPermissions userPermissions;
  public static final String PROJECT_ID_ROUTE_PARAMETER = "projectId";
  public static final String EXPERIMENT_ID_ROUTE_PARAMETER = "experimentId";
  private final ProjectDetailsComponent projectDetailsComponent;
  private final ExperimentListComponent experimentListComponent;
  private final OfferDownload offerDownload;
  private final QualityControlDownload qualityControlDownload;
  private final OfferList offerList;
  private final QualityControlList qualityControlList;
  private Context context;

  public ProjectInformationMain(@Autowired ProjectDetailsComponent projectDetailsComponent,
      @Autowired ExperimentListComponent experimentListComponent,
      @Autowired UserPermissions userPermissions,
      @Autowired AddExperimentToProjectService addExperimentToProjectService,
      @Autowired OntologyTermInformationService ontologyTermInformationService,
      @Autowired ExperimentInformationService experimentInformationService,
      @Autowired ProjectPurchaseService projectPurchaseService,
      @Autowired QualityControlService qualityControlService) {
    this.projectDetailsComponent = requireNonNull(projectDetailsComponent,
        "projectDetailsComponent must not be null");
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

    offerList = getConfiguredOfferList();
    qualityControlList = getConfiguredQualityControlList();
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
    add(projectDetailsComponent, offerList, offerDownload, experimentListComponent,
        qualityControlList, qualityControlDownload);
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

  private OfferList getConfiguredOfferList() {
    OfferList component = new OfferList();
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
    projectPurchaseService.deleteOffer(context.projectId().orElseThrow().value(),
        deleteOfferClickEvent.offerId());
    deleteOfferClickEvent.getSource().remove(deleteOfferClickEvent.offerId());
    offerDownload.removeHref();
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

  private static void refreshOffers(ProjectPurchaseService projectPurchaseService, String projectId,
      OfferList offerList) {
    List<OfferInfo> offers = projectPurchaseService.linkedOffers(projectId)
        .stream()
        .map(offer -> new OfferInfo(offer.id(), offer.getFileName(), offer.isSigned()))
        .toList();
    offerList.setOffers(offers);
  }

  private QualityControlList getConfiguredQualityControlList() {
    QualityControlList component = new QualityControlList();
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
    qualityControlService.deleteQualityControl(context.projectId().orElseThrow().value(),
        deleteQualityControlEvent.qualityControlId());
    qualityControlDownload.removeHref();
    refreshQualityControls();
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
    qualityControlList.setQualityControls(qualityControls);
  }

  private String getExperimentNameFromQualityControl(QualityControlUpload qualityControl) {
    return qualityControl.experimentId().map(
        experimentId -> experimentInformationService.find(experimentId).map(Experiment::getName)
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
    projectDetailsComponent.setContext(context);
    experimentListComponent.setContext(context);
    refreshOffers(projectPurchaseService, context.projectId().orElseThrow().value(), offerList);
    refreshQualityControls();
  }

  private void onExperimentAddEvent(ExperimentAddEvent event) {
    ProjectId projectId = context.projectId().orElseThrow();
    ExperimentId createdExperiment = createExperiment(projectId, event.getExperimentDraft());
    event.getSource().close();
    displayExperimentCreationSuccess();
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
    var creationDialog = new AddExperimentDialog(ontologyTermInformationService);
    creationDialog.addExperimentAddEventListener(this::onExperimentAddEvent);
    creationDialog.addCancelListener(event -> event.getSource().close());
    creationDialog.open();
  }

  private void displayExperimentCreationSuccess() {
    SuccessMessage successMessage = new SuccessMessage("Experiment Creation succeeded", "");
    StyledNotification notification = new StyledNotification(successMessage);
    notification.open();
  }

  private ExperimentId createExperiment(ProjectId projectId,
      ExperimentDraft experimentDraft) {
    Result<ExperimentId, RuntimeException> result = addExperimentToProjectService.addExperimentToProject(
        projectId,
        experimentDraft.getExperimentName(),
        experimentDraft.getSpecies(),
        experimentDraft.getSpecimens(),
        experimentDraft.getAnalytes());
    if (result.isValue()) {
      return result.getValue();
    } else {
      throw new ApplicationException("Experiment Creation failed");
    }
  }
}
