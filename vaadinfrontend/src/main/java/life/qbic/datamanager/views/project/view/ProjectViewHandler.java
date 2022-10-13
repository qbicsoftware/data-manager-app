package life.qbic.datamanager.views.project.view;

import java.util.Objects;
import life.qbic.datamanager.views.project.view.components.ProjectLinksComponent;
import life.qbic.datamanager.views.project.view.components.ProjectDetailsComponent;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;

/**
 * Handler for the project view page that routes request parameter to the components.
 *
 * @since 1.0.0
 */
class ProjectViewHandler {

  private static final Logger log = LoggerFactory.logger(ProjectViewHandler.class);
  private final ProjectLinksComponent projectLinksComponent;

  private final ProjectDetailsComponent projectDetailsComponent;

  public ProjectViewHandler(ProjectDetailsComponent projectDetailsComponent,
      ProjectLinksComponent projectLinksComponent) {
    Objects.requireNonNull(projectDetailsComponent);
    Objects.requireNonNull(projectLinksComponent);

    this.projectLinksComponent = projectLinksComponent;
    this.projectDetailsComponent = projectDetailsComponent;
  }

  /**
   * Forwards a route parameter to all page components
   *
   * @param parameter the route parameter
   * @since 1.0.0
   */
  public void routeParameter(String parameter) {
    this.projectDetailsComponent.projectId(parameter);
    this.projectLinksComponent.projectId(parameter);
  }
}
