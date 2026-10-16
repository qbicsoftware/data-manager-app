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
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.Result;
import life.qbic.datamanager.security.UserPermissions;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.general.Main;
import life.qbic.datamanager.views.general.dialog.AlertDialog;
import life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory;
import life.qbic.datamanager.views.notifications.Toast;
import life.qbic.datamanager.views.projects.project.ProjectMainLayout;
import life.qbic.datamanager.views.projects.project.experiments.ExperimentInformationMain;
import life.qbic.datamanager.views.projects.project.experiments.experiment.create.AddExperimentDialog;
import life.qbic.datamanager.views.projects.project.experiments.experiment.create.AddExperimentDialog.ExperimentAddEvent;
import life.qbic.datamanager.views.projects.project.experiments.experiment.create.AddExperimentDialog.ExperimentDraft;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.AddExperimentToProjectService;
import life.qbic.projectmanagement.application.ontology.SpeciesLookupService;
import life.qbic.projectmanagement.application.ontology.TerminologyService;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * Project Information Main Component
 * <p>
 * This component hosts the summary of a {@link Project} identified by the {@link ProjectId} in the
 * URL. The project page is a single centred column: the summary owns the identity, progress and
 * experiments sections; the former right-hand rails (experiments, offers, sample QC) have been
 * removed. Offers move to the project settings surface when it exists; the offer and QC upload
 * dialogs are kept for reuse there.
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
  private final transient SpeciesLookupService ontologyTermInformationService;
  private final transient UserPermissions userPermissions;
  private final ProjectSummaryComponent projectSummaryComponent;
  private final transient MessageSourceNotificationFactory messageSourceNotificationFactory;
  private final transient TerminologyService terminologyService;
  private Context context;

  public ProjectInformationMain(@Autowired ProjectSummaryComponent projectSummaryComponent,
      @Autowired UserPermissions userPermissions,
      @Autowired AddExperimentToProjectService addExperimentToProjectService,
      @Autowired SpeciesLookupService ontologyTermInformationService,
      @Autowired TerminologyService terminologyService,
      MessageSourceNotificationFactory messageSourceNotificationFactory) {
    this.projectSummaryComponent = requireNonNull(projectSummaryComponent);
    this.userPermissions = requireNonNull(userPermissions, "userPermissions must not be null");
    this.addExperimentToProjectService = requireNonNull(addExperimentToProjectService,
        "addExperimentToProjectService must not be null");
    this.ontologyTermInformationService = requireNonNull(ontologyTermInformationService,
        "ontologyTermInformationService must not be null");
    this.messageSourceNotificationFactory = requireNonNull(messageSourceNotificationFactory,
        "messageSourceNotificationFactory must not be null");
    this.terminologyService = requireNonNull(terminologyService);

    this.projectSummaryComponent.addAddExperimentListener(event -> showAddExperimentDialog());
    addClassName("project");
    add(this.projectSummaryComponent);
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

  private void setContext(Context context) {
    this.context = context;
    projectSummaryComponent.setContext(context);
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
    AlertDialog.alert(this)
        .warning()
        .title("Discard changes?")
        .message("By aborting the editing process and closing the dialog, you will lose all information entered.")
        .confirmButton("Discard changes", () -> creationDialog.close())
        .cancelButton("Keep editing", () -> {})
        .build()
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
        experimentDraft.getAnalytes());
    if (result.isValue()) {
      return result.getValue();
    } else {
      throw new ApplicationException("Experiment Creation failed");
    }
  }
}
