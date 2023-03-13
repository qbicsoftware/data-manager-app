package life.qbic.datamanager.views.project.view;

import java.util.Objects;
import life.qbic.datamanager.views.project.view.components.ExperimentDetailsComponent;
import life.qbic.datamanager.views.project.view.components.ExperimentListComponent;
import life.qbic.datamanager.views.project.view.components.ProjectDetailsComponent;
import life.qbic.datamanager.views.project.view.components.ProjectLinksComponent;

/**
 * Handler for the project view page that routes request parameter to the components.
 *
 * @since 1.0.0
 */
class ProjectViewHandler {

  private final ProjectLinksComponent projectLinksComponent;
  private final ProjectDetailsComponent projectDetailsComponent;
  private final ExperimentDetailsComponent experimentDetailsComponent;
  private final ExperimentListComponent experimentListComponent;

  public ProjectViewHandler(ProjectDetailsComponent projectDetailsComponent,
      ProjectLinksComponent projectLinksComponent,
      ExperimentDetailsComponent experimentDetailsComponent,
      ExperimentListComponent experimentListComponent) {
    Objects.requireNonNull(projectDetailsComponent);
    Objects.requireNonNull(projectLinksComponent);
    Objects.requireNonNull(experimentDetailsComponent);
    Objects.requireNonNull(experimentListComponent);
    this.projectDetailsComponent = projectDetailsComponent;
    this.projectLinksComponent = projectLinksComponent;
    this.experimentDetailsComponent = experimentDetailsComponent;
    this.experimentListComponent = experimentListComponent;
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
    this.experimentListComponent.projectId(parameter);
    this.experimentDetailsComponent.projectId(parameter);
  }
}
