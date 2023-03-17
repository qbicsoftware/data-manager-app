package life.qbic.datamanager.views.project.view;

import java.util.Objects;
import life.qbic.datamanager.views.project.view.components.ExperimentDetailsComponent;
import life.qbic.datamanager.views.project.view.components.ExperimentListComponent;
import life.qbic.datamanager.views.project.view.components.ProjectDetailsComponent;
import life.qbic.datamanager.views.project.view.components.ProjectLinksComponent;
import life.qbic.datamanager.views.project.view.components.ProjectNavigationBarComponent;

/**
 * Handler for the project view page that routes request parameter to the components.
 *
 * @since 1.0.0
 */
class ProjectViewHandler {

  private final ProjectNavigationBarComponent projectNavigationBarComponent;

  private final ProjectDetailsComponent projectDetailsComponent;
  private final ProjectLinksComponent projectLinksComponent;
  private final ExperimentDetailsComponent experimentDetailsComponent;
  private final ExperimentListComponent experimentListComponent;

  public ProjectViewHandler(ProjectNavigationBarComponent projectNavigationBarComponent,
      ProjectDetailsComponent projectDetailsComponent,
      ProjectLinksComponent projectLinksComponent,
      ExperimentDetailsComponent experimentDetailsComponent,
      ExperimentListComponent experimentListComponent) {
    Objects.requireNonNull(projectNavigationBarComponent);
    Objects.requireNonNull(projectDetailsComponent);
    Objects.requireNonNull(projectLinksComponent);
    Objects.requireNonNull(experimentDetailsComponent);
    Objects.requireNonNull(experimentListComponent);

    this.projectNavigationBarComponent = projectNavigationBarComponent;
    this.projectLinksComponent = projectLinksComponent;
    this.projectDetailsComponent = projectDetailsComponent;
    this.experimentDetailsComponent = experimentDetailsComponent;
    this.experimentListComponent = experimentListComponent;
  }

  /**
   * Forwards a route parameter to all page components
   *
   * @param projectId the route parameter
   * @since 1.0.0
   */
  public void projectId(String projectId) {
    this.projectNavigationBarComponent.projectId(projectId);
    this.projectDetailsComponent.projectId(projectId);
    this.projectLinksComponent.projectId(projectId);
    this.experimentDetailsComponent.projectId(projectId);
    this.experimentListComponent.projectId(projectId);
  }
}
