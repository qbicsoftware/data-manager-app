package life.qbic.datamanager.views.project.view;

import java.util.Objects;
import life.qbic.datamanager.views.project.view.components.ExperimentalDesignDetailComponent;
import life.qbic.datamanager.views.project.view.components.ProjectDetailsComponent;
import life.qbic.datamanager.views.project.view.components.ProjectLinksComponent;
import life.qbic.datamanager.views.project.view.components.ProjectNavigationBarComponent;
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
  private final ProjectNavigationBarComponent projectNavigationBarComponent;
  private final ProjectDetailsComponent projectDetailsComponent;

  private final ExperimentalDesignDetailComponent experimentalDesignDetailComponent;

  public ProjectViewHandler(ProjectNavigationBarComponent projectNavigationBarComponent,
      ProjectDetailsComponent projectDetailsComponent,
      ProjectLinksComponent projectLinksComponent,
      ExperimentalDesignDetailComponent experimentalDesignDetailComponent) {
    Objects.requireNonNull(projectDetailsComponent);
    Objects.requireNonNull(projectLinksComponent);
    Objects.requireNonNull(experimentalDesignDetailComponent);
    Objects.requireNonNull(projectNavigationBarComponent);
    this.projectNavigationBarComponent = projectNavigationBarComponent;
    this.projectLinksComponent = projectLinksComponent;
    this.projectDetailsComponent = projectDetailsComponent;
    this.experimentalDesignDetailComponent = experimentalDesignDetailComponent;
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
