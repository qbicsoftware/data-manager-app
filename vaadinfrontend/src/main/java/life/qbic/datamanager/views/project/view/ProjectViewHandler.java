package life.qbic.datamanager.views.project.view;

import java.util.Objects;
import life.qbic.datamanager.views.project.view.components.ExperimentalDesignDetailComponent;
import life.qbic.datamanager.views.project.view.components.ProjectDetailsComponent;
import life.qbic.datamanager.views.project.view.components.ProjectLinksComponent;
import life.qbic.datamanager.views.project.view.components.ProjectNavigationBarComponent;
import life.qbic.datamanager.views.project.view.sample.SampleInformationPage;

/**
 * Handler for the project view page that routes request parameter to the components.
 *
 * @since 1.0.0
 */
class ProjectViewHandler {

  private final ProjectLinksComponent projectLinksComponent;
  private final ProjectNavigationBarComponent projectNavigationBarComponent;
  private final ProjectDetailsComponent projectDetailsComponent;
  private final ExperimentalDesignDetailComponent experimentalDesignDetailComponent;
  private final SampleInformationPage sampleInformationPage;

  public ProjectViewHandler(ProjectNavigationBarComponent projectNavigationBarComponent,
      ProjectDetailsComponent projectDetailsComponent, ProjectLinksComponent projectLinksComponent,
      ExperimentalDesignDetailComponent experimentalDesignDetailComponent,
      SampleInformationPage sampleInformationPage) {
    Objects.requireNonNull(projectDetailsComponent);
    Objects.requireNonNull(projectLinksComponent);
    Objects.requireNonNull(experimentalDesignDetailComponent);
    Objects.requireNonNull(projectNavigationBarComponent);
    Objects.requireNonNull(sampleInformationPage);
    this.projectNavigationBarComponent = projectNavigationBarComponent;
    this.projectLinksComponent = projectLinksComponent;
    this.projectDetailsComponent = projectDetailsComponent;
    this.experimentalDesignDetailComponent = experimentalDesignDetailComponent;
    this.sampleInformationPage = sampleInformationPage;
  }

  /**
   * Forwards a route parameter to all page components
   *
   * @param projectId the route parameter
   * @since 1.0.0
   */
  public void projectId(String projectId) {
    this.projectDetailsComponent.projectId(projectId);
    this.projectLinksComponent.projectId(projectId);
    this.experimentalDesignDetailComponent.projectId(projectId);
    this.projectNavigationBarComponent.projectId(projectId);
    this.sampleInformationPage.projectId(projectId);
  }
}
