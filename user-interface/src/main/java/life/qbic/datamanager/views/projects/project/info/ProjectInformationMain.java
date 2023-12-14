package life.qbic.datamanager.views.projects.project.info;

import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.html.Div;
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
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.Result;
import life.qbic.datamanager.security.UserPermissions;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.notifications.StyledNotification;
import life.qbic.datamanager.views.notifications.SuccessMessage;
import life.qbic.datamanager.views.projects.project.ProjectMainLayout;
import life.qbic.datamanager.views.projects.project.experiments.ExperimentInformationMain;
import life.qbic.datamanager.views.projects.project.experiments.ExperimentListComponent;
import life.qbic.datamanager.views.projects.project.experiments.ExperimentListComponent.AddExperimentClickEvent;
import life.qbic.datamanager.views.projects.project.experiments.ExperimentListComponent.ExperimentSelectionEvent;
import life.qbic.datamanager.views.projects.project.experiments.experiment.create.AddExperimentDialog;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.AddExperimentToProjectService;
import life.qbic.projectmanagement.application.OntologyTermInformationService;
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
public class ProjectInformationMain extends Div implements BeforeEnterObserver,
    RouterLayout {

  @Serial
  private static final long serialVersionUID = 5797835576569148873L;
  private static final Logger log = logger(ProjectInformationMain.class);
  private final transient AddExperimentToProjectService addExperimentToProjectService;
  private final transient OntologyTermInformationService ontologyTermInformationService;
  private final UserPermissions userPermissions;
  public static final String PROJECT_ID_ROUTE_PARAMETER = "projectId";
  public static final String EXPERIMENT_ID_ROUTE_PARAMETER = "experimentId";
  private final ProjectDetailsComponent projectDetailsComponent;
  private final ExperimentListComponent experimentListComponent;
  private final ProjectLinksComponent projectLinksComponent; //TODO replace with OfferLinks
  private Context context;

  public ProjectInformationMain(@Autowired ProjectDetailsComponent projectDetailsComponent,
      @Autowired ExperimentListComponent experimentListComponent,
      @Autowired ProjectLinksComponent projectLinksComponent,
      @Autowired UserPermissions userPermissions,
      @Autowired AddExperimentToProjectService addExperimentToProjectService,
      @Autowired OntologyTermInformationService ontologyTermInformationService) {
    this.projectDetailsComponent = requireNonNull(projectDetailsComponent,
        "projectDetailsComponent must not be null");
    this.experimentListComponent = requireNonNull(experimentListComponent,
        "experimentListComponent must not be null");
    this.projectLinksComponent = requireNonNull(projectLinksComponent,
        "projectLinksComponent must not be null");
    this.userPermissions = requireNonNull(userPermissions, "userPermissions must not be null");
    this.addExperimentToProjectService = requireNonNull(addExperimentToProjectService,
        "addExperimentToProjectService must not be null");
    this.ontologyTermInformationService = requireNonNull(ontologyTermInformationService,
        "ontologyTermInformationService must not be null");

    this.experimentListComponent.addExperimentSelectionListener(this::onExperimentSelectionEvent);
    this.experimentListComponent.addAddButtonListener(this::onAddExperimentClicked);

    addClassNames("main", "project");
    add(projectDetailsComponent, projectLinksComponent, experimentListComponent);

    log.debug(String.format(
        "New instance for %s(#%s) created with %s(#%s), %s(#%s) and %s(#%s)",
        this.getClass().getSimpleName(), System.identityHashCode(this),
        projectDetailsComponent.getClass().getSimpleName(),
        System.identityHashCode(projectDetailsComponent),
        experimentListComponent.getClass().getSimpleName(),
        System.identityHashCode(experimentListComponent),
        projectLinksComponent.getClass().getSimpleName(),
        System.identityHashCode(projectLinksComponent)));
  }

  private void onAddExperimentClicked(AddExperimentClickEvent event) {
    log.debug("Add experiment clicked: " + event);
    showAddExperimentDialog();
  }

  private void onExperimentSelectionEvent(ExperimentSelectionEvent event) {
    routeToExperiment(event.getExperimentId());
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
    projectDetailsComponent.setContext(context);
    projectLinksComponent.setContext(context);
    experimentListComponent.setContext(context);
  }

  private void onExperimentAddEvent(AddExperimentDialog.ExperimentAddEvent event) {
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
      AddExperimentDialog.ExperimentDraft experimentDraft) {
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
