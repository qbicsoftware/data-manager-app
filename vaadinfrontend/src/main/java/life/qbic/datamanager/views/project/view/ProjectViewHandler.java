package life.qbic.datamanager.views.project.view;

import java.util.Objects;
import life.qbic.datamanager.views.project.view.pages.experiment.ExperimentInformationPage;
import life.qbic.datamanager.views.project.view.pages.projectinformation.ProjectInformationPage;

/**
 * Handler for the project view page that routes request parameter to the components.
 *
 * @since 1.0.0
 */
class ProjectViewHandler {

  private final ProjectInformationPage projectInformationPage;
  private final ExperimentInformationPage experimentInformationPage;

  public ProjectViewHandler(ProjectInformationPage projectInformationPage,
      ExperimentInformationPage experimentInformationPage) {
    Objects.requireNonNull(projectInformationPage);
    Objects.requireNonNull(experimentInformationPage);

    this.projectInformationPage = projectInformationPage;
    this.experimentInformationPage = experimentInformationPage;
  }

  /**
   * Forwards a route parameter to all page components
   *
   * @param projectId the route parameter
   * @since 1.0.0
   */
  public void projectId(String projectId) {
    this.projectInformationPage.projectId(projectId);
    this.experimentInformationPage.projectId(projectId);
  }
}
