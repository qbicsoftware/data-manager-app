package life.qbic.datamanager.views.projects.project.info;

import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.PermitAll;
import java.io.Serial;
import java.util.Objects;
import life.qbic.application.commons.ApplicationException;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.MainLayout;
import life.qbic.datamanager.views.general.MainComponent;
import life.qbic.datamanager.views.projects.project.ProjectNavigationBarComponent;
import life.qbic.datamanager.views.projects.project.experiments.ExperimentInformationMain;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.domain.project.ProjectId;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentId;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * Project Information Main Component
 * <p>
 * This component hosts the components necessary to show and update the
 * {@link life.qbic.projectmanagement.domain.project.Project} information via the provided
 * {@link life.qbic.projectmanagement.domain.project.ProjectId} in the URL
 */

@SpringComponent
@UIScope
@Route(value = "projects/:projectId?/info", layout = MainLayout.class)
@PermitAll
public class ProjectInformationMain extends MainComponent implements BeforeEnterObserver,
    RouterLayout {

  @Serial
  private static final long serialVersionUID = 5797835576569148873L;
  private static final Logger log = logger(ProjectInformationMain.class);
  private final ProjectNavigationBarComponent projectNavigationBarComponent;
  private final ProjectContentComponent projectContentComponent;
  private final ProjectSupportComponent projectSupportComponent;
  public static final String PROJECT_ID_ROUTE_PARAMETER = "projectId";
  private Context context;

  public ProjectInformationMain(
      @Autowired ProjectNavigationBarComponent projectNavigationBarComponent,
      @Autowired ProjectContentComponent projectContentComponent,
      @Autowired ProjectSupportComponent projectSupportComponent) {
    super(projectContentComponent, projectSupportComponent);
    Objects.requireNonNull(projectNavigationBarComponent);
    Objects.requireNonNull(projectContentComponent);
    Objects.requireNonNull(projectSupportComponent);
    this.projectNavigationBarComponent = projectNavigationBarComponent;
    this.projectContentComponent = projectContentComponent;
    this.projectSupportComponent = projectSupportComponent;
    layoutComponent();
    log.debug(String.format(
        "New instance for project Information Page (#%s) created with Project Navigation Bar Component (#%s) and Project Content Component (#%s) and Project Support Component (#%s)",
        System.identityHashCode(this), System.identityHashCode(projectNavigationBarComponent),
        System.identityHashCode(projectContentComponent),
        System.identityHashCode(projectSupportComponent)));
  }

  private void layoutComponent() {
    addClassName("project");
    addComponentAsFirst(projectNavigationBarComponent);
  }

  /**
   * Extracts {@link ExperimentId} from the provided URL before the user accesses the page
   * <p>
   * This method is responsible for checking if the provided {@link ExperimentId} is valid and
   * triggering its propagation to the components within the {@link ExperimentInformationMain}
   */

  @Override
  public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
    String projectID = beforeEnterEvent.getRouteParameters().get(PROJECT_ID_ROUTE_PARAMETER)
        .orElseThrow();
    if (!ProjectId.isValid(projectID)) {
      throw new ApplicationException("invalid project id " + projectID);
    }
    ProjectId parsedProjectId = ProjectId.parse(projectID);
    this.context = new Context().with(parsedProjectId);
    setContext(context);
  }

  private void setContext(Context context) {
    projectContentComponent.setContext(context);
    projectNavigationBarComponent.projectId(context.projectId().orElseThrow());
    projectSupportComponent.projectId(context.projectId().orElseThrow());
  }

}
