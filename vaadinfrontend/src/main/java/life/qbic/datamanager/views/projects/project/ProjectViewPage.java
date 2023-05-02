package life.qbic.datamanager.views.projects.project;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;
import jakarta.annotation.security.PermitAll;
import java.io.Serial;
import java.util.Objects;
import life.qbic.application.commons.ApplicationException;
import life.qbic.datamanager.views.MainLayout;
import life.qbic.datamanager.views.projects.project.experiments.ExperimentInformationPage;
import life.qbic.datamanager.views.projects.project.info.ProjectInformationPage;
import life.qbic.datamanager.views.projects.project.samples.SampleInformationPage;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
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
  private final transient ProjectViewHandler handler;

  public ProjectViewPage(@Autowired ProjectInformationPage projectInformationPage,
      @Autowired ExperimentInformationPage experimentInformationPage,
      @Autowired SampleInformationPage sampleInformationPage) {
    Objects.requireNonNull(projectInformationPage);
    Objects.requireNonNull(experimentInformationPage);
    handler = new ProjectViewHandler(projectInformationPage,
        experimentInformationPage, sampleInformationPage);
    log.debug(String.format(
        "New instance for project view (#%s) created with a project information page (#%s), an experiment information page (#%s), and a sample information page (#%s)",
        System.identityHashCode(this),
        System.identityHashCode(projectInformationPage),
        System.identityHashCode(experimentInformationPage),
        System.identityHashCode(sampleInformationPage)));
  }

  @Override
  public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
    beforeEnterEvent.getRouteParameters().get("projectId").ifPresentOrElse(projectIdParam -> {
      ProjectId projectId;
      try {
        projectId = ProjectId.parse(projectIdParam);
      } catch (IllegalArgumentException e) {
        throw new ApplicationException("Provided projectId " + projectIdParam + "is invalid");
      }
      handler.setProjectId(projectId);
    }, () -> {
      throw new ApplicationException("no project id provided");
    });
  }
}
