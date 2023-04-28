package life.qbic.datamanager.views.projects.project;

import java.util.Objects;
import life.qbic.datamanager.views.projects.project.experiments.ExperimentInformationPage;
import life.qbic.datamanager.views.projects.project.info.ProjectInformationPage;
import life.qbic.datamanager.views.projects.project.samples.SampleInformationPage;
import life.qbic.projectmanagement.domain.project.ProjectId;

/**
 * Handler for the project view page that routes request parameter to the components.
 *
 * @since 1.0.0
 */
class ProjectViewHandler {

  private final ProjectInformationPage projectInformationPage;
  private final ExperimentInformationPage experimentInformationPage;
  private final SampleInformationPage sampleInformationPage;

  public ProjectViewHandler(ProjectInformationPage projectInformationPage,
      ExperimentInformationPage experimentInformationPage,
      SampleInformationPage sampleInformationPage) {
    Objects.requireNonNull(projectInformationPage);
    Objects.requireNonNull(experimentInformationPage);
    Objects.requireNonNull(sampleInformationPage);

    this.projectInformationPage = projectInformationPage;
    this.experimentInformationPage = experimentInformationPage;
    this.sampleInformationPage = sampleInformationPage;
  }

  /**
   * Forwards a route parameter to all page components
   *
   * @param projectId the route parameter
   * @since 1.0.0
   */
  public void setProjectId(ProjectId projectId) {
    this.projectInformationPage.projectId(projectId);
    this.experimentInformationPage.projectId(projectId);
    this.sampleInformationPage.projectId(projectId);
  }
}
