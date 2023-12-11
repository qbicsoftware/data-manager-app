package life.qbic.datamanager.views.projects.project.info;

import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParam;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.PermitAll;
import java.io.Serial;
import java.util.List;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.Result;
import life.qbic.datamanager.security.UserPermissions;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.OfferDownload;
import life.qbic.datamanager.views.general.MainComponent;
import life.qbic.datamanager.views.notifications.StyledNotification;
import life.qbic.datamanager.views.notifications.SuccessMessage;
import life.qbic.datamanager.views.projects.project.ProjectMainLayout;
import life.qbic.datamanager.views.projects.project.experiments.ExperimentInformationMain;
import life.qbic.datamanager.views.projects.project.experiments.experiment.create.ExperimentAddDialog;
import life.qbic.datamanager.views.projects.project.info.OfferList.DeleteOfferClickEvent;
import life.qbic.datamanager.views.projects.project.info.OfferList.DownloadOfferClickEvent;
import life.qbic.datamanager.views.projects.project.info.OfferList.OfferInfo;
import life.qbic.datamanager.views.projects.project.info.OfferList.UploadOfferClickEvent;
import life.qbic.datamanager.views.projects.purchase.UploadPurchaseDialog;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.AddExperimentToProjectService;
import life.qbic.projectmanagement.application.OntologyTermInformationService;
import life.qbic.projectmanagement.application.purchase.OfferDTO;
import life.qbic.projectmanagement.application.purchase.ProjectPurchaseService;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
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
public class ProjectInformationMain extends MainComponent implements BeforeEnterObserver,
    RouterLayout {

  @Serial
  private static final long serialVersionUID = 5797835576569148873L;
  private static final Logger log = logger(ProjectInformationMain.class);
  private final ProjectContentComponent projectContentComponent;
  private final ProjectSupportComponent projectSupportComponent;
  private final OfferList offerList;
  private final transient AddExperimentToProjectService addExperimentToProjectService;
  private final transient OntologyTermInformationService ontologyTermInformationService;
  private final UserPermissions userPermissions;
  public static final String PROJECT_ID_ROUTE_PARAMETER = "projectId";
  public static final String EXPERIMENT_ID_ROUTE_PARAMETER = "experimentId";
  private Context context;
  private final OfferDownload offerDownload;
  private final ProjectPurchaseService projectPurchaseService;

  public ProjectInformationMain(
      @Autowired ProjectContentComponent projectContentComponent,
      @Autowired ProjectSupportComponent projectSupportComponent,
      @Autowired UserPermissions userPermissions,
      @Autowired AddExperimentToProjectService addExperimentToProjectService,
      @Autowired OntologyTermInformationService ontologyTermInformationService,
      @Autowired ProjectPurchaseService projectPurchaseService) {
    super(projectContentComponent, projectSupportComponent);
    requireNonNull(userPermissions, "userPermissions must not be null");
    requireNonNull(projectContentComponent);
    requireNonNull(projectSupportComponent);
    requireNonNull(addExperimentToProjectService);
    requireNonNull(ontologyTermInformationService);
    requireNonNull(projectPurchaseService, "projectPurchaseService must not be null");
    this.projectPurchaseService = projectPurchaseService;
    this.projectContentComponent = projectContentComponent;
    this.projectSupportComponent = projectSupportComponent;
    this.userPermissions = userPermissions;
    this.addExperimentToProjectService = addExperimentToProjectService;
    this.ontologyTermInformationService = ontologyTermInformationService;
    this.offerDownload = new OfferDownload(
        (projectId, offerId) -> this.projectPurchaseService.getOfferWithContent(projectId, offerId)
            .orElseThrow());
    this.offerList = setupOfferList();

    addClassName("project");
    addListeners();
    log.debug(String.format(
        "New instance for %s(#%s) created with %s(#%s) and %s(#%s)",
        this.getClass().getSimpleName(), System.identityHashCode(this),
        projectContentComponent.getClass().getSimpleName(),
        System.identityHashCode(projectContentComponent),
        projectSupportComponent.getClass().getSimpleName(),
        System.identityHashCode(projectSupportComponent)));
    add(offerDownload, offerList);

  }

  private OfferList setupOfferList() {
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

  /**
   * Extracts {@link ExperimentId} from the provided URL before the user accesses the page
   * <p>
   * This method is responsible for checking if the provided {@link ExperimentId} is valid and
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

  private void setContext(Context context) {
    this.context = context;
    refreshOffers(projectPurchaseService, context.projectId().orElseThrow().value(), offerList);
    projectContentComponent.setContext(context);
    projectSupportComponent.setContext(context);
  }

  private void addListeners() {
    projectSupportComponent.addExperimentSelectionListener(
        event -> routeToExperiment(event.getExperimentId()));
    projectSupportComponent.addExperimentAddButtonClickEventListener(
        event -> showAddExperimentDialog());
  }

  private void onExperimentAddEvent(ExperimentAddDialog.ExperimentAddEvent event) {
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
    var creationDialog = new ExperimentAddDialog(ontologyTermInformationService);
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
      ExperimentAddDialog.ExperimentDraft experimentDraft) {
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
