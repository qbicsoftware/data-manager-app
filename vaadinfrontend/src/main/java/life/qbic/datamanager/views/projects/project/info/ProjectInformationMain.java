package life.qbic.datamanager.views.projects.project.info;

import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParam;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.PermitAll;
import java.io.Serial;
import java.util.Objects;
import life.qbic.datamanager.views.MainLayout;
import life.qbic.datamanager.views.general.MainComponent;
import life.qbic.datamanager.views.projects.project.ProjectNavigationBarComponent;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.domain.project.ProjectId;
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
  private final transient ProjectInformationMainHandler projectInformationMainHandler;
  public static final String PROJECT_ID_ROUTE_PARAMETER = "projectId";

  public ProjectInformationMain(
      @Autowired ProjectNavigationBarComponent projectNavigationBarComponent,
      @Autowired ProjectContentComponent projectContentComponent,
      @Autowired ProjectSupportComponent projectSupportComponent) {
    super(projectContentComponent, projectSupportComponent);
    Objects.requireNonNull(projectNavigationBarComponent);
    Objects.requireNonNull(projectContentComponent);
    Objects.requireNonNull(projectSupportComponent);
    this.projectNavigationBarComponent = projectNavigationBarComponent;
    layoutComponent();
    projectInformationMainHandler = new ProjectInformationMainHandler(projectNavigationBarComponent,
        projectContentComponent,
        projectSupportComponent);
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

  public void projectId(ProjectId projectId) {
    projectInformationMainHandler.setProjectId(projectId);
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    event.getRouteParameters().get(PROJECT_ID_ROUTE_PARAMETER)
        .ifPresent(
            projectInformationMainHandler::propagateProjectId);
  }

  private final class ProjectInformationMainHandler {

    private final ProjectNavigationBarComponent projectNavigationComponent;
    private final ProjectContentComponent projectContentComponent;
    private final ProjectSupportComponent projectSupportComponent;

    public ProjectInformationMainHandler(ProjectNavigationBarComponent projectNavigationComponent,
        ProjectContentComponent projectContentComponent,
        ProjectSupportComponent projectSupportComponent) {
      this.projectNavigationComponent = projectNavigationComponent;
      this.projectContentComponent = projectContentComponent;
      this.projectSupportComponent = projectSupportComponent;
    }

    public void setProjectId(ProjectId projectId) {
      projectNavigationComponent.projectId(projectId);
      projectContentComponent.projectId(projectId);
      projectSupportComponent.projectId(projectId);
    }

    /**
     * Reroutes to the ProjectId provided in the URL
     * <p>
     * This method generates the URL and routes the user via {@link RouteParam} to the provided
     * ProjectId
     */
    private void propagateProjectId(String projectParam) {
      try {
        ProjectId projectId = ProjectId.parse(projectParam);
        projectInformationMainHandler.setProjectId(projectId);
      } catch (IllegalArgumentException e) {
        log.debug(
            String.format("Provided ProjectId %s is invalid due to %s", projectParam,
                e.getMessage()));
      }
    }
  }

}
