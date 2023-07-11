package life.qbic.datamanager.views.projects.project;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;
import jakarta.annotation.security.PermitAll;
import java.io.Serial;
import java.util.Objects;
import life.qbic.application.commons.Result;
import life.qbic.datamanager.views.AppRoutes.Projects;
import life.qbic.datamanager.views.MainLayout;
import life.qbic.datamanager.views.projects.project.experiments.ExperimentInformationMain;
import life.qbic.datamanager.views.projects.project.info.ProjectInformationMain;
import life.qbic.datamanager.views.projects.project.samples.SampleInformationPage;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.domain.project.ProjectId;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Project view page that shows project information and additional components to manage project
 * data.
 *
 * @since 1.0.0
 */
@Route(value = "projects/:projectId?")
@PermitAll
@ParentLayout(MainLayout.class)
public class ProjectViewPage extends Div implements BeforeEnterObserver, RouterLayout {

  @Serial
  private static final long serialVersionUID = 3402433356187177105L;
  private static final Logger log = LoggerFactory.logger(ProjectViewPage.class);
  private final transient Handler handler;

  public ProjectViewPage(@Autowired ProjectInformationMain projectInformationMain,
      @Autowired ExperimentInformationMain experimentInformationMain,
      @Autowired SampleInformationPage sampleInformationPage,
      @Autowired ProjectInformationService projectInformationService) {
    Objects.requireNonNull(projectInformationMain);
    Objects.requireNonNull(experimentInformationMain);
    Objects.requireNonNull(sampleInformationPage);
    stylePage();
    handler = new Handler(projectInformationMain,
        experimentInformationMain, sampleInformationPage, projectInformationService);
    log.debug(String.format(
        "New instance for project view (#%s) created with a project information page (#%s), an experiment information page (#%s), and a sample information page (#%s)",
        System.identityHashCode(this),
        System.identityHashCode(projectInformationMain),
        System.identityHashCode(experimentInformationMain),
        System.identityHashCode(sampleInformationPage)));
  }

  @Override
  public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
    beforeEnterEvent.getRouteParameters().get("projectId").ifPresentOrElse(
        param -> handler.validateProjectId(param)
            .onError(e -> navigateToNotFound(e, beforeEnterEvent))
            .onValue(handler::setProjectId),
        () -> beforeEnterEvent.forwardTo(Projects.PROJECTS));
  }

  private void stylePage() {
    this.setWidthFull();
    this.setHeightFull();
  }

  private void navigateToNotFound(RuntimeException e, BeforeEnterEvent enterEvent) {
    log.error(e.getMessage(), e);
    enterEvent.rerouteToError(NotFoundException.class);
  }

  /**
   * Handler for the project view page that routes request parameter to the components.
   *
   * @since 1.0.0
   */
  static class Handler {

    private final ProjectInformationMain projectInformationMain;
    private final ExperimentInformationMain experimentInformationMain;
    private final SampleInformationPage sampleInformationPage;

    private final ProjectInformationService projectInformationService;


    public Handler(ProjectInformationMain projectInformationMain,
        ExperimentInformationMain experimentInformationMain,
        SampleInformationPage sampleInformationPage,
        ProjectInformationService projectInformationService) {
      Objects.requireNonNull(projectInformationMain);
      Objects.requireNonNull(experimentInformationMain);
      Objects.requireNonNull(sampleInformationPage);
      Objects.requireNonNull(projectInformationService);

      this.projectInformationMain = projectInformationMain;
      this.experimentInformationMain = experimentInformationMain;
      this.sampleInformationPage = sampleInformationPage;
      this.projectInformationService = projectInformationService;
    }

    /**
     * Forwards a route parameter to all page components
     *
     * @param projectId the route parameter
     * @since 1.0.0
     */
    public void setProjectId(ProjectId projectId) {
      this.projectInformationMain.projectId(projectId);
      this.experimentInformationMain.projectId(projectId);
      this.sampleInformationPage.projectId(projectId);
    }

    private Result<ProjectId, RuntimeException> validateProjectId(String projectIdParam) {
      ProjectId projectId;
      try {
        projectId = ProjectId.parse(projectIdParam);
      } catch (RuntimeException e) {
        return Result.fromError(e);
      }
      boolean isProjectPresent = projectInformationService.find(projectId).isPresent();
      if (isProjectPresent) {
        return Result.fromValue(projectId);
      } else {
        return Result.fromError(
            new NotFoundException("Project " + projectIdParam + " was not found."));
      }
    }
  }
}
