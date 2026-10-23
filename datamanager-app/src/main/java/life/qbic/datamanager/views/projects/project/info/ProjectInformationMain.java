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
import life.qbic.datamanager.security.UserPermissions;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.general.Main;
import life.qbic.datamanager.views.projects.project.ProjectMainLayout;
import life.qbic.datamanager.views.projects.project.experiments.experiment.create.CreateExperimentMain;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * Project Information Main Component
 * <p>
 * This component hosts the summary of a {@link ProjectId} identified by the project id in the URL.
 * The project page is a single centred column: the summary owns the identity, progress and
 * experiments sections; the former right-hand rails (experiments, offers, sample QC) have been
 * removed. Creating an experiment navigates to the dedicated create route instead of opening a
 * dialog (project owner, 2026-10-19).
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

  private final transient UserPermissions userPermissions;
  private final ProjectSummaryComponent projectSummaryComponent;
  private Context context;

  public ProjectInformationMain(@Autowired ProjectSummaryComponent projectSummaryComponent,
      @Autowired UserPermissions userPermissions) {
    this.projectSummaryComponent = requireNonNull(projectSummaryComponent);
    this.userPermissions = requireNonNull(userPermissions, "userPermissions must not be null");
    this.projectSummaryComponent.addAddExperimentListener(event -> routeToCreateExperiment());
    addClassName("project");
    add(this.projectSummaryComponent);
  }

  /**
   * Extracts the project id from the provided URL before the user accesses the page.
   *
   * @param beforeEnterEvent the event carrying the route parameters
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

  private void routeToCreateExperiment() {
    RouteParameters routeParameters = new RouteParameters(
        new RouteParam(PROJECT_ID_ROUTE_PARAMETER,
            context.projectId().map(ProjectId::value).orElseThrow()));
    getUI().ifPresent(ui -> ui.navigate(CreateExperimentMain.class, routeParameters));
    log.debug("re-routing to the experiment creation page");
  }
}
